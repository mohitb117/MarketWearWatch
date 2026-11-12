package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.InstrumentCategory
import com.marketwear.shared.model.MarketInstrument
import com.marketwear.shared.model.MarketRegistry
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KtorMarketClient(
    private val client: HttpClient = createDefaultHttpClient()
) {

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
        val encodedTicker = instrument.yahooTicker.replace("^", "%5E")
        val endpoint = "https://query1.finance.yahoo.com/v8/finance/chart/$encodedTicker?interval=1d&range=1d"

        val response = client.get(endpoint) {
            header("User-Agent", "Mozilla/5.0 (Android; MarketWear Mobile)")
            header("Accept", "application/json")
        }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("HTTP error ${response.status.value}: ${response.status.description}")
        }

        val bodyText = response.bodyAsText()
        return parseResponse(instrument, bodyText)
    }

    suspend fun fetchAllQuotes(): List<IndexQuote> = coroutineScope {
        MarketRegistry.ALL_INSTRUMENTS.map { instrument ->
            async {
                try {
                    fetchQuote(instrument)
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun parseResponse(instrument: MarketInstrument, jsonString: String): IndexQuote {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(jsonString).jsonObject
        val chart = root["chart"]?.jsonObject ?: throw IllegalArgumentException("Missing 'chart' in response")
        val resultArr = chart["result"]?.jsonArray ?: throw IllegalArgumentException("Missing 'result' in chart")

        if (resultArr.isEmpty()) {
            throw IllegalArgumentException("Empty result array in chart response")
        }

        val meta = resultArr[0].jsonObject["meta"]?.jsonObject
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

        return IndexQuote(
            symbol = instrument.symbol,
            name = instrument.name,
            price = regularMarketPrice,
            change = change,
            percentChange = percentChange,
            dayHigh = dayHigh,
            dayLow = dayLow,
            lastUpdated = System.currentTimeMillis(),
            category = if (instrument.isSector) "SECTOR" else "INDEX"
        )
    }

    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(Android) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        prettyPrint = false
                        isLenient = true
                    })
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 6000
                    connectTimeoutMillis = 6000
                    socketTimeoutMillis = 6000
                }
            }
        }
    }
}
