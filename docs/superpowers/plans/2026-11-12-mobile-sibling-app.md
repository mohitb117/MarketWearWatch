# Mobile Sibling App & Wearable Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a companion Android mobile app (`:mobile`) with Ktor networking and a Material 3 dashboard that offloads API polling from the Pixel Watch via the Wearable Data Layer, sharing models via a new `:shared` module.

**Architecture:** Multi-module Gradle configuration (`:shared`, `:mobile`, `:app`). Phone uses Ktor Client to fetch Yahoo Finance indices and pushes updates to the watch via `DataClient` (`/market/quotes`). The watch persists incoming payloads into Room and updates complications, falling back to local fetching only when un-tethered or when manual refresh occurs with phone unavailable.

**Tech Stack:** Kotlin 2.0.20, Jetpack Compose (Material 3 for mobile, Wear Compose for watch), Ktor 2.3.12 (Android engine, ContentNegotiation, Serialization), Google Play Services Wearable (18.2.0), AndroidX Room 2.6.1, AndroidX WorkManager 2.9.1.

**Spec:** [docs/superpowers/specs/2026-11-12-mobile-sibling-app-design.md](file:///Users/mohitb117/workspaces/MarketWearWatch/docs/superpowers/specs/2026-11-12-mobile-sibling-app-design.md)

## Global Constraints

- Android `compileSdk = 34`, `minSdk = 26` for `:mobile` and `:shared`, `minSdk = 30` for `:app`.
- Kotlin `2.0.20`, Java `17`.
- Single source of truth for contracts: All Data Layer paths and keys defined in `:shared`.
- Wear OS complications remain strictly offline-first (Room-backed) to prevent watch face stalls.
- Package compatibility: `:mobile` and `:app` share compatible package/capability definitions for Google Play Services Wearable communication.

## Review Focus

- Watch manual refresh with phone reachable delegates via `/market/request_refresh` without local radio battery drain.
- Watch manual refresh with phone unreachable falls back seamlessly to direct local fetch.
- Watch background worker does not execute duplicate HTTP fetches when data was synced <30 min ago by phone.
- Malformed or partial network JSON from Ktor handles errors without crashing or clearing Room cache.
- Wearable Data Layer payload includes unique timestamp so repeated identical prices still trigger watch updates.

---

### Task 1: Multi-Module Root Setup & `:shared` Library

**Files:**
- Create: `android-wear-app/shared/build.gradle.kts`
- Create: `android-wear-app/shared/src/main/java/com/marketwear/shared/model/IndexQuote.kt`
- Create: `android-wear-app/shared/src/main/java/com/marketwear/shared/contracts/WearConstants.kt`
- Create: `android-wear-app/shared/src/test/java/com/marketwear/shared/IndexQuoteSerializationTest.kt`
- Modify: `android-wear-app/settings.gradle.kts`
- Modify: `android-wear-app/gradle/libs.versions.toml`
- Modify: `android-wear-app/app/build.gradle.kts`
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/data/IndexQuote.kt` (re-export or alias from shared)

**Interfaces:**
- Produces: `com.marketwear.shared.model.IndexQuote` (`symbol`, `name`, `price`, `change`, `percentChange`, `dayHigh`, `dayLow`, `lastUpdated`)
- Produces: `com.marketwear.shared.contracts.WearConstants` (`PATH_MARKET_QUOTES = "/market/quotes"`, `PATH_REQUEST_REFRESH = "/market/request_refresh"`, `CAPABILITY_WATCH = "marketwear_watch_app"`, `CAPABILITY_PHONE = "marketwear_phone_app"`, data map keys)

- [ ] **Step 1: Update `libs.versions.toml` and root `settings.gradle.kts`**
Add `kotlinx-serialization-json`, Ktor 2.3.12 versions and libraries to `libs.versions.toml`. Update `settings.gradle.kts` to `include(":shared", ":app", ":mobile")`.

- [ ] **Step 2: Create `shared/build.gradle.kts`**
Apply `com.android.library`, `kotlin.android`, `kotlinx.serialization`.

- [ ] **Step 3: Write test for `IndexQuote` serialization and `WearConstants`**
Write `IndexQuoteSerializationTest.kt` verifying serialization/deserialization to JSON and verifying `WearConstants` contract keys.

- [ ] **Step 4: Implement `IndexQuote.kt` and `WearConstants.kt` in `:shared`**
Implement the data classes and constants. Link `:app` to `implementation(project(":shared"))`.

- [ ] **Step 5: Run unit tests and verify `:shared` compiles**
Run: `./gradlew :shared:testDebugUnitTest :app:compileDebugKotlin`
Expected: PASS

- [ ] **Step 6: Commit**
```bash
git add android-wear-app/shared/ android-wear-app/settings.gradle.kts android-wear-app/gradle/libs.versions.toml android-wear-app/app/
git commit -m "feat(shared): scaffold shared library module with domain models and contracts"
```

---

### Task 2: `:mobile` Module Scaffolding & Ktor Market Client

**Files:**
- Create: `android-wear-app/mobile/build.gradle.kts`
- Create: `android-wear-app/mobile/src/main/AndroidManifest.xml`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/data/KtorMarketClient.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/data/MobileMarketRepository.kt`
- Create: `android-wear-app/mobile/src/test/java/com/marketwear/mobile/KtorMarketClientParsingTest.kt`

**Interfaces:**
- Consumes: `IndexQuote` from `:shared`
- Produces: `KtorMarketClient.fetchQuote(symbol: String): IndexQuote`
- Produces: `MobileMarketRepository.fetchAllQuotes(): List<IndexQuote>`

- [ ] **Step 1: Create `mobile/build.gradle.kts` and `AndroidManifest.xml`**
Configure Android application with `applicationId = "com.marketwear.indices"`, Compose dependencies, Ktor client (`ktor-client-android`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`), `play-services-wearable`, and `work-runtime-ktx`.

- [ ] **Step 2: Write failing test for JSON response parsing**
Write `KtorMarketClientParsingTest.kt` asserting that valid Yahoo Finance chart JSON maps into standard `IndexQuote` and that invalid HTTP responses throw expected custom exceptions.

- [ ] **Step 3: Implement `KtorMarketClient` and `MobileMarketRepository`**
Implement HTTP fetching using Ktor Android engine with 6-second timeout and JSON parsing.

- [ ] **Step 4: Run test to verify it passes**
Run: `./gradlew :mobile:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add android-wear-app/mobile/
git commit -m "feat(mobile): scaffold mobile module and implement Ktor market client"
```

---

### Task 3: Mobile Wearable Sync Manager & WorkManager Background Worker

**Files:**
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/sync/WearDataSyncManager.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/sync/MobileSyncWorker.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/sync/MobileWearableListenerService.kt`
- Create: `android-wear-app/mobile/src/main/res/values/wear.xml`
- Modify: `android-wear-app/mobile/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `WearConstants` and `IndexQuote` from `:shared`, `MobileMarketRepository`
- Produces: `WearDataSyncManager.syncQuotesToWatch(quotes: List<IndexQuote>): Boolean`
- Produces: `WearDataSyncManager.checkWatchConnected(): Flow<Boolean>`
- Produces: `MobileSyncWorker.schedulePeriodicSync(context: Context)`

- [ ] **Step 1: Declare capability in `mobile/src/main/res/values/wear.xml` and manifest**
Declare `marketwear_phone_app` capability and register `MobileWearableListenerService` for message `/market/request_refresh`.

- [ ] **Step 2: Implement `WearDataSyncManager`**
Implement `putDataItem` via `Wearable.getDataClient(context)` publishing a `PutDataMapRequest` to `/market/quotes` containing quote attributes and `sync_timestamp`. Implement `CapabilityClient` check for `marketwear_watch_app`.

- [ ] **Step 3: Implement `MobileSyncWorker` and `MobileWearableListenerService`**
`MobileSyncWorker` calls `MobileMarketRepository.fetchAllQuotes()` and forwards to `WearDataSyncManager.syncQuotesToWatch()`. `MobileWearableListenerService` handles on-demand refresh messages and immediately triggers sync.

- [ ] **Step 4: Verify mobile sync compilation**
Run: `./gradlew :mobile:compileDebugKotlin`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add android-wear-app/mobile/
git commit -m "feat(mobile): implement WearDataSyncManager, periodic sync worker, and request listener"
```

---

### Task 4: Mobile Compose Material 3 Dashboard

**Files:**
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/ui/MobileMarketUiState.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/ui/MobileViewModel.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/ui/MobileDashboardScreen.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/MainActivity.kt`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/MarketMobileApplication.kt`

**Interfaces:**
- Consumes: `MobileMarketRepository`, `WearDataSyncManager`
- Produces: `MobileViewModel.uiState: StateFlow<MobileMarketUiState>`
- Produces: `MobileDashboardScreen` composable displaying market indices, connection status, and refresh controls

- [ ] **Step 1: Implement `MobileMarketUiState` and `MobileViewModel`**
Expose `quotes`, `isSyncing`, `isWatchConnected`, `lastSyncedTimestamp`, `errorMessage`. Provide `refreshAndSync()` action.

- [ ] **Step 2: Implement `MobileDashboardScreen` in Material 3**
Build a clean, modern dashboard with:
  - Material 3 `TopAppBar` with title and paired watch status chip.
  - S&P 500 and Nasdaq 100 metric cards showing price, day range progress, and positive/negative trend colors.
  - Bottom action bar with last updated time and "Refresh & Sync to Watch" button with loading spinner.

- [ ] **Step 3: Implement `MainActivity` and `MarketMobileApplication`**
Wire `setContent { MobileDashboardScreen() }` and schedule periodic WorkManager sync in `onCreate()`.

- [ ] **Step 4: Verify mobile app assembly**
Run: `./gradlew :mobile:assembleDebug`
Expected: PASS

- [ ] **Step 5: Commit**
```bash
git add android-wear-app/mobile/
git commit -m "feat(mobile): implement Material 3 mobile dashboard UI and ViewModel"
```

---

### Task 5: Wear OS Integration for Data Layer & On-Demand Refresh

**Files:**
- Create: `android-wear-app/app/src/main/java/com/marketwear/indices/service/MarketWearableListenerService.kt`
- Create: `android-wear-app/app/src/main/res/values/wear.xml`
- Modify: `android-wear-app/app/src/main/AndroidManifest.xml`
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/data/MarketRepository.kt`
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/ui/MarketViewModel.kt`
- Modify: `android-wear-app/app/src/main/java/com/marketwear/indices/sync/MarketSyncWorker.kt`

**Interfaces:**
- Consumes: `WearConstants` from `:shared`, `DataClient` / `MessageClient` events
- Produces: `MarketWearableListenerService` writing to `MarketDatabase` and updating complications
- Produces: Delegated refresh in `MarketRepository.refreshWithPhoneFallback()`

- [ ] **Step 1: Declare watch capability in `app/src/main/res/values/wear.xml` and manifest**
Declare `marketwear_watch_app` capability and register `MarketWearableListenerService` in `:app` manifest for `DATA_CHANGED`.

- [ ] **Step 2: Implement `MarketWearableListenerService` in `:app`**
Override `onDataChanged()`. Extract `DataMap` for `/market/quotes`, deserialize into `IndexQuoteEntity`, batch insert into Room `MarketDatabase`, and trigger `MarketSyncWorker.triggerComplicationUpdates()`.

- [ ] **Step 3: Update `MarketRepository` for smart bidirectional refresh**
Add `requestRefreshWithPhoneFallback()`:
  - Query `CapabilityClient.getCapability(CAPABILITY_PHONE)`.
  - If a phone node is active, send message `/market/request_refresh` via `MessageClient`.
  - If no phone node is found or message fails, invoke direct local fetch fallback.

- [ ] **Step 4: Update `MarketSyncWorker` staleness guard**
In `MarketSyncWorker.doWork()`, check Room latest timestamp. If data is fresh (<30 min), return `Result.success()` without running HTTP calls.

- [ ] **Step 5: Verify complete multi-module project build**
Run: `./gradlew assembleDebug`
Expected: PASS for all modules (`:shared`, `:mobile`, `:app`).

- [ ] **Step 6: Commit**
```bash
git add android-wear-app/app/
git commit -m "feat(wear): integrate Wearable Data Layer receiver and smart phone-delegated refresh"
```
