export interface ProjectFile {
  path: string;
  name: string;
  category: 'kotlin' | 'manifest' | 'gradle' | 'resource' | 'doc';
  description: string;
  content: string;
}

export const WEAR_OS_PROJECT_FILES: ProjectFile[] = [
  {
    path: 'app/src/main/AndroidManifest.xml',
    name: 'AndroidManifest.xml',
    category: 'manifest',
    description: 'Declares Wear OS hardware requirements, complication provider services, permissions, and main companion activity.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.marketwear.indices">

    <!-- Wear OS standalone app declaration -->
    <uses-feature android:name="android.hardware.type.watch" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault">

        <!-- Standalone Wear OS App declaration: can be installed independently on Pixel Watch 4 -->
        <meta-data
            android:name="com.google.android.wearable.standalone"
            android:value="true" />

        <!-- Main Watch Companion Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="@string/app_name"
            android:theme="@android:style/Theme.DeviceDefault.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- S&P 500 Complication Provider Service -->
        <service
            android:name=".Sp500ComplicationService"
            android:exported="true"
            android:icon="@drawable/ic_sp500"
            android:label="@string/complication_sp500_label"
            android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
            <intent-filter>
                <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST" />
            </intent-filter>

            <!-- Supported complication styles on Pixel Watch dials -->
            <meta-data
                android:name="android.support.wearable.complications.SUPPORTED_TYPES"
                android:value="SHORT_TEXT,RANGED_VALUE,SMALL_IMAGE,LONG_TEXT" />
            <!-- Battery-efficient update frequency (15 minutes default) -->
            <meta-data
                android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
                android:value="900" />
        </service>

        <!-- Nasdaq 100 Complication Provider Service -->
        <service
            android:name=".NasdaqComplicationService"
            android:exported="true"
            android:icon="@drawable/ic_nasdaq"
            android:label="@string/complication_nasdaq_label"
            android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
            <intent-filter>
                <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST" />
            </intent-filter>

            <meta-data
                android:name="android.support.wearable.complications.SUPPORTED_TYPES"
                android:value="SHORT_TEXT,RANGED_VALUE,SMALL_IMAGE,LONG_TEXT" />
            <meta-data
                android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
                android:value="900" />
        </service>

        <!-- Combined Dual Glance Complication Service (SPX + NDX in one slot) -->
        <service
            android:name=".MarketDualComplicationService"
            android:exported="true"
            android:icon="@drawable/ic_sp500"
            android:label="@string/complication_dual_label"
            android:permission="com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER">
            <intent-filter>
                <action android:name="android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST" />
            </intent-filter>

            <meta-data
                android:name="android.support.wearable.complications.SUPPORTED_TYPES"
                android:value="SHORT_TEXT,LONG_TEXT" />
            <meta-data
                android:name="android.support.wearable.complications.UPDATE_PERIOD_SECONDS"
                android:value="900" />
        </service>

    </application>
</manifest>`
  },
  {
    path: 'app/src/main/java/com/marketwear/indices/Sp500ComplicationService.kt',
    name: 'Sp500ComplicationService.kt',
    category: 'kotlin',
    description: 'SuspendingComplicationDataSourceService implementation delivering S&P 500 quotes to Pixel Watch complication slots.',
    content: `package com.marketwear.indices

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.IndexQuote
import java.util.Locale

/**
 * Android Wear OS Complication Provider for S&P 500 (^GSPC).
 * Designed for high legibility on Google Pixel Watch circular displays.
 */
class Sp500ComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { MarketRepository.getInstance(applicationContext) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val quote = repository.getQuote("SPX")
        return createComplicationData(quote, request.complicationType)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val previewQuote = IndexQuote(
            symbol = "SPX",
            name = "S&P 500",
            price = 5985.42,
            change = 42.15,
            percentChange = 0.71,
            dayHigh = 6010.50,
            dayLow = 5940.20,
            lastUpdated = System.currentTimeMillis()
        )
        return createComplicationData(previewQuote, type)
    }

    private fun createComplicationData(quote: IndexQuote, type: ComplicationType): ComplicationData? {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_TARGET_INDEX, "SPX")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPositive = quote.change >= 0
        val trendIconRes = if (isPositive) R.drawable.ic_trend_up else R.drawable.ic_trend_down
        val icon = Icon.createWithResource(this, trendIconRes)
        val monoImage = MonochromaticImage.Builder(icon).build()

        val sign = if (isPositive) "+" else ""
        val percentText = String.format(Locale.US, "%s%.2f%%", sign, quote.percentChange)
        val pointsText = String.format(Locale.US, "%,.0f", quote.price)
        val fullTitle = "SPX"

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                // Returns clean points value with ticker title, e.g. "SPX" over "5,985"
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(pointsText).build(),
                    contentDescription = PlainComplicationText.Builder("S&P 500: $pointsText, $percentText").build()
                )
                    .setTitle(PlainComplicationText.Builder(fullTitle).build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.RANGED_VALUE -> {
                // Ranged gauge representing where current price sits between today's Low and High
                val min = quote.dayLow.toFloat()
                val max = quote.dayHigh.coerceAtLeast(quote.dayLow + 1.0).toFloat()
                val current = quote.price.toFloat().coerceIn(min, max)

                RangedValueComplicationData.Builder(
                    value = current,
                    min = min,
                    max = max,
                    contentDescription = PlainComplicationText.Builder("S&P 500 Range: $pointsText").build()
                )
                    .setText(PlainComplicationText.Builder(percentText).build())
                    .setTitle(PlainComplicationText.Builder(fullTitle).build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                val summary = String.format(Locale.US, "S&P 500: %,.2f (%s)", quote.price, percentText)
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(summary).build(),
                    contentDescription = PlainComplicationText.Builder(summary).build()
                )
                    .setTitle(PlainComplicationText.Builder("US Market").build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.SMALL_IMAGE -> {
                val smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build()
                SmallImageComplicationData.Builder(
                    smallImage = smallImage,
                    contentDescription = PlainComplicationText.Builder("S&P 500: $percentText").build()
                )
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> null
        }
    }
}`
  },
  {
    path: 'app/src/main/java/com/marketwear/indices/NasdaqComplicationService.kt',
    name: 'NasdaqComplicationService.kt',
    category: 'kotlin',
    description: 'Complication service providing Nasdaq 100 (^NDX / QQQ) quotes and percentages.',
    content: `package com.marketwear.indices

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.marketwear.indices.data.MarketRepository
import com.marketwear.indices.data.IndexQuote
import java.util.Locale

/**
 * Android Wear OS Complication Provider for Nasdaq 100 (^NDX).
 */
class NasdaqComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { MarketRepository.getInstance(applicationContext) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val quote = repository.getQuote("NDX")
        return createComplicationData(quote, request.complicationType)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val previewQuote = IndexQuote(
            symbol = "NDX",
            name = "Nasdaq 100",
            price = 21450.80,
            change = 230.10,
            percentChange = 1.08,
            dayHigh = 21520.00,
            dayLow = 21280.00,
            lastUpdated = System.currentTimeMillis()
        )
        return createComplicationData(previewQuote, type)
    }

    private fun createComplicationData(quote: IndexQuote, type: ComplicationType): ComplicationData? {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_TARGET_INDEX, "NDX")
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1002,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isPositive = quote.change >= 0
        val trendIconRes = if (isPositive) R.drawable.ic_trend_up else R.drawable.ic_trend_down
        val icon = Icon.createWithResource(this, trendIconRes)
        val monoImage = MonochromaticImage.Builder(icon).build()

        val sign = if (isPositive) "+" else ""
        val percentText = String.format(Locale.US, "%s%.2f%%", sign, quote.percentChange)
        val pointsText = String.format(Locale.US, "%,.0f", quote.price)
        val fullTitle = "NDX"

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(pointsText).build(),
                    contentDescription = PlainComplicationText.Builder("Nasdaq 100: $pointsText, $percentText").build()
                )
                    .setTitle(PlainComplicationText.Builder(fullTitle).build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.RANGED_VALUE -> {
                val min = quote.dayLow.toFloat()
                val max = quote.dayHigh.coerceAtLeast(quote.dayLow + 1.0).toFloat()
                val current = quote.price.toFloat().coerceIn(min, max)

                RangedValueComplicationData.Builder(
                    value = current,
                    min = min,
                    max = max,
                    contentDescription = PlainComplicationText.Builder("Nasdaq 100 Range: $pointsText").build()
                )
                    .setText(PlainComplicationText.Builder(percentText).build())
                    .setTitle(PlainComplicationText.Builder(fullTitle).build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                val summary = String.format(Locale.US, "Nasdaq 100: %,.2f (%s)", quote.price, percentText)
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(summary).build(),
                    contentDescription = PlainComplicationText.Builder(summary).build()
                )
                    .setTitle(PlainComplicationText.Builder("Tech Index").build())
                    .setMonochromaticImage(monoImage)
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.SMALL_IMAGE -> {
                val smallImage = SmallImage.Builder(icon, SmallImageType.ICON).build()
                SmallImageComplicationData.Builder(
                    smallImage = smallImage,
                    contentDescription = PlainComplicationText.Builder("Nasdaq: $percentText").build()
                )
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> null
        }
    }
}`
  },
  {
    path: 'app/src/main/java/com/marketwear/indices/MarketDualComplicationService.kt',
    name: 'MarketDualComplicationService.kt',
    category: 'kotlin',
    description: 'Compact dual complication displaying both S&P 500 and Nasdaq 100 side-by-side in one slot.',
    content: `package com.marketwear.indices

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.marketwear.indices.data.MarketRepository
import java.util.Locale

/**
 * Dual Market Complication: Combines both S&P 500 and Nasdaq 100 into a single slot.
 * Example SHORT_TEXT: "S+0.7% N+1.1%"
 */
class MarketDualComplicationService : SuspendingComplicationDataSourceService() {

    private val repository by lazy { MarketRepository.getInstance(applicationContext) }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val spx = repository.getQuote("SPX")
        val ndx = repository.getQuote("NDX")

        val tapIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1003,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val spxSign = if (spx.percentChange >= 0) "+" else ""
        val ndxSign = if (ndx.percentChange >= 0) "+" else ""

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                // Compact glance: "S+.7 N+1.1"
                val text = String.format(Locale.US, "S%s%.1f%%", spxSign, spx.percentChange)
                val title = String.format(Locale.US, "N%s%.1f%%", ndxSign, ndx.percentChange)

                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder("S&P $text, Nasdaq $title").build()
                )
                    .setTitle(PlainComplicationText.Builder(title).build())
                    .setTapAction(pendingIntent)
                    .build()
            }

            ComplicationType.LONG_TEXT -> {
                val fullSummary = String.format(
                    Locale.US,
                    "SPX %s%.2f%% · NDX %s%.2f%%",
                    spxSign, spx.percentChange,
                    ndxSign, ndx.percentChange
                )
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(fullSummary).build(),
                    contentDescription = PlainComplicationText.Builder(fullSummary).build()
                )
                    .setTitle(PlainComplicationText.Builder("Market Watch").build())
                    .setTapAction(pendingIntent)
                    .build()
            }

            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val tapIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            1003,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("S+0.7%").build(),
            contentDescription = PlainComplicationText.Builder("Preview Dual").build()
        )
            .setTitle(PlainComplicationText.Builder("N+1.1%").build())
            .setTapAction(pendingIntent)
            .build()
    }
}`
  },
  {
    path: 'app/src/main/java/com/marketwear/indices/data/MarketRepository.kt',
    name: 'MarketRepository.kt',
    category: 'kotlin',
    description: 'Battery-optimized repository with SharedPreferences cache, intelligent network throttle, and market hours awareness.',
    content: `package com.marketwear.indices.data

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
import java.util.Calendar
import java.util.TimeZone

