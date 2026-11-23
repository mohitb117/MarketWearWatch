# Obsidian Terminal Pro UI & Notification Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform MarketWear from a debug/boilerplate layout into a bespoke, professional multiplatform financial application across Android, Wear OS, iOS, Desktop, and Web.

**Architecture:** 
1. Upgrade shared design tokens in `:shared-ui` to Obsidian Terminal Pro (`#070A0F`, `#0D131C`, `#141C2A` with emerald `#00E676`, crimson `#FF3B30`, and subtle hairline borders).
2. Clean up `PortfolioScoreCard` and `MarketDashboardBody` by eliminating raw debug formulas and duplicated cards.
3. Move Live Notification / Lock Screen preview & controls into a modern `ModalBottomSheet` on mobile, removing the permanent duplicate card and the clunky persistent bottom sync bar.
4. Eliminate all ASCII text brackets from Android Phone and Wear OS notifications, implementing clean typography and dual native actions (`Open on Watch` / `Open on Phone`).

**Tech Stack:** Kotlin Multiplatform (Compose Multiplatform), Material 3, Android 16 ProgressStyle Notification API, Wear OS OngoingActivity, Glance AppWidget.

**Spec:** User request to remove boilerplate/AI-generated vibes and replace with full-fledged professional mobile developer quality across mobile, watch, desktop, and web.

## Global Constraints

- Zero booleans for state or intent definitions (use Kotlin enums / sealed interfaces).
- Pure Kotlin Multiplatform code in `:shared` and `:shared-ui`.
- Tabular fonts and exact alignment for all numeric data.
- Absolute preservation of existing MVI store contracts and synchronization pipelines.

---

### Task 1: Obsidian Terminal Pro Theme & Design Tokens (`:shared-ui`)

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/theme/MarketWearPalette.kt`
- Modify: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/theme/MarketWearShapes.kt`
- Modify: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/theme/MarketWearDimens.kt`
- Test: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/components/PortfolioScoreCard.kt`

- [ ] Refine `MarketWearPalette.kt` with deep obsidian void layers (`#070A0F`, `#0E1420`, `#151E2E`), translucent hairline borders (`rgba(255,255,255,0.08)`), refined typography tones (`#F0F4F8` primary, `#8B9BB4` secondary, `#4A5870` tertiary).
- [ ] Refine trend badges: vibrant emerald `#00E676` and electric crimson `#FF3B30` with dark glowing container chips (`rgba(0, 230, 118, 0.12)` and `rgba(255, 59, 48, 0.12)`).
- [ ] Ensure all chips and pills have consistent corner radius (10.dp or pill) and subtle borders.

---

### Task 2: Portfolio Score Hero & Dashboard Polish (`:shared-ui`)

**Files:**
- Modify: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/components/PortfolioScoreCard.kt`
- Modify: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/components/MarketDashboardBody.kt`
- Modify: `shared-ui/src/commonMain/kotlin/com/marketwear/sharedui/components/PortfolioFluctuationBar.kt`
- Test: `shared/src/commonTest/kotlin/com/marketwear/shared/PortfolioLiveUpdateFormatterTest.kt`

- [ ] In `PortfolioScoreCard.kt`, replace raw debug text (`Norm (100%): 95.53 (-4.47%) · Range 97.34–100.00 · 37/37 active · Center 100.0 (±1% / ±5% / ±25%)`) with a clean, executive metrics row:
  - Intraday Low / High badge: `Low 97.34 · High 100.00`
  - Baseline: `Morning Baseline 100.00`
  - Breadth: `37/37 Active`
- [ ] Implement clean segmented toggle for Normalized vs Raw without clunky debug labels.
- [ ] In `MarketDashboardBody.kt`, ensure `topSlot` is reserved for contextual notifications or dismissible banners, not permanently duplicated cards.

---

### Task 3: Mobile Dashboard & Notification Settings Sheet (`:mobile`)

**Files:**
- Modify: `mobile/src/main/java/com/marketwear/mobile/ui/MobileDashboardScreen.kt`
- Modify: `mobile/src/main/java/com/marketwear/mobile/ui/MobileMarketUiState.kt`
- Modify: `mobile/src/main/java/com/marketwear/mobile/ui/MobileMarketStore.kt`

- [ ] In `MobileDashboardScreen.kt`, remove the clunky persistent bottom sync bar ("Watch Sync 6:44:59 PM" + giant blue button). Replace with:
  - Top bar subtle sync subtitle or pull-to-refresh.
  - Floating/embedded discreet sync status.
- [ ] Wire `LiveNotificationChip` in the top bar to open a sleek `ModalBottomSheet` titled **Live Activity & Lock Screen Settings**.
- [ ] Put `LiveUpdateNotificationPreviewCard` inside the bottom sheet with the `[ Collapsed | Expanded ]` toggle, live bar toggle, and Android 16 live updates permission button.
- [ ] Remove `LiveUpdateNotificationPreviewCard` from the main scrollable dashboard so the user is greeted immediately by the stunning Hero Portfolio Card!

---

### Task 4: Phone & Watch Live Notifications Elevation (`:mobile` and `:app`)

**Files:**
- Modify: `mobile/src/main/java/com/marketwear/mobile/notification/PortfolioLiveNotificationManager.kt`
- Modify: `app/src/main/java/com/marketwear/indices/notification/WatchLiveNotificationManager.kt`

- [ ] In `PortfolioLiveNotificationManager.kt`, refine `buildAndroid16ProgressNotification` and `buildLegacyProgressNotification`:
  - Rich typography with clear metrics, no raw debug strings.
  - Contextual action buttons: `Toggle Norm` · `Open Dashboard` · `Sync`.
- [ ] In `WatchLiveNotificationManager.kt`, completely strip out any ASCII brackets `[====|====]`.
  - Provide a clean Wear OS notification layout with graphical percentage chips.
  - Keep two clean distinct actions: `Open on Watch` and `Open on Phone`.

---

### Task 5: Compilation, Deployment & Verification

- [ ] Compile `:shared-ui:compileDebugKotlinAndroid`, `:mobile:assembleDebug`, `:app:assembleDebug`, `:desktop:assemble`.
- [ ] Run test suite: `./gradlew test`.
- [ ] Install updated APKs to connected devices (Galaxy Z Fold6 and Pixel Watch 4).
- [ ] Verify ongoing notification, lock screen live activity, bottom sheet, and hero dashboard.
- [ ] Commit and push to PR #15.
