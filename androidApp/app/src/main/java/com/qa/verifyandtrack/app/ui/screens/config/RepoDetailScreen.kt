package com.qa.verifyandtrack.app.ui.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.data.model.AppConfig
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    navController: NavHostController,
    repoId: String?,
    viewModel: ConfigViewModel = viewModel()
) {
    val repos by viewModel.repos.collectAsState()
    val repo = repos.firstOrNull { it.id == repoId }

    var showRepoDialog by remember { mutableStateOf(false) }
    var showAppDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<AppConfig?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo?.displayLabel ?: "Repository") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (repo != null) {
                        IconButton(onClick = { showRepoDialog = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit repository")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (repo != null) {
                FloatingActionButton(
                    onClick = {
                        editingApp = null
                        showAppDialog = true
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add app")
                }
            }
        }
    ) { padding ->
        if (repo == null) {
            EmptyState("Repository not found.")
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(repo.displayLabel, style = MaterialTheme.typography.titleLarge)
                        Text(repo.fullName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Apps: ${repo.apps.size}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = if (repo.githubToken.isNullOrBlank()) "GitHub token not set" else "GitHub token set",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apps", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (repo.apps.isEmpty()) {
                item { EmptyState("No apps configured yet.") }
            } else {
                items(repo.apps) { app ->
                    AppCard(
                        app = app,
                        onEdit = {
                            editingApp = app
                            showAppDialog = true
                        },
                        onDelete = { viewModel.deleteApp(repo.id, app.id) }
                    )
                }
            }
        }
    }

    if (repo == null) {
        return
    }

    if (showRepoDialog) {
        RepoFormDialog(
            initial = repo,
            onDismiss = { showRepoDialog = false },
            onSave = { updated ->
                viewModel.updateRepo(updated)
                showRepoDialog = false
            }
        )
    }

    if (showAppDialog) {
        AppFormDialog(
            initial = editingApp,
            onDismiss = { showAppDialog = false },
            onSave = { app ->
                if (editingApp == null) {
                    viewModel.addApp(repo.id, app)
                } else {
                    viewModel.updateApp(repo.id, app)
                }
                showAppDialog = false
                editingApp = null
            }
        )
    }
}

@Composable
private fun AppCard(
    app: AppConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleSmall)
                    Text(app.platform, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit app")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete app")
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Build", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(6.dp))
                Text(app.buildNumber, style = MaterialTheme.typography.bodySmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }
            app.playStoreUrl?.let { url ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
