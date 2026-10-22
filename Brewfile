# Local tools for the linters, Git hooks, and agent edit hooks: `make bootstrap` (brew bundle).
# CI installs pinned versions of the linters (scripts/ci/install-tools.sh) and Gradle pins
# ktlint (gradle/libs.versions.toml); scripts/lint-repo.sh and the agent hook note any drift.
# JDK 21, Android Studio (Android SDK), Xcode, and Docker are installed separately (make doctor).

brew "jq"          # agent hooks (scripts/agent-hook.sh) read tool calls as JSON
brew "ktlint"      # per-file Kotlin formatting after each agent edit
brew "swiftlint"   # Swift lint (make lint-repo) and agent edits
brew "shellcheck"  # shell scripts and Git hooks
brew "actionlint"  # GitHub Actions workflows
brew "typos-cli"   # spelling (code, docs, and agent edits)
brew "hadolint"    # backend Dockerfile
brew "gitleaks"    # secret scan in the pre-commit hook
brew "gh"          # pull requests, branch protection, and doctor's secret check
