package com.qa.verifyandtrack.app.ui.screens.dashboard

import android.content.Intent
import android.net.Uri
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
import com.qa.verifyandtrack.app.data.AppPreferences
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
    val projectUrls = repo?.projects?.filter { it.isNotBlank() } ?: emptyList()

    var buildMenuExpanded by remember { mutableStateOf(false) }
    var issueFilterMenuExpanded by remember { mutableStateOf(false) }
    var issueFilter by remember { mutableStateOf(IssueFilterOption.QA_ISSUES) }
    var selectedPrNumbers by remember { mutableStateOf(setOf<Int>()) }
    var showBulkActions by remember { mutableStateOf(false) }
    val selectedPrs = pullRequests.filter { selectedPrNumbers.contains(it.number) }
    val hasSelection = selectedPrNumbers.isNotEmpty()
    val context = LocalContext.current
    val activity = LocalOnBackPressedDispatcherOwner.current as? androidx.activity.ComponentActivity
    val adService = remember { activity?.let { AdService(it) } }

    LaunchedEffect(repoId) {
        viewModel.setRepoId(repoId)
    }

    LaunchedEffect(hasSelection) {
        if (hasSelection) {
            showBulkActions = true
        }
    }

    LaunchedEffect(pullRequests) {
        val available = pullRequests.map { it.number }.toSet()
        selectedPrNumbers = selectedPrNumbers.intersect(available)
        if (selectedPrNumbers.isEmpty()) {
            showBulkActions = false
        }
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

                if (projectUrls.isNotEmpty()) {
                    item {
                        QACard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.Default),
                                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                            ) {
                                Text("Projects", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (projectUrls.size == 1) "1 project configured" else "${projectUrls.size} projects configured",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        val projectUrl = projectUrls.firstOrNull().orEmpty()
                                        if (projectUrl.isNotBlank()) {
                                            navController.navigate(Screen.ProjectWebView.createRoute(projectUrl))
                                        } else {
                                            viewModel.showError("Project URL is missing.")
                                        }
                                    }
                                ) {
                                    Text("Open Project")
                                }
                            }
                        }
                    }
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
                                issueFilterMenuExpanded = true
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

                    DropdownMenu(expanded = issueFilterMenuExpanded, onDismissRequest = { issueFilterMenuExpanded = false }) {
                        IssueFilterOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    issueFilterMenuExpanded = false
                                    if (option.opensInGitHub) {
                                        val currentRepo = repo
                                        if (currentRepo == null) {
                                            viewModel.showError("Repository not loaded yet.")
                                        } else {
                                            openGitHubIssues(context, currentRepo.owner, currentRepo.name, option.githubState)
                                        }
                                    } else {
                                        issueFilter = option
                                    }
                                }
                            )
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
                            val qaFiltered = filterIssuesForBuild(issues, selectedBuild, issueBuildMap, issueVerifyFixMap)
                            val noBuildFiltered = issues.filter { issue ->
                                issueBuildMap[issue.number] == null && issueVerifyFixMap[issue.number] == null
                            }
                            val filtered = when (issueFilter) {
                                IssueFilterOption.QA_ISSUES -> qaFiltered
                                IssueFilterOption.ALL_OPEN -> issues
                                IssueFilterOption.NO_BUILD_NUMBERS -> noBuildFiltered
                                IssueFilterOption.ALL_ISSUES -> qaFiltered
                                IssueFilterOption.CLOSED_ISSUES -> qaFiltered
                            }
                            val emptyMessage = when (issueFilter) {
                                IssueFilterOption.QA_ISSUES -> if (issues.isEmpty()) "No open issues found." else "No issues match this build."
                                IssueFilterOption.ALL_OPEN -> "No open issues found."
                                IssueFilterOption.NO_BUILD_NUMBERS -> "No open issues missing build numbers."
                                IssueFilterOption.ALL_ISSUES -> "No open issues found."
                                IssueFilterOption.CLOSED_ISSUES -> "No open issues found."
                            }
                            if (filtered.isEmpty()) {
                                item { EmptyState(emptyMessage) }
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
                                        selected = selectedPrNumbers.contains(pr.number),
                                        onSelectionChange = { selected ->
                                            selectedPrNumbers = if (selected) {
                                                selectedPrNumbers + pr.number
                                            } else {
                                                selectedPrNumbers - pr.number
                                            }
                                        },
                                        onTitleClick = {
                                            val routeRepoId = repo?.id ?: repoId
                                            if (!routeRepoId.isNullOrBlank()) {
                                                navController.navigate(Screen.PullRequestDetail.createRoute(routeRepoId, pr.number))
                                            } else {
                                                viewModel.showError("Missing repository ID.")
                                            }
                                        },
                                        onMerge = {
                                            if (FeatureGates.canMergePR(userProfile)) {
                                                val deleteBranches = AppPreferences.isDeleteBranchesEnabled(context)
                                                viewModel.mergePR(pr, deleteBranches)
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
                                        onReadyAndMerge = {
                                            if (FeatureGates.canMergePR(userProfile)) {
                                                val deleteBranches = AppPreferences.isDeleteBranchesEnabled(context)
                                                viewModel.readyAndMergePR(pr, deleteBranches)
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

    if (showBulkActions && hasSelection) {
        ModalBottomSheet(
            onDismissRequest = { showBulkActions = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.Default),
                verticalArrangement = Arrangement.spacedBy(Spacing.Small)
            ) {
                Text("PR Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${selectedPrs.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Button(onClick = {
                    if (FeatureGates.canMergePR(userProfile)) {
                        val deleteBranches = AppPreferences.isDeleteBranchesEnabled(context)
                        viewModel.readyAndMergeSelectedPRs(selectedPrs, deleteBranches)
                        selectedPrNumbers = emptySet()
                        showBulkActions = false
                    } else {
                        viewModel.showPaywallFor("Merge Pull Requests")
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ready + Merge")
                }

                OutlinedButton(onClick = {
                    if (FeatureGates.canMergePR(userProfile)) {
                        viewModel.readySelectedPRs(selectedPrs)
                        selectedPrNumbers = emptySet()
                        showBulkActions = false
                    } else {
                        viewModel.showPaywallFor("Merge Pull Requests")
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ready")
                }

                OutlinedButton(onClick = {
                    if (FeatureGates.canMergePR(userProfile)) {
                        val deleteBranches = AppPreferences.isDeleteBranchesEnabled(context)
                        viewModel.mergeSelectedPRs(selectedPrs, deleteBranches)
                        selectedPrNumbers = emptySet()
                        showBulkActions = false
                    } else {
                        viewModel.showPaywallFor("Merge Pull Requests")
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Merge")
                }

                OutlinedButton(onClick = {
                    if (FeatureGates.canDenyPR(userProfile)) {
                        viewModel.denySelectedPRs(selectedPrs)
                        selectedPrNumbers = emptySet()
                        showBulkActions = false
                    } else {
                        viewModel.showPaywallFor("Close Pull Requests")
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Deny")
                }

                TextButton(onClick = {
                    selectedPrNumbers = emptySet()
                    showBulkActions = false
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear Selection")
                }
            }
        }
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

private enum class IssueFilterOption(
    val label: String,
    val opensInGitHub: Boolean = false,
    val githubState: String = "open"
) {
    QA_ISSUES("QA Issues"),
    ALL_OPEN("All Open Issues"),
    NO_BUILD_NUMBERS("Issues w/o Build Numbers"),
    ALL_ISSUES("All Issues (GitHub)", opensInGitHub = true, githubState = "all"),
    CLOSED_ISSUES("Closed Issues (GitHub)", opensInGitHub = true, githubState = "closed")
}

private fun openGitHubIssues(context: android.content.Context, owner: String, repo: String, state: String) {
    val query = when (state.lowercase()) {
        "closed" -> "is%3Aissue+is%3Aclosed"
        "all" -> "is%3Aissue+is%3Aall"
        else -> "is%3Aissue+is%3Aopen"
    }
    val url = "https://github.com/$owner/$repo/issues?q=$query"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}
