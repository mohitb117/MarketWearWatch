package com.marketwear.indices.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.marketwear.indices.data.db.IndexQuoteDao
import com.marketwear.indices.data.db.IndexQuoteEntity
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.isUsMarketWindowActive
import com.marketwear.shared.ui.MarketDashboardState
import com.marketwear.shared.ui.MarketUiStateBuilder
import com.marketwear.shared.ui.RefreshPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository responsible for managing market indices data using Room as the single source of truth.
 * Supports offloading network sync to paired phone with concurrent standalone fallback and
 * persisting user-configured portfolio holding selections.
 */
class MarketRepository(
    private val context: Context,
    private val dao: IndexQuoteDao,
    private val sharedClient: SharedMarketClient,
    private val selectionStore: WatchPortfolioSelectionStore = WatchPortfolioSelectionStore(context)
) {

    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val dataClient = Wearable.getDataClient(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val quotesFlow: Flow<List<IndexQuote>> = dao.getAllQuotesFlow().map { list ->
        if (list.isEmpty()) {
            MarketRegistry.getFallbackQuotes()
        } else {
            list.map { it.toDomainModel() }
        }
    }

    /**
     * Single Authoritative Unified Data Flow for Wear OS.
     * Combines Room quotes with user portfolio preferences (weight mode, custom weights, disabled holdings)
     * at the persistence layer, pre-computing the exact [MarketDashboardState] for all presentation modalities.
     */
    val dashboardFlow: StateFlow<MarketDashboardState> = combine(
        quotesFlow,
        selectionStore.weightMode,
        selectionStore.disabledHoldingIds,
        selectionStore.customWeights
    ) { quotes, weightMode, disabledIds, customWeights ->
        MarketUiStateBuilder.buildDashboardState(
            quotes = quotes,
            weightMode = weightMode,
            disabledHoldingIds = disabledIds,
            customWeights = customWeights
        )
    }.stateIn(
        scope = repoScope,
        started = SharingStarted.Eagerly,
        initialValue = MarketUiStateBuilder.buildDashboardState(
            quotes = MarketRegistry.getFallbackQuotes(),
            weightMode = selectionStore.weightMode.value,
            disabledHoldingIds = selectionStore.disabledHoldingIds.value,
            customWeights = selectionStore.customWeights.value
        )
    )

    companion object {
        private const val ACTIVE_FRESHNESS_MS = 5 * 60 * 1000L       // 5 minutes during market hours
        private const val OFF_HOURS_FRESHNESS_MS = 6 * 60 * 60 * 1000L // 6 hours overnight/weekends
    }

    /**
     * Complication-safe quote retrieval: strictly offline Room read.
     */
    suspend fun getQuote(symbol: String): IndexQuote = withContext(Dispatchers.IO) {
        dao.getQuote(symbol)?.toDomainModel() ?: getFallbackQuote(symbol)
    }

    /**
     * Battery-aware refresh driven by [RefreshPolicy]:
     * Skips network/Data Layer wakeups under [RefreshPolicy.CACHE_FIRST] when cached Room data is fresh.
     * Otherwise delegates to paired phone via Wearable Data Layer, or uses concurrent Ktor fallback.
     */
    suspend fun refreshWithPhoneFallback(
        policy: RefreshPolicy = RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO
    ): Unit = withContext(Dispatchers.IO) {
        if (policy == RefreshPolicy.CACHE_FIRST && isCacheFresh()) {
            return@withContext
        }

        // Signal paired phone to refresh its own local state if reachable
        val phoneNode = getConnectedPhoneNode()
        if (phoneNode != null) {
            try {
                messageClient.sendMessage(
                    phoneNode.id,
                    WearConstants.PATH_REQUEST_REFRESH,
                    ByteArray(0)
                ).await()
            } catch (_: Exception) {
                // Phone notification failure is non-fatal
            }
        }

        // Always perform authoritative fetch directly on watch (works over Wi-Fi, LTE, or Bluetooth internet proxy).
        // This guarantees the local Room DB, complications, tiles, and notifications update immediately.
        refreshAllOnline(policy = policy)
    }

    private suspend fun isCacheFresh(): Boolean {
        val cached = dao.getAllQuotes()
        if (cached.size < MarketRegistry.ALL_INSTRUMENTS.size) return false
        val now = System.currentTimeMillis()
        val maxAge = if (isUsMarketWindowActive()) ACTIVE_FRESHNESS_MS else OFF_HOURS_FRESHNESS_MS
        val allWithinTtl = cached.all { (now - it.lastUpdated) < maxAge }
        if (!allWithinTtl) return false

        // Invalidate if regular US equity markets opened since the quotes were cached
        val oldestUpdate = cached.minOfOrNull { it.lastUpdated } ?: 0L
        return !com.marketwear.shared.platform.hasMarketOpenedSince(oldestUpdate, now)
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

    suspend fun refreshAllOnline(
        policy: RefreshPolicy = RefreshPolicy.CACHE_FIRST
    ): List<IndexQuote> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext dao.getAllQuotes().map { it.toDomainModel() }
        }

        val fetchedQuotes = try {
            sharedClient.fetchAllQuotes(
                policy = policy,
                disabledHoldingIds = selectionStore.disabledHoldingIds.value,
                customWeights = selectionStore.customWeights.value
            )
        } catch (_: Exception) {
            emptyList()
        }

        val intradayCache = sharedClient.getCachedPortfolioTickerData()
        if (intradayCache.isNotEmpty()) {
            selectionStore.saveTickerIntradayData(intradayCache)
        }

        if (fetchedQuotes.isNotEmpty()) {
            val entities = fetchedQuotes.map { IndexQuoteEntity.fromDomainModel(it) }
            dao.insertOrUpdate(entities)
            syncQuotesToPhone(fetchedQuotes, intradayCache)
        }

        dao.getAllQuotes().map { it.toDomainModel() }
    }

    suspend fun syncQuotesToPhone(
        quotes: List<IndexQuote>,
        intradayData: Map<String, com.marketwear.shared.model.TickerIntradayData> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_MARKET_QUOTES)
            val dataMap = putDataMapReq.dataMap
            dataMap.putString(WearConstants.KEY_QUOTES_PAYLOAD, json.encodeToString(quotes))
            dataMap.putLong(WearConstants.KEY_SYNC_TIMESTAMP, System.currentTimeMillis())
            if (intradayData.isNotEmpty()) {
                dataMap.putString(
                    WearConstants.KEY_PORTFOLIO_INTRADAY_PAYLOAD,
                    json.encodeToString(intradayData)
                )
            }
            val putRequest = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putRequest).await()
        } catch (_: Exception) {
            // Local data remains authoritative on Watch if phone disconnected
        }
    }

    suspend fun persistRecomputedPortfolioQuote(quote: IndexQuote) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(IndexQuoteEntity.fromDomainModel(quote))
    }

    suspend fun syncDisabledHoldingsToPhone(
        disabledHoldingIds: Set<String>,
        customWeights: Map<String, Double> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            val putDataMapReq = PutDataMapRequest.create(WearConstants.PATH_PORTFOLIO_CONFIG)
            val dataMap = putDataMapReq.dataMap
            dataMap.putString(
                WearConstants.KEY_DISABLED_HOLDING_IDS,
                json.encodeToString(disabledHoldingIds.toList())
            )
            dataMap.putString(
                WearConstants.KEY_CUSTOM_WEIGHTS,
                json.encodeToString(customWeights)
            )
            dataMap.putLong(WearConstants.KEY_CONFIG_TIMESTAMP, System.currentTimeMillis())

            val putRequest = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putRequest).await()
        } catch (_: Exception) {
            // Local selection store remains authoritative on Watch
        }
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
