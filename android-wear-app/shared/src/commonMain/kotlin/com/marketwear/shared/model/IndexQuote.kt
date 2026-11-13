package com.marketwear.shared.model

import kotlinx.serialization.Serializable
import kotlin.math.abs

/**
 * Snapshot of a financial index or sector quote, shared across Android Phone, Wear OS, and iOS.
 */
@Serializable
data class IndexQuote(
    val symbol: String,        // "SPX", "NDX", or "^YH101".."^YH311"
    val name: String,          // "S&P 500", "Nasdaq 100", "Basic Materials", etc.
    val price: Double,         // e.g. 5985.42
    val change: Double,        // e.g. +42.15
    val percentChange: Double, // e.g. +0.71
    val dayHigh: Double,       // e.g. 6010.50
    val dayLow: Double,        // e.g. 5940.20
    val lastUpdated: Long,     // Unix timestamp millis
    val category: String = if (symbol.startsWith("^YH")) "SECTOR" else "INDEX"
) {
    val isPositive: Boolean get() = change >= 0

    val formattedPoints: String
        get() = NumberFormatter.formatIntegerWithCommas(price)

    val formattedChange: String
        get() = (if (isPositive) "+" else "") + NumberFormatter.formatFixed(change, 2)

    val formattedPercent: String
        get() = (if (isPositive) "+" else "") + NumberFormatter.formatFixed(percentChange, 2) + "%"

    val formattedCompactPoints: String
        get() = NumberFormatter.formatCompact(price)

    val formattedCompactChange: String
        get() {
            val sign = if (isPositive) "+" else "-"
            return sign + NumberFormatter.formatCompact(abs(change))
        }
}
