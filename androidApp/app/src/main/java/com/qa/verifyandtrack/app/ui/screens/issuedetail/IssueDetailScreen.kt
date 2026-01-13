package com.qa.verifyandtrack.app.ui.screens.issuedetail

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.data.model.Comment
import com.qa.verifyandtrack.app.ui.components.LoadingIndicator
import com.qa.verifyandtrack.app.ui.theme.Spacing
import com.qa.verifyandtrack.app.ui.viewmodel.IssueDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailScreen(
    navController: NavHostController,
    repoId: String?,
    issueNumber: Int?,
    viewModel: IssueDetailViewModel = viewModel()
) {
    val issue by viewModel.issue.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(repoId, issueNumber) {
        viewModel.loadIssue(repoId, issueNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue #${issueNumber ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                LoadingIndicator("Loading issue...")
            }
            !error.isNullOrBlank() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                }
            }
            issue == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text("Issue not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Default)
                ) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(Spacing.Default)) {
                                Text(
                                    text = issue!!.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(Spacing.Small))
                                Text(
                                    text = "State: ${issue!!.state.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(Spacing.Small))
                                Text(
                                    text = issue!!.description.ifBlank { "No description provided." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    if (issue!!.labels.isNotEmpty()) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                                issue!!.labels.forEach { label ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(label) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium)
                    }

                    if (comments.isEmpty()) {
                        item {
                            Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(comments) { comment ->
                            CommentCard(comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: Comment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.Default)) {
            Text(comment.text.ifBlank { "No comment text." }, style = MaterialTheme.typography.bodyMedium)
            comment.buildNumber?.let { build ->
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text("Build $build", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
