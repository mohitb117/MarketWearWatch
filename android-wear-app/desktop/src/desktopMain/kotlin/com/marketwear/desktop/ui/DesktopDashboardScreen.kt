package com.marketwear.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.desktop.ui.components.DesktopWatchPreviewCard
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import com.marketwear.sharedui.components.MarketDashboardBody
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTheme

@Composable
fun DesktopDashboardScreen(
    viewModel: DesktopMarketViewModel = remember { DesktopMarketViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()
    DesktopDashboardContent(
        uiState = uiState,
        onSelectScenario = viewModel::selectScenario,
        onToggleWeightMode = viewModel::togglePortfolioWeightMode,
        onRefreshLive = viewModel::refreshLiveNetworkQuotes
    )
}

@Composable
fun DesktopDashboardContent(
    uiState: DesktopMarketUiState,
    onSelectScenario: (DesktopSimulationScenario) -> Unit,
    onToggleWeightMode: () -> Unit,
    onRefreshLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MarketWear KMP Desktop Playground",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Shared :shared + :shared-ui · Vico 3.3.1 Compose Multiplatform · Updated ${uiState.formattedLastUpdated}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesktopSimulationScenario.entries.forEach { scenario ->
                            FilterChip(
                                selected = uiState.scenario == scenario,
                                onClick = { onSelectScenario(scenario) },
                                label = { Text(scenario.label, fontSize = MarketWearTextSizes.FilterChip) }
                            )
                        }

                        Button(
                            onClick = onRefreshLive,
                            enabled = uiState.refreshState != RefreshState.REFRESHING,
                            shape = MarketWearShapes.PrimaryButton
                        ) {
                            val label = when (uiState.refreshState) {
                                RefreshState.REFRESHING -> "Syncing..."
                                RefreshState.IDLE -> "Fetch Live"
                                RefreshState.ERROR -> "Retry Live"
                            }
                            Text(label)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane: Interactive Wear OS Round Simulator + 37 Holdings Breakdown
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DesktopWatchPreviewCard(
                        state = uiState.portfolioState,
                        onToggleWeightMode = onToggleWeightMode
                    )

                    Spacer(modifier = Modifier.height(MarketWearDimens.SectionSpacing))

                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = MarketWearShapes.MetricCard,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Tracked Basket (${uiState.topHoldings.size} Holdings · ${PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(MarketWearDimens.SmallSpacing)
                            ) {
                                items(uiState.topHoldings, key = { "${it.name}_${it.weightPercent}" }) { pos ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                MarketWearShapes.CompactRow
                                            )
                                            .padding(horizontal = 10.dp, vertical = MarketWearDimens.SmallSpacing),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pos.displayTicker,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = MarketWearTextSizes.HoldingRow
                                        )
                                        Text(
                                            text = "${pos.weightPercent}% raw",
                                            fontSize = MarketWearTextSizes.HoldingRow,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Pane: Shared Compose Multiplatform MarketDashboardBody from :shared-ui
                MarketDashboardBody(
                    portfolioState = uiState.portfolioState,
                    indices = uiState.indices,
                    sectors = uiState.sectors,
                    onToggleWeightMode = onToggleWeightMode,
                    errorMessage = uiState.errorMessage,
                    chartHeight = MarketWearDimens.DesktopChartHeight,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Preview
@Composable
fun DesktopDashboardContentPreview() {
    val state = DesktopMarketViewModel.buildUiState(
        scenario = DesktopSimulationScenario.DEFAULT_BASKET,
        quotes = DesktopMarketViewModel.quotesForScenario(DesktopSimulationScenario.DEFAULT_BASKET),
        weightMode = PortfolioWeightMode.NORMALIZED_100,
        refreshState = RefreshState.IDLE,
        errorMessage = null
    )
    MarketWearTheme {
        DesktopDashboardContent(
            uiState = state,
            onSelectScenario = {},
            onToggleWeightMode = {},
            onRefreshLive = {}
        )
    }
}
