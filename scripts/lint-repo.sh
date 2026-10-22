#!/usr/bin/env bash
# Repository linters that need no Gradle: workflows, shell scripts, spelling, the Dockerfile,
# Swift, and the AGENTS.md architecture contracts. CI installs pinned tools
# (scripts/ci/install-tools.sh) and passes --strict, which fails on a missing tool; locally a
# missing tool is skipped with an install hint.
# Usage: scripts/lint-repo.sh [--strict]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
strict=0
[[ "${1:-}" != --strict ]] || strict=1
status=0

# pinned TOOL: the version CI installs (scripts/ci/install-tools.sh).
pinned() {
  local var
  var="$(tr '[:lower:]' '[:upper:]' <<< "$1")"
  awk -F= -v v="$var" '$1 == v {print $2; exit}' scripts/ci/install-tools.sh
}

# run TOOL INSTALL_HINT COMMAND...: runs COMMAND when TOOL is available.
run() {
  local tool="$1" hint="$2" local_version want
  shift 2
  if ! command -v "$tool" >/dev/null 2>&1; then
    if ((strict)); then
      printf 'error: %s is not installed\n' "$tool" >&2
      status=1
    else
      printf '[skip] %-10s not installed (%s)\n' "$tool" "$hint"
    fi
    return 0
  fi
  printf '==> %s\n' "$tool"
  # Newer and older releases add or drop rules, so a local pass may still fail in CI.
  local_version="$("$tool" --version 2>&1 | grep -m1 -oE '[0-9]+\.[0-9]+\.[0-9]+' || true)"
  want="$(pinned "$tool")"
  if [[ -n "$want" && -n "$local_version" && "$local_version" != "$want" ]]; then
    printf '    note: local %s %s, CI pins %s (results can differ)\n' "$tool" "$local_version" "$want"
  fi
  "$@" || status=1
}

run actionlint 'brew install actionlint' actionlint -color
run shellcheck 'brew install shellcheck' bash -c "git ls-files -z '*.sh' .githooks | xargs -0 shellcheck"
run typos 'brew install typos-cli' typos
run hadolint 'brew install hadolint' hadolint backend/Dockerfile
run swiftlint 'brew install swiftlint' bash -c 'cd mobile/iosApp && swiftlint lint --strict --quiet'
printf '==> architecture contracts\n'
scripts/check-architecture.sh || status=1

((status == 0)) && printf 'Repository lint passed.\n'
exit "$status"
