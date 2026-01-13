package com.qa.verifyandtrack.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.qa.verifyandtrack.app.data.model.FeatureGates
import com.qa.verifyandtrack.app.data.model.Repository
import com.qa.verifyandtrack.app.ui.components.EmptyState
import com.qa.verifyandtrack.app.ui.components.LoadingIndicator
import com.qa.verifyandtrack.app.ui.components.library.BannerAd
import com.qa.verifyandtrack.app.ui.components.library.PaywallDialog
import com.qa.verifyandtrack.app.ui.components.library.QACard
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.theme.Spacing
import com.qa.verifyandtrack.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = viewModel()) {
    val repos by viewModel.repos.collectAsState()
    val todos by viewModel.todos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showPaywall by remember { mutableStateOf(false) }
    var paywallFeature by remember { mutableStateOf("") }

    val todoByRepoId = remember(todos) { todos.associateBy { it.repoId } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Repositories")
                        userProfile?.let { profile ->
                            val limit = if (profile.hasUnlimitedRepos) "∞" else "${profile.repoLimit}"
                            Text(
                                "${repos.size}/$limit repos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshTodos() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (FeatureGates.canAddRepo(userProfile, repos.size)) {
                        navController.navigate(Screen.Configuration.route)
                    } else {
                        paywallFeature = "Unlimited Repositories"
                        showPaywall = true
                    }
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add repository")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && repos.isEmpty()) {
                LoadingIndicator("Loading repositories...")
                return@Column
            }

            if (repos.isEmpty()) {
                EmptyState("No repositories configured yet.")
                return@Column
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.Default),
                verticalArrangement = Arrangement.spacedBy(Spacing.Default)
            ) {
                item {
                    Text("Your Repos", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(Spacing.Small))
                }
                items(repos) { repo ->
                    RepoCard(
                        repo = repo,
                        issueCount = todoByRepoId[repo.id]?.openIssueCount,
                        prCount = todoByRepoId[repo.id]?.openPrCount,
                        onClick = { navController.navigate(Screen.Dashboard.createRoute(repo.id)) }
                    )
                }
            }

            // Banner Ad for free users
            if (FeatureGates.shouldShowAds(userProfile)) {
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // Paywall dialog
    if (showPaywall) {
        PaywallDialog(
            featureName = paywallFeature,
            onDismiss = { showPaywall = false },
            onUpgrade = {
                showPaywall = false
                navController.navigate(Screen.Upgrade.route)
            }
        )
    }
}

@Composable
private fun RepoCard(
    repo: Repository,
    issueCount: Int?,
    prCount: Int?,
    onClick: () -> Unit
) {
    QACard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.Default),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!repo.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = repo.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(Spacing.Medium))
            }
            Column {
                Text(
                    repo.displayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    repo.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
                Text(
                    text = "Issues: ${issueCount ?: "—"} • PRs: ${prCount ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
