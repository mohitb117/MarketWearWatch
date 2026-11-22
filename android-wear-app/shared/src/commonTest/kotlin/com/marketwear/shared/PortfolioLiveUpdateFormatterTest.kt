package com.marketwear.shared

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioMomentumBand
import com.marketwear.shared.ui.TrendDirection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PortfolioLiveUpdateFormatterTest {

    private fun portfolioQuoteWithPct(score: Double, pct: Double): IndexQuote {
        return IndexQuote(
            symbol = PortfolioRegistry.PORTFOLIO_SYMBOL,
            name = "Portfolio Score",
            price = score,
            change = score - 100.0,
            percentChange = pct,
            dayHigh = maxOf(score, 100.0),
            dayLow = minOf(score, 100.0),
            lastUpdated = 1700000000000L,
            category = "PORTFOLIO"
        )
    }

    @Test
    fun flatScoreMapsToExactMidpointAndBalancedSegments() {
        val spec = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(100.0, 0.0))
        assertEquals(500, spec.progress)
        assertEquals(PortfolioMomentumBand.FLAT, spec.momentumBand)
        assertEquals("0.00%", spec.shortCriticalText)
        assertEquals(2, spec.segments.size)
        assertEquals(500, spec.segments[0].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, spec.segments[0].argbColor)
        assertEquals(500, spec.segments[1].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, spec.segments[1].argbColor)
        assertEquals(1000, spec.segments.sumOf { it.length })
    }

    @Test
    fun logLinearScaleIsMonotonicSmoothAndUnbounded() {
        assertEquals(0.0, PortfolioLiveUpdateFormatter.logLinearHalfFraction(0.0))
        val samples = listOf(0.3, 1.5, 4.47, 10.0, 25.0, 50.0, 100.0, 500.0)
        val fractions = samples.map { PortfolioLiveUpdateFormatter.logLinearHalfFraction(it) }
        for (i in 0 until fractions.lastIndex) {
            assertTrue(
                fractions[i] < fractions[i + 1],
                "Expected strictly increasing fraction for ${samples[i]} < ${samples[i + 1]}"
            )
        }
        val fivePctFrac = PortfolioLiveUpdateFormatter.logLinearHalfFraction(5.0)
        assertTrue(fivePctFrac in 0.48..0.52, "Expected ~0.50 at 5%, got $fivePctFrac")
        val twentyFivePctFrac = PortfolioLiveUpdateFormatter.logLinearHalfFraction(25.0)
        assertEquals(0.90, twentyFivePctFrac, 0.0001)
    }

    @Test
    fun positiveMovesExtendRightFromCenterLineInGreen() {
        val spec = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(101.50, 1.50))
        assertTrue(spec.progress > 500)
        assertEquals(TrendDirection.POSITIVE, spec.trendDirection)
        assertEquals(PortfolioMomentumBand.MODERATE_GAIN, spec.momentumBand)
        assertEquals("+1.50%", spec.shortCriticalText)
        assertEquals(1000, spec.segments.sumOf { it.length })
        assertTrue(spec.segments.all { it.length > 0 })
        // Center-origin rightward gain: [0..500 dark], [500..progress green], [progress..1000 dark]
        assertEquals(3, spec.segments.size)
        assertEquals(500, spec.segments[0].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, spec.segments[0].argbColor)
        assertEquals(spec.progress - 500, spec.segments[1].length)
        assertEquals(spec.accentArgb, spec.segments[1].argbColor)
        assertEquals(1000 - spec.progress, spec.segments[2].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, spec.segments[2].argbColor)
    }

    @Test
    fun negativeMovesExtendLeftFromCenterLineWithIncreasingRedIntensity() {
        val mildDrop = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(99.70, -0.30))
        val deepDrop = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(97.60, -2.40))
        val screenshotDrop = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(95.53, -4.47))
        val severeDrop = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(94.96, -5.04))

        assertTrue(severeDrop.progress < screenshotDrop.progress)
        assertTrue(screenshotDrop.progress < deepDrop.progress)
        assertTrue(deepDrop.progress < mildDrop.progress)
        assertTrue(mildDrop.progress < 500)
        assertEquals(TrendDirection.NEGATIVE, deepDrop.trendDirection)
        assertEquals(PortfolioMomentumBand.STRONG_LOSS, screenshotDrop.momentumBand)
        assertEquals("-4.47%", screenshotDrop.shortCriticalText)
        assertEquals(1000, mildDrop.segments.sumOf { it.length })
        assertEquals(1000, deepDrop.segments.sumOf { it.length })
        assertEquals(1000, screenshotDrop.segments.sumOf { it.length })
        assertTrue(deepDrop.intensity > mildDrop.intensity)
        assertNotEquals(mildDrop.accentArgb, deepDrop.accentArgb)

        // Center-origin leftward loss: [0..progress dark], [progress..500 red], [500..1000 dark]
        assertEquals(3, screenshotDrop.segments.size)
        assertTrue(screenshotDrop.progress in 250..280)
        assertEquals(screenshotDrop.progress, screenshotDrop.segments[0].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, screenshotDrop.segments[0].argbColor)
        assertEquals(500 - screenshotDrop.progress, screenshotDrop.segments[1].length)
        assertEquals(screenshotDrop.accentArgb, screenshotDrop.segments[1].argbColor)
        assertEquals(500, screenshotDrop.segments[2].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, screenshotDrop.segments[2].argbColor)
    }

    @Test
    fun extremeUnboundedSwingsRemainWellFormedWithNonZeroSegments() {
        val gain100 = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(200.0, 100.0))
        val loss100 = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(0.0, -100.0))
        val gain500 = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(600.0, 500.0))
        val loss500 = PortfolioLiveUpdateFormatter.buildSpec(portfolioQuoteWithPct(-400.0, -500.0))

        assertTrue(gain100.progress in 990..1000)
        assertTrue(loss100.progress in 0..10)
        assertEquals(1000, gain500.progress)
        assertEquals(0, loss500.progress)

        listOf(gain100, loss100, gain500, loss500).forEach { spec ->
            assertEquals(1000, spec.segments.sumOf { it.length })
            assertTrue(spec.segments.all { it.length > 0 })
            assertTrue(spec.shortCriticalText.length <= 7)
        }

        // At full left saturation (-500%), red spans full left half (0..500) and right half remains dark
        assertEquals(2, loss500.segments.size)
        assertEquals(500, loss500.segments[0].length)
        assertEquals(loss500.accentArgb, loss500.segments[0].argbColor)
        assertEquals(500, loss500.segments[1].length)
        assertEquals(PortfolioLiveUpdateFormatter.TRACK_DARK, loss500.segments[1].argbColor)
    }
}
