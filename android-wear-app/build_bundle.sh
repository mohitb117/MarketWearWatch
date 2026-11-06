#!/bin/bash
set -e

echo "=================================================="
echo "  MarketWear: Building Release App Bundle (.aab)  "
echo "=================================================="

# Check for gradlew
if [ ! -f "./gradlew" ]; then
    echo "Error: gradlew not found. Please run this script from the root of the project."
    exit 1
fi

chmod +x ./gradlew

echo "Running Gradle bundleRelease..."
./gradlew bundleRelease

OUTPUT_AAB=$(find app/build/outputs/bundle/release/ -name "*.aab" | head -n 1)

if [ -n "$OUTPUT_AAB" ]; then
    echo ""
    echo "SUCCESS! Release AAB generated at:"
    echo "  -> $OUTPUT_AAB"
    echo ""
    echo "You can now drop this file directly into Google Play Console (Internal App Sharing)."
else
    echo "Build finished, check app/build/outputs/bundle/release/ for the bundle."
fi
