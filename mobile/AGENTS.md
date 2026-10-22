# Mobile Agent Guide

Scope: `mobile/**`. Read `../docs/AI_AGENT_GUIDE.md` for the cross-stack behavior.

## Targets and entry points

| Target | Entry point |
|---|---|
| Shared UI/domain/data | `composeApp/src/commonMain/kotlin/com/example/myproject` |
| Android app | `androidApp`, `MainActivity`, `MyProjectApplication` |
| iOS app | `iosApp/iosApp/iOSApp.swift`, shared `MainViewController.kt` |
| Desktop gallery | `composeApp/src/desktopMain/.../ComponentGallery.kt` |

Build Android with `:androidApp:*`; `composeApp` is the shared library, not the APK.

## Runtime composition

- `platform/AppConfig.kt` models capabilities sourced from `app.properties` through
  Android BuildConfig, iOS xcconfig/Info.plist, and desktop system properties.
- `di/AppModule.kt` `appModules(config, platformModule)` always loads observability,
  storage, network and core screens; it adds `backend`, `auth`, Tasks (`offline`), maps
  and billing modules only for their capability. Platform modules supply
  `SqlDriverFactory` and, only with `billing`, the platform `PremiumRepository`.
- `navigation/NavigationFeatures.kt` decides which routes exist; `AppNavigation.kt`
  registers and links a route only when available. `NavigationGraphTest` (desktopTest)
  starts Koin for several capability sets and resolves every reachable ViewModel; add
  new routes to its exhaustive `when`.
- `presentation/app/AppViewModel.kt` resolves the start destination once (off the main
  thread), owns theme, review prompt, session-expiry navigation, and the force-update
  gate (`GET /api/v1/config`, `backend` only; failures never block the app).
- `data/network/ApiHttpClient.kt` builds the only API client: per-request
  `X-Request-ID`, base URL resolved per request (debug override applies immediately),
  bearer tokens sent only to the API origin (never auth endpoints or other hosts),
  retries only for idempotent methods (`allowRetry()` opts a POST in). Refresh tokens
  rotate: the new pair is stored compare-and-set via `TokenStore`; the session is
  cleared only when `/auth/refresh` returns 401/403 (429/5xx/offline keep it).

## Functional reference implementations

- Auth: `data/auth`, `domain/auth`, `presentation/auth`, `presentation/profile`. Tokens
  live in Android Keystore-encrypted prefs / iOS Keychain (`IOSSecureTokenStore`).
  Sign-out calls `POST /api/v1/auth/logout`, clears stored and Ktor-cached tokens, then
  every `UserDataCleaner`. Profile supports display-name edit (`PUT /users/me`) and
  account deletion (`DELETE /users/me`, confirmation dialog). Validation lives in
  `ValidateLoginUseCase` / `ValidateRegistrationUseCase`.
- Offline Tasks: SQLDelight is authoritative for reads; mutations update locally and
  enqueue sync work under one write mutex. Sync sends only rows with queued changes in
  batches of at most 500, commits per batch removing only the operations it sent, and
  keeps rows edited during an in-flight sync. Failures back off exponentially; deletes
  are tombstones; titles are capped at 240 characters. Sync runs only when both
  `backend` and `auth` are enabled; otherwise Tasks are local-only.
- Billing: Play Billing 8 / StoreKit 2 behind `billing`. Pending purchases return
  `PurchaseResult.Pending` and are granted when they complete; StoreKit transactions
  are finished and `Transaction.updates` is observed.
- Firebase: requires `firebase` in FEATURES and a real `androidApp/google-services.json`
  (the checked-in placeholder is ignored). Shared code reaches Firebase by reflection;
  keep rules are in `androidApp/proguard-rules.pro`.
- Developer tools: debug-only Settings destination for a validated API URL override
  and a full local reset.

App-specific extension points (honest defaults, not fake data): Search indexes local
Tasks (`SearchRepository`), Notifications use `EmptyNotificationsRepository`, Feedback
opens the mail app to `support_email`, and `app_store_id`, `settings_privacy_url`,
`settings_terms_url` are placeholder string resources to replace per app.

## Adding features

Prefer `../scripts/create-feature.sh FeatureName` (`make feature NAME=...` from the root).
It generates the feature and wires its Koin module, route, `NavigationFeatures` entry,
destination, and `NavigationGraphTest` case above the `// create-feature:<name>` anchors;
keep those anchors. Then link to the route, translate the strings, implement the data
repository, gate the module and route by capability if optional, and replace the generated
placeholder test with behavioral ViewModel/use-case tests. Do not put business logic in ViewModels; use constructor-injected use cases and
Channel-backed effects.

