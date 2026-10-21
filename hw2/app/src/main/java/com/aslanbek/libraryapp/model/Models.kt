package com.aslanbek.libraryapp.model

// Domain model adapted from HW1 (LibraryApp console project).
// Pure Kotlin: nothing here knows about Android or Compose.

// ---------- Interface ----------

/** Anything that can be taken home. Magazines do not implement it. */
interface Borrowable {
    val loanDays: Int
    fun lateFee(daysLate: Int): Double
}

/** Kind of catalog item. Used by the filter chips and by the "add item" form. */
enum class ItemType(val label: String, val singular: String) {
    BOOK("Books", "Book"),
    DVD("DVDs", "DVD"),
    MAGAZINE("Magazines", "Magazine")
}

// ---------- Inheritance: abstract base class + subclasses ----------

abstract class LibraryItem(
    val id: Int,
    val title: String,
    val year: Int,
    val genres: Set<String>
) {
    abstract val type: ItemType

    /** Short line shown under the title in the list (author, duration, issue). */
    abstract val subtitle: String

    /** Every subclass describes itself in its own way (polymorphism). */
    abstract fun describe(): String

    override fun toString(): String = "#$id \"$title\" ($year)"
}

class Book(
    id: Int,
    title: String,
    year: Int,
    genres: Set<String>,
    val author: String,
    val pages: Int
) : LibraryItem(id, title, year, genres), Borrowable {
    override val type: ItemType get() = ItemType.BOOK
    override val subtitle: String get() = "$author · $year"
    override val loanDays: Int get() = 21
    override fun lateFee(daysLate: Int): Double = daysLate * 0.25
    override fun describe(): String =
        "Book by $author, $pages pages. Loan period $loanDays days, late fee 0.25 per day."
}

class Dvd(
    id: Int,
    title: String,
    year: Int,
    genres: Set<String>,
    val durationMinutes: Int
) : LibraryItem(id, title, year, genres), Borrowable {
    override val type: ItemType get() = ItemType.DVD
    override val subtitle: String get() = "Film · $durationMinutes min · $year"
    override val loanDays: Int get() = 7
    override fun lateFee(daysLate: Int): Double = daysLate * 1.0
    override fun describe(): String =
        "DVD, $durationMinutes minutes. Loan period $loanDays days, late fee 1.00 per day."
}

/** Magazines can only be read in the library, so they are NOT Borrowable. */
class Magazine(
    id: Int,
    title: String,
    year: Int,
    genres: Set<String>,
    val issue: Int
) : LibraryItem(id, title, year, genres) {
    override val type: ItemType get() = ItemType.MAGAZINE
    override val subtitle: String get() = "Issue $issue · $year"
    override fun describe(): String =
        "Magazine, issue $issue. Reading room only: it cannot be taken home."
}

// ---------- Data class ----------

data class Member(val id: Int, val name: String, val isStudent: Boolean = false)

// ---------- Sealed classes ----------

/** All possible outcomes of trying to borrow an item. */
sealed class BorrowResult {
    data class Success(val item: LibraryItem, val member: Member, val loanDays: Int) : BorrowResult()
    data class AlreadyBorrowed(val item: LibraryItem) : BorrowResult()
    data class NotBorrowable(val item: LibraryItem) : BorrowResult()
    data class LimitReached(val member: Member, val limit: Int) : BorrowResult()
    data object ItemNotFound : BorrowResult()
}

/** All possible outcomes of trying to return an item. */
sealed class ReturnResult {
    data class Returned(val item: LibraryItem, val member: Member) : ReturnResult()
    data class NotHeldByMember(val item: LibraryItem, val member: Member) : ReturnResult()
    data object ItemNotFound : ReturnResult()
}