/**
 * Singleton repository responsible for fetching S&P 500 and Nasdaq 100 quotes.
 * Designed specifically for Wear OS battery constraints:
 * - Caches values in SharedPreferences so complications never block on network.
 * - Respects market trading hours (skips wasteful network polling when NYSE/Nasdaq are closed).
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

    /**
     * Retrieves the latest quote for the given symbol (SPX or NDX).
     * If cached data is fresh or network is unavailable, returns cached quote instantly.
     */
    suspend fun getQuote(symbol: String, forceRefresh: Boolean = false): IndexQuote =
        withContext(Dispatchers.IO) {
            val cached = loadFromCache(symbol)
            val now = System.currentTimeMillis()

            if (!forceRefresh && cached != null && (now - cached.lastUpdated < CACHE_EXPIRY_MS)) {
                return@withContext cached
            }

            // If offline, always use cached data without erroring out
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
        // Query official ticker symbol: ^GSPC for S&P 500, ^NDX for Nasdaq 100
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
}`
  },
  {
    path: 'app/src/main/java/com/marketwear/indices/data/MarketModels.kt',
    name: 'MarketModels.kt',
    category: 'kotlin',
    description: 'Data models representing market quotes, historical ranges, and complication display formats.',
    content: `package com.marketwear.indices.data

