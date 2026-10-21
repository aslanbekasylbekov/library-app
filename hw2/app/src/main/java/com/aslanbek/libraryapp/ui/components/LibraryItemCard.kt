package com.aslanbek.libraryapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aslanbek.libraryapp.data.SampleData
import com.aslanbek.libraryapp.model.Borrowable
import com.aslanbek.libraryapp.model.LibraryItem
import com.aslanbek.libraryapp.model.Member
import com.aslanbek.libraryapp.ui.theme.LibraryTheme
import java.util.Locale

/**
 * Reusable: one catalog entry. Stateless about the library data — it only shows
 * what it is given and reports clicks through onBorrow / onReturn.
 * The only state it owns is whether the details are expanded (pure UI state).
 */
@Composable
fun LibraryItemCard(
    item: LibraryItem,
    holder: Member?,
    currentMemberId: Int,
    rating: Double?,
    onBorrow: () -> Unit,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable: the card stays expanded after rotation
    var expanded by rememberSaveable(item.id) { mutableStateOf(false) }

    val status = when {
        item !is Borrowable -> ItemStatus.READING_ROOM
        holder == null -> ItemStatus.AVAILABLE
        holder.id == currentMemberId -> ItemStatus.MINE
        else -> ItemStatus.BORROWED
    }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemTypeIcon(type = item.type)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.genres.isNotEmpty()) {
                        Text(
                            text = item.genres.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide details" else "Show details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                HorizontalDivider()
                Text(text = item.describe(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (rating != null) {
                        "Rating: ★ " + String.format(Locale.US, "%.1f", rating)
                    } else {
                        "Rating not loaded yet: tap ★ in the top bar"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = status, holderName = holder?.name)
                if (rating != null && !expanded) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "★ " + String.format(Locale.US, "%.1f", rating),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(Modifier.weight(1f))
                when (status) {
                    ItemStatus.AVAILABLE -> Button(onClick = onBorrow) { Text("Borrow") }
                    ItemStatus.MINE -> OutlinedButton(onClick = onReturn) { Text("Return") }
                    ItemStatus.BORROWED, ItemStatus.READING_ROOM -> Unit
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryItemCardPreview() {
    LibraryTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val items = SampleData.items()
            LibraryItemCard(items[0], null, 1, 4.2, {}, {})
            LibraryItemCard(items[4], SampleData.members[0], 1, null, {}, {})
            LibraryItemCard(items[6], null, 1, null, {}, {})
        }
    }
}
