package com.marketwear.sharedui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marketwear.shared.ui.MarketSessionStatus
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearTextStyles

/**
 * Icon kind for KMP navigation tabs rendered without external icon-font dependencies.
 */
enum class NavGlyphKind {
    PULSE,
    HOLDINGS,
    SECTORS
}

@Composable
fun MarketSessionPill(
    status: MarketSessionStatus,
    modifier: Modifier = Modifier
) {
    val (bg, dot, fg) = when (status) {
        MarketSessionStatus.LIVE_MARKET -> Triple(
            MarketWearPalette.PositivePillBg,
            MarketWearPalette.PositiveTrend,
            MarketWearPalette.PositiveTrend
        )
        MarketSessionStatus.OFF_HOURS -> Triple(
            MarketWearPalette.AmberPillBg,
            MarketWearPalette.AmberAccent,
            MarketWearPalette.AmberAccent
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.badgeText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun DeltaPill(
    text: String,
    trendDirection: TrendDirection,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (trendDirection) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositivePillBg to MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativePillBg to MarketWearPalette.NegativeTrend
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Canvas(modifier = Modifier.size(9.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                when (trendDirection) {
                    TrendDirection.POSITIVE -> {
                        moveTo(w * 0.5f, h * 0.15f)
                        lineTo(w * 0.9f, h * 0.85f)
                        lineTo(w * 0.1f, h * 0.85f)
                    }
                    TrendDirection.NEGATIVE -> {
                        moveTo(w * 0.1f, h * 0.15f)
                        lineTo(w * 0.9f, h * 0.15f)
                        lineTo(w * 0.5f, h * 0.85f)
                    }
                }
                close()
            }
            drawPath(path = path, color = fg)
        }
        Text(
            text = text,
            style = MarketWearTextStyles.TabularSmall,
            color = fg,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun MarketNavGlyph(
    kind: NavGlyphKind,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        when (kind) {
            NavGlyphKind.PULSE -> {
                val path = Path().apply {
                    moveTo(w * 0.08f, h * 0.72f)
                    lineTo(w * 0.36f, h * 0.44f)
                    lineTo(w * 0.58f, h * 0.62f)
                    lineTo(w * 0.90f, h * 0.24f)
                }
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawCircle(
                    color = tint,
                    radius = 2.dp.toPx(),
                    center = Offset(w * 0.90f, h * 0.24f)
                )
            }
            NavGlyphKind.HOLDINGS -> {
                val barH = h * 0.18f
                val corner = CornerRadius(barH / 2f, barH / 2f)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.1f, h * 0.16f),
                    size = Size(w * 0.80f, barH),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = tint.copy(alpha = 0.82f),
                    topLeft = Offset(w * 0.1f, h * 0.43f),
                    size = Size(w * 0.58f, barH),
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = tint.copy(alpha = 0.64f),
                    topLeft = Offset(w * 0.1f, h * 0.70f),
                    size = Size(w * 0.40f, barH),
                    cornerRadius = corner
                )
            }
            NavGlyphKind.SECTORS -> {
                val boxSize = Size(w * 0.36f, h * 0.36f)
                val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                drawRoundRect(color = tint, topLeft = Offset(w * 0.10f, h * 0.10f), size = boxSize, cornerRadius = corner)
                drawRoundRect(color = tint, topLeft = Offset(w * 0.54f, h * 0.10f), size = boxSize, cornerRadius = corner)
                drawRoundRect(color = tint, topLeft = Offset(w * 0.10f, h * 0.54f), size = boxSize, cornerRadius = corner)
                drawRoundRect(color = tint.copy(alpha = 0.65f), topLeft = Offset(w * 0.54f, h * 0.54f), size = boxSize, cornerRadius = corner)
            }
        }
    }
}
