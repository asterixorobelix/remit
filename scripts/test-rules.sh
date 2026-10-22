#!/usr/bin/env bash
# Runs the Firestore security rules tests (firebase/rules-tests) against the Firestore emulator.
# Needs the Firebase CLI and Node 22+. Usage: scripts/test-rules.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

command -v firebase >/dev/null 2>&1 || { echo 'firebase not found: npm install -g firebase-tools' >&2; exit 1; }
node_bin="$(command -v node)" || { echo 'node not found: install Node 22+' >&2; exit 1; }
[[ -d firebase/rules-tests/node_modules ]] || npm --prefix firebase/rules-tests ci --ignore-scripts

# An absolute node path, because the standalone Firebase CLI puts its own embedded node first on
# PATH inside emulators:exec. A demo- project id is emulator-only and never reaches a real project.
exec firebase emulators:exec --only firestore --project demo-remit-rules \
  "$node_bin --test firebase/rules-tests/firestore.rules.test.js"
