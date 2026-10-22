# KMP + Compose Multiplatform Architecture Research

**Research Date:** January 2026
**Domain:** Kotlin Multiplatform Mobile Development
**Focus:** Production patterns for solo developers building Android + iOS apps

---

## System Overview

```
+-------------------------------------------------------------------------+
|                           APPLICATION LAYER                              |
+-------------------------------------------------------------------------+
|                                                                          |
|   +------------------+     +------------------+     +------------------+  |
|   |    Android App   |     |     iOS App      |     |   Desktop App    |  |
|   |  (androidMain)   |     |    (iosMain)     |     |    (jvmMain)     |  |
|   +--------+---------+     +--------+---------+     +--------+---------+  |
|            |                        |                        |           |
|            +------------------------+------------------------+           |
|                                     |                                    |
+-------------------------------------------------------------------------+
|                          COMPOSE MULTIPLATFORM                           |
+-------------------------------------------------------------------------+
|                                     |                                    |
|   +----------------------------------------------------------------+    |
|   |                        composeApp Module                        |    |
|   |                                                                 |    |
|   |  +---------------------------+  +---------------------------+   |    |
|   |  |     Presentation Layer    |  |      Platform Layer       |   |    |
|   |  |                           |  |                           |   |    |
|   |  |  +--------------------+   |  |  +--------------------+   |   |    |
|   |  |  | Screens (Compose)  |   |  |  | expect/actual      |   |   |    |
|   |  |  +--------------------+   |  |  | - Logger           |   |   |    |
|   |  |  | ViewModels         |   |  |  | - Analytics        |   |   |    |
|   |  |  +--------------------+   |  |  | - CrashLogger      |   |   |    |
|   |  |  | UI State Classes   |   |  |  | - AppConfig        |   |   |    |
|   |  |  +--------------------+   |  |  +--------------------+   |   |    |
|   |  +---------------------------+  +---------------------------+   |    |
|   |                                                                 |    |
|   |  +---------------------------+  +---------------------------+   |    |
|   |  |       Domain Layer        |  |        Data Layer         |   |    |
|   |  |                           |  |                           |   |    |
|   |  |  +--------------------+   |  |  +--------------------+   |   |    |
|   |  |  | Use Cases          |   |  |  | Repositories       |   |   |    |
|   |  |  | (Optional)         |   |  |  +--------------------+   |   |    |
|   |  |  +--------------------+   |  |  | Data Sources       |   |   |    |
|   |  |  | Domain Models      |   |  |  | - Remote (Ktor)    |   |   |    |
|   |  |  +--------------------+   |  |  | - Local (SQLDelight|   |   |    |
|   |  |  | Repository         |   |  |  |   / Room)          |   |   |    |
|   |  |  | Interfaces         |   |  |  +--------------------+   |   |    |
|   |  |  +--------------------+   |  |  | DTOs & Mappers     |   |   |    |
|   |  +---------------------------+  +--------------------+------+   |    |
|   |                                                                 |    |
|   |  +--------------------------------------------------------------+   |
|   |  |                    DI Module (Koin)                           |   |
|   |  |  - DataModule (repositories, data sources, APIs)              |   |
|   |  |  - DomainModule (use cases)                                   |   |
|   |  |  - PresentationModule (ViewModels)                            |   |
|   |  |  - PlatformModule (expect/actual implementations)             |   |
|   |  +--------------------------------------------------------------+   |
|   +----------------------------------------------------------------+    |
|                                                                          |
+-------------------------------------------------------------------------+
```

---

## Component Responsibilities

| Component | Layer | Responsibility | Platform Scope |
|-----------|-------|----------------|----------------|
| **Screens** | Presentation | Render UI, collect user events | Common (Compose) |
| **ViewModels** | Presentation | Manage UI state, handle business logic delegation | Common |
| **UI State** | Presentation | Immutable data classes representing screen state | Common |
| **Use Cases** | Domain | Encapsulate reusable business logic | Common |
| **Domain Models** | Domain | Pure Kotlin entities, business rules | Common |
| **Repository Interfaces** | Domain | Define data access contracts | Common |
| **Repository Impl** | Data | Implement data access, coordinate sources | Common |
| **Remote Data Source** | Data | Network API calls (Ktor) | Common |
| **Local Data Source** | Data | Database operations (SQLDelight/Room) | Common |
| **DTOs** | Data | Data transfer objects for serialization | Common |
| **Logger** | Platform | Platform-specific logging | expect/actual |
| **Analytics** | Platform | Event tracking (Firebase/custom) | expect/actual |
| **CrashLogger** | Platform | Crash reporting (Crashlytics) | expect/actual |
| **AppConfig** | Platform | Environment configuration | expect/actual |

---

## Recommended Project Structure

