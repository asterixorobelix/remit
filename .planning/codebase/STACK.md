# Technology Stack

**Analysis Date:** 2026-01-15

## Languages

**Primary:**
- Kotlin 2.3.0 - All application code (backend and mobile)

**Secondary:**
- Swift - iOS app wrapper (`mobile/iosApp/`)
- SQL - Database migrations (`backend/src/main/resources/db/migration/`)

## Runtime

**Environment:**
- JVM (OpenJDK 17) - Backend server and Gradle builds
- Android Runtime - Mobile app (API 24-36, target 35)
- iOS 14+ - Native iOS compilation

**Package Manager:**
- Gradle 8.11.1 with Kotlin DSL
- Lockfile: Gradle dependency locking available

## Frameworks

**Core:**
- Ktor 3.3.3 - Backend REST API server (`backend/build.gradle.kts`)
- Compose Multiplatform 1.9.3 - Cross-platform UI (`mobile/gradle/libs.versions.toml`)
- Material Design 3 - UI components and theming

**Testing:**
- Kotest 6.0.7 - Backend unit tests (`backend/build.gradle.kts`)
- Kotlin Test - Shared test framework
- MockK 1.14.7 - Mocking library
- JUnit 4 - Mobile test runner

**Build/Dev:**
- Gradle Kotlin DSL - Build configuration
- Detekt 1.23.8 - Static analysis (`backend/detekt.yml`, `mobile/detekt-mobile.yml`)
- JaCoCo - Test coverage (80% minimum)
- Fastlane - Mobile deployment automation (`mobile/fastlane/`)

## Key Dependencies

**Critical:**
- Exposed ORM 0.61.0 - Type-safe database queries (`backend/build.gradle.kts`)
- Koin 4.1.1 - Dependency injection (`mobile/gradle/libs.versions.toml`)
- kotlinx.serialization - JSON serialization
- JWT (Auth0) 4.5.0 - Authentication (`backend/build.gradle.kts`)

**Infrastructure:**
- HikariCP 7.0.2 - Database connection pooling
- Flyway 10.22.0 - Database migrations
- Ktor Client 3.3.3 - HTTP client for mobile
- Firebase Crashlytics 33.15.0 - Crash reporting (optional)

## Configuration

**Environment:**
- Backend: `.env` files with `AppConfig.load()` (`backend/.env.example`)
- Mobile: `local.properties` with BuildConfig (`mobile/local.properties.template`)
- Key configs: DATABASE_URL, JWT_SECRET, API_BASE_URL required

**Build:**
- `backend/build.gradle.kts` - Backend dependencies and plugins
- `mobile/composeApp/build.gradle.kts` - KMP and Compose setup
- `mobile/gradle/libs.versions.toml` - Centralized version catalog

## Platform Requirements

**Development:**
- macOS/Linux/Windows (any platform with JDK 17)
- Android Studio or IntelliJ IDEA
- Xcode 15+ (for iOS builds)

**Production:**
- Backend: Railway/Docker deployment (Dockerfile support)
- Mobile: Android API 24+ (Android 7.0), iOS 14+
- TestFlight/App Store Connect for iOS
- Google Play Console for Android

---

*Stack analysis: 2026-01-15*
*Update after major dependency changes*
