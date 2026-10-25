#!/usr/bin/env bash
# Checks local toolchains and template configuration. Required failures exit non-zero.
set -uo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
failures=0
ok() { printf '[ok]       %-12s %s\n' "$1" "$2"; }
missing() { printf '[missing]  %-12s %s\n' "$1" "$2"; failures=$((failures + 1)); }
optional() { printf '[optional] %-12s %s\n' "$1" "$2"; }
tool() { # tool NAME required|optional WHY
  if command -v "$1" >/dev/null 2>&1; then ok "$1" "$(command -v "$1")"
  elif [[ "$2" == required ]]; then missing "$1" "$3"
  else optional "$1" "$3"; fi
}

for name in git perl; do tool "$name" required 'needed by setup and scripts'; done

if command -v java >/dev/null 2>&1; then
  java_major="$(java -version 2>&1 | awk -F'"' '/version/ {split($2, v, "."); print (v[1] == "1" ? v[2] : v[1]); exit}')"
  if [[ "$java_major" =~ ^[0-9]+$ ]] && ((java_major >= 21)); then ok java "JDK $java_major"
  else missing java "JDK 21+ required (found ${java_major:-unknown})"; fi
else
  missing java 'JDK 21+ required'
fi

if [[ -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" || -f "$ROOT/mobile/local.properties" ]]; then
  ok android-sdk "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-mobile/local.properties}}"
else
  missing android-sdk 'set ANDROID_HOME or create mobile/local.properties (sdk.dir=...)'
fi
tool adb optional 'install/debug on devices'

if [[ "$(uname -s)" == Darwin ]]; then
  tool xcodebuild required 'iOS builds'
  tool xcrun required 'iOS simulator tooling'
else
  optional xcodebuild 'iOS builds require macOS'
fi

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then ok docker 'docker compose available'
else optional docker 'backend container (docker compose up); make backend works without it'; fi
tool firebase optional 'Firebase emulators for the debug app (make emulators); npm install -g firebase-tools'
tool bundle optional 'Fastlane store releases (cd mobile && bundle install)'
tool gh optional 'GitHub automation (scripts/protect-main.sh)'
# Lint and hook tools (make bootstrap installs them from the Brewfile).
tool jq optional 'agent hooks (scripts/agent-hook.sh); make bootstrap'
tool ktlint optional 'Kotlin formatting after agent edits; make bootstrap'
for name in swiftlint shellcheck actionlint typos hadolint gitleaks; do
  tool "$name" optional 'make lint-repo, pre-commit, and agent edit hooks; make bootstrap'
done
pinned_ktlint="$(awk -F'"' '/^ktlint = "/ {print $2; exit}' "$ROOT/gradle/libs.versions.toml")"
if command -v ktlint >/dev/null 2>&1; then
  local_ktlint="$(ktlint --version 2>/dev/null | grep -m1 -oE '[0-9]+\.[0-9]+\.[0-9]+')"
  [[ "$local_ktlint" == "$pinned_ktlint" ]] ||
    printf '[warn]     %-12s %s\n' ktlint "local $local_ktlint differs from the Gradle pin $pinned_ktlint (gradle/libs.versions.toml)"
fi
if [[ "$(git -C "$ROOT" config core.hooksPath 2>/dev/null)" == .githooks ]]; then ok git-hooks '.githooks (pre-commit)'
else optional git-hooks 'pre-commit checks are not installed: make setup-hooks'; fi

for wrapper in backend/gradlew mobile/gradlew; do
  if [[ -x "$ROOT/$wrapper" ]]; then ok "$(dirname "$wrapper")" "$wrapper"; else missing "$wrapper" 'must be executable (chmod +x)'; fi
done

if [[ -f "$ROOT/app.properties" ]]; then
  if grep -Eq '^PACKAGE_NAME=com\.example\.remit$' "$ROOT/app.properties"; then
    printf '[warn]     %-12s %s\n' template 'not initialized yet: run ./setup.sh --name ... --package ...'
  fi
  if grep -Eq '^API_BASE_URL_(RELEASE|STAGING)=https://(staging-)?api\.example\.com' "$ROOT/app.properties"; then
    printf '[warn]     %-12s %s\n' endpoints 'set API_BASE_URL_STAGING/RELEASE in app.properties before release'
  fi
  "$ROOT/scripts/sync-app-config.sh" --check >/dev/null 2>&1 || missing xcconfig 'out of sync with app.properties: run scripts/sync-app-config.sh'
  # The Android app needs Firebase for Send and History: a real google-services.json, or (debug
  # builds) the local emulators, at 10.0.2.2 unless FIREBASE_EMULATOR_HOST in local.properties or
  # the environment says otherwise (as mobile/androidApp/build.gradle.kts reads it).
  if grep -Eiq '^FEATURES=(.*,)?[[:space:]]*firebase[[:space:]]*(,|$)' "$ROOT/app.properties"; then
    services="$ROOT/mobile/androidApp/google-services.json"
    if grep -q '^FIREBASE_EMULATOR_HOST=' "$ROOT/mobile/local.properties" 2>/dev/null; then
      emulator_host="$(sed -n 's/^FIREBASE_EMULATOR_HOST=//p' "$ROOT/mobile/local.properties" | tail -1)"
    else
      emulator_host="${FIREBASE_EMULATOR_HOST-10.0.2.2}"
    fi
    if [[ -f "$services" ]] && ! grep -Eq 'DEFAULT TEMPLATE|your-project-id-here' "$services"; then
      ok firebase-app 'mobile/androidApp/google-services.json'
    elif [[ -n "$emulator_host" ]]; then
      ok firebase-app "debug app uses the emulators at $emulator_host (make emulators, make backend-emulated)"
    else
      printf '[warn]     %-12s %s\n' firebase-app "FIREBASE_EMULATOR_HOST is blank, so Send and History fail in the debug app: remove it (mobile/local.properties or the environment), then rebuild (README \"Run it\")"
    fi
  fi
else
  missing app.properties 'missing'
fi

# Release delivery on GitHub (informational; needs gh with access to the repository's secrets).
if command -v gh >/dev/null 2>&1 && secrets="$(cd "$ROOT" && gh secret list --json name --jq '.[].name' 2>/dev/null)"; then
  # Release secrets normally live in the main-only "release" environment.
  secrets+=$'\n'"$(cd "$ROOT" && gh secret list --env release --json name --jq '.[].name' 2>/dev/null || true)"
  todo=()
  for name in ANDROID_SIGNING_KEY ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD \
    GOOGLE_PLAY_SERVICE_ACCOUNT_JSON MATCH_GIT_BASIC_AUTHORIZATION MATCH_PASSWORD \
    APPLE_TEAM_ID ASC_KEY_ID ASC_ISSUER_ID ASC_KEY_CONTENT; do
    grep -qx "$name" <<< "$secrets" || todo+=("$name")
  done
  if [[ "$(cd "$ROOT" && gh api 'repos/{owner}/{repo}/branches/main' --jq .protected 2>/dev/null)" == true ]] &&
    ! grep -qx RELEASE_TOKEN <<< "$secrets"; then
    todo+=(RELEASE_TOKEN)
  fi
  if ((${#todo[@]})); then
    printf '[todo]     %-12s %s\n' github "Mobile CD secrets not set: ${todo[*]} (docs/RELEASING.md)"
  else
    ok github 'Mobile CD secrets are set'
  fi
  extras=()
  for pair in OPENROUTER_API_KEY:'AI review' SEMGREP_APP_TOKEN:'Semgrep Pro rules' \
    FIREBASE_TEST_LAB_KEY:'real-device smoke tests'; do
    grep -qx "${pair%%:*}" <<< "$secrets" || extras+=("${pair%%:*} (${pair#*:})")
  done
  if ((${#extras[@]})); then
    optional github "not set: $(IFS=,; printf '%s' "${extras[*]}" | sed 's/,/, /g')"
  fi
fi

printf '[reminder] Review APP_CHECKLIST.md (make app-checklist): Share app, legal links, GitHub secrets, and more.\n'
((failures == 0)) || { printf 'Doctor found %s required issue(s).\n' "$failures"; exit 1; }
printf 'Doctor found no required issues.\n'
