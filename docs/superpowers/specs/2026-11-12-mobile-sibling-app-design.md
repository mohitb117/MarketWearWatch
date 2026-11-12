# Mobile Sibling App & Wearable Data Layer Architecture Design

## 1. Intent & Context
MarketWear started as a standalone Android Wear OS watch app for the Pixel Watch providing S&P 500 and Nasdaq 100 complications and an on-wrist dashboard. While functioning, direct network polling on the watch can consume battery and suffer from spotty connectivity when Bluetooth tethering transitions.

This design introduces a companion Android mobile application (`:mobile`), extracts shared models into a common library (`:shared`), and establishes a paired data-offloading pipeline via the Google Play Services Wearable Data Layer (`DataClient`). By utilizing modern Ktor client on mobile to query financial endpoints and streaming quotes to the watch, the watch operates with maximum power efficiency and responsiveness while retaining standalone fallback capabilities.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- **Multi-Module Project**: Establish a 3-module architecture: `:app` (Wear OS), `:mobile` (Phone), and `:shared` (domain models and communication contracts).
- **Modern Mobile Architecture**: Build `:mobile` using Jetpack Compose, Material 3, and Ktor (`io.ktor:ktor-client-android`, `ktor-client-content-negotiation`, `kotlinx-serialization-json`).
- **Wearable Data Layer Offloading**: Phone fetches index quotes via Ktor and publishes them using `DataClient` (`PutDataMapRequest` at `/market/quotes`).
- **Lightweight Wear Operation**: Wear OS app receives `DataEvent` updates via `WearableListenerService`, inserts them directly into the local Room DB, and notifies watch face complications via `ComplicationDataSourceUpdateRequester`.
- **Standalone Fallback**: If the watch is disconnected from the phone (e.g. out of Bluetooth/Wi-Fi range), its local `MarketSyncWorker` activates as an autonomous fallback so complications never display stale or broken data.
- **Simple, Elegant Mobile Dashboard**: A clean Material 3 mobile UI displaying index quotes (SPX & NDX), current status, last synced timestamp, and manual refresh trigger.

### Non-Goals
- Complex portfolio management, stock searching, or historical interactive candlestick charting on mobile (future milestone).
- Full remote complication configuration from phone in v1 (keeping mobile dashboard simple first).
- iOS companion app.

### Anti-Goals
- No tight coupling where watch fails completely if phone is unreachable.
- No battery-draining continuous polling or socket connections when screens are idle.

---

## 3. Constraints
- **Android Target**: Android SDK 34, minSdk 26 for `:mobile` (phone), minSdk 30 for `:app` (Wear OS).
- **Kotlin & Tooling**: Kotlin 2.0.20, Gradle 8.9, Java 17.
- **Wearable APIs**: `com.google.android.gms:play-services-wearable`.
- **Package Consistency**: Both `:app` and `:mobile` must share the same `applicationId` / signature requirements or compatible capability declarations to communicate seamlessly over Wearable Data Layer.

---

## 4. Approaches Considered

### Approach 1: Multi-Module with `:shared` Library (Selected)
- **Design**:
  - `:shared`: Houses `IndexQuote`, JSON serialization serializers, and Wearable Data Layer constants (`MARKET_QUOTES_PATH = "/market/quotes"`, keys for price, change, percent, timestamp).
  - `:mobile`: Phone app module with Ktor client, WorkManager periodic sync, Compose Material 3 UI, and `WearableDataSender`.
  - `:app`: Wear app module retaining Room database, complications, and a `MarketWearableListenerService` to update Room.
- **Trade-offs**: Clean separation, single source of truth for contracts, zero model duplication. Requires initial multi-module Gradle setup.
- **Decision**: Chosen by user for clean architecture and long-term maintainability.

### Approach 2: Dual Standalone Modules without `:shared`
- **Design**: `:mobile` and `:app` remain isolated without a shared library, duplicating `IndexQuote` models and relying on manual string keys.
- **Trade-offs**: Faster initial setup, but prone to schema drift and breaks contract safety across the Wearable Data Layer.
- **Decision**: Rejected.

---

## 5. Technical Specification & Implementation Plan

