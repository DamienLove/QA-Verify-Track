package com.qa.verifyandtrack.app.ui.components.library

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.qa.verifyandtrack.app.ui.theme.Elevation

@Composable
fun QACard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Small),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Small),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}
