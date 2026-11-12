package com.marketwear.indices.sync

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.marketwear.indices.MarketDualComplicationService
import com.marketwear.indices.NasdaqComplicationService
import com.marketwear.indices.Sp500ComplicationService
import com.marketwear.indices.data.MarketRepository
import java.util.concurrent.TimeUnit

class MarketSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = MarketRepository.getInstance(applicationContext)
        return try {
            repository.refreshAllOnline()
            triggerComplicationUpdates(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "MarketSyncWorkerPeriodic"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // 15 minutes is the minimum PeriodicWorkRequest interval in Android WorkManager
            val syncRequest = PeriodicWorkRequestBuilder<MarketSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun triggerComplicationUpdates(context: Context) {
            val services = listOf(
                Sp500ComplicationService::class.java,
                NasdaqComplicationService::class.java,
                MarketDualComplicationService::class.java
            )

            services.forEach { serviceClass ->
                try {
                    val requester = ComplicationDataSourceUpdateRequester.create(
                        context,
                        ComponentName(context, serviceClass)
                    )
                    requester.requestUpdateAll()
                } catch (_: Exception) {
                    // Ignored if complication is not placed on current watch face
                }
            }
        }
    }
}
