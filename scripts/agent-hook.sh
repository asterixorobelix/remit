#!/usr/bin/env bash
# Claude Code / Codex tool hook. The tool call arrives as JSON on stdin.
#   pre:  block edits that break a project rule: generated files, credentials and machine-local
#         config, lint baselines, and migrations that main already has (add the next one instead)
#   post: per-file feedback on the edited file, each tool only when installed (make bootstrap):
#         ktlint (formats first, then reports what it cannot fix), SwiftLint, ShellCheck,
#         actionlint, and typos
#   bash: block shell commands that skip checks or bypass review: --no-verify, plain force pushes,
#         force pushes to main, and admin merges
# Exit 2 sends stderr back to the agent; anything else never blocks work.
set -uo pipefail
mode="${1:-}"
command -v jq >/dev/null 2>&1 || exit 0
input="$(cat)"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

block() {
  printf 'BLOCKED: %s\n' "$1" >&2
  exit 2
}

# The absolute path an Edit/Write call targets (Claude Code: file_path; Codex: path).
target_file() {
  jq -r '.tool_input.file_path // .tool_input.path // empty' <<< "$input" 2>/dev/null
}

# Migrations main already has may have run on a device or server, so they are immutable.
on_main() {
  local ref
  ref="$(git -C "$ROOT" rev-parse --verify --quiet origin/main || git -C "$ROOT" rev-parse --verify --quiet HEAD)" || return 1
  git -C "$ROOT" cat-file -e "$ref:$1" 2>/dev/null
}

pre() {
  local file rel
  file="$(target_file)"
  [[ -n "$file" ]] || return 0
  rel="${file#"$ROOT"/}"
  case "$file" in
    */build/*|*/generated/*|*.g.kt)
      block "$rel is generated. Edit its source (*.sq, *.gradle.kts, composeResources) instead." ;;
    */.env|*/keystore.properties|*/local.properties|*.jks|*.keystore|*.p8|*.p12|*/google-services.json|*/GoogleService-Info.plist)
      block "$rel holds credentials or machine-local settings. Ask the user to change it." ;;
    *lint-baseline.xml|*detekt-baseline*.xml)
      block "$rel only holds accepted findings. Fix the code instead of adding a baseline entry (AGENTS.md)." ;;
    */db/migration/V*__*.sql|*/sqldelight/*.sqm|*/sqldelight/databases/*.db)
      if on_main "$rel"; then
        block "$rel is a migration that main already has. Add the next Vn__*.sql or N.sqm instead (AGENTS.md)."
      fi ;;
  esac
}

# check LABEL COMMAND...: runs COMMAND from the repository root and keeps its output on failure.
report=""
check() {
  local label="$1" result
  shift
  if ! result="$(cd "$ROOT" && "$@" 2>&1)"; then
    report+="$label:"$'\n'"$result"$'\n'
  fi
}
has() { command -v "$1" >/dev/null 2>&1; }
# SwiftLint reads mobile/iosApp/.swiftlint.yml from its working directory.
# shellcheck disable=SC2329 # invoked through check
swiftlint_strict() { cd "$ROOT/mobile/iosApp" && swiftlint lint --strict --quiet "$1"; }

post() {
  local file pinned local_version
  file="$(target_file)"
  [[ -n "$file" && -f "$file" ]] || return 0
  case "$file" in
    *.kt|*.kts)
      if has ktlint; then
        check 'ktlint (auto-formatted; these need a manual fix)' ktlint --format --relative "$file"
        pinned="$(awk -F'"' '/^ktlint = "/ {print $2; exit}' "$ROOT/gradle/libs.versions.toml")"
        local_version="$(ktlint --version 2>/dev/null | grep -m1 -oE '[0-9]+\.[0-9]+\.[0-9]+')"
        if [[ -n "$report" && -n "$pinned" && "$local_version" != "$pinned" ]]; then
          report+="note: local ktlint $local_version, Gradle pins $pinned; ktlintCheck is authoritative."$'\n'
        fi
      fi ;;
    *.swift)
      if has swiftlint; then
        (cd "$ROOT/mobile/iosApp" && swiftlint lint --fix --quiet "$file" >/dev/null 2>&1)
        check 'swiftlint --strict' swiftlint_strict "$file"
      fi ;;
    *.sh|*/.githooks/*)
      has shellcheck && check shellcheck shellcheck "$file" ;;
    */.github/workflows/*.yml|*/.github/workflows/*.yaml)
      has actionlint && check actionlint actionlint "$file" ;;
  esac
  has typos && check 'typos (fix, or allow the word in _typos.toml)' typos --format brief "$file"
  if [[ -n "$report" ]]; then
    printf '%s' "$report" >&2
    exit 2
  fi
}

bash_command() {
  local cmd segment
  # Claude Code sends a string; Codex may send an argv array.
  cmd="$(jq -r '.tool_input.command | if type == "array" then join(" ") else (. // empty) end' <<< "$input" 2>/dev/null)"
  [[ -n "$cmd" ]] || return 0
  # One simple command per line (split on newlines, ;, &&, ||, and |), so a flag only counts for
  # the git or gh invocation it belongs to, and prose in a heredoc or commit message rarely matches.
  cmd="${cmd//&&/$'\n'}"
  cmd="${cmd//||/$'\n'}"
  cmd="${cmd//;/$'\n'}"
  cmd="${cmd//|/$'\n'}"
  local git_re='[[:space:]]git[[:space:]]+(-C[[:space:]]+[^[:space:]]+[[:space:]]+)?'
  while IFS= read -r segment; do
    segment=" $segment "
    if grep -qE "${git_re}(commit|push|merge|rebase|cherry-pick|revert|am|pull)([[:space:]].*)?[[:space:]]--no-verify[[:space:]]" <<< "$segment"; then
      block 'do not skip Git hooks with --no-verify. Fix what the pre-commit hook reports (AGENTS.md).'
    fi
    if grep -qE "${git_re}push[[:space:]]" <<< "$segment"; then
      if grep -qE '[[:space:]](--force|-f)[[:space:]]' <<< "$segment"; then
        block 'plain force pushes can drop commits. Use --force-with-lease on your own branch.'
      fi
      if grep -qE '[[:space:]](--force-with-lease[^[:space:]]*|\+[^[:space:]]*main)[[:space:]]' <<< "$segment" &&
        grep -qE '[[:space:]:+]main[[:space:]]' <<< "$segment"; then
        block 'never force push to main.'
      fi
    fi
    if grep -qE '[[:space:]]gh[[:space:]]+pr[[:space:]]+merge([[:space:]].*)?[[:space:]]--admin[[:space:]]' <<< "$segment"; then
      block 'do not bypass branch protection with --admin. Use gh pr merge --auto --squash (AGENTS.md).'
    fi
  done <<< "$cmd"
}

case "$mode" in
  pre) pre ;;
  post) post ;;
  bash) bash_command ;;
esac
exit 0
