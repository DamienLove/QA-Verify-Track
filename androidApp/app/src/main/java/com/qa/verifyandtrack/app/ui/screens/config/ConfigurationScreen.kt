package com.qa.verifyandtrack.app.ui.screens.config

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.viewmodel.ConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationScreen(navController: NavHostController, viewModel: ConfigViewModel = viewModel()) {
    val repos by viewModel.repos.collectAsState()

    var showRepoDialog by remember { mutableStateOf(false) }
    var editingRepo by remember { mutableStateOf<Repository?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configuration") }) },
        floatingActionButton = {
            IconButton(onClick = {
                editingRepo = null
                showRepoDialog = true
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add repo")
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
                        onClick = {
                            navController.navigate(Screen.RepoDetail.createRoute(repo.id))
                        },
                        onEdit = {
                            editingRepo = repo
                            showRepoDialog = true
                        },
                        onDelete = { viewModel.deleteRepo(repo.id) }
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
}

@Composable
private fun RepoCard(
    repo: Repository,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(repo.displayLabel, style = MaterialTheme.typography.titleMedium)
                    Text(repo.fullName, style = MaterialTheme.typography.bodyMedium)
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${repo.apps.size} apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
