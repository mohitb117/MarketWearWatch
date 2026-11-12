# Mobile Jetpack Glance Widget Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a glanceable Android home screen widget for the mobile app using Jetpack Glance that displays S&P 500 and Nasdaq 100 indices matching the watch UI aesthetic, reusing data and auto-updating with the background sync pipeline.

**Architecture:** Jetpack Glance (`androidx.glance:glance-appwidget`) declarative widget in `:mobile`. Reads `IndexQuote` from `MobileMarketRepository` (which consumes `:shared`). Updates are pushed to `MarketGlanceWidget` via `updateAll(context)` whenever `MobileSyncWorker` runs or the user refreshes.

**Tech Stack:** Kotlin 2.0.20, Jetpack Glance 1.1.0 (`glance-appwidget`, `glance-material3`), Glance Compose runtime.

**Spec:** [docs/superpowers/specs/2026-11-12-mobile-glance-widget-design.md](file:///Users/mohitb117/workspaces/MarketWearWatch/docs/superpowers/specs/2026-11-12-mobile-glance-widget-design.md)

## Global Constraints

- Android `compileSdk = 34`, `minSdk = 26` for `:mobile`.
- Jetpack Glance `1.1.0`.
- Data models from `:shared` reused without duplication.
- Widget launches `MainActivity` on tap.

## Review Focus

- Cold widget placement before any network fetch displays fallback values or loading state without crashing.
- Tap on widget brings `MainActivity` to the foreground cleanly.
- Background sync (`MobileSyncWorker`) updates the widget in the background without requiring user interaction.

---

### Task 1: Glance Dependencies, AppWidget Meta-Data & Receiver

**Files:**
- Modify: `android-wear-app/gradle/libs.versions.toml`
- Modify: `android-wear-app/mobile/build.gradle.kts`
- Create: `android-wear-app/mobile/src/main/res/xml/market_widget_info.xml`
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/widget/MarketGlanceWidgetReceiver.kt`
- Modify: `android-wear-app/mobile/src/main/AndroidManifest.xml`

**Interfaces:**
- Produces: `MarketGlanceWidgetReceiver : GlanceAppWidgetReceiver`
- Produces: `R.xml.market_widget_info`

- [x] **Step 1: Add Glance to `libs.versions.toml` and `mobile/build.gradle.kts`**
Add `glance = "1.1.0"`, `glance-appwidget`, and `glance-material3`.

- [x] **Step 2: Create `market_widget_info.xml`**
Define app widget provider configuration: `minWidth = 140dp`, `minHeight = 110dp`, `targetCellWidth = 3`, `targetCellHeight = 2`, `resizeMode = "horizontal|vertical"`.

- [x] **Step 3: Implement `MarketGlanceWidgetReceiver` and update manifest**
Register receiver for `android.appwidget.action.APPWIDGET_UPDATE` with `<meta-data android:name="android.appwidget.provider" android:resource="@xml/market_widget_info" />`.

- [x] **Step 4: Verify compilation**
Run: `./gradlew :mobile:compileDebugKotlin`
Expected: PASS

- [x] **Step 5: Commit**
```bash
git add android-wear-app/gradle/libs.versions.toml android-wear-app/mobile/
git commit -m "feat(widget): add Glance dependencies and widget provider configuration"
```

---

### Task 2: Implement `MarketGlanceWidget` UI and Automatic Update Integration

**Files:**
- Create: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/widget/MarketGlanceWidget.kt`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/widget/MarketGlanceWidgetReceiver.kt`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/sync/MobileSyncWorker.kt`
- Modify: `android-wear-app/mobile/src/main/java/com/marketwear/mobile/ui/MobileViewModel.kt`

**Interfaces:**
- Consumes: `IndexQuote` from `:shared`, `MobileMarketRepository`
- Produces: `MarketGlanceWidget.Content()` rendering S&P 500 and Nasdaq 100 rows matching watch aesthetic
- Produces: Automatic call to `MarketGlanceWidget().updateAll(context)` upon sync

- [x] **Step 1: Implement `MarketGlanceWidget`**
Extend `GlanceAppWidget`. In `provideGlance`, read quotes from `MobileMarketRepository.getInstance().quotes.value`.
Render a dark card with rounded corners:
  - Header: "MarketWear" + last updated timestamp.
  - S&P 500 row: points, change, and percentage with green/red indicator.
  - Nasdaq 100 row: points, change, and percentage.
  - Tap modifier: `actionStartActivity<MainActivity>()`.

- [x] **Step 2: Hook widget updates into background worker and ViewModel**
In `MobileSyncWorker.doWork()`, call `MarketGlanceWidget().updateAll(applicationContext)` after quotes fetch.
In `MobileViewModel.refreshAndSync()`, call `MarketGlanceWidget().updateAll(application)` on success.

- [x] **Step 3: Verify build and unit tests**
Run: `./gradlew :mobile:testDebugUnitTest :mobile:assembleDebug`
Expected: PASS

- [x] **Step 4: Install and verify on mobile device**
Run: `adb -s adb-RFCX80FBB1X-A6BjoN._adb-tls-connect._tcp install -r mobile/build/outputs/apk/debug/mobile-debug.apk`
Expected: Success

- [x] **Step 5: Commit**
```bash
git add android-wear-app/mobile/
git commit -m "feat(widget): implement MarketGlanceWidget with watch-consistent dual index UI"
```
