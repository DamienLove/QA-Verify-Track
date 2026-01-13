package com.qa.verifyandtrack.app.ui.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.Repository
import java.util.UUID

@Composable
fun RepoFormDialog(
    initial: Repository?,
    onDismiss: () -> Unit,
    onSave: (Repository) -> Unit
) {
    var displayName by remember { mutableStateOf(initial?.displayName ?: "") }
    var owner by remember { mutableStateOf(initial?.owner ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var token by remember { mutableStateOf(initial?.githubToken ?: "") }
    var avatarUrl by remember { mutableStateOf(initial?.avatarUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Repository" else "Edit Repository") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") })
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Owner") })
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Repo Name") })
                OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("GitHub Token") })
                OutlinedTextField(value = avatarUrl, onValueChange = { avatarUrl = it }, label = { Text("Avatar URL") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val repo = Repository(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    owner = owner.trim(),
                    name = name.trim(),
                    displayName = displayName.trim().ifBlank { null },
                    githubToken = token.trim().ifBlank { null },
                    avatarUrl = avatarUrl.trim().ifBlank { null },
                    apps = initial?.apps ?: emptyList(),
                    isConnected = initial?.isConnected ?: true
                )
                onSave(repo)
            }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AppFormDialog(
    initial: AppConfig? = null,
    onDismiss: () -> Unit,
    onSave: (AppConfig) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var platform by remember { mutableStateOf(initial?.platform ?: "android") }
    var playStoreUrl by remember { mutableStateOf(initial?.playStoreUrl ?: "") }
    var buildNumber by remember { mutableStateOf(initial?.buildNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add App" else "Edit App") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = platform, onValueChange = { platform = it }, label = { Text("Platform") })
                OutlinedTextField(value = buildNumber, onValueChange = { buildNumber = it }, label = { Text("Build Number") })
                OutlinedTextField(value = playStoreUrl, onValueChange = { playStoreUrl = it }, label = { Text("Play Store URL") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val app = AppConfig(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    platform = platform.trim().ifBlank { "android" },
                    buildNumber = buildNumber.trim(),
                    playStoreUrl = playStoreUrl.trim().ifBlank { null }
                )
                onSave(app)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
