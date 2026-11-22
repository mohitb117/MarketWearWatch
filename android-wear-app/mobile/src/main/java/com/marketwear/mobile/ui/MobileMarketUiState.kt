package com.marketwear.mobile.ui

import androidx.compose.runtime.Immutable
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.ui.LiveNotificationDeliveryState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketIntent
import com.marketwear.shared.ui.MarketSessionStatus
import com.marketwear.shared.ui.PortfolioCardUiState
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.SectorDisplayMode
import com.marketwear.shared.ui.SectorSortOrder
import com.marketwear.shared.ui.WatchConnectionState

/**
 * Primary top-level destinations in the Mobile Companion app.
 */
enum class MobileAppTab(val label: String) {
    PULSE("Pulse"),
    HOLDINGS("Holdings"),
    SECTORS("Sectors")
}

/**
 * Unidirectional MVI intents handled by [MobileMarketStore].
 */
sealed interface MobileMarketIntent {
    data class Core(val intent: MarketIntent) : MobileMarketIntent
    data class SetLiveNotificationPreference(val preference: LiveNotificationPreference) : MobileMarketIntent
    data object RefreshNotificationCapabilities : MobileMarketIntent
    data class SelectTab(val tab: MobileAppTab) : MobileMarketIntent
    data class SelectSectorDisplayMode(val mode: SectorDisplayMode) : MobileMarketIntent
    data class SelectSectorSortOrder(val order: SectorSortOrder) : MobileMarketIntent
}

@Immutable
data class MobileMarketUiState(
    val quotes: List<IndexQuote> = emptyList(),
    val portfolioState: PortfolioCardUiState = PortfolioCardUiState(),
    val indices: List<IndexQuote> = emptyList(),
    val sectors: List<IndexQuote> = emptyList(),
    val refreshState: RefreshState = RefreshState.IDLE,
    val watchConnectionState: WatchConnectionState = WatchConnectionState.DISCONNECTED,
    val disabledHoldingIds: Set<String> = emptySet(),
    val customWeights: Map<String, Double> = emptyMap(),
    val liveNotificationPreference: LiveNotificationPreference = LiveNotificationPreference.ACTIVE,
    val liveNotificationState: LiveNotificationDeliveryState = LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE,
    val activeTab: MobileAppTab = MobileAppTab.PULSE,
    val sectorDisplayMode: SectorDisplayMode = SectorDisplayMode.HEATMAP,
    val sectorSortOrder: SectorSortOrder = SectorSortOrder.TOP_GAINERS,
    val marketSessionStatus: MarketSessionStatus = MarketSessionStatus.OFF_HOURS,
    val lastSyncedTimestamp: Long = 0L,
    val formattedLastSynced: String = "Not synced",
    val errorMessage: String? = null
)