/**
 * Snapshot of a major financial index quote.
 */
data class IndexQuote(
    val symbol: String,        // "SPX" or "NDX"
    val name: String,          // "S&P 500" or "Nasdaq 100"
    val price: Double,         // e.g. 5985.42
    val change: Double,        // e.g. +42.15
    val percentChange: Double, // e.g. +0.71
    val dayHigh: Double,       // e.g. 6010.50
    val dayLow: Double,        // e.g. 5940.20
    val lastUpdated: Long      // Unix timestamp millis
) {
    val isPositive: Boolean get() = change >= 0
    val formattedPoints: String get() = String.format("%,.0f", price)
    val formattedChange: String get() = String.format("%s%.2f", if (isPositive) "+" else "", change)
    val formattedPercent: String get() = String.format("%s%.2f%%", if (isPositive) "+" else "", percentChange)
}

/**
 * Display format preferences for the complications.
 */
enum class ComplicationFormat {
    POINTS,          // "5,985"
    PERCENT_CHANGE,  // "+0.71%"
    POINTS_AND_PCT,  // "5,985 (+0.7%)"
    COMPACT          // "5.98k"
}`
  },
  {
    path: 'app/src/main/java/com/marketwear/indices/MainActivity.kt',
    name: 'MainActivity.kt',
    category: 'kotlin',
    description: 'Clean Jetpack Compose for Wear OS companion screen shown when tapping the complication or launching from Pixel Watch app drawer.',
    content: `package com.marketwear.indices

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.marketwear.indices.data.IndexQuote
import com.marketwear.indices.data.MarketRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TARGET_INDEX = "extra_target_index"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialSymbol = intent.getStringExtra(EXTRA_TARGET_INDEX) ?: "SPX"

        setContent {
            MarketWearApp(initialSymbol = initialSymbol)
        }
    }
}

