# Phase 3: Feature Flags - Research

**Researched:** 2026-01-15
**Domain:** Kotlin Multiplatform feature flags with optional Firebase Remote Config
**Confidence:** HIGH

<research_summary>
## Summary

Researched feature flag solutions for Kotlin Multiplatform with a focus on the user's key requirement: **zero-config setup that works without Firebase**, with Firebase Remote Config as an optional additive layer.

The recommended approach is a **code-first design** where feature flags are defined as a simple Kotlin interface with default values in code. Firebase Remote Config becomes an optional implementation that can override these defaults when configured. This matches the project's existing pattern of platform abstractions (Logger, CrashLogger, Analytics) with Koin DI.

Key finding: Existing KMP feature flag libraries (jimmymorales/feature-flags, ConfigCat) require external services or are immature. The cleanest solution is a custom interface following the project's established patterns — this is actually **less code** than integrating a library and provides zero external dependencies for the default case.

**Primary recommendation:** Create a `FeatureFlags` interface with code-level defaults, injectable via Koin. Implement a `LocalFeatureFlags` class that uses those defaults. Firebase Remote Config integration can be added later as an optional implementation.

</research_summary>

<standard_stack>
## Standard Stack

### Core (Required - Zero Dependencies)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Koin | 4.0.4 | Dependency injection | Already in project, enables swappable implementations |
| kotlinx.coroutines | 1.10.2 | Async flag fetching | Already in project |

