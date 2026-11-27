#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_NAME="MarketWearIsland"
APP_BUNDLE="$ROOT_DIR/macosApp/build/${APP_NAME}.app"
CONTENTS="$APP_BUNDLE/Contents"
MACOS_BIN="$CONTENTS/MacOS/$APP_NAME"
SDK_PATH="$(xcrun --show-sdk-path --sdk macosx)"

mkdir -p "$CONTENTS/MacOS" "$CONTENTS/Resources" /tmp/mw_swift_cache

if [[ -f "$ROOT_DIR/macosApp/Resources/AppIcon.icns" ]]; then
  cp "$ROOT_DIR/macosApp/Resources/AppIcon.icns" "$CONTENTS/Resources/AppIcon.icns"
fi
if [[ -f "$ROOT_DIR/start_headless_bridge.sh" ]]; then
  cp "$ROOT_DIR/start_headless_bridge.sh" "$CONTENTS/Resources/start_headless_bridge.sh"
  chmod +x "$CONTENTS/Resources/start_headless_bridge.sh"
fi

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
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
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

echo "Building WidgetKit desktop extension..."
WIDGET_NAME="MarketWearDesktopWidget"
WIDGET_BUNDLE="$CONTENTS/PlugIns/${WIDGET_NAME}.appex"
WIDGET_CONTENTS="$WIDGET_BUNDLE/Contents"
WIDGET_BIN="$WIDGET_CONTENTS/MacOS/$WIDGET_NAME"

mkdir -p "$WIDGET_CONTENTS/MacOS" "$WIDGET_CONTENTS/Resources"

if [[ -f "$ROOT_DIR/macosApp/Resources/AppIcon.icns" ]]; then
  cp "$ROOT_DIR/macosApp/Resources/AppIcon.icns" "$WIDGET_CONTENTS/Resources/AppIcon.icns"
fi

TMPDIR=/tmp xcrun swiftc \
  -parse-as-library \
  -O \
  -module-cache-path /tmp/mw_swift_cache \
  -sdk "$SDK_PATH" \
  -target arm64-apple-macos14.0 \
  -framework AppKit \
  -framework SwiftUI \
  -framework WidgetKit \
  -o "$WIDGET_BIN" \
  "$ROOT_DIR/macosApp/Sources/MarketWearSharedViews.swift" \
  "$ROOT_DIR/macosApp/WidgetExtension/MarketWearDesktopWidget.swift"

cat > "$WIDGET_CONTENTS/Info.plist" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>MarketWearDesktopWidget</string>
    <key>CFBundleDisplayName</key>
    <string>MarketWear Watch</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>com.marketwear.macos.island.widget</string>
    <key>CFBundleInfoDictionaryVersion</key>
    <string>6.0</string>
    <key>CFBundleName</key>
    <string>MarketWearDesktopWidget</string>
    <key>CFBundlePackageType</key>
    <string>XPC!</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleSupportedPlatforms</key>
    <array>
        <string>MacOSX</string>
    </array>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>NSExtension</key>
    <dict>
        <key>NSExtensionPointIdentifier</key>
        <string>com.apple.widgetkit-extension</string>
    </dict>
</dict>
</plist>
EOF

ENTITLEMENTS="$ROOT_DIR/macosApp/Resources/Widget.entitlements"
xattr -cr "$WIDGET_BUNDLE"
codesign --force --sign - --entitlements "$ENTITLEMENTS" "$WIDGET_BUNDLE"
xattr -cr "$APP_BUNDLE"
codesign --force --sign - "$APP_BUNDLE"

echo "Installing $APP_NAME to /Applications/..."
rm -rf "/Applications/${APP_NAME}.app"
cp -R "$APP_BUNDLE" "/Applications/${APP_NAME}.app"
xattr -cr "/Applications/${APP_NAME}.app"
codesign --force --sign - --entitlements "$ENTITLEMENTS" "/Applications/${APP_NAME}.app/Contents/PlugIns/${WIDGET_NAME}.appex"
codesign --force --sign - "/Applications/${APP_NAME}.app"

# Register with LaunchServices & PluginKit so WidgetKit discovers the extension
/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister -f "/Applications/${APP_NAME}.app"
pluginkit -a "/Applications/${APP_NAME}.app/Contents/PlugIns/${WIDGET_NAME}.appex" 2>/dev/null || true
sleep 1
killall chronod 2>/dev/null || true
killall NotificationCenter 2>/dev/null || true
swift -e 'import AppKit; if let img = NSImage(contentsOfFile: "/Applications/MarketWearIsland.app/Contents/Resources/AppIcon.icns") { NSWorkspace.shared.setIcon(img, forFile: "/Applications/MarketWearIsland.app") }' 2>/dev/null || true

if ! defaults read com.apple.dock persistent-apps 2>/dev/null | grep -q "MarketWearIsland.app"; then
  echo "Adding MarketWearIsland to macOS Dock..."
  defaults write com.apple.dock persistent-apps -array-add "<dict><key>tile-data</key><dict><key>file-data</key><dict><key>_CFURLString</key><string>/Applications/MarketWearIsland.app</string><key>_CFURLStringType</key><integer>0</integer></dict></dict></dict>"
  killall Dock 2>/dev/null || true
fi

echo "Built and installed cleanly."
if [[ "${1:-}" != "--build-only" ]]; then
  open -a "/Applications/${APP_NAME}.app"
fi
