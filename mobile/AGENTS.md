# Mobile Agent Guide

Scope: `mobile/**`. Read `../docs/AI_AGENT_GUIDE.md` for the cross-stack behavior.

## Targets and entry points

| Target | Entry point |
|---|---|
| Payments logic (KMP, no UI) | `shared/src/commonMain/kotlin/com/asterixorobelix/remit/shared` |
| Shared UI/domain/data | `composeApp/src/commonMain/kotlin/com/asterixorobelix/remit` |
| Android app | `androidApp`: `MainActivity`, `RemitApplication`, `AndroidAppConfig` (BuildConfig) |
| Baseline Profile | `baselineprofile` (`BaselineProfileGenerator`, `make baseline-profile`) |
| iOS app | `iosApp/iosApp/iOSApp.swift`, shared `MainViewController.kt` |
| Desktop gallery | `composeApp/src/desktopMain/.../ComponentGallery.kt` |

Build Android with `:androidApp:*`; `composeApp` is the shared library, not the APK. `shared`
holds the payments domain, validation, use cases, API client, and Firebase (expect/actual) with
no Compose dependency, so a SwiftUI or desktop app could use it as is.

Both KMP modules use AGP 9's `com.android.kotlin.multiplatform.library` plugin (plus
`com.android.lint`): one Android variant, no build types or BuildConfig. Per-build values (API URL,
environment, emulator host, analytics flags) are BuildConfig fields in `androidApp`, read by
`AndroidAppConfig` and handed to shared code by `startRemit(config)` (`Startup.android.kt`). Add a
per-build value there, not in `composeApp`. `gradle.properties` keeps AGP 9's defaults (built-in
Kotlin, the new DSL, R8 strict full mode); do not add the opt-out flags back.

## Payments (`shared` + `composeApp/presentation/payments`)

- Money is `Money(minorUnits: Long, currency)`; never `Double`. `DecimalAmount.parse` reads typed
  or JSON text exactly. `AmountInput` limits what the amount field accepts (digits and one `.` or
  `,`) but never truncates decimals: "1,000" in USD must reach validation, not become "1,00".
  `RecipientInput` caps the recipient field at `EmailAddress.MAX_LENGTH` (254, the longest address
  validation and the backend accept), ignoring a longer edit instead of cutting it.
  `PaymentValidator` must agree with the backend's: both run
  `../testdata/payment-validation-cases.json`; extend it when a rule changes.
- `SendPaymentUseCase` serializes sends and keeps one idempotency key per payment
  (`IdempotencyKeys`): a retry or double tap reuses it; `startNewPayment()` (after success) or an
  edited draft gets a new one. The POST opts in to `allowRetry(PAYMENT_RETRY_POLICY)` (one quick
  retry) with a 10 s timeout through `PaymentRequestConfigurer` (`di/PaymentsModule.kt`), which is
  safe only because of that key.
- The key is written ahead of the request to `PendingPaymentStore`, implemented by
  `SqlDelightPendingPaymentStore` (`PendingPayment.sq`, one row). After a restart
  `SendPaymentViewModel` restores it as an "Unfinished payment" the user can send again (same key)
  or discard. A server rejection or an idempotency conflict clears it.
- `KtorPaymentsApi` maps every outcome to `SendPaymentResult`; `PaymentRepositoryImpl` refreshes
  an expired Firebase ID token once under the same key. The app never writes to Firestore; the
  backend does, and History listens (`platformPaymentHistorySource`, a snapshot listener on
  Android; iOS and desktop fail clearly).
