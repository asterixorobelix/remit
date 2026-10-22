# Releasing to Google Play and the App Store

Releases run in two manual workflows, or through Fastlane locally:

1. **Mobile CD** (`.github/workflows/mobile-cd.yml`) builds a new version, uploads it for
   testers (Play internal track and TestFlight by default), and tags it `vX.Y.Z`.
2. **Mobile Promote** (`.github/workflows/mobile-promote.yml`) ships that same tested build
   wider without rebuilding it: Play beta or a staged production rollout, external
   TestFlight testers, or App Store review with a phased release.

`app.properties` is the source of the package/bundle identifier and version; Fastlane reads
it, so no identifier secrets are needed.

Before the first release, work through [RELEASE_CHECKLIST.md](../RELEASE_CHECKLIST.md)
and [APP_CHECKLIST.md](../APP_CHECKLIST.md).

## How Mobile CD works

1. **Validate** (Linux): fails fast if a selected platform's secrets are missing or `main`
   is protected without a `RELEASE_TOKEN`, computes the next version without committing,
   then runs `make check-template` and the Linux part of `make check-mobile`. It refuses
   to run from a branch other than `main` or to reuse an existing tag.
2. **Build Android** (Linux): signed AAB and APK, built before anything is uploaded so a
   release-build or signing failure stops the run with no store changes.
3. **Upload iOS** (macOS 26, Xcode 26): `fastlane ios beta`, `beta_external`, or `release`.
   Match fetches signing assets, and the Xcode build phase compiles the shared Kotlin
   framework. App Store Connect rejects builds made with an SDK older than iOS 26.
4. **Upload Android**: `fastlane android ci_<track>` uploads the AAB built in step 2
   (`internal`, `beta`, or `ci_release` for production at a 10% rollout). Optional Firebase
   App Distribution runs afterwards and never blocks the release.
5. **Finalize**: only after every selected upload succeeds does it bump the version on the
   latest `main`, commit `chore: release vX.Y.Z`, push the commit and tag atomically
   (retrying if `main` moved), and publish a GitHub release with the artifacts.

Release notes are generated from conventional commits since the previous tag
(`scripts/release-notes.sh`, or `make release-notes` to preview): `feat` becomes "New",
`fix` "Fixes", `perf` "Improvements"; internal types are left out. They become the GitHub
release body, the TestFlight "What to Test" text, and the Play "What's new" text (first
500 characters, language `PLAY_LOCALE`, default `en-US`). The `release_notes` input
overrides them. Tick **store_listing** to also upload the listing written in
`mobile/store_assets/STORE_LISTING.md` (converted by `fastlane/sync_store_listing.rb`).

If `main` is protected, add a `RELEASE_TOKEN` secret to the `release` environment (a
fine-grained PAT owned by an admin, with Contents and Administration read and write on this
repository only) so finalize can push.

### When a release fails

- **Before any upload** (validate, Android build, or the iOS build): fix the problem and
  start a new run. Nothing was uploaded or committed.
- **After an upload** (for example the Android upload or finalize fails after iOS
  succeeded): open the failed run and choose **Re-run failed jobs**. Do not start a new run:
  the version has not been committed yet, and the stores reject a build number they already
  have. The upload lanes check the store first and skip a build that is already there.
- With `testflight_external`, a re-run that finds the build already uploaded skips external
  distribution; distribute it to the group in App Store Connect.

## Promoting a tested build

After testers have used a Mobile CD build, run **Mobile Promote** with its tag (empty means
the newest `v*` tag):

| Input | Effect |
|---|---|
| `android: beta` | Moves the build from `android_from` (internal) to the beta track |
| `android: production` | Staged production rollout at `android_rollout` (0.1 = 10%) |
| same again, higher `android_rollout` | Widens the rollout; `1` releases to everyone |
| `ios: testflight_external` | Gives the build to `TESTFLIGHT_EXTERNAL_GROUPS` |
| `ios: app_store` | Submits it for review; after approval it releases in phases over 7 days |

Each step checks the store first, so re-running a promotion is safe. Promotion only calls
the store APIs and runs on Linux.

## One-time setup

### Android

1. **Upload key.** Create a keystore and keep it (and its passwords) somewhere safe:
   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   Local signed builds read `mobile/keystore.properties` (copy
   `mobile/keystore.properties.template`; it is gitignored).
2. **Create the app** in Play Console with the `PACKAGE_NAME` from `app.properties`, and
   upload the first AAB manually (`make apk-release` / `:androidApp:bundleRelease`).
   Play's API cannot create apps.
3. **Service account.** In Google Cloud, create a service account with a JSON key, then
   invite its `client_email` in Play Console → Users and permissions with release
   permissions for the app.

### iOS

