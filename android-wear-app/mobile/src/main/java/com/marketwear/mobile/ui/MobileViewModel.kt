package com.marketwear.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.states
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.mobile.notification.PortfolioLiveNotificationManager
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.platform.isUsMarketWindowActive
import com.marketwear.shared.ui.LiveNotificationDeliveryState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.MarketSessionStatus
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.NotificationExpansionMode
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshState
import com.marketwear.shared.ui.SectorDisplayMode
import com.marketwear.shared.ui.SectorSortOrder
import com.marketwear.shared.ui.WatchConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MobileViewModel(
    application: Application,
    repository: MobileMarketRepository,
    syncManager: WearDataSyncManager,
    selectionStore: PortfolioSelectionStore = PortfolioSelectionStore(application),
    liveNotificationManager: PortfolioLiveNotificationManager = PortfolioLiveNotificationManager(application),
    storeFactory: StoreFactory = DefaultStoreFactory()
) : AndroidViewModel(application) {

    private val store: MobileMarketStore = MobileMarketStoreFactory(
        storeFactory = storeFactory,
        application = application,
        repository = repository,
        syncManager = syncManager,
        selectionStore = selectionStore,
        liveNotificationManager = liveNotificationManager
    ).create()

    val uiState: StateFlow<MobileMarketUiState> = store.states.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = store.state
    )

    fun onIntent(intent: MobileMarketIntent) {
        store.accept(intent)
    }

    override fun onCleared() {
        store.dispose()
        super.onCleared()
    }

    internal companion object {
        private val SYNC_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault())

        fun buildUiState(
            quotes: List<IndexQuote>,
            weightMode: PortfolioWeightMode,
            refreshState: RefreshState,
            watchConnectionState: WatchConnectionState,
            lastSyncedTimestamp: Long,
            errorMessage: String?,
            disabledHoldingIds: Set<String> = emptySet(),
            customWeights: Map<String, Double> = emptyMap(),
            liveNotificationPreference: LiveNotificationPreference = LiveNotificationPreference.ACTIVE,
            liveNotificationState: LiveNotificationDeliveryState = LiveNotificationDeliveryState.PROMOTED_LIVE_UPDATE,
            notificationExpansionMode: NotificationExpansionMode = NotificationExpansionMode.EXPANDED,
            activeTab: MobileAppTab = MobileAppTab.PULSE,
            sectorDisplayMode: SectorDisplayMode = SectorDisplayMode.HEATMAP,
            sectorSortOrder: SectorSortOrder = SectorSortOrder.TOP_GAINERS,
            marketSessionStatus: MarketSessionStatus = MarketSessionStatus.fromMarketWindow(isUsMarketWindowActive())
        ): MobileMarketUiState {
            val dashboard = MarketUiStateBuilder.buildDashboardState(
                quotes = quotes,
                weightMode = weightMode,
                fallbackTimestampMillis = lastSyncedTimestamp,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights
            )
            val formattedTime = if (lastSyncedTimestamp > 0L) {
                Instant.ofEpochMilli(lastSyncedTimestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(SYNC_TIME_FORMATTER)
            } else {
                "Not synced"
            }
            return MobileMarketUiState(
                quotes = quotes,
                portfolioState = dashboard.portfolioState,
                indices = dashboard.indices,
                sectors = dashboard.sectors,
                refreshState = refreshState,
                watchConnectionState = watchConnectionState,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights,
                liveNotificationPreference = liveNotificationPreference,
                liveNotificationState = liveNotificationState,
                notificationExpansionMode = notificationExpansionMode,
                activeTab = activeTab,
                sectorDisplayMode = sectorDisplayMode,
                sectorSortOrder = sectorSortOrder,
                marketSessionStatus = marketSessionStatus,
                lastSyncedTimestamp = lastSyncedTimestamp,
                formattedLastSynced = formattedTime,
                errorMessage = errorMessage
            )
        }
    }
}
