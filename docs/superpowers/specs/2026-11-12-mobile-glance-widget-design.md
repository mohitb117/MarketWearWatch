# Mobile Jetpack Glance Widget Design

## 1. Intent & Context
Users want a quick, glanceable home screen widget on their mobile phone that mirrors the look, feel, and data of the MarketWear watch app and complications without opening the app.

## 2. Goals, Non-Goals, and Anti-Goals
- **Goals**:
  - Add a Jetpack Glance (`androidx.glance:glance-appwidget`) widget to `:mobile`.
  - Maximize code reuse: directly display `IndexQuote` models from `:shared`.
  - Mirror the watch layout: dual index view (S&P 500 and Nasdaq 100) with points, change, and percentage.
  - Automatically update the widget when `MobileSyncWorker` runs or when the user refreshes in the mobile app.
  - Tap action launches `MainActivity`.
- **Non-Goals**: Interactive scrolling or chart rendering inside the widget.
- **Anti-Goals**: No separate networking or duplicate data polling for the widget.

## 3. Constraints
- Framework: `androidx.glance:glance-appwidget:1.1.0` and `androidx.glance:glance-material3:1.1.0`.
- Minimum SDK: 26 (matching `:mobile`).

## 4. Approaches Considered
- **Approach 1: Jetpack Glance (Selected)**: Modern Compose-style declarative UI with type-safe state and built-in Glance theme support.
- **Approach 2: Legacy XML RemoteViews**: XML layouts and AppWidgetProvider. Rejected because Glance integrates seamlessly with modern Compose and coroutines.

## 5. Technical Specification
- **Widget Component**: `MarketGlanceWidget : GlanceAppWidget()` in `:mobile`.
- **Receiver**: `MarketGlanceWidgetReceiver : GlanceAppWidgetReceiver()`.
- **XML Meta-data**: `mobile/src/main/res/xml/market_widget_info.xml`.
- **State & Updates**:
  - `MarketGlanceWidget` reads cached quotes from `MobileMarketRepository.getInstance().quotes`.
  - In `MobileSyncWorker.doWork()` and `MobileViewModel.refreshAndSync()`, call `MarketGlanceWidget().updateAll(context)`.
- **UI Layout**:
  - Dark container matching watch aesthetic (`#121212` background, rounded corners).
  - Row 1: S&P 500 with formatted points, percentage change in green (`#4CAF50`) or red (`#F44336`).
  - Row 2: Nasdaq 100 with formatted points, percentage change.
  - Footer: "MarketWear" label + updated timestamp.
  - Tap modifier: `actionStartActivity<MainActivity>()`.

## 6. What's Left to the Builder
- Exact padding and font scale adjustments across varying home screen grid sizes.
