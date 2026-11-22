package com.marketwear.shared.model

import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class PortfolioHolding(
    val name: String,
    val displayTicker: String,
    val yahooTicker: String,
    val weightPercent: Double,
    val id: String = yahooTicker
)

@Serializable
data class TickerIntradayData(
    val yahooTicker: String,
    val previousClose: Double,
    val currentPrice: Double,
    val intradayCloses: List<Double> = emptyList()
)

/**
 * Hardcoded 37 combined UBS & Fidelity portfolio positions with percentage weights and proxies,
 * plus 100-baseline score calculation (both Normalized 100% and Unnormalized 59.41% modes).
 */
object PortfolioRegistry {
    const val PORTFOLIO_SYMBOL = "PORTFOLIO"
    const val BASELINE_SCORE = 100.0
    const val MIN_HOLDING_WEIGHT_PERCENT = 0.05
    const val MAX_HOLDING_WEIGHT_PERCENT = 100.0
    private const val MAX_SPARKLINE_POINTS = 28

    val HOLDINGS: List<PortfolioHolding> = listOf(
        PortfolioHolding("Apple Inc.", "AAPL", "AAPL", 5.93, "aapl"),
        PortfolioHolding("General Atlantic Global Growth", "Proxy: QQQ", "QQQ", 5.46, "ga_qqq"),
        PortfolioHolding("Silver Point Capital Fund", "Proxy: BKLN", "BKLN", 5.41, "silver_point_bkln"),
        PortfolioHolding("NVIDIA Corp.", "NVDA", "NVDA", 5.02, "nvda"),
        PortfolioHolding("Coatue Innovative Strategies", "Proxy: ARKK", "ARKK", 4.06, "coatue_arkk"),
        PortfolioHolding("Amazon.com Inc.", "AMZN", "AMZN", 3.43, "amzn"),
        PortfolioHolding("Microsoft Corp.", "MSFT", "MSFT", 3.42, "msft"),
        PortfolioHolding("Alphabet Inc. (Class A & C)", "GOOGL / GOOG", "GOOGL", 3.28, "googl"),
        PortfolioHolding("Netflix Inc.", "NFLX", "NFLX", 1.60, "nflx"),
        PortfolioHolding("Broadcom Inc.", "AVGO", "AVGO", 1.56, "avgo"),
        PortfolioHolding("Berkshire Hathaway (Class B)", "BRK.B", "BRK-B", 1.33, "brk_b"),
        PortfolioHolding("Eli Lilly & Co.", "LLY", "LLY", 1.26, "lly"),
        PortfolioHolding("Tesla Inc.", "TSLA", "TSLA", 1.23, "tsla"),
        PortfolioHolding("Meta Platforms Inc.", "META", "META", 1.13, "meta"),
        PortfolioHolding("JPMorgan Chase & Co.", "JPM", "JPM", 1.13, "jpm"),
        PortfolioHolding("Fidelity 500 Index Fund", "FXAIX (IVV)", "IVV", 1.06, "fxaix_ivv"),
        PortfolioHolding("Vanguard S&P 500 Value ETF", "VOOV", "VOOV", 1.02, "voov"),
        PortfolioHolding("Visa Inc.", "V", "V", 0.90, "v"),
        PortfolioHolding("Micron Technology Inc.", "MU", "MU", 0.79, "mu"),
        PortfolioHolding("Johnson & Johnson", "JNJ", "JNJ", 0.77, "jnj"),
        PortfolioHolding("Mastercard Inc.", "MA", "MA", 0.71, "ma"),
        PortfolioHolding("Advanced Micro Devices Inc.", "AMD", "AMD", 0.71, "amd"),
        PortfolioHolding("iShares Core S&P 500 ETF", "IVV", "IVV", 0.69, "ishares_ivv"),
        PortfolioHolding("Walmart Inc.", "WMT", "WMT", 0.68, "wmt"),
        PortfolioHolding("AbbVie Inc.", "ABBV", "ABBV", 0.64, "abbv"),
        PortfolioHolding("BNS TA-GEARS TPX Note", "Proxy: EWJ", "EWJ", 0.61, "bns_ewj"),
        PortfolioHolding("Cisco Systems Inc.", "CSCO", "CSCO", 0.59, "csco"),
        PortfolioHolding("Amphenol Corp.", "APH", "APH", 0.58, "aph"),
        PortfolioHolding("BofA TA-GEARS EWZ Note", "Proxy: EWZ", "EWZ", 0.56, "bofa_ewz"),
        PortfolioHolding("MS TAR-SS Intl Basket Note", "Proxy: VXUS", "VXUS", 0.55, "ms_vxus"),
        PortfolioHolding("Intel Corp.", "INTC", "INTC", 0.54, "intc"),
        PortfolioHolding("Costco Wholesale Corp.", "COST", "COST", 0.52, "cost"),
        PortfolioHolding("Palantir Technologies Inc.", "PLTR", "PLTR", 0.51, "pltr"),
        PortfolioHolding("ExxonMobil Corp.", "XOM", "XOM", 0.49, "xom"),
        PortfolioHolding("Merck & Co. Inc.", "MRK", "MRK", 0.47, "mrk"),
        PortfolioHolding("Fidelity Overseas Fund", "FOSFX (VXUS)", "VXUS", 0.45, "fosfx_vxus"),
        PortfolioHolding("JPM TA-GEARS KWEB Note", "Proxy: KWEB", "KWEB", 0.32, "jpm_kweb")
    )

