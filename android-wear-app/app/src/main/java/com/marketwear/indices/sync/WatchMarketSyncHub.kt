package com.marketwear.indices.sync

import android.content.Context
import androidx.wear.tiles.TileService
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.WatchPortfolioSelectionStore
import com.marketwear.indices.notification.WatchLiveNotificationManager
import com.marketwear.indices.tile.MarketWearPortfolioTileService
import com.marketwear.shared.ui.MarketDashboardState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Single Source of Truth Reactive Dispatcher for Wear OS.
 * Observes the repository's single authoritative [MarketRepository.dashboardFlow]
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
    private val selectionStore: WatchPortfolioSelectionStore? = null,
    private val liveNotificationManager: WatchLiveNotificationManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    init {
        scope.launch {
            repository.dashboardFlow.collect { dashboard ->
                dispatchToAllModalities(dashboard)
            }
        }
    }

    private fun dispatchToAllModalities(dashboard: MarketDashboardState) {
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

        // 3. Ongoing Wrist Notification & OngoingActivity (driven directly by the single unified dashboard flow)
        try {
            liveNotificationManager.showOrUpdateFromDashboard(dashboard)
        } catch (_: Exception) {
            // Ignored if notifications disabled
        }
    }
}
