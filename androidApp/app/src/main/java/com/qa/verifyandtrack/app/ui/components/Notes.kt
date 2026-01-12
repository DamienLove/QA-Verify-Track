package com.qa.verifyandtrack.app.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val NOTES_PREFS = "qa_notes_prefs"
private const val NOTES_KEY = "notes_value"

@Composable
fun Notes(contentPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE) }
    var showDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(prefs.getString(NOTES_KEY, "") ?: "") }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(contentPadding)
                .padding(16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "Notes")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
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
                    showDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
