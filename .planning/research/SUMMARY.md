# Project Research Summary

**Project:** KMP Solo Developer Template
**Domain:** Developer tooling / Kotlin Multiplatform project template
**Researched:** January 15, 2026
**Confidence:** HIGH

## Executive Summary

This research analyzed the ecosystem for building production-ready Kotlin Multiplatform templates targeting solo developers who want to ship cross-platform apps (Android + iOS) to app stores quickly.

**Key Finding:** The KMP ecosystem has matured significantly. With Compose Multiplatform 1.8.0 (May 2025), iOS support became stable, and Google officially supports KMP for Jetpack libraries. The template's existing foundation (Ktor 3.3.3, Koin 4.1.1, Navigation 2.9.1) aligns with current best practices.

**Competitive Position:** The primary competitor is KMPShip (EUR 79). This template has advantages (Ktor backend, comprehensive CI/CD) but gaps in authentication, push notifications, and RevenueCat integration. Closing these gaps creates a compelling alternative.

**Critical Risk:** iOS-specific issues (build times, CocoaPods conflicts, privacy manifests, Compose performance) require early attention. These are well-documented pitfalls that catch many KMP projects off guard.

## Key Findings

### Recommended Stack

The template's current stack is well-aligned with 2025 best practices. Key recommendations:

**Core technologies (current and recommended):**
- **Kotlin 2.3.0**: Current — no change needed
- **Compose Multiplatform 1.10.0**: Update from 1.9.3 for Navigation 3 support and Hot Reload
- **Ktor 3.3.3**: Current — no change needed
- **Koin 4.1.1**: Current — no change needed
- **Navigation 2.9.1**: Current — consider Navigation 3 (alpha) when stable

**Stack additions to consider:**
- **SQLDelight 2.2.1**: For offline-first local database (or Room KMP 2.7.0+)
- **DataStore 1.2.0**: For key-value preferences
- **Coil 3.3.0**: Image loading alternative to Kamel

### Expected Features

**Must have (table stakes):**
- Authentication flow (email/password + social login) — users expect this
- Push notifications (FCM + APNs) — critical for retention (4x engagement)
- Onboarding flow (3-5 screens) — increases retention by 50%
- Network retry logic with exponential backoff — resilience expected

**Should have (competitive):**
- In-App Purchases (RevenueCat) — matches KMPShip, enables monetization
- Deep linking (Universal Links + App Links) — modern apps require this
- Feature flags (Firebase Remote Config) — enables trunk-based development

**Defer (v2+):**
- Offline-first architecture pattern — high complexity, add when needed
- Multi-module architecture — overkill for solo developers
- GraphQL integration — REST is simpler for most apps

### Architecture Approach

The template follows recommended patterns: single `composeApp` module with package-based organization, MVVM + Repository pattern, and expect/actual for platform abstractions.

**Major components:**
1. **Presentation Layer**: Compose screens + ViewModels with StateFlow
2. **Domain Layer (optional)**: Use cases for complex business logic
3. **Data Layer**: Repository pattern with local-first caching
4. **Platform Layer**: expect/actual for Logger, Analytics, CrashLogger

**Key principles:**
- Unidirectional Data Flow (UDF) for state management
- Repository as single source of truth
- Koin modules organized by layer (data, domain, presentation, platform)

### Critical Pitfalls

1. **iOS build times**: Building all architectures takes 3x longer — build only needed architecture
2. **CocoaPods/SPM conflicts**: Transitive dependencies cause linker errors — audit dependencies carefully
3. **iOS privacy manifest**: Required since Spring 2024 — add before first submission
4. **Firebase integration**: Requires manual initialization + dSYM upload for iOS
5. **Navigation state loss**: ViewModels recreated unexpectedly on iOS — test thoroughly

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Foundation Hardening
**Rationale:** Current template has strong foundation but needs iOS-specific hardening and stack updates before adding features.
**Delivers:** Production-ready build configuration, iOS privacy manifest, updated Compose Multiplatform
**Addresses:** Build time optimization, Gradle compatibility, iOS architecture selection
**Avoids:** iOS build time bloat, privacy manifest rejection, Gradle version conflicts

