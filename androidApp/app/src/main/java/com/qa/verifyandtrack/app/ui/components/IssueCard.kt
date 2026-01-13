package com.qa.verifyandtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.data.model.Issue
import com.qa.verifyandtrack.app.ui.components.library.QACard
import com.qa.verifyandtrack.app.ui.theme.Spacing

@Composable
fun IssueCard(
    issue: Issue,
    onMarkFixed: () -> Unit = {},
    onReopen: () -> Unit = {},
    onBlock: () -> Unit = {},
    onAnalyze: () -> Unit = {},
    canUseAI: Boolean = false
) {
    QACard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.Default)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Spacing.Small))
                Text(
                    text = issue.priority.uppercase(),
                    color = Color.White,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(priorityColor(issue.priority))
                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(Spacing.Small))
            Text(
                text = issue.description.takeIf { it.isNotBlank() } ?: "No description provided.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (issue.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.Small))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    items(issue.labels) { label ->
                        AssistChip(
                            onClick = {},
                            label = { Text(label) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.Default))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                TextButton(onClick = onMarkFixed) {
                    Icon(Icons.Filled.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text("Fixed")
                }
                TextButton(onClick = onReopen) {
                    Icon(Icons.Filled.Replay, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text("Open")
                }
                TextButton(onClick = onBlock) {
                    Icon(Icons.Filled.Block, contentDescription = null)
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text("Block")
                }
                TextButton(onClick = onAnalyze) {
                    Icon(
                        if (canUseAI) Icons.Filled.SmartToy else Icons.Filled.Lock,
                        contentDescription = if (canUseAI) "AI Analysis" else "Pro Feature"
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text("AI")
                }
            }
        }
    }
}

private fun priorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "critical" -> Color(0xFFEF4444)
        "high" -> Color(0xFFF97316)
        "medium" -> Color(0xFFF59E0B)
        "low" -> Color(0xFF10B981)
        else -> Color(0xFF6366F1)
    }
}
