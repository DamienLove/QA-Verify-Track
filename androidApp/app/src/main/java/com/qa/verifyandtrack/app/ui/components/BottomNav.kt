package com.qa.verifyandtrack.app.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.navigation.bottomNavScreens

@Composable
fun BottomNav(
    navController: NavHostController,
    currentRepoId: String? = null,
    onNotesClick: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavScreens.forEach { screen ->
            val selected = currentRoute?.startsWith(screen.route.substringBefore("/")) == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    when (screen) {
                        Screen.Notes -> {
                            onNotesClick()
                        }
                        Screen.Dashboard -> {
                            val route = when {
                                currentRoute?.startsWith("dashboard/") == true -> currentRoute
                                !currentRepoId.isNullOrBlank() -> Screen.Dashboard.createRoute(currentRepoId)
                                else -> Screen.Home.route
                            }
                            navController.navigate(route) {
                                launchSingleTop = true
                            }
                        }
                        else -> {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                            }
                        }
                    }
                },
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.label) } },
                label = { Text(screen.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
