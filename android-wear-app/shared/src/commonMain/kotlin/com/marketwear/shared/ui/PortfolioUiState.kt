package com.marketwear.shared.ui

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Extensible visualization mode for Portfolio Daily Score (100-baseline) cards and charts.
 */
@Serializable
enum class PortfolioWeightMode {
    /** Re-scales the 37 tracked holdings (59.41% raw weight) to 100% of the tracked equity basket. */
    NORMALIZED_100,

    /** Uses the raw portfolio allocation percentages (59.41% total weight; remaining 40.59% treated as 0% daily drift). */
    RAW_PORTFOLIO;

    fun next(): PortfolioWeightMode = when (this) {
        NORMALIZED_100 -> RAW_PORTFOLIO
        RAW_PORTFOLIO -> NORMALIZED_100
    }
}

/**
 * Extensible data synchronization / refresh lifecycle state across Wear OS and Mobile ViewModels.
 */
enum class RefreshState {
    IDLE,
    REFRESHING,
    ERROR
}

/**
 * Extensible Wear OS companion connection state for the Mobile app.
 */
enum class WatchConnectionState {
    CONNECTED,
    DISCONNECTED
}

/**
 * Directional price/score movement state.
 */
@Serializable
enum class TrendDirection {
    POSITIVE,
    NEGATIVE;

    companion object {
        fun fromChange(change: Double): TrendDirection =
            if (change >= 0.0) POSITIVE else NEGATIVE
    }
}

/**
 * User preference for the Android 16 Progress-Centric portfolio Live Update notification.
 */
enum class LiveNotificationPreference {
    ACTIVE,
    PAUSED;

    fun next(): LiveNotificationPreference = when (this) {
        ACTIVE -> PAUSED
        PAUSED -> ACTIVE
    }

    companion object {
        fun fromStorageKey(key: String?): LiveNotificationPreference =
            entries.firstOrNull { it.name == key } ?: ACTIVE
    }
}

/**
 * Resolved delivery & promotion readiness state for the Live Update portfolio notification.
 */
enum class LiveNotificationDeliveryState {
    PROMOTED_LIVE_UPDATE,
    STANDARD_ONGOING_NEEDS_OPT_IN,
    PERMISSION_REQUIRED,
    PAUSED_BY_USER
}

/**
 * Shared, platform-agnostic UI state for the Portfolio Daily Score (100-baseline) card and Vico chart.
 * Pre-computes all display strings, active/secondary quotes, and sparkline series in the ViewModel
 * / state holder layer so Composables remain pure, declarative, and zero-logic.
 */
@Serializable
data class PortfolioCardUiState(
    val weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
    val normalizedQuote: IndexQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L),
    val unnormalizedQuote: IndexQuote = PortfolioRegistry.toUnnormalizedQuote(normalizedQuote),
    val activeQuote: IndexQuote = normalizedQuote,
    val secondaryQuote: IndexQuote = unnormalizedQuote,
    val trendDirection: TrendDirection = TrendDirection.fromChange(activeQuote.change),
    val watchTitleLabel: String = "Portfolio (100%)",
    val mobileModePillLabel: String = "Normalized (100%)",
    val watchSecondaryLabel: String = "Raw: ${unnormalizedQuote.formattedScore}",
    val mobileSecondarySummary: String = "Raw (${PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT}%): ${unnormalizedQuote.formattedScore} (${unnormalizedQuote.formattedPercent})",
    val mobileRangeSummary: String = "Range: ${activeQuote.dayLow} – ${activeQuote.dayHigh}",
    val mobileLegendLabel: String = "Solid: Normalized (100%) · Dashed: Raw (${PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT}%)",
    val primarySparkline: List<Double> = if (activeQuote.sparkline.size >= 2) activeQuote.sparkline else DEFAULT_BASELINE_POINTS,
    val secondarySparkline: List<Double> = if (secondaryQuote.sparkline.size >= 2) secondaryQuote.sparkline else primarySparkline,
    val enabledHoldingsCount: Int = PortfolioRegistry.HOLDINGS.size,
    val totalHoldingsCount: Int = PortfolioRegistry.HOLDINGS.size,
    val activeRawWeightPercent: Double = PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT,
    val liveUpdateSpec: PortfolioLiveUpdateSpec = PortfolioLiveUpdateFormatter.buildSpec(
        activeQuote = activeQuote,
        enabledHoldingsCount = enabledHoldingsCount,
        totalHoldingsCount = totalHoldingsCount
    )
) {
    companion object {
        val DEFAULT_BASELINE_POINTS: List<Double> = listOf(100.0, 100.0)
    }
}

/**
 * Partitioned market quotes and pre-computed [PortfolioCardUiState] produced by UI state holders / ViewModels.
 */
@Serializable
data class MarketDashboardState(
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val latestUpdatedMillis: Long = 0L
)

/**
 * Pure, unit-testable state reducer that partitions raw [IndexQuote] lists and builds
 * [PortfolioCardUiState] for each [PortfolioWeightMode].
 */
object MarketUiStateBuilder {

