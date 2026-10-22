# Architecture

**Analysis Date:** 2026-01-15

## Pattern Overview

**Overall:** Modular Monorepo with Clean Architecture + MVVM

**Key Characteristics:**
- Single Git repository with two independent applications (backend + mobile)
- Plugin-based backend architecture (Ktor)
- MVVM + Unidirectional Data Flow (mobile)
- Repository pattern for data access
- Koin for dependency injection

## Layers

**Backend - Application Layer:**
- Purpose: Entry point and orchestration
- Contains: `Application.kt`, module initialization
- Location: `backend/src/main/kotlin/com/example/myproject/Application.kt`
- Depends on: All plugins
- Used by: Server startup

**Backend - Plugin Layer:**
- Purpose: Composable server functionality
- Contains: Security, Routing, Databases, Serialization, StatusPages, Documentation
- Location: `backend/src/main/kotlin/com/example/myproject/plugins/`
- Depends on: Configuration layer
- Used by: Application module

**Backend - Domain Layer:**
- Purpose: Business entities and use cases
- Contains: User entity, routes, repository
- Location: `backend/src/main/kotlin/com/example/myproject/domain/`
- Depends on: Database layer
- Used by: Plugin layer (Routing)

**Mobile - Presentation Layer:**
- Purpose: UI rendering and state management
- Contains: Composables, ViewModels, UI state classes
- Location: `mobile/composeApp/src/commonMain/kotlin/.../presentation/`
- Depends on: Domain/Data layer
- Used by: App root composable

**Mobile - Data Layer:**
- Purpose: Data access and caching
- Contains: Repositories, APIs, DTOs
- Location: `mobile/composeApp/src/commonMain/kotlin/.../data/`
- Depends on: Ktor HttpClient
- Used by: Presentation layer (ViewModels)

**Mobile - Platform Layer:**
- Purpose: Cross-platform abstractions
- Contains: Logger, CrashLogger, Analytics, AppConfig
- Location: `mobile/composeApp/src/commonMain/kotlin/.../platform/`
- Depends on: Platform-specific implementations (androidMain/iosMain)
- Used by: All layers

## Data Flow

**Backend HTTP Request:**

1. HTTP request received by Ktor server
2. `Routing` plugin matches endpoint
3. `Security` plugin validates JWT (if protected)
4. Domain route handler processes request (`UserRoutes.kt`)
5. Repository performs database operation (`UserRepository.kt`)
6. Exposed ORM executes SQL via HikariCP
7. Response serialized to JSON and returned

**Mobile User Interaction:**

1. User interaction triggers Composable event
2. Event calls ViewModel function (`UserViewModel.loadUsers()`)
3. ViewModel launches coroutine in `viewModelScope`
4. Repository method called (`UserRepository.refreshUsers()`)
5. API client makes HTTP request (`UserApi.getUsers()`)
6. Response cached in Repository StateFlow
7. ViewModel observes and updates UI state
8. Composable recomposes with `collectAsState()`

**State Management:**
- Backend: Stateless - database per request
- Mobile: StateFlow for reactive state, ViewModel for screen state

## Key Abstractions

**Repository:**
- Purpose: Single source of truth for domain data
- Examples: `backend/.../domain/user/UserRepository.kt`, `mobile/.../data/user/UserRepository.kt`
- Pattern: Caching with Flow exposure, Result-based error handling

**ViewModel:**
- Purpose: Screen state management and business logic coordination
- Examples: `mobile/.../presentation/user/UserViewModel.kt`, `mobile/.../AppViewModel.kt`
- Pattern: BaseViewModel with loading/error state management

**Plugin (Backend):**
- Purpose: Encapsulate server functionality
- Examples: `configureRouting()`, `configureSecurity()`, `configureDatabases()`
- Pattern: Extension functions on Application

**Platform Abstraction:**
- Purpose: Cross-platform interfaces with platform implementations
- Examples: `Logger`, `CrashLogger`, `Analytics`, `AppConfig`
- Pattern: expect/actual declarations

## Entry Points

**Backend Entry:**
- Location: `backend/src/main/kotlin/com/example/myproject/Application.kt`
- Triggers: `./gradlew run` or Docker container start
- Responsibilities: Load config, install plugins, start Netty server

**Mobile Entry:**
- Location: `mobile/composeApp/src/commonMain/kotlin/com/example/myproject/App.kt`
- Triggers: App launch on Android/iOS
- Responsibilities: Initialize Koin, set theme, render root composable

**CI/CD Entry:**
- Location: `.github/workflows/backend-ci.yml`, `.github/workflows/mobile-ci.yml`
- Triggers: Push to main/develop, PR creation
- Responsibilities: Test, lint, build, deploy

## Error Handling

**Strategy:** Exception bubbling to centralized handlers

**Patterns:**
- Backend: `StatusPages` plugin catches exceptions, maps to HTTP responses (`StatusPages.kt`)
- Mobile: Result<T> return types, error states in ViewModel
- Logging: Errors logged with context before handling

## Cross-Cutting Concerns

**Logging:**
- Backend: Logback via SLF4J (`backend/build.gradle.kts`)
- Mobile: Platform `Logger` abstraction with log levels

**Validation:**
- Backend: Request validation in route handlers
- Mobile: Input validation in ViewModels before API calls

**Authentication:**
- Backend: JWT middleware on protected routes (`Security.kt`)
- Mobile: Token storage and header injection (not yet implemented)

---

*Architecture analysis: 2026-01-15*
*Update when major patterns change*
