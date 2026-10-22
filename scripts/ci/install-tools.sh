#!/usr/bin/env bash
# Installs the pinned, checksum-verified linters and secret scanner that CI runs (Linux x86_64).
# Locally, install the same tools with Homebrew (scripts/lint-repo.sh prints the commands).
# To upgrade a tool, change its version and SHA-256 together.
# Usage: scripts/ci/install-tools.sh DEST_DIR   (then put DEST_DIR on PATH)
set -euo pipefail
dest="${1:?usage: scripts/ci/install-tools.sh DEST_DIR}"
if [[ "$(uname -s)-$(uname -m)" != Linux-x86_64 ]]; then
  printf 'error: these pins are for Linux x86_64; install the tools locally with Homebrew\n' >&2
  exit 2
fi
mkdir -p "$dest"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# fetch URL SHA256: downloads into $work and verifies the checksum; prints the file path.
fetch() {
  local file="$work/${1##*/}"
  curl -fsSL --retry 3 --retry-all-errors -o "$file" "$1"
  printf '%s  %s\n' "$2" "$file" | sha256sum --check --quiet >&2
  printf '%s' "$file"
}

SHELLCHECK=0.11.0
file="$(fetch "https://github.com/koalaman/shellcheck/releases/download/v$SHELLCHECK/shellcheck-v$SHELLCHECK.linux.x86_64.tar.xz" \
  8c3be12b05d5c177a04c29e3c78ce89ac86f1595681cab149b65b97c4e227198)"
tar -xJf "$file" -C "$dest" --strip-components=1 "shellcheck-v$SHELLCHECK/shellcheck"

ACTIONLINT=1.7.12
file="$(fetch "https://github.com/rhysd/actionlint/releases/download/v$ACTIONLINT/actionlint_${ACTIONLINT}_linux_amd64.tar.gz" \
  8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8)"
tar -xzf "$file" -C "$dest" actionlint

GITLEAKS=8.30.1
file="$(fetch "https://github.com/gitleaks/gitleaks/releases/download/v$GITLEAKS/gitleaks_${GITLEAKS}_linux_x64.tar.gz" \
  551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb)"
tar -xzf "$file" -C "$dest" gitleaks

HADOLINT=2.15.1
file="$(fetch "https://github.com/hadolint/hadolint/releases/download/v$HADOLINT/hadolint-linux-x86_64" \
  c7187db94eeeeca956519a6af171adc31453941a1e777961f6e680f697c8c507)"
install -m 0755 "$file" "$dest/hadolint"

# The static build needs no Swift runtime on the runner.
SWIFTLINT=0.65.1
file="$(fetch "https://github.com/realm/SwiftLint/releases/download/$SWIFTLINT/swiftlint_linux_amd64.zip" \
  caeed6f4a679c35539ffaf124f6c4ab4a8416917f7d8796279dc52b74026059d)"
unzip -q -o "$file" swiftlint-static -d "$work"
install -m 0755 "$work/swiftlint-static" "$dest/swiftlint"

TYPOS=1.50.3
file="$(fetch "https://github.com/crate-ci/typos/releases/download/v$TYPOS/typos-v$TYPOS-x86_64-unknown-linux-musl.tar.gz" \
  aca6b5d546307092b8d0a8e0a89dd80f9da51f2f7617c5e45c5607c1684ffbf2)"
tar -xzf "$file" -C "$dest" ./typos

for tool in shellcheck actionlint gitleaks hadolint swiftlint typos; do
  printf '%-11s %s\n' "$tool" "$("$dest/$tool" --version 2>&1 | grep -m1 -oE '[0-9]+\.[0-9]+\.[0-9]+')"
done
