package com.marketwear.shared.model

import android.annotation.SuppressLint
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Snapshot of a major financial index quote, shared between Phone and Wear OS.
 */
@SuppressLint(value = ["DefaultLocale", "UnsafeOptInUsageError"])
@Immutable
@Serializable
data class IndexQuote
constructor(
    val symbol: String,        // "SPX" or "NDX"
    val name: String,          // "S&P 500" or "Nasdaq 100"
    val price: Double,         // e.g. 5985.42
    val change: Double,        // e.g. +42.15
    val percentChange: Double, // e.g. +0.71
    val dayHigh: Double,       // e.g. 6010.50
    val dayLow: Double,        // e.g. 5940.20
    val lastUpdated: Long,     // Unix timestamp millis
    val category: String = if (symbol.startsWith("^YH")) "SECTOR" else "INDEX"
) {
    val isPositive: Boolean get() = change >= 0
    val formattedPoints: String get() = String.format("%,.0f", price)
    val formattedChange: String
        get() = String.format("%s%.2f", if (isPositive) "+" else "", change)
    val formattedPercent: String
        get() = String.format("%s%.2f%%", if (isPositive) "+" else "", percentChange)

    val formattedCompactPoints: String get() = formatCompactNumber(price)
    val formattedCompactChange: String
        get() {
            val sign = if (isPositive) "+" else "-"
            return sign + formatCompactNumber(kotlin.math.abs(change))
        }

    private fun formatCompactNumber(value: Double): String {
        val abs = kotlin.math.abs(value)
        return when {
            abs >= 1_000_000 -> String.format(java.util.Locale.US, "%.2fM", abs / 1_000_000.0)
            abs >= 100_000 -> String.format(java.util.Locale.US, "%.0fk", abs / 1_000.0)
            abs >= 10_000 -> String.format(java.util.Locale.US, "%.1fk", abs / 1_000.0)
            abs >= 1_000 -> String.format(java.util.Locale.US, "%.2fk", abs / 1_000.0)
            else -> String.format(java.util.Locale.US, "%.2f", abs)
        }
    }
}
