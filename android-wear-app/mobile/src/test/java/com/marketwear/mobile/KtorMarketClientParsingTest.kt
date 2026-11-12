package com.marketwear.mobile

import com.marketwear.mobile.data.KtorMarketClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KtorMarketClientParsingTest {

    private val sampleValidJsonResponse = """
        {
          "chart": {
            "result": [
              {
                "meta": {
                  "symbol": "^GSPC",
                  "regularMarketPrice": 5985.42,
                  "previousClose": 5943.27,
                  "regularMarketDayHigh": 6010.50,
                  "regularMarketDayLow": 5940.20
                }
              }
            ],
            "error": null
          }
        }
    """.trimIndent()

    @Test
    fun testParseValidQuote() {
        val mockEngine = MockEngine { _ ->
            respond(
                content = sampleValidJsonResponse,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testHttpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = KtorMarketClient(testHttpClient)
        val quote = runBlocking { client.fetchQuote("SPX") }

        assertEquals("SPX", quote.symbol)
        assertEquals("S&P 500", quote.name)
        assertEquals("INDEX", quote.category)
        assertEquals(5985.42, quote.price, 0.001)
        assertEquals(42.15, quote.change, 0.01)
        assertEquals(0.71, quote.percentChange, 0.01)
        assertEquals(6010.50, quote.dayHigh, 0.001)
        assertEquals(5940.20, quote.dayLow, 0.001)
    }

    @Test
    fun testParseSectorQuote() {
        val sampleSectorJson = """
            {
              "chart": {
                "result": [
                  {
                    "meta": {
                      "symbol": "^YH101",
                      "regularMarketPrice": 2220463.5,
                      "previousClose": 2186163.5,
                      "regularMarketDayHigh": 2250000.0,
                      "regularMarketDayLow": 2190000.0
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val mockEngine = MockEngine { _ ->
            respond(
                content = sampleSectorJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val testHttpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val client = KtorMarketClient(testHttpClient)
        val quote = runBlocking { client.fetchQuote("^YH101") }

        assertEquals("^YH101", quote.symbol)
        assertEquals("Basic Materials", quote.name)
        assertEquals("SECTOR", quote.category)
        assertEquals(2220463.5, quote.price, 0.001)
        assertEquals(34300.0, quote.change, 0.01)
        assertEquals(1.57, quote.percentChange, 0.01)
    }

    @Test
    fun testHttpErrorHandling() {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Server Error",
                status = HttpStatusCode.InternalServerError
            )
        }

        val testHttpClient = HttpClient(mockEngine)
        val client = KtorMarketClient(testHttpClient)

        assertThrows(RuntimeException::class.java) {
            runBlocking { client.fetchQuote("SPX") }
        }
    }
}
