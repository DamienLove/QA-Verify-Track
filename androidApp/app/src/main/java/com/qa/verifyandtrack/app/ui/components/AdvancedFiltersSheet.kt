package com.qa.verifyandtrack.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.ui.theme.Spacing

data class AdvancedFilters(
    val assignee: String = "",
    val priorities: Set<String> = emptySet(),
    val labels: Set<String> = emptySet(),
    val issueTypes: Set<String> = emptySet(),
    val dateFrom: Long? = null,
    val dateTo: Long? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedFiltersSheet(
    initialFilters: AdvancedFilters = AdvancedFilters(),
    availableLabels: List<String> = emptyList(),
    onApply: (AdvancedFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var assignee by remember { mutableStateOf(initialFilters.assignee) }
    var selectedPriorities by remember { mutableStateOf(initialFilters.priorities) }
    var selectedLabels by remember { mutableStateOf(initialFilters.labels) }
    var selectedIssueTypes by remember { mutableStateOf(initialFilters.issueTypes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.Default)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Advanced Filters",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            // Assignee Filter
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(
                    "Assignee",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = assignee,
                    onValueChange = { assignee = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter GitHub username") },
                    singleLine = true
                )
            }

            // Priority Filter
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(
                    "Priority",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    listOf("critical", "high", "medium", "low").forEach { priority ->
                        FilterChip(
                            selected = priority in selectedPriorities,
                            onClick = {
                                selectedPriorities = if (priority in selectedPriorities) {
                                    selectedPriorities - priority
                                } else {
                                    selectedPriorities + priority
                                }
                            },
                            label = { Text(priority.capitalize()) }
                        )
                    }
                }
            }

            // Issue Type Filter
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(
                    "Issue Type",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                ) {
                    listOf("bug", "feature", "ui").forEach { type ->
                        FilterChip(
                            selected = type in selectedIssueTypes,
                            onClick = {
                                selectedIssueTypes = if (type in selectedIssueTypes) {
                                    selectedIssueTypes - type
                                } else {
                                    selectedIssueTypes + type
                                }
                            },
                            label = { Text(type.capitalize()) }
                        )
                    }
                }
            }

            // Labels Filter
            if (availableLabels.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        "Labels",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small)
                    ) {
                        availableLabels.take(5).forEach { label ->
                            FilterChip(
                                selected = label in selectedLabels,
                                onClick = {
                                    selectedLabels = if (label in selectedLabels) {
                                        selectedLabels - label
                                    } else {
                                        selectedLabels + label
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (availableLabels.size > 5) {
                        Text(
                            "Showing first 5 labels",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Default)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onApply(
                            AdvancedFilters(
                                assignee = assignee.trim(),
                                priorities = selectedPriorities,
                                labels = selectedLabels,
                                issueTypes = selectedIssueTypes
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filters")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.Large))
        }
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