1. **App ID and App Store Connect record** for the `PACKAGE_NAME` bundle identifier.
2. **Certificates repo for Match.** Create a private Git repo. Create a fine-grained
   PAT with `Contents: read/write` on that repo only, then:
   `printf 'github-user:PAT' | base64` → `MATCH_GIT_BASIC_AUTHORIZATION`.
3. **Seed signing assets** once from a Mac:
   ```bash
   cd mobile && bundle install
   MATCH_GIT_URL=https://github.com/you/certificates MATCH_PASSWORD=... \
     APPLE_TEAM_ID=... bundle exec fastlane ios certificates
   ```
4. **App Store Connect API key** (Users and Access → Integrations → Keys, App Manager
   role). Download the `.p8` once and base64 it:
   `base64 -i AuthKey_XXXX.p8 | tr -d '\n'` → `ASC_KEY_CONTENT`.

## GitHub secrets

Store these in the **`release` environment**, not as repository secrets:
`gh secret set NAME --env release`. Mobile CD and Mobile Promote run in that environment, and
only protected branches (`main`) may use it (`make protect-main` creates it). Repository
secrets, by contrast, are readable by workflow code in any same-repository pull request,
including one opened by an AI agent. GitHub does not copy secrets into repositories created
from a template, so every app repository needs its own.

| Secret | Platform | Value |
|---|---|---|
| `ANDROID_SIGNING_KEY` | Android | `base64 -i release.keystore \| tr -d '\n'` |
| `ANDROID_KEYSTORE_PASSWORD` | Android | Keystore password |
| `ANDROID_KEY_ALIAS` | Android | Key alias (e.g. `upload`) |
| `ANDROID_KEY_PASSWORD` | Android | Key password |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Android | Service-account JSON (raw or base64) |
| `MATCH_GIT_URL` | iOS, optional | Certificates repo; `mobile/fastlane/Matchfile` defaults to the owner's shared repo |
| `MATCH_GIT_BASIC_AUTHORIZATION` | iOS | base64 of `user:PAT`; the PAT needs only read access to the certificates repo |
| `MATCH_PASSWORD` | iOS | Match encryption passphrase |
| `APPLE_TEAM_ID` | iOS | Developer team ID |
| `ASC_KEY_ID`, `ASC_ISSUER_ID` | iOS | From the API key page |
| `ASC_KEY_CONTENT` | iOS | base64 of the `.p8` file |
| `GOOGLE_SERVICES_JSON` | Optional | base64 `google-services.json` ([Firebase](FIREBASE.md)) |
| `FIREBASE_ANDROID_APP_ID`, `FIREBASE_SERVICE_ACCOUNT_JSON` | Optional | Firebase App Distribution of the release APK to `beta-testers` |
| `RELEASE_TOKEN` | Both, when `main` is protected | Fine-grained PAT for this repository only: Contents and Administration, read and write (tokens without Administration cannot push past protection) |

Optional repository **variables** (not secrets): `TESTFLIGHT_EXTERNAL_GROUPS`
(comma-separated, default `Beta Testers`), `FIREBASE_TESTER_GROUPS` (default
`beta-testers`), `COPYRIGHT_HOLDER` (App Store copyright line; without it only the year
in an existing `copyright.txt` is updated), and `PLAY_LOCALE` (default `en-US`). The
groups must exist in App Store Connect and Firebase before a release uses them.

## Running a release

GitHub → Actions → **Mobile CD** → Run workflow on `main`, choosing platform, version
bump, Android track, and iOS destination. Keep the defaults (internal / TestFlight) for a
release candidate, test it, then use **Mobile Promote**. A fix found in testing is a new
patch version through Mobile CD.

Locally (from `mobile/`, with the same variables exported or in `fastlane/.env` copied
from `fastlane/.env.template`):

```bash
bundle exec fastlane android internal      # test, lint, bump, build, upload
bundle exec fastlane android promote to:beta
bundle exec fastlane android promote to:production rollout:0.1
bundle exec fastlane android rollout rollout:0.5
bundle exec fastlane ios beta              # TestFlight (internal testers)
bundle exec fastlane ios promote_external  # same build to external testers
bundle exec fastlane ios submit_for_review # same build to App Store review, phased release
bundle exec fastlane check_env             # show which variables are set
```

Local lanes that bump the version edit `app.properties` and the iOS xcconfig through
`bump-version.sh`; commit those changes yourself.

## Branch protection (optional)

`scripts/protect-main.sh` (`make protect-main`) protects `main` for AI-agent pull requests:
every CI job and the AI review must pass, every review thread must be resolved, pull
requests touching `.github/CODEOWNERS` paths (workflows, Fastlane, database schemas,
backend configuration, permissions) need the owner's approval, and force pushes and deletion are blocked. It also
enables auto-merge, so an agent can run `gh pr merge --auto --squash`. Admins can still
push directly. Requires an authenticated `gh`.
