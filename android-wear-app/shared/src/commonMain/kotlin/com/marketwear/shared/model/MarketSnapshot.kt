package com.marketwear.shared.model

import com.marketwear.shared.ui.PortfolioWeightMode
import kotlinx.serialization.Serializable

/**
 * Immutable Single Source of Truth snapshot representing market quotes, user configuration,
 * and intraday ticker history across all presentation modalities (App screens, Ongoing
 * Notifications, Home Widgets, Complications, and Tiles).
 */
@Serializable
data class MarketSnapshot(
    val quotes: List<IndexQuote> = emptyList(),
    val timestampMillis: Long = 0L,
    val disabledHoldingIds: Set<String> = emptySet(),
    val customWeights: Map<String, Double> = emptyMap(),
    val weightMode: PortfolioWeightMode = PortfolioWeightMode.NORMALIZED_100,
    val tickerIntradayData: Map<String, TickerIntradayData> = emptyMap()
) {
    val portfolioQuote: IndexQuote?
        get() = quotes.firstOrNull { it.symbol == PortfolioRegistry.PORTFOLIO_SYMBOL }

    val spxQuote: IndexQuote?
        get() = quotes.firstOrNull { it.symbol == "SPX" }

    val ndxQuote: IndexQuote?
        get() = quotes.firstOrNull { it.symbol == "NDX" }
}
