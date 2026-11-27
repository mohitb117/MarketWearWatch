#!/usr/bin/env bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"

# If port 49172 is already responding, no need to start another daemon
if lsof -i :49172 >/dev/null 2>&1; then
  exit 0
fi

# Detect available JDK 17+ or fallback
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x /usr/libexec/java_home ]]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17+ 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)"
  fi
fi
if [[ -z "${JAVA_HOME:-}" && -d /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ]]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
fi

cd "$DIR"
exec ./gradlew :desktop:runHeadlessBridge >/tmp/marketwear_headless_bridge.log 2>&1
