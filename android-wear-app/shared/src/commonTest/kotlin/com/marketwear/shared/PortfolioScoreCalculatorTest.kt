package com.marketwear.shared

import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortfolioScoreCalculatorTest {

    @Test
    fun holdingsListMatches37PositionsAnd59Point41PercentTotalWeight() {
        assertEquals(37, PortfolioRegistry.HOLDINGS.size)
        assertEquals(59.41, PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT, 0.001)
    }

    @Test
    fun flatMarketProducesExact100ScoreInBothWeightModes() {
        val data = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.associateWith { ticker ->
            TickerIntradayData(
                yahooTicker = ticker,
                previousClose = 100.0,
                currentPrice = 100.0,
                intradayCloses = listOf(100.0, 100.0, 100.0)
            )
        }
        val normQuote = PortfolioRegistry.computePortfolioQuote(data, 1700000000000L)
        assertEquals(100.0, normQuote.price, 0.001)
        assertEquals(0.0, normQuote.change, 0.001)

        val unnormQuote = PortfolioRegistry.toUnnormalizedQuote(normQuote)
        assertEquals(100.0, unnormQuote.price, 0.001)
        assertEquals(0.0, unnormQuote.change, 0.001)
    }

    @Test
    fun uniformUpwardAndDownwardMovesProduceExpectedNormalizedAndUnnormalizedScores() {
        // +1.0% across all holdings -> Normalized = 101.00, Unnormalized = 100.59
        val upData = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.associateWith { ticker ->
            TickerIntradayData(
                yahooTicker = ticker,
                previousClose = 100.0,
                currentPrice = 101.0,
                intradayCloses = listOf(100.0, 100.5, 101.0)
            )
        }
        val normUp = PortfolioRegistry.computePortfolioQuote(upData, 1700000000000L)
        assertEquals(101.0, normUp.price, 0.01)
        assertEquals(1.0, normUp.change, 0.01)
        assertTrue(normUp.sparkline.size >= 3)
        assertEquals(100.0, normUp.sparkline.first(), 0.01)
        assertEquals(101.0, normUp.sparkline.last(), 0.01)

        val unnormUp = PortfolioRegistry.toUnnormalizedQuote(normUp)
        assertEquals(100.59, unnormUp.price, 0.01)
        assertEquals(0.59, unnormUp.change, 0.01)

        // -1.0% across all holdings -> Normalized = 99.00, Unnormalized = 99.41
        val downData = PortfolioRegistry.UNIQUE_YAHOO_TICKERS.associateWith { ticker ->
            TickerIntradayData(
                yahooTicker = ticker,
                previousClose = 100.0,
                currentPrice = 99.0,
                intradayCloses = listOf(100.0, 99.5, 99.0)
            )
        }
        val normDown = PortfolioRegistry.computePortfolioQuote(downData, 1700000000000L)
        assertEquals(99.0, normDown.price, 0.01)
        assertEquals(-1.0, normDown.change, 0.01)

        val unnormDown = PortfolioRegistry.toUnnormalizedQuote(normDown)
        assertEquals(99.41, unnormDown.price, 0.01)
        assertEquals(-0.59, unnormDown.change, 0.01)
    }

    @Test
    fun marketUiStateBuilderTogglesWeightModesAndPrecomputesLabels() {
        val fallbackQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(1700000000000L)

        val normState = MarketUiStateBuilder.buildPortfolioCardState(
            normalizedQuote = fallbackQuote,
            weightMode = PortfolioWeightMode.NORMALIZED_100
        )
        assertEquals(PortfolioWeightMode.NORMALIZED_100, normState.weightMode)
        assertEquals(TrendDirection.POSITIVE, normState.trendDirection)
        assertEquals("Portfolio (100%)", normState.watchTitleLabel)
        assertEquals("Normalized (100%)", normState.mobileModePillLabel)
        assertEquals(fallbackQuote.price, normState.activeQuote.price, 0.001)
        assertTrue(normState.mobileSecondarySummary.contains("Raw (59.41%)"))

        val toggledMode = normState.weightMode.next()
        assertEquals(PortfolioWeightMode.RAW_PORTFOLIO, toggledMode)

        val rawState = MarketUiStateBuilder.buildPortfolioCardState(
            normalizedQuote = fallbackQuote,
            weightMode = toggledMode
        )
        assertEquals(PortfolioWeightMode.RAW_PORTFOLIO, rawState.weightMode)
        assertEquals("Portfolio (59.41%)", rawState.watchTitleLabel)
        assertEquals("Raw (59.41%)", rawState.mobileModePillLabel)
        assertEquals(normState.unnormalizedQuote.price, rawState.activeQuote.price, 0.001)
        assertEquals(fallbackQuote.price, rawState.secondaryQuote.price, 0.001)
        assertTrue(rawState.mobileSecondarySummary.contains("Norm (100%)"))
    }

    @Test
    fun buildDashboardStatePartitionsPortfolioIndicesAndSectors() {
        val allQuotes = MarketRegistry.getFallbackQuotes()
        val dashboard = MarketUiStateBuilder.buildDashboardState(
            quotes = allQuotes,
            weightMode = PortfolioWeightMode.NORMALIZED_100
        )

        assertEquals("PORTFOLIO", dashboard.portfolioState.activeQuote.symbol)
        assertEquals(2, dashboard.indices.size)
        assertEquals(listOf("SPX", "NDX"), dashboard.indices.map { it.symbol })
        assertEquals(11, dashboard.sectors.size)
        assertTrue(dashboard.latestUpdatedMillis >= 0L)

        // Verify pre-computed IndexQuote range properties
        val spx = dashboard.indices.first()
        assertTrue(spx.dayRangeProgress in 0f..1f)
        assertEquals("5,960", spx.formattedDayLow)
        assertEquals("6,005", spx.formattedDayHigh)

        // Verify fallback sparkline when empty
        val emptySparklineQuote = dashboard.portfolioState.normalizedQuote.copy(sparkline = emptyList())
        val fallbackSparkState = MarketUiStateBuilder.buildPortfolioCardState(
            normalizedQuote = emptySparklineQuote,
            weightMode = PortfolioWeightMode.NORMALIZED_100
        )
        assertEquals(PortfolioCardUiState.DEFAULT_BASELINE_POINTS, fallbackSparkState.primarySparkline)
    }
}