### 5.1 Multi-Module Gradle Layout
```
MarketWearWatch/
├── build.gradle.kts
├── settings.gradle.kts (include(":shared", ":mobile", ":app"))
├── shared/
│   ├── build.gradle.kts (com.android.library, kotlinx.serialization)
│   └── src/main/java/com/marketwear/shared/
│       ├── model/IndexQuote.kt
│       └── contracts/WearConstants.kt
├── mobile/
│   ├── build.gradle.kts (com.android.application, compose, ktor, play-services-wearable, workmanager)
│   └── src/main/java/com/marketwear/mobile/
│       ├── data/KtorMarketClient.kt
│       ├── data/MobileMarketRepository.kt
│       ├── sync/WearDataSyncManager.kt
│       ├── sync/MobileSyncWorker.kt
│       ├── ui/MobileDashboardScreen.kt
│       ├── ui/MobileViewModel.kt
│       └── MainActivity.kt
└── app/ (Wear OS)
    ├── build.gradle.kts (depends on project(":shared"))
    └── src/main/java/com/marketwear/indices/
        ├── service/MarketWearableListenerService.kt
        └── ...
```

### 5.2 Network Layer (Mobile Ktor)
- Engine: `Android` or `CIO`.
- Content Negotiation: `kotlinx.serialization.json`.
- Endpoints: Yahoo Finance query endpoints for `^GSPC` and `^IXIC`.
- Output: Maps directly into `IndexQuote` domain models.

### 5.3 Wearable Data Layer Sync & Bidirectional Refresh
- **Data Push Path (Phone -> Watch)**: `/market/quotes`
  - Payload contains serialized index quotes and a timestamp.
  - Watch's `MarketWearableListenerService` receives `onDataChanged()`, inserts quotes into local Room `MarketDatabase`, and notifies complications.
- **On-Demand Refresh Path (Watch -> Phone)**: `/market/request_refresh`
  - When the user taps "Refresh" on the watch:
    - Watch checks `CapabilityClient` / `NodeClient` for a reachable paired phone node.
    - **Phone Available**: Watch sends a lightweight message to `/market/request_refresh`. The phone's `MobileWearableListenerService` triggers `MobileMarketRepository.refreshAllOnline()` using Ktor and pushes the fresh result back via `DataClient`.
    - **Phone Unavailable**: Watch immediately falls back to performing the direct online fetch locally.

### 5.4 Mobile Dashboard UI
- Single-screen Material 3 interface:
  - TopAppBar: "MarketWear" with paired watch connectivity indicator.
  - Cards: S&P 500 and Nasdaq 100 with current price, change badge, day high/low.
  - Sync Status: Last synced time, active sync spinner, and manual "Refresh & Sync to Watch" button.

### 5.5 Fallback & Battery Strategy
- **Offline-First Complications & UI**: All complications (`Sp500ComplicationService`, `NasdaqComplicationService`, `MarketDualComplicationService`) and the watch UI read strictly from the local Room database (`MarketDatabase`). They never make blocking synchronous network calls.
- **Staleness-Driven Fallback**: The watch keeps `MarketSyncWorker` scheduled every 15 minutes with network constraints. In `doWork()`, it checks the age of the latest Room data:
  - If data is fresh (<30 minutes, kept updated by phone's `DataClient`), the watch skips network execution and returns `Result.success()`, keeping battery usage near zero.
  - If data is stale (>30 minutes, indicating phone is out of range, dead, or in deep Doze), the watch fetches directly from Yahoo Finance to ensure complications never go dark.
- **Watch Discovery & Capabilities**: Uses `CapabilityClient` with capability `"marketwear_watch_app"` declared in `app/src/main/res/values/wear.xml`. The mobile app queries `getCapability(...)` to verify that the MarketWear watch app is installed and reachable.

---

## 6. What's Left to the Builder
- Choice between Ktor `CIO` or `Android` engine (Android engine recommended for mobile battery efficiency).
- Visual styling tokens for the mobile Material 3 dashboard (elevations, color palettes matching watch branding).
- Error handling UI banner (e.g. snackbar or subtle offline pill) when phone loses internet connection.
