package com.marketwear.shared.contracts

/**
 * Constants governing communication across Wearable Data Layer & cross-device sync.
 */
object WearConstants {
    // Data Layer paths
    const val PATH_MARKET_QUOTES = "/market/quotes"
    const val PATH_REQUEST_REFRESH = "/market/request_refresh"
    const val PATH_PORTFOLIO_CONFIG = "/market/portfolio_config"

    // Capability declarations for node discovery
    const val CAPABILITY_WATCH = "marketwear_watch_app"
    const val CAPABILITY_PHONE = "marketwear_phone_app"

    // DataMap payload keys
    const val KEY_SYNC_TIMESTAMP = "sync_timestamp"
    const val KEY_QUOTES_PAYLOAD = "quotes_payload"
    const val KEY_PORTFOLIO_INTRADAY_PAYLOAD = "portfolio_intraday_payload"
    const val KEY_DISABLED_HOLDING_IDS = "disabled_holding_ids"
    const val KEY_CUSTOM_WEIGHTS = "custom_holding_weights"
    const val KEY_CONFIG_TIMESTAMP = "config_timestamp"

    // Individual index quote prefix keys
    const val PREFIX_SPX = "spx_"
    const val PREFIX_NDX = "ndx_"

    const val FIELD_SYMBOL = "symbol"
    const val FIELD_NAME = "name"
    const val FIELD_PRICE = "price"
    const val FIELD_CHANGE = "change"
    const val FIELD_PERCENT = "percent"
    const val FIELD_HIGH = "high"
    const val FIELD_LOW = "low"
    const val FIELD_TIME = "time"
}
