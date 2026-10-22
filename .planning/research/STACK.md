# KMP/CMP Production Stack Research (January 2025)

> Research conducted: January 15, 2026
> Focus: Production-ready Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP) stack

---

## Executive Summary

**Kotlin Multiplatform has reached production stability.** With Compose Multiplatform 1.8.0 (May 2025), iOS support became stable, making KMP a complete solution for cross-platform mobile development. Major companies including Shopify, Forbes, CashApp, and JetBrains run KMP in production.

**Key Milestone**: Google officially supports KMP for sharing business logic between Android and iOS, and is actively adding KMP support to Jetpack libraries (Room, DataStore, Navigation, Paging, ViewModel).

---

## Core Technologies

| Technology | Version | Purpose | Confidence |
|------------|---------|---------|------------|
| **Kotlin** | 2.3.0 | Language & compiler | HIGH |
| **Compose Multiplatform** | 1.10.0 | Shared UI framework | HIGH |
| **Gradle** | 8.5+ | Build system | HIGH |
| **Android Gradle Plugin** | 8.13.2 | Android build tooling | HIGH |
| **KSP** | 2.3.0-1.0.31 | Annotation processing | HIGH |

### Version Compatibility Matrix

| Compose Multiplatform | Minimum Kotlin | Notes |
|----------------------|----------------|-------|
| 1.10.x | 2.1.20+ | Navigation 3, Hot Reload |
| 1.9.x | 2.1.20+ | Web beta, AGP 9.0 support |
| 1.8.x | 2.1.0+ | iOS stable, K2 transition |

**Source**: [JetBrains Compose Compatibility Guide](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) - HIGH confidence

---

## Supporting Libraries

### Networking

| Library | Version | Recommendation | Confidence |
|---------|---------|----------------|------------|
| **Ktor Client** | 3.3.3 | Primary choice for KMP | HIGH |
| Retrofit + OkHttp | 2.11.x | Android-only fallback | HIGH |

**Ktor Client Configuration Best Practices:**
- Use platform-specific engines: `OkHttp` or `CIO` for Android, `Darwin` or `CIO` for iOS
- Install essential plugins: `ContentNegotiation(Json)`, `HttpTimeout`, `Logging`
- Use `expect/actual` pattern for HttpClient factory
- Version 3.0+ switched to kotlinx-io (based on Okio)

```kotlin
// libs.versions.toml
[versions]
ktor = "3.3.3"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
```

