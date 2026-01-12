package com.qa.verifyandtrack.app.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.ui.theme.*

enum class BadgeType {
    PRIORITY_CRITICAL, PRIORITY_HIGH, PRIORITY_MEDIUM, PRIORITY_LOW,
    STATUS_OPEN, STATUS_FIXED, STATUS_BLOCKED, STATUS_DRAFT, CUSTOM
}

@Composable
fun QABadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier,
    customColor: Color? = null
) {
    val backgroundColor = when (type) {
        BadgeType.PRIORITY_CRITICAL -> PriorityCritical
        BadgeType.PRIORITY_HIGH -> PriorityHigh
        BadgeType.PRIORITY_MEDIUM -> PriorityMedium
        BadgeType.PRIORITY_LOW -> PriorityLow
        BadgeType.STATUS_OPEN -> StatusOpen
        BadgeType.STATUS_FIXED -> StatusFixed
        BadgeType.STATUS_BLOCKED -> StatusBlocked
        BadgeType.STATUS_DRAFT -> StatusDraft
        BadgeType.CUSTOM -> customColor ?: MaterialTheme.colorScheme.primary
    }

    Text(
        text = text.uppercase(),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
