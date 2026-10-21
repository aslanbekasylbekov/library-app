package com.aslanbek.libraryapp.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aslanbek.libraryapp.data.Library
import com.aslanbek.libraryapp.data.SampleData
import com.aslanbek.libraryapp.model.BorrowResult
import com.aslanbek.libraryapp.model.ItemType
import com.aslanbek.libraryapp.model.ReturnResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Holds the screen state and all application logic.
 *
 * The UI never changes data itself: it only calls these functions (events go up),
 * and draws whatever [uiState] contains (state comes down).
 *
 * A ViewModel survives configuration changes such as rotation: the Activity is
 * destroyed and created again, but it receives this same ViewModel instance.
 */
class LibraryViewModel : ViewModel() {

    private val members = SampleData.members
    private val library = Library.createDefault(members)
    private var nextMessageId = 0L

    /** Short id shown on the "State demo" tab to prove the instance is reused after rotation. */
    val instanceId: String = Integer.toHexString(System.identityHashCode(this))

    /**
     * Observable state. It is Compose State, so any composable that reads it is
     * recomposed when it changes. Only the ViewModel can assign it (private set).
     */
    var uiState by mutableStateOf(
        LibraryUiState(
            libraryName = library.name,
            items = library.catalog,
            loans = library.loansSnapshot(),
            members = members,
            currentMemberId = members.first().id,
            maxLoans = library.maxLoansPerMember
        )
    )
        private set

    init {
        Log.d(TAG, "LibraryViewModel created: $instanceId")
    }

    private inline fun update(transform: (LibraryUiState) -> LibraryUiState) {
        uiState = transform(uiState)
    }

    // ---------- Search and filters ----------

    fun onQueryChange(query: String) = update { it.copy(query = query) }

    fun onTypeFilterChange(type: ItemType?) = update { it.copy(typeFilter = type) }

    fun onOnlyAvailableChange(onlyAvailable: Boolean) = update { it.copy(onlyAvailable = onlyAvailable) }

    fun onMemberSelected(memberId: Int) = update { it.copy(currentMemberId = memberId) }

    // ---------- Borrow / return ----------

    fun borrow(itemId: Int) {
        val member = uiState.currentMember ?: return
        val result = library.borrow(itemId, member)
        update { it.copy(loans = library.loansSnapshot()) }
        showMessage(result.toMessage())
    }

    fun returnItem(itemId: Int) {
        val member = uiState.currentMember ?: return
        val result = library.returnItem(itemId, member)
        update { it.copy(loans = library.loansSnapshot()) }
        showMessage(
            when (result) {
                is ReturnResult.Returned -> "${result.member.name} returned \"${result.item.title}\""
                is ReturnResult.NotHeldByMember -> "${result.member.name} does not hold \"${result.item.title}\""
                ReturnResult.ItemNotFound -> "Item not found"
            }
        )
    }

    /** `when` over a sealed class is exhaustive: the compiler checks every case is handled. */
    private fun BorrowResult.toMessage(): String = when (this) {
        is BorrowResult.Success -> "${member.name} borrowed \"${item.title}\" for $loanDays days"
        is BorrowResult.AlreadyBorrowed -> "\"${item.title}\" is already borrowed"
        is BorrowResult.NotBorrowable -> "\"${item.title}\" is for the reading room only"
        is BorrowResult.LimitReached -> "${member.name} already has $limit items. Return one first."
        BorrowResult.ItemNotFound -> "Item not found"
    }

    // ---------- Adding items ----------

    /** Returns an error text for the form, or null if the item was added. */
    fun addItem(input: NewItemInput): String? {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return when (val result = input.validate(library.nextId(), currentYear)) {
            is ItemValidation.Invalid -> result.message
            is ItemValidation.Valid -> {
                library.addItem(result.item)
                update { it.copy(items = library.catalog) }
                showMessage("Added \"${result.item.title}\" to the catalog")
                null
            }
        }
    }

    // ---------- Coroutines ----------

    /**
     * Loads all ratings in parallel. Runs in viewModelScope, so it keeps going
     * if the screen is rotated, and is cancelled only when the ViewModel is cleared.
     */
    fun loadRatings() {
        if (uiState.isLoadingRatings) return
        update { it.copy(isLoadingRatings = true) }
        viewModelScope.launch {
            val ratings = uiState.items
                .map { item -> async { item.id to library.fetchRating(item) } }
                .awaitAll()
                .toMap()
            update { it.copy(ratings = ratings, isLoadingRatings = false) }
            showMessage("Loaded ${ratings.size} ratings")
        }
    }

    // ---------- State demo ----------

    fun incrementCounter() = update { it.copy(viewModelCounter = it.viewModelCounter + 1) }

    // ---------- Messages ----------

    private fun showMessage(text: String) {
        update { it.copy(message = UserMessage(nextMessageId++, text)) }
    }

    fun onMessageShown(id: Long) {
        if (uiState.message?.id == id) update { it.copy(message = null) }
    }

    override fun onCleared() {
        Log.d(TAG, "LibraryViewModel cleared: $instanceId")
        super.onCleared()
    }

    private companion object {
        const val TAG = "LibraryViewModel"
    }
}
