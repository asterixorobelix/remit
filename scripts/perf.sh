#!/usr/bin/env bash
# Load test for POST /api/v1/payments with JMeter (perf/payments.jmx): 5 concurrent users by
# default, each creating payments and replaying each one with the same idempotency key. Start the
# Firebase emulators and the backend against them first (make emulators, make backend-emulated).
# Extra arguments go to JMeter, for example -Jusers=10 -Jloops=50 -Jhost=api.example.com. The
# Firebase web API key comes from FIREBASE_API_KEY (never -J: JMeter logs every -J property).
# Writes build/perf/results.jtl and the HTML dashboard build/perf/report/index.html, prints the
# totals, and fails when any sample failed an assertion. Usage: scripts/perf.sh [jmeter options]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out="$ROOT/build/perf"

command -v jmeter >/dev/null 2>&1 || { echo 'jmeter not found: brew install jmeter' >&2; exit 1; }
command -v jq >/dev/null 2>&1 || { echo 'jq not found: brew install jq' >&2; exit 1; }

rm -rf "$out"
mkdir -p "$out"
jmeter -n -t "$ROOT/perf/payments.jmx" -l "$out/results.jtl" -j "$out/jmeter.log" -e -o "$out/report" "$@"

stats="$out/report/statistics.json"
jq -r 'to_entries[] | .value
  | "\(.transaction): \(.sampleCount) samples, \(.errorCount) errors, mean \(.meanResTime | floor) ms, " +
    "p90 \(.pct1ResTime | floor) ms, p95 \(.pct2ResTime | floor) ms, p99 \(.pct3ResTime | floor) ms, " +
    "max \(.maxResTime | floor) ms, \(.throughput * 100 | floor / 100) req/s"' "$stats"
errors="$(jq '.Total.errorCount' "$stats")"
if [[ "$errors" != 0 ]]; then
  echo "$errors samples failed; see $out/report/index.html" >&2
  exit 1
fi
echo "Report: $out/report/index.html"
