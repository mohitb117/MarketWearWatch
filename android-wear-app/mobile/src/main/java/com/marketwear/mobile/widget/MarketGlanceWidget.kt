package com.marketwear.mobile.widget

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
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
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.glance.cartesian.CartesianChartImage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketGlanceWidget : GlanceAppWidget(), KoinComponent {

    private val repository: MobileMarketRepository by inject()

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val quotes = repository.quotes.value.ifEmpty {
            MarketRegistry.getFallbackQuotes()
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

    val portfolioQuote = quotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }
        ?: PortfolioRegistry.createDefaultFallbackPortfolioQuote(lastUpdated)
    val indices = quotes.filter { it.symbol == "SPX" || it.symbol == "NDX" }
    val sectors = quotes.filter {
        it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL && it.symbol != "SPX" && it.symbol != "NDX"
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

        // Scrollable List: Portfolio Score + Major Indices + Sector Levels
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            item {
                PortfolioWidgetItem(normalizedQuote = portfolioQuote)
                Spacer(modifier = GlanceModifier.height(6.dp))
            }

            items(indices) { quote ->
                IndexItem(quote = quote)
                Spacer(modifier = GlanceModifier.height(4.dp))
            }

            if (sectors.isNotEmpty()) {
                item {
                    Text(
                        text = "Sectors",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF9E9E9E)),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }

                items(sectors) { quote ->
                    IndexItem(quote = quote)
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

private val WidgetPortfolioRangeProvider = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: com.patrykandpatrick.vico.compose.common.data.ExtraStore): Double =
        minOf(minY, 100.0) - 0.08

    override fun getMaxY(minY: Double, maxY: Double, extraStore: com.patrykandpatrick.vico.compose.common.data.ExtraStore): Double =
        maxOf(maxY, 100.0) + 0.08
}

@androidx.compose.runtime.Composable
private fun PortfolioWidgetItem(normalizedQuote: IndexQuote) {
    val unnormalizedQuote = PortfolioRegistry.toUnnormalizedQuote(normalizedQuote)
    val isPositive = normalizedQuote.change >= 0
    val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    val pts = if (normalizedQuote.sparkline.size >= 2) normalizedQuote.sparkline else listOf(100.0, 100.0)
    val secPts = if (unnormalizedQuote.sparkline.size >= 2) unnormalizedQuote.sparkline else pts

    val chartModel = remember(pts, secPts) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                series(pts)
                series(secPts)
            }
        )
    }

    val primaryLine = LineCartesianLayer.rememberLine(
        fill = remember(trendColor) { LineCartesianLayer.LineFill.single(Fill(trendColor)) },
        stroke = remember { LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp, cap = StrokeCap.Round) }
    )
    val secondaryLine = LineCartesianLayer.rememberLine(
        fill = remember { LineCartesianLayer.LineFill.single(Fill(Color(0x8864B5F6))) },
        stroke = remember {
            LineCartesianLayer.LineStroke.Dashed(
                thickness = 1.25.dp,
                cap = StrokeCap.Round,
                dashLength = 4.dp,
                gapLength = 4.dp
            )
        }
    )
    val baselineLine = rememberLineComponent(
        fill = Fill(Color(0xFF546E7A)),
        thickness = 1.dp
    )
    val baselineDecoration = remember(baselineLine) {
        HorizontalLine(
            y = { 100.0 },
            line = baselineLine
        )
    }

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF152232))
            .cornerRadius(10.dp)
            .padding(8.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Portfolio Score (100%)",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF90CAF9)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "${normalizedQuote.formattedScore} (${normalizedQuote.formattedPercent})",
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
                text = "Raw (55.4%): ${unnormalizedQuote.formattedScore}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFB0BEC5)),
                    fontSize = 10.sp
                )
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = unnormalizedQuote.formattedPercent,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFB0BEC5)),
                    fontSize = 10.sp
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(4.dp))

        CartesianChartImage(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(primaryLine, secondaryLine),
                    rangeProvider = WidgetPortfolioRangeProvider
                ),
                decorations = listOf(baselineDecoration)
            ),
            model = chartModel,
            contentDescription = "Portfolio intraday score chart",
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(34.dp)
        )
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
