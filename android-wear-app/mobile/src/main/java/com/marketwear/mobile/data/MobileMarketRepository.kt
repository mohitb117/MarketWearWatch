package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.ui.RefreshPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class MobileMarketRepository(
    private val client: KtorMarketClient = KtorMarketClient()
) {
    private val _quotes = MutableStateFlow<List<IndexQuote>>(emptyList())
    val quotes: StateFlow<List<IndexQuote>> = _quotes.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: MobileMarketRepository? = null

        fun getInstance(): MobileMarketRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MobileMarketRepository().also { INSTANCE = it }
            }
        }
    }

    /**
     * Fetches fresh quotes from Yahoo Finance for Portfolio Score, major indices, and all 11 sectors.
     * Throws on total network failure so workers and callers are aware of failures
     * and NEVER syncs dummy/fake quotes to the watch.
     */
    suspend fun fetchAllQuotes(
        policy: RefreshPolicy = RefreshPolicy.CACHE_FIRST,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): List<IndexQuote> = withContext(Dispatchers.IO) {
        val results = client.fetchAllQuotes(
            policy = policy,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        if (results.isEmpty()) {
            throw RuntimeException("Failed to fetch market quotes: empty result set")
        }
        _quotes.value = results
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
        val current = _quotes.value.ifEmpty { MarketRegistry.getFallbackQuotes() }
        val recomputed = client.recomputePortfolioQuote(disabledHoldingIds, customWeights) ?: return current
        val updated = listOf(recomputed) + current.filter { it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL }
        _quotes.value = updated
        return updated
    }
}
