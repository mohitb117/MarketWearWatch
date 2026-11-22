package com.marketwear.desktop.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketwear.desktop.ui.components.DesktopWatchPreviewCard
import com.marketwear.shared.platform.isUsMarketWindowActive
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketSessionStatus
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.RefreshState
import com.marketwear.sharedui.components.LiveUpdateNotificationPreviewCard
import com.marketwear.sharedui.components.MarketDashboardBody
import com.marketwear.sharedui.components.MarketSessionPill
import com.marketwear.sharedui.components.PortfolioFluctuationBar
import com.marketwear.sharedui.components.PortfolioHoldingsConfigurator
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme
import kotlinx.coroutines.Dispatchers

@Composable
fun DesktopDashboardScreen(
    viewModel: DesktopMarketViewModel = remember {
        DesktopMarketViewModel(mainContext = Dispatchers.Main.immediate)
    }
) {
    DisposableEffect(viewModel) {
        onDispose { viewModel.dispose() }
    }
    val uiState by viewModel.uiState.collectAsState()
    DesktopDashboardContent(
        uiState = uiState,
        onIntent = viewModel::onIntent
    )
}

@Composable
fun DesktopDashboardContent(
    uiState: DesktopMarketUiState,
    onIntent: (DesktopMarketIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "MarketWear KMP Desktop Playground",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            MarketSessionPill(
                                status = MarketSessionStatus.fromMarketWindow(isUsMarketWindowActive())
                            )
                        }
                        Text(
                            text = "Shared :shared + :shared-ui · Android 16 ProgressStyle Preview · Updated ${uiState.formattedLastUpdated}",
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
                                onClick = { onIntent(DesktopMarketIntent.SelectScenario(scenario)) },
                                label = { Text(scenario.label, fontSize = MarketWearTextSizes.FilterChip) }
                            )
                        }

                        Button(
                            onClick = {
                                onIntent(DesktopMarketIntent.Core(MarketIntent.RequestRefresh(RefreshPolicy.FORCE_CORE_QUOTES)))
                            },
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
                // Left Pane: Interactive Wear OS Round Simulator + 37 Configurable Holdings
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DesktopWatchPreviewCard(
                        state = uiState.portfolioState,
                        indices = uiState.indices,
                        sectors = uiState.sectors,
                        onIntent = { onIntent(DesktopMarketIntent.Core(it)) }
                    )

                    Spacer(modifier = Modifier.height(MarketWearDimens.SectionSpacing))

                    Card(
                        modifier = Modifier.fillMaxSize(),
                        shape = MarketWearShapes.MetricCard,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        PortfolioHoldingsConfigurator(
                            holdings = uiState.topHoldings,
                            disabledHoldingIds = uiState.disabledHoldingIds,
                            customWeights = uiState.customWeights,
                            onIntent = { onIntent(DesktopMarketIntent.Core(it)) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        )
                    }
                }

                // Right Pane: Android 16 Progress-Centric Notification Preview + Shared MarketDashboardBody
                MarketDashboardBody(
                    portfolioState = uiState.portfolioState,
                    indices = uiState.indices,
                    sectors = uiState.sectors,
                    onIntent = { onIntent(DesktopMarketIntent.Core(it)) },
                    errorMessage = uiState.errorMessage,
                    chartHeight = MarketWearDimens.DesktopChartHeight,
                    topSlot = {
                        LiveUpdateNotificationPreviewCard(
                            spec = uiState.portfolioState.liveUpdateSpec,
                            onIntent = { onIntent(DesktopMarketIntent.Core(it)) }
                        )
                    },
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
            onIntent = {}
        )
    }
}
