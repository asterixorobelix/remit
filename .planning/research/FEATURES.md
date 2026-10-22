# KMP Solo Developer Template - Feature Research

**Research Date:** January 15, 2026
**Purpose:** Identify table stakes, differentiators, and anti-features for a production-ready Kotlin Multiplatform template targeting solo developers.

---

## Executive Summary

After analyzing successful mobile app templates (KMPShip, Swift Starter Kits, React Native Starter Kit, Indie Kit), popular GitHub Android templates, and industry best practices for 2025-2026, this document categorizes features into three tiers to guide template development priorities.

**Key Finding:** The most successful indie developer templates focus on monetization enablement (auth, payments, analytics) while avoiding over-engineering. KMPShip has established the benchmark for KMP templates at EUR 79 with authentication, subscriptions via RevenueCat, push notifications, and CI/CD.

---

## Table Stakes (Users Expect These)

These features are baseline requirements. Without them, the template lacks competitive viability.

### 1. Authentication Flow
**Complexity:** HIGH (2-3 weeks)
**Confidence:** HIGH

**Required Components:**
- Email/password authentication
- Social login (Google, Apple Sign-In)
- Token storage in platform secure storage (iOS Keychain, Android Keystore)
- Session management with refresh tokens
- Logout with token revocation

**Best Practices (2025):**
- Use PKCE with system browser (ASWebAuthenticationSession on iOS, Custom Tabs on Android)
- Passkeys support is becoming standard (phishing-resistant, biometric-based)
- Short-lived access tokens with refresh tokens in platform vaults
- Re-authenticate for sensitive actions (step-up auth)

