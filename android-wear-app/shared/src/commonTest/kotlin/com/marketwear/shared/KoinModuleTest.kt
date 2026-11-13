package com.marketwear.shared

import com.marketwear.shared.di.SharedDi
import com.marketwear.shared.di.sharedModule
import com.marketwear.shared.network.SharedMarketClient
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class KoinModuleTest : KoinTest {

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun sharedModuleResolvesHttpClientAndMarketClientSingletons() {
        startKoin {
            modules(sharedModule)
        }

        val httpClient1 = get<HttpClient>()
        val httpClient2 = get<HttpClient>()
        assertNotNull(httpClient1)
        assertSame(httpClient1, httpClient2)

        val marketClient1 = get<SharedMarketClient>()
        val marketClient2 = get<SharedMarketClient>()
        assertNotNull(marketClient1)
        assertSame(marketClient1, marketClient2)
        assertSame(marketClient1, SharedDi.marketClient)
    }
}
