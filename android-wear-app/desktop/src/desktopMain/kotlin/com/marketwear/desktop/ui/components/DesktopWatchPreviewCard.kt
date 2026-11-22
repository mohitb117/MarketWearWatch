package com.marketwear.desktop.ui.components

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTextStyles
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

private val WatchCardShape = RoundedCornerShape(14.dp)
private val WatchBezelColor = MarketWearPalette.WatchBezelOuter
private val WatchScreenColor = MarketWearPalette.WatchBezelInner
private val WatchCardBgColor = Color(0xFF0E1824)
private val PositiveWatchColor = MarketWearPalette.PositiveTrend
private val NegativeWatchColor = MarketWearPalette.NegativeTrend
private val SecondaryWatchCurveColor = MarketWearPalette.SecondaryCurve
private val BaselineWatchColor = MarketWearPalette.BaselineRule

/**
 * Interactive face modes inside the Desktop Wear OS Watch Simulator.
 */
enum class WatchPreviewFaceMode(val label: String) {
    PORTFOLIO("Score"),
    INDICES("Indices"),
    SECTORS("Sectors")
}

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
    onIntent: (MarketIntent) -> Unit,
    modifier: Modifier = Modifier,
    indices: List<IndexQuote> = emptyList(),
    sectors: List<IndexQuote> = emptyList()
) {
    var faceMode by remember { mutableStateOf(WatchPreviewFaceMode.PORTFOLIO) }
    val trendColor = when (state.trendDirection) {
        TrendDirection.POSITIVE -> PositiveWatchColor
        TrendDirection.NEGATIVE -> NegativeWatchColor
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(252.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            trendColor.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(WatchScreenColor)
                    .border(width = 5.dp, color = WatchBezelColor, shape = CircleShape)
                    .padding(18.dp)
            ) {
                when (faceMode) {
                    WatchPreviewFaceMode.PORTFOLIO -> WatchPortfolioDialFace(
                        state = state,
                        trendColor = trendColor,
                        onIntent = onIntent
                    )
                    WatchPreviewFaceMode.INDICES -> WatchListDialFace(
                        title = "Wear OS · Indices",
                        items = indices
                    )
                    WatchPreviewFaceMode.SECTORS -> WatchListDialFace(
                        title = "Wear OS · Top Sectors",
                        items = sectors.sortedByDescending { it.percentChange }.take(3)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WatchPreviewFaceMode.entries.forEach { mode ->
                FilterChip(
                    selected = faceMode == mode,
                    onClick = { faceMode = mode },
                    label = {
                        Text(
                            text = mode.label,
                            fontSize = MarketWearTextSizes.FilterChip,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun WatchListDialFace(
    title: String,
    items: List<IndexQuote>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (items.isEmpty()) {
            Text(
                text = "Waiting for quotes",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        } else {
            items.forEach { quote ->
                val accent = if (quote.percentChange >= 0.0) PositiveWatchColor else NegativeWatchColor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (quote.symbol.startsWith("^")) quote.name else quote.symbol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = quote.formattedPercent,
                        style = MarketWearTextStyles.TabularSmall,
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchPortfolioDialFace(
    state: PortfolioCardUiState,
    trendColor: Color,
    onIntent: (MarketIntent) -> Unit
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
                .clickable { onIntent(MarketIntent.ToggleWeightMode) }
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

                // Compact 50%-centered bipolar fluctuation strip matching Wear OS watch card
                val liveSpec = state.liveUpdateSpec
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@Canvas
                    val total = liveSpec.segments.sumOf { it.length }.coerceAtLeast(1).toFloat()
                    val pillPath = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = 0f,
                                top = 0f,
                                right = w,
                                bottom = h,
                                cornerRadius = CornerRadius(h / 2f, h / 2f)
                            )
                        )
                    }
                    clipPath(pillPath) {
                        var x = 0f
                        for (seg in liveSpec.segments) {
                            val sw = (seg.length / total) * w
                            if (sw > 0f) {
                                drawRect(
                                    color = Color(seg.argbColor),
                                    topLeft = Offset(x, 0f),
                                    size = Size(sw, h)
                                )
                                x += sw
                            }
                        }
                    }
                    val midX = w * 0.5f
                    drawLine(
                        color = Color(0xFFE2E8F0),
                        start = Offset(midX, 0f),
                        end = Offset(midX, h),
                        strokeWidth = 1.5.dp.toPx()
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

                val decorations = remember(baselineDecoration) { listOf(baselineDecoration) }

                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(
                            lineProvider = LineCartesianLayer.LineProvider.series(primaryLine, secondaryLine),
                            rangeProvider = DesktopWatchRangeProvider
                        ),
                        decorations = decorations
                    ),
                    modelProducer = modelProducer,
                    scrollState = rememberVicoScrollState(scrollEnabled = false),
                    zoomState = rememberVicoZoomState(zoomEnabled = false),
                    animationSpec = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Tap mode · ${state.enabledHoldingsCount}/${state.totalHoldingsCount} active",
                    fontSize = 8.sp,
                    color = Color(0xFF94A3B8)
                )
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
            onIntent = {}
        )
    }
}