    private val bridgeJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun encodeDashboardJson(dashboard: MarketDashboardState): String =
        bridgeJson.encodeToString(MarketDashboardState.serializer(), dashboard)

    fun buildPortfolioCardState(
        normalizedQuote: IndexQuote,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): PortfolioCardUiState {
        val totalHoldingsCount = PortfolioRegistry.HOLDINGS.size
        val enabledHoldings = PortfolioRegistry.activeHoldings(disabledHoldingIds, customWeights)
        val enabledHoldingsCount = enabledHoldings.size.coerceAtLeast(1)
        val activeRawWeight = PortfolioRegistry.rawWeightPercentFor(disabledHoldingIds, customWeights)
            .let { if (it > 0.0) it else PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT }
        val unnormalizedQuote = PortfolioRegistry.toUnnormalizedQuote(normalizedQuote, rawWeightSum = activeRawWeight)
        val (activeQuote, secondaryQuote) = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 -> normalizedQuote to unnormalizedQuote
            PortfolioWeightMode.RAW_PORTFOLIO -> unnormalizedQuote to normalizedQuote
        }
        val rawPctStr = activeRawWeight.toString()

        val primarySparkline = if (activeQuote.sparkline.size >= 2) {
            activeQuote.sparkline
        } else {
            PortfolioCardUiState.DEFAULT_BASELINE_POINTS
        }
        val secondarySparkline = if (secondaryQuote.sparkline.size >= 2) {
            secondaryQuote.sparkline
        } else {
            primarySparkline
        }

        val watchTitleLabel = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 -> "Portfolio (100%)"
            PortfolioWeightMode.RAW_PORTFOLIO -> "Portfolio ($rawPctStr%)"
        }
        val mobileModePillLabel = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 -> "Normalized (100%)"
            PortfolioWeightMode.RAW_PORTFOLIO -> "Raw ($rawPctStr%)"
        }
        val watchSecondaryLabel = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 -> "Raw: ${unnormalizedQuote.formattedScore}"
            PortfolioWeightMode.RAW_PORTFOLIO -> "Norm: ${normalizedQuote.formattedScore}"
        }
        val mobileSecondarySummary = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 ->
                "Raw ($rawPctStr%): ${unnormalizedQuote.formattedScore} (${unnormalizedQuote.formattedPercent})"
            PortfolioWeightMode.RAW_PORTFOLIO ->
                "Norm (100%): ${normalizedQuote.formattedScore} (${normalizedQuote.formattedPercent})"
        }
        val mobileLegendLabel = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 ->
                "Solid: Normalized (100%) · Dashed: Raw ($rawPctStr%)"
            PortfolioWeightMode.RAW_PORTFOLIO ->
                "Solid: Raw ($rawPctStr%) · Dashed: Normalized (100%)"
        }
        val liveUpdateSpec = PortfolioLiveUpdateFormatter.buildSpec(
            activeQuote = activeQuote,
            enabledHoldingsCount = enabledHoldingsCount,
            totalHoldingsCount = totalHoldingsCount,
            modeBadgeLabel = mobileModePillLabel,
            secondarySummaryLine = mobileSecondarySummary
        )

        return PortfolioCardUiState(
            weightMode = weightMode,
            normalizedQuote = normalizedQuote,
            unnormalizedQuote = unnormalizedQuote,
            activeQuote = activeQuote,
            secondaryQuote = secondaryQuote,
            trendDirection = TrendDirection.fromChange(activeQuote.change),
            watchTitleLabel = watchTitleLabel,
            mobileModePillLabel = mobileModePillLabel,
            watchSecondaryLabel = watchSecondaryLabel,
            mobileSecondarySummary = mobileSecondarySummary,
            mobileRangeSummary = "Range: ${activeQuote.dayLow} – ${activeQuote.dayHigh}",
            mobileLegendLabel = mobileLegendLabel,
            primarySparkline = primarySparkline,
            secondarySparkline = secondarySparkline,
            enabledHoldingsCount = enabledHoldingsCount,
            totalHoldingsCount = totalHoldingsCount,
            activeRawWeightPercent = activeRawWeight,
            liveUpdateSpec = liveUpdateSpec
        )
    }

    fun buildDashboardState(
        quotes: List<IndexQuote>,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        fallbackTimestampMillis: Long = 0L,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): MarketDashboardState {
        val latestUpdated = quotes.maxOfOrNull { it.lastUpdated } ?: fallbackTimestampMillis
        val portfolioQuote = quotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }
            ?: PortfolioRegistry.createDefaultFallbackPortfolioQuote(latestUpdated)
        val indices = quotes.filter { it.symbol == "SPX" || it.symbol == "NDX" }
        val sectors = quotes.filter {
            it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL && it.symbol != "SPX" && it.symbol != "NDX"
        }

        return MarketDashboardState(
            portfolioState = buildPortfolioCardState(
                normalizedQuote = portfolioQuote,
                weightMode = weightMode,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights
            ),
            indices = indices,
            sectors = sectors,
            latestUpdatedMillis = latestUpdated
        )
    }
}
