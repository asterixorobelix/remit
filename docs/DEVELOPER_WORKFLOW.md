# Solo developer workflow

## Start a project

Preview the rename, apply it, then verify everything builds. The initializer refuses to
run twice unless `--force` is explicit.

```bash
./setup.sh --name Focus --package dev.example.focus \
  --features auth,offline,backend --dry-run
./setup.sh --name Focus --package dev.example.focus \
  --features auth,offline,backend
make bootstrap           # lint/hook tools from the Brewfile, Git hooks, then make doctor
make app-checklist
make check
```

Capabilities live in `FEATURES` in `app.properties`: `minimal` (core UI and
preferences only), `auth`, `offline`, `backend`, `maps`, `billing`, and `firebase`.
Koin loads, and navigation exposes, only what is enabled. Disabled capabilities keep
their source and dependencies, so delete what the app will never use.

After editing `app.properties` by hand, run `scripts/sync-app-config.sh` to update the
iOS xcconfig. The pre-commit hook and CI check that the two stay in sync.

Keep [APP_CHECKLIST.md](../APP_CHECKLIST.md) in the project. It tracks what each real app
must finish itself, such as **Share app** in Settings → About, password reset, legal
links, and store IDs.

## Run locally

```bash
make backend-h2          # API on :8085 with in-memory H2, development mode
make run                 # or: PostgreSQL in Docker + API against it
make mobile              # build, install, and launch on a running emulator/device
make ios-run             # build, install, and launch on the booted (or first) simulator
make ios-app             # unsigned simulator build only; or run the iosApp scheme in Xcode
make ui                  # desktop component gallery (shared theme and components)
make ui-hot              # same, with Compose Hot Reload
```

Debug builds point to the local API: Android uses `10.0.2.2:8085`, iOS uses
`localhost:8085`. Settings → Developer tools (debug builds only) overrides the API URL
and resets local state. Swagger UI is at <http://localhost:8085/swagger>.
`make ios-run` builds unsigned, so a sign-in session is not kept in the Keychain; run the
iosApp scheme from Xcode with a team selected to test persisted sessions.

## Add a feature

```bash
./scripts/create-feature.sh Bookmarks --dry-run
make feature NAME=Bookmarks          # or ./scripts/create-feature.sh Bookmarks
```

The generator writes the state/event/effect contract, screen (with a back-navigating top
bar), ViewModel, use cases, repository interface, Koin module, ViewModel test, SQLDelight
table, the next `.sqm` migration, and strings. It then wires the feature in: the Koin
module in `appModules`, `AppRoute.Bookmarks`, its availability in `NavigationFeatures`,
its destination in `AppNavigation`, and `NavigationGraphTest`. The result compiles and
passes `make quick` immediately. Wiring inserts above `// create-feature:<name>` anchor
comments; keep them (a missing anchor is reported and left for you). `--no-wire` skips
wiring, and names that clash with existing declarations (`Tasks`, `Settings`) are refused.
Then:

1. Link to it with `navController.navigate(AppRoute.Bookmarks)` from Home, Settings, or a tab.
2. Implement the repository (use Tasks in `data/task` as the reference).
3. If optional, gate the module in `appModules` and the route in `NavigationFeatures` by capability.
4. Translate the strings and replace the placeholder UI.
5. Add backend routes, a service, and a Flyway `Vn__*.sql` if the feature syncs.

Tasks is the reference for data-backed features. Local reads are authoritative, and
writes are queued durably and synced in batches. Retries back off exponentially, deletes
are tombstones, and conflicts resolve last-write-wins on `updatedAt`.

## Quality loop

Iterate with the fast loop, then run the full gate before merging:

```bash
make quick               # only modules changed on this branch: backend test, detekt, ktlint;
                         #   mobile shared/desktop JVM tests, detekt, ktlint, Android compile,
                         #   migrations; architecture contracts
make test-watch          # re-run shared JVM tests (commonTest + desktopTest) on every save
make test-mobile-fast    # same tests once, without Android/iOS test variants
make check-template      # seconds: initializer, generator, config sync, resources, architecture
make check-backend       # tests, detekt, ktlint, coverage gate
make check-mobile        # tests, detekt, ktlint, Android Lint, Android, desktop, iOS framework,
                         #   migrations
make check               # all of the above (CI also runs the rows below)
make lint-repo           # actionlint, ShellCheck, typos, hadolint, SwiftLint, architecture
make format              # ktlint and SwiftLint auto-fixes
make coverage            # Kover HTML reports and totals for backend and mobile
```

Coverage is gated three ways: the backend's `koverVerify` (80% line, 70% branch), the mobile
`koverVerifyUnitTests` floor (70% of logic; `@Composable` code is covered by screenshots), and,
on pull requests, 80% of changed lines in both builds. detekt runs detekt's default rules plus
each build's overrides on every source set, tests included; ktlint's version is pinned in
`gradle/libs.versions.toml`.

Gradle configuration cache, build cache, and parallel execution are on in both
`backend/gradle.properties` and `mobile/gradle.properties`, so repeat runs mostly reuse
earlier work.

Pushes to `main` and pull requests run the same gates in CI, path-filtered per module;
documentation-only pushes skip CI. CI adds a Semgrep security scan, gitleaks, screenshot
verification, and on pull requests coverage of changed lines and an AI review. A nightly
workflow runs Android instrumentation tests (emulator or Firebase Test Lab) when mobile code
changed since its last successful run. Label a pull request `preview` for an installable
Android build. Screenshot baselines come from CI: after an intended UI change, run
`scripts/update-screenshots.sh` and commit the images.

## Releases

`./bump-version.sh patch --yes` (or `make release-patch`) updates `app.properties` and the
iOS xcconfig. It never commits or tags. Store delivery runs through the Mobile CD
workflow, which uploads first and only then commits, tags, and publishes. See
[RELEASING.md](RELEASING.md) for accounts, signing, and secrets, and
[BILLING_TESTING.md](BILLING_TESTING.md) for in-app purchases.

## Release health

Every API request carries an `X-Request-ID`. The backend accepts well-formed IDs (or
generates one), returns it, and includes it in every log line, so a client error can be
traced in server logs. The app emits a versioned release-health event at startup and
redacts common secret fields from non-fatal reports. Firebase Crashlytics/Analytics is
optional ([FIREBASE.md](FIREBASE.md)); without it, platform logging is used.
