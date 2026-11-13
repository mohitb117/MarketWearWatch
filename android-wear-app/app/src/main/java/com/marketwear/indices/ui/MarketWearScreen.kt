package com.marketwear.indices.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.marketwear.indices.ui.components.IndexCard
import com.marketwear.indices.ui.components.PortfolioScoreWatchCard
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import org.koin.androidx.compose.koinViewModel

@Composable
fun MarketWearApp(
    initialSymbol: String = "SPX",
    viewModel: MarketViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    MarketWearContent(
        uiState = uiState,
        onToggleWeightMode = viewModel::togglePortfolioWeightMode,
        onRefresh = { viewModel.refresh(force = true) }
    )
}

@Composable
fun MarketWearContent(
    uiState: MarketUiState,
    onToggleWeightMode: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        modifier = modifier,
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            autoCentering = AutoCenteringParams(itemIndex = 0),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp, start = 12.dp, end = 12.dp)
        ) {
            item(key = "header_title") {
                Text(
                    text = "Market Watch",
                    style = MaterialTheme.typography.caption1,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            item(key = "portfolio_score_card") {
                PortfolioScoreWatchCard(
                    state = uiState.portfolioState,
                    onToggleWeightMode = onToggleWeightMode
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            items(
                items = uiState.indices,
                key = { it.symbol }
            ) { quote ->
                IndexCard(quote = quote)
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (uiState.sectors.isNotEmpty()) {
                item(key = "sectors_header") {
                    Text(
                        text = "Sectors",
                        style = MaterialTheme.typography.caption1,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                    )
                }

                items(
                    items = uiState.sectors,
                    key = { it.symbol }
                ) { quote ->
                    IndexCard(quote = quote)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            item(key = "refresh_action") {
                val refreshLabel = when (uiState.refreshState) {
                    RefreshState.REFRESHING -> "Syncing"
                    RefreshState.IDLE -> "Refresh"
                    RefreshState.ERROR -> "Retry"
                }
                CompactChip(
                    onClick = onRefresh,
                    colors = ChipDefaults.secondaryChipColors(),
                    icon = {
                        if (uiState.refreshState == RefreshState.REFRESHING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                indicatorColor = MaterialTheme.colors.primary
                            )
                        }
                    },
                    label = {
                        Text(
                            text = refreshLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "timestamp_footer") {
                Text(
                    text = "Updated ${uiState.formattedLastUpdated}",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(
    name = "Wear OS Full Market Dashboard",
    widthDp = 227,
    heightDp = 227,
    showBackground = true,
    backgroundColor = 0xFF000000
)
@Composable
fun MarketWearContentPreview() {
    val fallbackQuotes = MarketRegistry.getFallbackQuotes()
    val dashboard = MarketUiStateBuilder.buildDashboardState(
        quotes = fallbackQuotes,
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MaterialTheme {
        MarketWearContent(
            uiState = MarketUiState(
                quotes = fallbackQuotes,
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                refreshState = RefreshState.IDLE,
                lastUpdated = dashboard.latestUpdatedMillis,
                formattedLastUpdated = "11:45 AM"
            ),
            onToggleWeightMode = {},
            onRefresh = {}
        )
    }
}