### Phase 2: Authentication & Push
**Rationale:** Authentication is foundational — many features depend on user identity. Push notifications are table stakes for mobile apps.
**Delivers:** Complete auth flow (login, register, forgot password, token storage), FCM/APNs integration
**Uses:** Secure storage (Keychain/Keystore), expect/actual for platform auth
**Implements:** Token management, session handling, permission pre-prompting

### Phase 3: Monetization & Distribution
**Rationale:** Solo developers need monetization from day one. Deep linking enables sharing and marketing.
**Delivers:** RevenueCat integration, Universal Links/App Links, feature flags
**Uses:** RevenueCat SDK, Firebase Remote Config
**Avoids:** Rolling custom payment integration (security risk)

### Phase 4: User Experience
**Rationale:** Onboarding and offline support improve retention and usability.
**Delivers:** Onboarding flow template, offline-first data pattern (optional)
**Implements:** Progressive onboarding, local database caching
**Addresses:** User retention, poor connectivity handling

### Phase 5: Documentation & Polish
**Rationale:** Template needs comprehensive documentation for developer adoption.
**Delivers:** QUICKSTART.md, FEATURE_SETUP.md, PRE_LAUNCH_CHECKLIST.md
**Addresses:** Developer experience, reducing time-to-first-build

### Phase Ordering Rationale

- **Foundation first**: iOS hardening prevents painful debugging later. Build configuration issues compound.
- **Auth before features**: Most features need user identity. Adding auth later requires refactoring.
- **Monetization before UX polish**: Solo developers need revenue path established early.
- **Documentation last**: Document what's actually built, not what's planned.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (Auth)**: Deep research on PKCE flow, passkeys, social auth SDK integration
- **Phase 3 (RevenueCat)**: Needs testing on physical devices, App Store Connect configuration

Phases with standard patterns (skip research-phase):
- **Phase 1 (Foundation)**: Well-documented Gradle/Compose updates
- **Phase 5 (Documentation)**: Straightforward markdown authoring

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified with JetBrains official docs, version compatibility confirmed |
| Features | HIGH | Based on competitor analysis (KMPShip), industry best practices |
| Architecture | HIGH | Standard patterns from Android Architecture Guidelines, Koin docs |
| Pitfalls | HIGH | From post-mortems, GitHub issues, experienced developer accounts |

**Overall confidence:** HIGH

### Gaps to Address

- **RevenueCat KMP integration**: No official docs found — may need native SDK wrappers
- **Passkeys support**: Emerging standard — consider as future enhancement
- **Navigation 3**: Currently alpha — monitor for stability before adopting

## Sources

### Primary (HIGH confidence)
- [JetBrains Compose Multiplatform Docs](https://www.jetbrains.com/help/kotlin-multiplatform-dev/)
- [Kotlin Multiplatform Compatibility Guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [Android Architecture Guidelines](https://developer.android.com/topic/architecture)
- [Koin KMP Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/)
- [Ktor Documentation](https://ktor.io/docs/)

### Secondary (MEDIUM confidence)
- [Touchlab Build Optimization](https://touchlab.co/touchlab-build-only-what-you-need)
- [KMPShip Feature Analysis](https://www.kmpship.app/)
- [ProAndroidDev KMP Scalability](https://proandroiddev.com/kotlin-multiplatform-scalability-challenges-on-a-large-project-b3140e12da9d)
- [JetBrains Compose GitHub Issues](https://github.com/JetBrains/compose-multiplatform/issues)

### Tertiary (LOW confidence)
- Community blog posts without production validation
- Pre-Kotlin 2.0 advice (may be outdated)

---
*Research completed: January 15, 2026*
*Ready for roadmap: yes*
