package com.qa.verifyandtrack.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
                                onSignOut = viewModel::signOut,
                                onRepoClick = { repoId ->
                                    navController.navigate("repo/$repoId")
                                }
                            )
                        }
                        composable("repo/{repoId}") { backStackEntry ->
                            val repoId = backStackEntry.arguments?.getString("repoId")
                            val repo = uiState.repos.find { it.id == repoId }
                            if (repo != null) {
                                RepoDetailScreen(
                                    repo = repo,
                                    onBack = { navController.popBackStack() }
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
    onSignOut: () -> Unit,
    onRepoClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.email ?: "Signed in") },
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out")
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
private fun RepoDetailScreen(
    repo: Repo,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

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
                    text = "Repository Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
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