```
project_setup/
├── mobile/
│   ├── composeApp/
│   │   └── src/
│   │       ├── commonMain/
│   │       │   └── kotlin/
│   │       │       └── com/example/myproject/
│   │       │           │
│   │       │           ├── App.kt                    # Main entry point
│   │       │           │
│   │       │           ├── presentation/             # UI LAYER
│   │       │           │   ├── screens/
│   │       │           │   │   ├── home/
│   │       │           │   │   │   ├── HomeScreen.kt
│   │       │           │   │   │   ├── HomeViewModel.kt
│   │       │           │   │   │   └── HomeUiState.kt
│   │       │           │   │   ├── user/
│   │       │           │   │   │   ├── UserScreen.kt
│   │       │           │   │   │   ├── UserViewModel.kt
│   │       │           │   │   │   └── UserUiState.kt
│   │       │           │   │   └── settings/
│   │       │           │   │       └── ...
│   │       │           │   ├── components/           # Reusable UI components
│   │       │           │   │   ├── LoadingIndicator.kt
│   │       │           │   │   ├── ErrorView.kt
│   │       │           │   │   └── EmptyStateView.kt
│   │       │           │   ├── navigation/           # Navigation setup
│   │       │           │   │   ├── AppNavHost.kt
│   │       │           │   │   └── Routes.kt
│   │       │           │   └── common/               # Base classes
│   │       │           │       ├── BaseViewModel.kt
│   │       │           │       └── UiEventHandler.kt
│   │       │           │
│   │       │           ├── domain/                   # DOMAIN LAYER (optional)
│   │       │           │   ├── model/                # Domain entities
│   │       │           │   │   ├── User.kt
│   │       │           │   │   └── Settings.kt
│   │       │           │   ├── repository/           # Repository interfaces
│   │       │           │   │   ├── UserRepository.kt
│   │       │           │   │   └── SettingsRepository.kt
│   │       │           │   └── usecase/              # Use cases
│   │       │           │       ├── GetUserUseCase.kt
│   │       │           │       ├── UpdateUserUseCase.kt
│   │       │           │       └── ValidateEmailUseCase.kt
│   │       │           │
│   │       │           ├── data/                     # DATA LAYER
│   │       │           │   ├── repository/           # Repository implementations
│   │       │           │   │   ├── UserRepositoryImpl.kt
│   │       │           │   │   └── SettingsRepositoryImpl.kt
│   │       │           │   ├── datasource/
│   │       │           │   │   ├── remote/           # Network APIs
│   │       │           │   │   │   ├── UserApi.kt
│   │       │           │   │   │   ├── UserRemoteDataSource.kt
│   │       │           │   │   │   └── dto/          # Data transfer objects
│   │       │           │   │   │       ├── UserDto.kt
│   │       │           │   │   │       └── CreateUserRequest.kt
│   │       │           │   │   └── local/            # Local storage
│   │       │           │   │       ├── UserLocalDataSource.kt
│   │       │           │   │       └── dao/          # Database DAOs
│   │       │           │   │           └── UserDao.kt
│   │       │           │   ├── mapper/               # DTO <-> Domain mappers
│   │       │           │   │   └── UserMapper.kt
│   │       │           │   └── cache/                # Caching utilities
│   │       │           │       └── CachePolicy.kt
│   │       │           │
│   │       │           ├── platform/                 # PLATFORM ABSTRACTIONS
│   │       │           │   ├── Logger.kt             # expect fun createLogger()
│   │       │           │   ├── Analytics.kt          # expect fun createAnalytics()
│   │       │           │   ├── CrashLogger.kt        # expect fun createCrashLogger()
│   │       │           │   └── AppConfig.kt          # expect class AppConfig
│   │       │           │
│   │       │           ├── di/                       # DEPENDENCY INJECTION
│   │       │           │   ├── AppModule.kt          # Main Koin module
│   │       │           │   ├── DataModule.kt
│   │       │           │   ├── DomainModule.kt
│   │       │           │   ├── PresentationModule.kt
│   │       │           │   └── PlatformModule.kt     # expect val platformModule
│   │       │           │
│   │       │           └── util/                     # SHARED UTILITIES
│   │       │               ├── Result.kt             # Result wrapper
│   │       │               ├── Constants.kt
│   │       │               └── Extensions.kt
│   │       │
│   │       ├── androidMain/
│   │       │   └── kotlin/
│   │       │       └── com/example/myproject/
│   │       │           ├── platform/                 # actual implementations
│   │       │           │   ├── Logger.android.kt
│   │       │           │   ├── Analytics.android.kt
│   │       │           │   ├── CrashLogger.android.kt
│   │       │           │   └── AppConfig.android.kt
│   │       │           └── di/
│   │       │               └── PlatformModule.android.kt
│   │       │
│   │       ├── iosMain/
│   │       │   └── kotlin/
│   │       │       └── com/example/myproject/
│   │       │           ├── platform/                 # actual implementations
│   │       │           │   ├── Logger.ios.kt
│   │       │           │   ├── Analytics.ios.kt
│   │       │           │   ├── CrashLogger.ios.kt
│   │       │           │   └── AppConfig.ios.kt
│   │       │           └── di/
│   │       │               └── PlatformModule.ios.kt
│   │       │
│   │       ├── commonTest/                           # Shared tests
│   │       │   └── kotlin/
│   │       │       └── com/example/myproject/
│   │       │           ├── data/
│   │       │           │   └── repository/
│   │       │           │       └── UserRepositoryTest.kt
│   │       │           ├── domain/
│   │       │           │   └── usecase/
│   │       │           │       └── GetUserUseCaseTest.kt
│   │       │           └── presentation/
│   │       │               └── UserViewModelTest.kt
│   │       │
│   │       ├── androidTest/                          # Android-specific tests
│   │       └── iosTest/                              # iOS-specific tests
│   │
│   └── iosApp/                                       # iOS entry point
│       └── iosApp/
│           ├── ContentView.swift
│           ├── iOSApp.swift
│           └── Info.plist
│
├── gradle/
│   └── libs.versions.toml                            # Version catalog
│
└── build.gradle.kts
```

