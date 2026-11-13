package com.marketwear.mobile.appfunctions

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.mobile.sync.WearDataSyncManager
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.platform.isUsMarketWindowActive
import kotlin.math.abs

/**
 * Structured quote payload returned to system Gemini / AI agents via Android 16 AppFunctions.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
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
@AppFunctionSerializable(isDescribedByKDoc = true)
data class MarketSummaryResponse(
    val isUsMarketOpenWindow: Boolean,
    val totalInstruments: Int,
    val quotes: List<MarketQuoteResponse>
)

/**
 * Result of refreshing market data and pushing it to the paired Wear OS watch.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SyncWatchResponse(
    val syncedCount: Int,
    val watchConnected: Boolean,
    val message: String
)

/**
 * Portfolio Daily Score payload exposing both Normalized (100% basket) and Unnormalized (59.41% raw weight)
 * 100-baseline scores and intraday curve points to system Gemini.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PortfolioScoreResponse(
    val normalizedScore: Double,
    val normalizedChangePercent: Double,
    val unnormalizedScore: Double,
    val unnormalizedChangePercent: Double,
    val trackedHoldingsCount: Int,
    val rawPortfolioWeightPercent: Double,
    val dayHighNormalized: Double,
    val dayLowNormalized: Double,
    val intradaySparklineNormalized: DoubleArray,
    val lastUpdatedMillis: Long
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
     * @param query Ticker symbol (e.g. SPX, NDX, ^GSPC, YH311) or sector/index display name.
     * @param forceRefresh When true, fetches fresh quotes from Yahoo Finance instead of using cache.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = true)
    suspend fun getMarketQuote(
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
     * @param forceRefresh When true, fetches fresh quotes from Yahoo Finance instead of using cache.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = true)
    suspend fun getAllMarketQuotes(
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
     * @param limit Maximum number of movers to return (clamped between 1 and 13).
     * @param forceRefresh When true, fetches fresh quotes before ranking movers.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = true)
    suspend fun getTopMarketMovers(
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
     * Gets the 100-baseline daily Portfolio Score across the 37 tracked UBS and Fidelity holdings,
     * returning both Normalized (100% basket) and Unnormalized (59.41% raw portfolio weight) scores
     * along with the intraday score curve.
     *
     * @param forceRefresh When true, fetches fresh quotes instead of using cached values.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = true)
    suspend fun getPortfolioScore(
        forceRefresh: Boolean = false
    ): PortfolioScoreResponse {
        val quotes = getOrFetchQuotes(forceRefresh)
        val normQuote = quotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }
            ?: PortfolioRegistry.createDefaultFallbackPortfolioQuote(System.currentTimeMillis())
        val unnormQuote = PortfolioRegistry.toUnnormalizedQuote(normQuote)

        return PortfolioScoreResponse(
            normalizedScore = normQuote.price,
            normalizedChangePercent = normQuote.percentChange,
            unnormalizedScore = unnormQuote.price,
            unnormalizedChangePercent = unnormQuote.percentChange,
            trackedHoldingsCount = PortfolioRegistry.HOLDINGS.size,
            rawPortfolioWeightPercent = PortfolioRegistry.TOTAL_RAW_WEIGHT_PERCENT,
            dayHighNormalized = normQuote.dayHigh,
            dayLowNormalized = normQuote.dayLow,
            intradaySparklineNormalized = normQuote.sparkline.toDoubleArray(),
            lastUpdatedMillis = normQuote.lastUpdated
        )
    }

    /**
     * Fetches fresh market quotes from Yahoo Finance and pushes them to the paired Wear OS watch.
     */
    @AppFunctionDeclaration(isDescribedByKDoc = true, isEnabled = true)
    suspend fun refreshAndSyncToWatch(): SyncWatchResponse {
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
