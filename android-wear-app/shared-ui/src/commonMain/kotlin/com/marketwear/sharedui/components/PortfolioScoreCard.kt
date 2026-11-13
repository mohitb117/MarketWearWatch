package com.marketwear.sharedui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
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
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore

private val SharedPortfolioRangeProvider = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        minOf(minY, 100.0) - 0.1

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        maxOf(maxY, 100.0) + 0.1
}

/**
 * Shared Compose Multiplatform Portfolio Daily Score (100-baseline) card with Vico 3.3.1
 * dual-curve line chart, reused across Android Mobile, Desktop, iOS, and Web.
 */
@Composable
fun PortfolioScoreCard(
    state: PortfolioCardUiState,
    onToggleWeightMode: () -> Unit,
    modifier: Modifier = Modifier,
    chartHeight: Dp = MarketWearDimens.DefaultChartHeight
) {
    val trendColor = when (state.trendDirection) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MarketWearShapes.PortfolioCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactWidth = maxWidth < 380.dp

            Column(modifier = Modifier.padding(MarketWearDimens.CardPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = MarketWearDimens.SmallSpacing)
                    ) {
                        Text(
                            text = "Portfolio Daily Score",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "37 UBS & Fidelity Holdings · 100.0 Morning Baseline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MarketWearShapes.TogglePill
                            )
                            .clickable(onClick = onToggleWeightMode)
                            .padding(horizontal = 10.dp, vertical = MarketWearDimens.SmallSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.mobileModePillLabel,
                            fontSize = MarketWearTextSizes.ModePill,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MarketWearDimens.ItemSpacing))

                if (isCompactWidth) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PrimaryScoreBlock(
                            state = state,
                            trendColor = trendColor,
                            singleLineDelta = true
                        )
                        Spacer(modifier = Modifier.height(MarketWearDimens.TinySpacing))
                        Text(
                            text = "${state.mobileSecondarySummary} · ${state.mobileRangeSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        PrimaryScoreBlock(
                            state = state,
                            trendColor = trendColor,
                            singleLineDelta = false,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = MarketWearDimens.SmallSpacing)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = state.mobileSecondarySummary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.mobileRangeSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MarketWearDimens.ItemSpacing))

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
                    stroke = remember {
                        LineCartesianLayer.LineStroke.Continuous(
                            thickness = MarketWearDimens.PrimaryLineThickness,
                            cap = StrokeCap.Round
                        )
                    }
                )
                val secondaryLine = LineCartesianLayer.rememberLine(
                    fill = remember { LineCartesianLayer.LineFill.single(Fill(MarketWearPalette.SecondaryCurve)) },
                    stroke = remember {
                        LineCartesianLayer.LineStroke.Dashed(
                            thickness = MarketWearDimens.SecondaryLineThickness,
                            cap = StrokeCap.Round,
                            dashLength = MarketWearDimens.DashedLineLength,
                            gapLength = MarketWearDimens.DashedGapLength
                        )
                    }
                )
                val baselineLine = rememberLineComponent(
                    fill = Fill(MarketWearPalette.BaselineRule),
                    thickness = MarketWearDimens.BaselineLineThickness
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
                            rangeProvider = SharedPortfolioRangeProvider
                        ),
                        decorations = decorations
                    ),
                    modelProducer = modelProducer,
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    zoomState = rememberVicoZoomState(zoomEnabled = false),
                    animationSpec = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            MarketWearShapes.ChartContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(MarketWearDimens.SmallSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.mobileLegendLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = MarketWearDimens.SmallSpacing)
                    )
                    Text(
                        text = "2h Throttle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryScoreBlock(
    state: PortfolioCardUiState,
    trendColor: Color,
    singleLineDelta: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = state.activeQuote.formattedScore,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "${state.activeQuote.formattedChange} (${state.activeQuote.formattedPercent}) from 100.00",
            color = trendColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = if (singleLineDelta) 1 else Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun PortfolioScoreCardNormalizedPreview() {
    val state = MarketUiStateBuilder.buildPortfolioCardState(
        normalizedQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L),
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MarketWearTheme {
        PortfolioScoreCard(
            state = state,
            onToggleWeightMode = {}
        )
    }
}

@Preview
@Composable
fun PortfolioScoreCardRawPreview() {
    val state = MarketUiStateBuilder.buildPortfolioCardState(
        normalizedQuote = PortfolioRegistry.createDefaultFallbackPortfolioQuote(0L),
        weightMode = PortfolioWeightMode.RAW_PORTFOLIO
    )
    MarketWearTheme {
        PortfolioScoreCard(
            state = state,
            onToggleWeightMode = {}
        )
    }
}
