package com.marketwear.sharedui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared color palette across Mobile, Desktop, iOS, Web, and Wear OS surfaces.
 */
object MarketWearPalette {
    // Dark theme surfaces & accents
    val DarkBackground = Color(0xFF0B0F14)
    val DarkSurface = Color(0xFF131A22)
    val DarkSurfaceVariant = Color(0xFF1C2632)
    val DarkPrimary = Color(0xFF64B5F6)
    val DarkOnPrimary = Color(0xFF062033)
    val DarkPrimaryContainer = Color(0xFF163A59)
    val DarkOnPrimaryContainer = Color(0xFFD2E9FF)
    val DarkTextPrimary = Color(0xFFF1F5F9)
    val DarkTextSecondary = Color(0xFF94A3B8)
    val DarkError = Color(0xFFF87171)
    val DarkOnError = Color(0xFF3B0A0A)
    val DarkErrorContainer = Color(0xFF3D1518)
    val DarkOnErrorContainer = Color(0xFFFECACA)

    // Light theme surfaces & accents
    val LightBackground = Color(0xFFF8FAFC)
    val LightSurface = Color.White
    val LightSurfaceVariant = Color(0xFFE2E8F0)
    val LightPrimary = Color(0xFF1565C0)
    val LightOnPrimary = Color.White
    val LightPrimaryContainer = Color(0xFFD2E4FF)
    val LightOnPrimaryContainer = Color(0xFF001C3A)
    val LightTextPrimary = Color(0xFF0F172A)
    val LightTextSecondary = Color(0xFF475569)

    // Trend & Chart Colors
    val PositiveTrend = Color(0xFF4ADE80)
    val NegativeTrend = Color(0xFFF87171)
    val SecondaryCurve = Color(0xFF64B5F6).copy(alpha = 0.65f)
    val BaselineRule = Color(0xFF64748B)

    // Watch Status Chip Colors
    val ConnectedBg = Color(0xFF132E1C)
    val ConnectedDot = Color(0xFF4ADE80)
    val ConnectedText = Color(0xFF86EFAC)
    val DisconnectedBg = Color(0xFF341619)
    val DisconnectedDot = Color(0xFFF87171)
    val DisconnectedText = Color(0xFFFCA5A5)
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
    const val WATCH_SECTORS_HEADER = "watch_sectors_header"
    const val WATCH_REFRESH_ACTION = "watch_refresh_action"
    const val WATCH_TIMESTAMP_FOOTER = "watch_timestamp_footer"
}
