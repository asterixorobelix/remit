#!/usr/bin/env bash
# Mirrors app.properties into the iOS xcconfig (Android and desktop read app.properties
# directly at build time). Pass --check to fail instead of writing when they differ.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONFIG="$ROOT/app.properties"
XCCONFIG="$ROOT/mobile/iosApp/Configuration/Config.xcconfig"
CHECK=false
case "${1:-}" in
  --check) CHECK=true ;;
  '') ;;
  *) printf 'Usage: %s [--check]\n' "$0" >&2; exit 2 ;;
esac

fail() { printf 'error: %s\n' "$*" >&2; exit 1; }
[[ -f "$CONFIG" ]] || fail "missing $CONFIG"
[[ -f "$XCCONFIG" ]] || fail "missing $XCCONFIG"

prop() {
  local value
  value="$(awk -v key="$1" 'index($0, key "=") == 1 { print substr($0, length(key) + 2); exit }' "$CONFIG")"
  [[ -n "$value" ]] || fail "app.properties is missing $1"
  printf '%s' "$value"
}
# xcconfig treats "//" as a comment, so URLs are written as "https:/$()/host".
xcconfig_url() { printf '%s' "${1/:\/\//:/\$()/}"; }

debug_url="$(prop API_BASE_URL_DEBUG)"
# 10.0.2.2 is the Android emulator's alias for the host; the iOS simulator uses localhost.
debug_url="${debug_url//10.0.2.2/localhost}"

SYNC_VALUES="$(printf '%s\t%s\n' \
  PRODUCT_BUNDLE_IDENTIFIER "$(prop PACKAGE_NAME)" \
  MARKETING_VERSION "$(prop VERSION_NAME)" \
  CURRENT_PROJECT_VERSION "$(prop VERSION_CODE)" \
  CAPABILITIES "$(prop FEATURES)" \
  IOS_BILLING_PRODUCT "$(prop IOS_BILLING_PRODUCT)" \
  API_BASE_URL "$(xcconfig_url "$(prop API_BASE_URL_RELEASE)")" \
  'API_BASE_URL[config=Debug]' "$(xcconfig_url "$debug_url")")"
export SYNC_VALUES

tmp="$(mktemp "${TMPDIR:-/tmp}/xcconfig.XXXXXX")"
trap 'rm -f "$tmp"' EXIT
perl -ne '
  BEGIN { for (split /\n/, $ENV{SYNC_VALUES}) { my ($k, $v) = split /\t/, $_, 2; $want{$k} = $v } }
  if (/^([A-Z_]+(?:\[[^\]]+\])?)=/ && exists $want{$1}) { $_ = "$1=$want{$1}\n"; $seen{$1} = 1 }
  print;
  END { for (sort keys %want) { print STDERR "missing $_\n" unless $seen{$_} } exit(keys(%seen) == keys(%want) ? 0 : 1) }
' "$XCCONFIG" > "$tmp" || fail "$XCCONFIG must define every synced key (see messages above)"

if cmp -s "$tmp" "$XCCONFIG"; then
  $CHECK && printf 'iOS xcconfig matches app.properties.\n'
  exit 0
fi
if $CHECK; then
  diff -u "$XCCONFIG" "$tmp" >&2 || true
  fail "iOS xcconfig is out of sync with app.properties; run scripts/sync-app-config.sh"
fi
cat "$tmp" > "$XCCONFIG"
printf 'Synced %s from app.properties.\n' "${XCCONFIG#"$ROOT"/}"
