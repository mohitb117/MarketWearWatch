package com.marketwear.sharedui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.SectorDisplayMode
import com.marketwear.shared.ui.SectorSortOrder
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearItemKeys
import com.marketwear.sharedui.theme.MarketWearTheme

/**
 * Shared responsive scrollable market dashboard body (Portfolio Score Hero + Major Indices + Sectors)
 * used across Android Mobile, Desktop, iOS, and Web targets.
 */
@Composable
fun MarketDashboardBody(
    portfolioState: PortfolioCardUiState,
    indices: List<IndexQuote>,
    sectors: List<IndexQuote>,
    onIntent: (MarketIntent) -> Unit,
    modifier: Modifier = Modifier,
    onConfigureHoldings: (() -> Unit)? = null,
    errorMessage: String? = null,
    chartHeight: Dp = MarketWearDimens.DefaultChartHeight,
    sectorDisplayMode: SectorDisplayMode = SectorDisplayMode.HEATMAP,
    sectorSortOrder: SectorSortOrder = SectorSortOrder.TOP_GAINERS,
    onSelectSectorDisplayMode: ((SectorDisplayMode) -> Unit)? = null,
    onSelectSectorSortOrder: ((SectorSortOrder) -> Unit)? = null,
    topSlot: (@Composable () -> Unit)? = null
) {
    var localDisplayMode by remember { mutableStateOf(sectorDisplayMode) }
    var localSortOrder by remember { mutableStateOf(sectorSortOrder) }
    val activeDisplayMode = if (onSelectSectorDisplayMode != null) sectorDisplayMode else localDisplayMode
    val activeSortOrder = if (onSelectSectorSortOrder != null) sectorSortOrder else localSortOrder

    BoxWithConstraints(modifier = modifier) {
        val isTwoColumnSectors = maxWidth >= 560.dp || activeDisplayMode == SectorDisplayMode.HEATMAP
        val isSideBySideIndices = maxWidth >= 420.dp
        val sortedSectors = remember(sectors, activeSortOrder) {
            sortSectors(sectors, activeSortOrder)
        }
        val chunkedSectors = remember(sortedSectors, activeDisplayMode) {
            if (activeDisplayMode == SectorDisplayMode.HEATMAP) sortedSectors.chunked(2) else emptyList()
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MarketWearDimens.ItemSpacing),
            contentPadding = PaddingValues(bottom = MarketWearDimens.ItemSpacing)
        ) {
            if (topSlot != null) {
                item(key = MarketWearItemKeys.LIVE_NOTIFICATION_BANNER) {
                    topSlot()
                }
            }

            item(key = MarketWearItemKeys.PORTFOLIO_HERO) {
                PortfolioScoreCard(
                    state = portfolioState,
                    onIntent = onIntent,
                    onConfigureHoldings = onConfigureHoldings,
                    chartHeight = chartHeight
                )
            }

            errorMessage?.let { error ->
                item(key = MarketWearItemKeys.ERROR_BANNER) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(MarketWearDimens.ItemSpacing)
                        )
                    }
                }
            }

            item(key = MarketWearItemKeys.INDICES_HEADER) {
                Text(
                    text = "Major US Indices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = MarketWearDimens.TinySpacing)
                )
            }

            if (isSideBySideIndices && indices.size == 2) {
                item(key = "indices_two_up_row") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MarketWearDimens.ItemSpacing)
                    ) {
                        IndexMetricCard(quote = indices[0], modifier = Modifier.weight(1f))
                        IndexMetricCard(quote = indices[1], modifier = Modifier.weight(1f))
                    }
                }
            } else {
                items(indices, key = { it.symbol }) { quote ->
                    IndexMetricCard(quote = quote)
                }
            }

            if (sortedSectors.isNotEmpty()) {
                item(key = MarketWearItemKeys.SECTORS_HEADER) {
                    Text(
                        text = "US Market Sectors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = MarketWearDimens.SmallSpacing)
                    )
                }

                item(key = "sector_breadth_banner") {
                    SectorBreadthBanner(sectors = sortedSectors)
                }

                item(key = "sector_control_bar") {
                    SectorControlBar(
                        displayMode = activeDisplayMode,
                        sortOrder = activeSortOrder,
                        onSelectDisplayMode = { mode ->
                            if (onSelectSectorDisplayMode != null) {
                                onSelectSectorDisplayMode(mode)
                            } else {
                                localDisplayMode = mode
                            }
                        },
                        onSelectSortOrder = { order ->
                            if (onSelectSectorSortOrder != null) {
                                onSelectSectorSortOrder(order)
                            } else {
                                localSortOrder = order
                            }
                        }
                    )
                }

                if (activeDisplayMode == SectorDisplayMode.HEATMAP) {
                    items(
                        items = chunkedSectors,
                        key = { row -> "hm_" + row.joinToString("_") { it.symbol } }
                    ) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MarketWearDimens.ItemSpacing)
                        ) {
                            SectorHeatmapTile(quote = row[0], modifier = Modifier.weight(1f))
                            if (row.size > 1) {
                                SectorHeatmapTile(quote = row[1], modifier = Modifier.weight(1f))
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(sortedSectors, key = { "list_${it.symbol}" }) { quote ->
                        IndexMetricCard(quote = quote)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MarketDashboardBodyPreview() {
    val dashboard = MarketUiStateBuilder.buildDashboardState(
        quotes = MarketRegistry.getFallbackQuotes(),
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MarketWearTheme {
        MarketDashboardBody(
            portfolioState = dashboard.portfolioState,
            indices = dashboard.indices,
            sectors = dashboard.sectors,
            onIntent = {}
        )
    }
}
