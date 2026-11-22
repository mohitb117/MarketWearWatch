package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketInstrument
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.createPlatformHttpClient
import com.marketwear.shared.ui.RefreshPolicy
import io.ktor.client.HttpClient

class KtorMarketClient(
    client: HttpClient = createDefaultHttpClient()
) {
    private val sharedClient = SharedMarketClient(client)

    suspend fun fetchQuote(symbol: String): IndexQuote = sharedClient.fetchQuote(symbol)

    suspend fun fetchQuote(instrument: MarketInstrument): IndexQuote = sharedClient.fetchQuote(instrument)

    suspend fun fetchAllQuotes(
        policy: RefreshPolicy = RefreshPolicy.CACHE_FIRST,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): List<IndexQuote> = sharedClient.fetchAllQuotes(
        policy = policy,
        disabledHoldingIds = disabledHoldingIds,
        customWeights = customWeights
    )

    fun getCachedPortfolioTickerData(): Map<String, TickerIntradayData> =
        sharedClient.getCachedPortfolioTickerData()

    fun recomputePortfolioQuote(
        disabledHoldingIds: Set<String>,
        customWeights: Map<String, Double> = emptyMap()
    ): IndexQuote? = sharedClient.recomputePortfolioQuote(
        disabledHoldingIds = disabledHoldingIds,
        customWeights = customWeights
    )

    companion object {
        fun createDefaultHttpClient(): HttpClient = createPlatformHttpClient()
    }
}
