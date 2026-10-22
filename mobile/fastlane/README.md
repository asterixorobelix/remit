fastlane documentation
----

# Installation


## Contents

  - [changelog](#changelog)
  - [test_all](#test_all)
  - [all_screenshots](#all_screenshots)
  - [clean](#clean)
  - [check_env](#check_env)
- [Android](#android)
  - [android test](#android-test)
  - [android lint](#android-lint)
  - [android build_debug](#android-build_debug)
  - [android build_release](#android-build_release)
  - [android build_release_apk](#android-build_release_apk)
  - [android bump_version](#android-bump_version)
  - [android screenshots](#android-screenshots)
  - [android internal](#android-internal)
  - [android ci_internal](#android-ci_internal)
  - [android beta](#android-beta)
  - [android release](#android-release)
  - [android promote_to_beta](#android-promote_to_beta)
  - [android promote_to_production](#android-promote_to_production)
- [iOS](#ios)
  - [ios certificates](#ios-certificates)
  - [ios certificates_dev](#ios-certificates_dev)
  - [ios precheck_app](#ios-precheck_app)
  - [ios screenshots](#ios-screenshots)
  - [ios setup_screenshot_tests](#ios-setup_screenshot_tests)
  - [ios test_ios](#ios-test_ios)
  - [ios build_framework](#ios-build_framework)
  - [ios build_framework_device](#ios-build_framework_device)
  - [ios build](#ios-build)
  - [ios build_dev](#ios-build_dev)
  - [ios beta](#ios-beta)
  - [ios beta_external](#ios-beta_external)
  - [ios release](#ios-release)
  - [ios release_submit](#ios-release_submit)

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

### changelog

```sh
[bundle exec] fastlane changelog
```

Print changelog (useful for release notes)

### test_all

```sh
[bundle exec] fastlane test_all
```

Run all tests (Android)

### all_screenshots

```sh
[bundle exec] fastlane all_screenshots
```

Capture screenshots for both iOS and Android

### clean

```sh
[bundle exec] fastlane clean
```

Clean build artifacts

### check_env

```sh
[bundle exec] fastlane check_env
```

Check if all environment variables are configured

----


## Android

### android test

```sh
[bundle exec] fastlane android test
```

Run Android unit tests

### android lint

```sh
[bundle exec] fastlane android lint
```

Run detekt code analysis

### android build_debug

```sh
[bundle exec] fastlane android build_debug
```

Build debug APK

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build release AAB for Play Store

### android build_release_apk

```sh
[bundle exec] fastlane android build_release_apk
```

Build release APK (for direct distribution)

### android bump_version

```sh
[bundle exec] fastlane android bump_version
```

Increment version code

### android screenshots

```sh
[bundle exec] fastlane android screenshots
```

Capture Android screenshots with screengrab

### android internal

```sh
[bundle exec] fastlane android internal
```

Deploy to Play Store Internal Testing track

### android ci_internal

```sh
[bundle exec] fastlane android ci_internal
```

Build and deploy to Internal Testing (CI - skips tests and version bump)

### android beta

```sh
[bundle exec] fastlane android beta
```

Deploy to Play Store Beta (Open Testing)

### android release

```sh
[bundle exec] fastlane android release
```

Deploy to Play Store Production (with staged rollout)

### android promote_to_beta

```sh
[bundle exec] fastlane android promote_to_beta
```

Promote Internal to Beta

### android promote_to_production

```sh
[bundle exec] fastlane android promote_to_production
```

Promote Beta to Production

----


## iOS

### ios certificates

```sh
[bundle exec] fastlane ios certificates
```

Sync code signing certificates and provisioning profiles

### ios certificates_dev

```sh
[bundle exec] fastlane ios certificates_dev
```

Sync development certificates

### ios precheck_app

```sh
[bundle exec] fastlane ios precheck_app
```

Run precheck to catch common App Store rejection issues

### ios screenshots

```sh
[bundle exec] fastlane ios screenshots
```

Capture screenshots on iPhone and iPad simulators

### ios setup_screenshot_tests

```sh
[bundle exec] fastlane ios setup_screenshot_tests
```

Create missing iOS snapshot helper and screenshot test files

### ios test_ios

```sh
[bundle exec] fastlane ios test_ios
```

Run tests on iPhone and iPad simulators to catch launch crashes

### ios build_framework

```sh
[bundle exec] fastlane ios build_framework
```

Build Kotlin Multiplatform framework (all targets)

### ios build_framework_device

```sh
[bundle exec] fastlane ios build_framework_device
```

Build Kotlin framework for device only (arm64, faster for CD)

### ios build

```sh
[bundle exec] fastlane ios build
```

Build iOS app for App Store

### ios build_dev

```sh
[bundle exec] fastlane ios build_dev
```

Build iOS app for development/testing

### ios beta

```sh
[bundle exec] fastlane ios beta
```

Deploy to TestFlight

### ios beta_external

```sh
[bundle exec] fastlane ios beta_external
```

Deploy to TestFlight with external testers

### ios release

```sh
[bundle exec] fastlane ios release
```

Submit to App Store for review

### ios release_submit

```sh
[bundle exec] fastlane ios release_submit
```

Submit to App Store and request review

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
