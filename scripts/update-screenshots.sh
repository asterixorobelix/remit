#!/usr/bin/env bash
# Screenshot baselines are recorded on Linux (CI's renderer), so nobody records them locally.
# When the Mobile job's screenshot verification fails, or no baselines exist yet, it records
# fresh images and uploads them as the "screenshots" artifact. This copies that artifact from
# the latest CI run of the current branch into the baseline directory; review the changed
# images (GitHub shows image diffs), then commit and push.
# Usage: scripts/update-screenshots.sh [RUN_ID]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
BASELINES=mobile/composeApp/screenshots/desktop
command -v gh >/dev/null || { printf 'error: install the GitHub CLI (https://cli.github.com)\n' >&2; exit 1; }

run="${1:-}"
if [[ -z "$run" ]]; then
  branch="$(git rev-parse --abbrev-ref HEAD)"
  run="$(gh run list --workflow CI --branch "$branch" --limit 1 --json databaseId --jq '.[0].databaseId // empty')"
  [[ -n "$run" ]] || { printf 'error: no CI run found for branch %s\n' "$branch" >&2; exit 1; }
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
gh run download "$run" --name screenshots --dir "$work" ||
  { printf 'error: run %s has no "screenshots" artifact (screenshots may already match)\n' "$run" >&2; exit 1; }
mkdir -p "$BASELINES"
find "$work" -name '*.png' ! -name '*_compare.png' ! -name '*_actual.png' -exec cp {} "$BASELINES"/ \;
git status --short -- "$BASELINES"
printf 'Copied baselines from CI run %s. Review the images, then commit them.\n' "$run"
