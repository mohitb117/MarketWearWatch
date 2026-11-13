package com.marketwear.mobile.appfunctions

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.service.AppFunction
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.platform.isUsMarketWindowActive
import kotlin.math.abs

/**
 * Structured quote payload returned to system Gemini / AI agents via Android 16 AppFunctions.
 */
@AppFunctionSerializable
data class MarketQuoteResponse(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val percentChange: Double,
    val dayHigh: Double,
    val dayLow: Double,
    val category: String,
    val lastUpdatedMillis: Long
)

/**
 * Full US market snapshot covering S&P 500, Nasdaq 100, and all 11 GICS sectors.
 */
@AppFunctionSerializable
data class MarketSummaryResponse(
    val isUsMarketOpenWindow: Boolean,
    val totalInstruments: Int,
    val quotes: List<MarketQuoteResponse>
)

/**
 * Result of refreshing market data and pushing it to the paired Wear OS watch.
 */
@AppFunctionSerializable
data class SyncWatchResponse(
    val syncedCount: Int,
    val watchConnected: Boolean,
    val message: String
)

private fun IndexQuote.toResponse(): MarketQuoteResponse = MarketQuoteResponse(
    symbol = symbol,
    name = name,
    price = price,
    change = change,
    percentChange = percentChange,
    dayHigh = dayHigh,
    dayLow = dayLow,
    category = category,
    lastUpdatedMillis = lastUpdated
)

/**
 * Android 16 AppFunctions provider (on-device MCP server) that lets system Gemini
 * query MarketWear's US index and sector data and trigger Wear OS syncs.
 */
class MarketAppFunctions(
    private val repository: MobileMarketRepository,
    private val syncManager: WearDataSyncManager
) {

    private suspend fun getOrFetchQuotes(forceRefresh: Boolean): List<IndexQuote> {
        val cached = repository.quotes.value
        return if (!forceRefresh && cached.isNotEmpty()) {
            cached
        } else {
            repository.fetchAllQuotes()
        }
    }

    /**
     * Gets the latest market quote for a specific US market index or GICS sector.
     * Supports symbols like SPX (S&P 500), NDX (Nasdaq 100), YH101-YH311, or sector names
     * such as Technology, Healthcare, Energy, Financials, Utilities, or Industrials.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param query Ticker symbol (e.g. SPX, NDX, ^GSPC, YH311) or sector/index display name.
     * @param forceRefresh When true, fetches fresh quotes from Yahoo Finance instead of using cache.
     */
    @AppFunction
    suspend fun getMarketQuote(
        appFunctionContext: AppFunctionContext,
        query: String,
        forceRefresh: Boolean = false
    ): MarketQuoteResponse {
        val quotes = getOrFetchQuotes(forceRefresh)
        return findMatchingQuote(quotes, query).toResponse()
    }

    internal fun findMatchingQuote(quotes: List<IndexQuote>, query: String): IndexQuote {
        val normalized = query.trim().lowercase()
        val bareQuery = normalized.removePrefix("^")
        return quotes.firstOrNull { quote ->
            quote.symbol.equals(normalized, ignoreCase = true) ||
                quote.symbol.removePrefix("^").equals(bareQuery, ignoreCase = true) ||
                quote.name.lowercase().contains(normalized) ||
                (normalized in listOf("^gspc", "s&p", "s&p 500", "sp500") && quote.symbol == "SPX") ||
                (normalized in listOf("^ndx", "nasdaq", "nasdaq 100", "qqq") && quote.symbol == "NDX")
        } ?: throw IllegalArgumentException(
            "Unknown market instrument '$query'. Available symbols: ${quotes.joinToString { it.symbol }}"
        )
    }

    /**
     * Gets all tracked US market quotes including S&P 500, Nasdaq 100, and all 11 GICS sectors,
     * along with whether the US market window is currently active.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param forceRefresh When true, fetches fresh quotes from Yahoo Finance instead of using cache.
     */
    @AppFunction
    suspend fun getAllMarketQuotes(
        appFunctionContext: AppFunctionContext,
        forceRefresh: Boolean = false
    ): MarketSummaryResponse {
        val quotes = getOrFetchQuotes(forceRefresh)
        return MarketSummaryResponse(
            isUsMarketOpenWindow = isUsMarketWindowActive(),
            totalInstruments = quotes.size,
            quotes = quotes.map { it.toResponse() }
        )
    }

    /**
     * Gets the top moving US market indices and GICS sectors sorted by absolute percentage change.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param limit Maximum number of movers to return (clamped between 1 and 13).
     * @param forceRefresh When true, fetches fresh quotes before ranking movers.
     */
    @AppFunction
    suspend fun getTopMarketMovers(
        appFunctionContext: AppFunctionContext,
        limit: Int = 5,
        forceRefresh: Boolean = false
    ): List<MarketQuoteResponse> {
        val clamped = limit.coerceIn(1, 13)
        return getOrFetchQuotes(forceRefresh)
            .sortedByDescending { abs(it.percentChange) }
            .take(clamped)
            .map { it.toResponse() }
    }

    /**
     * Fetches fresh market quotes from Yahoo Finance and pushes them to the paired Wear OS watch.
     *
     * @param appFunctionContext The AppFunction execution context.
     */
    @AppFunction
    suspend fun refreshAndSyncToWatch(
        appFunctionContext: AppFunctionContext
    ): SyncWatchResponse {
        val quotes = repository.fetchAllQuotes()
        val connected = syncManager.isWatchConnected()
        if (connected) {
            syncManager.syncQuotesToWatch(quotes)
        }
        return SyncWatchResponse(
            syncedCount = quotes.size,
            watchConnected = connected,
            message = if (connected) {
                "Refreshed ${quotes.size} quotes and queued sync to Wear OS watch."
            } else {
                "Refreshed ${quotes.size} quotes; no Wear OS watch is currently connected."
            }
        )
    }
}