**Sources**:
- [Ktor Documentation](https://ktor.io/docs/releases.html) - HIGH confidence
- [Ktor 3.3.0 Release Notes](https://ktor.io/docs/whats-new-330.html) - HIGH confidence

---

### Persistence

| Library | Version | Best For | Confidence |
|---------|---------|----------|------------|
| **SQLDelight** | 2.2.1 | SQL-first, mature KMP support | HIGH |
| **Room KMP** | 2.7.0+ | Jetpack ecosystem, annotation-based | HIGH |
| **DataStore** | 1.2.0 | Key-value preferences | HIGH |
| multiplatform-settings | 1.2.0+ | Simple settings (wraps DataStore) | MEDIUM |

**Recommendation**:
- **SQLDelight** for complex queries and SQL expertise
- **Room KMP** for teams familiar with Android Room
- **DataStore** for preferences and simple key-value storage

```kotlin
// SQLDelight setup
[versions]
sqldelight = "2.2.1"

[libraries]
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }

[plugins]
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

```kotlin
// DataStore setup
[versions]
datastore = "1.2.0"

[libraries]
datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
```

**Sources**:
- [SQLDelight GitHub](https://github.com/sqldelight/sqldelight/releases) - HIGH confidence
- [Android DataStore KMP Guide](https://developer.android.com/kotlin/multiplatform/datastore) - HIGH confidence
- [Room vs SQLDelight Comparison](https://medium.com/@muralivitt/database-solutions-for-kmp-cmp-sqldelight-vs-room-ea9a52c7bce7) - MEDIUM confidence

---

### Dependency Injection

| Library | Version | Recommendation | Confidence |
|---------|---------|----------------|------------|
| **Koin** | 4.1.1 | Primary choice for KMP | HIGH |
| Koin Annotations | 4.1.1 | Compile-time safety with KSP | HIGH |
| Hilt | N/A | Android-only, not KMP compatible | HIGH |

**Koin 4.x Features:**
- Built on Kotlin 2.0
- Full Compose Multiplatform support
- Koin Annotations for compile-time module generation
- WASM support

```kotlin
[versions]
koin = "4.1.1"

[libraries]
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
```

**Sources**:
- [Koin Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/) - HIGH confidence
- [Koin 4.1 Release Blog](https://blog.kotzilla.io/koin-4.1-is-here) - HIGH confidence

---

### Navigation

| Library | Version | Best For | Confidence |
|---------|---------|----------|------------|
| **Navigation Compose (Jetpack)** | 2.9.x | First-party, type-safe | HIGH |
| **Navigation 3** | Alpha | New architecture, coming 2025 | MEDIUM |
| **Voyager** | 1.1.0+ | Simple, minimal boilerplate | MEDIUM |
| **Decompose** | 3.0+ | Architecture separation, native UI support | MEDIUM |

**Recommendation**:
- **Navigation Compose (Jetpack)** for stability and first-party support
- **Navigation 3** when it reaches stable (currently alpha)
- **Voyager** for simpler projects preferring less boilerplate

**Navigation 3 Features** (Alpha):
- Type-safe, no XML graphs
- Developer-owned back stack (`SnapshotStateList<T>`)
- `NavDisplay` replaces `NavHost`
- Multi-layer navigation support
- Compose Multiplatform support via JetBrains fork

```kotlin
// Current stable navigation
[versions]
navigation = "2.9.1"

[libraries]
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }
```

**Sources**:
- [Jetpack Navigation 3 Announcement](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html) - HIGH confidence
- [Navigation 3 in CMP](https://johnoreilly.dev/posts/navigation3-cmp/) - MEDIUM confidence
- [Voyager GitHub](https://github.com/adrielcafe/voyager) - HIGH confidence

---

### Image Loading

| Library | Version | Recommendation | Confidence |
|---------|---------|----------------|------------|
| **Coil 3** | 3.3.0 | Primary choice, familiar API | HIGH |
| **Kamel** | 1.0.8 | Built for multiplatform, Ktor-native | MEDIUM |

**Recommendation**:
- **Coil 3** for teams familiar with Coil from Android
- **Kamel** for projects already using Ktor extensively

```kotlin
// Coil 3 setup
[versions]
coil = "3.3.0"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }
```

```kotlin
// Kamel setup (alternative)
[versions]
kamel = "1.0.8"

[libraries]
kamel = { module = "media.kamel:kamel-image-default", version.ref = "kamel" }
```

**Sources**:
- [Coil GitHub](https://github.com/coil-kt/coil) - HIGH confidence
- [Coil 3 Migration Guide](https://coil-kt.github.io/coil/upgrading_to_coil3/) - HIGH confidence
- [Kamel GitHub](https://github.com/Kamel-Media/Kamel) - HIGH confidence

---

### Serialization

| Library | Version | Purpose | Confidence |
|---------|---------|---------|------------|
| **kotlinx.serialization** | 1.9.0 (stable) | JSON/CBOR/ProtoBuf | HIGH |
| kotlinx.serialization | 1.10.0-RC | Latest features | MEDIUM |

```kotlin
[versions]
kotlin = "2.3.0"
serialization = "1.9.0"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

