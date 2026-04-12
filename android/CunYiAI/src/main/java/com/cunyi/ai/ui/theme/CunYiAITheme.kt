package com.cunyi.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 老年人友好浅色配色
private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryGreenLight,
    onPrimaryContainer = TextPrimary,
    secondary = AccentOrange,
    onSecondary = Color.White,
    secondaryContainer = AccentYellow,
    onSecondaryContainer = TextPrimary,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = BackgroundWhite,
    onSurface = TextPrimary,
    surfaceVariant = InputBackground,
    onSurfaceVariant = TextSecondary,
    error = AlertRed,
    onError = Color.White
)

// 老年人友好深色配色（备用）
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenLight,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = TextOnDark,
    secondary = AccentOrange,
    onSecondary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextOnDark,
    surface = Color(0xFF1E1E1E),
    onSurface = TextOnDark,
    error = AlertRed,
    onError = Color.White
)

@Composable
fun CunYiAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ElderlyTypography,
        content = content
    )
}
