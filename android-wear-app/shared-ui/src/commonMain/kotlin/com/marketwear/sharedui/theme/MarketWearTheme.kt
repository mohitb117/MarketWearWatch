package com.marketwear.sharedui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

val MarketWearDarkColorScheme = darkColorScheme(
    primary = MarketWearPalette.DarkPrimary,
    onPrimary = MarketWearPalette.DarkOnPrimary,
    primaryContainer = MarketWearPalette.DarkPrimaryContainer,
    onPrimaryContainer = MarketWearPalette.DarkOnPrimaryContainer,
    secondary = MarketWearPalette.PositiveTrend,
    onSecondary = MarketWearPalette.DarkOnPrimary,
    background = MarketWearPalette.DarkBackground,
    onBackground = MarketWearPalette.DarkTextPrimary,
    surface = MarketWearPalette.DarkSurface,
    onSurface = MarketWearPalette.DarkTextPrimary,
    surfaceVariant = MarketWearPalette.DarkSurfaceVariant,
    onSurfaceVariant = MarketWearPalette.DarkTextSecondary,
    error = MarketWearPalette.DarkError,
    onError = MarketWearPalette.DarkOnError,
    errorContainer = MarketWearPalette.DarkErrorContainer,
    onErrorContainer = MarketWearPalette.DarkOnErrorContainer
)

val MarketWearLightColorScheme = lightColorScheme(
    primary = MarketWearPalette.LightPrimary,
    onPrimary = MarketWearPalette.LightOnPrimary,
    primaryContainer = MarketWearPalette.LightPrimaryContainer,
    onPrimaryContainer = MarketWearPalette.LightOnPrimaryContainer,
    background = MarketWearPalette.LightBackground,
    onBackground = MarketWearPalette.LightTextPrimary,
    surface = MarketWearPalette.LightSurface,
    onSurface = MarketWearPalette.LightTextPrimary,
    surfaceVariant = MarketWearPalette.LightSurfaceVariant,
    onSurfaceVariant = MarketWearPalette.LightTextSecondary
)

/**
 * Shared Compose Multiplatform Material 3 theme used across Android Mobile, Desktop, iOS, and Web.
 * Defaults to Dark Mode (`darkTheme = true`).
 */
@Composable
fun MarketWearTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MarketWearDarkColorScheme else MarketWearLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
