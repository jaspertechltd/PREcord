package com.example.precord.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrecordPurple,
    onPrimary = PrecordSurface,
    primaryContainer = PrecordPurpleDark,
    secondary = PrecordYellow,
    onSecondary = PrecordSurface,
    background = PrecordSurface,
    surface = PrecordSurface,
    surfaceVariant = PrecordSurfaceVariant,
    onBackground = PrecordOnSurface,
    onSurface = PrecordOnSurface,
    onSurfaceVariant = PrecordOnSurfaceVariant,
    error = PrecordError
)

@Composable
fun PrecordTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
