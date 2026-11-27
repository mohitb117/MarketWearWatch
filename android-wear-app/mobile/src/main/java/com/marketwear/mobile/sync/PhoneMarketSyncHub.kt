package com.marketwear.mobile.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.mobile.notification.PortfolioLiveNotificationManager
import com.marketwear.mobile.widget.MarketGlanceWidget
import com.marketwear.shared.ui.MarketDashboardState
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.SyncUrgency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Process-wide Single Source of Truth Reactive Dispatcher for Android Mobile.
 * Observes [MobileMarketRepository.dashboardFlow] and automatically coordinates:
 *  1. Android 16 Live Notifications (or legacy ongoing progress bar notifications).
 *  2. Android Glance AppWidget updates on the home screen.
 *  3. Bidirectional Wearable Data Layer synchronization to the paired Pixel Watch.
 *
 * Eliminates ad-hoc imperative calls across background workers, viewmodels, and receivers.
 */
class PhoneMarketSyncHub(
    private val context: Context,
    private val repository: MobileMarketRepository,
    private val selectionStore: PortfolioSelectionStore,
    private val liveNotificationManager: PortfolioLiveNotificationManager,
    private val syncManager: WearDataSyncManager,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private var lastSyncedQuotes: List<com.marketwear.shared.model.IndexQuote> = emptyList()

    init {
        // 1. Reactive observation of the Single Source of Truth unified dashboard flow
        scope.launch {
            repository.dashboardFlow.collect { dashboard ->
                dispatchDashboardToModalities(dashboard)
            }
        }

        // 2. Reactive observation of Live Notification preference (Active vs Paused)
        scope.launch {
            selectionStore.liveNotificationPreference.drop(1).collect { preference ->
                when (preference) {
                    LiveNotificationPreference.ACTIVE -> {
                        dispatchNotification(repository.dashboardFlow.value)
                    }
                    LiveNotificationPreference.PAUSED -> {
                        liveNotificationManager.dismiss()
                    }
                }
            }
        }

        // 3. Reactive observation of notification expansion mode changes
        scope.launch {
            selectionStore.notificationExpansionMode.drop(1).collect {
                if (selectionStore.liveNotificationPreference.value == LiveNotificationPreference.ACTIVE) {
                    dispatchNotification(repository.dashboardFlow.value)
                }
            }
        }
    }

    private fun dispatchDashboardToModalities(dashboard: MarketDashboardState) {
        // A. Live Notification (driven directly by the pre-computed liveUpdateSpec from the repository)
        if (selectionStore.liveNotificationPreference.value == LiveNotificationPreference.ACTIVE) {
            dispatchNotification(dashboard)
        }

        // B. Glance AppWidget (Home Screen)
        try {
            scope.launch {
                MarketGlanceWidget().updateAll(context)
            }
        } catch (_: Exception) {
            // Glance updates may fail if widget is not placed
        }

        // C. Paired Watch Sync via Wearable Data Layer (deduplicated)
        val snapshot = repository.snapshotFlow.value
        if (snapshot.quotes.isNotEmpty() && snapshot.quotes != lastSyncedQuotes) {
            lastSyncedQuotes = snapshot.quotes
            scope.launch {
                syncManager.syncQuotesToWatch(
                    quotes = snapshot.quotes,
                    urgency = SyncUrgency.IMMEDIATE,
                    intradayData = snapshot.tickerIntradayData
                )
            }
        }
    }

    private fun dispatchNotification(dashboard: MarketDashboardState) {
        try {
            liveNotificationManager.showOrUpdate(
                spec = dashboard.portfolioState.liveUpdateSpec,
                expansionMode = selectionStore.notificationExpansionMode.value
            )
        } catch (_: Exception) {
            // Safeguard against notification posting errors
        }
    }
}
