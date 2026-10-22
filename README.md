# Kotlin Full-Stack App Template

A starting point for shipping an Android + iOS app with a Kotlin backend: Compose
Multiplatform client (plus a desktop component gallery), Ktor API, and CI/CD that goes
all the way to the stores. The reference features work end to end rather than being
mockups: registration and login with rotating refresh tokens, account deletion, and an
offline-first Tasks list that syncs with the server.

Finish the app-specific work in [APP_CHECKLIST.md](APP_CHECKLIST.md) before shipping
(Share app, password reset, legal links, store IDs, and more).

## What's included

| Area | Implementation |
|---|---|
| Mobile | Compose Multiplatform (Android, iOS, desktop gallery), Material 3, Compose Navigation, Koin, StateFlow + Channel effects |
| Data | Ktor client with bearer refresh, SQLDelight cache with durable sync queue, kotlinx.serialization/datetime |
| Backend | Ktor, JWT with refresh rotation and reuse detection, Exposed, Flyway, PostgreSQL (H2 for dev/tests), Swagger UI |
| Capabilities | `auth`, `offline`, `backend`, `maps`, `billing`, `firebase` toggled in `app.properties` |
| Quality | Unit tests, detekt (with Compose rules), ktlint, Android Lint, SwiftLint, screenshot tests, Kover coverage gates plus coverage of changed lines, an OpenAPI contract test, architecture-contract checks, Semgrep and gitleaks, pre-commit hook |
| Agents | `AGENTS.md`/`CLAUDE.md` guides, edit and shell hooks for Claude Code and Codex (per-file lint feedback; blocks on generated files, credentials, applied migrations, `--no-verify`, and force pushes), a permission allowlist, and a mobile MCP server for emulators and simulators |
| Review | AI review on pull requests through OpenRouter (any model), CODEOWNERS for sensitive paths, auto-merge once checks pass and review threads are resolved |
| Delivery | Path-filtered GitHub Actions CI tuned for low minutes, preview builds for labelled pull requests, nightly device smoke tests, transactional Mobile CD, and Mobile Promote to ship tested builds without rebuilding |

## Quick start

Requirements: JDK 21+, Android SDK, Xcode (for iOS), optionally Docker for PostgreSQL.

```bash
# 1. Create your repo from this template, clone it, then rename the project.
./setup.sh --name Focus --package dev.example.focus --features auth,offline,backend --dry-run
./setup.sh --name Focus --package dev.example.focus --features auth,offline,backend

# 2. Install the lint/hook tools (Homebrew) and Git hooks, check the toolchain, run every gate.
make bootstrap         # brew bundle + setup-hooks + doctor
make check

# 3. Run it.
make backend-h2        # API on http://localhost:8085 with in-memory H2 (or: make run for PostgreSQL)
make mobile            # install the Android debug app (talks to 10.0.2.2:8085)
make ui                # desktop component gallery for fast UI iteration
open mobile/iosApp/iosApp.xcodeproj   # iOS: run the iosApp scheme on a simulator

# 4. Before the first release: per-app work, GitHub secrets, and store setup.
make app-checklist     # make doctor also lists release secrets missing on GitHub
```

`setup.sh` rewrites the package, the PascalCase type name (`FocusApplication`,
`FocusUITests`), and the lowercase slug (Gradle root, database, JWT issuer) across
Kotlin, Gradle, Swift, Xcode, Fastlane, ProGuard, Docker, and docs. It moves every
source set (including SQLDelight), then removes the template's maintainer notes.
Run it once. `--force` re-runs a package change on an initialized tree.

## Everyday commands

`make help` lists everything. The ones you'll use most:

| Command | Does |
|---|---|
| `make quick` | Fast checks for modules changed on this branch (inner loop) |
| `make check` | All CI gates: template contracts, backend, mobile (Android, desktop, iOS, migrations) |
| `make check-backend` / `make check-mobile` | One side only |
| `make backend-h2` / `make run` | API with H2 / with Docker PostgreSQL |
| `make mobile` / `make ios-run` / `make ui` | Install and launch on Android / iOS simulator / desktop gallery |
| `make test-watch` | Re-run shared JVM tests on every save |
| `make feature NAME=Bookmarks` | Scaffold a feature (contract, screen, ViewModel, use cases, repository, Koin, test, table) and wire it into DI, navigation, and `NavigationGraphTest` |
| `make release-patch` | Bump the version in `app.properties` and the iOS xcconfig (no commit) |

## Configuration

`app.properties` is the single source for app name, package, version, API endpoints,
capabilities, and store product IDs. Android and desktop read it at build time. The
iOS xcconfig mirrors it: `scripts/sync-app-config.sh` writes the xcconfig, and
`--check` fails CI if it drifts. Release builds on both platforms use
`API_BASE_URL_RELEASE`; debug builds use the local backend.

The backend is configured with environment variables. It defaults to production and
refuses to start there with unsafe settings. See [backend/README.md](backend/README.md).

## Project layout

| Path | Contents |
|---|---|
| `mobile/composeApp` | Shared UI, domain, data, platform actuals, SQLDelight schema, desktop gallery |
| `mobile/androidApp` / `mobile/iosApp` | Android application module / Xcode project |
| `mobile/fastlane` | Store upload lanes (read identity from `app.properties`) |
| `backend` | Ktor API, Flyway migrations, Dockerfile |
| `scripts` | Doctor, initializer tests, config sync, resource checks, feature generator, branch protection |
| `.github/workflows` | CI, device smoke, CodeQL, Mobile CD |

## Documentation

| Doc | For |
|---|---|
| [docs/DEVELOPER_WORKFLOW.md](docs/DEVELOPER_WORKFLOW.md) | Day-to-day setup, iteration, features, releases |
| [docs/RELEASING.md](docs/RELEASING.md) | Store accounts, signing, GitHub secrets, Mobile CD |
| [docs/FIREBASE.md](docs/FIREBASE.md) | Optional Crashlytics/Analytics |
| [docs/BILLING_TESTING.md](docs/BILLING_TESTING.md) | Play Billing and StoreKit testing |
| [APP_CHECKLIST.md](APP_CHECKLIST.md) / [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) | Per-app work and store submission |
| [TROUBLESHOOTING.md](TROUBLESHOOTING.md) | Common build and runtime problems |
| [AGENTS.md](AGENTS.md), [docs/AI_AGENT_GUIDE.md](docs/AI_AGENT_GUIDE.md) | Contracts and source map for coding agents |
| [backend/README.md](backend/README.md), [mobile/AGENTS.md](mobile/AGENTS.md) | Module details |

## Stack

Versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml), shared by
both builds and watched by Dependabot. Kotlin 2.3, Compose Multiplatform 1.10, AGP 9,
Ktor 3.3, Koin 4.1, SQLDelight 2.3, Exposed 1.5, Flyway 13. Android minSdk 24 and
targetSdk 36; iOS 18.2+.

## License

The template has no license file. Add one that fits your app before publishing it.
