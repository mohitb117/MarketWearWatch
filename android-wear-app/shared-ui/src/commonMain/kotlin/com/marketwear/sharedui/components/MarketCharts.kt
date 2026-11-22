package com.marketwear.sharedui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import kotlin.math.roundToInt

/**
 * Interactive dual-curve Portfolio Daily Score Canvas chart:
 * - Smooth cubic Bezier primary curve with vertical alpha gradient fill
 * - Dashed comparison curve comparing Normalized (100%) vs Raw basket
 * - Dashed 100.00 morning baseline rule
 * - Touch/pointer scrubbing crosshair with glowing focus ring
 */
@Composable
fun InteractivePortfolioChart(
    primaryPoints: List<Double>,
    secondaryPoints: List<Double>,
    trendDirection: TrendDirection,
    scrubbedIndex: Int?,
    onScrubIndexChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = MarketWearDimens.DefaultChartHeight,
    baselineValue: Double = 100.0
) {
    val primaryColor = when (trendDirection) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }
    val secondaryColor = MarketWearPalette.SecondaryCurve
    val baselineColor = MarketWearPalette.BaselineRule

    val safePrimary = if (primaryPoints.size >= 2) primaryPoints else listOf(baselineValue, baselineValue)
    val safeSecondary = if (secondaryPoints.size >= 2) secondaryPoints else safePrimary

    val allValues = safePrimary + safeSecondary + baselineValue
    val rawMin = allValues.minOrNull() ?: (baselineValue - 0.5)
    val rawMax = allValues.maxOrNull() ?: (baselineValue + 0.5)
    val spread = (rawMax - rawMin).coerceAtLeast(0.18)
    val minY = rawMin - spread * 0.14
    val maxY = rawMax + spread * 0.14

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .pointerInput(safePrimary) {
                detectHorizontalDragGestures(
                    onDragEnd = { onScrubIndexChange(null) },
                    onDragCancel = { onScrubIndexChange(null) },
                    onHorizontalDrag = { change, _ ->
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val fraction = (change.position.x / widthPx).coerceIn(0f, 1f)
                        val idx = (fraction * safePrimary.lastIndex).roundToInt()
                            .coerceIn(0, safePrimary.lastIndex)
                        onScrubIndexChange(idx)
                    }
                )
            }
            .pointerInput(safePrimary) {
                detectTapGestures(
                    onPress = { offset ->
                        val widthPx = size.width.toFloat().coerceAtLeast(1f)
                        val fraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        val idx = (fraction * safePrimary.lastIndex).roundToInt()
                            .coerceIn(0, safePrimary.lastIndex)
                        onScrubIndexChange(idx)
                        tryAwaitRelease()
                        onScrubIndexChange(null)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val padTop = 10.dp.toPx()
        val padBottom = 10.dp.toPx()
        val usableH = (h - padTop - padBottom).coerceAtLeast(1f)

        fun yFor(value: Double): Float {
            val normalized = ((value - minY) / (maxY - minY)).toFloat().coerceIn(0f, 1f)
            return padTop + usableH * (1f - normalized)
        }

        // 1. Morning 100.00 dashed baseline
        val baselineY = yFor(baselineValue)
        drawLine(
            color = baselineColor.copy(alpha = 0.55f),
            start = Offset(0f, baselineY),
            end = Offset(w, baselineY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )

        // 2. Secondary dashed comparison curve
        val secondaryOffsets = safeSecondary.mapIndexed { idx, value ->
            val x = if (safeSecondary.size == 1) 0f else (idx.toFloat() / safeSecondary.lastIndex) * w
            Offset(x, yFor(value))
        }
        val secondaryPath = buildSmoothPath(secondaryOffsets)
        drawPath(
            path = secondaryPath,
            color = secondaryColor,
            style = Stroke(
                width = MarketWearDimens.SecondaryLineThickness.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
            )
        )

        // 3. Primary curve + gradient area fill
        val primaryOffsets = safePrimary.mapIndexed { idx, value ->
            val x = if (safePrimary.size == 1) 0f else (idx.toFloat() / safePrimary.lastIndex) * w
            Offset(x, yFor(value))
        }
        val primaryPath = buildSmoothPath(primaryOffsets)

        val areaPath = Path().apply {
            addPath(primaryPath)
            lineTo(primaryOffsets.last().x, h)
            lineTo(primaryOffsets.first().x, h)
            close()
        }
        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.28f),
                    primaryColor.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = padTop,
                endY = h
            )
        )

        drawPath(
            path = primaryPath,
            color = primaryColor,
            style = Stroke(
                width = MarketWearDimens.PrimaryLineThickness.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // 4. Focus indicator (scrubbed point or live tip dot)
        val focusIdx = scrubbedIndex?.coerceIn(0, primaryOffsets.lastIndex) ?: primaryOffsets.lastIndex
        val focusOffset = primaryOffsets[focusIdx]

        if (scrubbedIndex != null) {
            drawLine(
                color = primaryColor.copy(alpha = 0.55f),
                start = Offset(focusOffset.x, 0f),
                end = Offset(focusOffset.x, h),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        drawCircle(
            color = primaryColor.copy(alpha = 0.25f),
            radius = if (scrubbedIndex != null) 11.dp.toPx() else 7.dp.toPx(),
            center = focusOffset
        )
        drawCircle(
            color = primaryColor,
            radius = if (scrubbedIndex != null) 4.5.dp.toPx() else 3.5.dp.toPx(),
            center = focusOffset
        )
    }
}

/**
 * Lightweight vector micro-sparkline for index cards and sector heatmap tiles.
 */
@Composable
fun MiniSparkline(
    points: List<Double>,
    trendDirection: TrendDirection,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.8.dp
) {
    val color = when (trendDirection) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }
    val safePoints = if (points.size >= 2) points else listOf(1.0, 1.0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val minV = safePoints.minOrNull() ?: 0.0
        val maxV = safePoints.maxOrNull() ?: 1.0
        val range = (maxV - minV).coerceAtLeast(1e-6)
        val padY = 2.dp.toPx()
        val usableH = (h - padY * 2).coerceAtLeast(1f)

        val offsets = safePoints.mapIndexed { idx, v ->
            val x = (idx.toFloat() / safePoints.lastIndex) * w
            val norm = ((v - minV) / range).toFloat().coerceIn(0f, 1f)
            val y = padY + usableH * (1f - norm)
            Offset(x, y)
        }
        val path = buildSmoothPath(offsets)

        val fillPath = Path().apply {
            addPath(path)
            lineTo(offsets.last().x, h)
            lineTo(offsets.first().x, h)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.22f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun buildSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }
    for (i in 0 until points.lastIndex) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val midX = (p0.x + p1.x) / 2f
        path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
    }
    return path
}
