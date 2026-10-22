# AI agent functionality guide

Source map and cross-stack contracts for coding agents. Start with root `AGENTS.md`,
then the scoped guide (`mobile/AGENTS.md`, `backend/AGENTS.md`) for the module you edit.

## System model

```text
app.properties ──┬─ Android BuildConfig / desktop system properties (read at build time)
                 ├─ iOS Config.xcconfig → Info.plist (mirrored by scripts/sync-app-config.sh)
                 ├─ Fastlane identity (package / bundle id)
                 └─ backend version (build-info.properties)

Compose UI → ViewModel → Use Case → Domain Repository
                                      ↓ implementation
                              SQLDelight + Ktor client
                                      ↓
                              Ktor routes → service
```

`app.properties` is authoritative for the app name, package, semantic and integer versions,
API endpoints, and capabilities. Never hand-edit the synced xcconfig keys; run
`scripts/sync-app-config.sh`. `--check` runs in CI, doctor, and pre-commit.

## Capability model

`FEATURES` lists the optional capabilities, `backend` and `firebase`; unknown values
(including `minimal`) are ignored. Capabilities control Koin composition and which
navigation routes exist (`navigation/NavigationFeatures.kt`). They do not remove source
files or Gradle dependencies.

| Capability | Working behavior | Main source |
|---|---|---|
| Core | Theme, preferences, onboarding, Home and Settings tabs, developer tools, gallery | `di/AppModule.kt` |
| `backend` | Force-update check via `GET /api/v1/config` | `presentation/app/AppViewModel.kt` |
| `firebase` | Android Crashlytics/Analytics when a real `google-services.json` exists | `androidApp/build.gradle.kts` |

`NavigationGraphTest` (desktopTest) resolves every reachable ViewModel for several
capability sets. Add new routes to it.

## UI and extension points

Implemented: onboarding (start destination resolved once in `AppViewModel`), typed
Compose Navigation with capability-gated routes, Material 3 theming with `Dimens`, a
placeholder Home tab (to be replaced by the app's main feature), Settings (theme, feedback,
rate the app, privacy/terms in an in-app web view, version), force update, the in-app
review prompt, and debug-only Developer tools (validated API override that applies
immediately, plus a reset of onboarding and local settings).

Honest defaults to replace per app, listed in `APP_CHECKLIST.md`: feedback (opens mail to
`support_email`) and the `app_store_id` and privacy/terms URL string resources. Do not
describe these as backend integrations.

**Share app is an explicit per-app reminder in `APP_CHECKLIST.md`**, not an implemented
feature.

## Initializer and generator contracts

- `setup.sh` rewrites `com.asterixorobelix.remit`, its path form, `Remit` (PascalCase type
  name), and `remit` (slug) in every text file outside `build/`, `.git/`, and similar
  directories. It moves package directories in every source set, including SQLDelight,
  renames `Remit*` paths, removes `.planning/`, syncs the xcconfig, and installs hooks.
  `scripts/test-initializer.sh` pins this behavior with a fixture. `make check-template`
  runs it.
- `scripts/create-feature.sh PascalCaseName` (`make feature NAME=...`) refuses overwrites
  and name clashes, and generates a sealed contract, screen, ViewModel with Channel effects,
  use cases, repository interface, Koin module, ViewModel test, SQLDelight table, the
  matching `.sqm` migration, and strings. It then wires the module, `AppRoute`,
  `NavigationFeatures`, the `AppNavigation` destination, and `NavigationGraphTest` above
  `// create-feature:<name>` anchors (`--no-wire` skips this). Keep the anchors.
  `scripts/test-feature-generator.sh` pins this against the real wiring files.

## Observability and configuration

Every mobile request gets a fresh `X-Request-ID`. The backend accepts well-formed IDs,
echoes them, and logs them via MDC in every line. Shared startup emits a release-health
event with the app version. Non-fatal error context redacts common
secret fields. Keep PII and credentials out of logs.

The backend defaults to production (no Swagger UI, no CORS origins) and refuses to start
with an unknown `APP_ENVIRONMENT`. `make backend`, `./gradlew run`, and docker-compose set
development.

## CI, delivery, and infrastructure

- `ci.yml` (pull requests and pushes to `main`; documentation-only pushes skip it):
  - **Template contracts**: initializer/generator contracts, then `scripts/lint-repo.sh`
    (actionlint, ShellCheck, typos, hadolint, SwiftLint, `scripts/check-architecture.sh`)
    with pinned tools from `scripts/ci/install-tools.sh`, and gitleaks on new commits.
  - **Security scan**: Semgrep (Community rules, or `semgrep ci` with `SEMGREP_APP_TOKEN`);
    pull requests report only findings they introduce.
  - **Backend**: tests (including `OpenApiContractTest`), detekt, ktlint, Kover coverage gate
    (80% line, 70% branch), plus a Docker image build and health
    check when the Dockerfile or build files change.
  - **Mobile** (Linux) and **iOS** (`macos-26`, waits for Mobile), and **Fastlane** (loads
    the Fastfile with the locked gems). Mobile details are in `mobile/AGENTS.md`.
  - Jobs are path-filtered; on `main` changes are measured from the last green run.
