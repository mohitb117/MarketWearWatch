package com.marketwear.indices.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.marketwear.indices.data.db.IndexQuoteEntity
import com.marketwear.indices.data.db.MarketDatabase
import com.marketwear.indices.sync.MarketSyncWorker
import com.marketwear.shared.contracts.WearConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Repository responsible for managing market indices data using Room as the single source of truth.
 * Supports offloading network sync to paired phone with standalone fallback.
 */
class MarketRepository private constructor(private val context: Context) {

    private val db = MarketDatabase.getInstance(context)
    private val dao = db.indexQuoteDao()
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    init {
        // Automatically schedule 15-minute battery-conscious background WorkManager sync
        MarketSyncWorker.schedulePeriodicSync(context)
    }

    val quotesFlow: Flow<List<IndexQuote>> = dao.getAllQuotesFlow().map { list ->
        if (list.isEmpty()) {
            listOf(getFallbackQuote("SPX"), getFallbackQuote("NDX"))
        } else {
            list.map { it.toDomainModel() }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MarketRepository? = null

        fun getInstance(context: Context): MarketRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MarketRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Complication-safe quote retrieval: strictly offline Room read.
     */
    suspend fun getQuote(symbol: String): IndexQuote = withContext(Dispatchers.IO) {
        dao.getQuote(symbol)?.toDomainModel() ?: getFallbackQuote(symbol)
    }

    /**
     * Smart refresh:
     * If phone is available, request refresh via Wearable Data Layer.
     * If phone is unavailable, fall back to direct watch network fetch.
     */
    suspend fun refreshWithPhoneFallback(): Unit = withContext(Dispatchers.IO) {
        val phoneNode = getConnectedPhoneNode()
        if (phoneNode != null) {
            try {
                messageClient.sendMessage(
                    phoneNode.id,
                    WearConstants.PATH_REQUEST_REFRESH,
                    ByteArray(0)
                ).await()
                return@withContext
            } catch (_: Exception) {
                // If message failed, fall through to direct fetch
            }
        }

        // Fallback: direct online fetch on watch
        refreshAllOnline()
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

    suspend fun refreshAllOnline(): List<IndexQuote> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext dao.getAllQuotes().map { it.toDomainModel() }
        }

        val symbols = listOf("SPX", "NDX")
        val results = mutableListOf<IndexQuoteEntity>()

        for (sym in symbols) {
            try {
                val quote = fetchOnlineQuote(sym)
                results.add(IndexQuoteEntity.fromDomainModel(quote))
            } catch (_: Exception) {
                // Keep existing cached data on failure
            }
        }

        if (results.isNotEmpty()) {
            dao.insertOrUpdate(results)
            MarketSyncWorker.triggerComplicationUpdates(context)
        }

        dao.getAllQuotes().map { it.toDomainModel() }
    }

    private fun fetchOnlineQuote(symbol: String): IndexQuote {
        val yahooTicker = if (symbol == "SPX") "%5EGSPC" else "%5EIXIC"
        val endpoint = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooTicker?interval=1d&range=1d"

        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (WearOS; Pixel Watch 4)")
        conn.setRequestProperty("Accept", "application/json")

        if (conn.responseCode != 200) {
            throw RuntimeException("HTTP error ${conn.responseCode}")
        }

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        val response = reader.use { it.readText() }
        conn.disconnect()

        val json = JSONObject(response)
        val result = json.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
        val meta = result.getJSONObject("meta")

        val regularMarketPrice = meta.getDouble("regularMarketPrice")
        val previousClose = meta.optDouble("previousClose", meta.optDouble("chartPreviousClose", regularMarketPrice))
        val dayHigh = meta.optDouble("regularMarketDayHigh", regularMarketPrice * 1.005)
        val dayLow = meta.optDouble("regularMarketDayLow", regularMarketPrice * 0.995)

        val change = regularMarketPrice - previousClose
        val percentChange = if (previousClose > 0) (change / previousClose) * 100.0 else 0.0

        val name = if (symbol == "SPX") "S&P 500" else "Nasdaq 100"

        return IndexQuote(
            symbol = symbol,
            name = name,
            price = regularMarketPrice,
            change = change,
            percentChange = percentChange,
            dayHigh = dayHigh,
            dayLow = dayLow,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun getFallbackQuote(symbol: String): IndexQuote {
        return if (symbol == "SPX") {
            IndexQuote("SPX", "S&P 500", 5985.40, 42.15, 0.71, 6010.50, 5940.20, System.currentTimeMillis())
        } else {
            IndexQuote("NDX", "Nasdaq 100", 21450.80, 230.10, 1.08, 21520.00, 21280.00, System.currentTimeMillis())
        }
    }
}
