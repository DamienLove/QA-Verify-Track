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
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.BugReport
import androidx.compose.material3.icons.filled.Close
import androidx.compose.material3.icons.filled.Done
import androidx.compose.material3.icons.filled.SmartToy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.data.model.Issue

@Composable
fun IssueCard(
    issue: Issue,
    onMarkFixed: (Issue) -> Unit = {},
    onReopen: (Issue) -> Unit = {},
    onBlock: (Issue) -> Unit = {},
    onAnalyze: (Issue) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.priority.uppercase(),
                    color = Color.White,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(priorityColor(issue.priority))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = issue.description.takeIf { it.isNotBlank() } ?: "No description provided.",
                style = MaterialTheme.typography.bodyMedium
            )
            if (issue.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onMarkFixed(issue) }) {
                    Icon(Icons.Filled.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Fixed")
                }
                TextButton(onClick = { onReopen(issue) }) {
                    Icon(Icons.Filled.BugReport, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reopen")
                }
                TextButton(onClick = { onBlock(issue) }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Block")
                }
                TextButton(onClick = { onAnalyze(issue) }) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
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