---

## Architectural Patterns

### Pattern 1: Repository with Offline-First Caching

**Confidence:** HIGH
**Sources:** [Android Data Layer](https://developer.android.com/topic/architecture/data-layer), [Carrion.dev KMP Architecture](https://carrion.dev/en/posts/kmp-architecture/), [DataSources & Repository Patterns](https://carrion.dev/en/posts/datasources-repository-patterns/)

The repository pattern serves as the single source of truth for data, coordinating between remote and local data sources.

```
                    +-------------------+
                    |    Repository     |
                    |   (Interface)     |
                    +--------+----------+
                             |
              +--------------+--------------+
              |                             |
    +---------v---------+         +---------v---------+
    | Remote DataSource |         | Local DataSource  |
    |   (Ktor Client)   |         | (SQLDelight/Room) |
    +-------------------+         +-------------------+
```

**Implementation Pattern:**

```kotlin
// Domain layer - repository interface
interface UserRepository {
    fun getUsers(): Flow<List<User>>
    suspend fun refreshUsers()
    suspend fun getUserById(id: String): User?
}

// Data layer - repository implementation
class UserRepositoryImpl(
    private val remoteDataSource: UserRemoteDataSource,
    private val localDataSource: UserLocalDataSource,
    private val mapper: UserMapper
) : UserRepository {

    override fun getUsers(): Flow<List<User>> = flow {
        // 1. Emit cached data first (offline-first)
        val cachedUsers = localDataSource.getUsers()
        emit(cachedUsers.map { mapper.toDomain(it) })

        // 2. Fetch fresh data from network
        try {
            val remoteUsers = remoteDataSource.getUsers()
            localDataSource.saveUsers(remoteUsers)
            emit(remoteUsers.map { mapper.toDomain(it) })
        } catch (e: Exception) {
            // Network error - cached data already emitted
            // Optionally emit error state
        }
    }

    override suspend fun refreshUsers() {
        val remoteUsers = remoteDataSource.getUsers()
        localDataSource.saveUsers(remoteUsers)
    }
}
```

**Key Principles:**
- Local data source is the source of truth
- Emit cached data immediately, then update from network
- Handle network errors gracefully (cached data still available)
- Use timestamp-based cache invalidation for freshness

---

### Pattern 2: Expect/Actual for Platform Abstractions

**Confidence:** HIGH
**Sources:** [ProAndroidDev - Expect/Actual Explained](https://proandroiddev.com/expect-actual-mechanism-in-kotlin-multiplatform-explained-a91e7d85af4e), [Koin KMP Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/)

Use `expect/actual` for platform-specific functionality that requires native APIs. Reserve interfaces + DI for runtime flexibility.

**When to use expect/actual:**
- Platform-specific APIs (Android Context, iOS UIDevice)
- Native library wrappers (Firebase, Crashlytics)
- Performance-critical native code
- Compile-time type safety needed

**When to use interfaces + DI:**
- Runtime behavior swapping
- Testing with mocks/fakes
- Plugin-style architecture
- Default implementations needed

**Implementation Pattern:**

```kotlin
// commonMain - expect declaration
expect fun createLogger(): Logger

expect val platformModule: Module

// androidMain - actual implementation
actual fun createLogger(): Logger = AndroidLogger()

actual val platformModule = module {
    single<Logger> { createLogger() }
    single<Analytics> { FirebaseAnalytics(get()) }
    single<CrashLogger> { CrashlyticsLogger() }
}

// iosMain - actual implementation
actual fun createLogger(): Logger = IOSLogger()

actual val platformModule = module {
    single<Logger> { createLogger() }
    single<Analytics> { IOSAnalytics() }
    single<CrashLogger> { IOSCrashLogger() }
}
```

**Best Practice:** Combine both patterns - use `expect/actual` for the platform module definition, then use interfaces within those modules for testability.

---

### Pattern 3: Shared ViewModel with Lifecycle-Aware Scoping

**Confidence:** HIGH
**Sources:** [JetBrains Common ViewModel](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-viewmodel.html), [Koin Compose Multiplatform](https://insert-koin.io/docs/reference/koin-compose/compose/)

ViewModels can be shared across platforms using the official Compose Multiplatform lifecycle library.

```kotlin
// Dependency (libs.versions.toml)
// lifecycle-viewmodel-compose = { module = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }

// ViewModel in commonMain
class UserViewModel(
    private val userRepository: UserRepository,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUsers()
                .catch { e ->
                    logger.e("UserViewModel", "Failed to load users", e)
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
                .collect { users ->
                    _uiState.update { it.copy(users = users, isLoading = false) }
                }
        }
    }
}

// Screen using ViewModel
@Composable
fun UserScreen(
    viewModel: UserViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    UserContent(
        uiState = uiState,
        onRefresh = viewModel::loadUsers
    )
}
```

**Important Note:** On non-JVM platforms, you cannot use `viewModel()` without parameters due to reflection limitations. Always provide an initializer or use Koin's `koinViewModel()`.

---

### Pattern 4: Koin Module Organization by Layer

**Confidence:** HIGH
**Sources:** [Koin Official Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/), [Koin DI in KMP - Medium](https://medium.com/@sahar.asadian90/koin-dependency-injection-in-kotlin-multiplatform-kmp-eb9c25824a9a)

Organize Koin modules by architectural layer for maintainability and clear dependency direction.

```kotlin
// DataModule.kt
val dataModule = module {
    // HTTP Client
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { isLenient = true })
            }
        }
    }

    // APIs
    single { UserApi(get()) }

    // Data Sources
    single { UserRemoteDataSource(get()) }
    single { UserLocalDataSource(get()) }

    // Repositories
    single<UserRepository> { UserRepositoryImpl(get(), get(), get()) }
}

// DomainModule.kt
val domainModule = module {
    // Use Cases
    factory { GetUserUseCase(get()) }
    factory { UpdateUserUseCase(get()) }
    factory { ValidateEmailUseCase() }
}

// PresentationModule.kt
val presentationModule = module {
    // ViewModels (use viewModel { } for lifecycle-aware scoping)
    viewModel { UserViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}

// AppModule.kt - combines all modules
val appModule = module {
    includes(
        platformModule,      // Platform-specific (expect/actual)
        dataModule,
        domainModule,
        presentationModule
    )
}

// Application initialization
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModule)
    }
}
```

**Best Practices:**
- Use `single { }` for singletons (repositories, data sources)
- Use `factory { }` for use cases (stateless, created per use)
- Use `viewModel { }` for ViewModels (lifecycle-aware)
- Use `includes()` to compose modules
- Keep platform module separate for expect/actual

---

## Data Flow Diagrams

### Unidirectional Data Flow (UDF)

```
+------------------+      Events        +------------------+
|                  | -----------------> |                  |
|    Composable    |                    |    ViewModel     |
|     (Screen)     |                    |                  |
|                  | <----------------- |                  |
+------------------+      UI State      +------------------+
        ^                                       |
        |                                       |
        |              StateFlow                |
        +---------------------------------------+

Example Flow:
1. User taps "Refresh" button
2. Screen calls viewModel.refresh()
3. ViewModel updates state: isLoading = true
4. ViewModel calls repository.getUsers()
5. Repository fetches data, returns Flow
6. ViewModel collects Flow, updates state
7. Screen recomposes with new state
```

### Repository Data Flow

```
+-------------+     +----------------+     +------------------+
|   Screen    | --> |   ViewModel    | --> |   Repository     |
+-------------+     +----------------+     +--------+---------+
                                                    |
                    +-------------------------------+
                    |
          +---------v---------+
          |   Check Cache     |
          +-------------------+
                    |
         +----------+----------+
         |                     |
    Cache Hit              Cache Miss
         |                     |
         v                     v
  +-------------+       +----------------+
  | Return Local|       | Fetch Remote   |
  |    Data     |       |     Data       |
  +-------------+       +--------+-------+
                                 |
                        +--------v-------+
                        | Update Local   |
                        |    Cache       |
                        +--------+-------+
                                 |
                        +--------v-------+
                        | Return Fresh   |
                        |    Data        |
                        +----------------+
```

### Navigation Architecture

```
+------------------------------------------------------------------+
|                         NavHost                                   |
|  +-----------------------------------------------------------+   |
|  |                                                           |   |
|  |   +-------------+    +-------------+    +-------------+   |   |
|  |   |    Home     | -> |    List     | -> |   Detail    |   |   |
|  |   |   Screen    |    |   Screen    |    |   Screen    |   |   |
|  |   +-------------+    +-------------+    +-------------+   |   |
|  |         |                                                 |   |
|  |         v                                                 |   |
|  |   +-------------+                                         |   |
|  |   |  Settings   |                                         |   |
|  |   |   Screen    |                                         |   |
|  |   +-------------+                                         |   |
|  |                                                           |   |
|  +-----------------------------------------------------------+   |
|                                                                   |
|  Navigation handled by:                                          |
|  - Jetpack Compose Navigation (multiplatform)                    |
|  - Type-safe route arguments                                     |
|  - Deep linking support                                          |
|  - Platform back gesture handling (iOS swipe, Esc on desktop)   |
+------------------------------------------------------------------+
```

---

## Scaling Considerations

### For Solo Developers (Current State)

**Confidence:** HIGH
**Source:** [Carrion.dev KMP Architecture](https://carrion.dev/en/posts/kmp-architecture/)

A single `composeApp` module with package-based organization is sufficient for solo developers:

```
composeApp/
└── src/commonMain/kotlin/com/example/
    ├── presentation/
    ├── domain/
    ├── data/
    ├── platform/
    └── di/
```

**Benefits:**
- Simple build configuration
- Fast incremental compilation
- Easy to navigate and understand
- No module dependency management overhead

### Scaling to Multi-Module (10+ features)

**Confidence:** MEDIUM
**Sources:** [KMP Modularization Guide](https://medium.com/@himanshugaur684/kmp-modularization-ultimate-guide-with-real-world-app-cdb5be94d096), [KMP Scalability Challenges](https://proandroiddev.com/kotlin-multiplatform-scalability-challenges-on-a-large-project-b3140e12da9d)

When the app grows significantly (10+ distinct features, multiple developers), consider modularization:

```
project/
├── core/
│   ├── core-common/          # Shared utilities
│   ├── core-network/         # Ktor client setup
│   ├── core-database/        # SQLDelight/Room setup
│   ├── core-ui/              # Shared Composables
│   └── core-platform/        # Platform abstractions
│
├── feature/
│   ├── feature-home/
│   │   ├── data/
│   │   ├── domain/
│   │   └── presentation/
│   ├── feature-user/
│   │   └── ...
│   └── feature-settings/
│       └── ...
│
├── shared/                   # Umbrella module for iOS
│   └── build.gradle.kts      # Exports all modules
│
└── app/
    ├── androidApp/
    └── iosApp/
```

**iOS Umbrella Module Pattern:**

For iOS, create an umbrella module that depends on all feature modules:

```kotlin
// shared/build.gradle.kts
kotlin {
    ios {
        binaries.framework {
            baseName = "shared"
            export(project(":feature:feature-home"))
            export(project(":feature:feature-user"))
            // ... all feature modules
        }
    }
}
```

**Warning:** Avoid exposing multiple frameworks to iOS - use a single umbrella framework to prevent dependency duplication.

### Build Performance Tips

**Confidence:** MEDIUM
**Source:** [KMP Scalability Challenges](https://proandroiddev.com/kotlin-multiplatform-scalability-challenges-on-a-large-project-b3140e12da9d)

- Use `build-logic` convention plugins to reduce Gradle boilerplate
- Consider publishing KMP library to local Maven for faster iOS builds
- Enable Gradle build cache and configuration cache
- Use `--parallel` for multi-module builds

---

## Anti-Patterns to Avoid

### 1. Putting Business Logic in expect/actual

**Confidence:** HIGH
**Source:** [KMP Anti-Patterns - Medium](https://medium.com/@karelvdmmisc/my-journey-with-kotlin-multiplatform-mobile-pitfalls-anti-patterns-and-solutions-525df7058018)

```kotlin
// BAD - Business logic in platform-specific code
// commonMain
expect fun calculateUserScore(user: User): Int

// androidMain
actual fun calculateUserScore(user: User): Int {
    // Complex business logic here - BAD!
    return user.posts * 10 + user.followers * 5
}

// GOOD - Business logic in common code
// commonMain
class ScoreCalculator {
    fun calculateUserScore(user: User): Int {
        return user.posts * 10 + user.followers * 5
    }
}

// Use expect/actual only for platform APIs
expect fun getDeviceInfo(): String
```

### 2. Mixing Platform-Specific Code in Shared Modules

**Confidence:** HIGH
**Source:** [Koin KMP Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/)

```kotlin
// BAD - Platform-specific imports in commonMain
import android.util.Log  // Won't compile on iOS!

class MyClass {
    fun logMessage(msg: String) {
        Log.d("TAG", msg)
    }
}

// GOOD - Use platform abstraction
// commonMain
interface Logger {
    fun d(tag: String, message: String)
}

// Inject via DI
class MyClass(private val logger: Logger) {
    fun logMessage(msg: String) {
        logger.d("TAG", msg)
    }
}
```

### 3. Overusing GlobalScope in ViewModels

**Confidence:** HIGH
**Source:** [Kotlin Anti-Patterns](https://medium.com/@a.poplawski96/donts-in-modern-android-kotlin-development-bad-practices-anti-patterns-chapter-i-d38cba2f5f7d)

```kotlin
// BAD - GlobalScope leaks coroutines
class UserViewModel : ViewModel() {
    fun loadUsers() {
        GlobalScope.launch {  // BAD!
            // This coroutine outlives the ViewModel
        }
    }
}

// GOOD - Use viewModelScope
class UserViewModel : ViewModel() {
    fun loadUsers() {
        viewModelScope.launch {  // Automatically cancelled
            // Safe coroutine tied to ViewModel lifecycle
        }
    }
}
```

### 4. Underestimating iOS-Specific Work

**Confidence:** HIGH
**Source:** [KMP Anti-Patterns - Medium](https://medium.com/@karelvdmmisc/my-journey-with-kotlin-multiplatform-mobile-pitfalls-anti-patterns-and-solutions-525df7058018)

A common misconception is that iOS only needs a thin Swift UI layer. In reality:
- iOS requires platform-specific navigation handling
- Apple's UX conventions differ from Android
- Swift/Kotlin bridging requires careful type mapping
- iOS debugging tools for shared code are less mature

**Mitigation:**
- Budget time for iOS-specific polish
- Test on real iOS devices early
- Use interface-based platform abstractions
- Document expected iOS behavior

### 5. Mutable Shared State Without Synchronization

**Confidence:** HIGH
**Source:** [Kotlin Concurrency Anti-Patterns](https://softwarepatternslexicon.com/patterns-kotlin/17/)

```kotlin
// BAD - Mutable shared state without protection
class UserRepository {
    private val cache = mutableMapOf<String, User>()  // Not thread-safe!

    suspend fun getUser(id: String): User {
        return cache.getOrPut(id) { fetchFromNetwork(id) }
    }
}

// GOOD - Use synchronization
class UserRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, User>()

    suspend fun getUser(id: String): User = mutex.withLock {
        cache.getOrPut(id) { fetchFromNetwork(id) }
    }
}

// BETTER - Use StateFlow for reactive data
class UserRepository {
    private val _users = MutableStateFlow<Map<String, User>>(emptyMap())
    val users: StateFlow<Map<String, User>> = _users.asStateFlow()
}
```

### 6. Ignoring Clean Architecture Boundaries

**Confidence:** HIGH
**Source:** [Clean Architecture Mistakes - Medium](https://medium.com/@takendra.saraswat224/5-anti-patterns-or-mistakes-developers-make-when-implementing-clean-architecture-in-android-apps-b3e80ec744fb)

```kotlin
// BAD - Domain layer knows about framework types
// domain/usecase/GetUserUseCase.kt
class GetUserUseCase {
    suspend fun execute(): LiveData<User> {  // BAD! Framework type in domain
        // ...
    }
}

// GOOD - Domain layer uses pure Kotlin types
class GetUserUseCase(private val repository: UserRepository) {
    suspend operator fun invoke(): Flow<User> {  // Pure Kotlin
        return repository.getUser()
    }
}
```

---

## Integration Points

### 1. Firebase Integration (Analytics, Crashlytics)

**Confidence:** HIGH
**Sources:** [Firebase Integration in KMP - Medium](https://levelup.gitconnected.com/firebase-integration-in-kotlin-multiplatform-6c45e15d234a), [Kermit and Crashlytics - Touchlab](https://touchlab.co/kermit-and-crashlytics)

Firebase requires platform-specific initialization but can be abstracted for common usage:

```kotlin
// commonMain - interface
interface Analytics {
    fun logEvent(name: String, params: Map<String, Any>? = null)
    fun setUserId(userId: String?)
}

// androidMain - Firebase implementation
class FirebaseAnalyticsImpl(
    private val firebaseAnalytics: FirebaseAnalytics
) : Analytics {
    override fun logEvent(name: String, params: Map<String, Any>?) {
        val bundle = Bundle().apply {
            params?.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    // ...
                }
            }
        }
        firebaseAnalytics.logEvent(name, bundle)
    }
}

// iosMain - placeholder or CocoaPods Firebase
class IOSAnalyticsImpl : Analytics {
    override fun logEvent(name: String, params: Map<String, Any>?) {
        // Use Firebase iOS SDK via CocoaPods or Swift interop
        // Or log to console for development
        println("Analytics: $name - $params")
    }
}
```

### 2. Ktor Client Configuration

**Confidence:** HIGH
**Source:** [Koin KMP Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/)

```kotlin
// commonMain - shared Ktor configuration
val networkModule = module {
    single<HttpClient> {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        get<Logger>().d("Ktor", message)
                    }
                }
                level = LogLevel.INFO
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
            }
        }
    }
}
```

### 3. Navigation with Jetpack Compose Navigation (Multiplatform)

**Confidence:** HIGH
**Source:** [Navigation in Compose - Kotlin Docs](https://kotlinlang.org/docs/multiplatform/compose-navigation.html)

```kotlin
// commonMain
// Routes.kt
sealed class Route(val path: String) {
    object Home : Route("home")
    object UserList : Route("users")
    data class UserDetail(val userId: String) : Route("users/$userId")
    object Settings : Route("settings")
}

// AppNavHost.kt
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.path
    ) {
        composable(Route.Home.path) {
            HomeScreen(
                onNavigateToUsers = { navController.navigate(Route.UserList.path) },
                onNavigateToSettings = { navController.navigate(Route.Settings.path) }
            )
        }

        composable(Route.UserList.path) {
            UserListScreen(
                onUserClick = { userId ->
                    navController.navigate(Route.UserDetail(userId).path)
                }
            )
        }

        composable("users/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            UserDetailScreen(userId = userId)
        }
    }
}
```

### 4. SQLDelight for Local Database

**Confidence:** MEDIUM
**Source:** [KMP Architecture Best Practices - Carrion.dev](https://carrion.dev/en/posts/kmp-architecture/)

```kotlin
// commonMain/sqldelight/com/example/User.sq
CREATE TABLE UserEntity (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    updatedAt INTEGER NOT NULL
);

getAll:
SELECT * FROM UserEntity ORDER BY name;

getById:
SELECT * FROM UserEntity WHERE id = ?;

insert:
INSERT OR REPLACE INTO UserEntity(id, name, email, updatedAt) VALUES (?, ?, ?, ?);

deleteAll:
DELETE FROM UserEntity;

// Generated code provides type-safe queries
class UserLocalDataSource(private val database: AppDatabase) {

    fun getUsers(): Flow<List<UserEntity>> {
        return database.userQueries.getAll().asFlow().mapToList(Dispatchers.IO)
    }

    suspend fun saveUsers(users: List<UserEntity>) {
        database.transaction {
            users.forEach { user ->
                database.userQueries.insert(
                    user.id, user.name, user.email, user.updatedAt
                )
            }
        }
    }
}
```

---

## Testing Architecture

**Confidence:** HIGH
**Sources:** [KMP Testing Guide 2025](https://www.kmpship.app/blog/kotlin-multiplatform-testing-guide-2025), [Kotlin Multiplatform Test Tutorial](https://kotlinlang.org/docs/multiplatform/multiplatform-run-tests.html)

### Test Source Set Strategy

```
composeApp/src/
├── commonTest/          # Runs on ALL platforms
│   └── kotlin/
│       └── com/example/
│           ├── data/
│           │   └── UserRepositoryTest.kt
│           ├── domain/
│           │   └── GetUserUseCaseTest.kt
│           └── presentation/
│               └── UserViewModelTest.kt
│
├── androidTest/         # Android-specific tests
│   └── kotlin/
│       └── com/example/
│           └── platform/
│               └── AndroidLoggerTest.kt
│
└── iosTest/             # iOS-specific tests
    └── kotlin/
        └── com/example/
            └── platform/
                └── IOSLoggerTest.kt
```

### Testing Strategy by Layer

| Layer | Test Location | What to Test | Mocking Strategy |
|-------|--------------|--------------|------------------|
| **ViewModel** | commonTest | State transitions, event handling | Fake repositories |
| **Use Cases** | commonTest | Business logic, validation | Fake repositories |
| **Repository** | commonTest | Data coordination, caching | Fake data sources |
| **Data Sources** | commonTest/platform | API contracts, DB operations | Ktor MockEngine |
| **Platform** | androidTest/iosTest | Platform-specific behavior | Real implementations |

### Recommended Test Dependencies

```toml
# libs.versions.toml
[versions]
kotlin-test = "2.0.0"
coroutines-test = "1.8.0"
turbine = "1.0.0"
kotest = "5.8.0"

[libraries]
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin-test" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines-test" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
```

### Example Test Pattern

```kotlin
// commonTest/UserViewModelTest.kt
class UserViewModelTest {

    private lateinit var viewModel: UserViewModel
    private lateinit var fakeRepository: FakeUserRepository
    private lateinit var fakeLogger: FakeLogger

    @BeforeTest
    fun setup() {
        fakeRepository = FakeUserRepository()
        fakeLogger = FakeLogger()
        viewModel = UserViewModel(fakeRepository, fakeLogger)
    }

    @Test
    fun `loadUsers emits loading then success state`() = runTest {
        // Arrange
        val expectedUsers = listOf(User("1", "John"), User("2", "Jane"))
        fakeRepository.setUsers(expectedUsers)

        // Act & Assert
        viewModel.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertTrue(initial.isLoading)

            // Success state
            val success = awaitItem()
            assertFalse(success.isLoading)
            assertEquals(expectedUsers, success.users)
        }
    }

    @Test
    fun `loadUsers handles error gracefully`() = runTest {
        // Arrange
        fakeRepository.setError(Exception("Network error"))

        // Act & Assert
        viewModel.uiState.test {
            skipItems(1) // Skip loading state

            val errorState = awaitItem()
            assertNotNull(errorState.error)
            assertEquals("Network error", errorState.error)
        }
    }
}

// Fake implementation
class FakeUserRepository : UserRepository {
    private var users: List<User> = emptyList()
    private var error: Throwable? = null

    fun setUsers(users: List<User>) { this.users = users }
    fun setError(error: Throwable) { this.error = error }

    override fun getUsers(): Flow<List<User>> = flow {
        error?.let { throw it }
        emit(users)
    }
}
```

---

## Sources & Confidence Levels

| Source | Confidence | Topics Covered |
|--------|------------|----------------|
| [JetBrains KMP Roadmap 2025](https://blog.jetbrains.com/kotlin/2024/10/kotlin-multiplatform-development-roadmap-for-2025/) | HIGH | Official roadmap, tooling updates |
| [JetBrains Common ViewModel Docs](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-viewmodel.html) | HIGH | ViewModel sharing pattern |
| [Kotlin Navigation Docs](https://kotlinlang.org/docs/multiplatform/compose-navigation.html) | HIGH | Official navigation library |
| [Koin KMP Documentation](https://insert-koin.io/docs/reference/koin-mp/kmp/) | HIGH | DI patterns, module organization |
| [Carrion.dev KMP Architecture](https://carrion.dev/en/posts/kmp-architecture/) | HIGH | Clean Architecture, repository pattern |
| [Android Data Layer](https://developer.android.com/topic/architecture/data-layer) | HIGH | Repository pattern, offline-first |
| [KMP Testing Guide 2025](https://www.kmpship.app/blog/kotlin-multiplatform-testing-guide-2025) | HIGH | Testing strategies, source sets |
| [ProAndroidDev Expect/Actual](https://proandroiddev.com/expect-actual-mechanism-in-kotlin-multiplatform-explained-a91e7d85af4e) | HIGH | expect/actual patterns |
| [KMP Anti-Patterns - Medium](https://medium.com/@karelvdmmisc/my-journey-with-kotlin-multiplatform-mobile-pitfalls-anti-patterns-and-solutions-525df7058018) | MEDIUM | Real-world pitfalls |
| [KMP Modularization Guide](https://medium.com/@himanshugaur684/kmp-modularization-ultimate-guide-with-real-world-app-cdb5be94d096) | MEDIUM | Multi-module architecture |
| [KMP Scalability Challenges](https://proandroiddev.com/kotlin-multiplatform-scalability-challenges-on-a-large-project-b3140e12da9d) | MEDIUM | Large project experience |
| [Kermit and Crashlytics](https://touchlab.co/kermit-and-crashlytics) | MEDIUM | Crash reporting integration |
| [Firebase KMP Integration](https://levelup.gitconnected.com/firebase-integration-in-kotlin-multiplatform-6c45e15d234a) | MEDIUM | Firebase setup patterns |

---

## Summary for Solo Developers

### Recommended Architecture Stack

1. **UI**: Compose Multiplatform (shared UI across Android/iOS)
2. **State Management**: ViewModel + StateFlow + UDF
3. **Architecture**: Clean Architecture (Presentation -> Domain -> Data)
4. **DI**: Koin with layer-based modules
5. **Networking**: Ktor Client
6. **Local Storage**: SQLDelight (or Room when stable)
7. **Navigation**: Jetpack Compose Navigation (multiplatform)
8. **Platform Abstractions**: expect/actual for Logger, Analytics, CrashLogger

### Key Principles

1. **Start simple**: Single `composeApp` module with packages until you need modules
2. **Share business logic**: ViewModels, repositories, use cases in commonMain
3. **Isolate platform code**: Use expect/actual sparingly, only for native APIs
4. **Test in commonTest**: Write tests once, run on all platforms
5. **Offline-first**: Local database as source of truth, network for sync
6. **Type safety**: Use sealed classes for state, Result types for errors

### Quick Reference Commands

```bash
# Build all platforms
./gradlew build

# Run Android
./gradlew :composeApp:installDebug

# Run tests
./gradlew allTests

# Code quality
./gradlew detekt

# iOS framework
./gradlew :composeApp:assembleReleaseXCFramework
```

---

*Last Updated: January 2026*
