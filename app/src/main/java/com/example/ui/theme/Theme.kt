package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SuperTvColorScheme = darkColorScheme(
    primary = RedAccent,
    onPrimary = TextPrimary,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = TextPrimary,
    secondary = RedAccentLight,
    onSecondary = TextPrimary,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

@Composable
fun SuperTvTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SuperTvColorScheme,
        typography = Typography,
        content = content
    )
}

