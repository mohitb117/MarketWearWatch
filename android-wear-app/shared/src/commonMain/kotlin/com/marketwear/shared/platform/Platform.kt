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
