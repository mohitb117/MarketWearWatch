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

actual fun hasMarketOpenedSince(lastFetchMillis: Long, currentMillis: Long): Boolean {
    if (lastFetchMillis <= 0L) return true
    val tz = TimeZone.getTimeZone("America/New_York")
    val currentCal = Calendar.getInstance(tz).apply { timeInMillis = currentMillis }
    val dayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) return false

    val currentMinutes = currentCal.get(Calendar.HOUR_OF_DAY) * 60 + currentCal.get(Calendar.MINUTE)
    // Regular US equity markets open at 9:30 AM ET (570 minutes since midnight)
    if (currentMinutes < 570) return false

    val lastCal = Calendar.getInstance(tz).apply { timeInMillis = lastFetchMillis }
    val isSameDay = currentCal.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
        currentCal.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR)

    if (!isSameDay) return true // Last fetch was on a previous day!

    val lastMinutes = lastCal.get(Calendar.HOUR_OF_DAY) * 60 + lastCal.get(Calendar.MINUTE)
    return lastMinutes < 570 // Last fetch was before 9:30 AM today!
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
