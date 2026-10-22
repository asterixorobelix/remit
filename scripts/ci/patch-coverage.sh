#!/usr/bin/env bash
# Fails when the lines a pull request adds or changes are less than PATCH_COVERAGE_MIN percent
# covered by tests (default 80). Lines missing from the coverage report, such as @Composable
# functions that Kover excludes, are not counted. The table goes to the job summary.
# Usage: scripts/ci/patch-coverage.sh BASE_REF REPORT_XML SRC_ROOT...
set -euo pipefail
(($# >= 3)) || { printf 'usage: %s BASE_REF REPORT_XML SRC_ROOT...\n' "$0" >&2; exit 2; }
base="$1" report="$2"
shift 2
[[ -f "$report" ]] || { printf 'error: coverage report %s not found\n' "$report" >&2; exit 1; }
min="${PATCH_COVERAGE_MIN:-80}"
markdown="$(mktemp)"
status=0
pipx run --spec diff-cover==10.6.0 diff-cover "$report" --compare-branch="$base" \
  --src-roots "$@" --fail-under="$min" --format "markdown:$markdown" || status=$?
{
  printf '### Coverage of changed lines: %s (minimum %s%%)\n\n' "${report%%/*}" "$min"
  sed 's/^#/###/' "$markdown"
} >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
((status == 0)) || printf '::error::Changed %s lines are below %s%% test coverage; add tests for the new logic.\n' "${report%%/*}" "$min"
exit "$status"
