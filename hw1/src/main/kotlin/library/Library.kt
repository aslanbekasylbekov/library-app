package library

import kotlinx.coroutines.delay

class Library(val name: String) {

    private val items = mutableListOf<LibraryItem>()
    private val loansByMember = mutableMapOf<Int, MutableList<LibraryItem>>()
    private val maxLoansPerMember = 2
    val catalog: List<LibraryItem> get() = items

    fun addItem(item: LibraryItem) {
        items.add(item)
    }

    fun search(predicate: (LibraryItem) -> Boolean): List<LibraryItem> = items.filter(predicate)
    fun allGenres(): Set<String> = items.flatMap { it.genres }.toSortedSet()
    fun itemsHeldBy(member: Member): List<LibraryItem> = loansByMember[member.id] ?: emptyList()

    fun borrow(itemId: Int, member: Member): BorrowResult {
        val item = items.find { it.id == itemId } ?: return BorrowResult.ItemNotFound
        val held = loansByMember.getOrPut(member.id) { mutableListOf() }

        if (item !is Borrowable) return BorrowResult.NotBorrowable(item)
        if (!item.isAvailable) return BorrowResult.AlreadyBorrowed(item)
        if (held.size >= maxLoansPerMember) return BorrowResult.LimitReached(member, maxLoansPerMember)

        item.isAvailable = false
        held.add(item)
        return BorrowResult.Success(item, member, item.loanDays)
    }

    fun returnItem(loan: Loan): Double {
        val item = loan.item
        loansByMember[loan.member.id]?.remove(item)
        item.isAvailable = true

        if (item !is Borrowable) return 0.0
        val daysLate = loan.daysKept - item.loanDays
        val fee = if (daysLate > 0) item.lateFee(daysLate) else 0.0
        return if (loan.member.isStudent) fee / 2 else fee
    }

    suspend fun fetchRating(item: LibraryItem): Double {
        delay(300L + item.id * 100L)
        return 3.0 + (item.title.length % 20) / 10.0
    }
}
