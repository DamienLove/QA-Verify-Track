package com.qa.verifyandtrack.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = QALightPrimary,
    onPrimary = QALightOnPrimary,
    primaryContainer = QALightPrimaryContainer,
    onPrimaryContainer = QALightOnPrimaryContainer,
    secondary = QALightSecondary,
    onSecondary = QALightOnSecondary,
    secondaryContainer = QALightSecondaryContainer,
    onSecondaryContainer = QALightOnSecondaryContainer,
    tertiary = QALightTertiary,
    onTertiary = QALightOnTertiary,
    tertiaryContainer = QALightTertiaryContainer,
    onTertiaryContainer = QALightOnTertiaryContainer,
    background = QALightBackground,
    onBackground = QALightOnBackground,
    surface = QALightSurface,
    onSurface = QALightOnSurface,
    surfaceVariant = QALightSurfaceVariant,
    onSurfaceVariant = QALightOnSurfaceVariant,
    error = QALightError,
    onError = QALightOnError,
    errorContainer = QALightErrorContainer,
    onErrorContainer = QALightOnErrorContainer,
    outline = QALightOutline,
    outlineVariant = QALightOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = QADarkPrimary,
    onPrimary = QADarkOnPrimary,
    primaryContainer = QADarkPrimaryContainer,
    onPrimaryContainer = QADarkOnPrimaryContainer,
    secondary = QADarkSecondary,
    onSecondary = QADarkOnSecondary,
    secondaryContainer = QADarkSecondaryContainer,
    onSecondaryContainer = QADarkOnSecondaryContainer,
    tertiary = QADarkTertiary,
    onTertiary = QADarkOnTertiary,
    tertiaryContainer = QADarkTertiaryContainer,
    onTertiaryContainer = QADarkOnTertiaryContainer,
    background = QADarkBackground,
    onBackground = QADarkOnBackground,
    surface = QADarkSurface,
    onSurface = QADarkOnSurface,
    surfaceVariant = QADarkSurfaceVariant,
    onSurfaceVariant = QADarkOnSurfaceVariant,
    error = QADarkError,
    onError = QADarkOnError,
    errorContainer = QADarkErrorContainer,
    onErrorContainer = QADarkOnErrorContainer,
    outline = QADarkOutline,
    outlineVariant = QADarkOutlineVariant
)

@Composable
fun QATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QATypography,
        shapes = QAShapes,
        content = content
    )
}
