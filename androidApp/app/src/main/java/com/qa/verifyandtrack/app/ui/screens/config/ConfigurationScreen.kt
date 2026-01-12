package com.qa.verifyandtrack.app.ui.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Delete
import androidx.compose.material3.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.viewmodel.ConfigViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(navController: NavHostController, viewModel: ConfigViewModel = viewModel()) {
    val repos by viewModel.repos.collectAsState()

    var showRepoDialog by remember { mutableStateOf(false) }
    var editingRepo by remember { mutableStateOf<Repository?>(null) }
    var showAppDialog by remember { mutableStateOf<Repository?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configuration") }) },
        floatingActionButton = {
            IconButton(onClick = {
                editingRepo = null
                showRepoDialog = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add repo")
            }
        }
    ) { padding ->
        if (repos.isEmpty()) {
            EmptyState("Add a repository to get started.")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(repos) { repo ->
                    RepoCard(
                        repo = repo,
                        onEdit = {
                            editingRepo = repo
                            showRepoDialog = true
                        },
                        onDelete = { viewModel.deleteRepo(repo.id) },
                        onAddApp = { showAppDialog = repo }
                    )
                }
            }
        }
    }

    if (showRepoDialog) {
        RepoFormDialog(
            initial = editingRepo,
            onDismiss = { showRepoDialog = false },
            onSave = { repo ->
                if (editingRepo == null) {
                    viewModel.addRepo(repo)
                } else {
                    viewModel.updateRepo(repo)
                }
                showRepoDialog = false
                editingRepo = null
            }
        )
    }

    showAppDialog?.let { repo ->
        AppFormDialog(
            onDismiss = { showAppDialog = null },
            onSave = { app ->
                viewModel.addApp(repo.id, app)
                showAppDialog = null
            }
        )
    }
}

@Composable
private fun RepoCard(
    repo: Repository,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddApp: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(repo.displayLabel, style = MaterialTheme.typography.titleMedium)
                    Text(repo.fullName, style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (repo.apps.isNotEmpty()) {
                Text("Apps", style = MaterialTheme.typography.titleSmall)
                repo.apps.forEach { app ->
                    Text("${app.name} (${app.platform}) - ${app.buildNumber}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onAddApp) {
                Text("Add App")
            }
        }
    }
}

@Composable
private fun RepoFormDialog(
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AppFormDialog(
    onDismiss: () -> Unit,
    onSave: (AppConfig) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("android") }
    var playStoreUrl by remember { mutableStateOf("") }
    var buildNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add App") },
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
                    id = UUID.randomUUID().toString(),
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
