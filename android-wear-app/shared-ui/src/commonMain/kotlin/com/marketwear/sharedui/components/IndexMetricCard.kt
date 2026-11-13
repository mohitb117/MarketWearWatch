package com.marketwear.sharedui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTheme

@Composable
fun IndexMetricCard(
    quote: IndexQuote,
    modifier: Modifier = Modifier
) {
    val trendColor = when (TrendDirection.fromChange(quote.change)) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MarketWearShapes.MetricCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(MarketWearDimens.CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = quote.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = quote.symbol,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = quote.formattedPoints,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${quote.formattedChange} (${quote.formattedPercent})",
                        color = trendColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Low: ${quote.formattedDayLow}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "High: ${quote.formattedDayHigh}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(MarketWearDimens.TinySpacing))

            LinearProgressIndicator(
                progress = { quote.dayRangeProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MarketWearDimens.ProgressBarHeight),
                color = trendColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
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
