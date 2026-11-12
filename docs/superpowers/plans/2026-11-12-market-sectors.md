# Market Sector Levels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Centralize market instrument querying to retrieve both major indices (S&P 500 and Nasdaq 100) and 11 Yahoo Finance sector levels (`^YH101` through `^YH311`), and display them in scrollable lists below the major indices across the Watch app, Mobile app, and Glance Widget.

**Architecture:** Define a centralized `MarketInstrument` registry in `:shared`. The mobile `KtorMarketClient` queries all 13 instruments in parallel using coroutine `async`/`awaitAll` and sends the complete list over the Wearable Data Layer (`quotesPayload`) to the watch's Room DB. Major indices remain pinned at the top, and all 11 sectors scroll smoothly below them across Wear OS (`ScalingLazyColumn`), Phone (`LazyColumn`), and Glance Widget (`LazyColumn`).

**Tech Stack:** Kotlin 2.0.20, Ktor 2.3.12, kotlinx.serialization 1.7.1, Wear Compose 1.4.0, Compose Material 3 1.2.1, Jetpack Glance 1.1.0, Room 2.6.1.

**Spec:** [docs/superpowers/specs/2026-11-12-market-sectors-design.md](file:///Users/mohitb117/workspaces/MarketWearWatch/docs/superpowers/specs/2026-11-12-market-sectors-design.md)

## Global Constraints

- 11 sectors: `^YH101` (Basic Materials), `^YH102` (Consumer Cyclical), `^YH103` (Financial Services), `^YH104` (Real Estate), `^YH205` (Consumer Defensive), `^YH206` (Healthcare), `^YH207` (Utilities), `^YH308` (Communication Services), `^YH309` (Energy), `^YH310` (Industrials), `^YH311` (Technology).
- 2 major indices: `SPX` (S&P 500) and `NDX` (Nasdaq 100).
- No duplicated networking logic: single source of truth for instrument definitions and Yahoo Finance chart API endpoints.
- No Room schema migration required (symbol remains primary key).
- Rotary crown scroll preserved and smooth on Wear OS.

## Review Focus

- Sector symbol encoding: `^` must be URL-encoded as `%5E` (e.g. `%5EYH101`) in Yahoo Finance requests.
- Parallel network queries: failure in one sector must not abort or throw away quotes for other sectors or major indices.
- Data Layer payload size: 13 serialized quotes fit well within Play Services 100KB limit (~2KB JSON).
- Cold startup: Watch and Widget provide fallback quotes for both indices and sectors if offline before initial sync.
- Crown scrolling: Scrolling seamlessly moves past the major indices into the sector list without stutter.

---

### Task 1: Centralized Instrument Registry & Data Layer Serialization

**Files:**
- Create: `android-wear-app/shared/src/main/java/com/marketwear/shared/model/MarketInstrument.kt`
- Modify: `android-wear-app/shared/src/main/java/com/marketwear/shared/model/IndexQuote.kt`
- Modify: `android-wear-app/app/build.gradle.kts`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/sync/WearDataSyncManager.kt`
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/service/MarketWearableListenerService.kt`
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/data/MarketRepository.kt`

**Interfaces:**
- Produces: `MarketInstrument`, `MarketRegistry.ALL_INSTRUMENTS`, `MarketRegistry.SECTORS`, `MarketRegistry.INDICES`
- Produces: `IndexQuote` with `category` field and fallback generator for any symbol
- Produces: Complete JSON list sync & ingestion between mobile and watch

- [x] **Step 1: Create `MarketInstrument.kt` in `:shared`**
Define `MarketInstrument(symbol, yahooTicker, name, category)` and `object MarketRegistry` listing all 13 instruments.

- [x] **Step 2: Update `IndexQuote.kt` in `:shared`**
Add `category: String = if (symbol.startsWith("^YH")) "SECTOR" else "INDEX"` and helper `MarketRegistry.getFallbackQuotes()`.

- [x] **Step 3: Update `:app` dependencies and Data Layer ingestion**
Add `kotlinx.serialization` plugin and library to `:app/build.gradle.kts`. Update `MarketWearableListenerService.kt` to decode `KEY_QUOTES_PAYLOAD` into `List<IndexQuoteEntity>` for Room DB storage.

- [x] **Step 4: Update `MarketRepository.kt` on the watch**
Support initial default quotes containing both indices and sectors from `MarketRegistry`.

- [x] **Step 5: Verify compilation**
Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew :shared:compileDebugKotlin :app:compileDebugKotlin`
Expected: PASS

- [x] **Step 6: Commit**
```bash
git add android-wear-app/shared/ android-wear-app/app/ android-wear-app/mobile/
git commit -m "feat(shared): add centralized MarketInstrument registry and full quote payload sync"
```

---

### Task 2: Centralized Concurrent Networking in `KtorMarketClient`

**Files:**
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/data/KtorMarketClient.kt`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/data/MobileMarketRepository.kt`
- Test: `android-wear-app/mobile/src/test/java/com/marketwear/mobile/data/KtorMarketClientTest.kt`

**Interfaces:**
- Produces: `KtorMarketClient.fetchQuote(instrument: MarketInstrument): IndexQuote`
- Produces: `KtorMarketClient.fetchAllQuotes(): List<IndexQuote>` using `coroutineScope` and `async`
- Produces: `MobileMarketRepository.fetchAllQuotes()` querying all instruments from `MarketRegistry`

- [x] **Step 1: Write unit tests for all instruments**
Update `KtorMarketClientTest.kt` to verify parsing of sector JSON (e.g. `^YH101` Basic Materials).

- [x] **Step 2: Generalize `KtorMarketClient`**
Remove hardcoded SPX/NDX logic; use `instrument.yahooTicker` and `instrument.name` dynamically, URL-encoding `^` to `%5E`. Add `fetchAllQuotes()` executing parallel `async` requests across `MarketRegistry.ALL_INSTRUMENTS`.

- [x] **Step 3: Update `MobileMarketRepository`**
Call `client.fetchAllQuotes()`.

- [x] **Step 4: Run unit tests**
Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew :mobile:testDebugUnitTest`
Expected: PASS

- [x] **Step 5: Commit**
```bash
git add android-wear-app/mobile/
git commit -m "feat(mobile): generalize KtorMarketClient with concurrent sector quote fetching"
```

---

### Task 3: UI Updates Across Watch, Phone, and Glance Widget

**Files:**
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/MainActivity.kt`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/ui/MobileDashboardScreen.kt`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/widget/MarketGlanceWidget.kt`

**Interfaces:**
- Produces: Watch `ScalingLazyColumn` showing major indices then `"Sectors"` header and 11 sector cards
- Produces: Phone `LazyColumn` showing major indices then `"Market Sectors"` section
- Produces: Phone `MarketGlanceWidget` with scrollable sector items beneath major indices

- [x] **Step 1: Update Watch UI (`MainActivity.kt`)**
Partition quotes into `indices` (`category == "INDEX"` or `symbol in listOf("SPX", "NDX")`) and `sectors`. Render indices first, then a styled "Market Sectors" header, followed by sector cards.

- [x] **Step 2: Update Phone Dashboard (`MobileDashboardScreen.kt`)**
Partition quotes into indices and sectors with section headers and clear layout.

- [x] **Step 3: Update Glance Widget (`MarketGlanceWidget.kt`)**
Use Glance `LazyColumn` with indices at top and scrollable sector rows.

- [x] **Step 4: Build and test all modules**
Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./gradlew test assembleDebug`
Expected: PASS

- [x] **Step 5: Commit**
```bash
git add android-wear-app/app/ android-wear-app/mobile/
git commit -m "feat(ui): display scrollable market sectors below indices on watch, phone, and widget"
```

---

### Task 4: End-to-End Verification & Device Deployment

**Files:** None (deployment & live verification)

- [x] **Step 1: Install `:app` on Pixel Watch 4**
Run: `adb -s adb-59171WRBNW40EA-6T7c7a._adb-tls-connect._tcp install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: Success

- [x] **Step 2: Install `:mobile` on Galaxy Z Fold 6**
Run: `adb -s adb-RFCX80FBB1X-A6BjoN._adb-tls-connect._tcp install -r mobile/build/outputs/apk/debug/mobile-debug.apk`
Expected: Success

- [x] **Step 3: Launch and verify live sector quotes on both devices**
Trigger refresh, verify logcat and UI rendering of sectors on watch, phone, and widget.

- [x] **Step 4: Push PR update**
Push branch `feature/mobile-companion-and-glance-widget` to update GitHub PR #1.
