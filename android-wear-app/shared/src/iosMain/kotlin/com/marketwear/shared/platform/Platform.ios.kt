package com.marketwear.shared.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.timeZoneWithName

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun isUsMarketWindowActive(): Boolean {
    val nyZone = NSTimeZone.timeZoneWithName("America/New_York") ?: return true
    val calendar = NSCalendar.currentCalendar.apply { timeZone = nyZone }
    val components = calendar.components(
        NSCalendarUnitWeekday or NSCalendarUnitHour or NSCalendarUnitMinute,
        fromDate = NSDate()
    )
    // NSCalendar weekday: 1 = Sunday, 7 = Saturday
    val weekday = components.weekday
    if (weekday == 1L || weekday == 7L) return false
    val minutesSinceMidnight = components.hour * 60L + components.minute
    return minutesSinceMidnight in 510L..1020L
}

actual fun createPlatformHttpClient(): HttpClient {
    return HttpClient(Darwin) {
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