**Source**: [kotlinx.serialization GitHub](https://github.com/Kotlin/kotlinx.serialization) - HIGH confidence

---

### State Management

| Pattern | Implementation | Confidence |
|---------|---------------|------------|
| **ViewModel + StateFlow** | Primary recommendation | HIGH |
| Compose State (`remember`) | UI-specific state | HIGH |
| MutableStateFlow | Screen-level state in ViewModel | HIGH |

**Best Practices:**
- Use `StateFlow` for ViewModel state (decouples from Compose)
- Use `collectAsStateWithLifecycle()` in Composables
- Keep `_uiState` private, expose immutable `uiState`
- Use `derivedStateOf` for computed values

```kotlin
// Recommended pattern
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()
}

@Composable
fun MyScreen(viewModel: MyViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    // Use state
}
```

**Note**: For Compose Multiplatform, add `kotlinx-coroutines-swing` dependency for proper ViewModel coroutine support.

**Sources**:
- [JetBrains ViewModel Guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-viewmodel.html) - HIGH confidence
- [State Management in CMP](https://medium.com/@ignatiah.x/state-management-in-compose-multiplatform-cmp-a-beginners-guide-d374c0f3c975) - MEDIUM confidence

---

### Firebase Integration

| Library | Platform | Status | Confidence |
|---------|----------|--------|------------|
| **GitLive Firebase SDK** | KMP | Community-maintained | MEDIUM |
| Firebase Android SDK | Android | Official | HIGH |
| Firebase iOS SDK | iOS | Official (native) | HIGH |

**Current State:**
- No official KMP Firebase SDK from Google
- GitLive SDK provides multiplatform wrappers but has gaps (e.g., Crashlytics not available for JVM/Desktop)
- Recommended approach: Use platform-specific Firebase SDKs with expect/actual

```kotlin
// GitLive Firebase SDK (community)
implementation("dev.gitlive:firebase-auth:2.0.0")
implementation("dev.gitlive:firebase-firestore:2.0.0")
implementation("dev.gitlive:firebase-analytics:2.0.0")
implementation("dev.gitlive:firebase-crashlytics:2.0.0")
```

**Recommendation**: For production apps, use official Firebase SDKs per platform with expect/actual pattern until official KMP support arrives.

**Sources**:
- [GitLive Firebase SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk) - HIGH confidence
- [Firebase KMP Feature Request](https://firebase.uservoice.com/forums/948424-general/suggestions/46591717-support-kotlin-multiplatform-kmp-in-the-sdks) - HIGH confidence

---

### Additional Essential Libraries

| Library | Version | Purpose | Confidence |
|---------|---------|---------|------------|
| **kotlinx-datetime** | 0.7.1 | Date/time handling | HIGH |
| **kotlinx-coroutines** | 1.10.2 | Async operations | HIGH |
| **Napier** | 2.7.x | Multiplatform logging | MEDIUM |

```kotlin
[versions]
datetime = "0.7.1"
coroutines = "1.10.2"

[libraries]
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
```

---

## Development Tools

| Tool | Version | Purpose | Confidence |
|------|---------|---------|------------|
| **Android Studio** | Meerkat+ | IDE with KMP templates | HIGH |
| **Detekt** | 1.23.x | Static analysis | HIGH |
| **JaCoCo** | Latest | Code coverage | HIGH |
| **Compose Hot Reload** | Bundled (1.10+) | Fast UI iteration | HIGH |

---

## Alternatives Considered

### Navigation Alternatives

| Library | Verdict | Reason |
|---------|---------|--------|
| **Decompose** | Consider | Better for apps needing native UI alongside Compose |
| **Appyx** | Skip | Less active maintenance |
| **PreCompose** | Skip | Navigation Compose now official |

### Database Alternatives

| Library | Verdict | Reason |
|---------|---------|--------|
| **Realm** | Consider | Good offline-first, but proprietary |
| **ObjectBox** | Skip | Limited KMP support |

### DI Alternatives

| Library | Verdict | Reason |
|---------|---------|--------|
| **Kodein** | Consider | Viable but smaller community |
| **kotlin-inject** | Consider | Compile-time, but less mature for KMP |

---

## What NOT to Use

### Deprecated APIs and Libraries

| Item | Status | Replacement |
|------|--------|-------------|
| `com.android.library` for KMP | Deprecated AGP 9.0+ | `com.android.kotlin.multiplatform.library` |
| `androidLibrary{}` block | Deprecated | `android{}` block |
| `ios()`, `watchos()`, `tvos()` shortcuts | Deprecated | Explicit target declarations |
| `KotlinCompilation.source` | Deprecated | Standard source set configuration |
| `publishAllLibraryVariants()` | Deprecated | Explicit variant publishing |
| Legacy library binaries | Removed in Kotlin 2.0 | Migrate dependencies to Kotlin 2.0+ |
| `org.jetbrains.compose.ui.tooling.preview.Preview` | Deprecated | `androidx.compose.ui.tooling.preview.Preview` |
| Compose Gradle plugin dependency aliases | Deprecated in 1.10 | Direct library references |

### Libraries to Avoid

| Library | Reason |
|---------|--------|
| **Retrofit** for shared code | Android-only, use Ktor for KMP |
| **Hilt** | Android-only, use Koin for KMP |
| **Gson** | Use kotlinx.serialization for KMP |
| **Java time APIs** in common | Not available in common code |

**Sources**:
- [Kotlin 2.2 Compatibility Guide](https://kotlinlang.org/docs/compatibility-guide-22.html) - HIGH confidence
- [KMP Compatibility Guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html) - HIGH confidence
- [Compose Multiplatform 1.10 What's New](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html) - HIGH confidence

---

## Recommended libs.versions.toml (Production 2025)

```toml
[versions]
# Core
kotlin = "2.3.0"
agp = "8.13.2"
composeMultiplatform = "1.10.0"
ksp = "2.3.0-1.0.31"

# Android SDK
android-compileSdk = "36"
android-minSdk = "24"
android-targetSdk = "35"

# AndroidX / Jetpack
androidx-activity = "1.10.1"
androidx-lifecycle = "2.9.6"
navigation = "2.9.1"
datastore = "1.2.0"

# Networking
ktor = "3.3.3"

# Persistence
sqldelight = "2.2.1"

# DI
koin = "4.1.1"

# Serialization
serialization = "1.9.0"

# Image Loading
coil = "3.3.0"
# OR kamel = "1.0.8"

# Utilities
datetime = "0.7.1"
coroutines = "1.10.2"

# Testing
junit = "4.13.2"
mockk = "1.13.x"

# Code Quality
detekt = "1.23.8"

# Firebase (Android)
firebaseBom = "33.15.0"

[libraries]
# Kotlin
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin" }

# AndroidX
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "androidx-activity" }
androidx-lifecycle-viewmodel = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel", version.ref = "androidx-lifecycle" }
androidx-lifecycle-runtime-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose", version.ref = "androidx-lifecycle" }
navigation-compose = { module = "org.jetbrains.androidx.navigation:navigation-compose", version.ref = "navigation" }
datastore = { module = "androidx.datastore:datastore", version.ref = "datastore" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }

# SQLDelight
sqldelight-android = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }

# Serialization
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

# Image Loading
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

# Utilities
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

# Firebase
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebaseBom" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics" }

[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

---

## Sources Summary

### Official Documentation (HIGH Confidence)
- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/)
- [JetBrains Compose Releases](https://github.com/JetBrains/compose-multiplatform/releases)
- [Android KMP Guide](https://developer.android.com/kotlin/multiplatform)
- [Ktor Documentation](https://ktor.io/docs/)
- [Koin Documentation](https://insert-koin.io/docs/)

### Official Blog Posts (HIGH Confidence)
- [Compose Multiplatform 1.8.0 Release](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/)
- [Compose Multiplatform 1.10.0 Release](https://blog.jetbrains.com/kotlin/2026/01/compose-multiplatform-1-10-0/)
- [KMP Roadmap August 2025](https://blog.jetbrains.com/kotlin/2025/08/kmp-roadmap-aug-2025/)
- [Android KMP Announcements Google I/O 2025](https://android-developers.googleblog.com/2025/05/android-kotlin-multiplatform-google-io-kotlinconf-2025.html)
- [Jetpack Navigation 3 Announcement](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html)

### Community Resources (MEDIUM Confidence)
- [klibs.io](https://klibs.io/) - Official Kotlin Multiplatform library catalog
- Medium articles and developer blogs (cited inline)

---

## Current Project Status

The existing project template at `/Users/nathanstasin/Documents/project_setup/mobile/gradle/libs.versions.toml` already uses many recommended versions:

| Component | Current | Recommended | Status |
|-----------|---------|-------------|--------|
| Kotlin | 2.3.0 | 2.3.0 | Current |
| Compose Multiplatform | 1.9.3 | 1.10.0 | Update available |
| Ktor | 3.3.3 | 3.3.3 | Current |
| Koin | 4.1.1 | 4.1.1 | Current |
| kotlinx-serialization | 1.9.0 | 1.9.0 | Current |
| Image Loading | Kamel 1.0.8 | Kamel 1.0.8 or Coil 3.3.0 | Current |
| Navigation | 2.9.1 | 2.9.1 | Current |

**Recommended Updates:**
1. Update Compose Multiplatform to 1.10.0 for Navigation 3 support and Hot Reload
2. Consider adding Coil 3 as an alternative to Kamel
3. Add SQLDelight 2.2.1 for database persistence
4. Add DataStore 1.2.0 for preferences

---

*Last updated: January 15, 2026*
