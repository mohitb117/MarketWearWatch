package com.marketwear.mobile.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.marketwear.mobile.MainActivity
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.shared.model.IndexQuote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val quotes = MobileMarketRepository.getInstance().quotes.value.ifEmpty {
            listOf(
                IndexQuote(
                    symbol = "SPX",
                    name = "S&P 500",
                    price = 5985.42,
                    change = 22.84,
                    percentChange = 0.38,
                    dayHigh = 6005.10,
                    dayLow = 5960.30,
                    lastUpdated = System.currentTimeMillis()
                ),
                IndexQuote(
                    symbol = "NDX",
                    name = "Nasdaq 100",
                    price = 21110.15,
                    change = -45.20,
                    percentChange = -0.21,
                    dayHigh = 21250.00,
                    dayLow = 21050.80,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }

        provideContent {
            WidgetContent(quotes = quotes)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(quotes: List<IndexQuote>) {
    val lastUpdated = quotes.maxOfOrNull { it.lastUpdated } ?: 0L
    val timeStr = if (lastUpdated > 0L) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(lastUpdated))
    } else {
        "--"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .cornerRadius(16.dp)
            .padding(10.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Market Watch",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF9E9E9E)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = timeStr,
                style = TextStyle(
                    color = ColorProvider(Color(0xFF757575)),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        // Index Cards
        quotes.take(2).forEachIndexed { index, quote ->
            if (index > 0) {
                Spacer(modifier = GlanceModifier.height(6.dp))
            }
            IndexItem(quote = quote)
        }
    }
}

@androidx.compose.runtime.Composable
private fun IndexItem(quote: IndexQuote) {
    val isPositive = quote.change >= 0
    val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E))
            .cornerRadius(10.dp)
            .padding(8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quote.name,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = quote.formattedPercent,
                style = TextStyle(
                    color = ColorProvider(trendColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(2.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = quote.formattedPoints,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = quote.formattedChange,
                style = TextStyle(
                    color = ColorProvider(trendColor),
                    fontSize = 11.sp
                )
            )
        }
    }
}
