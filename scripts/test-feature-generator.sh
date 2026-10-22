#!/usr/bin/env bash
# Contract test for scripts/create-feature.sh: generates into a throwaway fixture and checks
# paths, migrations, resources, and style limits the real build enforces.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/feature-generator.XXXXXX")"
trap 'rm -rf "$TEST_ROOT"' EXIT
FIXTURE="$TEST_ROOT/project with spaces"
SRC="$FIXTURE/mobile/composeApp/src"
mkdir -p "$FIXTURE/scripts" "$SRC/commonMain/sqldelight/databases" \
  "$SRC/commonMain/composeResources/values" "$SRC/commonMain/composeResources/values-de"
cp "$ROOT/scripts/create-feature.sh" "$FIXTURE/scripts/"
printf 'PACKAGE_NAME=dev.sample.app\n' > "$FIXTURE/app.properties"
printf 'SQLite format 3\0' > "$SRC/commonMain/sqldelight/databases/1.db"
for locale in values values-de; do
  printf '<resources>\n    <string name="app_name">Sample</string>\n</resources>\n' > "$SRC/commonMain/composeResources/$locale/strings.xml"
done
# Real wiring targets, renamed to the fixture package, so anchors are tested as shipped.
APP_SRC="$ROOT/mobile/composeApp/src"
wiring=(
  commonMain/kotlin/PKG/di/AppModule.kt commonMain/kotlin/PKG/navigation/AppRoute.kt
  commonMain/kotlin/PKG/navigation/NavigationFeatures.kt commonMain/kotlin/PKG/navigation/AppNavigation.kt
  desktopTest/kotlin/PKG/navigation/NavigationGraphTest.kt
)
TEMPLATE_PKG="$(awk -F= '$1=="PACKAGE_NAME" { print $2 }' "$ROOT/app.properties")"
for target in "${wiring[@]}"; do
  from="$APP_SRC/${target/PKG/$(tr . / <<< "$TEMPLATE_PKG")}"
  to="$SRC/${target/PKG/dev/sample/app}"
  mkdir -p "$(dirname "$to")"
  FROM_PKG="$TEMPLATE_PKG" perl -pe 's/\Q$ENV{FROM_PKG}\E/dev.sample.app/g' "$from" > "$to"
done
APP_KT="$SRC/commonMain/kotlin/dev/sample/app"
GRAPH_TEST="$SRC/desktopTest/kotlin/dev/sample/app/navigation/NavigationGraphTest.kt"
generate() { "$FIXTURE/scripts/create-feature.sh" "$@"; }
fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }

output="$(generate SampleFeature --dry-run)"
for suffix in Contract.kt ViewModel.kt Screen.kt UseCases.kt Repository.kt LocalRepository.kt Module.kt ViewModelTest.kt SampleFeature.sq 1.sqm; do
  grep -q "$suffix" <<< "$output" || fail "dry-run misses $suffix"
done
grep -q 'AppNavigation.kt (wire destinations)' <<< "$output" || fail 'dry-run does not list wiring'
[[ ! -e "$APP_KT/feature" ]] || fail 'dry-run wrote files'
for bad in bad-name lower Object In Developer; do
  if generate "$bad" --dry-run >/dev/null 2>&1; then fail "accepted invalid name $bad"; fi
done

generate SampleFeature >/dev/null
generate ShoppingListItems >/dev/null
feature="$SRC/commonMain/kotlin/dev/sample/app/feature/samplefeature"
db="$SRC/commonMain/sqldelight/dev/sample/app/data/db"
[[ -f "$feature/SampleFeatureViewModel.kt" ]] || fail "sources not under the package path"
[[ -f "$SRC/commonTest/kotlin/dev/sample/app/feature/samplefeature/SampleFeatureViewModelTest.kt" ]] || fail 'test missing'
grep -qx 'package dev.sample.app.feature.samplefeature' "$feature/SampleFeatureViewModel.kt" || fail 'wrong package'
[[ "$(grep -c 'CREATE TABLE' "$db/1.sqm")" == 2 ]] || fail 'migration 1.sqm must contain both tables'
for locale in values values-de; do
  strings="$SRC/commonMain/composeResources/$locale/strings.xml"
  grep -q 'name="shopping_list_items_error_refresh"' "$strings" || fail "strings missing in $locale"
  [[ "$(tail -n 1 "$strings")" == '</resources>' ]] || fail "malformed $locale/strings.xml"
