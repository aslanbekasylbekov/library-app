package com.aslanbek.libraryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aslanbek.libraryapp.data.Library
import com.aslanbek.libraryapp.data.SampleData
import com.aslanbek.libraryapp.model.ItemType
import com.aslanbek.libraryapp.ui.LibraryStats
import com.aslanbek.libraryapp.ui.LibraryUiState
import com.aslanbek.libraryapp.ui.components.EmptyState
import com.aslanbek.libraryapp.ui.components.LibraryItemCard
import com.aslanbek.libraryapp.ui.components.MemberSelector
import com.aslanbek.libraryapp.ui.components.SectionHeader
import com.aslanbek.libraryapp.ui.components.StatCard
import com.aslanbek.libraryapp.ui.theme.LibraryTheme

/** Items held by the selected reader, plus statistics for the whole library. */
@Composable
fun LoansScreen(
    state: LibraryUiState,
    onMemberSelected: (Int) -> Unit,
    onReturn: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val myItems = state.myItems
    val stats = state.stats
    val readerName = state.currentMember?.name ?: "Reader"

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "selector") {
            MemberSelector(
                members = state.members,
                selectedId = state.currentMemberId,
                onSelect = onMemberSelected
            )
        }
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("On loan", "${myItems.size} / ${state.maxLoans}", Modifier.weight(1f))
                StatCard("Free now", "${stats.available}", Modifier.weight(1f))
                StatCard("Genres", "${stats.genres.size}", Modifier.weight(1f))
            }
        }
        item(key = "mine-header") { SectionHeader("Borrowed by $readerName") }

        if (myItems.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Person,
                    title = "No loans yet",
                    message = "Open the Catalog tab and borrow a book or a DVD."
                )
            }
        } else {
            items(items = myItems, key = { it.id }) { item ->
                LibraryItemCard(
                    item = item,
                    holder = state.currentMember,
                    currentMemberId = state.currentMemberId,
                    rating = state.ratings[item.id],
                    onBorrow = {},
                    onReturn = { onReturn(item.id) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        item(key = "stats-header") { SectionHeader("Library statistics") }
        item(key = "stats-card") { LibraryStatsCard(stats) }
    }
}

@Composable
private fun LibraryStatsCard(stats: LibraryStats, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StatLine("Items in catalog", "${stats.totalItems}")
            StatLine("On loan right now", "${stats.onLoan}")
            HorizontalDivider()
            ItemType.entries.forEach { type ->
                StatLine(type.label, "${stats.countByType[type] ?: 0}")
            }
            HorizontalDivider()
            StatLine("Pages in all books", "${stats.totalBookPages}")
            StatLine("Oldest item", stats.oldest?.let { "${it.title} (${it.year})" } ?: "—")
            Text(
                text = stats.genres.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LoansScreenPreview() {
    val library = Library.createDefault()
    LibraryTheme {
        LoansScreen(
            state = LibraryUiState(
                items = library.catalog,
                loans = library.loansSnapshot(),
                members = SampleData.members,
                currentMemberId = 3
            ),
            onMemberSelected = {},
            onReturn = {}
        )
    }
}
