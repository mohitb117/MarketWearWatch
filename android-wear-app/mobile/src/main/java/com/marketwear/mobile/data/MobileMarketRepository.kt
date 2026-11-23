package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.MarketSnapshot
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.ui.PortfolioWeightMode
import com.marketwear.shared.ui.RefreshPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Authoritative phone-side repository for market quotes and portfolio state.
 * Emits unified [MarketSnapshot] instances as a Single Source of Truth for
 * the mobile dashboard UI, live notifications, glance widget, and wear data sync.
 */
class MobileMarketRepository(
    private val client: KtorMarketClient = KtorMarketClient(),
    private val selectionStore: PortfolioSelectionStore? = null
) {
    private val initialQuotes: List<IndexQuote> =
        selectionStore?.cachedQuotes?.value?.ifEmpty { null } ?: emptyList()

    private val _quotes = MutableStateFlow<List<IndexQuote>>(initialQuotes)
    val quotes: StateFlow<List<IndexQuote>> = _quotes.asStateFlow()

    private val _snapshot = MutableStateFlow(buildSnapshot(initialQuotes))
    val snapshotFlow: StateFlow<MarketSnapshot> = _snapshot.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: MobileMarketRepository? = null

        fun getInstance(selectionStore: PortfolioSelectionStore? = null): MobileMarketRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MobileMarketRepository(selectionStore = selectionStore).also { INSTANCE = it }
            }
        }
    }

    private fun buildSnapshot(
        quotes: List<IndexQuote>,
        intraday: Map<String, TickerIntradayData> = client.getCachedPortfolioTickerData(),
        disabledIds: Set<String> = selectionStore?.disabledHoldingIds?.value ?: emptySet(),
        customWeights: Map<String, Double> = selectionStore?.customWeights?.value ?: emptyMap(),
        weightMode: PortfolioWeightMode = selectionStore?.weightMode?.value ?: PortfolioWeightMode.NORMALIZED_100
    ): MarketSnapshot {
        val latestTs = quotes.maxOfOrNull { it.lastUpdated } ?: System.currentTimeMillis()
        return MarketSnapshot(
            quotes = quotes,
            timestampMillis = latestTs,
            disabledHoldingIds = disabledIds,
            customWeights = customWeights,
            weightMode = weightMode,
            tickerIntradayData = intraday
        )
    }

    /**
     * Atomically commits quotes into the single source of truth, persisting to
     * [PortfolioSelectionStore] and updating [snapshotFlow].
     */
    fun commitQuotes(
        quotes: List<IndexQuote>,
        intradayData: Map<String, TickerIntradayData> = emptyMap()
    ) {
        if (quotes.isEmpty()) return
        _quotes.value = quotes
        selectionStore?.saveCachedQuotes(quotes)
        _snapshot.value = buildSnapshot(
            quotes = quotes,
            intraday = intradayData.ifEmpty { client.getCachedPortfolioTickerData() }
        )
    }

    /**
     * Fetches fresh quotes from Yahoo Finance for Portfolio Score, major indices, and all 11 sectors.
     * Throws on total network failure so workers and callers are aware of failures
     * and NEVER syncs dummy/fake quotes to the watch.
     */
    suspend fun fetchAllQuotes(
        policy: RefreshPolicy = RefreshPolicy.CACHE_FIRST,
        disabledHoldingIds: Set<String> = selectionStore?.disabledHoldingIds?.value ?: emptySet(),
        customWeights: Map<String, Double> = selectionStore?.customWeights?.value ?: emptyMap()
    ): List<IndexQuote> = withContext(Dispatchers.IO) {
        val results = client.fetchAllQuotes(
            policy = policy,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        if (results.isEmpty()) {
            throw RuntimeException("Failed to fetch market quotes: empty result set")
        }
        val intraday = client.getCachedPortfolioTickerData()
        commitQuotes(results, intraday)
        results
    }

    fun getCachedPortfolioTickerData(): Map<String, TickerIntradayData> =
        client.getCachedPortfolioTickerData()

    /**
     * Instantaneous offline recalculation of the Portfolio Score when toggled holdings or custom weights change.
     */
    fun recomputePortfolioForSelection(
        disabledHoldingIds: Set<String>,
        customWeights: Map<String, Double> = emptyMap()
    ): List<IndexQuote> {
        val current = _quotes.value.ifEmpty {
            selectionStore?.cachedQuotes?.value?.ifEmpty { null } ?: MarketRegistry.getFallbackQuotes()
        }
        val recomputed = client.recomputePortfolioQuote(disabledHoldingIds, customWeights) ?: return current
        val updated = listOf(recomputed) + current.filter { it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL }
        commitQuotes(updated)
        return updated
    }
}
