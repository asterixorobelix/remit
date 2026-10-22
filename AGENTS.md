# Project Setup Template — Agent Entry Point

A reusable full-stack template for shipping Android and iOS apps: Compose
Multiplatform/KMP client, Ktor server, SQLDelight storage, Koin DI, Fastlane, and
GitHub Actions.

## Starting a new app from this template

A copy of this template is not ready to ship until the steps below are done. When the user
starts a new app or asks what is left before launch, walk them through these in order and
never mark an item done without implementing and verifying it.

1. **Initialize the identity.** If `make doctor` warns `template not initialized`, run
   `./setup.sh --name <Name> --package <reverse.dns> --features <list> --dry-run`, then
   without `--dry-run`. It renames the package, types, bundle IDs, and slugs across Kotlin,
   Swift, Gradle, Xcode, Fastlane, and Docker. Never rename these by hand.
2. **Work through [APP_CHECKLIST.md](APP_CHECKLIST.md)** (`make app-checklist`). It covers
   features each app must add (Share app), defaults to replace (API
   endpoints, support email, store IDs, privacy/terms URLs, branding, copy), permissions
   and privacy manifests, backend environment variables, and GitHub/store delivery
   (secrets, `RELEASE_TOKEN`, `CODEOWNERS`, tester groups). These are reminders, not
   implemented features.
3. **Re-run `make doctor`.** It flags an uninitialized package, example endpoints, and
   Mobile CD secrets missing on GitHub. Then `make check`.

The CI/CD workflows, Fastlane, Docker image, and smoke tests need no manual edits for a new
app: they read identity from `app.properties`, and `setup.sh` renames the rest.

## Read before changing code

- [AI functionality guide](docs/AI_AGENT_GUIDE.md): capabilities, cross-stack
  contracts (remote config, request IDs), source locations, and the validation matrix.
- `mobile/AGENTS.md` and `backend/AGENTS.md`: scoped rules for those directories.
- [Developer workflow](docs/DEVELOPER_WORKFLOW.md) and [Releasing](docs/RELEASING.md).
- `app.properties`: the source of truth for identity, versions, endpoints, and
  capabilities. `APP_CHECKLIST.md` lists per-app work (see above).
- `.github/workflows` read identity from `app.properties`; never hardcode a package or app
  name there. Keep actions pinned to commit SHAs, keep path filters and `timeout-minutes`,
  and avoid adding macOS jobs (10x minutes) where Linux works.

The Android app module is `:androidApp`; `:composeApp` is the shared Compose library and
`:shared` the payments logic with no UI. Payments span the app, the backend, Firestore rules,
and `testdata/`; read "Payments contract" in [docs/AI_AGENT_GUIDE.md](docs/AI_AGENT_GUIDE.md)
before changing any of them. `e2e/` (Appium) and `perf/` (JMeter) need the local stack
(`make emulators`, `make backend-emulated`).

## Working through pull requests

Pull requests from agents merge on their own once everything below is green, so every check
is the review. Do not try to get around one.

1. Branch from `main`, keep one concern per pull request, and use conventional-commit
   titles (`feat:`, `fix:`, ...). They become the release notes.
2. Before pushing, run `make quick` and `make lint-repo`, then the validation-matrix rows
   for what you changed (`docs/AI_AGENT_GUIDE.md`). `make format` fixes Kotlin and Swift style.
3. Fill in the pull request template, including the commands you ran and their results.
4. CI must pass. Changed lines need tests: CI fails a pull request whose changed lines are
   below 80% covered (`@Composable` functions are excluded). If screenshot tests fail on an
   intended UI change, run `scripts/update-screenshots.sh`, review the images, and commit them.
5. Address every AI review suggestion: apply it, or reply why not, then resolve the thread.
   A "Possible security concern" label needs a maintainer.
6. Enable auto-merge (`gh pr merge --auto --squash`). Changes to `.github/CODEOWNERS` paths
   (workflows, Fastlane, database schemas, backend configuration, permissions, these
   instructions) wait for the owner's approval.
7. Never disable a lint rule, test, or check, add a lint baseline entry, lower a coverage
   threshold, or edit CI to make a pull request pass; fix the code, or explain in the pull
   request why a rule is wrong.
8. Label the pull request `preview` for an installable Android build.

## Agent guardrails

`scripts/agent-hook.sh` runs on every edit and shell command (Claude Code: `.claude/settings.json`;
Codex: `.codex/hooks.json`). When it reports a problem, fix it; never work around it.

- **Before an edit** it blocks generated files (`build/`, `generated/`), credentials and
  machine-local files (`.env`, `keystore.properties`, `local.properties`, signing keys,
  `google-services.json`), lint baselines, and migrations main already has (SQLDelight `.sqm`,
  schema `.db`).
