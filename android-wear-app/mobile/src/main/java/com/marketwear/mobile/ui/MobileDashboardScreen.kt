package com.marketwear.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.WatchConnectionState
import com.marketwear.sharedui.components.MarketDashboardBody
import com.marketwear.sharedui.components.WatchStatusChip
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun MobileDashboardScreen(
    viewModel: MobileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    MobileDashboardContent(
        uiState = uiState,
        onToggleWeightMode = viewModel::togglePortfolioWeightMode,
        onRefreshAndSync = { viewModel.refreshAndSync(forceRefresh = true) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDashboardContent(
    uiState: MobileMarketUiState,
    onToggleWeightMode: () -> Unit,
    onRefreshAndSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = MarketWearDimens.ScreenPadding)
                    ) {
                        Text(
                            text = "MarketWear",
                            fontWeight = FontWeight.Bold,
                            fontSize = MarketWearTextSizes.TopBarTitle
                        )
                        WatchStatusChip(connectionState = uiState.watchConnectionState)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(MarketWearDimens.ScreenPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Last synced to watch: ${uiState.formattedLastSynced}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val isRefreshing = uiState.refreshState == RefreshState.REFRESHING
                    Button(
                        onClick = onRefreshAndSync,
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MarketWearDimens.PrimaryButtonHeight),
                        shape = MarketWearShapes.PrimaryButton
                    ) {
                        when (uiState.refreshState) {
                            RefreshState.REFRESHING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(MarketWearDimens.ButtonLoaderSize),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Syncing...")
                            }
                            RefreshState.IDLE -> {
                                Text("Refresh & Sync to Watch", fontWeight = FontWeight.SemiBold)
                            }
                            RefreshState.ERROR -> {
                                Text("Retry Refresh & Sync", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        MarketDashboardBody(
            portfolioState = uiState.portfolioState,
            indices = uiState.indices,
            sectors = uiState.sectors,
            onToggleWeightMode = onToggleWeightMode,
            errorMessage = uiState.errorMessage,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(
                    horizontal = MarketWearDimens.ScreenPadding,
                    vertical = MarketWearDimens.ItemSpacing
                )
        )
    }
}

@Preview(
    name = "Mobile Full Dashboard Screen",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    backgroundColor = 0xFF0B0F14
)
@Composable
fun MobileDashboardContentPreview() {
    val fallbackQuotes = MarketRegistry.getFallbackQuotes()
    val dashboard = MarketUiStateBuilder.buildDashboardState(
        quotes = fallbackQuotes,
        weightMode = PortfolioWeightMode.NORMALIZED_100
    )
    MarketWearTheme {
        MobileDashboardContent(
            uiState = MobileMarketUiState(
                quotes = fallbackQuotes,
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                refreshState = RefreshState.IDLE,
                watchConnectionState = WatchConnectionState.CONNECTED,
                lastSyncedTimestamp = 1763056800000L,
                formattedLastSynced = "11:45:00 AM",
                errorMessage = null
            ),
            onToggleWeightMode = {},
            onRefreshAndSync = {}
        )
    }
}
