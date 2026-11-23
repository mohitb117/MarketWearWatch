package com.marketwear.sharedui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioLiveUpdateFormatter
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.TrendDirection
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme

/**
 * Shared Obsidian Terminal Portfolio Daily Score (100-baseline) hero card with interactive
 * touch/pointer-scrubbable dual-curve Canvas chart and synced center-origin log-linear gauge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioScoreCard(
    state: PortfolioCardUiState,
    onIntent: (MarketIntent) -> Unit,
    modifier: Modifier = Modifier,
    onConfigureHoldings: (() -> Unit)? = null,
    chartHeight: Dp = MarketWearDimens.DefaultChartHeight
) {
    var scrubbedIndex by remember(state.weightMode, state.primarySparkline) {
        mutableStateOf<Int?>(null)
    }
    val scrubbedScore = scrubbedIndex?.let { state.primarySparkline.getOrNull(it) }
    val displayQuote = remember(state.activeQuote, scrubbedScore) {
        if (scrubbedScore != null) {
            val delta = scrubbedScore - PortfolioRegistry.BASELINE_SCORE
            state.activeQuote.copy(
                price = scrubbedScore,
                change = delta,
                percentChange = delta
            )
        } else {
            state.activeQuote
        }
    }
    val displayTrend = TrendDirection.fromChange(displayQuote.change)
    val trendColor = when (displayTrend) {
        TrendDirection.POSITIVE -> MarketWearPalette.PositiveTrend
        TrendDirection.NEGATIVE -> MarketWearPalette.NegativeTrend
    }
    val displayLiveSpec = remember(displayQuote, state.liveUpdateSpec, state.enabledHoldingsCount, state.totalHoldingsCount, scrubbedScore) {
        if (scrubbedScore != null) {
            PortfolioLiveUpdateFormatter.buildSpec(
                activeQuote = displayQuote,
                enabledHoldingsCount = state.enabledHoldingsCount,
                totalHoldingsCount = state.totalHoldingsCount
            )
        } else {
            state.liveUpdateSpec
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MarketWearShapes.PortfolioCard,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    trendColor.copy(alpha = 0.45f),
                    MaterialTheme.colorScheme.outlineVariant
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            trendColor.copy(alpha = 0.09f),
                            Color.Transparent
                        )
                    )
                )
        ) {
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
                            text = "${state.enabledHoldingsCount}/${state.totalHoldingsCount} Holdings · 100.0 Morning Baseline",
                            style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(MarketWearShapes.TogglePill)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                shape = MarketWearShapes.TogglePill
                            )
                            .clickable { onIntent(MarketIntent.ToggleWeightMode) }
                            .padding(horizontal = 10.dp, vertical = MarketWearDimens.SmallSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.mobileModePillLabel,
                            fontSize = MarketWearTextSizes.ModePill,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MarketWearDimens.ItemSpacing))

                if (isCompactWidth) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PrimaryScoreBlock(
                            scoreText = displayQuote.formattedScore,
                            deltaPercentText = displayQuote.formattedPercent,
                            trendDirection = displayTrend,
                            subtitleText = if (scrubbedScore != null) "Scrubbed intraday" else "vs 100.00 open"
                        )
                        Spacer(modifier = Modifier.height(MarketWearDimens.SmallSpacing))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${state.mobileSecondarySummary} · ${state.mobileRangeSummary}",
                                style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onConfigureHoldings != null) {
                                HoldingsConfigurePill(
                                    enabledCount = state.enabledHoldingsCount,
                                    totalCount = state.totalHoldingsCount,
                                    onClick = onConfigureHoldings
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        PrimaryScoreBlock(
                            scoreText = displayQuote.formattedScore,
                            deltaPercentText = displayQuote.formattedPercent,
                            trendDirection = displayTrend,
                            subtitleText = if (scrubbedScore != null) "Scrubbed intraday" else "vs 100.00 open",
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = MarketWearDimens.SmallSpacing)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            if (onConfigureHoldings != null) {
                                HoldingsConfigurePill(
                                    enabledCount = state.enabledHoldingsCount,
                                    totalCount = state.totalHoldingsCount,
                                    onClick = onConfigureHoldings
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Text(
                                text = state.mobileSecondarySummary,
                                style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.mobileRangeSummary,
                                style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MarketWearDimens.ItemSpacing))

                // Center-origin log-linear bipolar fluctuation meter synced with Android 16 ProgressStyle
                PortfolioFluctuationBar(spec = displayLiveSpec)

                Spacer(modifier = Modifier.height(MarketWearDimens.ItemSpacing))

                Surface(
                    shape = MarketWearShapes.ChartContainer,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        InteractivePortfolioChart(
                            primaryPoints = state.primarySparkline,
                            secondaryPoints = state.secondarySparkline,
                            trendDirection = displayTrend,
                            scrubbedIndex = scrubbedIndex,
                            onScrubIndexChange = { scrubbedIndex = it },
                            height = chartHeight
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
                                text = if (scrubbedScore != null) "Release to reset" else "Drag to scrub · 2h Throttle",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldingsConfigurePill(
    enabledCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(MarketWearShapes.TogglePill)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = MarketWearShapes.TogglePill
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Holdings ($enabledCount/$totalCount)",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun PrimaryScoreBlock(
    scoreText: String,
    deltaPercentText: String,
    trendDirection: TrendDirection,
    subtitleText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = scoreText,
            style = MaterialTheme.typography.headlineMedium.merge(MarketWearTextStyles.TabularHero),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeltaPill(
                text = deltaPercentText,
                trendDirection = trendDirection
            )
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            onIntent = {}
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
            onIntent = {}
        )
    }
}
