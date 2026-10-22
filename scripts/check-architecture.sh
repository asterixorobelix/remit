#!/usr/bin/env bash
# Mechanical checks for the AGENTS.md contracts that detekt and ktlint cannot express:
# forbidden libraries, layer boundaries, state exposure, Koin registration, and literal UI
# values. grep-only, so it is fast enough for make check-template, the pre-commit hook, and CI.
# Usage: scripts/check-architecture.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PACKAGE="$(awk -F= '$1=="PACKAGE_NAME" {print $2}' app.properties)"
[[ -n "$PACKAGE" ]] || { printf 'error: PACKAGE_NAME is missing from app.properties\n' >&2; exit 2; }
PKG_RE="${PACKAGE//./\\.}"
SRC=mobile/composeApp/src
status=0

# check NAME FIX PATTERN PATHSPEC... : report every matching line (tracked and new files).
check() {
  local name="$1" fix="$2" pattern="$3" hits
  shift 3
  # Comment lines (KDoc samples, explanations) are not code.
  hits="$(git grep --untracked -nIE "$pattern" -- "$@" ':(exclude)**/build/**' \
    | grep -vE '^[^:]+:[0-9]+:[[:space:]]*(\*|//|/\*)' || true)"
  if [[ -n "$hits" ]]; then
    printf '[architecture] %s\n  fix: %s\n' "$name" "$fix"
    printf '  %s\n' "${hits//$'\n'/$'\n'  }"
    printf '\n'
    status=1
  fi
}

# Word boundaries are spelled out because \b is not portable across grep implementations.
W='(^|[^A-Za-z0-9_])'
MOBILE_MAIN=(":(glob)$SRC/*Main/**/*.kt" ':(glob)mobile/androidApp/src/main/**/*.kt')
SHARED_UI=(":(glob)$SRC/commonMain/**/*.kt" ":(glob)$SRC/androidMain/**/*.kt" ":(glob)$SRC/iosMain/**/*.kt")

check 'Forbidden library import' \
  'use Koin, Compose Navigation, StateFlow, Ktor, kotlinx.serialization, Coil, and SQLDelight (AGENTS.md)' \
  '^import (dagger\.|javax\.inject\.|cafe\.adriel\.voyager|androidx\.lifecycle\.(Mutable)?LiveData|retrofit2\.|com\.google\.gson|com\.squareup\.moshi|com\.fasterxml\.jackson|io\.reactivex|com\.bumptech\.glide|com\.squareup\.picasso|androidx\.room\.)' \
  ':(glob)**/*.kt'

check 'Forbidden library dependency' \
  'remove it; see the library table in AGENTS.md' \
  '(com\.google\.dagger|cafe\.adriel\.voyager|com\.squareup\.retrofit2|com\.google\.code\.gson|com\.squareup\.moshi|io\.reactivex|com\.github\.bumptech\.glide|com\.squareup\.picasso|androidx\.room)' \
  'gradle/libs.versions.toml' ':(glob)**/*.gradle.kts'

check 'Domain layer depends on a framework or an outer layer' \
  'keep domain/ pure Kotlin: depend on repository interfaces, never data/, presentation/, ui/, Android, Ktor, SQLDelight, or Koin' \
  "^import (androidx\\.|android\\.|io\\.ktor\\.|app\\.cash\\.sqldelight|org\\.koin\\.|${PKG_RE}\\.(data|presentation|ui|di)\\.)" \
  ":(glob)$SRC/commonMain/**/domain/**/*.kt"

check 'Presentation depends on the data layer' \
  'ViewModels call use cases from domain/; let Koin inject the data layer' \
  "^import ${PKG_RE}\\.data\\." \
  ":(glob)$SRC/commonMain/**/presentation/**/*.kt"

check 'Publicly exposed MutableStateFlow' \
  'keep MutableStateFlow private and expose StateFlow via asStateFlow()' \
  '^[[:space:]]*((public|internal|protected|override)[[:space:]]+)*(val|var)[[:space:]]+[A-Za-z0-9_]+[[:space:]]*(:[[:space:]]*MutableStateFlow|=[[:space:]]*MutableStateFlow)' \
  "${MOBILE_MAIN[@]}"

check 'Mutable public state in a ViewModel' \
  'expose immutable StateFlow; no public var properties' \
  '^[[:space:]]*((public|internal|override|open)[[:space:]]+)*var[[:space:]]' \
  ":(glob)$SRC/**/*ViewModel.kt"

