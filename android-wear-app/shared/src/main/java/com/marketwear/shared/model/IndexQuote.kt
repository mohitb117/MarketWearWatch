package com.marketwear.shared.model

import kotlinx.serialization.Serializable

/**
 * Snapshot of a major financial index quote, shared between Phone and Wear OS.
 */
@Serializable
data class IndexQuote(
    val symbol: String,        // "SPX" or "NDX"
    val name: String,          // "S&P 500" or "Nasdaq 100"
    val price: Double,         // e.g. 5985.42
    val change: Double,        // e.g. +42.15
    val percentChange: Double, // e.g. +0.71
    val dayHigh: Double,       // e.g. 6010.50
    val dayLow: Double,        // e.g. 5940.20
    val lastUpdated: Long      // Unix timestamp millis
) {
    val isPositive: Boolean get() = change >= 0
    val formattedPoints: String get() = String.format("%,.0f", price)
    val formattedChange: String get() = String.format("%s%.2f", if (isPositive) "+" else "", change)
    val formattedPercent: String get() = String.format("%s%.2f%%", if (isPositive) "+" else "", percentChange)
}
