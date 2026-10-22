# Requirements: KMP Solo Developer Template

**Defined:** 2026-01-15
**Core Value:** Zero-to-app-store with minimal friction. Every common integration pre-wired and documented.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Infrastructure

- [x] **INFRA-01**: iOS app includes valid privacy manifest file
- [x] **INFRA-02**: Privacy manifest declares required reason APIs used by Kotlin runtime
- [x] **INFRA-03**: Compose Multiplatform updated to 1.10.0
- [x] **INFRA-04**: iOS builds only compile required architecture (not all three)
- [x] **INFRA-05**: Gradle build configuration passes compatibility checks

### Feature Flags

- [x] **FLAG-01**: Developer can define feature flags with Firebase Remote Config
- [x] **FLAG-02**: Feature flags support boolean, string, and number types
- [x] **FLAG-03**: Feature flags have local fallback values for offline scenarios
- [x] **FLAG-04**: Feature flag interface is injectable via Koin for testability
- [x] **FLAG-05**: Debug builds can override feature flags locally

### Network

- [x] **NET-01**: HTTP requests retry with exponential backoff on transient failures
- [x] **NET-02**: Retry logic includes jitter to prevent thundering herd
- [x] **NET-03**: Request/connect/socket timeouts are configurable
- [x] **NET-04**: Network errors return typed Result (Success/Error/Loading)
- [x] **NET-05**: Retry respects coroutine cancellation

### Documentation

- [x] **DOC-01**: QUICKSTART.md enables clone-to-running in under 10 minutes
- [x] **DOC-02**: FEATURE_SETUP.md documents Firebase, push, auth, analytics setup
- [x] **DOC-03**: PRE_LAUNCH_CHECKLIST.md covers all app store submission requirements
- [x] **DOC-04**: Privacy policy template is included
- [x] **DOC-05**: Terms of Service template is included

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Authentication

- **AUTH-01**: User can sign up with email and password
- **AUTH-02**: User can log in with email and password
- **AUTH-03**: User can reset password via email link
- **AUTH-04**: Auth tokens stored securely (Keychain/Keystore)
- **AUTH-05**: Session persists across app restart
- **AUTH-06**: User can sign in with Google
- **AUTH-07**: User can sign in with Apple

### Push Notifications

- **PUSH-01**: App registers for push with FCM (Android) and APNs (iOS)
- **PUSH-02**: Push token is managed and refreshed automatically
- **PUSH-03**: Permission pre-prompting screen explains value before system prompt
- **PUSH-04**: Android notification channels configured for granular control

### Onboarding

- **ONBD-01**: Onboarding flow displays 3-5 intro screens
- **ONBD-02**: User can skip onboarding at any point
- **ONBD-03**: Onboarding progress indicator shows current position

### Deep Linking

- **LINK-01**: Universal Links configured for iOS
- **LINK-02**: App Links configured for Android
- **LINK-03**: Deep link routes to correct screen in navigation

### Monetization

- **PAID-01**: RevenueCat SDK integrated
- **PAID-02**: Subscription offerings configurable
- **PAID-03**: User can restore purchases
- **PAID-04**: Paywall template is customizable

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Multi-module architecture | Overkill for solo developers, adds build complexity |
| GraphQL integration | REST is simpler, template already uses Ktor |
| Mandatory domain layer | Optional per Android guidelines, adds boilerplate |
| Roll-your-own backend auth | Security risk, use Firebase Auth or similar BaaS |
| Offline-first data sync | High complexity, defer to v2+ when needed |
| Consent management (GDPR) | Complex multi-jurisdiction requirements, defer |
| Debug menu | Nice-to-have, not critical for template |
| Web platform support | Focus on Android + iOS only |
| Multi-language templates | Kotlin-only, no Flutter/React Native |

## Traceability

Which phases cover which requirements. Updated by create-roadmap.

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFRA-01 | Phase 1 | Complete |
| INFRA-02 | Phase 1 | Complete |
| INFRA-03 | Phase 2 | Complete |
| INFRA-04 | Phase 2 | Complete |
| INFRA-05 | Phase 2 | Complete |
| FLAG-01 | Phase 3 | Complete |
| FLAG-02 | Phase 3 | Complete |
| FLAG-03 | Phase 3 | Complete |
| FLAG-04 | Phase 3 | Complete |
| FLAG-05 | Phase 3 | Complete |
| NET-01 | Phase 4 | Complete |
| NET-02 | Phase 4 | Complete |
| NET-03 | Phase 4 | Complete |
| NET-04 | Phase 4 | Complete |
| NET-05 | Phase 4 | Complete |
| DOC-01 | Phase 5 | Complete |
| DOC-02 | Phase 5 | Complete |
| DOC-03 | Phase 5 | Complete |
| DOC-04 | Phase 5 | Complete |
| DOC-05 | Phase 5 | Complete |

**Coverage:**
- v1 requirements: 20 total
- Mapped to phases: 20
- Unmapped: 0 ✓

---
*Requirements defined: 2026-01-15*
*Last updated: 2026-01-16 after Phase 5 completion — All v1 requirements complete*
