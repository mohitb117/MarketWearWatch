package com.marketwear.shared

import com.marketwear.shared.contracts.WearConstants
import com.marketwear.shared.model.IndexQuote
import com.marketwear.shared.model.MarketRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IndexQuoteSerializationTest {

    @Test
    fun testSerializationRoundTrip() {
        val original = IndexQuote(
            symbol = "SPX",
            name = "S&P 500",
            price = 5985.42,
            change = 42.15,
            percentChange = 0.71,
            dayHigh = 6010.50,
            dayLow = 5940.20,
            lastUpdated = 1700000000000L
        )

        val json = Json.encodeToString(original)
        assertTrue(json.contains("\"symbol\":\"SPX\""))

        val deserialized = Json.decodeFromString<IndexQuote>(json)
        assertEquals(original, deserialized)
        assertEquals("5,985", deserialized.formattedPoints)
        assertEquals("+42.15", deserialized.formattedChange)
        assertEquals("+0.71%", deserialized.formattedPercent)
    }

    @Test
    fun testCompactFormatting() {
        val sectorQuote = IndexQuote(
            symbol = "^YH101",
            name = "Basic Materials",
            price = 2220463.0,
            change = 34300.0,
            percentChange = 1.57,
            dayHigh = 2250000.0,
            dayLow = 2190000.0,
            lastUpdated = 1700000000000L
        )
        assertEquals("2.22M", sectorQuote.formattedCompactPoints)
        assertEquals("+34.3k", sectorQuote.formattedCompactChange)
        assertEquals(13, MarketRegistry.ALL_INSTRUMENTS.size)
    }

    @Test
    fun testWearConstantsContracts() {
        assertEquals("/market/quotes", WearConstants.PATH_MARKET_QUOTES)
        assertEquals("/market/request_refresh", WearConstants.PATH_REQUEST_REFRESH)
        assertEquals("marketwear_watch_app", WearConstants.CAPABILITY_WATCH)
        assertEquals("marketwear_phone_app", WearConstants.CAPABILITY_PHONE)
        assertNotNull(WearConstants.KEY_SYNC_TIMESTAMP)
        assertNotNull(WearConstants.KEY_QUOTES_PAYLOAD)
    }
}
