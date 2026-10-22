# External Integrations

**Analysis Date:** 2026-01-15

## APIs & External Services

**Cat As A Service (Demo API):**
- Service: `https://cataas.com/cat?json=true` - Random cat images
  - SDK/Client: Ktor HttpClient with kotlinx.serialization
  - Auth: None (public API)
  - Implementation: `mobile/composeApp/src/commonMain/kotlin/.../CatAsAServiceApi.kt`

**Backend API (Internal):**
- Service: User management REST API
  - Base URL: `http://10.0.2.2:8080` (Android emulator localhost)
  - Endpoints: `/api/users` - CRUD operations
  - Implementation: `mobile/composeApp/src/commonMain/kotlin/.../data/user/UserApi.kt`
  - Auth: JWT tokens on protected endpoints

## Data Storage

**Databases:**
- PostgreSQL (production) - Primary data store
  - Connection: `DATABASE_URL` env var
  - Client: Exposed ORM 0.61.0
  - Config: `backend/src/main/kotlin/.../configuration/AppConfig.kt`

- H2 (development) - In-memory database
  - Default: `jdbc:h2:mem:test` for local development
  - Migrations: Flyway in `backend/src/main/resources/db/migration/`

**Connection Pooling:**
- HikariCP 7.0.2
  - Pool size: Configurable via `DB_POOL_SIZE` (default: 10)
  - Config: `backend/src/main/kotlin/.../plugins/Databases.kt`

**File Storage:**
- Not implemented in current codebase

**Caching:**
- In-memory caching via StateFlow in repositories
- No external cache (Redis) configured

## Authentication & Identity

**Auth Provider:**
- Custom JWT implementation
  - Library: Auth0 JWT 4.5.0
  - Token type: Bearer tokens
  - Config: `backend/src/main/kotlin/.../plugins/Security.kt`

**JWT Configuration:**
- Secret: `JWT_SECRET` env var (required in production)
- Issuer: Configurable via `JWT_ISSUER`
- Audience: Configurable via `JWT_AUDIENCE`
- Expiration: Configurable (default varies by environment)

**OAuth Integrations:**
- Not implemented in current codebase

## Monitoring & Observability

**Crash Reporting:**
- Firebase Crashlytics (optional)
  - BOM: 33.15.0 (`mobile/gradle/libs.versions.toml`)
  - Implementation: `mobile/composeApp/src/.../platform/CrashLogger.kt`
  - Fallback: Console logging if Firebase not configured

**Analytics:**
- Firebase Analytics (optional)
  - Implementation: `mobile/composeApp/src/.../platform/Analytics.kt`
  - Events: Standard event constants in `AnalyticsEvents`
  - Fallback: Console logging if Firebase not configured

**Logging:**
- Backend: Logback via SLF4J
  - Level: Configurable via environment

- Mobile: Platform Logger abstraction
  - Implementation: `mobile/composeApp/src/.../platform/Logger.kt`
  - Levels: DEBUG, INFO, WARN, ERROR

## CI/CD & Deployment

**Hosting:**
- Backend: Railway/Docker
  - Deployment: Via Git push or manual trigger
  - Container: Dockerfile available

- Mobile: App stores
  - iOS: TestFlight via fastlane
  - Android: Play Store Internal Track via fastlane

**CI Pipeline:**
- GitHub Actions
  - Workflows: `.github/workflows/`
  - Backend: `backend-ci.yml` (test, detekt, coverage)
  - Mobile: `mobile-ci.yml` (build, test, detekt)
  - Deployment: `mobile-fastlane-deploy.yml`
  - Secrets: Configured in GitHub repository settings

## Environment Configuration

**Development:**
- Required: Copy `.env.example` to `.env`, `local.properties.template` to `local.properties`
- Secrets: Local files only (gitignored)
- Mock services: H2 in-memory database, no Firebase required

**Staging:**
- Separate database instance
- Firebase test project (optional)
- Deployment via feature branches

**Production:**
- Secrets management: Environment variables
- Database: PostgreSQL with connection pooling
- Firebase: Production project for Crashlytics/Analytics

## Webhooks & Callbacks

**Incoming:**
- None configured in current codebase

**Outgoing:**
- None configured in current codebase

## API Documentation

**OpenAPI/Swagger:**
- Available at `/docs` endpoint
- Plugin: `backend/src/main/kotlin/.../plugins/Documentation.kt`
- Auto-generated from route definitions

---

*Integration audit: 2026-01-15*
*Update when adding/removing external services*
