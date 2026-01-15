package com.qa.verifyandtrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import android.net.Uri

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Login : Screen("login", "Login")
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Notes : Screen("notes", "Notes", Icons.Filled.Edit)
    data object Configuration : Screen("configuration", "Config", Icons.Filled.Settings)
    data object RepoDetail : Screen("repoDetail/{repoId}", "Repo Detail") {
        fun createRoute(repoId: String) = "repoDetail/$repoId"
    }
    data object Dashboard : Screen("dashboard/{repoId}", "Dashboard", Icons.Filled.Dashboard) {
        fun createRoute(repoId: String) = "dashboard/$repoId"
    }
    data object IssueDetail : Screen("issueDetail/{repoId}/{issueNumber}", "Issue Detail") {
        fun createRoute(repoId: String, issueNumber: Int) = "issueDetail/$repoId/$issueNumber"
    }
    data object PullRequestDetail : Screen("pullRequestDetail/{repoId}/{pullNumber}", "PR Detail") {
        fun createRoute(repoId: String, pullNumber: Int) = "pullRequestDetail/$repoId/$pullNumber"
    }
    data object ProjectWebView : Screen("projectWebView/{encodedUrl}", "Project") {
        fun createRoute(url: String): String = "projectWebView/${Uri.encode(url)}"
    }
    data object QuickIssue : Screen("quickIssue/{repoId}?build={build}", "Quick Issue", Icons.Filled.Build) {
        fun createRoute(repoId: String, build: String? = null): String {
            val trimmed = build?.trim().orEmpty()
            val encoded = if (trimmed.isNotBlank()) Uri.encode(trimmed) else ""
            return if (encoded.isNotBlank()) "quickIssue/$repoId?build=$encoded" else "quickIssue/$repoId"
        }
    }
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    data object Upgrade : Screen("upgrade", "Upgrade", Icons.Filled.Star)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Notes,
    Screen.Configuration,
    Screen.Dashboard
)