**Sources:**
- [Mobile Authentication Best Practices 2025](https://nextnative.dev/blog/mobile-authentication-best-practices)
- [OAuth for Mobile Apps - Curity](https://curity.io/resources/learn/oauth-for-mobile-apps-best-practices/)
- [OWASP Mobile Authentication Architectures](https://mas.owasp.org/MASTG/0x04e-Testing-Authentication-and-Session-Management/)

---

### 2. Push Notifications Infrastructure
**Complexity:** MEDIUM (1 week)
**Confidence:** HIGH

**Required Components:**
- Firebase Cloud Messaging (FCM) for Android
- Apple Push Notification service (APNs) for iOS
- Push token management
- Notification channels (Android 14+)
- Permission pre-prompting pattern

**Key Statistics:**
- iOS opt-in rate: 51%, Android: 81%
- Users who opt-in are 4x more engaged and 2x more likely retained
- Personalized notifications have 259% higher engagement

**Best Practices:**
- Use pre-qualification prompt before system permission request
- Create 3-5 notification channels for granular user control
- Respect Android 15's Notification Cooldown feature
- Keep titles under 50-60 characters

**Sources:**
- [Pushwoosh - Android Push Notifications 2026](https://www.pushwoosh.com/blog/android-push-notifications/)
- [MoEngage - Push Notification Best Practices 2025](https://www.moengage.com/learn/push-notification-best-practices/)
- [MagicBell - Fundamentals of Mobile Push Notifications](https://www.magicbell.com/blog/fundamentals-of-mobile-push-notifications)

---

### 3. Environment Configuration & Build Variants
**Complexity:** LOW (2-3 days)
**Confidence:** HIGH

**Required Components:**
- Debug, Staging, Release build types
- BuildConfig/AppConfig for environment switching
- Different app IDs per environment (side-by-side installs)
- Secure storage of API keys (not in source control)

**Best Practices:**
- Use `buildConfigField` for runtime access to environment values
- Maintain different Firebase configurations per environment
- Use BuildKonfig for KMP projects

**Current Template Status:** ALREADY IMPLEMENTED

**Sources:**
- [Android Build Variants Documentation](https://developer.android.com/build/build-variants)
- [Managing KMP Configurations](https://sujanpoudel.me/blogs/managing-configurations-for-different-environments-in-kmp/)

---

### 4. Crash Reporting & Analytics Abstraction
**Complexity:** LOW (already partially implemented)
**Confidence:** HIGH

**Required Components:**
- Firebase Crashlytics integration
- Firebase Analytics or privacy-focused alternative
- Platform abstraction layer (expect/actual)
- Consent management for GDPR compliance

**Privacy Requirements (2025):**
- Google Consent Mode v2 is now mandatory
- Must capture granular consent: analytics_storage, ad_storage, ad_user_data, ad_personalization
- Multi-jurisdiction compliance (GDPR, CCPA/CPRA, LGPD, India DPDP Act)

**Current Template Status:** PARTIALLY IMPLEMENTED (abstraction exists, consent management missing)

**Sources:**
- [Secure Privacy - Mobile App Consent Management 2025](https://secureprivacy.ai/blog/mobile-app-sdk-consent-management)
- [Didomi - Mobile App Compliance 2025](https://www.didomi.io/blog/mobile-app-compliance-2025)

---

### 5. Onboarding/Intro Flow Pattern
**Complexity:** MEDIUM (3-5 days)
**Confidence:** HIGH

**Required Components:**
- 3-5 intro screens maximum
- Skip option always available
- Value-first demonstration
- Personalization questions (optional)
- Progress indicators

**Key Statistics:**
- 25% of users abandon apps after one session
- Effective onboarding increases retention by up to 50%
- Day 30 retention averages only 3.4%

**Successful Patterns:**
1. **Progressive onboarding** - introduce features gradually
2. **Personalization-first** - let users customize experience upfront
3. **Value-first/freemium** - try core features before registration
4. **Gamification** - points, badges, streaks

**Anti-patterns to Avoid:**
- 5+ intro screens explaining features users don't care about yet
- Forcing registration to access core functionality
- Onboarding that looks different from actual app

**Sources:**
- [UXCam - Top 10 Onboarding Flow Examples 2025](https://uxcam.com/blog/10-apps-with-great-user-onboarding/)
- [BE-DEV - User Onboarding Patterns 2025](https://be-dev.pl/blog/user-onboarding-in-mobile-apps-what-patterns-work-in-2025)
- [Design Studio UX - Mobile Onboarding Best Practices 2026](https://www.designstudiouiux.com/blog/mobile-app-onboarding-best-practices/)

---

### 6. Network Layer with Retry Logic
**Complexity:** MEDIUM (1 week)
**Confidence:** HIGH

**Required Components:**
- Ktor client with proper error handling
- Exponential backoff with jitter
- Cancellation-aware retry logic
- Timeout configuration
- Result/sealed class for success/error states

**Implementation Pattern:**
```kotlin
// RetryPolicy with exponential backoff
data class RetryPolicy(
    val numRetries: Int = 3,
    val delayMillis: Long = 400,
    val delayFactor: Double = 2.0
)
// Delays: 400ms, 800ms, 1600ms, 3200ms
```

**Best Practices:**
- Add jitter to prevent thundering herd problem
- Check for cancellation in retry loops
- Use `Result.retry()` with WorkManager
- Attach CoroutineExceptionHandler to viewModelScope

**Sources:**
- [ValueOf.io - Retrying Network Requests with Flow](https://www.valueof.io/blog/kotlin-flow-retry-policy-with-exponential-backoff)
- [ProAndroidDev - Advanced Exception Handling in Kotlin Coroutines](https://proandroiddev.com/advanced-exception-handling-in-kotlin-coroutines-a-guide-for-android-developers-e1aede099252)
- [kotlin-retry library](https://github.com/michaelbull/kotlin-retry)

---

## Differentiators (Competitive Advantage)

These features would set the template apart from competitors like KMPShip.

### 1. In-App Purchases & Subscriptions (RevenueCat)
**Complexity:** HIGH (2 weeks)
**Confidence:** HIGH

**Why Differentiating:**
- KMPShip includes this; absence would be a competitive disadvantage
- Solo developers need monetization from day one
- RevenueCat replaced 37,000 lines of code with ~1K lines for one developer

**Required Components:**
- RevenueCat SDK integration
- Entitlements/offerings setup
- Paywall templates (configurable)
- Receipt validation
- Subscription status tracking
- Restore purchases flow

**RevenueCat Benefits:**
- Free until $2,500 monthly tracked revenue
- Cross-platform subscription management
- No direct StoreKit/Google Play Billing code needed
- A/B testing for paywalls

**Implementation Notes:**
- Cannot test in iOS Simulator (physical device required)
- App Store Connect changes can take hours to propagate
- Must sign Paid Applications Agreement before testing

**Sources:**
- [RevenueCat for Indie Apps](https://www.revenuecat.com/indies/)
- [RevenueCat iOS Documentation](https://www.revenuecat.com/docs/getting-started/installation/ios)

---

### 2. Deep Linking Infrastructure
**Complexity:** MEDIUM (1 week)
**Confidence:** HIGH

**Required Components:**
- Universal Links (iOS) configuration
- App Links (Android) configuration
- `.well-known` file generation
- Deep link routing in navigation
- Deferred deep linking support (optional)

**Best Practices:**
- Use Universal Links/App Links over custom URL schemes (more secure)
- Never wrap links in other URLs (breaks deep linking)
- Create intermediate landing page for social media deep links
- Consider MMP integration (Branch, AppsFlyer) for attribution

**Implementation Requirements:**
- Two-way association: website JSON file + app configuration
- Apple Developer Program enrollment required
- Intent filters in Android manifest
- Associated Domains capability in Xcode

**Sources:**
- [Expo - Overview of Deep Links](https://docs.expo.dev/linking/overview/)
- [Nimble - iOS Universal Links and Android App Links](https://nimblehq.co/blog/guideline-ios-universal-links-android-app-links)
- [Branch - Universal Links vs Deep Links](https://www.branch.io/resources/blog/universal-links-uri-schemes-app-links-and-deep-links-whats-the-difference/)

---

### 3. Offline-First Architecture Pattern
**Complexity:** HIGH (2-3 weeks)
**Confidence:** MEDIUM

**Why Differentiating:**
- Most templates are online-only
- Critical for apps used in poor connectivity areas
- Demonstrates architectural sophistication

**Core Architecture:**
- Local database (SQLDelight/Room) as single source of truth
- Network as synchronization mechanism only
- Pending sync queue with WorkManager
- Conflict resolution strategy (last-write-wins or first-write-wins)

**Required Components:**
- Repository pattern with local-first reads
- Background sync with WorkManager
- NetworkCallback for connectivity awareness
- Cache invalidation with TTL
- Optimistic UI updates

**Recommended Stack:**
- SQLDelight for KMP (Room for Android-only)
- Kotlin Flow for reactive data streams
- WorkManager for guaranteed sync delivery

**Sources:**
- [Android Developers - Build Offline-First App](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [droidcon - Complete Guide to Offline-First Architecture](https://www.droidcon.com/2025/12/16/the-complete-guide-to-offline-first-architecture-in-android/)
- [DEV.to - Offline-First Mobile App Architecture](https://dev.to/odunayo_dada/offline-first-mobile-app-architecture-syncing-caching-and-conflict-resolution-518n)

---

### 4. Feature Flags System
**Complexity:** LOW-MEDIUM (3-5 days)
**Confidence:** MEDIUM

**Why Differentiating:**
- Enables trunk-based development
- Allows gradual rollouts and A/B testing
- Runtime control without app updates

**Options:**
1. **Firebase Remote Config** (FREE) - Real-time updates, condition-based targeting
2. **LaunchDarkly** (PAID) - More granular control, better for enterprises
3. **Custom implementation** - Simple but requires backend

**Firebase Remote Config Features:**
- Percentage-based rollouts
- Targeting by: country, language, app version, OS type, user percentile
- Real-time updates (no app restart needed)
- Free tier sufficient for most indie developers

**Implementation:**
- Abstract feature flag access behind interface
- Support for boolean, string, number, and JSON flags
- Local fallback values for offline scenarios

**Sources:**
- [Firebase Remote Config Documentation](https://firebase.google.com/docs/remote-config)
- [DEV.to - Using Firebase Remote Config for Feature Flags](https://dev.to/jacobandrewsky/using-firebase-remote-config-for-feature-flags-2ji9)
- [dohyeon.com - Feature Flags with KMP & Firebase](https://dohyeon.com/effective-feature-flags-with-kotlin-multiplatform-firebase/)

---

### 5. Debug Menu / Developer Tools
**Complexity:** LOW (2-3 days)
**Confidence:** LOW (based on common practices)

**Why Differentiating:**
- Accelerates development and debugging
- Professional touch that developers appreciate
- Often overlooked in templates

**Potential Components:**
- Environment switcher (without rebuild)
- Network request inspector
- Feature flag overrides
- Clear cache/data
- Force crash (test Crashlytics)
- Show current user/session info
- Copy logs to clipboard

**Implementation:**
- Only visible in debug builds
- Accessible via shake gesture or hidden setting

---

### 6. Consent Management / Privacy Dashboard
**Complexity:** MEDIUM (1 week)
**Confidence:** HIGH

**Why Differentiating:**
- Privacy enforcement has reached critical mass in 2025
- Most templates ignore GDPR/CCPA compliance
- Could prevent billion-dollar fines (Meta: $1.3B for mobile tracking)

**Required Components:**
- First-launch consent dialog
- Granular consent controls (analytics, ads, personalization)
- Consent revocation mechanism
- Google Consent Mode v2 integration
- Consent state persistence
- Privacy policy link

**Multi-jurisdiction Requirements:**
- GDPR (EU): Explicit consent required
- CCPA/CPRA (California): Opt-out for data sales
- LGPD (Brazil): GDPR-style consent
- India DPDP Act: Consent-centric with data localization

**Sources:**
- [Secure Privacy - Mobile App Consent Management SDK 2025](https://secureprivacy.ai/blog/mobile-app-sdk-consent-management)
- [UniConsent - Mobile App Consent Management 2025](https://www.uniconsent.com/blog/mobile-app-consent-management-2025)

---

## Anti-Features (Commonly Requested but Problematic)

These features are often requested but add complexity that hurts solo developers more than it helps.

### 1. Multi-Module Architecture from Day One
**Why Anti-Feature:**
- Over-engineering for most indie apps
- Increases build times significantly
- Adds cognitive overhead
- Better to modularize when needed

**Confidence:** MEDIUM

**When It Makes Sense:**
- Team of 3+ developers
- Very large app with clear feature boundaries
- Need to ship features to different stakeholders

**Sources:**
- [Anti-Patterns - Premature Optimization](https://sahandsaba.com/nine-anti-patterns-every-programmer-should-be-aware-of-with-examples.html)

---

### 2. Full Clean Architecture with All Layers
**Why Anti-Feature:**
- Domain layer is optional per Google's architecture guide
- Use cases add boilerplate for simple CRUD operations
- Can be added later when complexity warrants it

**Confidence:** HIGH

**Recommendation:**
- Start with UI + Data layers only
- Add domain layer when:
  - Complex business logic spans multiple data sources
  - Logic needs reuse across multiple ViewModels
  - Business rules require isolation for testing

**Sources:**
- [Android Developers - Guide to App Architecture](https://developer.android.com/topic/architecture)

---

### 3. GraphQL Integration
**Why Anti-Feature:**
- Overkill for most solo developer apps
- REST is simpler and well-understood
- Adds significant complexity to both client and server
- Most indie apps don't need query flexibility

**Confidence:** MEDIUM

**When It Makes Sense:**
- Complex data relationships
- Mobile clients with varying data needs
- Already have GraphQL backend

---

### 4. Comprehensive Testing Infrastructure from Start
**Why Anti-Feature:**
- 80% coverage requirement slows MVP iteration
- Solo developers need speed to market first
- Testing becomes valuable after product-market fit

**Confidence:** MEDIUM

**Recommendation:**
- Include testing setup (dependencies, examples)
- Don't enforce coverage requirements in template
- Focus on critical path testing

---

### 5. Multi-tenant / Team Management
**Why Anti-Feature:**
- Enterprise feature, not indie developer need
- Adds significant complexity to auth and data models
- Can be added if app scales to enterprise

**Confidence:** HIGH

---

### 6. Custom Backend Authentication (Roll Your Own)
**Why Anti-Feature:**
- Security risk if done incorrectly
- Firebase Auth / Auth0 / Passage are battle-tested
- Solo developers shouldn't manage password hashing, token rotation, etc.

**Confidence:** HIGH

**Recommendation:**
- Use Firebase Auth or similar BaaS
- Provide backend auth only for advanced users who need it

---

### 7. Internationalization Beyond String Resources
**Why Anti-Feature:**
- RTL layout support, plural rules, date/currency formatting add complexity
- Most indie apps start English-only or with 1-2 languages
- String resources are sufficient for MVP

**Confidence:** MEDIUM

**Current Template Status:** Has string resources with English/Spanish - appropriate level

---

### 8. Accessibility Beyond Basics
**Why Anti-Feature:**
- Full accessibility audit is time-consuming
- Semantic actions, screen reader optimization require expertise
- Basic content descriptions are sufficient for MVP

**Confidence:** LOW

**Recommendation:**
- Require contentDescription on interactive elements
- Defer comprehensive accessibility to post-launch

---

## Feature Dependencies Diagram

```
                           ┌─────────────────────┐
                           │   Authentication    │
                           │   (Table Stakes)    │
                           └──────────┬──────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │ Push            │     │ In-App          │     │ Analytics       │
    │ Notifications   │     │ Purchases       │     │ (Crashlytics)   │
    │ (Table Stakes)  │     │ (Differentiator)│     │ (Table Stakes)  │
    └────────┬────────┘     └────────┬────────┘     └────────┬────────┘
             │                       │                       │
             │                       │                       │
             ▼                       ▼                       ▼
    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │ Onboarding      │     │ Deep Linking    │     │ Consent         │
    │ Flow            │     │ (Differentiator)│     │ Management      │
    │ (Table Stakes)  │     │                 │     │ (Differentiator)│
    └─────────────────┘     └─────────────────┘     └─────────────────┘

                    Independent Features (No Dependencies)
    ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
    │ Environment     │     │ Feature Flags   │     │ Offline-First   │
    │ Config          │     │ (Differentiator)│     │ (Differentiator)│
    │ (Table Stakes)  │     │                 │     │                 │
    └─────────────────┘     └─────────────────┘     └─────────────────┘

                           Network Layer (Foundation)
                    ┌───────────────────────────────────┐
                    │   Retry Logic / Error Handling    │
                    │   (Table Stakes - Foundation)     │
                    └───────────────────────────────────┘
```

### Dependency Notes:
1. **Authentication** is foundational - many features depend on user identity
2. **Push Notifications** require FCM setup, which pairs with Analytics
3. **In-App Purchases** require authenticated users for subscription management
4. **Deep Linking** benefits from auth for personalized link handling
5. **Consent Management** affects Analytics configuration
6. **Environment Config** is infrastructure, not dependent on other features
7. **Feature Flags** independent but often used to control other features
8. **Offline-First** is architectural pattern affecting entire data layer

---

## MVP Definition & Prioritization

### Phase 1: Foundation (Current + Quick Wins)
**Timeline:** Already implemented + 1-2 weeks

| Feature | Status | Effort | Priority |
|---------|--------|--------|----------|
| Environment Config | DONE | - | - |
| Crash Reporting Abstraction | DONE | - | - |
| Analytics Abstraction | DONE | - | - |
| MVVM + Repository Pattern | DONE | - | - |
| CI/CD + Fastlane | DONE | - | - |
| Network Retry Logic | PARTIAL | LOW | HIGH |
| String Resources (i18n) | DONE | - | - |

### Phase 2: Table Stakes Completion
**Timeline:** 3-4 weeks

| Feature | Status | Effort | Priority |
|---------|--------|--------|----------|
| Authentication Flow | NOT STARTED | HIGH | CRITICAL |
| Push Notifications | NOT STARTED | MEDIUM | HIGH |
| Onboarding Flow Pattern | NOT STARTED | MEDIUM | HIGH |

### Phase 3: Key Differentiators
**Timeline:** 4-6 weeks

| Feature | Status | Effort | Priority |
|---------|--------|--------|----------|
| In-App Purchases (RevenueCat) | NOT STARTED | HIGH | HIGH |
| Deep Linking | NOT STARTED | MEDIUM | MEDIUM |
| Feature Flags | NOT STARTED | LOW | MEDIUM |

### Phase 4: Advanced Differentiators
**Timeline:** 2-4 weeks (optional)

| Feature | Status | Effort | Priority |
|---------|--------|--------|----------|
| Offline-First Pattern | NOT STARTED | HIGH | LOW |
| Consent Management | NOT STARTED | MEDIUM | MEDIUM |
| Debug Menu | NOT STARTED | LOW | LOW |

---

## Competitive Analysis Summary

### KMPShip (Primary Competitor)
**Price:** EUR 79 one-time
**Features:**
- Authentication (Google, Apple, Email)
- Subscriptions (RevenueCat)
- Push Notifications
- Firebase Remote Config
- Clean Architecture
- CI/CD (Fastlane)

**Our Advantages (Current):**
- Backend included (Ktor with JWT, PostgreSQL, Flyway)
- More comprehensive CI/CD pipeline
- Better documentation (CLAUDE.md)
- Free/open-source (if applicable)

**Gaps to Close:**
- Authentication flow
- Push notifications
- In-app purchases

### Other Templates (GitHub)
- Most focus on architecture patterns, not production readiness
- Few include monetization infrastructure
- Backend rarely included

---

## Recommendations

### Must Implement (Table Stakes)
1. **Authentication Flow** - Without this, template is unusable for most apps
2. **Push Notifications** - Expected by users, critical for retention
3. **Onboarding Pattern** - Reusable component with best practices baked in

### Should Implement (Differentiators)
1. **RevenueCat Integration** - Matches competitor, enables monetization
2. **Deep Linking** - Modern apps require this for sharing/marketing
3. **Feature Flags** - Low effort, high value for iteration speed

### Could Implement (Nice to Have)
1. **Offline-First Pattern** - Advanced, adds significant value for certain app types
2. **Consent Management** - Increasingly important, complex to do right
3. **Debug Menu** - Developer experience improvement

### Won't Implement (Anti-Features)
1. Multi-module from day one
2. Full Clean Architecture with mandatory domain layer
3. GraphQL
4. Multi-tenant/team management
5. Roll-your-own backend auth (use Firebase Auth instead)

---

## Sources & Confidence Levels

### HIGH Confidence (Multiple authoritative sources agree)
- [Android Developers - App Architecture](https://developer.android.com/topic/architecture)
- [Firebase Documentation](https://firebase.google.com/docs)
- [RevenueCat Documentation](https://www.revenuecat.com/docs/)
- [JetBrains KMP Roadmap](https://blog.jetbrains.com/kotlin/2024/10/kotlin-multiplatform-development-roadmap-for-2025/)
- [OWASP Mobile Security](https://mas.owasp.org/MASTG/)

### MEDIUM Confidence (Industry best practices, some variation)
- [KMPShip Features](https://www.kmpship.app/) - Commercial competitor
- [UXCam Onboarding Examples](https://uxcam.com/blog/10-apps-with-great-user-onboarding/)
- [droidcon Offline Architecture](https://www.droidcon.com/2025/12/16/the-complete-guide-to-offline-first-architecture-in-android/)
- [Secure Privacy GDPR Guidance](https://secureprivacy.ai/blog/mobile-app-sdk-consent-management)

### LOW Confidence (Single source or inference)
- Debug menu features (common practice, no authoritative source)
- Accessibility recommendations (based on general guidelines)
- Anti-feature categorization (judgment call based on solo developer context)

---

*Last Updated: January 15, 2026*
