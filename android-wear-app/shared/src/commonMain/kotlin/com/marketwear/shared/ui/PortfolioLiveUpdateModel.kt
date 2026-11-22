package com.marketwear.shared.ui

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.round
import kotlin.math.roundToInt

/**
 * Momentum band derived from the daily portfolio percentage return relative to the 100.00 baseline.
 */
@Serializable
enum class PortfolioMomentumBand {
    STRONG_GAIN,
    MODERATE_GAIN,
    MILD_GAIN,
    FLAT,
    MILD_LOSS,
    MODERATE_LOSS,
    STRONG_LOSS
}

/**
 * Colored segment within the 0..1000 Android 16 ProgressStyle (or Compose Canvas) bipolar track,
 * where 500 (50%) marks the 100.00 morning baseline.
 */
@Serializable
data class BipolarProgressSegment(
    val length: Int,
    val argbColor: Int
)

/**
 * Platform-agnostic visual specification for the center-origin bipolar daily portfolio fluctuation
 * indicator, shared across Android 16 Progress-Centric Notifications and Compose Multiplatform UI.
 */
@Serializable
data class PortfolioLiveUpdateSpec(
    val progress: Int,
    val maxProgress: Int = PortfolioLiveUpdateFormatter.MAX_PROGRESS,
    val baselinePoint: Int = PortfolioLiveUpdateFormatter.MIDPOINT,
    val intensity: Float,
    val trendDirection: TrendDirection,
    val momentumBand: PortfolioMomentumBand,
    val accentArgb: Int,
    val segments: List<BipolarProgressSegment>,
    val shortCriticalText: String,
    val title: String,
    val subtitle: String,
    val baselineLabel: String = "100.0"
)

/**
 * Pure KMP builder converting an [IndexQuote] (normalized or raw portfolio score) into a
 * center-origin bipolar [PortfolioLiveUpdateSpec] using a smooth log-linear (symlog) scale:
 * gains grow rightward from center (500) in intensity-scaled green, while losses grow
 * leftward from center (500) in intensity-scaled red across an unbounded range.
 */
object PortfolioLiveUpdateFormatter {
    const val MAX_PROGRESS: Int = 1000
    const val MIDPOINT: Int = 500

    const val LOG_LINEAR_KNEE_PERCENT: Double = 1.0
    const val LOG_REFERENCE_PERCENT: Double = 25.0
    const val LOG_REFERENCE_FRACTION: Double = 0.90
    val LOG_SCALE_TICK_PERCENTS: List<Double> = listOf(1.0, 5.0, 25.0)

    private val LOG_REFERENCE_U: Double = ln(1.0 + LOG_REFERENCE_PERCENT / LOG_LINEAR_KNEE_PERCENT)
    private val TAIL_DECAY_ALPHA: Double =
        LOG_REFERENCE_FRACTION / ((1.0 - LOG_REFERENCE_FRACTION) * LOG_REFERENCE_U)

    const val TRACK_DARK: Int = 0xFF141C27.toInt()
    const val BASELINE_POINT_WHITE: Int = 0xFFE2E8F0.toInt()
    const val NEUTRAL_ACCENT: Int = 0xFF94A3B8.toInt()

    private const val GREEN_MILD: Int = 0xFF4ADE80.toInt()
    private const val GREEN_MID: Int = 0xFF10B981.toInt()
    private const val GREEN_STRONG: Int = 0xFF00E676.toInt()

    private const val RED_MILD: Int = 0xFFFCA5A5.toInt()
    private const val RED_MID: Int = 0xFFEF4444.toInt()
    private const val RED_STRONG: Int = 0xFFDC2626.toInt()

    private const val FLAT_EPSILON_PERCENT = 0.005

    /**
     * Maps an absolute percentage change `[0, +∞)` onto `[0.0, 1.0]` of a single half-wing
     * using a C¹-smooth log-linear curve: near-linear for small intraday moves (`~1%`),
     * `~0.50` at `5%`, `0.90` at `25%`, and asymptotically approaching `1.0` for larger swings.
     */
    fun logLinearHalfFraction(absPercentChange: Double): Double {
        if (absPercentChange.isNaN() || absPercentChange <= 0.0) return 0.0
        if (absPercentChange.isInfinite()) return 1.0
        val u = ln(1.0 + absPercentChange / LOG_LINEAR_KNEE_PERCENT)
        return if (u <= LOG_REFERENCE_U) {
            (LOG_REFERENCE_FRACTION * (u / LOG_REFERENCE_U)).coerceIn(0.0, 1.0)
        } else {
            val tail = 1.0 - exp(-TAIL_DECAY_ALPHA * (u - LOG_REFERENCE_U))
            (LOG_REFERENCE_FRACTION + (1.0 - LOG_REFERENCE_FRACTION) * tail).coerceIn(0.0, 1.0)
        }
    }

