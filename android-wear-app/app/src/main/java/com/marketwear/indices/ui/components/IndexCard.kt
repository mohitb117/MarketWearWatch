package com.marketwear.indices.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.marketwear.indices.R
import com.marketwear.indices.data.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.TrendDirection

private val IndexWatchCardShape = RoundedCornerShape(16.dp)
private val IndexWatchCardBgColor = Color(0xFF101720)
private val IndexWatchBorderColor = Color(0xFF233040)
private val PositiveTrendWatchColor = Color(0xFF4ADE80)
private val NegativeTrendWatchColor = Color(0xFFF87171)
private val PositivePillWatchBg = Color(0xFF0F2D1E)
private val NegativePillWatchBg = Color(0xFF33151A)
private val IndexWatchTabularStyle = TextStyle(fontFeatureSettings = "tnum")

fun getInstrumentIcon(symbol: String): Int {
    return when (symbol) {
        "SPX" -> R.drawable.ic_sp500
        "NDX" -> R.drawable.ic_nasdaq
        "^YH101" -> R.drawable.ic_sector_materials
        "^YH102" -> R.drawable.ic_sector_cyclical
        "^YH103" -> R.drawable.ic_sector_financial
        "^YH104" -> R.drawable.ic_sector_realestate
        "^YH205" -> R.drawable.ic_sector_defensive
        "^YH206" -> R.drawable.ic_sector_healthcare
        "^YH207" -> R.drawable.ic_sector_utilities
        "^YH308" -> R.drawable.ic_sector_communication
        "^YH309" -> R.drawable.ic_sector_energy
        "^YH310" -> R.drawable.ic_sector_industrials
        "^YH311" -> R.drawable.ic_sector_technology
        else -> R.drawable.ic_trend_up
    }
}

@Composable
fun IndexCard(
    quote: IndexQuote,
    modifier: Modifier = Modifier
) {
    val isPositive = TrendDirection.fromChange(quote.change) == TrendDirection.POSITIVE
    val trendColor = if (isPositive) PositiveTrendWatchColor else NegativeTrendWatchColor
    val pillBgColor = if (isPositive) PositivePillWatchBg else NegativePillWatchBg
    val iconRes = remember(quote.symbol) { getInstrumentIcon(quote.symbol) }

    Card(
        onClick = { /* Tap */ },
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, IndexWatchBorderColor, IndexWatchCardShape),
        shape = IndexWatchCardShape,
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = IndexWatchCardBgColor,
            endBackgroundColor = IndexWatchCardBgColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = quote.name,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                    Text(
                        text = quote.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = quote.formattedPercent,
                    color = trendColor,
                    style = IndexWatchTabularStyle,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(pillBgColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = quote.formattedCompactPoints,
                    style = IndexWatchTabularStyle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = quote.formattedCompactChange,
                    color = trendColor,
                    style = IndexWatchTabularStyle,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Preview(
    name = "Wear OS Index Card (S&P 500)",
    widthDp = 200,
    heightDp = 80,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun IndexCardPreview() {
    val spx = MarketRegistry.getFallbackQuotes().first { it.symbol == "SPX" }
    MaterialTheme {
        IndexCard(quote = spx)
    }
}

@Preview(
    name = "Wear OS Sector Card (Technology)",
    widthDp = 200,
    heightDp = 80,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun SectorCardPreview() {
    val tech = MarketRegistry.getFallbackQuotes().first { it.symbol == "^YH311" }
    MaterialTheme {
        IndexCard(quote = tech)
    }
}
