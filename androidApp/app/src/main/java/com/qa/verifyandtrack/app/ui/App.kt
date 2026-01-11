package com.qa.verifyandtrack.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.qa.verifyandtrack.app.R
import com.qa.verifyandtrack.app.data.model.Repo
import com.qa.verifyandtrack.app.ui.theme.QATheme
import com.qa.verifyandtrack.app.viewmodel.MainViewModel
import com.qa.verifyandtrack.app.viewmodel.UiState
import kotlinx.coroutines.launch

@Composable
fun QAApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = rememberNavController()

    val googleClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) viewModel.signInWithGoogle(account)
        } catch (e: Exception) {
            scope.launch { snackbarHostState.showSnackbar("Google sign-in failed: ${e.localizedMessage}") }
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
    }

    QATheme {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (uiState.user == null) {
                    LoginScreen(
                        uiState = uiState,
                        onLogin = viewModel::signIn,
                        onRegister = viewModel::register,
                        onGoogle = { googleLauncher.launch(googleClient.signInIntent) }
                    )
                } else {
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onAvatarClick = { navController.navigate("profile") },
                                onRepoClick = { repoId ->
                                    navController.navigate("repo/$repoId")
                                }
                            )
                        }
                        composable("profile") {
                            ProfileScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onSignOut = viewModel::signOut,
                                onGlobalSettings = { navController.navigate("globalSettings") }
                            )
                        }
                        composable("globalSettings") {
                            GlobalSettingsScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onSave = viewModel::saveGlobalSettings
                            )
                        }
                        composable("repo/{repoId}") { backStackEntry ->
                            val repoId = backStackEntry.arguments?.getString("repoId")
                            val repo = uiState.repos.find { it.id == repoId }
                            if (repo != null) {
                                RepoDetailScreen(
                                    repo = repo,
                                    globalSettings = uiState.globalSettings,
                                    onBack = { navController.popBackStack() },
                                    onSaveTokenSettings = viewModel::saveRepoTokenSettings
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun LoginScreen(
    uiState: UiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onGoogle: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "QA Verify & Track")
        Text(text = "Sign in to sync repos across devices")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onLogin(email.trim(), password) },
            enabled = !uiState.loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Sign In") }
        OutlinedButton(
            onClick = { onRegister(email.trim(), password) },
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) { Text("Create Account") }
        OutlinedButton(
            onClick = onGoogle,
            enabled = !uiState.loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) { Text("Continue with Google") }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeScreen(
    uiState: UiState,
    onAvatarClick: () -> Unit,
    onRepoClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Repositories") },
                actions = {
                    IconButton(onClick = onAvatarClick) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            val email = uiState.user?.email ?: ""
                            val initial = email.firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        RepoList(
            repos = uiState.repos,
            onRepoClick = onRepoClick,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun RepoList(
    repos: List<Repo>,
    onRepoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        if (repos.isEmpty()) {
            item {
                Text(
                    text = "No repositories saved yet. Use the web app to add and manage your repositories.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(repos) { repo ->
                Card(
                    colors = CardDefaults.cardColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRepoClick(repo.id) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = repo.displayLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Branch: ${repo.branch}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (repo.apps.isNotEmpty()) {
                            Text(
                                text = "${repo.apps.size} app(s) configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileScreen(
    uiState: UiState,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onGlobalSettings: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val email = uiState.user?.email ?: ""
                            val initial = email.firstOrNull()?.uppercase() ?: "U"
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = uiState.user?.email ?: "No email",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = uiState.user?.uid?.take(8) ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item {
                ListItem(
                    headlineContent = { Text("Global Settings") },
                    supportingContent = { Text("Configure GitHub token and defaults") },
                    leadingContent = {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onGlobalSettings() }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                ListItem(
                    headlineContent = { Text("Sign Out") },
                    supportingContent = { Text("Log out of your account") },
                    leadingContent = {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onSignOut() }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Repositories: ${uiState.repos.size}",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GlobalSettingsScreen(
    uiState: UiState,
    onBack: () -> Unit,
    onSave: (com.qa.verifyandtrack.app.data.model.GlobalSettings) -> Unit
) {
    BackHandler(onBack = onBack)

    var githubToken by remember { mutableStateOf(uiState.globalSettings.globalGithubToken ?: "") }
    var defaultBranch by remember { mutableStateOf(uiState.globalSettings.defaultBranch) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "GitHub Configuration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                OutlinedTextField(
                    value = githubToken,
                    onValueChange = { githubToken = it },
                    label = { Text("GitHub Personal Access Token") },
                    supportingText = { Text("This token will be used for all repositories unless overridden") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = defaultBranch,
                    onValueChange = { defaultBranch = it },
                    label = { Text("Default Branch") },
                    supportingText = { Text("Default branch name for new repositories") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                FilledTonalButton(
                    onClick = {
                        onSave(
                            com.qa.verifyandtrack.app.data.model.GlobalSettings(
                                globalGithubToken = githubToken.ifBlank { null },
                                defaultBranch = defaultBranch,
                                theme = uiState.globalSettings.theme
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Settings")
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "About GitHub Token",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Your GitHub Personal Access Token is used to authenticate API requests. It will be stored securely and used for all repositories unless you specify a custom token per repository.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RepoDetailScreen(
    repo: Repo,
    globalSettings: com.qa.verifyandtrack.app.data.model.GlobalSettings,
    onBack: () -> Unit,
    onSaveTokenSettings: (String, Boolean, String?) -> Unit
) {
    BackHandler(onBack = onBack)

    var useCustomToken by remember(repo.id) { mutableStateOf(repo.useCustomToken) }
    var customToken by remember(repo.id) { mutableStateOf(repo.githubToken ?: "") }

    LaunchedEffect(repo.useCustomToken, repo.githubToken) {
        useCustomToken = repo.useCustomToken
        customToken = repo.githubToken.orEmpty()
    }

    val normalizedCustomToken = customToken.trim()
    val globalToken = globalSettings.globalGithubToken?.takeIf { it.isNotBlank() }
    val effectiveToken = if (useCustomToken) {
        normalizedCustomToken.ifBlank { null }
    } else {
        globalToken
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(repo.displayLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "GitHub PAT",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Main PAT",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Shared across repositories unless a custom token is enabled",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (globalToken != null) {
                                "Token: ${globalToken.take(4)}...${globalToken.takeLast(4)}"
                            } else {
                                "No main PAT configured"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use custom PAT for this repository",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Disable to use your main PAT",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = useCustomToken,
                        onCheckedChange = { useCustomToken = it }
                    )
                }
            }

            if (useCustomToken) {
                item {
                    OutlinedTextField(
                        value = customToken,
                        onValueChange = { customToken = it },
                        label = { Text("Custom GitHub PAT") },
                        supportingText = { Text("Only used for this repository") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        onSaveTokenSettings(
                            repo.id,
                            useCustomToken,
                            normalizedCustomToken.ifBlank { null }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Token Settings")
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (useCustomToken) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer        
                        }
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = if (useCustomToken) "Using Custom Token" else "Using Main Token",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (useCustomToken) {
                                "This repository uses its own token"
                            } else {
                                "This repository uses your main GitHub token"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (effectiveToken != null) {
                                "Token: ${effectiveToken.take(4)}...${effectiveToken.takeLast(4)}"
                            } else {
                                "No token configured"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (!useCustomToken && globalToken == null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Configure main PAT in settings",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Repository Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                DetailItem(label = "Owner", value = repo.owner)
            }

            item {
                DetailItem(label = "Name", value = repo.name)
            }

            item {
                DetailItem(label = "Branch", value = repo.branch)
            }

            item {
                DetailItem(label = "Connected", value = if (repo.isConnected) "Yes" else "No")
            }

            if (repo.apiEndpoint != null) {
                item {
                    DetailItem(label = "API Endpoint", value = repo.apiEndpoint)
                }
            }

            if (repo.apps.isNotEmpty()) {
                item {
                    Text(
                        text = "Apps (${repo.apps.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(repo.apps.size) { index ->
                    val app = repo.apps[index]
                    Card(
                        colors = CardDefaults.cardColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Platform: ${app.platform}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Build: ${app.buildNumber}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            app.playStoreUrl?.let { url ->
                                Text(
                                    text = "Play Store: $url",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (repo.projects.isNotEmpty()) {
                item {
                    Text(
                        text = "Projects (${repo.projects.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(repo.projects.size) { index ->
                    Text(
                        text = "• ${repo.projects[index]}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
