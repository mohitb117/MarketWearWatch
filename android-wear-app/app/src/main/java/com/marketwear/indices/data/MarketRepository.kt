package com.marketwear.indices.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.marketwear.indices.data.db.IndexQuoteEntity
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.isUsMarketWindowActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository responsible for managing market indices data using Room as the single source of truth.
 * Supports offloading network sync to paired phone with concurrent standalone fallback.
 */
class MarketRepository private constructor(private val context: Context) {

    private val db = MarketDatabase.getInstance(context)
    private val dao = db.indexQuoteDao()
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val sharedClient by lazy { SharedMarketClient() }

    val quotesFlow: Flow<List<IndexQuote>> = dao.getAllQuotesFlow().map { list ->
        if (list.isEmpty()) {
            MarketRegistry.getFallbackQuotes()
        } else {
            list.map { it.toDomainModel() }
        }
    }

    companion object {
        private const val ACTIVE_FRESHNESS_MS = 5 * 60 * 1000L       // 5 minutes during market hours
        private const val OFF_HOURS_FRESHNESS_MS = 6 * 60 * 60 * 1000L // 6 hours overnight/weekends

        @Volatile
        private var INSTANCE: MarketRepository? = null

        fun getInstance(context: Context): MarketRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MarketRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Complication-safe quote retrieval: strictly offline Room read.
     */
    suspend fun getQuote(symbol: String): IndexQuote = withContext(Dispatchers.IO) {
        dao.getQuote(symbol)?.toDomainModel() ?: getFallbackQuote(symbol)
    }

    /**
     * Battery-aware refresh:
     * Skips network/Data Layer wakeups when `!force` and cached Room data is already fresh.
     * Otherwise delegates to paired phone via Wearable Data Layer, or uses concurrent Ktor fallback.
     */
    suspend fun refreshWithPhoneFallback(force: Boolean = true): Unit = withContext(Dispatchers.IO) {
        if (!force && isCacheFresh()) {
            return@withContext
        }

        val phoneNode = getConnectedPhoneNode()
        if (phoneNode != null) {
            try {
                messageClient.sendMessage(
                    phoneNode.id,
                    WearConstants.PATH_REQUEST_REFRESH,
                    ByteArray(0)
                ).await()
                return@withContext
            } catch (_: Exception) {
                // If message failed, fall through to direct fetch
            }
        }

        // Fallback: concurrent online fetch on watch (single pooled Ktor client)
        refreshAllOnline()
    }

    private suspend fun isCacheFresh(): Boolean {
        val cached = dao.getAllQuotes()
        if (cached.size < MarketRegistry.ALL_INSTRUMENTS.size) return false
        val now = System.currentTimeMillis()
        val maxAge = if (isUsMarketWindowActive()) ACTIVE_FRESHNESS_MS else OFF_HOURS_FRESHNESS_MS
        return cached.all { (now - it.lastUpdated) < maxAge }
    }

    private suspend fun getConnectedPhoneNode(): com.google.android.gms.wearable.Node? {
        return try {
            val capabilityInfo = capabilityClient
                .getCapability(WearConstants.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE)
                .await()
            capabilityInfo.nodes.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshAllOnline(): List<IndexQuote> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext dao.getAllQuotes().map { it.toDomainModel() }
        }

        val fetchedQuotes = try {
            sharedClient.fetchAllQuotes()
        } catch (_: Exception) {
            emptyList()
        }

        if (fetchedQuotes.isNotEmpty()) {
            val entities = fetchedQuotes.map { IndexQuoteEntity.fromDomainModel(it) }
            dao.insertOrUpdate(entities)
            MarketSyncWorker.triggerComplicationUpdates(context)
        }

        dao.getAllQuotes().map { it.toDomainModel() }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getFallbackQuote(symbol: String): IndexQuote {
        return MarketRegistry.getFallbackQuotes().find { it.symbol == symbol }
            ?: IndexQuote(symbol, symbol, 1000.0, 0.0, 0.0, 1000.0, 1000.0, System.currentTimeMillis())
    }
}
