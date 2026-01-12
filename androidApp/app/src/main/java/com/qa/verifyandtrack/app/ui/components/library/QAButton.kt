package com.qa.verifyandtrack.app.ui.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.qa.verifyandtrack.app.ui.theme.Spacing

enum class QAButtonVariant {
    PRIMARY, SECONDARY, OUTLINED, TEXT
}

@Composable
fun QAButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: QAButtonVariant = QAButtonVariant.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    when (variant) {
        QAButtonVariant.PRIMARY -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading
        ) {
            ButtonContent(text, loading, icon)
        }
        QAButtonVariant.SECONDARY -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading
        ) {
            ButtonContent(text, loading, icon)
        }
        QAButtonVariant.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading
        ) {
            ButtonContent(text, loading, icon)
        }
        QAButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled && !loading
        ) {
            ButtonContent(text, loading, icon)
        }
    }
}

@Composable
private fun ButtonContent(text: String, loading: Boolean, icon: ImageVector?) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
    } else if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
    }
    Text(text)
}
