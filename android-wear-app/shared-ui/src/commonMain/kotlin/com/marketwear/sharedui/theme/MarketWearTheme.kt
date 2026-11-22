package com.marketwear.sharedui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.marketwear.shared.ui.ThemeAppearance

val MarketWearDarkColorScheme = darkColorScheme(
    primary = MarketWearPalette.DarkPrimary,
    onPrimary = MarketWearPalette.DarkOnPrimary,
    primaryContainer = MarketWearPalette.DarkPrimaryContainer,
    onPrimaryContainer = MarketWearPalette.DarkOnPrimaryContainer,
    secondary = MarketWearPalette.PositiveTrend,
    onSecondary = MarketWearPalette.DarkOnPrimary,
    secondaryContainer = MarketWearPalette.PositivePillBg,
    tertiary = MarketWearPalette.AmberAccent,
    background = MarketWearPalette.DarkBackground,
    onBackground = MarketWearPalette.DarkTextPrimary,
    surface = MarketWearPalette.DarkSurface,
    onSurface = MarketWearPalette.DarkTextPrimary,
    surfaceVariant = MarketWearPalette.DarkSurfaceVariant,
    onSurfaceVariant = MarketWearPalette.DarkTextSecondary,
    surfaceContainerLow = MarketWearPalette.DarkSurface,
    surfaceContainer = MarketWearPalette.DarkSurfaceElevated,
    surfaceContainerHigh = MarketWearPalette.DarkSurfaceHigh,
    outline = MarketWearPalette.DarkHairlineBorder,
    outlineVariant = MarketWearPalette.DarkHairlineBorder,
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
 * Defaults to Dark Mode ([ThemeAppearance.DARK]).
 */
@Composable
fun MarketWearTheme(
    appearance: ThemeAppearance = ThemeAppearance.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appearance) {
        ThemeAppearance.DARK -> MarketWearDarkColorScheme
        ThemeAppearance.LIGHT -> MarketWearLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
