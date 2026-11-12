/**
 * Constants governing communication across Google Play Services Wearable Data Layer.
 * Direct mirror of com.marketwear.shared.contracts.WearConstants.kt
 */
export const WearConstants = {
  // Data Layer paths
  PATH_MARKET_QUOTES: '/market/quotes',
  PATH_REQUEST_REFRESH: '/market/request_refresh',

  // Capability declarations for node discovery
  CAPABILITY_WATCH: 'marketwear_watch_app',
  CAPABILITY_PHONE: 'marketwear_phone_app',

  // DataMap payload keys
  KEY_SYNC_TIMESTAMP: 'sync_timestamp',
  KEY_QUOTES_PAYLOAD: 'quotes_payload',

  // Individual index quote prefix keys
  PREFIX_SPX: 'spx_',
  PREFIX_NDX: 'ndx_',

  FIELD_SYMBOL: 'symbol',
  FIELD_NAME: 'name',
  FIELD_PRICE: 'price',
  FIELD_CHANGE: 'change',
  FIELD_PERCENT: 'percent',
  FIELD_HIGH: 'high',
  FIELD_LOW: 'low',
  FIELD_TIME: 'time',
} as const;

export type WearConstantsType = typeof WearConstants;
