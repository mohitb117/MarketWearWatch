package com.marketwear.mobile.sync

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.mobile.notification.PortfolioLiveNotificationManager
import com.marketwear.mobile.widget.MarketGlanceWidget
import com.marketwear.shared.platform.isUsMarketWindowActive
import com.marketwear.shared.ui.LiveNotificationPreference
import com.marketwear.shared.ui.RefreshPolicy
import com.marketwear.shared.ui.SyncUrgency
import com.marketwear.shared.ui.WatchConnectionState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class MobileSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: MobileMarketRepository by inject()
    private val syncManager: WearDataSyncManager by inject()
    private val selectionStore: PortfolioSelectionStore by inject()
    private val liveNotificationManager: PortfolioLiveNotificationManager by inject()

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(KEY_LAST_BG_SYNC, 0L)
        val now = System.currentTimeMillis()

        // Skip background polling overnight and on weekends if we already have recent data (<6 hrs)
        if (!isUsMarketWindowActive() && (now - lastSync) < OFF_HOURS_MAX_AGE_MS) {
            return Result.success()
        }

        return try {
            val disabledIds = selectionStore.disabledHoldingIds.value
            val customWeights = selectionStore.customWeights.value
            val quotes = repository.fetchAllQuotes(
                policy = RefreshPolicy.CACHE_FIRST,
                disabledHoldingIds = disabledIds,
                customWeights = customWeights
            )

            prefs.edit().putLong(KEY_LAST_BG_SYNC, now).apply()

            if (selectionStore.liveNotificationPreference.value == LiveNotificationPreference.ACTIVE) {
                liveNotificationManager.showOrUpdateFromQuotes(
                    quotes = quotes,
                    disabledHoldingIds = disabledIds,
                    customWeights = customWeights
                )
            }

            try {
                MarketGlanceWidget().updateAll(applicationContext)
            } catch (_: Exception) {
                // Ignore widget update errors in background
            }

            if (syncManager.getWatchConnectionState() == WatchConnectionState.CONNECTED) {
                // Non-urgent background sync lets Play Services batch Bluetooth/Wi-Fi transmissions
                syncManager.syncQuotesToWatch(
                    quotes = quotes,
                    urgency = SyncUrgency.COALESCED_BACKGROUND,
                    intradayData = repository.getCachedPortfolioTickerData()
                )
            }

            Result.success()
        } catch (e: Exception) {
            // Only retry if we haven't synced in a long time; avoid retry storms
            if ((now - lastSync) > OFF_HOURS_MAX_AGE_MS) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "MobileMarketSyncWorker"
        private const val PREFS_NAME = "marketwear_mobile_sync"
        private const val KEY_LAST_BG_SYNC = "last_bg_sync_ts"
        private const val OFF_HOURS_MAX_AGE_MS = 6 * 60 * 60 * 1000L // 6 hours

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<MobileSyncWorker>(
                30, TimeUnit.MINUTES,
                10, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
        }
    }
}
