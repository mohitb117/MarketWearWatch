package com.marketwear.sharedui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme

@Composable
fun IndexMetricCard(
    quote: IndexQuote,
    modifier: Modifier = Modifier
) {
    val trendDirection = TrendDirection.fromChange(quote.change)
    val trendColor = when (trendDirection) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MarketWearShapes.MetricCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(MarketWearDimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = MarketWearDimens.SmallSpacing)
                ) {
                    Text(
                        text = quote.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = quote.symbol,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = quote.formattedPoints,
                        style = MaterialTheme.typography.titleLarge.merge(MarketWearTextStyles.TabularMetric),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    DeltaPill(
                        text = "${quote.formattedChange} (${quote.formattedPercent})",
                        trendDirection = trendDirection
                    )
                }
            }

            if (quote.sparkline.size >= 2) {
                Spacer(modifier = Modifier.height(10.dp))
                MiniSparkline(
                    points = quote.sparkline,
                    trendDirection = trendDirection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "L ${quote.formattedDayLow}",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "H ${quote.formattedDayHigh}",
                    style = MaterialTheme.typography.labelSmall.merge(MarketWearTextStyles.Tabular),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            val progressFraction = quote.dayRangeProgress.coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas
                val barH = MarketWearDimens.ProgressBarHeight.toPx()
                val top = (h - barH) / 2f
                val corner = barH / 2f

                drawRoundRect(
                    color = MarketWearPalette.TrackBackground,
                    topLeft = Offset(0f, top),
                    size = Size(w, barH),
                    cornerRadius = CornerRadius(corner, corner)
                )

                val fillW = (w * progressFraction).coerceAtLeast(barH)
                drawRoundRect(
                    color = trendColor.copy(alpha = 0.8f),
                    topLeft = Offset(0f, top),
                    size = Size(fillW.coerceAtMost(w), barH),
                    cornerRadius = CornerRadius(corner, corner)
                )

                val dotX = (w * progressFraction).coerceIn(barH / 2f, (w - barH / 2f).coerceAtLeast(barH / 2f))
                drawCircle(
                    color = trendColor,
                    radius = 4.dp.toPx(),
                    center = Offset(dotX, h / 2f)
                )
            }
        }
    }
}

@Preview
@Composable
fun IndexMetricCardPreview() {
    val spx = MarketRegistry.getFallbackQuotes().first { it.symbol == "SPX" }
    MarketWearTheme {
        IndexMetricCard(quote = spx)
    }
}

@Preview
@Composable
fun SectorMetricCardPreview() {
    val tech = MarketRegistry.getFallbackQuotes().first { it.symbol == "^YH311" }
    MarketWearTheme {
        IndexMetricCard(quote = tech)
    }
}
