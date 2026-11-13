package com.marketwear.shared.model

import com.marketwear.shared.platform.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
enum class InstrumentCategory {
    INDEX,
    SECTOR,
    PORTFOLIO
}

@Serializable
data class MarketInstrument(
    val symbol: String,
    val yahooTicker: String,
    val name: String,
    val category: InstrumentCategory
) {
    val isSector: Boolean get() = category == InstrumentCategory.SECTOR
}

object MarketRegistry {
    val SPX = MarketInstrument("SPX", "^GSPC", "S&P 500", InstrumentCategory.INDEX)
    val NDX = MarketInstrument("NDX", "^IXIC", "Nasdaq 100", InstrumentCategory.INDEX)

    val YH101 = MarketInstrument("^YH101", "^YH101", "Basic Materials", InstrumentCategory.SECTOR)
    val YH102 = MarketInstrument("^YH102", "^YH102", "Consumer Cyclical", InstrumentCategory.SECTOR)
    val YH103 = MarketInstrument("^YH103", "^YH103", "Financial Services", InstrumentCategory.SECTOR)
    val YH104 = MarketInstrument("^YH104", "^YH104", "Real Estate", InstrumentCategory.SECTOR)
    val YH205 = MarketInstrument("^YH205", "^YH205", "Consumer Defensive", InstrumentCategory.SECTOR)
    val YH206 = MarketInstrument("^YH206", "^YH206", "Healthcare", InstrumentCategory.SECTOR)
    val YH207 = MarketInstrument("^YH207", "^YH207", "Utilities", InstrumentCategory.SECTOR)
    val YH308 = MarketInstrument("^YH308", "^YH308", "Communication Services", InstrumentCategory.SECTOR)
    val YH309 = MarketInstrument("^YH309", "^YH309", "Energy", InstrumentCategory.SECTOR)
    val YH310 = MarketInstrument("^YH310", "^YH310", "Industrials", InstrumentCategory.SECTOR)
    val YH311 = MarketInstrument("^YH311", "^YH311", "Technology", InstrumentCategory.SECTOR)

    val INDICES = listOf(SPX, NDX)
    val SECTORS = listOf(
        YH101, YH102, YH103, YH104, YH205, YH206,
        YH207, YH308, YH309, YH310, YH311
    )
    val ALL_INSTRUMENTS = INDICES + SECTORS

    fun findBySymbol(symbol: String): MarketInstrument? {
        return ALL_INSTRUMENTS.find { it.symbol == symbol }
    }

    fun isSector(symbol: String): Boolean {
        return symbol.startsWith("^YH")
    }

    fun getFallbackQuotes(): List<IndexQuote> {
        val now = currentTimeMillis()
        return listOf(
            PortfolioRegistry.createDefaultFallbackPortfolioQuote(now),
            IndexQuote("SPX", "S&P 500", 5985.42, 22.84, 0.38, 6005.10, 5960.30, now, "INDEX"),
            IndexQuote("NDX", "Nasdaq 100", 21110.15, -45.20, -0.21, 21250.00, 21050.80, now, "INDEX"),
            IndexQuote("^YH101", "Basic Materials", 2220463.0, 34300.0, 1.57, 2250000.0, 2190000.0, now, "SECTOR"),
            IndexQuote("^YH102", "Consumer Cyclical", 1850120.0, 12400.0, 0.67, 1870000.0, 1830000.0, now, "SECTOR"),
            IndexQuote("^YH103", "Financial Services", 1420500.0, -8200.0, -0.57, 1440000.0, 1410000.0, now, "SECTOR"),
            IndexQuote("^YH104", "Real Estate", 512300.0, 2100.0, 0.41, 520000.0, 508000.0, now, "SECTOR"),
            IndexQuote("^YH205", "Consumer Defensive", 980400.0, -1500.0, -0.15, 990000.0, 975000.0, now, "SECTOR"),
            IndexQuote("^YH206", "Healthcare", 1650300.0, 8900.0, 0.54, 1670000.0, 1640000.0, now, "SECTOR"),
            IndexQuote("^YH207", "Utilities", 420800.0, 1100.0, 0.26, 425000.0, 418000.0, now, "SECTOR"),
            IndexQuote("^YH308", "Communication Services", 1280900.0, 14500.0, 1.14, 1295000.0, 1270000.0, now, "SECTOR"),
            IndexQuote("^YH309", "Energy", 890200.0, -6400.0, -0.71, 905000.0, 885000.0, now, "SECTOR"),
            IndexQuote("^YH310", "Industrials", 1750600.0, 9300.0, 0.53, 1770000.0, 1740000.0, now, "SECTOR"),
            IndexQuote("^YH311", "Technology", 27007258.0, 290900.0, 1.09, 27300000.0, 26800000.0, now, "SECTOR")
        )
    }
}
