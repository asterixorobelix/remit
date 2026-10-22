# KMP Solo Developer Template

## What This Is

A production-ready Kotlin Multiplatform template that eliminates repetitive setup work for solo developers building cross-platform apps. Clone it, run setup, and start coding your actual idea — not boilerplate.

## Core Value

**Zero-to-app-store with minimal friction.** Every common integration (auth, crash reporting, analytics, push, etc.) is pre-wired and documented. Solo developers focus on their unique app logic, not infrastructure.

## Requirements

### Validated

<!-- Existing capabilities confirmed working -->

- ✓ Ktor backend with JWT auth, PostgreSQL/H2, Flyway migrations — existing
- ✓ Compose Multiplatform UI with Material 3 theming — existing
- ✓ MVVM + Repository pattern with Koin DI — existing
- ✓ Firebase Crashlytics abstraction with fallback — existing
- ✓ Firebase Analytics abstraction with fallback — existing
- ✓ CI/CD pipelines (GitHub Actions) for build/test/deploy — existing
- ✓ Fastlane automation for App Store/Play Store — existing
- ✓ Detekt static analysis with zero-tolerance — existing
- ✓ Package renaming via `setup.sh` — existing
- ✓ Store listing templates and release checklist — existing
- ✓ Platform abstractions (Logger, AppConfig) — existing

<!-- v1.0 Template Enhancement -->

- ✓ iOS privacy manifest for App Store compliance — v1.0
- ✓ Compose Multiplatform 1.10.0 with ARM64 optimization — v1.0
- ✓ Feature flags system (interface + local + debug decorator) — v1.0
- ✓ Network resilience with exponential backoff and jitter — v1.0
- ✓ Configurable HTTP timeouts and retry policies — v1.0
- ✓ QUICKSTART.md with accurate project information — v1.0
- ✓ FEATURE_SETUP.md with per-feature configuration guides — v1.0
- ✓ Privacy Policy template — v1.0
- ✓ Terms of Service template — v1.0

### Active

<!-- Candidates for v1.1+ -->

**Infrastructure (plug-and-play)**
- [ ] Mobile auth UI screens (login, register, forgot password, token storage)
- [ ] Push notification scaffolding (FCM + APNs with Koin-injectable service)
- [ ] Onboarding flow template (swipeable intro screens)
- [ ] Deep linking setup (email verification, password reset, universal links)
- [ ] In-app purchases/subscriptions scaffolding (StoreKit + Google Play Billing)
- [ ] Offline-first data sync pattern (local database + sync with backend)

**Developer Experience**
- [ ] Environment switcher in debug builds (switch dev/staging/prod from UI)
- [ ] Consolidated secrets management (single source of truth for all credentials)
- [ ] Pre-commit hooks (catch common issues before commit)

**App Store Automation**
- [ ] Automated screenshot capture for all device sizes (fastlane snapshot/screengrab)
- [ ] Changelog generation from git commits
- [ ] Version bumping script (updates Android versionCode/Name + iOS CFBundle together)

### Out of Scope

- Custom backend hosting solutions — Railway/Docker already supported
- Web platform support — focusing on Android + iOS only
- Multiple language templates — Kotlin-only, no Flutter/React Native
- Team collaboration features — this is for solo developers

## Context

**Existing Codebase:**
- Full-stack Kotlin monorepo with backend (Ktor) and mobile (KMP + Compose)
- Codebase mapped in `.planning/codebase/` (7 documents)
- Known issues documented in `.planning/codebase/CONCERNS.md`

**Target User:**
- Solo Kotlin/Android developers
- Building cross-platform apps (Android + iOS)
- Want to ship to app stores quickly
- Prefer convention over configuration

**Claude Code Skills to Use:**
- `/app-icons` — Generate all app icon sizes from single SVG
- `/gradle-expert` — Gradle build troubleshooting
- `/fastlane-expert` — Deployment automation
- `/kmp-expert` — KMP architecture guidance
- `/material3-compose` — UI component generation
- `/state-manager` — Screen state management scaffolding
- Other relevant skills as needed during implementation

## Constraints

- **Tech Stack**: Kotlin Multiplatform + Compose — no deviation
- **Compatibility**: Android API 24+ (7.0), iOS 14+
- **Dependencies**: Use version catalog (`libs.versions.toml`) — no hardcoded versions
- **Architecture**: Clean Architecture + MVVM + Repository pattern
- **Quality**: Detekt must pass, 80% test coverage target maintained

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Firebase for crash/analytics | Industry standard, free tier sufficient for solo devs | ✓ Good |
| Koin for DI | KMP-compatible, simpler than Hilt for multiplatform | ✓ Good |
| Repository pattern for data | Single source of truth, testable, offline-ready | ✓ Good |
| Fastlane for store deployment | Cross-platform automation, well-documented | ✓ Good |
| Use Claude Code skills for generation | Leverage existing automation for consistency | ✓ Good |
| iOS ARM64-only targets | Intel Mac simulators deprecated since M1 (2020) | ✓ Good |
| Ktor 3.x createClientPlugin for retry | Modern API, replaces deprecated HttpSend interceptor | ✓ Good |
| Boolean-only feature flags | Simplified initial implementation, multi-type deferred | ✓ Good |
| Privacy manifest reason codes | 0A2A.1 (FileTimestamp), CA92.1 (UserDefaults) for CMP | ✓ Good |

## Context

**Current State (v1.0):**
- Full-stack Kotlin monorepo with backend (Ktor 3.3.3) and mobile (KMP + Compose 1.10.0)
- iOS privacy manifest included for App Store compliance
- Feature flags system ready for Firebase Remote Config integration
- Network layer with configurable retry/timeout policies
- 4,007 lines of Kotlin mobile code
- Codebase mapped in `.planning/codebase/` (7 documents)

**Known Issues:**
- 19 pre-existing detekt issues (4h 35min tech debt) - documented for future maintenance
- Compose DSL deprecation warnings (informational, non-blocking)
- KMP + AGP 9.0 compatibility warning (future concern)

---
*Last updated: 2026-01-16 after v1.0 milestone*
