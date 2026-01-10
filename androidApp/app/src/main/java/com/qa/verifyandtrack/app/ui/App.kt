package com.qa.verifyandtrack.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
                    HomeScreen(
                        uiState = uiState,
                        onAddSample = viewModel::saveSampleRepos,
                        onSignOut = viewModel::signOut
                    )
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
private fun HomeScreen(
    uiState: UiState,
    onAddSample: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.user?.email ?: "Signed in") },
                actions = {
                    IconButton(onClick = onAddSample) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Sync sample repos")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Sign out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        RepoList(
            repos = uiState.repos,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun RepoList(
    repos: List<Repo>,
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
                    text = "No repositories saved yet. Tap the refresh icon to add sample repos or save yours from the web app.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            items(repos) { repo ->
                Card(
                    colors = CardDefaults.cardColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(text = repo.displayLabel)
                        Text(text = "Branch: ${repo.branch}")
                    }
                }
            }
        }
    }
}
