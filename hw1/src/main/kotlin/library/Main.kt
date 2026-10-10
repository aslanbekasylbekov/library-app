package library

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

fun printHeader(title: String) {
    println()
    println("=== $title ===")
}

fun printItems(items: List<LibraryItem>, format: (LibraryItem) -> String) {
    if (items.isEmpty()) {
        println("  (nothing found)")
        return
    }
    for ((index, item) in items.withIndex()) {
        println("  ${index + 1}. ${format(item)}")
    }
}

fun explain(result: BorrowResult): String = when (result) {
    is BorrowResult.Success ->
        "OK: ${result.member.name} borrowed \"${result.item.title}\" for ${result.loanDays} days"
    is BorrowResult.AlreadyBorrowed ->
        "FAILED: \"${result.item.title}\" is already borrowed"
    is BorrowResult.NotBorrowable ->
        "FAILED: \"${result.item.title}\" cannot be taken home"
    is BorrowResult.LimitReached ->
        "FAILED: ${result.member.name} already has ${result.limit} items"
    BorrowResult.ItemNotFound ->
        "FAILED: no item with this id"
}

fun createLibrary(): Library {
    val library = Library("City Library")
    library.addItem(Book(1, "Kotlin in Action", 2017, setOf("Programming", "Education"), "Dmitry Jemerov", 360))
    library.addItem(Book(2, "Clean Code", 2008, setOf("Programming"), "Robert C. Martin", 464))
    library.addItem(Book(3, "Dune", 1965, setOf("Science Fiction", "Classic"), "Frank Herbert", 612))
    library.addItem(Book(4, "The Hobbit", 1937, setOf("Fantasy", "Classic"), "J. R. R. Tolkien", 310))
    library.addItem(Dvd(5, "Interstellar", 2014, setOf("Science Fiction", "Drama"), 169))
    library.addItem(Dvd(6, "Spirited Away", 2001, setOf("Fantasy", "Animation"), 125))
    library.addItem(Magazine(7, "National Geographic", 2024, setOf("Science", "Education"), 5))
    return library
}

fun main() = runBlocking {
    val library = createLibrary()
    val appVersion: Double = 1.0
    val isOpen: Boolean = true
    var failedAttempts: Int = 0

    println("Welcome to ${library.name}! (app version $appVersion)")
    if (!isOpen) {
        println("Sorry, the library is closed today.")
        return@runBlocking
    }

    printHeader("Catalog")
    printItems(library.catalog) { it.describe() }

    printHeader("Genres")
    val genres: Set<String> = library.allGenres()
    println("  ${genres.size} unique genres: ${genres.joinToString()}")

    printHeader("Search")
    println("Science Fiction:")
    printItems(library.search { "Science Fiction" in it.genres }) { it.toString() }

    println("Published before 2000:")
    printItems(library.search { it.year < 2000 }) { "${it.title} (${it.year})" }

    val isLongBook: (LibraryItem) -> Boolean = { it is Book && it.pages > 400 }
    println("Books longer than 400 pages:")
    printItems(library.search(isLongBook)) { "${it.title} - ${(it as Book).pages} pages" }

    printHeader("Statistics")
    val books = library.catalog.filterIsInstance<Book>()
    val titlesUpperCase = books.map { it.title.uppercase() }
    val totalPages = books.map { it.pages }.reduce { sum, pages -> sum + pages }
    val oldest = library.catalog.reduce { oldest, item -> if (item.year < oldest.year) item else oldest }
    val countByType: Map<String, Int> = library.catalog.groupingBy { it::class.simpleName ?: "Unknown" }.eachCount()

    println("  Book titles: $titlesUpperCase")
    println("  Total pages in all books: $totalPages")
    println("  Average book length: ${totalPages / books.size} pages")
    println("  Oldest item: ${oldest.title} (${oldest.year})")
    for ((type, count) in countByType) {
        println("  $type: $count")
    }

    printHeader("Borrowing")
    val alice = Member(1, "Alice", isStudent = true)
    val bob = Member(2, "Bob")

    val aliceRenamed = alice.copy(name = "Alice Smith")
    println("  $alice")
    println("  Copy with a new name: $aliceRenamed, same person: ${alice == aliceRenamed}")

    val requests = listOf(1 to alice, 5 to alice, 3 to alice, 1 to bob, 7 to bob, 42 to bob, 3 to bob)
    for ((itemId, member) in requests) {
        val result = library.borrow(itemId, member)
        if (result !is BorrowResult.Success) failedAttempts++
        println("  ${explain(result)}")
    }
    println("  Failed attempts: $failedAttempts of ${requests.size}")

    printHeader("Still on the shelf")
    val available = library.search { it.isAvailable }
    var i = 0
    while (i < available.size) {
        println("  - ${available[i].title}")
        i++
    }

    printHeader("Returns")
    val loans = listOf(
        Loan(library.catalog[0], alice, daysKept = 30),  // book, 9 days late, student discount
        Loan(library.catalog[4], alice, daysKept = 5),   // DVD, on time
        Loan(library.catalog[2], bob, daysKept = 25)     // book, 4 days late
    )
    val fees = loans.map { library.returnItem(it) }
    for ((loan, fee) in loans.zip(fees)) {
        val status = if (fee > 0) "late, fee $${"%.2f".format(fee)}" else "on time"
        println("  ${loan.member.name} returned \"${loan.item.title}\" after ${loan.daysKept} days: $status")
    }
    val totalFees = fees.reduce { sum, fee -> sum + fee }
    println("  Total fees collected: $${"%.2f".format(totalFees)}")

    printHeader("Online ratings (coroutines)")

    val saveJob = launch {
        println("  [background] saving the loan history...")
        delay(500)
        println("  [background] loan history saved")
    }

    val time = measureTimeMillis {
        val ratings = library.catalog
            .map { item -> async { item to library.fetchRating(item) } }
            .awaitAll()

        for ((item, rating) in ratings.sortedByDescending { it.second }) {
            println("  ${"%.1f".format(rating)}  ${item.title}")
        }
    }
    println("  Loaded ${library.catalog.size} ratings in $time ms")

    saveJob.join()
    println()
    println("Goodbye!")
}
