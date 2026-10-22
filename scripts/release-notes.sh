#!/usr/bin/env bash
# Release notes from conventional commits since the previous release tag (v*).
# feat -> New, fix -> Fixes, perf -> Improvements; chore/ci/docs/test/refactor/build/style/deps
# are internal and left out. A "!" after the type or a BREAKING CHANGE footer is flagged.
# Usage: scripts/release-notes.sh [--plain] [FROM_REF [TO_REF]]
#   --plain   store text (no headings or hashes) for Play and TestFlight
#   FROM_REF  defaults to the newest v* tag reachable from TO_REF (all history if none)
#   TO_REF    defaults to HEAD
set -euo pipefail
plain=0
if [[ "${1:-}" == --plain ]]; then plain=1; shift; fi
to="${2:-HEAD}"
from="${1:-$(git describe --tags --abbrev=0 --match 'v[0-9]*' "$to" 2>/dev/null || true)}"
range="${from:+$from..}$to"

# One "type|breaking|subject|hash" line per conventional commit, newest first.
breaking="$(git log --no-merges --format=%h -E --grep='^BREAKING[ -]CHANGE' "$range")"
pattern='^([a-z]+)(\([^)]*\))?(!)?: (.+)$'
commits=""
while IFS=$'\t' read -r hash subject; do
  [[ "$subject" =~ $pattern ]] || continue
  type="${BASH_REMATCH[1]}"
  text="${BASH_REMATCH[4]}"
  flag=0
  if [[ -n "${BASH_REMATCH[3]}" ]] || grep -qx "$hash" <<< "$breaking"; then flag=1; fi
  text="$(tr '[:lower:]' '[:upper:]' <<< "${text:0:1}")${text:1}"
  commits+="$type|$flag|${text//|//}|$hash"$'\n'
done < <(git log --no-merges --format='%h%x09%s' "$range")

section() { # section TYPE HEADING
  local lines
  lines="$(awk -F'|' -v t="$1" -v plain="$plain" '$1 == t {
    flag = ($2 == "1") ? " (breaking)" : ""
    if (plain) print "- " $3 flag; else print "- " $3 flag " (" $4 ")"
  }' <<< "$commits")"
  [[ -n "$lines" ]] || return 0
  if ((plain)); then printf '%s\n' "$lines"; else printf '### %s\n%s\n\n' "$2" "$lines"; fi
}

out="$(section feat New; section fix Fixes; section perf Improvements)"
if [[ -z "$out" ]]; then
  out='Bug fixes and improvements.'
fi
printf '%s\n' "$out"
