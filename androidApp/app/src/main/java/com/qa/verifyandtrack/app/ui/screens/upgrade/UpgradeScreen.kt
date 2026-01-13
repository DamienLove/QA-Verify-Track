package com.qa.verifyandtrack.app.ui.screens.upgrade

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.qa.verifyandtrack.app.ui.theme.QABrandGreen
import com.qa.verifyandtrack.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Pro") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            Spacer(modifier = Modifier.height(Spacing.Large))

            Icon(
                Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = QABrandGreen
            )

            Text(
                "Unlock Pro Features",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                "Take your QA workflow to the next level",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.Default))

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                FeatureItem(
                    icon = Icons.Filled.SmartToy,
                    title = "AI-Powered Analysis",
                    description = "Get intelligent insights on issues and bugs"
                )
                FeatureItem(
                    icon = Icons.Filled.Storage,
                    title = "Unlimited Repositories",
                    description = "Track as many projects as you need"
                )
                FeatureItem(
                    icon = Icons.Filled.CallMerge,
                    title = "Advanced PR Operations",
                    description = "Merge, deny, and manage pull requests"
                )
                FeatureItem(
                    icon = Icons.Filled.Download,
                    title = "Export & Reporting",
                    description = "Download data in CSV format"
                )
                FeatureItem(
                    icon = Icons.Filled.Block,
                    title = "Ad-Free Experience",
                    description = "No banner or interstitial ads"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.Large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "$9.99/month",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Cancel anytime",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Button(
                onClick = {
                    // TODO: Implement subscription flow (Google Play Billing)
                    // For now, just show a message
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Upgrade Now", style = MaterialTheme.typography.titleMedium)
            }

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Maybe Later")
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Default)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = QABrandGreen,
            modifier = Modifier.size(24.dp)
        )
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
