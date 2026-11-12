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
import com.marketwear.mobile.widget.MarketGlanceWidget
import java.util.concurrent.TimeUnit

class MobileSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = MobileMarketRepository.getInstance()
            val quotes = repository.fetchAllQuotes()
            try {
                MarketGlanceWidget().updateAll(applicationContext)
            } catch (e: Exception) {
                // Ignore widget update errors in background
            }
            val syncManager = WearDataSyncManager(applicationContext)
            val success = syncManager.syncQuotesToWatch(quotes)
            if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "MobileMarketSyncWorker"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<MobileSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