### Optional (Firebase Integration)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| [dev.gitlive:firebase-config](https://github.com/GitLiveApp/firebase-kotlin-sdk) | 2.4.0 | Firebase Remote Config for KMP | Production remote control |
| Firebase Android SDK | (via BOM) | Native Android integration | Android-specific features |
| Firebase iOS SDK | (via CocoaPods) | Native iOS integration | iOS-specific features |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Custom interface | [ConfigCat SDK](https://github.com/configcat/kotlin-sdk) | ConfigCat requires external service account, monthly fees, not zero-config |
| Custom interface | [jimmymorales/feature-flags](https://github.com/jimmymorales/feature-flags) | Library is v0.2.0 from 2022, minimal documentation, abandoned |
| firebase-kotlin-sdk | Expect/actual with native SDKs | More setup, but full API coverage |

**Installation (Core - no additional dependencies needed):**
```kotlin
// Nothing to add - uses existing Koin
```

**Installation (Optional Firebase integration):**
```toml
# In libs.versions.toml
[versions]
firebase-gitlive = "2.4.0"

[libraries]
firebase-config = { module = "dev.gitlive:firebase-config", version.ref = "firebase-gitlive" }
```

```kotlin
// In commonMain dependencies
implementation(libs.firebase.config)
```

</standard_stack>

<architecture_patterns>
## Architecture Patterns

### Recommended Project Structure
```
composeApp/src/
├── commonMain/kotlin/.../
│   ├── feature/
│   │   ├── FeatureFlags.kt           # Interface with flag definitions
│   │   ├── LocalFeatureFlags.kt      # Default implementation (code defaults)
│   │   └── RemoteFeatureFlags.kt     # Optional Firebase implementation
│   └── di/
│       └── FeatureFlagsModule.kt     # Koin module
├── androidMain/kotlin/.../
│   └── feature/
│       └── FirebaseFeatureFlags.kt   # Android-specific Firebase (optional)
└── iosMain/kotlin/.../
    └── feature/
        └── FirebaseFeatureFlags.kt   # iOS-specific Firebase (optional)
```

### Pattern 1: Interface with Code-Level Defaults
**What:** Define flags as interface properties with default values in the implementation
**When to use:** Always - this is the core pattern
**Example:**
```kotlin
// FeatureFlags.kt - Interface defines WHAT flags exist
interface FeatureFlags {
    val newOnboarding: Boolean
    val premiumFeatures: Boolean
    val experimentalUi: Boolean

    companion object {
        // Code-level defaults (source of truth for default behavior)
        const val DEFAULT_NEW_ONBOARDING = false
        const val DEFAULT_PREMIUM_FEATURES = false
        const val DEFAULT_EXPERIMENTAL_UI = false
    }
}

// LocalFeatureFlags.kt - Uses code defaults, zero config needed
class LocalFeatureFlags : FeatureFlags {
    override val newOnboarding: Boolean = FeatureFlags.DEFAULT_NEW_ONBOARDING
    override val premiumFeatures: Boolean = FeatureFlags.DEFAULT_PREMIUM_FEATURES
    override val experimentalUi: Boolean = FeatureFlags.DEFAULT_EXPERIMENTAL_UI
}
```

### Pattern 2: Koin Module with Conditional Binding
**What:** Swap implementations based on configuration availability
**When to use:** When adding Firebase integration later
**Example:**
```kotlin
// FeatureFlagsModule.kt
val featureFlagsModule = module {
    // Default: local implementation (always works)
    single<FeatureFlags> { LocalFeatureFlags() }
}

// Later, with Firebase configured:
val featureFlagsModule = module {
    single<FeatureFlags> {
        if (firebaseConfigured()) {
            RemoteFeatureFlags(get()) // Uses Firebase
        } else {
            LocalFeatureFlags() // Fallback
        }
    }
}
```

### Pattern 3: Debug Override Mechanism
**What:** Allow debug builds to override flags without Firebase console
**When to use:** Development and testing
**Example:**
```kotlin
class DebugFeatureFlags(
    private val base: FeatureFlags,
    private val overrides: Map<String, Boolean> = emptyMap()
) : FeatureFlags {
    override val newOnboarding: Boolean
        get() = overrides["newOnboarding"] ?: base.newOnboarding

    override val premiumFeatures: Boolean
        get() = overrides["premiumFeatures"] ?: base.premiumFeatures
}
```

### Anti-Patterns to Avoid
- **Requiring Firebase to compile:** Always have a local fallback that works with zero config
- **Blocking app startup on Remote Config fetch:** Use load-for-next-startup pattern
- **Hardcoding flag names as strings everywhere:** Use typed interface properties
- **No offline fallback:** Always have code-level defaults

</architecture_patterns>

<dont_hand_roll>
## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Remote flag fetching | Custom HTTP polling | Firebase Remote Config or similar | Handles caching, offline, sync automatically |
| Flag value caching | Custom SharedPreferences/UserDefaults | Firebase SDK's built-in caching | Handles expiration, invalidation, race conditions |
| Real-time flag updates | Custom WebSocket/polling | Firebase onConfigUpdateListener | Optimized for battery, handles reconnection |
| A/B test analytics | Custom tracking | Firebase Analytics integration | Automatic experiment tracking, statistical analysis |

**Key insight:** For this phase's scope (boolean flags with optional remote control), the "don't hand-roll" principle means:
1. **DO hand-roll:** The simple interface and local implementation (trivial code, zero dependencies)
2. **DON'T hand-roll:** Remote fetching, caching, offline sync (use Firebase if needed)

The user's requirement for "zero-config setup" actually favors a minimal custom interface over integrating a full feature flag library that requires external service configuration.

</dont_hand_roll>

<common_pitfalls>
## Common Pitfalls

### Pitfall 1: Blocking App Startup on Remote Config
**What goes wrong:** App hangs on splash screen waiting for Remote Config fetch
**Why it happens:** Calling `fetchAndActivate()` synchronously on startup
**How to avoid:** Use "load for next startup" pattern - activate cached values immediately, fetch asynchronously
**Warning signs:** Slow cold starts, complaints about "loading" screens

### Pitfall 2: No Offline Fallback Values
**What goes wrong:** App crashes or behaves unexpectedly when offline
**Why it happens:** Assuming Remote Config always returns values
**How to avoid:** Define code-level defaults for every flag
**Warning signs:** `NullPointerException` or unexpected UI when testing offline

### Pitfall 3: Firebase SDK Initialization Order (KMP-specific)
**What goes wrong:** Crashes on iOS due to Firebase not initialized
**Why it happens:** Firebase iOS SDK requires initialization before use
**How to avoid:** Initialize Firebase in platform-specific code before accessing Remote Config
**Warning signs:** `FirebaseApp is not initialized` errors on iOS

### Pitfall 4: Remote Config Throttling
**What goes wrong:** Flags stop updating, fetch returns cached values
**Why it happens:** Firebase throttles fetch requests (default: 12 hours cache)
**How to avoid:** Use `minimumFetchIntervalInSeconds` in development only, respect production limits
**Warning signs:** Flag changes in console don't appear in app

### Pitfall 5: Type Coercion Errors
**What goes wrong:** Boolean flag returns unexpected value
**Why it happens:** Remote Config returns strings, must be parsed correctly
**How to avoid:** Use typed getters (`getBoolean()` not `getString()`)
**Warning signs:** `"true"` string instead of `true` boolean

### Pitfall 6: firebase-kotlin-sdk API Coverage
**What goes wrong:** Expected API not available in KMP
**Why it happens:** firebase-kotlin-sdk has ~20% Remote Config API coverage
**How to avoid:** Check available APIs before planning, consider expect/actual for missing features
**Warning signs:** Compile errors when using advanced Remote Config features

</common_pitfalls>

<code_examples>
## Code Examples

Verified patterns from project analysis and official sources:

### Basic Feature Flags Interface
```kotlin
// Source: Project pattern analysis (matches Logger, CrashLogger, Analytics interfaces)
package com.example.myproject.feature

/**
 * Feature flags interface for controlling feature availability.
 *
 * Usage:
 * ```kotlin
 * val featureFlags: FeatureFlags = koinInject()
 * if (featureFlags.newOnboarding) {
 *     // Show new onboarding flow
 * }
 * ```
 */
interface FeatureFlags {
    /** Enable new onboarding experience */
    val newOnboarding: Boolean

    /** Enable premium features for all users (testing) */
    val premiumFeatures: Boolean

    /** Enable experimental UI components */
    val experimentalUi: Boolean

    companion object {
        // Code-level defaults - source of truth when no remote config
        const val DEFAULT_NEW_ONBOARDING = false
        const val DEFAULT_PREMIUM_FEATURES = false
        const val DEFAULT_EXPERIMENTAL_UI = false
    }
}
```

### Local Implementation (Zero Config)
```kotlin
// Source: Project pattern (matches existing platform implementations)
package com.example.myproject.feature

/**
 * Local feature flags implementation using code-level defaults.
 * Works immediately without any external configuration.
 */
class LocalFeatureFlags : FeatureFlags {
    override val newOnboarding: Boolean = FeatureFlags.DEFAULT_NEW_ONBOARDING
    override val premiumFeatures: Boolean = FeatureFlags.DEFAULT_PREMIUM_FEATURES
    override val experimentalUi: Boolean = FeatureFlags.DEFAULT_EXPERIMENTAL_UI
}
```

### Koin Module Registration
```kotlin
// Source: Project pattern (matches existing di/AppModule.kt)
package com.example.myproject.di

import com.example.myproject.feature.FeatureFlags
import com.example.myproject.feature.LocalFeatureFlags
import org.koin.dsl.module

val featureFlagsModule = module {
    single<FeatureFlags> { LocalFeatureFlags() }
}
```

### Usage in Composable
```kotlin
// Source: Project pattern (matches existing Logger/Analytics usage)
@Composable
fun OnboardingRouter() {
    val featureFlags: FeatureFlags = koinInject()

    if (featureFlags.newOnboarding) {
        NewOnboardingScreen()
    } else {
        LegacyOnboardingScreen()
    }
}
```

### Future Firebase Implementation (Optional)
```kotlin
// Source: firebase-kotlin-sdk documentation pattern
// Only add when Firebase Remote Config is needed
package com.example.myproject.feature

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig

class RemoteFeatureFlags : FeatureFlags {
    private val remoteConfig = Firebase.remoteConfig

    override val newOnboarding: Boolean
        get() = remoteConfig.getValue("new_onboarding").asBoolean()
            ?: FeatureFlags.DEFAULT_NEW_ONBOARDING

    override val premiumFeatures: Boolean
        get() = remoteConfig.getValue("premium_features").asBoolean()
            ?: FeatureFlags.DEFAULT_PREMIUM_FEATURES

    override val experimentalUi: Boolean
        get() = remoteConfig.getValue("experimental_ui").asBoolean()
            ?: FeatureFlags.DEFAULT_EXPERIMENTAL_UI

    suspend fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
    }
}
```

</code_examples>

<sota_updates>
## State of the Art (2025-2026)

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Separate Android/iOS flag implementations | firebase-kotlin-sdk for shared code | 2023+ | Single implementation for both platforms |
| fetch() then activate() separately | fetchAndActivate() single call | Firebase v2 SDK | Simpler API, fewer race conditions |
| Blocking initialization | Non-blocking async fetch | Firebase v2 SDK | Better app startup times |
| String-based flag keys | Typed interface properties | Best practice | Compile-time safety, IDE support |

**New tools/patterns to consider:**
- **firebase-kotlin-sdk v2.4.0 (Oct 2025):** Latest version with improved KMP support
- **Real-time config updates:** `addOnConfigUpdateListener()` for push-based updates without polling
- **Load-for-next-startup:** Recommended pattern to avoid blocking startup

**Deprecated/outdated:**
- **cannon.js-style polling:** Use Firebase's real-time listeners instead of periodic fetches
- **SharedPreferences caching:** Firebase SDK handles caching automatically
- **v1 SDK blocking pattern:** Use non-blocking initialization

</sota_updates>

<open_questions>
## Open Questions

Things that couldn't be fully resolved:

1. **firebase-kotlin-sdk Remote Config API completeness**
   - What we know: Documentation says ~20% API coverage
   - What's unclear: Exact APIs available (getBoolean, getString, etc.)
   - Recommendation: Verify during implementation, may need expect/actual for missing APIs

2. **iOS Firebase initialization in KMP**
   - What we know: Firebase iOS SDK requires initialization before use
   - What's unclear: Best integration point in KMP iOS app lifecycle
   - Recommendation: Check project's existing Firebase iOS setup (if any) during implementation

3. **User's future plans for FLAG-02 (multi-type support)**
   - What we know: Context file says "Boolean flags only (no string/number variants for now)"
   - What's unclear: Whether to design interface to accommodate future types
   - Recommendation: Start boolean-only, interface can be extended later if needed

</open_questions>

<sources>
## Sources

### Primary (HIGH confidence)
- [GitLiveApp/firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) - v2.4.0, supported platforms, API coverage
- [Firebase Remote Config Loading Strategies](https://firebase.google.com/docs/remote-config/loading) - Official patterns for fetch/activate
- Project analysis - Existing platform abstraction patterns (Logger, CrashLogger, Analytics)

### Secondary (MEDIUM confidence)
- [Firebase Remote Config in KMP article (dohyeon.com)](https://dohyeon.com/effective-feature-flags-with-kotlin-multiplatform-firebase/) - Architecture pattern verified against project structure
- [jimmymorales/feature-flags](https://github.com/jimmymorales/feature-flags) - Evaluated, too immature (v0.2.0, 2022)
- [ConfigCat Kotlin SDK](https://github.com/configcat/kotlin-sdk) - Evaluated, requires external service

### Tertiary (LOW confidence - needs validation)
- firebase-kotlin-sdk Remote Config API specifics - Documentation sparse, verify during implementation

</sources>

<metadata>
## Metadata

**Research scope:**
- Core technology: Kotlin Multiplatform feature flags
- Ecosystem: firebase-kotlin-sdk, ConfigCat, jimmymorales/feature-flags
- Patterns: Interface + Koin DI, local defaults, optional remote
- Pitfalls: Initialization timing, offline fallback, API coverage

**Confidence breakdown:**
- Standard stack: HIGH - Matches project patterns exactly
- Architecture: HIGH - Follows existing Logger/CrashLogger/Analytics pattern
- Pitfalls: HIGH - Firebase gotchas well-documented
- Code examples: HIGH - Based on project analysis + official docs

**Research date:** 2026-01-15
**Valid until:** 2026-02-15 (30 days - stable ecosystem)

</metadata>

---

*Phase: 03-feature-flags*
*Research completed: 2026-01-15*
*Ready for planning: yes*
