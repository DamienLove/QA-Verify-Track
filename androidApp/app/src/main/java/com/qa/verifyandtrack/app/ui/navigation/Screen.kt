package com.qa.verifyandtrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Login : Screen("login", "Login")
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Configuration : Screen("configuration", "Config", Icons.Filled.Settings)
    data object Dashboard : Screen("dashboard/{repoId}", "Dashboard", Icons.Filled.Dashboard) {
        fun createRoute(repoId: String) = "dashboard/$repoId"
    }
    data object QuickIssue : Screen("quickIssue/{repoId}", "Quick Issue", Icons.Filled.Build) {
        fun createRoute(repoId: String) = "quickIssue/$repoId"
    }
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Configuration,
    Screen.Dashboard
)
