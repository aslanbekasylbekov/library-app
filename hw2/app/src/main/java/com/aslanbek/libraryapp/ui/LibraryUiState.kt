package com.aslanbek.libraryapp.ui

import com.aslanbek.libraryapp.model.Book
import com.aslanbek.libraryapp.model.Dvd
import com.aslanbek.libraryapp.model.ItemType
import com.aslanbek.libraryapp.model.LibraryItem
import com.aslanbek.libraryapp.model.Magazine
import com.aslanbek.libraryapp.model.Member

/** One-time message for the Snackbar. The id makes two equal texts count as two events. */
data class UserMessage(val id: Long, val text: String)

/**
 * Everything the screens need to draw themselves. Immutable: the ViewModel
 * replaces it with copy(...) on every change, and Compose redraws what changed.
 */
data class LibraryUiState(
    val libraryName: String = "",
    val items: List<LibraryItem> = emptyList(),
    val loans: Map<Int, Int> = emptyMap(),          // item id -> member id
    val members: List<Member> = emptyList(),
    val currentMemberId: Int = 0,
    val maxLoans: Int = 3,
    val query: String = "",
    val typeFilter: ItemType? = null,                // null = all types
    val onlyAvailable: Boolean = false,
    val ratings: Map<Int, Double> = emptyMap(),      // item id -> rating
    val isLoadingRatings: Boolean = false,
    val viewModelCounter: Int = 0,
    val message: UserMessage? = null
) {
    val currentMember: Member? get() = members.find { it.id == currentMemberId }

    /** Catalog after search, type filter and the "only available" checkbox. */
    val visibleItems: List<LibraryItem>
        get() = items.filter { item ->
            val matchesType = typeFilter == null || item.type == typeFilter
            val matchesAvailability = !onlyAvailable || item.id !in loans
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                item.title.contains(q, ignoreCase = true) ||
                item.subtitle.contains(q, ignoreCase = true) ||
                item.genres.any { it.contains(q, ignoreCase = true) }
            matchesType && matchesAvailability && matchesQuery
        }

    /** Items the selected member currently holds. */
    val myItems: List<LibraryItem> get() = items.filter { loans[it.id] == currentMemberId }

    fun holderOf(itemId: Int): Member? = loans[itemId]?.let { id -> members.find { it.id == id } }

    val stats: LibraryStats get() = LibraryStats.from(items, loans)
}

/** Numbers for the statistics card. Uses map / filter / reduce like in HW1. */
data class LibraryStats(
    val totalItems: Int,
    val available: Int,
    val onLoan: Int,
    val genres: Set<String>,
    val countByType: Map<ItemType, Int>,
    val totalBookPages: Int,
    val oldest: LibraryItem?
) {
    companion object {
        fun from(items: List<LibraryItem>, loans: Map<Int, Int>): LibraryStats {
            val books = items.filterIsInstance<Book>()
            val pages = books.map { it.pages }
            return LibraryStats(
                totalItems = items.size,
                available = items.count { it.id !in loans },
                onLoan = loans.size,
                genres = items.flatMap { it.genres }.toSortedSet(),
                countByType = items.groupingBy { it.type }.eachCount(),
                totalBookPages = if (pages.isEmpty()) 0 else pages.reduce { sum, p -> sum + p },
                oldest = items.reduceOrNull { oldest, item -> if (item.year < oldest.year) item else oldest }
            )
        }
    }
}

/** Raw text from the "Add item" form, exactly as the user typed it. */
data class NewItemInput(
    val type: ItemType,
    val title: String,
    val author: String,
    val year: String,
    val number: String,   // pages for a book, minutes for a DVD, issue for a magazine
    val genres: String    // comma separated
)

sealed class ItemValidation {
    data class Valid(val item: LibraryItem) : ItemValidation()
    data class Invalid(val message: String) : ItemValidation()
}

/** Checks the form and builds the right subclass. Pure function, easy to test. */
fun NewItemInput.validate(newId: Int, currentYear: Int): ItemValidation {
    val cleanTitle = title.trim()
    val yearValue = year.trim().toIntOrNull()
    val numberValue = number.trim().toIntOrNull()
    val numberName = when (type) {
        ItemType.BOOK -> "pages"
        ItemType.DVD -> "minutes"
        ItemType.MAGAZINE -> "issue number"
    }

    if (cleanTitle.isEmpty()) return ItemValidation.Invalid("Enter a title")
    if (type == ItemType.BOOK && author.isBlank()) return ItemValidation.Invalid("Enter the author")
    if (yearValue == null || yearValue !in 1450..currentYear) {
        return ItemValidation.Invalid("Year must be between 1450 and $currentYear")
    }
    if (numberValue == null || numberValue <= 0) {
        return ItemValidation.Invalid("Enter a positive number of $numberName")
    }

    val genreSet = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    val item = when (type) {
        ItemType.BOOK -> Book(newId, cleanTitle, yearValue, genreSet, author.trim(), numberValue)
        ItemType.DVD -> Dvd(newId, cleanTitle, yearValue, genreSet, numberValue)
        ItemType.MAGAZINE -> Magazine(newId, cleanTitle, yearValue, genreSet, numberValue)
    }
    return ItemValidation.Valid(item)
}
