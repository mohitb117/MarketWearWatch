package com.marketwear.shared.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

internal object NumberFormatter {

    fun formatIntegerWithCommas(value: Double): String {
        val rounded = value.roundToLong()
        val isNegative = rounded < 0
        val digits = abs(rounded).toString()
        val sb = StringBuilder()
        var count = 0
        for (i in digits.length - 1 downTo 0) {
            sb.append(digits[i])
            count++
            if (count % 3 == 0 && i > 0) {
                sb.append(',')
            }
        }
        if (isNegative) sb.append('-')
        return sb.reverse().toString()
    }

    fun formatFixed(value: Double, decimals: Int): String {
        if (decimals <= 0) return value.roundToLong().toString()
        val factor = 10.0.pow(decimals)
        val scaled = (abs(value) * factor).roundToLong()
        val intPart = scaled / factor.toLong()
        val fracPart = (scaled % factor.toLong()).toString().padStart(decimals, '0')
        val sign = if (value < 0) "-" else ""
        return "$sign$intPart.$fracPart"
    }

    fun formatCompact(value: Double): String {
        val absVal = abs(value)
        return when {
            absVal >= 1_000_000 -> "${formatFixed(absVal / 1_000_000.0, 2)}M"
            absVal >= 100_000 -> "${formatFixed(absVal / 1_000.0, 0)}k"
            absVal >= 10_000 -> "${formatFixed(absVal / 1_000.0, 1)}k"
            absVal >= 1_000 -> "${formatFixed(absVal / 1_000.0, 2)}k"
            else -> formatFixed(absVal, 2)
        }
    }
}
