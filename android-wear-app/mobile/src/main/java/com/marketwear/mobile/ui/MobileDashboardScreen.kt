package com.marketwear.mobile.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.marketwear.mobile.notification.PortfolioLiveNotificationManager
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.ui.LiveNotificationDeliveryState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketSessionStatus
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.SectorDisplayMode
import com.marketwear.shared.ui.WatchConnectionState
import com.marketwear.sharedui.components.IndexMetricCard
import com.marketwear.sharedui.components.MarketDashboardBody
import com.marketwear.sharedui.components.MarketNavGlyph
import com.marketwear.sharedui.components.MarketSessionPill
import com.marketwear.sharedui.components.NavGlyphKind
import com.marketwear.sharedui.components.PortfolioHoldingsConfigurator
import com.marketwear.sharedui.components.SectorBreadthBanner
import com.marketwear.sharedui.components.SectorControlBar
import com.marketwear.sharedui.components.SectorHeatmapTile
import com.marketwear.sharedui.components.WatchStatusChip
import com.marketwear.sharedui.components.sortSectors
import com.marketwear.sharedui.theme.MarketWearDimens
import com.marketwear.sharedui.theme.MarketWearPalette
import com.marketwear.sharedui.theme.MarketWearShapes
import com.marketwear.sharedui.theme.MarketWearTextSizes
import com.marketwear.sharedui.theme.MarketWearTextStyles
import com.marketwear.sharedui.theme.MarketWearTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun MobileDashboardScreen(
    viewModel: MobileViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationManager = remember(context) { PortfolioLiveNotificationManager(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.onIntent(
                MobileMarketIntent.SetLiveNotificationPreference(LiveNotificationPreference.ACTIVE)
            )
        }
        viewModel.onIntent(MobileMarketIntent.RefreshNotificationCapabilities)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationManager.isNotificationPermissionGranted()
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(MobileMarketIntent.RefreshNotificationCapabilities)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MobileDashboardContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onToggleLiveNotification = {
            when (uiState.liveNotificationState) {
                LiveNotificationDeliveryState.PERMISSION_REQUIRED -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onIntent(
                            MobileMarketIntent.SetLiveNotificationPreference(LiveNotificationPreference.ACTIVE)
                        )
                    }
                }
                LiveNotificationDeliveryState.PAUSED_BY_USER -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !notificationManager.isNotificationPermissionGranted()
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onIntent(
                            MobileMarketIntent.SetLiveNotificationPreference(LiveNotificationPreference.ACTIVE)
                        )
                    }
                }
                LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE,
                LiveNotificationDeliveryState.STANDARD_ONGOING_NEEDS_OPT_IN -> {
                    viewModel.onIntent(
                        MobileMarketIntent.SetLiveNotificationPreference(LiveNotificationPreference.PAUSED)
                    )
                }
            }
        },
        onOpenPromotionSettings = {
            context.startActivity(notificationManager.createPromotionSettingsIntent())
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileDashboardContent(
    uiState: MobileMarketUiState,
    onIntent: (MobileMarketIntent) -> Unit = {},
    modifier: Modifier = Modifier,
    onToggleLiveNotification: () -> Unit = {},
    onOpenPromotionSettings: () -> Unit = {}
) {
    BackHandler(enabled = uiState.activeTab != MobileAppTab.PULSE) {
        onIntent(MobileMarketIntent.SelectTab(MobileAppTab.PULSE))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val useNavigationRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.weight(0.1f))
                    MobileAppTab.entries.forEach { tab ->
                        val selected = uiState.activeTab == tab
                        val glyphTint = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        NavigationRailItem(
                            selected = selected,
                            onClick = { onIntent(MobileMarketIntent.SelectTab(tab)) },
                            icon = {
                                MarketNavGlyph(
                                    kind = tab.toNavGlyphKind(),
                                    tint = glyphTint
                                )
                            },
                            label = { Text(tab.label, maxLines = 1, softWrap = false) }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = MarketWearDimens.SmallSpacing)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = "MarketWear",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = MarketWearTextSizes.TopBarTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    MarketSessionPill(status = uiState.marketSessionStatus)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LiveNotificationChip(
                                        state = uiState.liveNotificationState,
                                        onClick = onToggleLiveNotification
                                    )
                                    WatchStatusChip(connectionState = uiState.watchConnectionState)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = if (useNavigationRail) Modifier else Modifier.navigationBarsPadding()
                        ) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = MarketWearDimens.ScreenPadding, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 10.dp)
                                ) {
                                    Text(
                                        text = "Watch Sync",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = uiState.formattedLastSynced,
                                        style = MaterialTheme.typography.bodySmall.merge(MarketWearTextStyles.Tabular),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                val isRefreshing = uiState.refreshState == RefreshState.REFRESHING
                                Button(
                                    onClick = {
                                        onIntent(
                                            MobileMarketIntent.Core(
                                                MarketIntent.RequestRefresh(RefreshPolicy.FORCE_CORE_QUOTES)
                                            )
                                        )
                                    },
                                    enabled = !isRefreshing,
                                    shape = MarketWearShapes.TogglePill,
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    when (uiState.refreshState) {
                                        RefreshState.REFRESHING -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(15.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Syncing...", fontWeight = FontWeight.SemiBold)
                                        }
                                        RefreshState.IDLE -> {
                                            Text("Refresh & Sync", fontWeight = FontWeight.SemiBold)
                                        }
                                        RefreshState.ERROR -> {
                                            Text("Retry Sync", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                ) {
                                    MobileAppTab.entries.forEach { tab ->
                                        val selected = uiState.activeTab == tab
                                        val glyphTint = if (selected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { onIntent(MobileMarketIntent.SelectTab(tab)) },
                                            icon = {
                                                MarketNavGlyph(
                                                    kind = tab.toNavGlyphKind(),
                                                    tint = glyphTint
                                                )
                                            },
                                            label = { Text(tab.label, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val contentModifier = Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .padding(
                            horizontal = MarketWearDimens.ScreenPadding,
                            vertical = MarketWearDimens.ItemSpacing
                        )

                    when (uiState.activeTab) {
                        MobileAppTab.PULSE -> MarketDashboardBody(
                            portfolioState = uiState.portfolioState,
                            indices = uiState.indices,
                            sectors = uiState.sectors,
                            onIntent = { coreIntent -> onIntent(MobileMarketIntent.Core(coreIntent)) },
                            onConfigureHoldings = {
                                onIntent(MobileMarketIntent.SelectTab(MobileAppTab.HOLDINGS))
                            },
                            sectorDisplayMode = uiState.sectorDisplayMode,
                            sectorSortOrder = uiState.sectorSortOrder,
                            onSelectSectorDisplayMode = { mode ->
                                onIntent(MobileMarketIntent.SelectSectorDisplayMode(mode))
                            },
                            onSelectSectorSortOrder = { order ->
                                onIntent(MobileMarketIntent.SelectSectorSortOrder(order))
                            },
                            errorMessage = uiState.errorMessage,
                            topSlot = when (uiState.liveNotificationState) {
                                LiveNotificationDeliveryState.STANDARD_ONGOING_NEEDS_OPT_IN -> {
                                    {
                                        Surface(
                                            shape = MarketWearShapes.MetricCard,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            border = BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Allow Android 16 Live Updates for pinned lock-screen & status-bar portfolio progress.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 8.dp)
                                                )
                                                TextButton(onClick = onOpenPromotionSettings) {
                                                    Text("Enable", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                                LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE,
                                LiveNotificationDeliveryState.PERMISSION_REQUIRED,
                                LiveNotificationDeliveryState.PAUSED_BY_USER -> null
                            },
                            modifier = contentModifier
                        )

                        MobileAppTab.HOLDINGS -> PortfolioHoldingsConfigurator(
                            holdings = PortfolioRegistry.HOLDINGS,
                            disabledHoldingIds = uiState.disabledHoldingIds,
                            customWeights = uiState.customWeights,
                            onIntent = { coreIntent -> onIntent(MobileMarketIntent.Core(coreIntent)) },
                            modifier = contentModifier
                        )

                        MobileAppTab.SECTORS -> MobileSectorsTabBody(
                            uiState = uiState,
                            onIntent = onIntent,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobileSectorsTabBody(
    uiState: MobileMarketUiState,
    onIntent: (MobileMarketIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedSectors = remember(uiState.sectors, uiState.sectorSortOrder) {
        sortSectors(uiState.sectors, uiState.sectorSortOrder)
    }
    val chunkedSectors = remember(sortedSectors, uiState.sectorDisplayMode) {
        if (uiState.sectorDisplayMode == SectorDisplayMode.HEATMAP) sortedSectors.chunked(2) else emptyList()
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MarketWearDimens.ItemSpacing),
        contentPadding = PaddingValues(bottom = MarketWearDimens.ItemSpacing)
    ) {
        item(key = "sectors_tab_breadth") {
            SectorBreadthBanner(sectors = sortedSectors)
        }
        item(key = "sectors_tab_controls") {
            SectorControlBar(
                displayMode = uiState.sectorDisplayMode,
                sortOrder = uiState.sectorSortOrder,
                onSelectDisplayMode = { mode ->
                    onIntent(MobileMarketIntent.SelectSectorDisplayMode(mode))
                },
                onSelectSortOrder = { order ->
                    onIntent(MobileMarketIntent.SelectSectorSortOrder(order))
                }
            )
        }
        if (uiState.sectorDisplayMode == SectorDisplayMode.HEATMAP) {
            items(
                items = chunkedSectors,
                key = { row -> "sec_tab_hm_" + row.joinToString("_") { it.symbol } }
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
            items(sortedSectors, key = { "sec_tab_list_${it.symbol}" }) { quote ->
                IndexMetricCard(quote = quote)
            }
        }
    }
}

private fun MobileAppTab.toNavGlyphKind(): NavGlyphKind = when (this) {
    MobileAppTab.PULSE -> NavGlyphKind.PULSE
    MobileAppTab.HOLDINGS -> NavGlyphKind.HOLDINGS
    MobileAppTab.SECTORS -> NavGlyphKind.SECTORS
}

@Composable
private fun LiveNotificationChip(
    state: LiveNotificationDeliveryState,
    onClick: () -> Unit
) {
    val (bgColor, dotColor, textColor, label) = when (state) {
        LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE -> QuadStyle(
            MarketWearPalette.PositivePillBg,
            MarketWearPalette.PositiveTrend,
            MarketWearPalette.ConnectedText,
            "Live Bar"
        )
        LiveNotificationDeliveryState.STANDARD_ONGOING_NEEDS_OPT_IN -> QuadStyle(
            MarketWearPalette.DarkPrimaryContainer,
            MarketWearPalette.DarkPrimary,
            MarketWearPalette.DarkOnPrimaryContainer,
            "Live Bar"
        )
        LiveNotificationDeliveryState.PERMISSION_REQUIRED -> QuadStyle(
            MarketWearPalette.NeutralPillBg,
            MarketWearPalette.DarkPrimary,
            MarketWearPalette.DarkTextPrimary,
            "Allow Bar"
        )
        LiveNotificationDeliveryState.PAUSED_BY_USER -> QuadStyle(
            MarketWearPalette.NeutralPillBg,
            MarketWearPalette.DarkTextSecondary,
            MarketWearPalette.DarkTextSecondary,
            "Bar Off"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(MarketWearShapes.StatusChip)
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.3f), MarketWearShapes.StatusChip)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = MarketWearDimens.TinySpacing)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

private data class QuadStyle(
    val bgColor: androidx.compose.ui.graphics.Color,
    val dotColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color,
    val label: String
)

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
            onIntent = {}
        )
    }
}
