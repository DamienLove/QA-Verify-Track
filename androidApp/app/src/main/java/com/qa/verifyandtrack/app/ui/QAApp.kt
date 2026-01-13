package com.qa.verifyandtrack.app.ui

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.qa.verifyandtrack.app.ui.components.BottomNav
import com.qa.verifyandtrack.app.ui.components.NotesDialog
import com.qa.verifyandtrack.app.ui.navigation.QAAppNavHost
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.navigation.shouldShowBottomNav
import com.qa.verifyandtrack.app.ui.theme.QATheme
import com.qa.verifyandtrack.app.ui.viewmodel.AuthViewModel

@Composable
fun QAApp() {
    QATheme {
        val navController = rememberNavController()
        val authViewModel: AuthViewModel = viewModel()
        val currentUser by authViewModel.currentUser.collectAsState()
        var showNotes by remember { mutableStateOf(false) }

        val startDestination = if (currentUser != null) Screen.Home.route else Screen.Login.route

        LaunchedEffect(currentUser) {
            if (currentUser == null) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }

        Scaffold(
            bottomBar = {
                if (currentUser != null && shouldShowBottomNav(navController)) {
                    BottomNav(
                        navController = navController,
                        onNotesClick = { showNotes = true }
                    )
                }
            }
        ) { paddingValues ->
            QAAppNavHost(
                navController = navController,
                authViewModel = authViewModel,
                startDestination = startDestination,
                modifier = Modifier.padding(paddingValues)
            )
            NotesDialog(showDialog = showNotes, onDismiss = { showNotes = false })
        }
    }
}
