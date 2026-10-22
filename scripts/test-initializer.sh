#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/template-initializer.XXXXXX")"
trap 'rm -rf "$TEST_ROOT"' EXIT
FIXTURE="$TEST_ROOT/project with spaces"
mkdir -p "$FIXTURE/mobile/iosApp/Configuration" "$FIXTURE/.githooks" "$FIXTURE/scripts" "$TEST_ROOT/caller"
cp "$ROOT/setup.sh" "$ROOT/setup-hooks.sh" "$FIXTURE/"
cp "$ROOT/scripts/sync-app-config.sh" "$FIXTURE/scripts/"
cp "$ROOT/APP_CHECKLIST.md" "$FIXTURE/"
printf '#!/usr/bin/env bash\nexit 0\n' > "$FIXTURE/.githooks/pre-commit"
# The fixture always uses the template identity, even after this repo is initialized.
cat > "$FIXTURE/app.properties" <<'EOF'
APP_NAME=myproject
PACKAGE_NAME=com.example.myproject
VERSION_NAME=1.0.0
VERSION_CODE=10000
API_BASE_URL_DEBUG=http://10.0.2.2:8085
API_BASE_URL_STAGING=https://staging-api.example.com
API_BASE_URL_RELEASE=https://api.example.com
FEATURES=auth,offline,backend
IOS_BILLING_PRODUCT=com.example.myproject.premium.monthly
EOF
cat > "$FIXTURE/mobile/iosApp/Configuration/Config.xcconfig" <<'EOF'
PRODUCT_NAME=MyProject
PRODUCT_BUNDLE_IDENTIFIER=com.example.myproject
CURRENT_PROJECT_VERSION=10000
MARKETING_VERSION=1.0.0
CAPABILITIES=auth,offline,backend
IOS_BILLING_PRODUCT=com.example.myproject.premium.monthly
API_BASE_URL=https:/$()/api.example.com
API_BASE_URL[config=Debug]=http:/$()/localhost:8085
EOF

source_roots=(
  backend/src/main/kotlin backend/src/test/kotlin
  mobile/composeApp/src/commonMain/kotlin mobile/composeApp/src/commonTest/kotlin
  mobile/composeApp/src/androidMain/kotlin mobile/composeApp/src/iosMain/kotlin
  mobile/composeApp/src/desktopMain/kotlin mobile/composeApp/src/desktopTest/kotlin
  mobile/androidApp/src/main/kotlin mobile/androidApp/src/androidTest/kotlin
  mobile/composeApp/src/commonMain/sqldelight
)
for base in "${source_roots[@]}"; do
  mkdir -p "$FIXTURE/$base/com/example/myproject"
  printf 'package com.example.myproject\n// myproject\n' > "$FIXTURE/$base/com/example/myproject/Sample.kt"
done
# Identity strings outside Kotlin: release keep rules, store tooling, iOS, and local services.
android_main="$FIXTURE/mobile/composeApp/src/androidMain/kotlin/com/example/myproject"
printf 'package com.example.myproject\nclass MyProjectApplication\n' > "$android_main/MyProjectApplication.kt"
mkdir -p "$FIXTURE/mobile/androidApp" "$FIXTURE/mobile/fastlane" "$FIXTURE/mobile/iosApp/MyProjectUITests"
printf -- '-keep class com.example.myproject.** { *; }\n' > "$FIXTURE/mobile/androidApp/proguard-rules.pro"
printf 'app_identifier("com.example.myproject")\n' > "$FIXTURE/mobile/fastlane/Appfile"
printf 'final class MyProjectUITests {}\n' > "$FIXTURE/mobile/iosApp/MyProjectUITests/MyProjectUITests.swift"
printf 'db:\n\tpsql -d myproject\n' > "$FIXTURE/Makefile"
printf 'rootProject.name = "myproject"\n' > "$FIXTURE/mobile/settings.gradle.kts"
printf '\0myproject binary' > "$FIXTURE/mobile/icon.bin"
mkdir -p "$FIXTURE/.planning"
printf 'maintainer notes for com.example.myproject\n' > "$FIXTURE/.planning/STATE.md"
mkdir -p "$FIXTURE/mobile/composeApp/build"
printf 'com.example.myproject' > "$FIXTURE/mobile/composeApp/build/Generated.kt"
git -C "$FIXTURE" init -q
git -C "$TEST_ROOT/caller" init -q
git -C "$TEST_ROOT/caller" config core.hooksPath untouched

fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
snapshot() { find "$FIXTURE" -type f ! -path '*/.git/*' -exec cksum {} \; | sort; }
expect_failure() {
  local before output
  before="$(snapshot)"
  if output="$("$FIXTURE/setup.sh" "$@" 2>&1)"; then fail "unexpected success: $*"; fi
  [[ "$output" == *error:* ]] || fail "missing actionable error: $output"
  [[ "$(snapshot)" == "$before" ]] || fail "invalid input mutated the project: $*"
}
assert_sources() {
  local path="$1" package="$2" base
  for base in "${source_roots[@]}"; do
    [[ -f "$FIXTURE/$base/$path/Sample.kt" ]] || fail "missing renamed source: $base/$path"
    grep -Fqx "package $package" "$FIXTURE/$base/$path/Sample.kt" || fail "incorrect package: $base"
    [[ ! -d "$FIXTURE/$base/com/example" ]] || fail "empty template package directories left in $base"
  done
}

