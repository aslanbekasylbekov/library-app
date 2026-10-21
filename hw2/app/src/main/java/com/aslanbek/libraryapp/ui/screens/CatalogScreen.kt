package com.aslanbek.libraryapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aslanbek.libraryapp.data.Library
import com.aslanbek.libraryapp.data.SampleData
import com.aslanbek.libraryapp.model.ItemType
import com.aslanbek.libraryapp.ui.LibraryUiState
import com.aslanbek.libraryapp.ui.components.EmptyState
import com.aslanbek.libraryapp.ui.components.LabeledCheckbox
import com.aslanbek.libraryapp.ui.components.LibraryItemCard
import com.aslanbek.libraryapp.ui.components.LibrarySearchField
import com.aslanbek.libraryapp.ui.components.MemberSelector
import com.aslanbek.libraryapp.ui.components.TypeFilterRow
import com.aslanbek.libraryapp.ui.theme.LibraryTheme

/**
 * Stateless screen: it receives the state and reports user actions as lambdas.
 * It does not know a ViewModel exists, so it can be previewed with any state.
 */
@Composable
fun CatalogScreen(
    state: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onTypeFilterChange: (ItemType?) -> Unit,
    onOnlyAvailableChange: (Boolean) -> Unit,
    onMemberSelected: (Int) -> Unit,
    onBorrow: (Int) -> Unit,
    onReturn: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = state.visibleItems

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MemberSelector(
                    members = state.members,
                    selectedId = state.currentMemberId,
                    onSelect = onMemberSelected
                )
                LibrarySearchField(query = state.query, onQueryChange = onQueryChange)
                TypeFilterRow(selected = state.typeFilter, onSelect = onTypeFilterChange)
                LabeledCheckbox(
                    text = "Show only available items",
                    checked = state.onlyAvailable,
                    onCheckedChange = onOnlyAvailableChange
                )
                if (state.isLoadingRatings) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    text = "Showing ${visibleItems.size} of ${state.items.size} items",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (visibleItems.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = "Nothing found",
                    message = "Try another search word or clear the filters."
                )
            }
        }

        items(items = visibleItems, key = { it.id }) { item ->
            LibraryItemCard(
                item = item,
                holder = state.holderOf(item.id),
                currentMemberId = state.currentMemberId,
                rating = state.ratings[item.id],
                onBorrow = { onBorrow(item.id) },
                onReturn = { onReturn(item.id) },
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun CatalogScreenPreview() {
    val library = Library.createDefault()
    LibraryTheme {
        CatalogScreen(
            state = LibraryUiState(
                libraryName = library.name,
                items = library.catalog,
                loans = library.loansSnapshot(),
                members = SampleData.members,
                currentMemberId = 1
            ),
            onQueryChange = {},
            onTypeFilterChange = {},
            onOnlyAvailableChange = {},
            onMemberSelected = {},
            onBorrow = {},
            onReturn = {}
        )
    }
}
