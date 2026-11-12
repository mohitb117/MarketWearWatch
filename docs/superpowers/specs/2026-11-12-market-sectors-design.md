# Market Sector Levels Design Specification

## 1. Intent & Context
Expand MarketWear from monitoring only two major market indices (S&P 500 and Nasdaq 100) to also monitoring the 11 US market sectors using Yahoo Finance sector indices (`^YH101` through `^YH311`). The sector quotes must appear directly below the major indices in scrollable views across the **Watch app**, **Phone app**, and **Glance Widget**, with centralized networking logic to avoid duplication.

## 2. Goals, Non-Goals, and Anti-Goals
- **Goals**:
  - Centralize market instruments (indices and sectors) in `:shared` via `MarketInstrument` registry.
  - Query Yahoo Finance via centralized `KtorMarketClient` using concurrent coroutine requests (`async`/`awaitAll`).
  - Transmit the full quote list over the Wearable Data Layer (`quotesPayload` JSON) so the Watch's Room DB caches all 13 quotes offline.
  - Render major indices pinned at top, followed by a scrollable list of all 11 sector cards on:
    - **Wear OS**: `ScalingLazyColumn` with crown rotary scroll support.
    - **Phone**: Material 3 `LazyColumn`.
    - **Glance Widget**: `androidx.glance.appwidget.lazy.LazyColumn` so users can scroll sectors directly from the phone home screen.
- **Non-Goals**: Interactive sector charting or real-time streaming WebSocket connections.
- **Anti-Goals**: No separate or duplicate networking logic on the watch or widget; all network calls originate from `KtorMarketClient` (with watch HTTP fallback using the same instrument registry).

## 3. Sector Instruments
| Symbol | Yahoo Ticker | Category | Display Name |
|---|---|---|---|
| `SPX` | `^GSPC` | `INDEX` | S&P 500 |
| `NDX` | `^IXIC` | `INDEX` | Nasdaq 100 |
| `^YH101` | `^YH101` | `SECTOR` | Basic Materials |
| `^YH102` | `^YH102` | `SECTOR` | Consumer Cyclical |
| `^YH103` | `^YH103` | `SECTOR` | Financial Services |
| `^YH104` | `^YH104` | `SECTOR` | Real Estate |
| `^YH205` | `^YH205` | `SECTOR` | Consumer Defensive |
| `^YH206` | `^YH206` | `SECTOR` | Healthcare |
| `^YH207` | `^YH207` | `SECTOR` | Utilities |
| `^YH308` | `^YH308` | `SECTOR` | Communication Services |
| `^YH309` | `^YH309` | `SECTOR` | Energy |
| `^YH310` | `^YH310` | `SECTOR` | Industrials |
| `^YH311` | `^YH311` | `SECTOR` | Technology |

## 4. Architecture & Data Flow
1. **Model (`:shared`)**:
   - `MarketInstrument(symbol, yahooTicker, name, category)`.
   - `IndexQuote` gains `category: String = "INDEX"`.
   - `MarketRegistry.ALL_INSTRUMENTS` lists all 13 instruments.
2. **Networking (`:mobile`)**:
   - `KtorMarketClient.fetchAllQuotes(): List<IndexQuote>` fires `async` requests per instrument in `Dispatchers.IO`.
   - Error handling: if an individual sector fails, remaining quotes still succeed; `fetchAllQuotes()` returns all successfully parsed quotes.
3. **Data Layer (`:mobile` -> `:app`)**:
   - `WearDataSyncManager` encodes `List<IndexQuote>` to `KEY_QUOTES_PAYLOAD`.
   - `MarketWearableListenerService` decodes `KEY_QUOTES_PAYLOAD` via `Json.decodeFromString<List<IndexQuote>>()` into Room `IndexQuoteEntity`s.
4. **UI**:
   - Watch `MainActivity`: `ScalingLazyColumn` items: header -> SPX card -> NDX card -> "Sectors" header -> sector cards -> refresh chip -> updated time.
   - Mobile `MobileDashboardScreen`: `LazyColumn` items: top indices -> "Sectors" header -> sector cards.
   - Mobile `MarketGlanceWidget`: `LazyColumn` with indices at top and scrollable sector rows.
