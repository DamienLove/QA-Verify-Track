package com.qa.verifyandtrack.app.ui.screens.dashboard

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.data.model.FeatureGates
import com.qa.verifyandtrack.app.data.service.AdService
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.components.IssueCard
import com.qa.verifyandtrack.app.ui.components.LoadingIndicator
import com.qa.verifyandtrack.app.ui.components.PRCard
import com.qa.verifyandtrack.app.ui.components.library.BannerAd
import com.qa.verifyandtrack.app.ui.components.library.PaywallDialog
import com.qa.verifyandtrack.app.ui.components.library.QACard
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.theme.*
import com.qa.verifyandtrack.app.ui.viewmodel.DashboardTab
import com.qa.verifyandtrack.app.ui.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavHostController, repoId: String?, viewModel: DashboardViewModel = viewModel()) {
    val repo by viewModel.repo.collectAsState()
    val issues by viewModel.issues.collectAsState()
    val issueBuildMap by viewModel.issueBuildMap.collectAsState()
    val issueVerifyFixMap by viewModel.issueVerifyFixMap.collectAsState()
    val pullRequests by viewModel.pullRequests.collectAsState()
    val selectedBuild by viewModel.selectedBuild.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val showPaywall by viewModel.showPaywall.collectAsState()
    val error by viewModel.error.collectAsState()

    var buildMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = LocalOnBackPressedDispatcherOwner.current as? androidx.activity.ComponentActivity
    val adService = remember { activity?.let { AdService(it) } }

    LaunchedEffect(repoId) {
        viewModel.setRepoId(repoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo?.displayLabel ?: "Dashboard") },
                actions = {
                    // Export button (Pro only)
                    IconButton(onClick = {
                        if (FeatureGates.canExportData(userProfile)) {
                            viewModel.exportData(context)
                        } else {
                            viewModel.showPaywallFor("Export & Reporting")
                        }
                    }) {
                        Icon(
                            if (FeatureGates.canExportData(userProfile)) Icons.Filled.Download else Icons.Filled.Lock,
                            contentDescription = "Export"
                        )
                    }

                    IconButton(onClick = { viewModel.syncGitHub() }) {
                        Icon(Icons.Filled.Sync, contentDescription = "Sync")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    repoId?.let { id ->
                        navController.navigate(Screen.QuickIssue.createRoute(id, selectedBuild))
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create Issue")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.Default),
                verticalArrangement = Arrangement.spacedBy(Spacing.Default)
            ) {
                // Stats Card
                item {
                    StatsCard(
                        totalIssues = issues.size,
                        openIssues = issues.count { it.state == "open" },
                        pullRequests = pullRequests.size
                    )
                }

                // Build filter + Advanced Filters button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
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

                        // Advanced Filters button (Pro only)
                        IconButton(onClick = {
                            if (FeatureGates.canAccessAdvancedFilters(userProfile)) {
                                viewModel.showAdvancedFilters()
                            } else {
                                viewModel.showPaywallFor("Advanced Filters")
                            }
                        }) {
                            Icon(
                                if (FeatureGates.canAccessAdvancedFilters(userProfile)) Icons.Filled.FilterList else Icons.Filled.Lock,
                                contentDescription = "Advanced Filters"
                            )
                        }
                    }

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

                // Tabs
                item {
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
                }

                // Content
                if (isLoading) {
                    item { LoadingIndicator("Syncing GitHub...") }
                } else {
                    when (activeTab) {
                        DashboardTab.Issues -> {
                            val filtered = filterIssuesForBuild(issues, selectedBuild, issueBuildMap, issueVerifyFixMap)
                            if (filtered.isEmpty()) {
                                item { EmptyState(if (issues.isEmpty()) "No open issues found." else "No issues match this build.") }
                            } else {
                                items(filtered) { issue ->
                                    IssueCard(
                                        issue = issue,
                                        onMarkFixed = {
                                            viewModel.markIssueFixed(issue.number, selectedBuild ?: "")
                                            viewModel.onIssueAction(activity, adService)
                                        },
                                        onReopen = {
                                            viewModel.markIssueOpen(issue.number, selectedBuild ?: "")
                                            viewModel.onIssueAction(activity, adService)
                                        },
                                        onBlock = {
                                            viewModel.blockIssue(issue.number, "Blocked from dashboard", selectedBuild ?: "")
                                            viewModel.onIssueAction(activity, adService)
                                        },
                                        onAnalyze = {
                                            if (FeatureGates.canUseAIAnalysis(userProfile)) {
                                                viewModel.analyzeIssue(issue)
                                            } else {
                                                viewModel.showPaywallFor("AI-Powered Analysis")
                                            }
                                        },
                                        onIssueClick = {
                                            val routeRepoId = repo?.id ?: repoId
                                            if (!routeRepoId.isNullOrBlank()) {
                                                navController.navigate(Screen.IssueDetail.createRoute(routeRepoId, issue.number))
                                            } else {
                                                viewModel.showError("Missing repository ID.")
                                            }
                                        },
                                        canUseAI = FeatureGates.canUseAIAnalysis(userProfile)
                                    )
                                }
                            }
                        }
                        DashboardTab.PullRequests -> {
                            if (pullRequests.isEmpty()) {
                                item { EmptyState("No pull requests found.") }
                            } else {
                                items(pullRequests) { pr ->
                                    PRCard(
                                        pullRequest = pr,
                                        onMerge = {
                                            if (FeatureGates.canMergePR(userProfile)) {
                                                if (pr.isDraft) {
                                                    viewModel.showError("Draft PRs cannot be merged. Tap Ready first.")
                                                } else {
                                                    viewModel.mergePR(pr.number)
                                                }
                                            } else {
                                                viewModel.showPaywallFor("Merge Pull Requests")
                                            }
                                        },
                                        onReadyForReview = {
                                            if (FeatureGates.canMergePR(userProfile)) {
                                                viewModel.markReadyForReview(pr.number)
                                            } else {
                                                viewModel.showPaywallFor("Merge Pull Requests")
                                            }
                                        },
                                        onDeny = {
                                            if (FeatureGates.canDenyPR(userProfile)) {
                                                viewModel.denyPR(pr.number)
                                            } else {
                                                viewModel.showPaywallFor("Close Pull Requests")
                                            }
                                        },
                                        onResolveConflict = {
                                            if (FeatureGates.canResolveConflicts(userProfile)) {
                                                viewModel.resolveConflict(pr.number)
                                            } else {
                                                viewModel.showPaywallFor("Resolve PR Conflicts")
                                            }
                                        },
                                        canMergePR = FeatureGates.canMergePR(userProfile),
                                        canDenyPR = FeatureGates.canDenyPR(userProfile),
                                        canResolve = FeatureGates.canResolveConflicts(userProfile)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Banner Ad for free users
            if (FeatureGates.shouldShowAds(userProfile)) {
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // AI Analysis Dialog
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

    // Paywall Dialog
    showPaywall?.let { featureName ->
        PaywallDialog(
            featureName = featureName,
            onDismiss = { viewModel.dismissPaywall() },
            onUpgrade = {
                viewModel.dismissPaywall()
                navController.navigate(Screen.Upgrade.route)
            }
        )
    }

    if (!error.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("Close") }
            }
        )
    }
}

@Composable
fun StatsCard(totalIssues: Int, openIssues: Int, pullRequests: Int) {
    QACard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Default),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Total", totalIssues.toString(), MaterialTheme.colorScheme.primary)
            StatItem("Open", openIssues.toString(), StatusOpen)
            StatItem("PRs", pullRequests.toString(), MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun parseBuildNumber(value: String?): Int? {
    if (value.isNullOrBlank()) return null
    val match = Regex("(\\d+)").find(value) ?: return null
    return match.value.toIntOrNull()
}

private fun filterIssuesForBuild(
    issues: List<com.qa.verifyandtrack.app.data.model.Issue>,
    build: String?,
    buildMap: Map<Int, Int>,
    verifyFixMap: Map<Int, Int>
): List<com.qa.verifyandtrack.app.data.model.Issue> {
    val targetBuild = parseBuildNumber(build)
    return issues.filter { issue ->
        val verifyBuild = verifyFixMap[issue.number]
        val statusBuild = buildMap[issue.number]
        if (targetBuild == null) {
            return@filter verifyBuild != null || statusBuild == null
        }
        if (verifyBuild != null && verifyBuild <= targetBuild) {
            return@filter true
        }
        if (statusBuild != null) {
            return@filter false
        }
        true
    }
}
