package library

interface Borrowable {
    val loanDays: Int
    fun lateFee(daysLate: Int): Double
}

abstract class LibraryItem(val id: Int, val title: String, val year: Int, val genres: Set<String>) {
    var isAvailable: Boolean = true
    abstract fun describe(): String
    override fun toString(): String = "#$id \"$title\" ($year)"
}

class Book(id: Int, title: String, year: Int, genres: Set<String>, val author: String, val pages: Int) : LibraryItem(id, title, year, genres), Borrowable {
    override val loanDays = 21
    override fun lateFee(daysLate: Int): Double = daysLate * 0.25
    override fun describe(): String = "Book: $title by $author, $pages pages"
}

class Dvd(id: Int, title: String, year: Int, genres: Set<String>, val durationMinutes: Int) : LibraryItem(id, title, year, genres), Borrowable {
    override val loanDays = 7
    override fun lateFee(daysLate: Int): Double = daysLate * 1.0
    override fun describe(): String = "DVD: $title, $durationMinutes min"
}

class Magazine(id: Int, title: String, year: Int, genres: Set<String>, val issue: Int) : LibraryItem(id, title, year, genres) {
    override fun describe(): String = "Magazine: $title, issue $issue (reading room only)"
}

data class Member(val id: Int, val name: String, val isStudent: Boolean = false)

data class Loan(val item: LibraryItem, val member: Member, val daysKept: Int)

sealed class BorrowResult {
    data class Success(val item: LibraryItem, val member: Member, val loanDays: Int) : BorrowResult()
    data class AlreadyBorrowed(val item: LibraryItem) : BorrowResult()
    data class NotBorrowable(val item: LibraryItem) : BorrowResult()
    data class LimitReached(val member: Member, val limit: Int) : BorrowResult()
    data object ItemNotFound : BorrowResult()
}
