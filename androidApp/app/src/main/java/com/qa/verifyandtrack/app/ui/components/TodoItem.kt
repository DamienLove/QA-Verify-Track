package com.qa.verifyandtrack.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.data.model.TodoItem

@Composable
fun TodoItemRow(item: TodoItem, onClick: (TodoItem) -> Unit = {}) {
    Card(onClick = { onClick(item) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = item.repoName, style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.openIssueCount} issues • ${item.openPrCount} PRs",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}
