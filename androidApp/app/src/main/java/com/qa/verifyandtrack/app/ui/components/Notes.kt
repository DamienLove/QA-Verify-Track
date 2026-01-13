package com.qa.verifyandtrack.app.ui.components

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val NOTES_PREFS = "qa_notes_prefs"
private const val NOTES_KEY = "notes_value"

@Composable
fun NotesDialog(showDialog: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE) }
    var noteText by remember { mutableStateOf(prefs.getString(NOTES_KEY, "") ?: "") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Notes") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    minLines = 4,
                    label = { Text("Personal notes") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString(NOTES_KEY, noteText).apply()
                    onDismiss()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}
