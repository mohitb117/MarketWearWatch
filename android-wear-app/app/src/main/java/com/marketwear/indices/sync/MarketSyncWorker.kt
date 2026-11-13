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
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.platform.isUsMarketWindowActive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class MarketSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val db: MarketDatabase by inject()
    private val repository: MarketRepository by inject()

    override suspend fun doWork(): Result {
        val quotes = db.indexQuoteDao().getAllQuotes()
        val now = System.currentTimeMillis()

        val maxAge = if (isUsMarketWindowActive()) {
            45 * 60 * 1000L // 45 min during active market hours (phone pushes every 30m)
        } else {
            6 * 60 * 60 * 1000L // 6 hours overnight/weekends when markets are closed
        }

        val isAllFresh = quotes.size >= MarketRegistry.ALL_INSTRUMENTS.size &&
            quotes.all { (now - it.lastUpdated) < maxAge }

        if (isAllFresh) {
            return Result.success()
        }

        return try {
            repository.refreshWithPhoneFallback(force = true)
            triggerComplicationUpdates(applicationContext)
            Result.success()
        } catch (e: Exception) {
            // Avoid aggressive retry storms on watch battery if cached quotes exist
            if (quotes.isNotEmpty()) Result.success() else Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "MarketSyncWorkerPeriodic"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Watch acts as a 60-minute standalone safety net; Phone companion pushes updates passively
            val syncRequest = PeriodicWorkRequestBuilder<MarketSyncWorker>(
                60, TimeUnit.MINUTES,
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
