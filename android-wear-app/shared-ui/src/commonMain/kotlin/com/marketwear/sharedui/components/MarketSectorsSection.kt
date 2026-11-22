package com.marketwear.sharedui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.ui.SectorDisplayMode
import com.marketwear.shared.ui.SectorSortOrder
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

fun sortSectors(
    sectors: List<IndexQuote>,
    sortOrder: SectorSortOrder
): List<IndexQuote> = when (sortOrder) {
    SectorSortOrder.TOP_GAINERS -> sectors.sortedByDescending { it.percentChange }
    SectorSortOrder.TOP_LOSERS -> sectors.sortedBy { it.percentChange }
    SectorSortOrder.ALPHABETICAL -> sectors.sortedBy { it.name }
}

@Composable
fun SectorBreadthBanner(
    sectors: List<IndexQuote>,
    modifier: Modifier = Modifier
) {
    val advancing = sectors.count { it.percentChange >= 0.0 }
    val declining = sectors.size - advancing
    val avgReturn = if (sectors.isNotEmpty()) sectors.map { it.percentChange }.average() else 0.0
    val avgTrend = TrendDirection.fromChange(avgReturn)
    val leader = sectors.maxByOrNull { it.percentChange }
    val laggard = sectors.minByOrNull { it.percentChange }
    val total = sectors.size.coerceAtLeast(1)
    val advRatio = advancing.toFloat() / total.toFloat()

    Card(
        shape = MarketWearShapes.MetricCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(MarketWearDimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = "US Sector Breadth",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$advancing Advancing · $declining Declining across ${sectors.size} sectors",
                        style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                DeltaPill(
                    text = "Avg ${formatSignedPct(avgReturn)}",
                    trendDirection = avgTrend
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            ) {
                if (advRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(advRatio.coerceAtLeast(0.01f))
                            .height(6.dp)
                            .background(MarketWearPalette.PositiveTrend)
                    )
                }
                val decRatio = 1f - advRatio
                if (decRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(decRatio.coerceAtLeast(0.01f))
                            .height(6.dp)
                            .background(MarketWearPalette.NegativeTrend)
                    )
                }
            }

            if (leader != null && laggard != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lead: ${leader.name} (${leader.formattedPercent})",
                        style = MarketWearTextStyles.TabularSmall,
                        color = MarketWearPalette.PositiveTrend,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lag: ${laggard.name} (${laggard.formattedPercent})",
                        style = MarketWearTextStyles.TabularSmall,
                        color = MarketWearPalette.NegativeTrend,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectorControlBar(
    displayMode: SectorDisplayMode,
    sortOrder: SectorSortOrder,
    onSelectDisplayMode: (SectorDisplayMode) -> Unit,
    onSelectSortOrder: (SectorSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            SectorSortOrder.entries.forEach { order ->
                FilterChip(
                    selected = sortOrder == order,
                    onClick = { onSelectSortOrder(order) },
                    label = {
                        Text(
                            text = order.label,
                            fontSize = MarketWearTextSizes.FilterChip,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SectorDisplayMode.entries.forEach { mode ->
                val selected = displayMode == mode
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onSelectDisplayMode(mode) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun SectorHeatmapTile(
    quote: IndexQuote,
    modifier: Modifier = Modifier
) {
    val trendDirection = TrendDirection.fromChange(quote.change)
    val accent = when (trendDirection) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }
    val intensity = (abs(quote.percentChange) / 2.2).toFloat().coerceIn(0.18f, 0.92f)
    val tintBg = accent.copy(alpha = intensity * 0.24f)

    Surface(
        shape = MarketWearShapes.MetricCard,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.42f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tintBg)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.symbol,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "L ${quote.formattedDayLow} / H ${quote.formattedDayHigh}",
                    style = MarketWearTextStyles.TabularSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = quote.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = quote.formattedPercent,
                style = MarketWearTextStyles.TabularMetric.copy(fontSize = 20.sp),
                color = accent,
                maxLines = 1
            )
            Text(
                text = quote.formattedPoints,
                style = MarketWearTextStyles.TabularSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (quote.sparkline.size >= 2) {
                Spacer(modifier = Modifier.height(8.dp))
                MiniSparkline(
                    points = quote.sparkline,
                    trendDirection = trendDirection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                )
            }
        }
    }
}

private fun formatSignedPct(value: Double): String {
    val sign = if (value >= 0.0) "+" else "-"
    val rounded = round(abs(value) * 100.0) / 100.0
    val whole = rounded.toInt()
    val frac = ((rounded - whole) * 100.0).roundToInt().coerceIn(0, 99)
    return "$sign$whole.${frac.toString().padStart(2, '0')}%"
}
