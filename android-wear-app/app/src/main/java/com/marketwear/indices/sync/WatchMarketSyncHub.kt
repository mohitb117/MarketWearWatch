package com.marketwear.indices.sync

import android.content.Context
import androidx.wear.tiles.TileService
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.WatchPortfolioSelectionStore
import com.marketwear.indices.notification.WatchLiveNotificationManager
import com.marketwear.indices.tile.MarketWearPortfolioTileService
import com.marketwear.shared.ui.PortfolioWeightMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class WatchSyncState(
    val quotes: List<IndexQuote>,
    val disabledIds: Set<String>,
    val customWeights: Map<String, Double>,
    val weightMode: PortfolioWeightMode
)

/**
 * Single Source of Truth Reactive Dispatcher for Wear OS.
 * Observes the Room database [MarketRepository.quotesFlow] and [WatchPortfolioSelectionStore]
 * to automatically and simultaneously synchronize all on-wrist presentation modalities:
 *  1. Complications (S&P 500, Nasdaq 100, Dual Glance, Portfolio Score)
 *  2. Wear OS Carousel Tile ([MarketWearPortfolioTileService])
 *  3. Ongoing Wrist Notification & OngoingActivity ([WatchLiveNotificationManager])
 *
 * Eliminates ad-hoc, fragmented imperative calls across listeners, workers, and view models.
 */
class WatchMarketSyncHub(
    private val context: Context,
    private val repository: MarketRepository,
    private val selectionStore: WatchPortfolioSelectionStore,
    private val liveNotificationManager: WatchLiveNotificationManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    init {
        scope.launch {
            combine(
                repository.quotesFlow,
                selectionStore.disabledHoldingIds,
                selectionStore.customWeights,
                selectionStore.weightMode
            ) { quotes, disabledIds, customWeights, weightMode ->
                WatchSyncState(quotes, disabledIds, customWeights, weightMode)
            }.distinctUntilChanged().collect { state ->
                if (state.quotes.isNotEmpty()) {
                    dispatchToAllModalities(
                        quotes = state.quotes,
                        disabledIds = state.disabledIds,
                        customWeights = state.customWeights,
                        weightMode = state.weightMode
                    )
                }
            }
        }
    }

    private fun dispatchToAllModalities(
        quotes: List<IndexQuote>,
        disabledIds: Set<String>,
        customWeights: Map<String, Double>,
        weightMode: PortfolioWeightMode
    ) {
        // 1. Wear Complications
        try {
            MarketSyncWorker.triggerComplicationUpdates(context)
        } catch (_: Exception) {
            // Ignored if complication manager throws
        }

        // 2. Wear OS Carousel Tile
        try {
            TileService.getUpdater(context).requestUpdate(MarketWearPortfolioTileService::class.java)
        } catch (_: Exception) {
            // Ignored if Tile service is uninstalled/inactive
        }

        // 3. Ongoing Wrist Notification & OngoingActivity
        try {
            liveNotificationManager.showOrUpdateFromQuotes(
                quotes = quotes,
                weightMode = weightMode,
                disabledHoldingIds = disabledIds,
                customWeights = customWeights
            )
        } catch (_: Exception) {
            // Ignored if notifications disabled
        }
    }
}
