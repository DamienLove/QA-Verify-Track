package com.qa.verifyandtrack.app.ui.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.qa.verifyandtrack.app.R
import com.qa.verifyandtrack.app.data.AppContainer
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.viewmodel.AuthUiState
import com.qa.verifyandtrack.app.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val snackState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    val uiState by authViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val webClientId = stringResource(id = R.string.default_web_client_id)
    val googleClient = remember(webClientId) {
        AppContainer.googleSignInClient(context, webClientId)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken
            if (!token.isNullOrBlank()) {
                authViewModel.signInWithGoogle(token)
            } else {
                scope.launch { snackState.showSnackbar("Missing Google ID token.") }
            }
        } catch (e: Exception) {
            scope.launch { snackState.showSnackbar(e.message ?: "Google sign-in failed.") }
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            val message = (uiState as AuthUiState.Error).message
            snackState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("QA Verify & Track") }) },
        snackbarHost = { SnackbarHost(snackState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isRegister) "Create account" else "Welcome back",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isRegister) {
                        authViewModel.signUp(email.trim(), password)
                    } else {
                        authViewModel.signIn(email.trim(), password)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isRegister) "Sign Up" else "Sign In")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { isRegister = !isRegister }) {
                Text(if (isRegister) "Already have an account? Sign in" else "Need an account? Sign up")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { launcher.launch(googleClient.signInIntent) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Continue with Google")
            }
        }
    }
}
