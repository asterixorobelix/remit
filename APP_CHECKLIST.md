# Checklist for each real app

Keep this file in every project created from the template. Review it after setup
and before the first release with `make app-checklist`. Check items off only after
implementing and verifying them in that app; these are manual reminders, not an
automated release gate.

## Share app

**Reminder: add “Share app” to Settings → About before launching a real app.**
The template does not currently implement a native share sheet. Decide the final
destination and copy for each app, then implement the feature there.

- [ ] Add a localized “Share app” action to Settings → About, alongside Rate app.
- [ ] Configure the published Google Play and App Store links, or a single public
      landing page linking to both stores. Keep the chosen URLs in `app.properties`
      and pass them through the platform configuration; never share a debug package
      URL, placeholder, private beta invitation, or development API endpoint.
- [ ] Compose localized share text containing the app name and public download link.
- [ ] Open the native Android and iOS share sheets through a shared platform
      abstraction. Handle unavailable sharing gracefully and support iPad presentation.
- [ ] Keep the action unavailable until a real public destination is configured.
- [ ] Verify opening, cancelling, and opening the sheet again on both platforms;
      cancellation must return to Settings without reporting an error.
- [ ] Send the text to another app and open the received link to verify it reaches
      the intended app listing or landing page.
- [ ] Check accessibility labels, large text, and light/dark appearance.

Start in `mobile/composeApp/src/commonMain/kotlin/<package>/presentation/settings/`.
Put localized copy in `composeResources/values*/strings.xml` and native sharing
implementations in `androidMain` and `iosMain`. Add behavioral tests for share-content
selection and ViewModel events if the implementation uses a ViewModel. Opening a
sheet is not proof that someone completed a share.

## Replace app-specific defaults

- [ ] Choose capabilities in `app.properties` and delete code for ones the app will
      never use (including the maps SPM package and location permissions below).
- [ ] Set `API_BASE_URL_STAGING` and `API_BASE_URL_RELEASE` in `app.properties`, then
      `scripts/sync-app-config.sh`. Verify auth and offline sync against them.
- [ ] Replace placeholder string resources in `composeResources/values/strings.xml`:
      `support_email` (Feedback opens the mail app), `app_store_id` (force update on
      iOS), `settings_privacy_url`, and `settings_terms_url`.
- [ ] Replace onboarding copy, paywall copy, branding, colors, and icons.
- [ ] **Password reset:** the backend has no reset endpoint and the template ships no
      reset screen. Add both (email provider, expiring single-use tokens) or state
      how users recover accounts.
- [ ] Implement real search and notification sources if the app keeps those screens
      (defaults: search over local Tasks, empty notifications).
- [ ] Set `minVersion` for force updates in the backend `/api/v1/config` when needed.
- [ ] Configure billing products and test purchases, restores, and pending purchases
      if billing is enabled ([docs/BILLING_TESTING.md](docs/BILLING_TESTING.md)).
- [ ] Verify sign-out, account deletion (required by App Store guideline 5.1.1(v)),
      and session expiry on both platforms.
- [ ] Verify light/dark themes, accessibility, localization, and offline/error states.

## Privacy, permissions, and legal

- [ ] Remove location permissions from `androidApp/src/main/AndroidManifest.xml` and
      `NSLocationWhenInUseUsageDescription` from `iosApp/iosApp/Info.plist` if the app
      does not use maps.
- [ ] Update `iosApp/iosApp/PrivacyInfo.xcprivacy` collected data types (email, name,
      diagnostics, location as applicable) and complete Play's Data safety form.
- [ ] Host the privacy policy and terms (templates in `docs/`) and link them in the stores.
- [ ] Choose a license for the repository if it will be public.

## Backend deployment

- [ ] Set `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `DATABASE_URL`, and `CORS_ALLOWED_HOSTS`
      in the hosting platform ([backend/README.md](backend/README.md)). The server
      refuses to start in production with unsafe values.
- [ ] Point `/health` (liveness) and `/ready` (readiness) at the platform's checks.

## GitHub, CI, and store delivery

The workflows need no editing after `setup.sh`: they read the identity from
`app.properties`. What each app must configure in GitHub and the stores:

- [ ] Push to a new GitHub repository and confirm the first **CI** run is green. Until
      one is, every push runs every job (CI measures changes from the last green run).
- [ ] Set the owner in `.github/CODEOWNERS`.
- [ ] Run `make protect-main` so agent pull requests merge only with green checks and resolved
      review threads, and changes to sensitive paths need your approval. It also creates the
      `release` environment: put `RELEASE_TOKEN` and the store secrets there
      (`gh secret set NAME --env release`), or Mobile CD stops at its first step.
- [ ] Add the store secrets for each platform you release ([docs/RELEASING.md](docs/RELEASING.md#github-secrets)),
      and do the one-time Play Console and App Store Connect setup described there.
- [ ] Optional repository variables: `TESTFLIGHT_EXTERNAL_GROUPS` (default
      `Beta Testers`), `FIREBASE_TESTER_GROUPS` (default `beta-testers`), and
      `COPYRIGHT_HOLDER` for the App Store copyright line. Tester groups must already
      exist in App Store Connect and Firebase.
- [ ] AI review: add an `OPENROUTER_API_KEY` secret (any model; pick one with the
      `AI_REVIEW_MODEL` variable, for example `openrouter/openai/gpt-5.6-sol`). Without it
      the check passes without reviewing.
- [ ] Optional free extras: `SEMGREP_APP_TOKEN` (Semgrep Pro rules, free up to 10
      contributors), `FIREBASE_TEST_LAB_KEY` (nightly smoke tests on real devices instead of
      the emulator), and a `FIREBASE_ANDROID_PREVIEW_APP_ID` variable (a Firebase app for
      `<package>.debug`) so `preview`-labelled pull requests reach testers' phones.
- [ ] CodeQL runs only when code scanning is enabled: free on public repositories,
      GitHub Code Security on private ones. Otherwise it is a one-minute weekly no-op.
- [ ] Faster or cheaper runners are a variable change: set `LINUX_RUNNER` / `MACOS_RUNNER`
      to another provider's label or a self-hosted runner (x86_64 Linux for the lint job).
- [ ] Private repositories bill macOS minutes at 10x Linux. CI already limits the macOS
      job to iOS-relevant changes; check usage in Settings → Billing after a few weeks.
- [ ] Do a first `internal`/`testflight` Mobile CD run, test it, then ship it with Mobile
      Promote rather than building again.

## Before release

- [ ] Run `make doctor` and `make check`, then the platform build and runtime checks
      in [the validation matrix](docs/AI_AGENT_GUIDE.md#validation-matrix).
- [ ] Complete [the store release checklist](RELEASE_CHECKLIST.md), including the
      Share app checks above. Document any intentionally omitted feature here.
