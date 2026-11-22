package com.marketwear.sharedui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared Obsidian Terminal & Daylight color palette across Mobile, Desktop, iOS, Web, and Wear OS.
 */
object MarketWearPalette {
    // Dark theme surfaces & accents (Obsidian Terminal Palette)
    val DarkBackground = Color(0xFF070B11)
    val DarkSurface = Color(0xFF0E1520)
    val DarkSurfaceVariant = Color(0xFF151F2E)
    val DarkSurfaceElevated = Color(0xFF151F2E)
    val DarkSurfaceHigh = Color(0xFF1D2A3D)
    val DarkHairlineBorder = Color(0xFF28384F)
    val DarkPrimary = Color(0xFF38BDF8)
    val DarkOnPrimary = Color(0xFF042233)
    val DarkPrimaryContainer = Color(0xFF10324A)
    val DarkOnPrimaryContainer = Color(0xFFBAE6FD)
    val DarkTextPrimary = Color(0xFFF1F5F9)
    val DarkTextSecondary = Color(0xFF94A3B8)
    val DarkError = Color(0xFFFB7185)
    val DarkOnError = Color(0xFF3F0614)
    val DarkErrorContainer = Color(0xFF38131E)
    val DarkOnErrorContainer = Color(0xFFFECDD3)

    // Light theme surfaces & accents (Daylight Terminal Palette)
    val LightBackground = Color(0xFFF3F6FA)
    val LightSurface = Color.White
    val LightSurfaceVariant = Color(0xFFEAF0F7)
    val LightPrimary = Color(0xFF0284C7)
    val LightOnPrimary = Color.White
    val LightPrimaryContainer = Color(0xFFDDF3FF)
    val LightOnPrimaryContainer = Color(0xFF082F49)
    val LightTextPrimary = Color(0xFF0F172A)
    val LightTextSecondary = Color(0xFF475569)

    // Trend, Bipolar Progress & Chart Colors
    val PositiveTrend = Color(0xFF34D399)
    val NegativeTrend = Color(0xFFFB7185)
    val PositivePillBg = Color(0xFF0B2E24)
    val NegativePillBg = Color(0xFF38131E)
    val AmberAccent = Color(0xFFFBBF24)
    val AmberPillBg = Color(0xFF332609)
    val NeutralPillBg = Color(0xFF1E293B)
    val SecondaryCurve = Color(0xFF38BDF8).copy(alpha = 0.72f)
    val BaselineRule = Color(0xFF64748B)
    val BaselineTick = Color(0xFFE2E8F0)
    val TrackBackground = Color(0xFF1E293B)
    val BaselineSlate = Color(0xFF334155)
    val WatchBezelOuter = Color(0xFF1C2430)
    val WatchBezelInner = Color(0xFF05080D)

    // Watch Status Chip Colors
    val ConnectedBg = Color(0xFF0B2E24)
    val ConnectedDot = Color(0xFF34D399)
    val ConnectedText = Color(0xFFA7F3D0)
    val DisconnectedBg = Color(0xFF38131E)
    val DisconnectedDot = Color(0xFFFB7185)
    val DisconnectedText = Color(0xFFFECDD3)
}

/**
 * Shared spacing, corner radius, stroke, and sizing constants.
 */
object MarketWearDimens {
    val CardCornerRadius = 18.dp
    val MetricCardCornerRadius = 16.dp
    val PillCornerRadius = 20.dp
    val ChipCornerRadius = 16.dp
    val ChartContainerCornerRadius = 12.dp
    val ButtonCornerRadius = 12.dp
    val CompactRowCornerRadius = 8.dp

    val ScreenPadding = 16.dp
    val CardPadding = 16.dp
    val SectionSpacing = 16.dp
    val ItemSpacing = 12.dp
    val SmallSpacing = 6.dp
    val TinySpacing = 4.dp

    val StatusDotSize = 8.dp
    val ProgressBarHeight = 6.dp
    val PrimaryButtonHeight = 50.dp
    val ButtonLoaderSize = 20.dp
    val DefaultChartHeight = 104.dp
    val DesktopChartHeight = 140.dp

    val PrimaryLineThickness = 2.5.dp
    val SecondaryLineThickness = 1.75.dp
    val BaselineLineThickness = 1.dp
    val DashedLineLength = 6.dp
    val DashedGapLength = 6.dp
}

/**
 * Shared typography size tokens.
 */
object MarketWearTextSizes {
    val TopBarTitle = 20.sp
    val StatusChip = 12.sp
    val ModePill = 11.sp
    val HoldingRow = 12.sp
    val FilterChip = 12.sp
}

/**
 * Shared Material shape tokens.
 */
object MarketWearShapes {
    val PortfolioCard = RoundedCornerShape(MarketWearDimens.CardCornerRadius)
    val MetricCard = RoundedCornerShape(MarketWearDimens.MetricCardCornerRadius)
    val TogglePill = RoundedCornerShape(MarketWearDimens.PillCornerRadius)
    val StatusChip = RoundedCornerShape(MarketWearDimens.ChipCornerRadius)
    val ChartContainer = RoundedCornerShape(MarketWearDimens.ChartContainerCornerRadius)
    val PrimaryButton = RoundedCornerShape(MarketWearDimens.ButtonCornerRadius)
    val CompactRow = RoundedCornerShape(MarketWearDimens.CompactRowCornerRadius)
}

/**
 * Stable LazyColumn / ScalingLazyColumn item keys shared across platforms.
 */
object MarketWearItemKeys {
    const val PORTFOLIO_HERO = "portfolio_score_hero"
    const val ERROR_BANNER = "error_banner"
    const val INDICES_HEADER = "indices_header"
    const val SECTORS_HEADER = "sectors_header"

    const val WATCH_HEADER_TITLE = "watch_header_title"
    const val WATCH_PORTFOLIO_CARD = "watch_portfolio_score_card"
    const val WATCH_CONFIGURE_HOLDINGS = "watch_configure_holdings"
    const val WATCH_SECTORS_HEADER = "watch_sectors_header"
    const val WATCH_REFRESH_ACTION = "watch_refresh_action"
    const val WATCH_TIMESTAMP_FOOTER = "watch_timestamp_footer"

    const val LIVE_NOTIFICATION_BANNER = "live_notification_banner"
    const val HOLDINGS_CONFIG_HEADER = "holdings_config_header"
}

/**
 * Shared typography styles enforcing tabular numerals (`tnum`) on live financial readouts.
 */
object MarketWearTextStyles {
    val Tabular = TextStyle(fontFeatureSettings = "tnum")

    val TabularHero = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = "tnum"
    )

    val TabularMetric = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum"
    )

    val TabularSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    )
}