- **After an edit** it formats and checks the file with whichever tools are installed
  (`make bootstrap`): ktlint for Kotlin, SwiftLint for Swift, ShellCheck for scripts and Git
  hooks, actionlint for workflows, and typos for everything.
- **Before a shell command** it blocks skipping Git hooks (`--no-verify`), plain force pushes
  (use `--force-with-lease` on your own branch), any force push to main, and admin merges
  (`gh pr merge --admin`).

Claude Code also gets a permission allowlist for the read-only and gate commands (`make quick`,
`make check*`, `./gradlew`, `git diff`, ...), is denied reading credential files, and loads the
`mobile` MCP server (`.mcp.json`) for driving Android emulators and iOS simulators. These files,
the hook, and every lint configuration are listed in `.github/CODEOWNERS`, so changing them
needs the owner's approval.

## Common commands

```bash
./setup.sh --name Focus --package dev.example.focus --features backend --dry-run
make doctor
make quick                 # inner loop: fast checks for modules changed on this branch
make check                 # template contracts + backend gate + mobile gate
make check-template        # fast: initializer, generator, config sync, resources, architecture
make lint                  # every linter: repo (lint-repo), backend, and mobile
make format                # ktlint and SwiftLint auto-fixes
make coverage              # Kover HTML reports and totals (backend and mobile)
make bootstrap             # install lint/hook tools (Brewfile) and Git hooks, then doctor
make release-notes         # notes from conventional commits since the last tag
make backend               # API on :8085 in development mode
make mobile / make ios-run # build, install, and launch on Android / iOS simulator
make ui                    # desktop component gallery
make feature NAME=Bookmarks  # scaffold and wire a feature (scripts/create-feature.sh)
```

Module gates (what `make check` runs):

```bash
cd backend && ./gradlew test detekt ktlintCheck koverVerify
cd mobile && ./gradlew test detekt ktlintCheck :androidApp:lintDebug :composeApp:lintDebug \
  :androidApp:assembleDebug :composeApp:compileKotlinDesktop \
  :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  :composeApp:verifySqlDelightMigration :composeApp:koverVerifyUnitTests
```

Static analysis and coverage, per build:

| Tool | Backend | Mobile |
|---|---|---|
| detekt 1.23 | defaults + `backend/detekt.yml`, main and test | defaults + `mobile/detekt-mobile.yml` with Compose rules, every source set (tests included) in `composeApp` and `androidApp` |
| ktlint (version pinned in `gradle/libs.versions.toml`) | all sources and scripts | all sources and scripts |
| Kover | `koverVerify`: 80% line, 70% branch | `koverVerifyUnitTests`: 80% line floor on logic, including `shared` (`@Composable` and the UI kit excluded); `shared` alone 90% |
| Changed lines (CI, pull requests) | 80% (diff-cover) | 80% (diff-cover) |
| Other | `OpenApiContractTest` (routes match the OpenAPI spec) | Android Lint, SwiftLint, Roborazzi screenshots |

## Non-negotiable implementation contracts

- Clean Architecture: presentation depends on use cases, use cases on domain repository
  interfaces, and the domain has no framework dependencies.
- Koin: `factoryOf` for use cases, `singleOf` for repositories, `viewModelOf` for ViewModels.
  Register optional modules only for their capability, and expose their routes through
  `NavigationFeatures`.
- UI: a sealed `UiState` with Loading/Content/Error/Empty, immutable public StateFlow,
  events up and state down, and a Channel-backed effect Flow for one-time actions. No
  literal user-facing strings, colors, or dimensions: use resources, `MaterialTheme`, and `Dimens`.
- Use Ktor, kotlinx.serialization, SQLDelight, kotlinx-datetime, Compose Navigation,
  Material 3, Compose resources, and accessible content descriptions.
- Mobile database changes require migrations: SQLDelight `.sqm` plus an updated schema
  baseline after the database has shipped. Never edit an applied migration.
- Never add Hilt/Dagger, Voyager, LiveData, Retrofit, Gson, or a public MutableStateFlow.
  `scripts/check-architecture.sh` enforces these and the layer, Koin, and UI-literal rules.
- Change identity only through `setup.sh`. Change versions through `bump-version.sh`,
  and endpoints in `app.properties` followed by `scripts/sync-app-config.sh`.
- Never let version scripts or failed release uploads create commits or tags.
- Add ViewModel and use-case tests. Use `make quick` while iterating, then run the relevant
  gates from the validation matrix before declaring work complete.
- Keep the `// create-feature:<name>` anchor comments; the feature generator wires new
  features in above them.
