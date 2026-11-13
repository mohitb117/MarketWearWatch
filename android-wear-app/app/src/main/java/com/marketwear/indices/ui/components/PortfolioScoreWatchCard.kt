package com.marketwear.indices.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private val PortfolioWatchCardShape = RoundedCornerShape(14.dp)
private val PortfolioCardStartColor = Color(0xFF132232)
private val PortfolioCardEndColor = Color(0xFF1E1E1E)
private val PositiveTrendWatchColor = Color(0xFF4CAF50)
private val NegativeTrendWatchColor = Color(0xFFF44336)
private val SecondaryCurveColor = Color(0xFF64B5F6).copy(alpha = 0.55f)
private val BaselineWatchColor = Color(0xFF546E7A)

private val PortfolioWatchRangeProvider = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        minOf(minY, 100.0) - 0.08

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        maxOf(maxY, 100.0) + 0.08
}

@Composable
fun PortfolioScoreWatchCard(
    state: PortfolioCardUiState,
    onToggleWeightMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trendColor = when (state.trendDirection) {
        TrendDirection.POSITIVE -> PositiveTrendWatchColor
        TrendDirection.NEGATIVE -> NegativeTrendWatchColor
    }

    Card(
        onClick = onToggleWeightMode,
        modifier = modifier.fillMaxWidth(),
        shape = PortfolioWatchCardShape,
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = PortfolioCardStartColor,
            endBackgroundColor = PortfolioCardEndColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
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

            PortfolioSparklineCanvas(
                primaryPoints = state.primarySparkline,
                secondaryPoints = state.secondarySparkline,
                primaryColor = trendColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Tap to toggle Norm (100%) / Raw · 2h sync",
                fontSize = 8.sp,
                color = Color(0xFF78909C),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Intraday dual-curve line chart rendered with Vico 3.3.1 [CartesianChartHost]
 * (primary curve + dashed secondary curve + 100.0 baseline decoration).
 */
@Composable
fun PortfolioSparklineCanvas(
    primaryPoints: List<Double>,
    secondaryPoints: List<Double>,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(primaryPoints, secondaryPoints) {
        modelProducer.runTransaction {
            lineModel {
                series(primaryPoints)
                series(secondaryPoints)
            }
        }
    }

    val primaryLine = LineCartesianLayer.rememberLine(
        fill = remember(primaryColor) { LineCartesianLayer.LineFill.single(Fill(primaryColor)) },
        stroke = remember { LineCartesianLayer.LineStroke.Continuous(thickness = 2.dp, cap = StrokeCap.Round) }
    )
    val secondaryLine = LineCartesianLayer.rememberLine(
        fill = remember { LineCartesianLayer.LineFill.single(Fill(SecondaryCurveColor)) },
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

    val decorations = remember(baselineDecoration) { listOf(baselineDecoration) }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(primaryLine, secondaryLine),
                rangeProvider = PortfolioWatchRangeProvider
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false),
        animationSpec = null,
        modifier = modifier
    )
}

@Preview(
    name = "Portfolio Score Watch Card (Normalized 100%)",
    widthDp = 200,
    heightDp = 120,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PortfolioScoreWatchCardNormalizedPreview() {
    val state = MarketUiStateBuilder.buildPortfolioCardState(
        normalizedQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L),
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MaterialTheme {
        PortfolioScoreWatchCard(
            state = state,
            onToggleWeightMode = {}
        )
    }
}

@Preview(
    name = "Portfolio Score Watch Card (Raw Portfolio)",
    widthDp = 200,
    heightDp = 120,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun PortfolioScoreWatchCardRawPreview() {
    val state = MarketUiStateBuilder.buildPortfolioCardState(
        normalizedQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L),
        weightMode = PortfolioWeightMode.RAW_PORTFOLIO
    )
    MaterialTheme {
        PortfolioScoreWatchCard(
            state = state,
            onToggleWeightMode = {}
        )
    }
}
