#!/usr/bin/env bash
# Fast inner-loop gate: runs only the checks for modules changed since the branch point
# (committed, staged, unstaged, and untracked). Mobile runs JVM tests (commonTest + desktopTest)
# and an Android compile instead of the iOS link and APK; `make check` remains the full CI gate.
# Usage: scripts/quick-check.sh [--all]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [[ "${1:-}" == --all ]]; then
  changed=$'backend/\nmobile/\nscripts/'
  base="(--all)"
else
  [[ -z "${1:-}" ]] || { printf 'Usage: %s [--all]\n' "$0" >&2; exit 2; }
  base="$(git merge-base HEAD origin/main 2>/dev/null || git rev-parse HEAD)"
  changed="$(git diff --name-only "$base"; git ls-files --others --exclude-standard)"
fi
touches() { grep -qE "$1" <<< "$changed"; }

ran=0
step() { printf '\n==> %s\n' "$1"; ran=1; }
if touches '^(scripts/|setup\.sh|setup-hooks\.sh|bump-version\.sh|app\.properties|Makefile|mobile/iosApp/Configuration/)'; then
  step 'template contracts'
  make --no-print-directory check-template
fi
if touches '^(backend/|gradle/libs\.versions\.toml|app\.properties)'; then
  step 'backend: test detekt ktlintCheck'
  (cd backend && ./gradlew test detekt ktlintCheck --quiet)
fi
if touches '^(mobile/|gradle/libs\.versions\.toml|app\.properties)'; then
  step 'mobile: desktopTest detekt ktlintCheck compileAndroidMain verifySqlDelightMigration'
  (cd mobile && ./gradlew :composeApp:desktopTest detekt ktlintCheck :composeApp:compileAndroidMain \
    :composeApp:verifySqlDelightMigration --quiet)
  ./scripts/check-resources.sh
fi
if touches '^(backend|mobile)/.*\.kts?$|^gradle/libs\.versions\.toml$'; then
  step 'architecture contracts'
  ./scripts/check-architecture.sh
fi

if ((ran)); then
  printf '\nQuick checks passed. Run make check (adds coverage, APK, desktop, iOS link) before merging.\n'
else
  printf 'No module changes since %s; nothing to check (use --all to force).\n' "${base:0:12}"
fi
