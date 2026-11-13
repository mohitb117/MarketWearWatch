# MarketWear: S&P 500 & Nasdaq 100 Complications for Google Pixel Watch

> **Looking for the full ecosystem architecture & diagrams?**  
> See the [Root MarketWear README](../README.md) for the multi-module architecture, Wearable Data Layer sync protocol, 11 market sectors, Jetpack Glance widget, and Mermaid diagrams.

A standalone, battery-efficient Wear OS application delivering live financial index quotes (S&P 500 ^GSPC and Nasdaq 100 ^NDX) directly to Google Pixel Watch dials via the official modern AndroidX WatchFace Complications API.

---

## Architecture & Battery Optimization
- **SuspendingComplicationDataSourceService**: Uses modern non-blocking coroutines to serve complication payloads.
- **Aggressive 15-Minute Cache**: Respects Google Wear OS power recommendations. Quotas and battery remain protected; updates are batched.
- **Ambient Always-On-Display (AOD) Compliant**: Monochromatic clean rendering on low-power display states.
- **Standalone App (\`com.google.android.wearable.standalone = true\`)**: Installs independently directly on Pixel Watch without requiring a running phone app.
- **KMP Multi-Surface Sharing**: Shares `:shared` (models, `PortfolioRegistry`, enum state shapes, `SharedMarketClient` with `Mutex` coalescing & `Semaphore(6)`), and powers Phone, Desktop (`./gradlew :desktop:runDesktop`), iOS, and Web via `:shared-ui` + `MarketWearDesignTokens`.
- **Repeatable ADB Install**: Run `./scripts/discover-and-install.sh` to restart `adb`, discover phone & watch via mDNS/USB, build `:app:assembleDebug` + `:mobile:assembleDebug`, and install both APKs.

---

## Installation on Pixel Watch 4

### Method 1: Google Play Personal App Library
1. In [Google Play Console](https://play.google.com/console), create or open your app.
2. Go to **Testing > Internal testing** or **Internal app sharing**.
3. Upload \`app-release.aab\` or \`app-release.apk\`.
4. Add your email address (\`mohitb117@gmail.com\`) to the testers list and accept the opt-in invite.
5. On your Pixel Watch, open **Google Play Store**, scroll down to **"Apps on your phone"** or **"Personal app library"**, and tap **Install**.

---

### Method 2: Wireless ADB Sideload (2 Minutes)
1. On Pixel Watch: **Settings > System > About > Versions**. Tap **Build number** 7 times.
2. Go to **Settings > Developer options**, enable **ADB debugging** and **Wireless debugging**.
3. Tap **Wireless debugging > Pair new device**. Note the IP, pairing port, and 6-digit code.
4. On your computer terminal:
   ```bash
   adb pair 192.168.1.xxx:PAIRING_PORT
   # Enter the 6-digit pairing code

   adb connect 192.168.1.xxx:CONNECT_PORT

   ./gradlew assembleRelease
   adb -s 192.168.1.xxx:CONNECT_PORT install -r app/build/outputs/apk/release/app-release.apk
   ```

---

### Method 3: Wear Installer 2 (From Phone)
1. Install **Wear Installer 2** from Google Play on your phone and Pixel Watch.
2. Send \`app-release.apk\` to your phone.
3. Select the APK inside Wear Installer 2 to install wirelessly onto your watch.

---

## Adding Complication to Your Watch Face
1. Touch and hold your Pixel Watch face.
2. Tap the **Edit (Pencil)** icon.
3. Swipe to **Complications**.
4. Tap any circular slot and select **Market Complications > S&P 500** or **Nasdaq 100**.
