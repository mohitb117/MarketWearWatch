package com.marketwear.mobile

import com.marketwear.mobile.data.KtorMarketClient
import com.marketwear.mobile.data.MobileMarketRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Test

class MobileMarketRepositoryTest {

    @Test
    fun testNetworkFailureThrowsAndDoesNotReturnFakeQuotes() {
        val mockEngine = MockEngine { _ ->
            respond(
                content = "Server Error",
                status = HttpStatusCode.ServiceUnavailable
            )
        }
        val mockClient = KtorMarketClient(HttpClient(mockEngine))
        val repository = MobileMarketRepository(mockClient)

        // Must throw instead of silently returning synthetic quotes
        assertThrows(Exception::class.java) {
            runBlocking { repository.fetchAllQuotes() }
        }
    }
}
