# Codebase Structure

**Analysis Date:** 2026-01-15

## Directory Layout

```
project_setup/
├── backend/                    # Ktor REST API server
│   ├── src/main/kotlin/        # Server source code
│   ├── src/main/resources/     # Config and migrations
│   ├── src/test/kotlin/        # Backend tests
│   └── build.gradle.kts        # Backend dependencies
├── mobile/                     # Kotlin Multiplatform app
│   ├── composeApp/             # Shared KMP module
│   │   ├── src/commonMain/     # Cross-platform code
│   │   ├── src/androidMain/    # Android implementations
│   │   ├── src/iosMain/        # iOS implementations
│   │   └── src/commonTest/     # Shared tests
│   ├── iosApp/                 # iOS Xcode project
│   ├── fastlane/               # Deployment automation
│   ├── gradle/                 # Version catalog
│   └── build.gradle.kts        # Mobile root config
├── .github/workflows/          # CI/CD pipelines
├── CLAUDE.md                   # AI assistant context
├── setup.sh                    # Project initialization
└── Makefile                    # Common commands
```

## Directory Purposes

**backend/**
- Purpose: Ktor REST API server application
- Contains: Kotlin source, SQL migrations, test files
- Key files: `Application.kt` (entry), `build.gradle.kts` (deps)
- Subdirectories: `src/main/kotlin/.../` (source), `src/test/kotlin/` (tests)

**backend/src/main/kotlin/com/example/myproject/**
- Purpose: Main application source code
- Contains: Application entry, plugins, domain logic, configuration
- Key files: `Application.kt`, `configuration/AppConfig.kt`
- Subdirectories: `plugins/`, `domain/`, `configuration/`, `di/`

**mobile/composeApp/src/commonMain/**
- Purpose: Shared Kotlin Multiplatform code
- Contains: UI, ViewModels, repositories, APIs, platform abstractions
- Key files: `App.kt`, `AppViewModel.kt`, `Theme.kt`
- Subdirectories: `presentation/`, `data/`, `platform/`, `di/`

**mobile/composeApp/src/androidMain/**
- Purpose: Android-specific implementations
- Contains: Platform implementations for Logger, Analytics, CrashLogger, AppConfig
- Key files: `Logger.android.kt`, `CrashLogger.android.kt`, `AppConfig.android.kt`

**mobile/composeApp/src/iosMain/**
- Purpose: iOS-specific implementations
- Contains: Platform implementations using Foundation/UIKit
- Key files: `Logger.ios.kt`, `CrashLogger.ios.kt`, `AppConfig.ios.kt`

**mobile/fastlane/**
- Purpose: App store deployment automation
- Contains: Fastfile lanes, signing config, screenshots
- Key files: `Fastfile`, `Matchfile`, `Appfile`, `SETUP.md`

**.github/workflows/**
- Purpose: CI/CD pipeline definitions
- Contains: GitHub Actions YAML workflows
- Key files: `backend-ci.yml`, `mobile-ci.yml`, `mobile-fastlane-deploy.yml`

## Key File Locations

**Entry Points:**
- `backend/src/main/kotlin/com/example/myproject/Application.kt` - Server entry
- `mobile/composeApp/src/commonMain/kotlin/.../App.kt` - Mobile root composable

**Configuration:**
- `backend/.env.example` - Backend environment template
- `backend/src/main/kotlin/.../configuration/AppConfig.kt` - Backend config
- `mobile/local.properties.template` - Mobile environment template
- `mobile/composeApp/src/commonMain/kotlin/.../platform/AppConfig.kt` - Mobile config
- `mobile/gradle/libs.versions.toml` - Dependency version catalog

**Core Logic:**
- `backend/src/main/kotlin/.../domain/user/` - User domain (routes, repository, entity)
- `backend/src/main/kotlin/.../plugins/` - Ktor plugins
- `mobile/composeApp/src/commonMain/kotlin/.../presentation/` - UI and ViewModels
- `mobile/composeApp/src/commonMain/kotlin/.../data/` - Repositories and APIs

**Testing:**
- `backend/src/test/kotlin/.../ApplicationTest.kt` - Backend tests
- `mobile/composeApp/src/commonTest/kotlin/.../ComposeAppCommonTest.kt` - Shared tests
- `mobile/composeApp/src/androidTest/kotlin/.../screenshots/` - Screenshot tests

**Documentation:**
- `CLAUDE.md` - AI assistant context (comprehensive)
- `README.md` - Project overview
- `FASTLANE_GUIDE.md` - Deployment guide
- `RELEASE_CHECKLIST.md` - Release steps

## Naming Conventions

**Files:**
- PascalCase.kt for classes (`UserViewModel.kt`, `UserScreen.kt`)
- PascalCase.kt for composables (`UserScreen.kt`, `App.kt`)
- lowercase.yml for configs (`detekt.yml`, `backend-ci.yml`)
- UPPERCASE.md for documentation (`README.md`, `CLAUDE.md`)

**Directories:**
- lowercase for packages (`presentation`, `data`, `domain`)
- camelCase for multi-word (`composeApp`, `commonMain`)
- plural for collections (`plugins`, `workflows`)

**Special Patterns:**
- `*.android.kt` for Android implementations
- `*.ios.kt` for iOS implementations
- `V{n}__*.sql` for migrations (e.g., `V1__create_users_table.sql`)

## Where to Add New Code

**New Feature:**
- Primary code: `mobile/composeApp/src/commonMain/kotlin/.../presentation/{feature}/`
- Data layer: `mobile/composeApp/src/commonMain/kotlin/.../data/{feature}/`
- Tests: `mobile/composeApp/src/commonTest/kotlin/.../`
- DI: Update `mobile/composeApp/src/commonMain/kotlin/.../di/AppModule.kt`

**New API Endpoint:**
- Routes: `backend/src/main/kotlin/.../domain/{entity}/{Entity}Routes.kt`
- Repository: `backend/src/main/kotlin/.../domain/{entity}/{Entity}Repository.kt`
- Entity: `backend/src/main/kotlin/.../domain/{entity}/{Entity}.kt`
- Tests: `backend/src/test/kotlin/.../`

**New Screen:**
- Screen: `mobile/composeApp/src/commonMain/kotlin/.../presentation/{screen}/{Screen}Screen.kt`
- ViewModel: `mobile/composeApp/src/commonMain/kotlin/.../presentation/{screen}/{Screen}ViewModel.kt`
- Navigation: Update `mobile/composeApp/src/commonMain/kotlin/.../App.kt`

**Platform Abstraction:**
- Interface: `mobile/composeApp/src/commonMain/kotlin/.../platform/{Name}.kt`
- Android: `mobile/composeApp/src/androidMain/kotlin/.../platform/{Name}.android.kt`
- iOS: `mobile/composeApp/src/iosMain/kotlin/.../platform/{Name}.ios.kt`

## Special Directories

**mobile/composeApp/src/commonMain/composeResources/**
- Purpose: String resources and assets for localization
- Source: Manually created resource files
- Committed: Yes
- Structure: `values/strings.xml` (English), `values-es/strings.xml` (Spanish)

**backend/src/main/resources/db/migration/**
- Purpose: Flyway database migrations
- Source: Manually created SQL files
- Committed: Yes
- Naming: `V{version}__{description}.sql`

---

*Structure analysis: 2026-01-15*
*Update when directory structure changes*
