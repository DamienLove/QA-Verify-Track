package com.qa.verifyandtrack.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.components.LoadingIndicator
import com.qa.verifyandtrack.app.ui.components.TodoItemRow
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = viewModel()) {
    val repos by viewModel.repos.collectAsState()
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repositories") },
                actions = {
                    IconButton(onClick = { viewModel.refreshTodos() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.Configuration.route) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add repository")
            }
        }
    ) { padding ->
        if (isLoading && repos.isEmpty()) {
            LoadingIndicator("Loading repositories...")
            return@Scaffold
        }
        if (repos.isEmpty()) {
            EmptyState("No repositories configured yet.")
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Your Repos", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(repos) { repo ->
                RepoCard(repo = repo, onClick = { navController.navigate(Screen.Dashboard.createRoute(repo.id)) })
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Todos", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = { viewModel.refreshTodos() }) {
                        Text("Refresh")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(todos) { todo ->
                TodoItemRow(item = todo) {
                    navController.navigate(Screen.Dashboard.createRoute(todo.repoId))
                }
            }
        }
    }
}

@Composable
private fun RepoCard(repo: Repository, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!repo.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = repo.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(repo.displayLabel, style = MaterialTheme.typography.titleMedium)
                Text(repo.fullName, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