    val ALL_HOLDING_IDS: Set<String> = HOLDINGS.map { it.id }.toSet()
    val TOP_10_HOLDING_IDS: Set<String> = HOLDINGS.take(10).map { it.id }.toSet()
    val DEFAULT_WEIGHTS_BY_ID: Map<String, Double> = HOLDINGS.associate { it.id to it.weightPercent }

    fun disabledIdsForTop10(): Set<String> = HOLDINGS.drop(10).map { it.id }.toSet()

    fun effectiveWeightPercent(
        holding: PortfolioHolding,
        customWeights: Map<String, Double> = emptyMap()
    ): Double = customWeights[holding.id] ?: holding.weightPercent

    fun resolvedHoldings(
        customWeights: Map<String, Double> = emptyMap()
    ): List<PortfolioHolding> {
        if (customWeights.isEmpty()) return HOLDINGS
        return HOLDINGS.map { holding ->
            val overrideWeight = customWeights[holding.id]
            if (overrideWeight != null) holding.copy(weightPercent = overrideWeight) else holding
        }
    }

    fun activeHoldings(
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): List<PortfolioHolding> {
        val resolved = resolvedHoldings(customWeights)
        if (disabledHoldingIds.isEmpty()) return resolved
        return resolved.filter { it.id !in disabledHoldingIds }
    }

    fun rawWeightPercentFor(
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): Double = round2(activeHoldings(disabledHoldingIds, customWeights).sumOf { it.weightPercent })

    fun uniqueTickersFor(disabledHoldingIds: Set<String> = emptySet()): List<String> =
        activeHoldings(disabledHoldingIds).map { it.yahooTicker }.distinct()

    /**
     * Exact sum of the 37 position weights (59.41%).
     */
    val TOTAL_RAW_WEIGHT_PERCENT: Double =
        round(HOLDINGS.sumOf { it.weightPercent } * 100.0) / 100.0

    /**
     * Distinct Yahoo Finance symbols required to price all 37 holdings.
     */
    val UNIQUE_YAHOO_TICKERS: List<String> =
        HOLDINGS.map { it.yahooTicker }.distinct()

    /**
     * Converts a Normalized (100% basket) score into the Unnormalized (55.41% total portfolio) score.
     */
    fun toUnnormalizedScore(
        normalizedScore: Double,
        rawWeightSum: Double = TOTAL_RAW_WEIGHT_PERCENT
    ): Double {
        val rawDelta = (normalizedScore - BASELINE_SCORE) * (rawWeightSum / 100.0)
        return round2(BASELINE_SCORE + rawDelta)
    }

    /**
     * Converts a Normalized PORTFOLIO [IndexQuote] into its Unnormalized (55.41% weight) counterpart,
     * scaling the score, change, dayHigh/dayLow, and every point on the intraday sparkline.
     */
    fun toUnnormalizedQuote(
        normalizedQuote: IndexQuote,
        rawWeightSum: Double = TOTAL_RAW_WEIGHT_PERCENT
    ): IndexQuote {
        val unnormPrice = toUnnormalizedScore(normalizedQuote.price, rawWeightSum)
        val unnormDelta = round2(unnormPrice - BASELINE_SCORE)
        val unnormSparkline = normalizedQuote.sparkline.map { toUnnormalizedScore(it, rawWeightSum) }
        val unnormHigh = toUnnormalizedScore(normalizedQuote.dayHigh, rawWeightSum)
        val unnormLow = toUnnormalizedScore(normalizedQuote.dayLow, rawWeightSum)
        return normalizedQuote.copy(
            price = unnormPrice,
            change = unnormDelta,
            percentChange = unnormDelta,
            dayHigh = maxOf(unnormHigh, unnormPrice, BASELINE_SCORE),
            dayLow = minOf(unnormLow, unnormPrice, BASELINE_SCORE),
            sparkline = unnormSparkline
        )
    }