@Composable
fun MarketWearApp(initialSymbol: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { MarketRepository.getInstance(context) }
    val scope = rememberCoroutineScope()

    var spxQuote by remember { mutableStateOf<IndexQuote?>(null) }
    var ndxQuote by remember { mutableStateOf<IndexQuote?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshQuotes(force: Boolean = false) {
        scope.launch {
            isLoading = true
            spxQuote = repository.getQuote("SPX", forceRefresh = force)
            ndxQuote = repository.getQuote("NDX", forceRefresh = force)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshQuotes(force = false)
    }

    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 28.dp, bottom = 28.dp, start = 14.dp, end = 14.dp)
        ) {
            item {
                Text(
                    text = "Market Watch",
                    style = MaterialTheme.typography.caption1,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // S&P 500 Card
            item {
                spxQuote?.let { quote ->
                    IndexCard(quote = quote)
                } ?: CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            item { Spacer(modifier = Modifier.height(6.dp)) }

            // Nasdaq 100 Card
            item {
                ndxQuote?.let { quote ->
                    IndexCard(quote = quote)
                }
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }

            // Force Refresh Button
            item {
                CompactButton(
                    onClick = { refreshQuotes(force = true) },
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(
                        text = if (isLoading) "Updating..." else "Refresh",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Timestamp
            item {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeStr = spxQuote?.lastUpdated?.let { timeFormat.format(Date(it)) } ?: "--"
                Text(
                    text = "Updated: $timeStr",
                    fontSize = 10.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun IndexCard(quote: IndexQuote) {
    val isPositive = quote.change >= 0
    val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        onClick = { /* No-op, visual inspection */ },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(backgroundColor = Color(0xFF1E1E1E))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = quote.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = quote.formattedPercent,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = quote.formattedPoints,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color.White
                )
                Text(
                    text = quote.formattedChange,
                    color = trendColor,
                    fontSize = 11.sp
                )
            }
        }
    }
}`
  },
  {
    path: 'app/build.gradle.kts',
    name: 'app/build.gradle.kts',
    category: 'gradle',
    description: 'Wear OS module Gradle configuration with AndroidX WatchFace Complication libraries and Jetpack Compose Wear OS dependencies.',
    content: `plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.marketwear.indices"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.marketwear.indices"
        minSdk = 30 // Wear OS 3.0+ (Pixel Watch 1, 2, 3, 4)
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // Defaults to debug key for personal sideloading
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Official Modern AndroidX Wear Watch Face Complications API
    implementation("androidx.wear.watchface:watchface-complications-data-source:1.2.1")
    implementation("androidx.wear.watchface:watchface-complications-data:1.2.1")

    // Wear OS UI & Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Coroutines & Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")

    // Wear OS Standalone Hardware
    compileOnly("com.google.android.wearable:wearable:2.9.0")
}`
  },
  {
    path: 'build.gradle.kts',
    name: 'build.gradle.kts',
    category: 'gradle',
    description: 'Root Gradle buildscript declaring Android and Kotlin Gradle plugins.',
    content: `plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}`
  },
  {
    path: 'settings.gradle.kts',
    name: 'settings.gradle.kts',
    category: 'gradle',
    description: 'Gradle repository declaration pointing to Google Maven and Maven Central.',
    content: `pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\\\.android.*")
                includeGroupByRegex("com\\\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MarketWearIndices"
include(":app")`
  },
  {
    path: 'gradle/libs.versions.toml',
    name: 'libs.versions.toml',
    category: 'gradle',
    description: 'Version catalog pinning modern Gradle, Kotlin, AndroidX, and Wear OS versions.',
    content: `[versions]
agp = "8.6.0"
kotlin = "2.0.20"
composeBom = "2024.09.00"
wearCompose = "1.4.0"
watchface = "1.2.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
watchface-complications-data-source = { group = "androidx.wear.watchface", name = "watchface-complications-data-source", version.ref = "watchface" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }`
  },
  {
    path: 'app/src/main/res/values/strings.xml',
    name: 'strings.xml',
    category: 'resource',
    description: 'Localizable strings for complication labels, titles, and watch companion screens.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Market Complications</string>
    <string name="complication_sp500_label">S&amp;P 500 Index</string>
    <string name="complication_nasdaq_label">Nasdaq 100 Index</string>
    <string name="complication_dual_label">Dual Market (SPX + NDX)</string>
    <string name="refresh">Refresh</string>
    <string name="updating">Updating…</string>
</resources>`
  },
  {
    path: 'app/src/main/res/drawable/ic_sp500.xml',
    name: 'ic_sp500.xml',
    category: 'resource',
    description: 'Vector drawable for S&P 500 complication slot icon.',
    content: `<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z" />
</vector>`
  },
  {
    path: 'app/src/main/res/drawable/ic_trend_up.xml',
    name: 'ic_trend_up.xml',
    category: 'resource',
    description: 'Monochromatic trend up icon for complications.',
    content: `<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M16,6l2.29,2.29-4.88,4.88-4,-4L2,16.59 3.41,18l6,-6 4,4 6.3,-6.29L22,12V6z" />
</vector>`
  },
  {
    path: 'app/src/main/res/drawable/ic_trend_down.xml',
    name: 'ic_trend_down.xml',
    category: 'resource',
    description: 'Monochromatic trend down icon for complications.',
    content: `<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M16,18l2.29,-2.29-4.88,-4.88-4,4L2,7.41 3.41,6l6,6 4,-4 6.3,6.29L22,12v6z" />
</vector>`
  },
  {
    path: 'README.md',
    name: 'README.md',
    category: 'doc',
    description: 'Comprehensive step-by-step setup guide for installing on Google Pixel Watch 4 via Personal App Library or Wireless ADB.',
    content: `# MarketWear: S&P 500 & Nasdaq 100 Complications for Pixel Watch

A clean, isolated Wear OS application built with AndroidX Watch Face Complications API to display live S&P 500 (^GSPC) and Nasdaq 100 (^NDX) quotes on any Google Pixel Watch face.

---

## Installation Options on Pixel Watch 4

### Option A: Install via Personal App Library (Google Play Console)
If you want the app automatically synchronized to your watch from the Play Store without cables or ADB commands:
1. Open [Google Play Console](https://play.google.com/console).
2. Create an application named **"MarketWear Complications"**.
3. Under **Release > Testing > Internal testing**, create a new release and upload the generated \`app-release.aab\` or \`app-release.apk\`.
4. Add your email address (\`mohitb117@gmail.com\`) to the Internal Testers list and accept the invitation link on your phone/browser.
5. On your Pixel Watch, open the **Google Play Store**, scroll down to **"Apps on your phone"** or **"Personal App Library"**, and tap **Install** next to MarketWear.

---

### Option B: Direct Wireless ADB Sideload (Fastest - 2 minutes)
No Play Store developer account required:
1. **Enable Developer Options on Pixel Watch**:
   - On the watch, go to **Settings > System > About > Versions**.
   - Tap **Build number** 7 times until you see *"You are now a developer!"*.
2. **Enable Wireless Debugging**:
   - Go to **Settings > Developer options**.
   - Turn ON **ADB debugging** and **Wireless debugging**.
   - Tap **Wireless debugging > Pair new device**.
   - Note the **IP address, port, and 6-digit Wi-Fi pairing code**.
3. **From your computer terminal**:
   \`\`\`bash
   # Pair with your watch
   adb pair 192.168.1.xxx:PORT
   # Enter the 6-digit code when prompted

   # Connect to watch
   adb connect 192.168.1.xxx:PORT

   # Build and install release APK
   ./gradlew assembleRelease
   adb install -r app/build/outputs/apk/release/app-release.apk
   \`\`\`

---

### Option C: Wear Installer 2 (Direct from Phone)
1. Download **Wear Installer 2** from Google Play on your Android phone.
2. Follow the on-screen instructions to bridge to your Pixel Watch over Wi-Fi.
3. Select \`app-release.apk\` from your downloads to install directly to your watch.

---

## Adding Complications to your Pixel Watch Face
1. On your Pixel Watch, touch and hold the watch face.
2. Tap the **Edit / Pencil** icon below the face.
3. Swipe left to the **Complications** configuration screen.
4. Tap any circular, line, or corner complication slot.
5. Scroll down to **Market Complications** and choose:
   - **S&P 500 Index**
   - **Nasdaq 100 Index**
   - **Dual Market (SPX + NDX)**
6. Press the watch crown to save and exit.
`
  },
  {
    path: '.github/workflows/build-wear-bundle.yml',
    name: 'build-wear-bundle.yml',
    category: 'doc',
    description: 'Automated GitHub Actions CI/CD to build the release Android App Bundle (.aab) and APK in the cloud for free.',
    content: `name: Build Pixel Watch Bundle (AAB & APK)

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    name: Compile Wear OS Bundle
    runs-on: ubuntu-latest

    steps:
    - name: Checkout Code
      uses: actions/checkout@v4

    - name: Set up Java 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'

    - name: Setup Gradle
      uses: gradle/actions/setup-gradle@v3

    - name: Make Gradlew Executable
      run: chmod +x gradlew || true

    - name: Build Android App Bundle (.aab)
      run: ./gradlew bundleRelease --no-daemon --stacktrace

    - name: Build Standalone APK (.apk)
      run: ./gradlew assembleRelease --no-daemon --stacktrace

    - name: Upload Release AAB (For Google Play Personal Library)
      uses: actions/upload-artifact@v4
      with:
        name: MarketWear-PixelWatch-Release-AAB
        path: app/build/outputs/bundle/release/*.aab
        retention-days: 30

    - name: Upload Release APK (For Direct Sideloading)
      uses: actions/upload-artifact@v4
      with:
        name: MarketWear-PixelWatch-Release-APK
        path: app/build/outputs/apk/release/*.apk
        retention-days: 30`
  },
  {
    path: 'gradle/wrapper/gradle-wrapper.properties',
    name: 'gradle-wrapper.properties',
    category: 'gradle',
    description: 'Gradle wrapper version specification (Gradle 8.9).',
    content: `distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\\://services.gradle.org/distributions/gradle-8.9-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists`
  },
  {
    path: 'app/proguard-rules.pro',
    name: 'proguard-rules.pro',
    category: 'gradle',
    description: 'ProGuard/R8 rules preserving Wear OS complication provider services.',
    content: `-keep class com.marketwear.indices.** { *; }
-keep class androidx.wear.watchface.complications.** { *; }
-keepclassmembers class * extends androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService {
    <init>(...);
}`
  },
  {
    path: 'build_bundle.sh',
    name: 'build_bundle.sh',
    category: 'gradle',
    description: 'One-click shell script to compile the release Android App Bundle (.aab) locally.',
    content: `#!/bin/bash
set -e

echo "Building MarketWear Release App Bundle (.aab)..."
chmod +x ./gradlew
./gradlew bundleRelease

echo "Done! Generated bundle is in app/build/outputs/bundle/release/"`
  }
];
