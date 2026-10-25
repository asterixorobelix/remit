#!/usr/bin/env bash
# Starts the backend API (make backend, make backend-emulated), or says plainly why it does not need
# to or cannot: it is already running, or another program holds its port.
# Usage: scripts/backend.sh [--emulated PROJECT_ID]
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
usage='usage: scripts/backend.sh [--emulated PROJECT_ID]'
project=''
case "${1:-}" in
  '') ;;
  --emulated) project="${2:?$usage}" ;;
  *) echo "$usage" >&2; exit 2 ;;
esac
target="make backend${project:+-emulated}"

# The backend reads PORT (8085 unless set; the debug app expects 8085). 4400 is the Firebase CLI's
# emulator hub, which lists the running emulators.
port="${PORT:-8085}"
url="http://localhost:$port"
HUB=http://127.0.0.1:4400

# Commands to type are indented on their own lines with a "$ " prompt, apart from the prose.
command_line() { printf '       $ %s\n' "$*"; }

addresses() { printf '  API          %s\n  Swagger UI   %s/swagger\n' "$url" "$url"; }

# stop_then_start PID COMMAND: how to stop what holds the port, then start the backend.
stop_then_start() {
  if [[ "$2" == *"$ROOT/backend/"* ]]; then
    printf '  1. Stop it: press Ctrl-C in the terminal running it, or run\n'
    command_line kill "$1"
  elif [[ "$2" == *[Dd]ocker* ]]; then
    printf '  1. Stop it (it runs in Docker):\n'
    command_line docker compose down
  elif [[ -n "$1" ]]; then
    printf '  1. Stop it: press Ctrl-C in the terminal running it, or run\n'
    command_line kill "$1"
  else
    printf '  1. Stop the program on port %s.\n' "$port"
  fi
  printf '  2. Start the backend:\n'
  command_line "$target"
}

# env_value COMMAND NAME: NAME's value in a "ps eww" line (a process's arguments, then its environment).
env_value() { tr ' ' '\n' <<<"$1" | sed -n "s/^$2=//p" | tail -1; }

# payments COMMAND: what the backend in COMMAND uses for payments; empty when it was not started
# from this checkout (Docker, say), whose environment ps cannot show.
payments() {
  [[ "$1" == *"$ROOT/backend/"* ]] || return 0
  local project_id
  project_id="$(env_value "$1" FIREBASE_PROJECT_ID)"
  if [[ -n "$(env_value "$1" FIRESTORE_EMULATOR_HOST)" ]]; then echo "Firebase emulators, project $project_id"
  elif [[ -n "$project_id" ]]; then echo "Firebase project $project_id"
  else echo 'disabled (no FIREBASE_PROJECT_ID), so they answer 503'; fi
}

# The steps after this one in README "Run it": the debug app reaches the emulators only when
# FIREBASE_EMULATOR_HOST is compiled in (mobile/local.properties or the environment).
app_steps() {
  printf '\nNext, in another terminal, build and launch the debug app (README "Run it"):\n'
  if ! grep -Eq '^FIREBASE_EMULATOR_HOST=.+' mobile/local.properties 2>/dev/null &&
    [[ -z "${FIREBASE_EMULATOR_HOST:-}" ]]; then
    command_line "echo 'FIREBASE_EMULATOR_HOST=10.0.2.2' >> mobile/local.properties"
  fi
  command_line make mobile
}

# Something on the port already? lsof exits non-zero when nothing listens, which is the usual case.
pid="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
holder=''
[[ -z "$pid" ]] || holder="$(ps eww -o command= -p "$pid" 2>/dev/null || true)"
if status="$(curl -fsS -m 2 "$url/api/v1/status" 2>/dev/null)" && [[ "$status" == *'"api"'* ]]; then
  using="$(payments "$holder")"
  if [[ -z "$project" || "$using" == "Firebase emulators, project $project" ]]; then
    printf 'The backend is already running (pid %s); nothing to start.\n' "${pid:-unknown}"
    addresses
    [[ -z "$using" ]] || printf '  Payments     %s\n' "$using"
    [[ -z "$project" ]] || app_steps
    printf '\nTo restart the backend:\n'
    stop_then_start "$pid" "$holder"
    exit 0
  fi
  {
    if [[ -n "$using" ]]; then
      printf 'A backend is already running (pid %s), but not against the Firebase emulators for project %s.\n' \
        "${pid:-unknown}" "$project"
      addresses
      printf '  Payments     %s\n' "$using"
    else
      printf 'A backend is already running at %s (pid %s), but not from this checkout (Docker, say),\n' \
        "$url" "${pid:-unknown}"
      printf 'so it may not use the Firebase emulators for project %s.\n' "$project"
    fi
    stop_then_start "$pid" "$holder"
  } >&2
  exit 1
fi
if [[ -n "$pid" ]] || (: <"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
  {
    if [[ "$holder" == *"$ROOT/backend/"* ]]; then
      printf 'A backend holds port %s (pid %s) but does not answer at %s/api/v1/status;\n' "$port" "$pid" "$url"
      printf 'it may still be starting. If it does not answer soon, restart it:\n'
    elif [[ -n "$pid" ]]; then
      printf 'Port %s (the backend) is in use by %s (pid %s).\n' "$port" "$(ps -o comm= -p "$pid" | sed 's|.*/||')" "$pid"
    else
      printf 'Port %s (the backend) is in use by another program.\n' "$port"
    fi
    stop_then_start "$pid" "$holder"
  } >&2
  exit 1
fi

export APP_ENVIRONMENT=development
if [[ -n "$project" ]]; then
  export FIREBASE_PROJECT_ID="$project" FIRESTORE_EMULATOR_HOST=localhost:8080 FIREBASE_AUTH_EMULATOR_HOST=localhost:9099
  emulators="$(curl -fsS -m 2 "$HUB/emulators" 2>/dev/null || true)"
  if [[ "$emulators" != *'"auth"'* || "$emulators" != *'"firestore"'* ]]; then
    printf 'The Firebase emulators are not running, so payments fail until you start them in another terminal:\n' >&2
    command_line make emulators >&2
    printf '\n' >&2
  fi
  printf 'Starting the backend against the Firebase emulators (project %s). Keep this terminal open.\n' "$project"
  addresses
  app_steps
else
  printf 'Starting the backend in development mode. Keep this terminal open.\n'
  addresses
  if [[ -n "${FIREBASE_PROJECT_ID:-}" ]]; then printf '  Payments     Firebase project %s\n' "$FIREBASE_PROJECT_ID"
  else printf '  Payments     disabled (no FIREBASE_PROJECT_ID), so they answer 503\n'; fi
fi
printf '\n'
cd backend
exec ./gradlew run
