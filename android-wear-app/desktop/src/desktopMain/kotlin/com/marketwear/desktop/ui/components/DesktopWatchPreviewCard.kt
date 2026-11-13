package com.marketwear.desktop.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private val WatchCardShape = RoundedCornerShape(14.dp)
private val WatchBezelColor = Color(0xFF263238)
private val WatchCardBgColor = Color(0xFF132232)
private val PositiveWatchColor = Color(0xFF4CAF50)
private val NegativeWatchColor = Color(0xFFF44336)
private val SecondaryWatchCurveColor = Color(0xFF64B5F6).copy(alpha = 0.55f)
private val BaselineWatchColor = Color(0xFF546E7A)

private val DesktopWatchRangeProvider = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        minOf(minY, 100.0) - 0.08

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        maxOf(maxY, 100.0) + 0.08
}

/**
 * Interactive round Wear OS simulator card rendered inside the KMP Desktop app
 * so developers can verify the compact watch card layout and Vico sparkline side-by-side.
 */
@Composable
fun DesktopWatchPreviewCard(
    state: PortfolioCardUiState,
    onToggleWeightMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trendColor = when (state.trendDirection) {
        TrendDirection.POSITIVE -> PositiveWatchColor
        TrendDirection.NEGATIVE -> NegativeWatchColor
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(236.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .border(width = 6.dp, color = WatchBezelColor, shape = CircleShape)
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Wear OS Simulator",
                fontSize = 10.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(WatchCardShape)
                    .background(WatchCardBgColor)
                    .clickable(onClick = onToggleWeightMode)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.watchTitleLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF90CAF9)
                    )
                    Text(
                        text = state.activeQuote.formattedPercent,
                        color = trendColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = state.activeQuote.formattedScore,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = state.watchSecondaryLabel,
                        fontSize = 10.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val modelProducer = remember { CartesianChartModelProducer() }
                LaunchedEffect(state.primarySparkline, state.secondarySparkline) {
                    modelProducer.runTransaction {
                        lineModel {
                            series(state.primarySparkline)
                            series(state.secondarySparkline)
                        }
                    }
                }

                val primaryLine = LineCartesianLayer.rememberLine(
                    fill = remember(trendColor) { LineCartesianLayer.LineFill.single(Fill(trendColor)) },
                    stroke = remember { LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp, cap = StrokeCap.Round) }
                )
                val secondaryLine = LineCartesianLayer.rememberLine(
                    fill = remember { LineCartesianLayer.LineFill.single(Fill(SecondaryWatchCurveColor)) },
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
                    fill = Fill(BaselineWatchColor),
                    thickness = 1.dp
                )
                val baselineDecoration = remember(baselineLine) {
                    HorizontalLine(
                        y = { 100.0 },
                        line = baselineLine
                    )
                }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(primaryLine, secondaryLine),
                            rangeProvider = DesktopWatchRangeProvider
                        ),
                        decorations = listOf(baselineDecoration)
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Tap to toggle Norm / Raw · 2h sync",
                    fontSize = 8.sp,
                    color = Color(0xFF78909C)
                )
            }
        }
    }
}

@Preview
@Composable
fun DesktopWatchPreviewCardPreview() {
    val state = MarketUiStateBuilder.buildPortfolioCardState(
        normalizedQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L),
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MarketWearTheme {
        DesktopWatchPreviewCard(
            state = state,
            onToggleWeightMode = {}
        )
    }
}
