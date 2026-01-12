package com.qa.verifyandtrack.app.ui.screens.dashboard

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.components.IssueCard
import com.qa.verifyandtrack.app.ui.components.LoadingIndicator
import com.qa.verifyandtrack.app.ui.components.PRCard
import com.qa.verifyandtrack.app.ui.viewmodel.DashboardTab
import com.qa.verifyandtrack.app.ui.viewmodel.DashboardViewModel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, repoId: String?, viewModel: DashboardViewModel = viewModel()) {
    val repo by viewModel.repo.collectAsState()
    val issues by viewModel.issues.collectAsState()
    val pullRequests by viewModel.pullRequests.collectAsState()
    val selectedBuild by viewModel.selectedBuild.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()

    var buildMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(repoId) {
        viewModel.setRepoId(repoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo?.displayLabel ?: "Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.syncGitHub() }) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = selectedBuild ?: "",
                    onValueChange = { viewModel.selectBuild(it) },
                    label = { Text("Target Build") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { buildMenuExpanded = true }) {
                            Icon(Icons.Filled.ArrowDropDown, "Builds")
                        }
                    },
                    singleLine = true
                )
                
                DropdownMenu(expanded = buildMenuExpanded, onDismissRequest = { buildMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Clear") }, onClick = {
                        viewModel.selectBuild(null)
                        buildMenuExpanded = false
                    })
                    repo?.apps?.map { it.buildNumber }?.distinct()?.forEach { build ->
                        DropdownMenuItem(text = { Text(build) }, onClick = {
                            viewModel.selectBuild(build)
                            buildMenuExpanded = false
                        })
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TabRow(selectedTabIndex = if (activeTab == DashboardTab.Issues) 0 else 1) {
                Tab(
                    selected = activeTab == DashboardTab.Issues,
                    onClick = { viewModel.setActiveTab(DashboardTab.Issues) },
                    text = { Text("Issues (${issues.size})") }
                )
                Tab(
                    selected = activeTab == DashboardTab.PullRequests,
                    onClick = { viewModel.setActiveTab(DashboardTab.PullRequests) },
                    text = { Text("PRs (${pullRequests.size})") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                LoadingIndicator("Syncing GitHub...")
            } else {
                when (activeTab) {
                    DashboardTab.Issues -> {
                        val filtered = if (selectedBuild.isNullOrBlank()) {
                            issues
                        } else {
                            issues.filter { it.description.contains(selectedBuild.orEmpty(), ignoreCase = true) }
                        }
                        if (filtered.isEmpty()) {
                            EmptyState(if (issues.isEmpty()) "No open issues found." else "No issues match this build.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(filtered) { issue ->
                                    IssueCard(
                                        issue = issue,
                                        onMarkFixed = { viewModel.markIssueFixed(issue.number, selectedBuild ?: "") },
                                        onReopen = { viewModel.markIssueOpen(issue.number) },
                                        onBlock = { viewModel.blockIssue(issue.number, "Blocked from dashboard") },
                                        onAnalyze = { viewModel.analyzeIssue(issue) }
                                    )
                                }
                            }
                        }
                    }
                    DashboardTab.PullRequests -> {
                        if (pullRequests.isEmpty()) {
                            EmptyState("No pull requests found.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(pullRequests) { pr ->
                                    PRCard(
                                        pullRequest = pr,
                                        onMerge = { viewModel.mergePR(pr.number) },
                                        onDeny = { viewModel.denyPR(pr.number) },
                                        onResolveConflict = { viewModel.resolveConflict(pr.number) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (!analysisResult.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAnalysis() },
            title = { Text("AI Analysis") },
            text = { Text(analysisResult.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAnalysis() }) { Text("Close") }
            }
        )
    }
}
