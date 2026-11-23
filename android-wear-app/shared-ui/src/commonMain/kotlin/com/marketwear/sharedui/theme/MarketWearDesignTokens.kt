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
    val DarkBackground = Color(0xFF06090E)
    val DarkSurface = Color(0xFF0C111A)
    val DarkSurfaceVariant = Color(0xFF131A26)
    val DarkSurfaceElevated = Color(0xFF182130)
    val DarkSurfaceHigh = Color(0xFF1F2B3E)
    val DarkHairlineBorder = Color(0xFF223046)
    val DarkPrimary = Color(0xFF38BDF8)
    val DarkOnPrimary = Color(0xFF042233)
    val DarkPrimaryContainer = Color(0xFF0E3047)
    val DarkOnPrimaryContainer = Color(0xFFBAE6FD)
    val DarkTextPrimary = Color(0xFFF1F5F9)
    val DarkTextSecondary = Color(0xFF8B9BB4)
    val DarkError = Color(0xFFFF453A)
    val DarkOnError = Color(0xFF3F0614)
    val DarkErrorContainer = Color(0xFF381016)
    val DarkOnErrorContainer = Color(0xFFFFD1D6)

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

    // Trend, Bipolar Progress & Chart Colors (Obsidian Pro Emerald & Crimson)
    val PositiveTrend = Color(0xFF00E676)
    val NegativeTrend = Color(0xFFFF3B30)
    val PositivePillBg = Color(0xFF062A1B)
    val NegativePillBg = Color(0xFF2B0C11)
    val AmberAccent = Color(0xFFFFB300)
    val AmberPillBg = Color(0xFF2D1F05)
    val NeutralPillBg = Color(0xFF141C28)
    val SecondaryCurve = Color(0xFF38BDF8).copy(alpha = 0.72f)
    val BaselineRule = Color(0xFF566982)
    val BaselineTick = Color(0xFFE2E8F0)
    val TrackBackground = Color(0xFF141C28)
    val BaselineSlate = Color(0xFF28364A)
    val WatchBezelOuter = Color(0xFF182230)
    val WatchBezelInner = Color(0xFF05080D)

    // Watch Status Chip Colors
    val ConnectedBg = Color(0xFF062A1B)
    val ConnectedDot = Color(0xFF00E676)
    val ConnectedText = Color(0xFF84F5C0)
    val DisconnectedBg = Color(0xFF2B0C11)
    val DisconnectedDot = Color(0xFFFF3B30)
    val DisconnectedText = Color(0xFFFFB3BA)
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
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = "tnum"
    )

    val TabularMetric = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.4).sp,
        fontFeatureSettings = "tnum"
    )

    val TabularSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum"
    )
}
