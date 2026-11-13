package com.marketwear.shared.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun isUsMarketWindowActive(): Boolean {
    val nyTime = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))
    val dayOfWeek = nyTime.get(Calendar.DAY_OF_WEEK)
    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
        return false
    }
    val minutesSinceMidnight = nyTime.get(Calendar.HOUR_OF_DAY) * 60 + nyTime.get(Calendar.MINUTE)
    // 8:30 AM ET (510) to 5:00 PM ET (1020)
    return minutesSinceMidnight in 510..1020
}

actual fun createPlatformHttpClient(): HttpClient {
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
