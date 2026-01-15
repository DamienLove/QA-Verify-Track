package com.qa.verifyandtrack.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.data.model.PullRequest
import com.qa.verifyandtrack.app.ui.components.library.QACard
import com.qa.verifyandtrack.app.ui.theme.Spacing

@Composable
fun PRCard(
    pullRequest: PullRequest,
    selected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {},
    onTitleClick: () -> Unit = {},
    onMerge: () -> Unit = {},
    onDeny: () -> Unit = {},
    onResolveConflict: () -> Unit = {},
    onReadyForReview: () -> Unit = {},
    onReadyAndMerge: () -> Unit = {},
    canMergePR: Boolean = false,
    canDenyPR: Boolean = false,
    canResolve: Boolean = false
) {
    QACard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.Default)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(checked = selected, onCheckedChange = onSelectionChange)
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text(
                        text = pullRequest.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTitleClick() }
                    )
                }
                if (pullRequest.isDraft) {
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("Draft", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = "${pullRequest.branch} -> ${pullRequest.targetBranch}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pullRequest.hasConflicts) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF97316))
                    Spacer(modifier = Modifier.width(Spacing.Small))
                    Text("Conflicts detected", color = Color(0xFFF97316), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Default))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                if (pullRequest.isDraft) {
                    TextButton(onClick = onReadyForReview) {
                        Icon(
                            if (canMergePR) Icons.Filled.Done else Icons.Filled.Lock,
                            contentDescription = if (canMergePR) "Ready for Review" else "Pro Feature"
                        )
                        Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                        Text("Ready")
                    }
                    TextButton(onClick = onReadyAndMerge) {
                        Icon(
                            if (canMergePR) Icons.Filled.DoneAll else Icons.Filled.Lock,
                            contentDescription = if (canMergePR) "Ready and Merge" else "Pro Feature"
                        )
                        Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                        Text("Ready + Merge")
                    }
                }
                TextButton(onClick = onMerge) {
                    Icon(
                        if (canMergePR) Icons.Filled.CallMerge else Icons.Filled.Lock,
                        contentDescription = if (canMergePR) "Merge PR" else "Pro Feature"
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text("Merge")
                }
                TextButton(onClick = onDeny) {
                    Icon(
                        if (canDenyPR) Icons.Filled.Close else Icons.Filled.Lock,
                        contentDescription = if (canDenyPR) "Deny PR" else "Pro Feature"
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text("Deny")
                }
                if (pullRequest.hasConflicts) {
                    TextButton(onClick = onResolveConflict) {
                        Icon(
                            if (canResolve) Icons.Filled.Warning else Icons.Filled.Lock,
                            contentDescription = if (canResolve) "Resolve Conflicts" else "Pro Feature"
                        )
                        Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                        Text("Resolve")
                    }
                }
            }
        }
    }
}
