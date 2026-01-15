package com.qa.verifyandtrack.app.ui.screens.prdetail

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.data.model.Comment
import com.qa.verifyandtrack.app.data.model.PullRequestDetail
import com.qa.verifyandtrack.app.data.model.PullRequestFile
import com.qa.verifyandtrack.app.ui.components.LoadingIndicator
import com.qa.verifyandtrack.app.ui.theme.Spacing
import com.qa.verifyandtrack.app.ui.viewmodel.PullRequestDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRequestDetailScreen(
    navController: NavHostController,
    repoId: String?,
    pullNumber: Int?,
    viewModel: PullRequestDetailViewModel = viewModel()
) {
    val pullRequest by viewModel.pullRequest.collectAsState()
    val files by viewModel.files.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isCommenting by viewModel.isCommenting.collectAsState()
    val commentError by viewModel.commentError.collectAsState()
    val commentSuccess by viewModel.commentSuccess.collectAsState()
    var commentDraft by remember { mutableStateOf("") }

    LaunchedEffect(repoId, pullNumber) {
        viewModel.loadPullRequest(repoId, pullNumber)
    }

    LaunchedEffect(commentSuccess) {
        if (commentSuccess) {
            commentDraft = ""
            viewModel.clearCommentSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PR #${pullNumber ?: ""}") },
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
                LoadingIndicator("Loading pull request...")
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
            pullRequest == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text("Pull request not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        PullRequestSummaryCard(pullRequest!!)
                    }

                    item {
                        Text("Files Changed (${files.size})", style = MaterialTheme.typography.titleMedium)
                    }

                    if (files.isEmpty()) {
                        item { Text("No file changes found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(files) { file ->
                            FileChangeCard(file)
                        }
                    }

                    item {
                        Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium)
                    }

                    if (comments.isEmpty()) {
                        item { Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(comments) { comment ->
                            CommentCard(comment)
                        }
                    }

                    item {
                        Text("Add Comment", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = commentDraft,
                            onValueChange = { commentDraft = it },
                            label = { Text("Comment") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { viewModel.addComment(commentDraft) },
                                enabled = !isCommenting && commentDraft.isNotBlank()
                            ) {
                                Text(if (isCommenting) "Posting..." else "Post")
                            }
                        }
                        if (!commentError.isNullOrBlank()) {
                            Text(
                                commentError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PullRequestSummaryCard(detail: PullRequestDetail) {
    val mergeableState = detail.mergeableState?.lowercase().orEmpty()
    val hasConflicts = detail.mergeable == false || mergeableState == "dirty" || mergeableState == "conflicting"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.Default)) {
            Text(detail.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = if (detail.isDraft) "Status: Draft" else "Status: Ready",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = "Mergeable: ${detail.mergeable?.toString() ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (detail.mergeableState != null) {
                Text(
                    text = "Mergeable state: ${detail.mergeableState}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = "Changed files: ${detail.changedFiles}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (hasConflicts) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Row {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF97316))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text("Conflicts detected", color = Color(0xFFF97316), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FileChangeCard(file: PullRequestFile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.Default)) {
            Text(file.filename, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = "Status: ${file.status.ifBlank { "unknown" }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
            Text(
                text = "+${file.additions}  -${file.deletions}  (${file.changes} changes)",
                style = MaterialTheme.typography.labelSmall
            )
            file.previousFilename?.takeIf { it.isNotBlank() }?.let { previous ->
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text("Renamed from $previous", style = MaterialTheme.typography.labelSmall)
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
