#!/usr/bin/env bash
# Builds the Debug app for the booted iOS simulator (booting an iPhone if none is), then
# installs and launches it. The build is unsigned; use Xcode for signed runs.
# Usage: scripts/ios-run.sh [simulator name or UDID]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUNDLE_ID="$(awk -F= '$1=="PACKAGE_NAME" { print $2 }' "$ROOT/app.properties")"
DERIVED="$ROOT/mobile/build/ios-derived-data"
UDID_RE='[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}'

PROJECT="$ROOT/mobile/iosApp/iosApp.xcodeproj"

# Only simulators this Xcode can build for: prefer an already-booted one, else the first iPhone.
eligible="$(xcodebuild -project "$PROJECT" -scheme iosApp -showdestinations 2>/dev/null | grep 'platform:iOS Simulator' || true)"
udid=""
if [[ -n "${1:-}" ]]; then
  udid="$(grep -F -- "$1" <<< "$eligible" | grep -oE "$UDID_RE" | head -n 1 || true)"
else
  for booted in $(xcrun simctl list devices booted | grep -oE "$UDID_RE" || true); do
    grep -q "$booted" <<< "$eligible" && { udid="$booted"; break; }
  done
  [[ -n "$udid" ]] || udid="$(grep 'name:iPhone' <<< "$eligible" | grep -oE "$UDID_RE" | head -n 1 || true)"
fi
[[ -n "$udid" ]] || { printf 'No eligible iOS simulator. Install one in Xcode > Settings > Components.\n' >&2; exit 1; }

xcrun simctl boot "$udid" 2>/dev/null || true   # already booted is fine
open -a Simulator
xcodebuild -project "$PROJECT" -scheme iosApp -configuration Debug \
  -destination "id=$udid" -derivedDataPath "$DERIVED" CODE_SIGNING_ALLOWED=NO -quiet build
app="$(find "$DERIVED/Build/Products/Debug-iphonesimulator" -maxdepth 1 -name '*.app' ! -name '*UITests*' | head -n 1)"
[[ -n "$app" ]] || { printf 'Build produced no .app under %s\n' "$DERIVED" >&2; exit 1; }
xcrun simctl install "$udid" "$app"
xcrun simctl launch "$udid" "$BUNDLE_ID"
