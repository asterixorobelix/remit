# Firebase (optional)

Firebase Crashlytics and Analytics are wired for **Android** and are off by default. Without
Firebase, `CrashLogger` and `Analytics` fall back to platform logging (Logcat/NSLog), so
shared code calls the same interfaces either way. iOS uses the logging fallback. Add the
Firebase iOS SDK through Swift Package Manager if you need it there.

## Enable on Android

1. Create a Firebase project and add an Android app with the `PACKAGE_NAME` from
   `app.properties`. Add `<package>.debug` and `<package>.staging` too if you want
   those builds reported.
2. Download `google-services.json` to `mobile/androidApp/google-services.json`. It is
   gitignored; never commit it.
3. Add `firebase` to `FEATURES` in `app.properties` and run `scripts/sync-app-config.sh`.
4. Build: `make apk`. The build applies the Google Services plugin only when both conditions
   hold, and logs a warning if `firebase` is enabled without the file. The Crashlytics plugin
   is always applied (the SDK is always linked and needs its build id) but uploads mapping
   files only for a real project.

At runtime, crash reports and analytics go to Firebase only when the `firebase` capability is
enabled and the app was configured from `google-services.json` (never for the emulators).
Collection is off in the manifest until the app turns it on, and a build without the file
deactivates Analytics entirely. The code calls the Firebase SDKs directly, so R8 needs no
keep rules for them.

## CI

Store the file as a secret: `base64 -i google-services.json | tr -d '\n'` →
`GOOGLE_SERVICES_JSON`. The `firebase-setup` action writes it before the Android release
build; without the secret, builds skip Firebase.

For Firebase App Distribution of release APKs, add `FIREBASE_ANDROID_APP_ID` and a
`FIREBASE_SERVICE_ACCOUNT_JSON` with the Firebase App Distribution Admin role (see
[RELEASING.md](RELEASING.md)).

## Verify

Install a release or staging build with the file present, then trigger a non-fatal error
through `CrashLogger.logException(...)`. It appears in the Crashlytics console within a
few minutes of the next app start. Logcat shows `Crashlytics unavailable` when the SDK is
not present.
