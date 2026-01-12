package com.qa.verifyandtrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.qa.verifyandtrack.app.ui.screens.auth.LoginScreen
import com.qa.verifyandtrack.app.ui.screens.config.ConfigurationScreen
import com.qa.verifyandtrack.app.ui.screens.dashboard.DashboardScreen
import com.qa.verifyandtrack.app.ui.screens.home.HomeScreen
import com.qa.verifyandtrack.app.ui.screens.quickissue.QuickIssueScreen
import com.qa.verifyandtrack.app.ui.viewmodel.AuthViewModel

@Composable
fun QAAppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Configuration.route) {
            ConfigurationScreen(navController = navController)
        }
        composable(
            route = Screen.Dashboard.route,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoId = backStackEntry.arguments?.getString("repoId")
            DashboardScreen(navController = navController, repoId = repoId)
        }
        composable(
            route = Screen.QuickIssue.route,
            arguments = listOf(navArgument("repoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val repoId = backStackEntry.arguments?.getString("repoId")
            QuickIssueScreen(navController = navController, repoId = repoId)
        }
    }
}

@Composable
fun shouldShowBottomNav(navController: NavHostController): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    return route != Screen.Login.route
}
