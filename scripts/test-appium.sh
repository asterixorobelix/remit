#!/usr/bin/env bash
# Runs the Appium UI tests (e2e/) on the connected Android emulator or device, starting an Appium
# server for the duration. It first builds and installs the debug app from the working tree, so the
# tests never run against a stale build. The app must talk to the local stack:
#   make emulators            # terminal 1: Firebase Auth + Firestore emulators
#   make backend-emulated     # terminal 2: the API against them
#   make test-appium          # with FIREBASE_EMULATOR_HOST=10.0.2.2 in mobile/local.properties
# Extra arguments go to Gradle, for example --tests '*sentPayment*'.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${APPIUM_PORT:-4723}"
LOG_DIR="$ROOT/build/appium"

die() { printf '%s\n' "$*" >&2; exit 1; }
command -v adb >/dev/null 2>&1 || die 'adb not found: install the Android SDK platform tools'
[[ "$(adb get-state 2>/dev/null)" == device ]] || die 'No Android device or emulator connected'
curl -fsS --max-time 3 -o /dev/null http://localhost:8085/health ||
  die 'No backend at localhost:8085: start make emulators and make backend-emulated first'

if [[ -z "${ANDROID_HOME:-}" ]]; then
  ANDROID_HOME="$(awk -F= '$1=="sdk.dir" { print $2 }' "$ROOT/mobile/local.properties" 2>/dev/null)"
fi
[[ -n "$ANDROID_HOME" ]] || die 'Set ANDROID_HOME or sdk.dir in mobile/local.properties'
export ANDROID_HOME

# Without the emulator host the debug app signs in to the real Firebase project, and the tests fail.
if [[ -z "${FIREBASE_EMULATOR_HOST:-}" ]]; then
  FIREBASE_EMULATOR_HOST="$(awk -F= '$1=="FIREBASE_EMULATOR_HOST" { print $2 }' "$ROOT/mobile/local.properties" 2>/dev/null)"
fi
[[ -n "$FIREBASE_EMULATOR_HOST" ]] ||
  die 'Set FIREBASE_EMULATOR_HOST=10.0.2.2 in mobile/local.properties so the app uses the emulators'

# Gradle's installDebug replaces whatever build is on the device (adb install -r).
(cd "$ROOT/mobile" && ./gradlew :androidApp:installDebug)

# Appium and its UiAutomator2 driver are pinned in e2e/appium/package.json.
export APPIUM_HOME="$ROOT/e2e/appium"
[[ -d "$APPIUM_HOME/node_modules" ]] || npm --prefix "$APPIUM_HOME" ci --ignore-scripts

mkdir -p "$LOG_DIR"
(cd "$APPIUM_HOME" && exec npx appium --port "$PORT" --log "$LOG_DIR/appium.log" --log-no-colors) \
  > "$LOG_DIR/appium.out" 2>&1 &
appium_pid=$!
trap 'kill "$appium_pid" 2>/dev/null || true' EXIT
curl -fsS --retry 30 --retry-delay 1 --retry-all-errors -o /dev/null "http://127.0.0.1:$PORT/status" ||
  die "Appium did not start; see $LOG_DIR/appium.log"

cd "$ROOT/mobile"
./gradlew :e2e:test -Pe2e -PappiumUrl="http://127.0.0.1:$PORT" --rerun "$@"