- Firebase: anonymous auth. Debug builds use the Firebase emulators when `FIREBASE_EMULATOR_HOST`
  is set in `local.properties` (`10.0.2.2` from the Android emulator; `make emulators` and
  `make backend-emulated` at the root). Without `google-services.json`, `startRemit` calls
  `startFirebaseForEmulators` (project `FirebaseSettings.EMULATOR_PROJECT_ID`, which must match the
  Makefile's `FIREBASE_PROJECT`); with neither, the Android actuals fall back to the "not set up"
  implementations instead of crashing. Their errors carry `FirebaseNotSetUpException`, which
  `PaymentRepositoryImpl` maps to `PaymentFailure.NOT_SET_UP` and `HistoryFailure.NOT_SET_UP`, so
  neither screen blames the connection; only `NETWORK` failures say "check your connection".
  History shows debug builds the setup fix (`showSetupHelp`), and `androidApp/build.gradle.kts` and
  `make doctor` warn when neither is configured.
- The Send screen shows "Already sent" only for a restored unfinished payment the server replays;
  a replay answering this tap's own automatic retry is a plain success.
- Test tags in `PaymentTestTags` are exposed as Android resource ids (`MainActivity`) for Appium;
  keep them stable.
- Kover: `shared` has its own 90% floor and is merged into `composeApp`'s report.

## Runtime composition

- `platform/AppConfig.kt` models capabilities sourced from `app.properties` through
  `androidApp`'s BuildConfig (`AndroidAppConfig`) and the iOS xcconfig/Info.plist (`IOSAppConfig`);
  tests use `FakeAppConfig`.
- `di/AppModule.kt` `appModules(config, platformModule)` always loads observability,
  storage, network and core screens; it adds the `backend` module (force-update check)
  only for its capability. Capabilities are `backend` and `firebase`; unknown `FEATURES`
  values (including `minimal`) are ignored. Platform modules supply `SqlDriverFactory`.
- `navigation/NavigationFeatures.kt` decides which routes exist; `AppNavigation.kt`
  registers and links a route only when available. `NavigationGraphTest` (desktopTest)
  starts Koin for several capability sets and resolves every reachable ViewModel; add
  new routes to its exhaustive `when`.
- `presentation/app/AppViewModel.kt` resolves the start destination once (onboarding,
  then Home), owns theme, review prompt, and the force-update gate
  (`GET /api/v1/config`, `backend` only; failures never block the app).
- Logging and crash reports: release builds log WARN and above (`AppConfig.minLogLevel`), and R8
  strips `Log.v`/`Log.d`/`Log.i` (`androidApp/proguard-rules.pro`). `AndroidCrashLogger` and
  `AndroidAnalytics` call Crashlytics and Analytics directly, only for a real Firebase project
  (never the emulators); collection is off in the manifest until they turn it on, and a build
  without `google-services.json` deactivates Analytics. `observabilityModule` wraps the
  platform crash logger in `RedactingCrashLogger`, which removes emails, bearer tokens, JWTs, API
  keys, and `name=value` secrets from messages, custom keys, and exception messages (causes
  included, stack trace kept). Always inject `CrashLogger`; never call Crashlytics directly.
- `data/network/ApiHttpClient.kt` builds the only API client: per-request
  `X-Request-ID`; callers pass absolute URLs from `ApiEndpoint.urlFor`, which resolves the base
  URL per request (the debug override, read from DataStore, applies immediately);
  retries only for idempotent methods (`allowRetry()` opts a POST in, optionally with its own
  `RetryPolicy`); a `Retry-After` longer than the policy's maximum delay ends retries. It sends no
  credentials; add authentication there when a feature needs it.

## App shell

- Tabs: Send (the `AppRoute.Home` route), History, and Settings, each with its own back stack.
- Settings: theme, feedback, rate the app, privacy/terms (in-app web view), version, and
  debug-only Developer tools (validated API URL override and a reset of onboarding and
  local settings).
- Onboarding, force update, and the in-app review prompt.
- Firebase: requires `firebase` in FEATURES and a real `androidApp/google-services.json`
  (the checked-in placeholder is ignored). The Crashlytics Gradle plugin is always applied
  (the always-linked SDK needs its build id) and uploads mapping files only for a real project.
- Settings: `DataStoreAppPreferences` (a Preferences DataStore; every read suspends). The platform
  module supplies the store, one per file per process; Android's migrates the SharedPreferences
  file it replaced.
- SQLDelight: `AppDatabase` is wired in `storageModule`. `PendingPayment.sq` (the payment in
  flight) is the only schema; it replaced the template's demo table.

App-specific extension points (honest defaults, not fake data): Feedback opens the mail
app to `support_email`, and `app_store_id`, `settings_privacy_url`, `settings_terms_url`
are placeholder string resources to replace per app.

## Android practices

Keep these when changing screens or the build; the README's "Android practices" lists them for
reviewers.

- State: screens collect with `collectAsStateWithLifecycle`. A ViewModel exposing a live source
  (a Firestore listener) uses `stateIn(viewModelScope, WhileSubscribed(5_000), ...)` so it stops
  when the screen is hidden (`PaymentHistoryViewModel`).
- Effects: one-off effects stay in a `Channel`, collected with `CollectEffects`
  (`presentation/common`), never a bare `LaunchedEffect { effects.collect {} }`: the composition
  outlives a stopped activity, so a bare collector handles (and loses) effects in the background.
  `CollectEffects` adds `repeatOnLifecycle(STARTED)` and `Dispatchers.Main.immediate`.
- Process death: what the user types goes in `SavedStateHandle` (`SendPaymentViewModel`);
  `koinViewModel()` supplies it.
- Startup: the splash screen stays until `AppUiState` is ready (start destination and saved
  theme), and `MainActivity` sets the system bar icons from the app's theme, not the system's.
- Release: R8 rules keep only code reached by reflection (no whole-library keeps).
  `data_extraction_rules.xml` keeps every file out of cloud backup and device transfer.
- Baseline Profile: regenerate with `make baseline-profile` (a connected API 33+ emulator) after
  changing startup or the main screens, and commit
  `androidApp/src/release/generated/baselineProfiles/`.

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
  client (`MockEngine`). It runs on the desktop JVM and as Android host tests
  (`testAndroidHostTest`). Hand-written fakes live in `commonTest/.../testing/`; MockK is
  not available on Kotlin/Native.
- `desktopTest`: `NavigationGraphTest`, `DataStoreAppPreferencesTest` (the real DataStore on a
  temporary file), and the Roborazzi screenshot tests in
  `screenshots/ScreenshotTest.kt` (component gallery, light and dark). Plain `test` only
  composes the screens; `verifyRoborazziDesktop` compares them with the baselines in
  `composeApp/screenshots/desktop/`.

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
- **Android Lint** (`:androidApp:lintDebug`, `:composeApp:lintAndroidMain`,
  `:shared:lintAndroidMain`): new issues fail, warnings included. `lint-baseline.xml` holds
  accepted findings only; never add to it to make a change pass. Lint checks `androidMain`, not
  `commonMain`.
- **SwiftLint** (`iosApp/.swiftlint.yml`, run in strict mode) for the Swift shell.
- `../scripts/check-architecture.sh` and `../scripts/check-resources.sh` (contracts, strings).
  The architecture check fails a file that declares a `*Screen(` composable without a `@Preview`.

## Previews

Every screen file ends with one private `@Preview` that renders the stateless content in every
UiState, side by side, through `ui/preview/ScreenStates` (`ScreenStates(states) { MyContent(it, onEvent = {}) }`).
Never preview a screen that calls `koinViewModel()`: split out the stateless part (as
`OnboardingScreen` does). Entrance animations start visible when `LocalInspectionMode.current` is
true (`ForceUpdateScreen`). `create-feature.sh` generates the preview for new features. The only
screen without one is `WebViewScreen`, an `expect` declaration around a platform view.

## Coverage and screenshots

- `:composeApp:koverXmlReportUnitTests` writes `build/reports/kover/reportUnitTests.xml` from
  `desktopTest` and `testAndroidHostTest`. It measures logic (domain, data, ViewModels,
  navigation) and leaves out `@Composable` functions, the UI kit (`ui/components`, `ui/theme`,
  covered by screenshots), generated code, and platform glue. CI fails a pull request whose
  changed lines are below 80% covered, and `:composeApp:koverVerifyUnitTests` (in
  `make check-mobile` and CI) fails when total line coverage drops below the 80% floor. Raise the
  floor in `composeApp/build.gradle.kts` as coverage grows; never lower it to pass.
  `make coverage` writes the HTML report (`build/reports/kover/htmlUnitTests/index.html`).
- Screenshot baselines are recorded on Linux by CI (fonts render differently on macOS), with a
  0.1% pixel threshold. After an intended UI change, let CI fail, then run
  `../scripts/update-screenshots.sh`, review the images, and commit them. Diffs are in the
  `screenshot-diffs` artifact.

## Verification

```bash
./gradlew test detekt ktlintCheck :androidApp:lintDebug :composeApp:lintAndroidMain \
  :shared:lintAndroidMain \
  :androidApp:assembleDebug :androidApp:assembleRelease \
  :composeApp:compileKotlinDesktop \
  :composeApp:linkDebugFrameworkIosSimulatorArm64 \
  :composeApp:verifySqlDelightMigration :composeApp:koverVerifyUnitTests
```

For UI/runtime changes, install the Android debug app and exercise the flow (debug
builds may use cleartext only to `10.0.2.2`/`localhost`). For iOS bridge or project
changes, run an unsigned `xcodebuild` simulator build. Resource changes must also pass
`../scripts/check-resources.sh`.