    fun buildSpec(
        activeQuote: IndexQuote,
        enabledHoldingsCount: Int = PortfolioRegistry.HOLDINGS.size,
        totalHoldingsCount: Int = PortfolioRegistry.HOLDINGS.size
    ): PortfolioLiveUpdateSpec {
        val pct = activeQuote.percentChange
        val absPct = abs(pct)
        val halfFraction = logLinearHalfFraction(absPct)
        val intensity = (halfFraction / logLinearHalfFraction(5.0)).coerceIn(0.0, 1.0).toFloat()

        val progress: Int
        val trendDirection: TrendDirection
        val momentumBand: PortfolioMomentumBand
        val accentArgb: Int
        val segments: List<BipolarProgressSegment>

        when {
            absPct < FLAT_EPSILON_PERCENT -> {
                progress = MIDPOINT
                trendDirection = TrendDirection.POSITIVE
                momentumBand = PortfolioMomentumBand.FLAT
                accentArgb = NEUTRAL_ACCENT
                segments = listOf(
                    BipolarProgressSegment(MIDPOINT, TRACK_DARK),
                    BipolarProgressSegment(MAX_PROGRESS - MIDPOINT, TRACK_DARK)
                )
            }

            pct > 0.0 -> {
                val deltaUnits = round(halfFraction * MIDPOINT).toInt().coerceIn(1, MIDPOINT)
                progress = MIDPOINT + deltaUnits
                trendDirection = TrendDirection.POSITIVE
                momentumBand = when {
                    intensity >= 0.65f -> PortfolioMomentumBand.STRONG_GAIN
                    intensity >= 0.25f -> PortfolioMomentumBand.MODERATE_GAIN
                    else -> PortfolioMomentumBand.MILD_GAIN
                }
                accentArgb = if (intensity <= 0.5f) {
                    lerpArgb(GREEN_MILD, GREEN_MID, intensity / 0.5f)
                } else {
                    lerpArgb(GREEN_MID, GREEN_STRONG, (intensity - 0.5f) / 0.5f)
                }
                // Center-origin rightward gain: dark left wing (0..500), green gain (500..progress), dark tail
                segments = buildList {
                    add(BipolarProgressSegment(MIDPOINT, TRACK_DARK))
                    add(BipolarProgressSegment(deltaUnits, accentArgb))
                    val remainingLen = MAX_PROGRESS - progress
                    if (remainingLen > 0) add(BipolarProgressSegment(remainingLen, TRACK_DARK))
                }
            }

            else -> {
                val deltaUnits = round(halfFraction * MIDPOINT).toInt().coerceIn(1, MIDPOINT)
                progress = MIDPOINT - deltaUnits
                trendDirection = TrendDirection.NEGATIVE
                momentumBand = when {
                    intensity >= 0.65f -> PortfolioMomentumBand.STRONG_LOSS
                    intensity >= 0.25f -> PortfolioMomentumBand.MODERATE_LOSS
                    else -> PortfolioMomentumBand.MILD_LOSS
                }
                accentArgb = if (intensity <= 0.5f) {
                    lerpArgb(RED_MILD, RED_MID, intensity / 0.5f)
                } else {
                    lerpArgb(RED_MID, RED_STRONG, (intensity - 0.5f) / 0.5f)
                }
                // Center-origin leftward loss: dark left remainder (0..progress), red loss (progress..500), dark right wing
                segments = buildList {
                    if (progress > 0) add(BipolarProgressSegment(progress, TRACK_DARK))
                    add(BipolarProgressSegment(deltaUnits, accentArgb))
                    add(BipolarProgressSegment(MAX_PROGRESS - MIDPOINT, TRACK_DARK))
                }
            }
        }

        val shortCriticalText = formatShortCriticalPercent(pct)
        val title = "Portfolio ${activeQuote.formattedScore} (${activeQuote.formattedPercent})"
        val subtitle = "$enabledHoldingsCount/$totalHoldingsCount holdings · " +
            "Range ${formatTwoDecimals(activeQuote.dayLow)}–${formatTwoDecimals(activeQuote.dayHigh)} · Center 100.0 · Log scale"

        return PortfolioLiveUpdateSpec(
            progress = progress,
            maxProgress = MAX_PROGRESS,
            baselinePoint = MIDPOINT,
            intensity = intensity,
            trendDirection = trendDirection,
            momentumBand = momentumBand,
            accentArgb = accentArgb,
            segments = segments,
            shortCriticalText = shortCriticalText,
            title = title,
            subtitle = subtitle
        )
    }

    internal fun formatShortCriticalPercent(percentChange: Double): String {
        if (abs(percentChange) < FLAT_EPSILON_PERCENT) return "0.00%"
        val sign = if (percentChange > 0.0) "+" else "-"
        val absVal = abs(percentChange)
        return if (absVal >= 10.0) {
            val rounded1 = round(absVal * 10.0) / 10.0
            val whole = rounded1.toInt()
            val frac = ((rounded1 - whole) * 10.0).roundToInt()
            "$sign$whole.$frac%"
        } else {
            val rounded2 = round(absVal * 100.0) / 100.0
            val whole = rounded2.toInt()
            val frac = ((rounded2 - whole) * 100.0).roundToInt().coerceIn(0, 99)
            "$sign$whole.${frac.toString().padStart(2, '0')}%"
        }
    }

    private fun formatTwoDecimals(value: Double): String {
        val rounded = round(value * 100.0) / 100.0
        val sign = if (rounded < 0.0) "-" else ""
        val absVal = abs(rounded)
        val whole = absVal.toInt()
        val frac = ((absVal - whole) * 100.0).roundToInt().coerceIn(0, 99)
        return "$sign$whole.${frac.toString().padStart(2, '0')}"
    }

    internal fun lerpArgb(startArgb: Int, endArgb: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        val a = lerpChannel((startArgb ushr 24) and 0xFF, (endArgb ushr 24) and 0xFF, t)
        val r = lerpChannel((startArgb ushr 16) and 0xFF, (endArgb ushr 16) and 0xFF, t)
        val g = lerpChannel((startArgb ushr 8) and 0xFF, (endArgb ushr 8) and 0xFF, t)
        val b = lerpChannel(startArgb and 0xFF, endArgb and 0xFF, t)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun lerpChannel(start: Int, end: Int, t: Float): Int =
        (start + ((end - start) * t).roundToInt()).coerceIn(0, 255)
}
