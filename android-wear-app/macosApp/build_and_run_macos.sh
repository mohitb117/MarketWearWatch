#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_NAME="MarketWearIsland"
APP_BUNDLE="$ROOT_DIR/macosApp/build/${APP_NAME}.app"
CONTENTS="$APP_BUNDLE/Contents"
MACOS_BIN="$CONTENTS/MacOS/$APP_NAME"
SDK_PATH="$(xcrun --show-sdk-path --sdk macosx)"

mkdir -p "$CONTENTS/MacOS" /tmp/mw_swift_cache

echo "Compiling $APP_NAME with Apple Swift against macOS SDK..."
TMPDIR=/tmp xcrun swiftc \
  -parse-as-library \
  -O \
  -module-cache-path /tmp/mw_swift_cache \
  -sdk "$SDK_PATH" \
  -target arm64-apple-macos14.0 \
  -framework AppKit \
  -framework SwiftUI \
  -framework WidgetKit \
  -o "$MACOS_BIN" \
  "$ROOT_DIR/macosApp/Sources/MarketWearSharedViews.swift" \
  "$ROOT_DIR/macosApp/Sources/MarketWearMacApp.swift"

cat > "$CONTENTS/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>MarketWearIsland</string>
    <key>CFBundleIdentifier</key>
    <string>com.marketwear.macos.island</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>MarketWearIsland</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSAppTransportSecurity</key>
    <dict>
        <key>NSAllowsLocalNetworking</key>
        <true/>
        <key>NSAllowsArbitraryLoads</key>
        <true/>
    </dict>
</dict>
</plist>
EOF

echo "Typechecking WidgetKit desktop extension..."
TMPDIR=/tmp xcrun swiftc \
  -typecheck \
  -parse-as-library \
  -module-cache-path /tmp/mw_swift_cache \
  -sdk "$SDK_PATH" \
  -target arm64-apple-macos14.0 \
  -framework SwiftUI \
  -framework WidgetKit \
  "$ROOT_DIR/macosApp/Sources/MarketWearSharedViews.swift" \
  "$ROOT_DIR/macosApp/WidgetExtension/MarketWearDesktopWidget.swift"

codesign --force --deep --sign - "$APP_BUNDLE" 2>/dev/null || true

echo "Installing $APP_NAME to /Applications/..."
rm -rf "/Applications/${APP_NAME}.app"
cp -R "$APP_BUNDLE" "/Applications/${APP_NAME}.app"

if ! defaults read com.apple.dock persistent-apps 2>/dev/null | grep -q "MarketWearIsland.app"; then
  echo "Adding MarketWearIsland to macOS Dock..."
  defaults write com.apple.dock persistent-apps -array-add "<dict><key>tile-data</key><dict><key>file-data</key><dict><key>_CFURLString</key><string>/Applications/MarketWearIsland.app</string><key>_CFURLStringType</key><integer>0</integer></dict></dict></dict>"
  killall Dock 2>/dev/null || true
fi

echo "Built and installed cleanly."
if [[ "${1:-}" != "--build-only" ]]; then
  open -a "/Applications/${APP_NAME}.app"
fi