done
long="$(find "$SRC" -name '*.kt' -exec awk 'length > 120 { print FILENAME ":" FNR }' {} +)"
[[ -z "$long" ]] || fail "lines over 120 characters: $long"
if grep -rq 'import .*\.\*$' "$SRC"; then fail 'wildcard import generated'; fi
if generate SampleFeature >/dev/null 2>&1; then fail 'overwrote an existing feature'; fi

# Wiring: Koin module, route, availability, destination, and graph test, with sorted imports.
expect() { grep -qF -- "$2" "$1" || fail "$(basename "$1") is missing: $2"; }
expect "$APP_KT/di/AppModule.kt" 'add(sampleFeatureModule)'
expect "$APP_KT/di/AppModule.kt" 'import dev.sample.app.feature.shoppinglistitems.shoppingListItemsModule'
expect "$APP_KT/navigation/AppRoute.kt" 'data object SampleFeature : AppRoute'
expect "$APP_KT/navigation/NavigationFeatures.kt" 'AppRoute.ShoppingListItems,'
expect "$APP_KT/navigation/AppNavigation.kt" 'composable<AppRoute.SampleFeature> {'
expect "$APP_KT/navigation/AppNavigation.kt" 'import dev.sample.app.feature.samplefeature.SampleFeatureRoute'
expect "$GRAPH_TEST" 'AppRoute.ShoppingListItems -> listOf(ShoppingListItemsViewModel::class)'
for target in "${wiring[@]}"; do
  file="$SRC/${target/PKG/dev/sample/app}"
  grep '^import' "$file" | grep -vE '^import (java|javax|kotlin)\.' | LC_ALL=C sort -c 2>/dev/null \
    || fail "unsorted imports in $(basename "$file")"
done
[[ "$(grep -c '// create-feature:' "$GRAPH_TEST")" == 2 ]] || fail 'anchors must survive wiring'
before="$(cat "$APP_KT/di/AppModule.kt")"
generate Unwired --no-wire >/dev/null
[[ "$before" == "$(cat "$APP_KT/di/AppModule.kt")" ]] || fail '--no-wire changed AppModule.kt'
perl -ni -e 'print unless m{// create-feature:destinations}' "$APP_KT/navigation/AppNavigation.kt"
output="$(generate Anchorless)"
grep -q 'Not wired (AppNavigation.kt' <<< "$output" || fail 'missing anchor not reported'
expect "$APP_KT/navigation/AppRoute.kt" 'data object Anchorless : AppRoute'

# ktlint import order (ASCII, java/javax/kotlin last) for every generated file, including
# Anchorless, whose resource keys sort before cd_navigate_back.
while IFS= read -r file; do
  grep '^import ' "$file" | awk '{ print ($2 ~ /^(java|javax|kotlin)\./ ? 1 : 0) " " $0 }' | LC_ALL=C sort -c 2>/dev/null \
    || fail "imports not in ktlint order in $(basename "$file")"
done < <(find "$SRC" -path '*/feature/*' -name '*.kt')
# compose-rules LambdaParameterInRestartableEffect: effects read the latest callback.
expect "$feature/SampleFeatureScreen.kt" 'val currentOnMessage by rememberUpdatedState(onMessage)'
# Every screen previews all of its states (check-architecture.sh enforces it).
expect "$feature/SampleFeatureScreen.kt" 'private fun SampleFeatureScreenPreview() {'
printf 'Feature generator contract passed.\n'
