#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILE="$ROOT/app.properties"
CURRENT="$(awk -F= '$1=="VERSION_NAME" {print $2}' "$FILE")"
CODE="$(awk -F= '$1=="VERSION_CODE" {print $2}' "$FILE")"
[[ $# -gt 0 ]] || { printf '%s (%s)\n' "$CURRENT" "$CODE"; exit 0; }
IFS=. read -r major minor patch <<< "$CURRENT"
case "$1" in
  patch) ((patch+=1)) ;;
  minor) ((minor+=1)); patch=0 ;;
  major) ((major+=1)); minor=0; patch=0 ;;
  *) printf 'Usage: %s [patch|minor|major]\n' "$0" >&2; exit 2 ;;
esac
# VERSION_CODE packs MAJOR.MINOR.PATCH as MMmmpp, so minor and patch must stay below 100.
((minor < 100 && patch < 100)) || { printf 'error: minor and patch must be below 100 for VERSION_CODE\n' >&2; exit 1; }
NEW="$major.$minor.$patch"
NEW_CODE=$((major * 10000 + minor * 100 + patch))
if [[ "${2:-}" != --yes ]]; then
  read -r -p "Bump $CURRENT ($CODE) to $NEW ($NEW_CODE)? [y/N] " answer
  [[ "$answer" =~ ^[Yy]$ ]] || exit 0
fi
perl -pi -e "s/^VERSION_NAME=.*/VERSION_NAME=$NEW/; s/^VERSION_CODE=.*/VERSION_CODE=$NEW_CODE/" "$FILE"
"$ROOT/scripts/sync-app-config.sh" >/dev/null
printf 'Version is now %s (%s). No commit or tag was created.\n' "$NEW" "$NEW_CODE"
