package com.aslanbek.libraryapp.data

import com.aslanbek.libraryapp.model.Book
import com.aslanbek.libraryapp.model.BorrowResult
import com.aslanbek.libraryapp.model.Borrowable
import com.aslanbek.libraryapp.model.Dvd
import com.aslanbek.libraryapp.model.LibraryItem
import com.aslanbek.libraryapp.model.Magazine
import com.aslanbek.libraryapp.model.Member
import com.aslanbek.libraryapp.model.ReturnResult
import kotlinx.coroutines.delay

/**
 * Business rules of the library (adapted from HW1 Library.kt).
 *
 * Differences from HW1: availability is no longer a mutable field inside each item.
 * Loans are kept in one map (item id -> member id), and the class hands out
 * read-only snapshots, so the ViewModel can publish immutable state to the UI.
 */
class Library(
    val name: String,
    val maxLoansPerMember: Int = 3
) {
    // List: the catalog keeps items in the order they were added
    private val items = mutableListOf<LibraryItem>()

    // Map: item id -> id of the member who holds it
    private val loans = mutableMapOf<Int, Int>()

    /** Read-only copy of the catalog. */
    val catalog: List<LibraryItem> get() = items.toList()

    /** Read-only copy of the current loans. */
    fun loansSnapshot(): Map<Int, Int> = loans.toMap()

    fun addItem(item: LibraryItem) {
        require(items.none { it.id == item.id }) { "Duplicate id ${item.id}" }
        items.add(item)
    }

    fun nextId(): Int = (items.maxOfOrNull { it.id } ?: 0) + 1

    /** Higher-order function: the caller decides what to search for. */
    fun search(predicate: (LibraryItem) -> Boolean): List<LibraryItem> = items.filter(predicate)

    fun borrow(itemId: Int, member: Member): BorrowResult {
        val item = items.find { it.id == itemId } ?: return BorrowResult.ItemNotFound

        if (item !is Borrowable) return BorrowResult.NotBorrowable(item)
        if (item.id in loans) return BorrowResult.AlreadyBorrowed(item)

        val held = loans.values.count { it == member.id }
        if (held >= maxLoansPerMember) return BorrowResult.LimitReached(member, maxLoansPerMember)

        loans[item.id] = member.id
        return BorrowResult.Success(item, member, item.loanDays)
    }

    fun returnItem(itemId: Int, member: Member): ReturnResult {
        val item = items.find { it.id == itemId } ?: return ReturnResult.ItemNotFound
        if (loans[itemId] != member.id) return ReturnResult.NotHeldByMember(item, member)
        loans.remove(itemId)
        return ReturnResult.Returned(item, member)
    }

    /**
     * Suspend function (from HW1): pretends to ask a remote server for an item's rating.
     * delay() pauses the coroutine without blocking the main thread.
     */
    suspend fun fetchRating(item: LibraryItem): Double {
        delay(800L + (item.id % 5) * 300L)
        return 3.0 + (item.title.length % 20) / 10.0
    }

    companion object {
        /** Catalog from HW1 plus a few extra items, with two loans already taken. */
        fun createDefault(members: List<Member> = SampleData.members): Library {
            val library = Library("City Library")
            SampleData.items().forEach(library::addItem)
            // Some items are already on loan, so the list shows every status
            members.getOrNull(2)?.let { library.borrow(3, it) }
            members.getOrNull(1)?.let { library.borrow(9, it) }
            return library
        }
    }
}

object SampleData {
    val members: List<Member> = listOf(
        Member(1, "Alice", isStudent = true),
        Member(2, "Bob"),
        Member(3, "Aigerim", isStudent = true)
    )

    fun items(): List<LibraryItem> = listOf(
        Book(1, "Kotlin in Action", 2017, setOf("Programming", "Education"), "Dmitry Jemerov", 360),
        Book(2, "Clean Code", 2008, setOf("Programming"), "Robert C. Martin", 464),
        Book(3, "Dune", 1965, setOf("Science Fiction", "Classic"), "Frank Herbert", 612),
        Book(4, "The Hobbit", 1937, setOf("Fantasy", "Classic"), "J. R. R. Tolkien", 310),
        Dvd(5, "Interstellar", 2014, setOf("Science Fiction", "Drama"), 169),
        Dvd(6, "Spirited Away", 2001, setOf("Fantasy", "Animation"), 125),
        Magazine(7, "National Geographic", 2024, setOf("Science", "Education"), 5),
        Book(8, "Atomic Habits", 2018, setOf("Self-help"), "James Clear", 320),
        Dvd(9, "The Matrix", 1999, setOf("Science Fiction", "Action"), 136),
        Magazine(10, "Wired", 2025, setOf("Technology"), 9)
    )
}
