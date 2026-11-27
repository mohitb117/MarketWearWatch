package com.marketwear.shared.platform

import io.ktor.client.HttpClient

/**
 * Returns the current epoch time in milliseconds.
 */
expect fun currentTimeMillis(): Long

/**
 * Creates a platform-configured Ktor [HttpClient] (Android engine on Android, Darwin engine on iOS).
 */
expect fun createPlatformHttpClient(): HttpClient

/**
 * Returns true if US equity markets are within active/extended trading hours (Mon-Fri, 8:30 AM - 5:00 PM ET).
 * Used to avoid waking device radios overnight and on weekends.
 */
expect fun isUsMarketWindowActive(): Boolean

/**
 * Returns true if regular US equity markets (9:30 AM America/New_York) opened between
 * [lastFetchMillis] and [currentMillis]. When true, pre-market caches must be invalidated.
 */
expect fun hasMarketOpenedSince(lastFetchMillis: Long, currentMillis: Long): Boolean