    /**
     * Computes the Normalized 100-baseline PORTFOLIO [IndexQuote] (and its intraday sparkline curve)
     * from fetched ticker data.
     */
    fun computePortfolioQuote(
        dataByYahooTicker: Map<String, TickerIntradayData>,
        timestampMillis: Long,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): IndexQuote {
        val selectedHoldings = activeHoldings(disabledHoldingIds, customWeights)
            .ifEmpty { resolvedHoldings(customWeights) }
        val availableHoldings = selectedHoldings.filter { holding ->
            val data = dataByYahooTicker[holding.yahooTicker]
            data != null && data.previousClose > 0.0
        }

        if (availableHoldings.isEmpty()) {
            return createDefaultFallbackPortfolioQuote(timestampMillis)
        }

        val weightSum = availableHoldings.sumOf { it.weightPercent }
        if (weightSum <= 0.0) {
            return createDefaultFallbackPortfolioQuote(timestampMillis)
        }
        val weightedReturnPct = availableHoldings.sumOf { holding ->
            val data = dataByYahooTicker.getValue(holding.yahooTicker)
            val pctReturn = ((data.currentPrice - data.previousClose) / data.previousClose) * 100.0
            holding.weightPercent * pctReturn
        } / weightSum

        val currentScore = round2(BASELINE_SCORE + weightedReturnPct)
        val currentDelta = round2(weightedReturnPct)

        // Build intraday curve across time buckets
        val maxBars = availableHoldings.maxOfOrNull { holding ->
            dataByYahooTicker[holding.yahooTicker]?.intradayCloses?.size ?: 0
        } ?: 0

        val rawSeries = mutableListOf(BASELINE_SCORE)
        if (maxBars > 0) {
            for (barIdx in 0 until maxBars) {
                var barWeightedSum = 0.0
                var barWeightTotal = 0.0
                for (holding in availableHoldings) {
                    val data = dataByYahooTicker.getValue(holding.yahooTicker)
                    val series = data.intradayCloses
                    if (series.isNotEmpty() && data.previousClose > 0.0) {
                        val mappedIdx = ((barIdx.toDouble() / maxBars) * series.size)
                            .toInt()
                            .coerceIn(0, series.lastIndex)
                        val barPrice = series[mappedIdx]
                        val barRet = ((barPrice - data.previousClose) / data.previousClose) * 100.0
                        barWeightedSum += holding.weightPercent * barRet
                        barWeightTotal += holding.weightPercent
                    }
                }
                if (barWeightTotal > 0.0) {
                    rawSeries.add(round2(BASELINE_SCORE + (barWeightedSum / barWeightTotal)))
                }
            }
        }
        // Always end with the exact current score
        if (rawSeries.lastOrNull() != currentScore) {
            rawSeries.add(currentScore)
        }

        val sparkline = downsample(rawSeries, MAX_SPARKLINE_POINTS)
        val dayHigh = sparkline.maxOrNull() ?: maxOf(BASELINE_SCORE, currentScore)
        val dayLow = sparkline.minOrNull() ?: minOf(BASELINE_SCORE, currentScore)

        return IndexQuote(
            symbol = PORTFOLIO_SYMBOL,
            name = "Portfolio Score",
            price = currentScore,
            change = currentDelta,
            percentChange = currentDelta,
            dayHigh = dayHigh,
            dayLow = dayLow,
            lastUpdated = timestampMillis,
            category = "PORTFOLIO",
            sparkline = sparkline
        )
    }

    fun createDefaultFallbackPortfolioQuote(timestampMillis: Long): IndexQuote {
        val defaultSparkline = listOf(
            100.00, 100.12, 99.94, 100.08, 100.24,
            100.19, 100.38, 100.52, 100.46, 100.68,
            100.61, 100.84
        )
        return IndexQuote(
            symbol = PORTFOLIO_SYMBOL,
            name = "Portfolio Score",
            price = 100.84,
            change = 0.84,
            percentChange = 0.84,
            dayHigh = 100.84,
            dayLow = 99.94,
            lastUpdated = timestampMillis,
            category = "PORTFOLIO",
            sparkline = defaultSparkline
        )
    }

    private fun downsample(points: List<Double>, maxPoints: Int): List<Double> {
        if (points.size <= maxPoints) return points
        val result = ArrayList<Double>(maxPoints)
        result.add(points.first())
        val innerCount = maxPoints - 2
        for (i in 1..innerCount) {
            val idx = ((i.toDouble() / (maxPoints - 1)) * (points.size - 1)).toInt()
            result.add(points[idx])
        }
        result.add(points.last())
        return result
    }

    private fun round2(value: Double): Double = round(value * 100.0) / 100.0
}
