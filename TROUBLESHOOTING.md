# Troubleshooting

Start with `make doctor`; it checks the JDK, Android SDK, Xcode, Docker, wrappers, and
whether the iOS xcconfig matches `app.properties`.

## Setup

| Symptom | Fix |
|---|---|
| `setup.sh`: `already initialized as ...` | The tree was renamed already. Use `--force` only to change the package again; class and display names stay as they are. |
| `setup.sh`: `must not contain 'remit'` or `is a Kotlin/Java keyword` | Choose a package without the template name and without segments such as `fun`, `in`, `object`. |
| `Skipped Git hooks: ... not a Git repository` | `git init`, then `./setup-hooks.sh`. |
| `iOS xcconfig is out of sync with app.properties` | `scripts/sync-app-config.sh`. Edit `app.properties`, never the synced xcconfig keys. |

## Gradle

- **Wrong JDK:** both builds need JDK 21+. Check `java -version`. In Android Studio set
  Settings → Build Tools → Gradle → Gradle JDK. Do not downgrade the Gradle wrapper; AGP 9
  requires the version that ships in `gradle/wrapper`.
- **`SDK location not found`:** set `ANDROID_HOME` or create `mobile/local.properties`
  with `sdk.dir=/path/to/Android/sdk` (see `local.properties.template`).
- **Stale or corrupt caches:** `./gradlew --stop`, then `./gradlew clean build --refresh-dependencies`
  in the module. Delete `~/.gradle/caches/` only as a last resort.
- **`composeApp` has no install task:** the APK is `:androidApp`
  (`./gradlew :androidApp:installDebug`); `composeApp` is the shared library.

## Android

- **Debug app can't reach the backend:** the emulator reaches your machine at
  `10.0.2.2:8085` (`API_BASE_URL_DEBUG`). Debug builds allow cleartext only to `10.0.2.2`
  and `localhost`. On a physical device run `adb reverse tcp:8085 tcp:8085` and set
  `http://localhost:8085` in Settings → Developer tools.
- **`INSTALL_FAILED_UPDATE_INCOMPATIBLE`:** a build signed with another key is installed:
  `adb uninstall <package>.debug`.
- **Release build crashes on JSON or reflection:** check `androidApp/proguard-rules.pro`.
  `setup.sh` renames its package rules; add keep rules for any new reflective code.

## iOS

- **`Cannot query the value of this provider` in `syncComposeResourcesForIos`:** Xcode
  is building an `x86_64` simulator slice, which Kotlin doesn't produce.
  `Config.xcconfig` excludes it; if you replaced that file, restore
  `EXCLUDED_ARCHS[sdk=iphonesimulator*]=x86_64`.
- **Kotlin changes not picked up:** the Xcode build phase runs
  `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`. Clean the build folder
  (⇧⌘K) if Xcode keeps an old framework.
- **Debug vs Release endpoints:** Debug uses `http://localhost:8085` and shows Developer
  tools. Release (TestFlight/App Store) uses `API_BASE_URL_RELEASE` and hides them.
- **Signing errors in CI:** run `bundle exec fastlane ios certificates` locally once to
  seed Match, and check the secrets in [docs/RELEASING.md](docs/RELEASING.md).

## Backend

- **`Unsupported application environment` at startup:** set `APP_ENVIRONMENT` to
  `development`, `staging`, or `production`; unset means production. Locally, `make backend`
  and `./gradlew run` set `development`.
- **Port 8085 in use:** `make backend` and `make backend-emulated` say whether it is a backend
  that is already running (and what it uses for payments) or another program, and how to stop it.
  Otherwise `lsof -i :8085`, stop that process or set `PORT`.

## Mobile database

- **`verifySqlDelightMigration` fails:** a `.sq` table changed without a migration. Add
  the next `N.sqm` in `commonMain/sqldelight/<package>/data/db/`, then
  `./gradlew :composeApp:generateCommonMainAppDatabaseSchema` once the release ships.

## CI and releases

- **Template contracts job fails:** run `make check-template` locally. It runs the initializer
  and generator tests, the xcconfig sync check, and the resource checks.
- **Mobile CD fails at "Check required secrets":** add the missing secrets listed in
  [docs/RELEASING.md](docs/RELEASING.md). Nothing was committed or tagged.
- **Release commit rejected by branch protection:** add a `RELEASE_TOKEN` secret that can
  push to `main`.

## Git hooks

- **Pre-commit is slow or blocking:** it runs detekt and a compile for the module whose
  Kotlin, Gradle, or SQL files are staged. Bypass once with `git commit --no-verify`;
  disable with `git config --unset core.hooksPath`; re-enable with `./setup-hooks.sh`.
  AI agents cannot bypass it: `scripts/agent-hook.sh` blocks `--no-verify`.
- **An agent edit or command is BLOCKED:** `scripts/agent-hook.sh` refuses edits to generated
  files, credentials, lint baselines, and migrations main already has, and shell commands that
  skip hooks or force push. The message names the alternative (for example, add the next
  migration). For a deliberate exception, make the change yourself outside the agent.
- **ktlint passes in the agent hook but fails in Gradle (or the reverse):** the hook uses the local
  CLI and Gradle uses the version in `gradle/libs.versions.toml`; `make doctor` warns when they
  differ. `brew upgrade ktlint` or bump the catalog so they match.
