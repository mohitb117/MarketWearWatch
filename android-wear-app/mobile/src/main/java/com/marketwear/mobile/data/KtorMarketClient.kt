package com.marketwear.mobile.data

import com.marketwear.shared.model.IndexQuote
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KtorMarketClient(
    private val client: HttpClient = createDefaultHttpClient()
) {
    suspend fun fetchQuote(symbol: String): IndexQuote {
        val yahooTicker = if (symbol == "SPX") "%5EGSPC" else "%5EIXIC"
        val endpoint = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooTicker?interval=1d&range=1d"

        val response = client.get(endpoint) {
            header("User-Agent", "Mozilla/5.0 (Android; MarketWear Mobile)")
            header("Accept", "application/json")
        }

        if (response.status != HttpStatusCode.OK) {
            throw RuntimeException("HTTP error ${response.status.value}: ${response.status.description}")
        }

        val bodyText = response.bodyAsText()
        return parseResponse(symbol, bodyText)
    }

    private fun parseResponse(symbol: String, jsonString: String): IndexQuote {
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
