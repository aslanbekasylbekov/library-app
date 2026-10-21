package com.aslanbek.libraryapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aslanbek.libraryapp.R
import com.aslanbek.libraryapp.model.ItemType

@DrawableRes
fun ItemType.iconRes(): Int = when (this) {
    ItemType.BOOK -> R.drawable.ic_book
    ItemType.DVD -> R.drawable.ic_dvd
    ItemType.MAGAZINE -> R.drawable.ic_magazine
}

/** Reusable: square tile with the picture for a book, DVD or magazine. */
@Composable
fun ItemTypeIcon(
    type: ItemType,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (type) {
        ItemType.BOOK -> colors.primaryContainer to colors.onPrimaryContainer
        ItemType.DVD -> colors.secondaryContainer to colors.onSecondaryContainer
        ItemType.MAGAZINE -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(type.iconRes()),
            contentDescription = type.singular,
            colorFilter = ColorFilter.tint(content),
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

enum class ItemStatus { AVAILABLE, MINE, BORROWED, READING_ROOM }

/** Reusable: small pill that shows whether an item can be taken. */
@Composable
fun StatusBadge(
    status: ItemStatus,
    modifier: Modifier = Modifier,
    holderName: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val (text, container, content) = when (status) {
        ItemStatus.AVAILABLE ->
            Triple("Available", colors.secondaryContainer, colors.onSecondaryContainer)
        ItemStatus.MINE ->
            Triple("Borrowed by you", colors.primaryContainer, colors.onPrimaryContainer)
        ItemStatus.BORROWED ->
            Triple("Borrowed by ${holderName ?: "another reader"}", colors.errorContainer, colors.onErrorContainer)
        ItemStatus.READING_ROOM ->
            Triple("Reading room only", colors.tertiaryContainer, colors.onTertiaryContainer)
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