For a shipped database, add the next `.sqm` migration; never edit only `.sq`. Refresh
the schema baseline with `generateCommonMainAppDatabaseSchema`, then verify it.
Queries can be added freely; table changes need a migration.

## Tests

- `commonTest`: use cases, ViewModels (Turbine + `Dispatchers.setMain`) and the Ktor
  client (`MockEngine`). Hand-written fakes live in `commonTest/.../testing/`; MockK is
  not available on Kotlin/Native.
- `desktopTest`: `TaskRepositoryImplTest` on in-memory SQLite, `NavigationGraphTest`, and the
  Roborazzi screenshot tests in `screenshots/ScreenshotTest.kt` (component gallery, Tasks with
  fake data, Login in an error state; light and dark). Plain `test` only composes the screens;
  `verifyRoborazziDesktop` compares them with the baselines in `composeApp/screenshots/desktop/`.
- `iosTest`: Keychain round trip (skips the round trip when the unhosted test binary has no
  keychain). `iosSimulatorArm64Test` must link `MapLibre.framework` from the Xcode SPM package:
  build the iOS app once, then pass
  `-PmapLibreFrameworkDir=<DerivedData>/Build/Products/Debug-iphonesimulator`.
- Keychain access needs a signed build; unsigned (`CODE_SIGNING_ALLOWED=NO`) simulator builds
  launch but cannot store a session.

## Static checks

- **detekt**: detekt's default rules plus the overrides in `detekt-mobile.yml`, on every
  hand-written source set of `composeApp` (common, Android, iOS, desktop, and their tests) and
  `androidApp`. The Compose rules plugin ("Compose" rule set) also applies: screens
  take a defaulted `viewModel` and a `modifier`, callbacks used in effects go through
  `rememberUpdatedState`, event callbacks are present tense (`onUpdateClick`), and custom
  modifiers use `Modifier.Node`.
- **ktlint** (`ktlintCheck`, `ktlintFormat`; settings in the root `.editorconfig`, version pinned
  in `gradle/libs.versions.toml`) on every source set and script; generated code under `build/`
  is excluded. Agents get the same check per file from the edit hook (`scripts/agent-hook.sh`).
- **Android Lint** (`:androidApp:lintDebug`, `:composeApp:lintDebug`): new issues fail,
  warnings included. `lint-baseline.xml` holds accepted findings only; never add to it to
  make a change pass. AGP 9 lints `androidMain`, not `commonMain`.
- **SwiftLint** (`iosApp/.swiftlint.yml`, run in strict mode) for the Swift shell.
- `../scripts/check-architecture.sh` and `../scripts/check-resources.sh` (contracts, strings).

## Coverage and screenshots

- `:composeApp:koverXmlReportUnitTests` writes `build/reports/kover/reportUnitTests.xml` from
  `desktopTest` and `testDebugUnitTest`. It measures logic (domain, data, ViewModels,
  navigation) and leaves out `@Composable` functions, the UI kit (`ui/components`, `ui/theme`,
  covered by screenshots), generated code, and platform glue. CI fails a pull request whose
  changed lines are below 80% covered, and `:composeApp:koverVerifyUnitTests` (in
  `make check-mobile` and CI) fails when total line coverage drops below the 70% floor. Raise the
  floor in `composeApp/build.gradle.kts` as coverage grows; never lower it to pass.
  `make coverage` writes the HTML report (`build/reports/kover/htmlUnitTests/index.html`).
- Screenshot baselines are recorded on Linux by CI (fonts render differently on macOS), with a
  0.1% pixel threshold. After an intended UI change, let CI fail, then run
  `../scripts/update-screenshots.sh`, review the images, and commit them. Diffs are in the
  `screenshot-diffs` artifact.

## Verification

```bash
./gradlew test detekt ktlintCheck :androidApp:lintDebug :composeApp:lintDebug \
  :androidApp:assembleDebug :androidApp:assembleRelease \
  :composeApp:compileKotlinDesktop \
  :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  :composeApp:verifySqlDelightMigration :composeApp:koverVerifyUnitTests
```

For UI/runtime changes, install the Android debug app and exercise the flow (debug
builds may use cleartext only to `10.0.2.2`/`localhost`). For iOS bridge or project
changes, run an unsigned `xcodebuild` simulator build. Resource changes must also pass
`../scripts/check-resources.sh`.
