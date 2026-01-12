package com.qa.verifyandtrack.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgeDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.data.model.PullRequest

@Composable
fun PRCard(
    pullRequest: PullRequest,
    onMerge: (PullRequest) -> Unit = {},
    onDeny: (PullRequest) -> Unit = {},
    onResolveConflict: (PullRequest) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(text = pullRequest.title, style = MaterialTheme.typography.titleMedium)
                if (pullRequest.isDraft) {
                    Badge(containerColor = BadgeDefaults.containerColor) {
                        Text("Draft")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${pullRequest.branch} -> ${pullRequest.targetBranch}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (pullRequest.hasConflicts) {
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFF97316))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Conflicts detected", color = Color(0xFFF97316))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onMerge(pullRequest) }) {
                    Icon(Icons.Filled.CallMerge, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Merge")
                }
                TextButton(onClick = { onDeny(pullRequest) }) {
                    Icon(Icons.Filled.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Deny")
                }
                if (pullRequest.hasConflicts) {
                    TextButton(onClick = { onResolveConflict(pullRequest) }) {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resolve")
                    }
                }
            }
        }
    }
}