before="$(snapshot)"
output="$("$FIXTURE/setup.sh" --name sample-app --package dev.sample.app --features auth,offline --dry-run)"
grep -q 'Preview complete' <<< "$output"
grep -q 'desktopMain' <<< "$output"
grep -q 'sqldelight' <<< "$output"
grep -q 'MyProjectUITests' <<< "$output"
grep -q 'remove .*\.planning' <<< "$output"
grep -q 'Share app' <<< "$output"
[[ "$(snapshot)" == "$before" ]] || fail 'dry-run mutated the project'
for option in --name --package --features; do expect_failure "$option"; done
expect_failure --name --package dev.sample.app
expect_failure --name SampleApp --package Not.Valid
expect_failure --name SampleApp --package dev.fun.app
expect_failure --name SampleApp --package dev.myproject.app
for features in '' 'auth,' ',auth' 'auth,,offline' unknown; do
  expect_failure --name SampleApp --package dev.sample.app --features "$features"
done

# Directory collisions must fail before any content is rewritten.
mkdir -p "$FIXTURE/mobile/composeApp/src/desktopMain/kotlin/dev/sample/app"
expect_failure --name SampleApp --package dev.sample.app
rmdir -p "$FIXTURE/mobile/composeApp/src/desktopMain/kotlin/dev/sample/app" 2>/dev/null || true

# Invoke from a different repository: only the initialized project gets hooks.
output="$(cd "$TEST_ROOT/caller" && "$FIXTURE/setup.sh" --name sample-app --package dev.sample.app --features auth,offline)"
grep -q 'Initialization complete' <<< "$output"
grep -q 'Share app' <<< "$output"
assert_sources dev/sample/app dev.sample.app
grep -qx 'APP_NAME=sample-app' "$FIXTURE/app.properties"
grep -qx 'FEATURES=auth,offline' "$FIXTURE/app.properties"
grep -qx 'IOS_BILLING_PRODUCT=dev.sample.app.premium.monthly' "$FIXTURE/app.properties"
xcconfig="$FIXTURE/mobile/iosApp/Configuration/Config.xcconfig"
grep -qx 'PRODUCT_NAME=SampleApp' "$xcconfig"
grep -qx 'PRODUCT_BUNDLE_IDENTIFIER=dev.sample.app' "$xcconfig"
grep -qx 'CAPABILITIES=auth,offline' "$xcconfig"
grep -qx 'IOS_BILLING_PRODUCT=dev.sample.app.premium.monthly' "$xcconfig"
"$FIXTURE/scripts/sync-app-config.sh" --check >/dev/null || fail 'xcconfig not synced with app.properties'
android_main="$FIXTURE/mobile/composeApp/src/androidMain/kotlin/dev/sample/app"
grep -qx 'class SampleAppApplication' "$android_main/SampleAppApplication.kt" || fail 'type name not renamed'
grep -qx 'final class SampleAppUITests {}' "$FIXTURE/mobile/iosApp/SampleAppUITests/SampleAppUITests.swift" || fail 'iOS target not renamed'
grep -q 'dev.sample.app' "$FIXTURE/mobile/androidApp/proguard-rules.pro" || fail 'keep rules not renamed'
grep -q 'dev.sample.app' "$FIXTURE/mobile/fastlane/Appfile" || fail 'fastlane identity not renamed'
grep -q 'sample_app' "$FIXTURE/Makefile" || fail 'database slug not renamed'
grep -qx 'rootProject.name = "sample_app"' "$FIXTURE/mobile/settings.gradle.kts" || fail 'Gradle root not renamed'
leftover="$(grep -rIl --exclude-dir=.git --exclude-dir=build -e myproject -e MyProject "$FIXTURE" | grep -v '/setup.sh$' || true)"
[[ -z "$leftover" ]] || fail "template identity left in: $leftover"
grep -q 'myproject binary' "$FIXTURE/mobile/icon.bin" || fail 'binary file was rewritten'
[[ ! -e "$FIXTURE/.planning" ]] || fail 'maintainer history not removed'
grep -qx 'com.example.myproject' "$FIXTURE/mobile/composeApp/build/Generated.kt"
grep -q 'Share app' "$FIXTURE/APP_CHECKLIST.md"
[[ "$(git -C "$FIXTURE" config core.hooksPath)" == .githooks ]] || fail 'project hooks not configured'
[[ "$(git -C "$TEST_ROOT/caller" config core.hooksPath)" == untouched ]] || fail 'caller hooks changed'
expect_failure --name SampleApp --package dev.sample.app

# Force can update capabilities without moving a directory into itself.
"$FIXTURE/setup.sh" --name sample-app --package dev.sample.app --features minimal --force >/dev/null
assert_sources dev/sample/app dev.sample.app
grep -qx 'FEATURES=minimal' "$FIXTURE/app.properties"
grep -qx 'CAPABILITIES=minimal' "$xcconfig"

# A nested package and a one-character app name are valid; re-runs keep type names.
"$FIXTURE/setup.sh" --name X --package dev.sample.app.nested --force >/dev/null
assert_sources dev/sample/app/nested dev.sample.app.nested
grep -qx 'APP_NAME=X' "$FIXTURE/app.properties"
grep -qx 'PRODUCT_BUNDLE_IDENTIFIER=dev.sample.app.nested' "$xcconfig"
[[ -f "$FIXTURE/mobile/composeApp/src/androidMain/kotlin/dev/sample/app/nested/SampleAppApplication.kt" ]] || fail 'type name changed on re-run'
printf 'Initializer validation, dry-run, rename, repeat initialization, and hook isolation checks passed.\n'
