#!/usr/bin/env bash
# Starts the Firebase Auth and Firestore emulators (make emulators), or says plainly why it does not
# need to or cannot: they are already running, or another program holds one of their ports.
# Usage: scripts/emulators.sh PROJECT_ID
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
project="${1:?usage: scripts/emulators.sh PROJECT_ID}"

# The ports firebase.json sets (backend-emulated and the debug app use the same ones); 4400 is the
# Firebase CLI's emulator hub, which lists the running emulators.
PORTS=(9099 8080 4000)
NAMES=(Auth Firestore 'Emulator UI')
HUB=http://127.0.0.1:4400

command -v firebase >/dev/null 2>&1 || { echo 'firebase not found: npm install -g firebase-tools' >&2; exit 1; }

# Commands to type are indented on their own lines with a "$ " prompt, apart from the prose.
command_line() { printf '       $ %s\n' "$*"; }

addresses() {
  printf '  Auth         localhost:9099\n  Firestore    localhost:8080\n  Emulator UI  http://localhost:4000\n'
}

next_steps() {
  printf '\nNext, start the backend in another terminal (README "Run it"):\n'
  command_line make backend-emulated
}

# stop_then_start WHAT [PID...]: how to stop the programs in the way, then start the emulators.
stop_then_start() {
  local what="$1"
  shift
  if (($#)); then
    printf '  1. Stop %s: press Ctrl-C in the terminal running them, or run\n' "$what"
    command_line kill "$@"
  else
    printf '  1. Stop %s.\n' "$what"
  fi
  printf '  2. Start the emulators:\n'
  command_line make emulators
}

# Already running? The hub answers with the CLI's pid; its command line names the project.
if emulators="$(curl -fsS -m 2 "$HUB/emulators" 2>/dev/null)"; then
  pid="$(curl -fsS -m 2 "$HUB/" 2>/dev/null | sed -n 's/.*"pid": *\([0-9]*\).*/\1/p' | head -1)"
  running="$(ps -o command= -p "${pid:-0}" 2>/dev/null | sed -n 's/.*--project[= ]\([^ ]*\).*/\1/p')"
  if [[ "$emulators" == *'"auth"'* && "$emulators" == *'"firestore"'* && "${running:-$project}" == "$project" ]]; then
    printf 'The Firebase emulators are already running (project %s, pid %s); nothing to start.\n' \
      "$project" "${pid:-unknown}"
    addresses
    next_steps
    printf '\nTo restart the emulators:\n'
    stop_then_start them ${pid:+"$pid"}
    exit 0
  fi
  {
    printf 'Firebase emulators for project %s are already running (pid %s), but backend-emulated\n' \
      "${running:-unknown}" "${pid:-unknown}"
    printf 'and the debug app need Auth and Firestore for project %s.\n' "$project"
    stop_then_start them ${pid:+"$pid"}
  } >&2
  exit 1
fi

# Not running: make sure nothing else holds the ports, since the CLI only says "port taken".
taken=0
leftover_firestore=0
pids=()
for i in "${!PORTS[@]}"; do
  port="${PORTS[i]}"
  # lsof exits non-zero when nothing listens, which is the usual case.
  owner="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -Fpc 2>/dev/null |
    awk '/^p/ {pid = substr($0, 2)} /^c/ {print pid, substr($0, 2); exit}' || true)"
  if [[ -n "$owner" ]]; then
    printf 'Port %s (%s) is in use by %s (pid %s).\n' "$port" "${NAMES[i]}" "${owner#* }" "${owner%% *}" >&2
    [[ " ${pids[*]-} " == *" ${owner%% *} "* ]] || pids+=("${owner%% *}")
    [[ "$port" != 8080 || "${owner#* }" != java ]] || leftover_firestore=1
    taken=1
  elif (: <"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
    printf 'Port %s (%s) is in use by another program.\n' "$port" "${NAMES[i]}" >&2
    taken=1
  fi
done
if ((taken)); then
  {
    ((!leftover_firestore)) ||
      printf 'The java process on 8080 is most likely a Firestore emulator left over from an earlier run.\n'
    stop_then_start 'those programs' ${pids[@]+"${pids[@]}"}
  } >&2
  exit 1
fi

printf 'Starting the Firebase emulators for project %s. Keep this terminal open.\n' "$project"
addresses
next_steps
printf '\n'
exec firebase emulators:start --only auth,firestore --project "$project"
