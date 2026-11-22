package com.marketwear.sharedui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioLiveUpdateSpec
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme

/**
 * Visual density mode for [PortfolioFluctuationBar].
 */
enum class FluctuationBarDensity {
    STANDARD,
    COMPACT
}

/**
 * 50%-centered bipolar portfolio fluctuation bar mirroring the Android 16 Progress-Centric
 * Notification: gains extend rightward above 50% in intensity-scaled green, while losses
 * extend leftward below 50% in intensity-scaled red.
 */
@Composable
fun PortfolioFluctuationBar(
    spec: PortfolioLiveUpdateSpec,
    modifier: Modifier = Modifier,
    density: FluctuationBarDensity = FluctuationBarDensity.STANDARD
) {
    val accentColor = Color(spec.accentArgb)
    val trackHeight = when (density) {
        FluctuationBarDensity.COMPACT -> 8.dp
        FluctuationBarDensity.STANDARD -> 10.dp
    }
    val canvasHeight = when (density) {
        FluctuationBarDensity.COMPACT -> 14.dp
        FluctuationBarDensity.STANDARD -> 18.dp
    }
    val dotDp = when (density) {
        FluctuationBarDensity.COMPACT -> 4.dp
        FluctuationBarDensity.STANDARD -> 5.dp
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val barH = trackHeight.toPx()
            val barTop = (h - barH) / 2f
            val corner = barH / 2f

            // Background rounded capsule
            drawRoundRect(
                color = MarketWearPalette.TrackBackground,
                topLeft = Offset(0f, barTop),
                size = Size(w, barH),
                cornerRadius = CornerRadius(corner, corner)
            )

            // Proportional bipolar segments clipped cleanly inside rounded capsule bounds
            val totalUnits = spec.segments.sumOf { it.length }.coerceAtLeast(1).toFloat()
            val pillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = 0f,
                        top = barTop,
                        right = w,
                        bottom = barTop + barH,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                )
            }
            clipPath(pillPath) {
                var cursorX = 0f
                for (segment in spec.segments) {
                    val segWidth = (segment.length / totalUnits) * w
                    if (segWidth > 0f) {
                        drawRect(
                            color = Color(segment.argbColor),
                            topLeft = Offset(cursorX, barTop),
                            size = Size(segWidth, barH)
                        )
                        cursorX += segWidth
                    }
                }
            }

            // Symmetric log-linear milestone ticks (±1%, ±5%, ±25%)
            val midX = w * (spec.baselinePoint.toFloat() / spec.maxProgress.coerceAtLeast(1))
            val scaleTickColor = Color(0xFF64748B).copy(alpha = 0.45f)
            val scaleTickStroke = 1.dp.toPx()
            for (tickPct in PortfolioLiveUpdateFormatter.LOG_SCALE_TICK_PERCENTS) {
                val halfFrac = PortfolioLiveUpdateFormatter.logLinearHalfFraction(tickPct).toFloat()
                val leftTickX = midX * (1f - halfFrac)
                val rightTickX = midX * (1f + halfFrac)
                drawLine(
                    color = scaleTickColor,
                    start = Offset(leftTickX, barTop),
                    end = Offset(leftTickX, barTop + barH),
                    strokeWidth = scaleTickStroke
                )
                drawLine(
                    color = scaleTickColor,
                    start = Offset(rightTickX, barTop),
                    end = Offset(rightTickX, barTop + barH),
                    strokeWidth = scaleTickStroke
                )
            }

            // Crisp 50% morning baseline tick
            val tickWidth = 2.dp.toPx()
            drawLine(
                color = MarketWearPalette.BaselineTick,
                start = Offset(midX, (barTop - 2.dp.toPx()).coerceAtLeast(0f)),
                end = Offset(midX, (barTop + barH + 2.dp.toPx()).coerceAtMost(h)),
                strokeWidth = tickWidth
            )

            // Active tracker indicator halo + core dot
            val trackerX = (w * (spec.progress.toFloat() / spec.maxProgress.coerceAtLeast(1)))
                .coerceIn(barH / 2f, (w - barH / 2f).coerceAtLeast(barH / 2f))
            val centerY = h / 2f
            val dotRadius = dotDp.toPx()
            drawCircle(
                color = accentColor.copy(alpha = 0.28f),
                radius = dotRadius * 1.55f,
                center = Offset(trackerX, centerY)
            )
            drawCircle(
                color = accentColor,
                radius = dotRadius,
                center = Offset(trackerX, centerY)
            )
        }

        if (density == FluctuationBarDensity.STANDARD) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "-25%+",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "-5%",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "0% · ${spec.baselineLabel}",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+5%",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "+25%+",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
fun PortfolioFluctuationBarPreview() {
    val quote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L)
    val spec = PortfolioLiveUpdateFormatter.buildSpec(quote)
    MarketWearTheme {
        PortfolioFluctuationBar(spec = spec)
    }
}
