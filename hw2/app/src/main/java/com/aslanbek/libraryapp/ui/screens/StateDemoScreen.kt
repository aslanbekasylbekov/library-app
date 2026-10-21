package com.aslanbek.libraryapp.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aslanbek.libraryapp.ui.components.CounterCard
import com.aslanbek.libraryapp.ui.components.SectionHeader
import com.aslanbek.libraryapp.ui.theme.LibraryTheme

/**
 * Shows where each kind of state lives and what happens to it when the
 * Activity is recreated (rotation, dark mode switch, language change...).
 */
@Composable
fun StateDemoScreen(
    viewModelCounter: Int,
    viewModelId: String,
    onIncrementViewModelCounter: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. remember: kept in the composition only. Rotation destroys the composition -> back to 0.
    var rememberCounter by remember { mutableIntStateOf(0) }

    // 2. rememberSaveable: also written to the saved-instance-state Bundle -> restored after rotation.
    var saveableCounter by rememberSaveable { mutableIntStateOf(0) }

    // 3. The ViewModel counter comes from outside (state hoisting) and survives as well.

    val activity = LocalContext.current.findActivity()
    val activityId = activity?.let { Integer.toHexString(System.identityHashCode(it)) } ?: "preview"

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tap +1 on every counter, then rotate the device or press the button at the bottom.",
            style = MaterialTheme.typography.bodyMedium
        )
        CounterCard(
            title = "remember",
            value = rememberCounter,
            description = "Stored in the composition only.",
            survivesRotation = false,
            onIncrement = { rememberCounter++ }
        )
        CounterCard(
            title = "rememberSaveable",
            value = saveableCounter,
            description = "Saved to the Bundle and restored.",
            survivesRotation = true,
            onIncrement = { saveableCounter++ }
        )
        CounterCard(
            title = "ViewModel",
            value = viewModelCounter,
            description = "Kept by the ViewModel, which outlives the Activity.",
            survivesRotation = true,
            onIncrement = onIncrementViewModelCounter
        )

        SectionHeader("Object instances")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InstanceLine("Activity ", activityId, "new one after every rotation")
                InstanceLine("ViewModel", viewModelId, "same object, reused")
            }
        }

        Button(
            onClick = { activity?.recreate() },
            enabled = activity != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("Recreate Activity (same as rotation)")
        }

        SectionHeader("What else uses each kind of state here")
        Text(
            text = "remember — objects that are cheap to rebuild, e.g. SnackbarHostState.\n" +
                "rememberSaveable — UI-only state: selected tab, expanded cards, the Add item form, the open dialog.\n" +
                "ViewModel — app data and logic: catalog, loans, search text, filters, ratings loading.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InstanceLine(name: String, id: String, note: String) {
    Text(
        text = "$name  $id  ($note)",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace
    )
}

/** LocalContext may be wrapped (e.g. by a theme), so walk up to the real Activity. */
private fun Context.findActivity(): Activity? {
    var context: Context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun StateDemoScreenPreview() {
    LibraryTheme {
        StateDemoScreen(viewModelCounter = 3, viewModelId = "1a2b3c", onIncrementViewModelCounter = {})
    }
}