- `ai-review.yml`: PR-Agent through OpenRouter (model from `AI_REVIEW_MODEL`; settings and
  project rules in `.pr_agent.toml`). Suggestions are review threads; a "Possible security
  concern" label fails the check until a maintainer clears it.
- `preview.yml`: the `preview` label builds an installable Android debug APK for a pull
  request (and sends it to Firebase App Distribution when configured).
- `mobile-device-smoke.yml`: nightly Android instrumentation (emulator, or Firebase Test Lab
  with Robo when `FIREBASE_TEST_LAB_KEY` is set), skipped when nothing Android-relevant
  changed since the last successful run.
- `codeql.yml`: weekly Java/Kotlin analysis, only when code scanning is enabled.
- `mobile-cd.yml`: validates on `main`, builds the signed Android release, uploads iOS then
  Android, then commits the version bump, tags, and publishes with generated release notes
  (`scripts/release-notes.sh`). Uploads skip builds the store already has, so "Re-run failed
  jobs" is safe. `mobile-promote.yml` ships a tagged, tested build to wider tracks or App
  Store review without rebuilding. Secrets are in `docs/RELEASING.md`.
- `.github/actions/java-gradle`: JDK plus a per-job Gradle cache that only `main` saves.
- Runners come from the `LINUX_RUNNER` / `MACOS_RUNNER` repository variables (defaults
  `ubuntu-24.04` / `macos-26`), so another provider or a self-hosted runner is a setting.
- Actions and the PR-Agent image are pinned to commit SHAs or digests; Dependabot updates
  actions monthly. CLI tool versions live in `scripts/ci/install-tools.sh`.
- `scripts/protect-main.sh`: required checks, resolved review threads, code-owner review for
  `.github/CODEOWNERS` paths, auto-merge, and the main-only `release` environment whose
  secrets Mobile CD and Mobile Promote use (pull request workflows cannot read them).
- `bump-version.sh`: updates `app.properties` and the xcconfig only. It never commits or tags.
- `backend/Dockerfile` (production default, non-root, UTC) and root `docker-compose.yml`
  (the backend alone, in development mode). `/health` is liveness.

Do not weaken transactional release ordering.

## Agent tooling

- `scripts/agent-hook.sh` (wired in `.claude/settings.json` and `.codex/hooks.json`): blocks edits
  to generated files, credentials, lint baselines, and migrations main already has; formats and
  lints every edited file (ktlint, SwiftLint, ShellCheck, actionlint, typos); blocks skipping Git
  hooks, plain force pushes, force pushes to main, and admin merges. Details in `AGENTS.md`.
- `.claude/settings.json` allowlists the gate and read-only commands and denies reading
  credential files. `.mcp.json` adds the `mobile` MCP server (emulators and simulators).
- `make bootstrap` installs the hook and lint tools from the `Brewfile`; `make doctor` reports
  missing tools, a ktlint CLI that differs from the Gradle pin, and uninstalled Git hooks.
- Mechanical contracts fail fast instead of relying on review: `scripts/check-architecture.sh`,
  `NavigationGraphTest`, `OpenApiContractTest`, `verifySqlDelightMigration`, Kover floors, and
  the changed-lines coverage check.

## Validation matrix

While iterating, `make quick` runs the fast subset for whatever changed on the branch
(backend test, detekt, ktlint; mobile shared/desktop JVM tests, detekt, ktlint, Android
compile, migrations; architecture and template contracts). It does not replace the rows below.

| Change | Minimum validation |
|---|---|
| Backend route/service | `make check-backend` (tests, detekt, ktlint, Kover); route changes also update `openapi/documentation.yaml` (`OpenApiContractTest`) |
| Shared mobile/domain/UI | `make check-mobile` (tests, detekt + Compose rules, ktlint, Android Lint, builds) |
| Visible UI change | CI screenshot verification; then `scripts/update-screenshots.sh` and commit the images |
| New logic | Tests that cover at least 80% of changed lines (CI checks pull requests); `make coverage` shows the report |
| SQLDelight schema/migration | `:composeApp:verifySqlDelightMigration` (part of `check-mobile`) |
| iOS/Kotlin Native | `:composeApp:linkDebugFrameworkIosSimulatorArm64` |
| Swift, Xcode, xcconfig | `make ios-app`, SwiftLint (`make lint-repo`) |
| Navigation or capabilities | `NavigationGraphTest` (desktopTest, part of `test`) |
| Resources | `scripts/check-resources.sh` |
| `app.properties` | `scripts/sync-app-config.sh --check` |
| Initializer/generator | `make check-template` |
| Shell/workflow/Dockerfile changes | `make lint-repo` (actionlint, ShellCheck, hadolint, typos) and `git diff --check` |
| Release notes | Conventional-commit subjects (`make release-notes` previews them) |
| UI/runtime behavior | Install, launch, and exercise the affected flow on Android and iOS |

`make doctor` warns about the template package and example endpoints until the app is
initialized and configured. Required failures must be fixed.
