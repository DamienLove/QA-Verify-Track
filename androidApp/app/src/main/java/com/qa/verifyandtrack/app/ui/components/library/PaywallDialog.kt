package com.qa.verifyandtrack.app.ui.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.ui.theme.Spacing

@Composable
fun PaywallDialog(
    featureName: String,
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Pro Feature",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Medium)
            ) {
                Text(
                    "$featureName is available in QA Verify Pro.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    Text(
                        "Upgrade to unlock:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text("• AI-powered analysis", style = MaterialTheme.typography.bodyMedium)
                    Text("• Unlimited repositories", style = MaterialTheme.typography.bodyMedium)
                    Text("• Advanced PR operations", style = MaterialTheme.typography.bodyMedium)
                    Text("• Export & reporting", style = MaterialTheme.typography.bodyMedium)
                    Text("• Ad-free experience", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpgrade) {
                Text("Upgrade to Pro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}
