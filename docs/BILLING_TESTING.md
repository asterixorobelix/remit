# Billing test setup

The template uses product IDs from `app.properties`:

- Android: `PLAY_BILLING_PRODUCT=premium_monthly_test`
- iOS: `IOS_BILLING_PRODUCT=com.example.myproject.premium.monthly`

The shared Xcode scheme loads `mobile/iosApp/iosApp/Products.storekit`, so purchase, cancellation,
restore, and interrupted-transaction flows can be tested locally without App Store
Connect. Keep the StoreKit product ID equal to `IOS_BILLING_PRODUCT`.

The Android client uses Play Billing Library 8 and queries a one-time (INAPP) product. Switch
the product type in `AndroidPremiumRepository` if you sell a subscription.

For Google Play, create a one-time product matching `PLAY_BILLING_PRODUCT`, upload an
internal-test build, add license testers in Play Console, and install from the Play
test link. Side-loaded APKs cannot exercise the real Play purchase sheet.

Before shipping, test success, cancellation, pending purchase, restore, offline
entitlement startup, and an already-owned product on both stores.
