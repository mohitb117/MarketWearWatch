package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketInstrument
import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.createPlatformHttpClient
import io.ktor.client.HttpClient

class KtorMarketClient(
    client: HttpClient = createDefaultHttpClient()
) {
    private val sharedClient = SharedMarketClient(client)

    suspend fun fetchQuote(symbol: String): IndexQuote = sharedClient.fetchQuote(symbol)

    suspend fun fetchQuote(instrument: MarketInstrument): IndexQuote = sharedClient.fetchQuote(instrument)

    suspend fun fetchAllQuotes(forceRefresh: Boolean = false): List<IndexQuote> =
        sharedClient.fetchAllQuotes(forceRefresh)

    companion object {
        fun createDefaultHttpClient(): HttpClient = createPlatformHttpClient()
    }
}
