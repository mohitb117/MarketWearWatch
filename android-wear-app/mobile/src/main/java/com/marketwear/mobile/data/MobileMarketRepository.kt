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

    suspend fun fetchAllQuotes(): List<IndexQuote> = withContext(Dispatchers.IO) {
        val symbols = listOf("SPX", "NDX")
        val results = mutableListOf<IndexQuote>()

        for (sym in symbols) {
            try {
                results.add(client.fetchQuote(sym))
            } catch (e: Exception) {
                // Keep last known quote if available
                val existing = _quotes.value.find { it.symbol == sym }
                if (existing != null) {
                    results.add(existing)
                } else {
                    results.add(getFallbackQuote(sym))
                }
            }
        }

        _quotes.value = results
        results
    }

    private fun getFallbackQuote(symbol: String): IndexQuote {
        return if (symbol == "SPX") {
            IndexQuote("SPX", "S&P 500", 5985.40, 42.15, 0.71, 6010.50, 5940.20, System.currentTimeMillis())
        } else {
            IndexQuote("NDX", "Nasdaq 100", 21450.80, 230.10, 1.08, 21520.00, 21280.00, System.currentTimeMillis())
        }
    }
}
