package com.qa.verifyandtrack.app.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.BuildConfig
import com.qa.verifyandtrack.app.data.AppPreferences
import com.qa.verifyandtrack.app.data.model.SubscriptionTier
import com.qa.verifyandtrack.app.ui.components.library.QAButton
import com.qa.verifyandtrack.app.ui.components.library.QACard
import com.qa.verifyandtrack.app.ui.navigation.Screen
import com.qa.verifyandtrack.app.ui.theme.QABrandGreen
import com.qa.verifyandtrack.app.ui.theme.Spacing
import com.qa.verifyandtrack.app.ui.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavHostController, viewModel: ProfileViewModel = viewModel()) {
    val userProfile by viewModel.userProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var deleteBranchesEnabled by remember { mutableStateOf(AppPreferences.isDeleteBranchesEnabled(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.Default),
            verticalArrangement = Arrangement.spacedBy(Spacing.Default)
        ) {
            // User Info Card
            QACard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Large),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Default),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Subscription Card
            QACard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Subscription",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            color = if (userProfile?.tier == SubscriptionTier.PRO) QABrandGreen else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = userProfile?.tier?.name ?: "FREE",
                                modifier = Modifier.padding(horizontal = Spacing.Default, vertical = Spacing.ExtraSmall),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (userProfile?.tier == SubscriptionTier.FREE) {
                        Text(
                            "Upgrade to Pro for unlimited repos, AI analysis, and ad-free experience",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        QAButton(
                            text = "Upgrade to Pro",
                            onClick = { navController.navigate(Screen.Upgrade.route) },
                            icon = Icons.Filled.Star,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        userProfile?.subscriptionExpiresAt?.let { expiresAt ->
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            val expiryDate = dateFormat.format(Date(expiresAt))
                            Text(
                                "Active until $expiryDate",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            "Active subscription",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.Small))
                        OutlinedButton(
                            onClick = { /* TODO: Manage subscription */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Subscription")
                        }
                    }
                }
            }

            // Settings Card
            QACard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Default)
                ) {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Theme Setting
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Default),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Palette, contentDescription = null)
                            Column {
                                Text(
                                    "Theme",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (themeMode) {
                                        "light" -> "Light"
                                        "dark" -> "Dark"
                                        else -> "System"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { showThemeDialog = true }) {
                            Text("Change")
                        }
                    }

                    Divider()

                    // Delete merged branches setting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Default),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null)
                            Column {
                                Text(
                                    "Delete merged branches",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Delete branches used in a single PR",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = deleteBranchesEnabled,
                            onCheckedChange = { enabled ->
                                deleteBranchesEnabled = enabled
                                AppPreferences.setDeleteBranchesEnabled(context, enabled)
                            }
                        )
                    }

                    Divider()

                    // Repository Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Default),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Storage, contentDescription = null)
                            Column {
                                Text(
                                    "Repositories",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (userProfile?.hasUnlimitedRepos == true) "Unlimited" else "Up to ${userProfile?.repoLimit ?: 2}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // About Card
            QACard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Default)
                ) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Default),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null)
                            Text(
                                "Version",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sign Out Button
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(Spacing.Large))
        }
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            viewModel.setThemeMode("system")
                            showThemeDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("System Default", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            viewModel.setThemeMode("light")
                            showThemeDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Light", modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            viewModel.setThemeMode("dark")
                            showThemeDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dark", modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.signOut()
                        showSignOutDialog = false
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
