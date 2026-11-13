package com.marketwear.shared.di

import com.marketwear.shared.network.SharedMarketClient
import com.marketwear.shared.platform.createPlatformHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Shared Koin DI module for all KMP targets (Android, Wear OS, iOS).
 */
val sharedModule = module {
    single<HttpClient> { createPlatformHttpClient() }
    single { SharedMarketClient(get()) }
}
