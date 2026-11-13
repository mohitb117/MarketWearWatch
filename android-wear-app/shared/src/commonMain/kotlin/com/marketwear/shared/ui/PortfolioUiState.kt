package com.marketwear.shared.ui

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry

/**
 * Extensible visualization mode for Portfolio Daily Score (100-baseline) cards and charts.
 */
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
enum class TrendDirection {
    POSITIVE,
    NEGATIVE;

    companion object {
        fun fromChange(change: Double): TrendDirection =
            if (change >= 0.0) POSITIVE else NEGATIVE
    }
}

/**
 * Shared, platform-agnostic UI state for the Portfolio Daily Score (100-baseline) card and Vico chart.
 * Pre-computes all display strings, active/secondary quotes, and sparkline series in the ViewModel
 * / state holder layer so Composables remain pure, declarative, and zero-logic.
 */
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
    val secondarySparkline: List<Double> = if (secondaryQuote.sparkline.size >= 2) secondaryQuote.sparkline else primarySparkline
) {
    companion object {
        val DEFAULT_BASELINE_POINTS: List<Double> = listOf(100.0, 100.0)
    }
}

/**
 * Partitioned market quotes and pre-computed [PortfolioCardUiState] produced by UI state holders / ViewModels.
 */
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

    fun buildPortfolioCardState(
        normalizedQuote: IndexQuote,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100
    ): PortfolioCardUiState {
        val unnormalizedQuote = PortfolioRegistry.toUnnormalizedQuote(normalizedQuote)
        val (activeQuote, secondaryQuote) = when (weightMode) {
            PortfolioWeightMode.NORMALIZED_100 -> normalizedQuote to unnormalizedQuote
            PortfolioWeightMode.RAW_PORTFOLIO -> unnormalizedQuote to normalizedQuote
        }
        val rawPctStr = PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT.toString()

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
            secondarySparkline = secondarySparkline
        )
    }

    fun buildDashboardState(
        quotes: List<IndexQuote>,
        weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
        fallbackTimestampMillis: Long = 0L
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
                weightMode = weightMode
            ),
            indices = indices,
            sectors = sectors,
            latestUpdatedMillis = latestUpdated
        )
    }
}
