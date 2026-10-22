#!/usr/bin/env bash
# Point Git at the project's .githooks directory (pre-commit checks).
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if ! git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  printf 'Skipped Git hooks: %s is not a Git repository (run git init, then ./setup-hooks.sh).\n' "$ROOT"
  exit 0
fi
chmod +x "$ROOT"/.githooks/*
git -C "$ROOT" config core.hooksPath .githooks
printf 'Git hooks installed (pre-commit). Bypass once with: git commit --no-verify\n'
