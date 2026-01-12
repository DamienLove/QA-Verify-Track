package com.qa.verifyandtrack.app.ui.screens.quickissue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.ui.viewmodel.QuickIssueViewModel

@Composable
fun QuickIssueScreen(navController: NavHostController, repoId: String?, viewModel: QuickIssueViewModel = viewModel()) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedLabels = remember { mutableStateListOf<String>() }
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val success by viewModel.success.collectAsState()

    val labelOptions = listOf("bug", "feature", "ui", "high", "medium", "low")

    LaunchedEffect(repoId) {
        viewModel.setRepoId(repoId)
    }

    LaunchedEffect(success) {
        if (success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Quick Issue") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )
            Text("Labels", style = MaterialTheme.typography.titleSmall)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(labelOptions) { label ->
                    FilterChip(
                        selected = selectedLabels.contains(label),
                        onClick = {
                            if (selectedLabels.contains(label)) {
                                selectedLabels.remove(label)
                            } else {
                                selectedLabels.add(label)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.createIssue(title.trim(), description.trim(), selectedLabels.toList()) },
                    enabled = !isLoading && title.isNotBlank()
                ) {
                    Text(if (isLoading) "Creating..." else "Create")
                }
            }
        }
    }

    if (!error.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Error") },
            text = { Text(error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { navController.popBackStack() }) { Text("Close") }
            }
        )
    }
}
