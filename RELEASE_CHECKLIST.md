# App Store Release Checklist


## Contents

- [Pre-Release Tasks](#pre-release-tasks)
  - [Legal Documents](#legal-documents)
  - [Code Preparation](#code-preparation)
  - [App-Specific Features](#app-specific-features)
  - [App Icons](#app-icons)
  - [Store Assets Directory](#store-assets-directory)
  - [Firebase & Crash Reporting](#firebase-crash-reporting)
- [Google Play Store](#google-play-store)
  - [Developer Account](#developer-account)
  - [Android Release Signing](#android-release-signing)
  - [Store Listing Assets](#store-listing-assets)
  - [App Content Rating](#app-content-rating)
  - [Privacy & Policies](#privacy-policies)
  - [App Category & Contact](#app-category-contact)
  - [Release Management](#release-management)
- [Apple App Store](#apple-app-store)
  - [Developer Account](#developer-account)
  - [iOS Build Preparation](#ios-build-preparation)
  - [App Store Connect Setup](#app-store-connect-setup)
  - [Store Listing Assets](#store-listing-assets)
  - [App Information](#app-information)
  - [Privacy](#privacy)
  - [Review Information](#review-information)
  - [Release Management](#release-management)
- [Marketing Materials](#marketing-materials)
  - [Brand Assets](#brand-assets)
  - [Press Kit](#press-kit)
  - [Social Media](#social-media)
  - [Website](#website)
- [Post-Release](#post-release)
  - [Monitoring](#monitoring)
  - [User Support](#user-support)
  - [Iteration](#iteration)
- [Quick Reference Commands](#quick-reference-commands)
- [Resources](#resources)

A comprehensive checklist for publishing your Compose Multiplatform app to Google Play Store and Apple App Store.

## Pre-Release Tasks

### Legal Documents

Before release, prepare your legal documents:

- [ ] Customize `docs/PRIVACY_POLICY.md` for your app's data practices
- [ ] Customize `docs/TERMS_OF_SERVICE.md` for your app's terms
- [ ] Host privacy policy and terms on your website
- [ ] Have legal counsel review documents (recommended)

> **Note**: Both templates include `[PLACEHOLDER]` values that must be replaced with your specific information before publishing.

### Code Preparation

- [ ] Remove all debug `println` statements
- [ ] Remove hardcoded test data and credentials
- [ ] Verify ProGuard/R8 rules are complete (`mobile/androidApp/proguard-rules.pro`)
- [ ] Run template contracts, tests, and static analysis from the root: `make check`
- [ ] Update versions through `./bump-version.sh patch --yes` and verify
      `app.properties` and iOS configuration; do not hand-edit generated build values.

### App-Specific Features

- [ ] Review [APP_CHECKLIST.md](APP_CHECKLIST.md) with `make app-checklist`.
- [ ] Set up store signing and CI secrets: [docs/RELEASING.md](docs/RELEASING.md).
- [ ] **Add Share app to Settings → About** using localized copy, public download
      links, and native Android/iOS sharing. This is a reminder in the template;
      implement it for your actual app before checking this item off.
- [ ] Verify sharing, cancelling, and opening the received link on both platforms.
- [ ] Finish or remove app-specific demo screens and placeholder URLs listed in
      the app checklist.

### App Icons

#### Android Icons

- [ ] Create app icon source (SVG recommended, 512x512px minimum)
- [ ] Generate launcher icons for all densities:
  - `mipmap-mdpi/ic_launcher.png` (48x48)
  - `mipmap-hdpi/ic_launcher.png` (72x72)
  - `mipmap-xhdpi/ic_launcher.png` (96x96)
  - `mipmap-xxhdpi/ic_launcher.png` (144x144)
  - `mipmap-xxxhdpi/ic_launcher.png` (192x192)
- [ ] Generate round launcher icons (`ic_launcher_round.png`) at same sizes
- [ ] Configure adaptive icons (API 26+):
  - Create `mipmap-anydpi-v26/ic_launcher.xml`
  - Create `mipmap-anydpi-v26/ic_launcher_round.xml`
  - Create foreground drawable in `drawable/ic_launcher_foreground.xml`
  - Define background color in `values/ic_launcher_background.xml`

#### iOS Icons

- [ ] Generate 1024x1024 App Store icon
- [ ] Add icon to Xcode asset catalog (`Assets.xcassets/AppIcon.appiconset`)

### Store Assets Directory

All store listing assets should be placed in `mobile/store_assets/`. Use the template `STORE_LISTING.md` for text content.

**Required assets:**
- [ ] Feature graphic (`feature_graphic.png`, 1024x500) - Google Play
- [ ] Promotional banner (`promo_banner_wide.png`, 1200x628) - Social/marketing
- [ ] App Store icon (`appstore_icon_1024.png`, 1024x1024) - iOS
- [ ] Phone screenshots (minimum 2, recommended 4-8)
- [ ] Complete `STORE_LISTING.md` with all text content

**Generate from SVG:**
```bash
cd mobile/store_assets
magick feature_graphic.svg -resize 1024x500 feature_graphic.png
magick ../app_icon_source.svg -resize 1024x1024 appstore_icon_1024.png
```

### Firebase & Crash Reporting (optional)

- [ ] Follow [docs/FIREBASE.md](docs/FIREBASE.md): add `firebase` to `FEATURES`, place
      the real `mobile/androidApp/google-services.json` (gitignored), and set the
      `GOOGLE_SERVICES_JSON` secret for CI.
- [ ] Verify a non-fatal report reaches Crashlytics from a release build.

---

## Google Play Store

### Developer Account

- [ ] Create Google Play Developer account ($25 one-time fee)
- [ ] Complete identity verification
- [ ] Set up payment profile for paid apps/in-app purchases

### Android Release Signing

- [ ] Generate release keystore:
  ```bash
  keytool -genkey -v -keystore release.keystore -alias myapp -keyalg RSA -keysize 2048 -validity 10000
  ```
- [ ] Create `keystore.properties` from template:
  ```properties
  storeFile=path/to/your/release.keystore
  storePassword=your_keystore_password
  keyAlias=your_key_alias
  keyPassword=your_key_password
  ```
- [ ] **CRITICAL**: Backup keystore and credentials securely (losing them = cannot update app)
- [ ] Build release AAB: `cd mobile && ./gradlew :androidApp:bundleRelease`

### Store Listing Assets

> **TIP**: Use `mobile/store_assets/STORE_LISTING.md` as your template for all text content.

#### Required

- [ ] **App name** (max 30 characters)
- [ ] **Short description** (max 80 characters) - see STORE_LISTING.md
- [ ] **Full description** (max 4000 characters) - see STORE_LISTING.md
- [ ] **App icon** (512x512 PNG, 32-bit, no alpha)
- [ ] **Feature graphic** (1024x500 PNG or JPG)
- [ ] **Screenshots** (minimum 2, recommended 8):
  - Phone: 16:9 or 9:16 aspect ratio, min 320px, max 3840px per side
  - 7" tablet (optional): same requirements
  - 10" tablet (optional): same requirements

#### Optional but Recommended

- [ ] **Promo video** (YouTube URL)
- [ ] **Wear OS screenshots** (if applicable)
- [ ] **TV banner** (if applicable, 1280x720)

### App Content Rating

- [ ] Complete content rating questionnaire
- [ ] Review and confirm rating classification

### Privacy & Policies

> **Templates Available**: Customize the legal templates in `docs/` folder, then host on your website:
> - `docs/PRIVACY_POLICY.md` - Privacy Policy template with GDPR/CCPA guidance
> - `docs/TERMS_OF_SERVICE.md` - Terms of Service template

- [ ] **Privacy policy URL** (required for apps with permissions or account features)
- [ ] Data safety form completion:
  - [ ] Data collection disclosure
  - [ ] Data sharing disclosure
  - [ ] Security practices
  - [ ] Data deletion options

### App Category & Contact

- [ ] Select app category
- [ ] Select content rating
- [ ] Provide contact email
- [ ] Provide contact website (optional)
- [ ] Provide contact phone (optional)

### Release Management

- [ ] Choose release track (Internal/Closed/Open testing, or Production)
- [ ] Upload signed AAB
- [ ] Write release notes
- [ ] Set rollout percentage (if staged rollout)
- [ ] Submit for review

---

## Apple App Store

### Developer Account

- [ ] Enroll in Apple Developer Program ($99/year)
- [ ] Accept latest agreements in App Store Connect
- [ ] Configure banking and tax information for paid apps

### iOS Build Preparation

- [ ] Verify the bundle identifier generated by `setup.sh` in iOS configuration
- [ ] Configure signing with distribution certificate
- [ ] Create App Store provisioning profile
- [ ] Update `Info.plist`:
  - [ ] `CFBundleDisplayName` - Display name
  - [ ] `CFBundleShortVersionString` - Uses the generated version configuration
  - [ ] `CFBundleVersion` - Uses the generated build-number configuration
  - [ ] `ITSAppUsesNonExemptEncryption` - Set to `false` if no custom encryption
- [ ] Archive and upload to App Store Connect

### App Store Connect Setup

- [ ] Create new app in App Store Connect
- [ ] Set primary language
- [ ] Set bundle ID
- [ ] Set SKU (internal reference)

### Store Listing Assets

> **TIP**: Use `mobile/store_assets/STORE_LISTING.md` as your template for all text content.

#### Required

- [ ] **App name** (max 30 characters)
- [ ] **Subtitle** (max 30 characters) - see STORE_LISTING.md
- [ ] **Description** (max 4000 characters) - see STORE_LISTING.md
- [ ] **Keywords** (max 100 characters, comma-separated) - see STORE_LISTING.md
- [ ] **Support URL**
- [ ] **Screenshots** for each device size:
  - 6.7" (iPhone 15 Pro Max): 1290x2796 or 2796x1290
  - 6.5" (iPhone 14 Plus): 1284x2778 or 2778x1284
  - 5.5" (iPhone 8 Plus): 1242x2208 or 2208x1242
  - iPad Pro 12.9" (6th gen): 2048x2732 or 2732x2048
  - iPad Pro 12.9" (2nd gen): 2048x2732 or 2732x2048

#### Optional but Recommended

- [ ] **Promotional text** (max 170 characters, can update without new version)
- [ ] **App preview videos** (15-30 seconds each)

### App Information

- [ ] Select primary category
- [ ] Select secondary category (optional)
- [ ] Content rights declaration
- [ ] Age rating questionnaire

### Privacy

- [ ] **Privacy policy URL** (required)
- [ ] App privacy details (nutrition labels):
  - [ ] Data used to track you
  - [ ] Data linked to you
  - [ ] Data not linked to you

### Review Information

- [ ] Contact information for App Review team
- [ ] Demo account credentials (if login required)
- [ ] Notes for reviewer (explain non-obvious features)

### Release Management

- [ ] Select version release option (manual/automatic)
- [ ] Submit for review
- [ ] Respond promptly to any review feedback

---

## Marketing Materials

### Brand Assets

- [ ] App icon in various sizes (for press kit)
- [ ] Logo variations (light/dark backgrounds)
- [ ] Brand color palette documentation
- [ ] Typography guidelines

### Press Kit

- [ ] Company/developer description
- [ ] App description (short, medium, long versions)
- [ ] Key features bullet points
- [ ] High-resolution screenshots
- [ ] App icon files (PNG, SVG)
- [ ] Founder/team photos (optional)

### Social Media

- [ ] Twitter/X assets (profile, header, post images)
- [ ] Facebook/Instagram assets
- [ ] LinkedIn company page assets

### Website

- [ ] Landing page
- [ ] App Store badges (use official badges)
- [ ] QR codes for app store links
- [ ] Privacy policy page
- [ ] Terms of service page

---

## Post-Release

### Monitoring

- [ ] Set up crash reporting alerts
- [ ] Monitor app reviews daily (first week)
- [ ] Track key metrics (downloads, retention, crashes)
- [ ] Set up Google Play Console / App Store Connect notifications

### User Support

- [ ] Support email address ready
- [ ] FAQ prepared
- [ ] Response templates for common issues

### Iteration

- [ ] Plan first update based on user feedback
- [ ] Schedule regular review of crash reports
- [ ] A/B test store listing elements

---

## Quick Reference Commands

```bash
# Navigate to mobile directory
cd mobile

# Run all tests
./gradlew test

# Run static analysis
./gradlew detekt

# Build debug APK
./gradlew :androidApp:assembleDebug

# Build release AAB (for Play Store)
./gradlew :androidApp:bundleRelease

# Build release APK
./gradlew :androidApp:assembleRelease

# Build iOS framework (the Xcode build phase does this for app builds)
./gradlew :composeApp:linkReleaseFrameworkIosArm64

# Generate keystore
keytool -genkey -v -keystore release.keystore -alias myapp -keyalg RSA -keysize 2048 -validity 10000
```

---

## Resources

- [Google Play Console](https://play.google.com/console)
- [App Store Connect](https://appstoreconnect.apple.com)
- [Google Play Asset Requirements](https://support.google.com/googleplay/android-developer/answer/9866151)
- [App Store Screenshot Specifications](https://developer.apple.com/help/app-store-connect/reference/screenshot-specifications)
- [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines)
- [Material Design Guidelines](https://m3.material.io)
