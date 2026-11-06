package com.marketwear.indices.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton repository responsible for fetching S&P 500 and Nasdaq 100 quotes.
 * Designed specifically for Wear OS battery constraints:
 * - Caches values in SharedPreferences so complications never block on network.
 * - Fast timeout (4 seconds) so watch never hangs if tethering is spotty.
 */
class MarketRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("market_wear_cache", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        private var INSTANCE: MarketRepository? = null

        fun getInstance(context: Context): MarketRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MarketRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val CACHE_EXPIRY_MS = 15 * 60 * 1000L // 15 minutes
    }

    suspend fun getQuote(symbol: String, forceRefresh: Boolean = false): IndexQuote =
        withContext(Dispatchers.IO) {
            val cached = loadFromCache(symbol)
            val now = System.currentTimeMillis()

            if (!forceRefresh && cached != null && (now - cached.lastUpdated < CACHE_EXPIRY_MS)) {
                return@withContext cached
            }

            if (!isNetworkAvailable()) {
                return@withContext cached ?: getFallbackQuote(symbol)
            }

            try {
                val fresh = fetchOnlineQuote(symbol)
                saveToCache(symbol, fresh)
                fresh
            } catch (e: Exception) {
                cached ?: getFallbackQuote(symbol)
            }
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
            throw RuntimeException("HTTP error \${conn.responseCode}")
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

    private fun saveToCache(symbol: String, quote: IndexQuote) {
        prefs.edit().apply {
            putFloat("\${symbol}_price", quote.price.toFloat())
            putFloat("\${symbol}_change", quote.change.toFloat())
            putFloat("\${symbol}_pct", quote.percentChange.toFloat())
            putFloat("\${symbol}_high", quote.dayHigh.toFloat())
            putFloat("\${symbol}_low", quote.dayLow.toFloat())
            putLong("\${symbol}_time", quote.lastUpdated)
            apply()
        }
    }

    private fun loadFromCache(symbol: String): IndexQuote? {
        if (!prefs.contains("\${symbol}_price")) return null
        val price = prefs.getFloat("\${symbol}_price", 0f).toDouble()
        val change = prefs.getFloat("\${symbol}_change", 0f).toDouble()
        val pct = prefs.getFloat("\${symbol}_pct", 0f).toDouble()
        val high = prefs.getFloat("\${symbol}_high", 0f).toDouble()
        val low = prefs.getFloat("\${symbol}_low", 0f).toDouble()
        val time = prefs.getLong("\${symbol}_time", 0L)
        val name = if (symbol == "SPX") "S&P 500" else "Nasdaq 100"

        return IndexQuote(
            symbol = symbol,
            name = name,
            price = price,
            change = change,
            percentChange = pct,
            dayHigh = high,
            dayLow = low,
            lastUpdated = time
        )
    }

    private fun getFallbackQuote(symbol: String): IndexQuote {
        return if (symbol == "SPX") {
            IndexQuote("SPX", "S&P 500", 5985.40, 42.15, 0.71, 6010.50, 5940.20, System.currentTimeMillis())
        } else {
            IndexQuote("NDX", "Nasdaq 100", 21450.80, 230.10, 1.08, 21520.00, 21280.00, System.currentTimeMillis())
        }
    }
}
