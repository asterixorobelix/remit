#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE="$ROOT/mobile/composeApp/src/commonMain/composeResources/values/strings.xml"
status=0
for translation in "$ROOT"/mobile/composeApp/src/commonMain/composeResources/values-*/strings.xml; do
  [[ -f "$translation" ]] || continue
  missing="$(comm -23 <(sed -n 's/.*name="\([^"]*\)".*/\1/p' "$BASE" | sort) <(sed -n 's/.*name="\([^"]*\)".*/\1/p' "$translation" | sort))"
  if [[ -n "$missing" ]]; then printf '%s is missing:\n%s\n' "$translation" "$missing"; status=1; fi
done
# grep (not rg) so the check cannot silently pass on machines without ripgrep.
if grep -rnE --include='*.kt' '^[[:space:]]*contentDescription[[:space:]]*=[[:space:]]*"[^"$]+' "$ROOT/mobile/composeApp/src"; then
  printf 'Use stringResource for accessibility descriptions.\n'; status=1
fi
exit "$status"
