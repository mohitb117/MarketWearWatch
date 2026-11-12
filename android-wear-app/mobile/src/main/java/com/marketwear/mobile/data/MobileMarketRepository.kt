package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
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
     * Fetches fresh quotes from Yahoo Finance.
     * Throws on network failure so workers and callers are aware of failures
     * and NEVER syncs dummy/fake quotes to the watch.
     */
    suspend fun fetchAllQuotes(): List<IndexQuote> = withContext(Dispatchers.IO) {
        val symbols = listOf("SPX", "NDX")
        val results = mutableListOf<IndexQuote>()

        for (sym in symbols) {
            val fresh = client.fetchQuote(sym)
            results.add(fresh)
        }

        _quotes.value = results
        results
    }
}