check 'ViewModel constructed directly' \
  'register it with viewModelOf in di/ and obtain it with koinViewModel()' \
  '(=|return|\(|\{)[[:space:]]*[A-Z][A-Za-z0-9]*ViewModel\(' \
  "${MOBILE_MAIN[@]}" ":(exclude,glob)$SRC/commonMain/**/di/**"

check 'Use case registered as a singleton' \
  'register use cases with factoryOf (AGENTS.md)' \
  '(singleOf\(::[A-Za-z0-9]*UseCase|single[[:space:]]*(<[^>]*>)?[[:space:]]*\{[[:space:]]*[A-Za-z0-9]*UseCase\()' \
  ":(glob)$SRC/**/di/**/*.kt"

check 'Repository registered as a factory' \
  'register repositories with singleOf (AGENTS.md)' \
  '(factoryOf\(::[A-Za-z0-9]*Repository|factory[[:space:]]*(<[^>]*>)?[[:space:]]*\{[[:space:]]*[A-Za-z0-9]*Repository)' \
  ":(glob)$SRC/**/di/**/*.kt"

# check_ui NAME FIX PATTERN PATHSPEC... : like check, but skips comments and @Preview functions
# (sample data in previews is fine) by tracking brace depth from the annotation to the end of
# the previewed function.
check_ui() {
  local name="$1" fix="$2" pattern="$3" files hits
  shift 3
  files="$(git ls-files --cached --others --exclude-standard -- "$@")"
  [[ -n "$files" ]] || return 0
  # shellcheck disable=SC2086 # file names in this template contain no spaces
  hits="$(awk -v pattern="$pattern" '
    FNR == 1 { preview = 0; started = 0; depth = 0 }
    /^[[:space:]]*(\*|\/\/|\/\*)/ { next }
    /@Preview/ { preview = 1; started = 0; depth = 0 }
    preview {
      line = $0
      opened = gsub(/[{]/, "{", line)
      closed = gsub(/[}]/, "}", line)
      if (opened > 0) started = 1
      depth += opened - closed
      if (started && depth <= 0) preview = 0
      next
    }
    $0 ~ pattern { print FILENAME ":" FNR ":" $0 }
  ' $files)"
  if [[ -n "$hits" ]]; then
    printf '[architecture] %s\n  fix: %s\n' "$name" "$fix"
    printf '  %s\n' "${hits//$'\n'/$'\n'  }"
    printf '\n'
    status=1
  fi
}

check_ui 'Literal user-facing text' \
  'use stringResource(Res.string.x); literals are fine only inside @Preview functions' \
  "${W}(Text[(][[:space:]]*\"|(text|title|subtitle|placeholder|supportingText|message)[[:space:]]*=[[:space:]]*\"[^\"\$]+\")" \
  "${SHARED_UI[@]}"

check_ui 'Hard-coded color outside the theme' \
  'add it to ui/theme and read it from MaterialTheme.colorScheme' \
  "${W}Color[(][[:space:]]*0x" \
  "${SHARED_UI[@]}" ":(exclude,glob)$SRC/commonMain/**/ui/theme/**"

# 0.dp is "no size", not a design value.
check_ui 'Hard-coded dimension outside the theme' \
  'use the Dimens object' \
  "(^|[^A-Za-z0-9_.])([1-9][0-9]*|0[.][0-9]*[1-9])([.][0-9]+)?[.](dp|sp)([^A-Za-z0-9_]|\$)" \
  "${SHARED_UI[@]}" ":(exclude,glob)$SRC/commonMain/**/ui/theme/**"

# Every file that declares a screen previews it (expect declarations have no body to preview).
screens_without_preview="$(git ls-files --cached --others --exclude-standard -- \
  ":(glob)$SRC/commonMain/**/*.kt" | while IFS= read -r file; do
  if grep -qE '^(internal )?fun [A-Z][A-Za-z0-9]*Screen[(]' "$file" && ! grep -q '@Preview' "$file"; then
    printf '%s\n' "$file"
  fi
done)"
if [[ -n "$screens_without_preview" ]]; then
  printf '[architecture] Screen without a @Preview\n'
  printf '  fix: add one private @Preview showing every UiState (ui/preview/ScreenStates)\n'
  printf '  %s\n' "${screens_without_preview//$'\n'/$'\n'  }"
  printf '\n'
  status=1
fi

if ((status == 0)); then
  printf 'Architecture contracts passed.\n'
fi
exit "$status"
