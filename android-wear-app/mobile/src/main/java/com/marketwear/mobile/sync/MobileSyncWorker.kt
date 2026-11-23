package com.marketwear.mobile.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.data.PortfolioSelectionStore
import com.marketwear.shared.platform.isUsMarketWindowActive
import com.marketwear.shared.ui.RefreshPolicy
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Background periodic sync worker for Android Phone.
 * Ingests fresh market quotes into [MobileMarketRepository], which then reactively
 * dispatches updates to live notifications, glance widget, and paired watch via [PhoneMarketSyncHub].
 */
class MobileSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val repository: MobileMarketRepository by inject()
    private val selectionStore: PortfolioSelectionStore by inject()

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
            repository.fetchAllQuotes(
                policy = RefreshPolicy.CACHE_FIRST,
                disabledHoldingIds = disabledIds,
                customWeights = customWeights
            )
            prefs.edit().putLong(KEY_LAST_BG_SYNC, now).apply()
            Result.success()
        } catch (e: Exception) {
            if ((now - lastSync) > OFF_HOURS_MAX_AGE_MS) Result.retry() else Result.success()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "MobileMarketSyncWorker"
        private const val PREFS_NAME = "market_sync_worker_prefs"
        private const val KEY_LAST_BG_SYNC = "key_last_bg_sync_timestamp"
        private const val OFF_HOURS_MAX_AGE_MS = 6 * 60 * 60 * 1000L

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Minimum 15-minute periodic interval on phone companion to conserve battery
            val syncRequest = PeriodicWorkRequestBuilder<MobileSyncWorker>(
                30, TimeUnit.MINUTES,
                15, TimeUnit.MINUTES
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
