package com.aslanbek.libraryapp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aslanbek.libraryapp.model.ItemType
import com.aslanbek.libraryapp.ui.NewItemInput

/**
 * Form for a new book, DVD or magazine.
 *
 * The draft fields use rememberSaveable: if the user rotates the phone while
 * typing, the dialog comes back with everything they typed.
 * Validation is NOT done here: onSubmit hands the raw input to the ViewModel,
 * which returns an error text (or null when the item was added).
 */
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onSubmit: (NewItemInput) -> String?
) {
    var type by rememberSaveable { mutableStateOf(ItemType.BOOK) }
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }
    var genres by rememberSaveable { mutableStateOf("") }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    val numberLabel = when (type) {
        ItemType.BOOK -> "Pages"
        ItemType.DVD -> "Minutes"
        ItemType.MAGAZINE -> "Issue no."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to catalog") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ItemType.entries.forEach { option ->
                        FilterChip(
                            selected = option == type,
                            onClick = { type = option; errorText = null },
                            label = { Text(option.singular) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; errorText = null },
                    label = { Text("Title") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == ItemType.BOOK) {
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it; errorText = null },
                        label = { Text("Author") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = year,
                        onValueChange = { input -> year = input.filter(Char::isDigit).take(4); errorText = null },
                        label = { Text("Year") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { input -> number = input.filter(Char::isDigit).take(5); errorText = null },
                        label = { Text(numberLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = genres,
                    onValueChange = { genres = it },
                    label = { Text("Genres, comma separated") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    errorText = onSubmit(NewItemInput(type, title, author, year, number, genres))
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
