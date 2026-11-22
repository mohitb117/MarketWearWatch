package com.marketwear.shared.network

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.InstrumentCategory
import com.marketwear.shared.model.MarketInstrument
import com.marketwear.shared.model.MarketRegistry
import com.marketwear.shared.model.PortfolioRegistry
import com.marketwear.shared.model.TickerIntradayData
import com.marketwear.shared.platform.createPlatformHttpClient
import com.marketwear.shared.platform.currentTimeMillis
import com.marketwear.shared.platform.isUsMarketWindowActive
import com.marketwear.shared.ui.RefreshPolicy
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Shared KMP network client for querying Yahoo Finance v8 chart API across Android & iPhone.
 * Includes a strict 2-hour cache for the 37 portfolio holdings to preserve watch/phone battery.
 */
class SharedMarketClient(
    private val client: HttpClient = createPlatformHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val requestSemaphore = Semaphore(6)
    private val fetchAllMutex = Mutex()
    private val portfolioMutex = Mutex()

    private var cachedAllQuotes: List<IndexQuote> = emptyList()
    private var lastCoreFetchMillis: Long = 0L

    private var cachedPortfolioQuote: IndexQuote? = null
    private var cachedTickerIntradayMap: Map<String, TickerIntradayData> = emptyMap()
    private var lastPortfolioFetchMillis: Long = 0L

    companion object {
        /**
         * The 37 portfolio holdings are only refreshed once every 2 hours (120 minutes).
         */
        const val PORTFOLIO_REFRESH_INTERVAL_MS = 2 * 60 * 60 * 1000L

        /**
         * Core indices/sectors cache TTL: 5 minutes during US market hours, 6 hours off-hours.
         */
        const val ACTIVE_CORE_CACHE_MS = 5 * 60 * 1000L
        const val OFF_HOURS_CORE_CACHE_MS = 6 * 60 * 60 * 1000L
    }

    suspend fun fetchQuote(symbol: String): IndexQuote {
        val instrument = MarketRegistry.findBySymbol(symbol)
            ?: MarketInstrument(
                symbol = symbol,
                yahooTicker = symbol,
                name = symbol,
                category = if (MarketRegistry.isSector(symbol)) InstrumentCategory.SECTOR else InstrumentCategory.INDEX
            )
        return fetchQuote(instrument)
    }

    suspend fun fetchQuote(instrument: MarketInstrument): IndexQuote {
        val bodyText = fetchYahooChartJson(instrument.yahooTicker)
        return parseResponse(instrument, bodyText)
    }

    private suspend fun fetchYahooChartJson(yahooTicker: String): String {
        val encodedTicker = yahooTicker.replace("^", "%5E")
        val endpoint = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedTicker?interval=15m&range=1d"

        val response = client.get(endpoint) {
            header("User-Agent", "Mozilla/5.0 (MarketWear KMP)")
            header("Accept", "application/json")
        }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("HTTP error ${response.status.value}: ${response.status.description}")
        }

        return response.bodyAsText()
    }

    fun getCachedPortfolioTickerData(): Map<String, TickerIntradayData> = cachedTickerIntradayMap

    suspend fun seedPortfolioTickerCache(
        data: Map<String, TickerIntradayData>,
        timestampMillis: Long = currentTimeMillis(),
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ) = portfolioMutex.withLock {
        if (data.isEmpty()) return@withLock
        cachedTickerIntradayMap = data
        cachedPortfolioQuote = PortfolioRegistry.computePortfolioQuote(
            dataByYahooTicker = data,
            timestampMillis = timestampMillis,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        if (lastPortfolioFetchMillis == 0L) {
            lastPortfolioFetchMillis = timestampMillis
        }
    }

    fun recomputePortfolioQuote(
        disabledHoldingIds: Set<String>,
        customWeights: Map<String, Double> = emptyMap(),
        timestampMillis: Long = lastPortfolioFetchMillis.takeIf { it > 0L } ?: currentTimeMillis()
    ): IndexQuote? {
        val data = cachedTickerIntradayMap
        if (data.isEmpty()) return cachedPortfolioQuote
        val recomputed = PortfolioRegistry.computePortfolioQuote(
            dataByYahooTicker = data,
            timestampMillis = timestampMillis,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        cachedPortfolioQuote = recomputed
        return recomputed
    }

    suspend fun fetchAllQuotes(
        policy: RefreshPolicy = RefreshPolicy.CACHE_FIRST,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): List<IndexQuote> = fetchAllMutex.withLock {
        val now = currentTimeMillis()
        val coreTtl = if (isUsMarketWindowActive()) ACTIVE_CORE_CACHE_MS else OFF_HOURS_CORE_CACHE_MS
        if (policy == RefreshPolicy.CACHE_FIRST && cachedAllQuotes.isNotEmpty() && (now - lastCoreFetchMillis) < coreTtl) {
            val refreshedPortfolio = recomputePortfolioQuote(disabledHoldingIds, customWeights)
            return@withLock if (refreshedPortfolio != null) {
                listOf(refreshedPortfolio) + cachedAllQuotes.filter { it.symbol != PortfolioRegistry.PORTFOLIO_SYMBOL }
            } else {
                cachedAllQuotes
            }
        }

        val coreQuotes = coroutineScope {
            MarketRegistry.ALL_INSTRUMENTS.map { instrument ->
                async {
                    requestSemaphore.withPermit {
                        try {
                            fetchQuote(instrument)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (coreQuotes.isEmpty()) {
            return@withLock cachedAllQuotes
        }

        val portfolioPolicy = when (policy) {
            RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO -> RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO
            RefreshPolicy.CACHE_FIRST,
            RefreshPolicy.FORCE_CORE_QUOTES -> RefreshPolicy.CACHE_FIRST
        }
        val portfolioQuote = fetchOrReusePortfolioQuote(
            policy = portfolioPolicy,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        val combined = if (portfolioQuote != null) {
            listOf(portfolioQuote) + coreQuotes
        } else {
            coreQuotes
        }
        cachedAllQuotes = combined
        lastCoreFetchMillis = currentTimeMillis()
        combined
    }

    /**
     * Fetches the 37 portfolio positions at most once every 2 hours unless
     * [RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO] is requested; reuses cached ticker data otherwise
     * and recomputes dynamically for [disabledHoldingIds].
     */
    suspend fun fetchOrReusePortfolioQuote(
        policy: RefreshPolicy = RefreshPolicy.CACHE_FIRST,
        disabledHoldingIds: Set<String> = emptySet(),
        customWeights: Map<String, Double> = emptyMap()
    ): IndexQuote? = portfolioMutex.withLock {
        val now = currentTimeMillis()
        val existingMap = cachedTickerIntradayMap
        val allowPortfolioCache = policy != RefreshPolicy.FORCE_ALL_INCLUDING_PORTFOLIO
        if (allowPortfolioCache && existingMap.isNotEmpty() && (now - lastPortfolioFetchMillis) < PORTFOLIO_REFRESH_INTERVAL_MS) {
            val recomputed = PortfolioRegistry.computePortfolioQuote(
                dataByYahooTicker = existingMap,
                timestampMillis = lastPortfolioFetchMillis,
                disabledHoldingIds = disabledHoldingIds,
                customWeights = customWeights
            )
            cachedPortfolioQuote = recomputed
            return@withLock recomputed
        }

        val tickerDataPairs = coroutineScope {
            PortfolioRegistry.UNIQUE_YAHOO_TICKERS.map { yahooTicker ->
                async {
                    requestSemaphore.withPermit {
                        try {
                            fetchTickerIntradayData(yahooTicker)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull()
        }

        if (tickerDataPairs.isEmpty()) {
            return@withLock cachedPortfolioQuote
        }

        val dataMap = tickerDataPairs.associateBy { it.yahooTicker }
        cachedTickerIntradayMap = dataMap
        val computed = PortfolioRegistry.computePortfolioQuote(
            dataByYahooTicker = dataMap,
            timestampMillis = now,
            disabledHoldingIds = disabledHoldingIds,
            customWeights = customWeights
        )
        cachedPortfolioQuote = computed
        lastPortfolioFetchMillis = now
        computed
    }

    internal suspend fun fetchTickerIntradayData(yahooTicker: String): TickerIntradayData {
        val bodyText = fetchYahooChartJson(yahooTicker)
        return parseTickerIntradayData(yahooTicker, bodyText)
    }

    internal fun parseTickerIntradayData(yahooTicker: String, jsonString: String): TickerIntradayData {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val chart = root["chart"]?.jsonObject ?: throw IllegalArgumentException("Missing 'chart' in response")
        val resultArr = chart["result"]?.jsonArray ?: throw IllegalArgumentException("Missing 'result' in chart")
        if (resultArr.isEmpty()) {
            throw IllegalArgumentException("Empty result array in chart response")
        }

        val resultObj = resultArr[0].jsonObject
        val meta = resultObj["meta"]?.jsonObject ?: throw IllegalArgumentException("Missing 'meta'")
        val regularMarketPrice = meta["regularMarketPrice"]?.jsonPrimitive?.double
            ?: throw IllegalArgumentException("Missing 'regularMarketPrice'")
        val previousClose = meta["previousClose"]?.jsonPrimitive?.doubleOrNull
            ?: meta["chartPreviousClose"]?.jsonPrimitive?.doubleOrNull
            ?: regularMarketPrice

        val closes = extractIntradayCloses(resultObj)
        return TickerIntradayData(
            yahooTicker = yahooTicker,
            previousClose = previousClose,
            currentPrice = regularMarketPrice,
            intradayCloses = closes
        )
    }

    internal fun parseResponse(instrument: MarketInstrument, jsonString: String): IndexQuote {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val chart = root["chart"]?.jsonObject ?: throw IllegalArgumentException("Missing 'chart' in response")
        val resultArr = chart["result"]?.jsonArray ?: throw IllegalArgumentException("Missing 'result' in chart")

        if (resultArr.isEmpty()) {
            throw IllegalArgumentException("Empty result array in chart response")
        }

        val resultObj = resultArr[0].jsonObject
        val meta = resultObj["meta"]?.jsonObject
            ?: throw IllegalArgumentException("Missing 'meta' in chart result")

        val regularMarketPrice = meta["regularMarketPrice"]?.jsonPrimitive?.double
            ?: throw IllegalArgumentException("Missing 'regularMarketPrice'")

        val previousClose = meta["previousClose"]?.jsonPrimitive?.doubleOrNull
            ?: meta["chartPreviousClose"]?.jsonPrimitive?.doubleOrNull
            ?: regularMarketPrice

        val dayHigh = meta["regularMarketDayHigh"]?.jsonPrimitive?.doubleOrNull ?: (regularMarketPrice * 1.005)
        val dayLow = meta["regularMarketDayLow"]?.jsonPrimitive?.doubleOrNull ?: (regularMarketPrice * 0.995)

        val change = regularMarketPrice - previousClose
        val percentChange = if (previousClose > 0) (change / previousClose) * 100.0 else 0.0
        val sparkline = extractIntradayCloses(resultObj)

        return IndexQuote(
            symbol = instrument.symbol,
            name = instrument.name,
            price = regularMarketPrice,
            change = change,
            percentChange = percentChange,
            dayHigh = dayHigh,
            dayLow = dayLow,
            lastUpdated = currentTimeMillis(),
            category = instrument.category.name,
            sparkline = sparkline
        )
    }

    private fun extractIntradayCloses(resultObj: kotlinx.serialization.json.JsonObject): List<Double> {
        val quoteArr = resultObj["indicators"]
            ?.jsonObject?.get("quote")
            ?.jsonArray
        if (quoteArr.isNullOrEmpty()) return emptyList()
        val closeArr = quoteArr[0].jsonObject["close"]?.jsonArray ?: return emptyList()
        return closeArr.mapNotNull { it.jsonPrimitive.doubleOrNull }
    }
}
