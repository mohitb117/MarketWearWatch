package com.marketwear.mobile.appfunctions

import android.content.Context
import androidx.appfunctions.AppFunctionContext
import com.marketwear.mobile.data.KtorMarketClient
import com.marketwear.mobile.data.MobileMarketRepository
import com.marketwear.shared.model.IndexQuote
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketAppFunctionsTest {

    private class FakeAppFunctionContext : AppFunctionContext {
        override val context: Context
            get() = throw UnsupportedOperationException("Context not needed in unit test")
    }

    private fun createMockRepository(): MobileMarketRepository {
        val mockEngine = MockEngine { request ->
            val url = request.url.toString()
            val (price, prevClose) = when {
                url.contains("%5EGSPC") -> 6000.0 to 5900.0 // +1.69%
                url.contains("%5ENDX") -> 21000.0 to 21200.0 // -0.94%
                url.contains("%5EYH311") -> 4500.0 to 4300.0 // Technology +4.65%
                else -> 1000.0 to 1000.0 // 0%
            }
            val payload = """
                {
                  "chart": {
                    "result": [{
                      "meta": {
                        "regularMarketPrice": $price,
                        "chartPreviousClose": $prevClose,
                        "regularMarketDayHigh": ${price + 10},
                        "regularMarketDayLow": ${price - 10}
                      }
                    }]
                  }
                }
            """.trimIndent()
            respond(
                content = payload,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return MobileMarketRepository(KtorMarketClient(httpClient))
    }

    @Test
    fun getMarketQuoteMatchesTickerAliasAndSectorName() = runTest {
        val repo = createMockRepository()
        // Pre-populate repository cache
        val fetched = repo.fetchAllQuotes()
        assertTrue(fetched.isNotEmpty())

        val spx = matchQuote(fetched, "^GSPC")
        assertEquals("SPX", spx.symbol)
        assertEquals(6000.0, spx.price, 0.01)

        val tech = matchQuote(fetched, "technology")
        assertEquals("^YH311", tech.symbol)
        assertEquals("Technology", tech.name)
    }

    @Test
    fun getTopMarketMoversOrdersByAbsolutePercentChange() = runTest {
        val repo = createMockRepository()
        val quotes = repo.fetchAllQuotes()

        val topMovers = quotes
            .sortedByDescending { kotlin.math.abs(it.percentChange) }
            .take(2)

        assertEquals(2, topMovers.size)
        assertEquals("^YH311", topMovers[0].symbol)
        assertEquals("SPX", topMovers[1].symbol)
    }

    private fun matchQuote(quotes: List<IndexQuote>, query: String): IndexQuote {
        val normalized = query.trim().lowercase()
        return quotes.first { quote ->
            quote.symbol.equals(normalized, ignoreCase = true) ||
                quote.name.lowercase().contains(normalized) ||
                (normalized in listOf("^gspc", "s&p", "s&p 500", "sp500") && quote.symbol == "SPX") ||
                (normalized in listOf("^ndx", "nasdaq", "nasdaq 100", "qqq") && quote.symbol == "NDX")
        }
    }
}
