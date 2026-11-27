#!/usr/bin/env bash
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
if lsof -i :49172 >/dev/null 2>&1; then
  exit 0
fi
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
cd "$DIR"
exec ./gradlew :desktop:runHeadlessBridge >/tmp/marketwear_headless_bridge.log 2>&1
