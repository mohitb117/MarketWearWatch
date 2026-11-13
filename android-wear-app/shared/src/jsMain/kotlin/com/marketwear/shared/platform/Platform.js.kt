package com.marketwear.shared.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.js.Date

actual fun currentTimeMillis(): Long = Date.now().toLong()

actual fun isUsMarketWindowActive(): Boolean {
    // Check US/Eastern market hours in UTC (roughly 13:30 - 22:00 UTC Mon-Fri)
    val now = Date()
    val utcDay = now.getUTCDay()
    if (utcDay == 0 || utcDay == 6) return false
    val minutesUtc = now.getUTCHours() * 60 + now.getUTCMinutes()
    return minutesUtc in (13 * 60 + 30)..(22 * 60)
}

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Js) {
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
