#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

ADB_BIN="${ADB_BIN:-$(command -v adb || true)}"
if [[ -z "${ADB_BIN}" ]]; then
  ADB_BIN="${HOME}/Library/Android/sdk/platform-tools/adb"
fi

if [[ ! -x "${ADB_BIN}" ]]; then
  echo "Error: adb binary not found at '${ADB_BIN}'. Set ADB_BIN or install Android platform-tools." >&2
  exit 1
fi

if [[ -z "${JAVA_HOME:-}" && -d "/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home" ]]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
fi

echo "==> Restarting ADB server to trigger fresh USB & mDNS discovery..."
"${ADB_BIN}" kill-server || true
pkill -x adb || true
sleep 1
"${ADB_BIN}" start-server || {
  sleep 1
  "${ADB_BIN}" start-server || true
}

echo "==> Discovering mDNS Wireless Debugging services (waiting up to 8s)..."
MDNS_OUTPUT=""
for _ in 1 2 3 4 5 6 7 8; do
  MDNS_OUTPUT="$("${ADB_BIN}" mdns services || true)"
  if [[ "${MDNS_OUTPUT}" == *"_adb-tls-connect._tcp"* ]] || [[ -n "$("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')" ]]; then
    break
  fi
  sleep 1
done
echo "${MDNS_OUTPUT}"

# Connect to any discovered _adb-tls-connect._tcp endpoints
while IFS= read -r line; do
  if [[ "${line}" == *"_adb-tls-connect._tcp"* ]]; then
    endpoint="$(echo "${line}" | awk '{print $NF}')"
    if [[ -n "${endpoint}" && "${endpoint}" == *:* ]]; then
      echo "==> Connecting wireless ADB endpoint: ${endpoint}"
      "${ADB_BIN}" connect "${endpoint}" || true
    fi
  fi
done <<< "${MDNS_OUTPUT}"

echo "==> Connected ADB devices:"
"${ADB_BIN}" devices -l

DEVICE_SERIALS=()
while IFS= read -r serial; do
  [[ -n "${serial}" ]] && DEVICE_SERIALS+=("${serial}")
done < <("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')

if [[ ${#DEVICE_SERIALS[@]} -eq 0 ]]; then
  echo "No authorized ADB devices found."
  echo "Tip: Ensure USB Debugging or Wireless Debugging is enabled on your phone and watch."
  exit 1
fi

echo "==> Building :app:assembleDebug (Wear OS) and :mobile:assembleDebug (Phone)..."
(cd "${PROJECT_DIR}" && ./gradlew :app:assembleDebug :mobile:assembleDebug)

WEAR_APK="${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
MOBILE_APK="${PROJECT_DIR}/mobile/build/outputs/apk/debug/mobile-debug.apk"

# Re-query devices after build (prefer non-ip:port transports first so deduplication keeps stable mDNS/USB serials)
DEVICE_SERIALS=()
while IFS= read -r serial; do
  if [[ -n "${serial}" && "${serial}" != *:* ]]; then
    DEVICE_SERIALS+=("${serial}")
  fi
done < <("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')
while IFS= read -r serial; do
  if [[ -n "${serial}" && "${serial}" == *:* ]]; then
    DEVICE_SERIALS+=("${serial}")
  fi
done < <("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')

SEEN_HW_SERIALS=""
for serial in "${DEVICE_SERIALS[@]+"${DEVICE_SERIALS[@]}"}"; do
  characteristics="$("${ADB_BIN}" -s "${serial}" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r' || true)"
  model="$("${ADB_BIN}" -s "${serial}" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)"
  hw_serial="$("${ADB_BIN}" -s "${serial}" shell getprop ro.serialno 2>/dev/null | tr -d '\r' || true)"

  if [[ -z "${characteristics}" && -z "${model}" ]]; then
    echo "==> Skipping unreachable transport ${serial}"
    continue
  fi
  if [[ -n "${hw_serial}" && "${SEEN_HW_SERIALS}" == *"|${hw_serial}|"* ]]; then
    echo "==> Skipping duplicate transport ${serial} for device ${hw_serial}"
    continue
  fi
  [[ -n "${hw_serial}" ]] && SEEN_HW_SERIALS="${SEEN_HW_SERIALS}|${hw_serial}|"

  if [[ "${characteristics}" == *"watch"* ]]; then
    echo "==> Installing Wear OS APK on ${model} (${serial})..."
    "${ADB_BIN}" -s "${serial}" install -r "${WEAR_APK}"
    "${ADB_BIN}" -s "${serial}" shell am start -n com.marketwear.indices/.MainActivity || true
  else
    echo "==> Installing Mobile APK on ${model} (${serial})..."
    "${ADB_BIN}" -s "${serial}" install -r "${MOBILE_APK}"
    "${ADB_BIN}" -s "${serial}" shell am start -n com.marketwear.indices/com.marketwear.mobile.MainActivity || true
  fi
done

echo "==> Done! Updated apps installed."
