# Backend Agent Guide

Scope: `backend/**`. Read `../docs/AI_AGENT_GUIDE.md` for cross-stack contracts and
[README.md](README.md) for run/build/deploy commands.

## Architecture and runtime

- `Application.kt`: `main()` loads `AppConfig.load()` (environment variables) and starts Netty;
  `Application.module(config)` installs everything. Tests call `module(testConfig())` and never
  read the process environment.
- Configuration fails closed: an unset `APP_ENVIRONMENT` means production, and staging/production
  refuse to start with H2, a JWT secret that is short or contains "change", or a localhost issuer/
  audience. `./gradlew run` and docker-compose set `development`; the Docker image sets `production`.
- `plugins/Databases.kt` starts Hikari (READ COMMITTED), runs Flyway before serving traffic,
  connects Exposed, and closes the pool on `ApplicationStopped`. Services receive the `Database`
  and use `database.dbQuery { }` (Exposed `suspendTransaction` on `Dispatchers.IO`).
- Routes are thin; validation, persistence, and reconciliation live in `service/`. `Routing.kt`
  constructs services explicitly; if adding a DI container, Koin is the required choice.
- Development/tests use H2 in PostgreSQL mode (added automatically to any H2 URL); production uses
  PostgreSQL. The JVM runs in UTC because `TIMESTAMP` columns are zone-less.
- Exposed 1.x: imports are `org.jetbrains.exposed.v1.*`; query with `selectAll().where { }`.
- The version shown by `/`, `/health`, and `/api/v1/status` comes from `../app.properties`
  through the generated `build-info.properties` (`BuildInfo.version`).

## Auth and sessions

- `JwtTokens` signs HS256 tokens; the `type` claim separates `access` from `refresh`, so neither
  is accepted in place of the other. Protected routes use the `auth-jwt` provider
  (`JWT_AUTH`) and read the user with `call.userId`.
- Refresh tokens are JWTs whose `jti` is stored in `refresh_tokens` (no token material). Every
  `/refresh` revokes the presented token and issues a new pair in one transaction. Presenting a
  revoked token revokes all of that user's active refresh tokens. `/logout` revokes one token and
  always answers 204. Expired rows are pruned when a user receives new tokens.
- Passwords: BCrypt on `Dispatchers.Default`; unknown emails are compared against a dummy hash so
  login timing does not reveal accounts. Validation (`service/Validation.kt`): email trimmed,
  lower-cased, <=255; password 8 characters to 72 UTF-8 bytes; display name trimmed, non-blank,
  <=100.
- `DELETE /api/v1/users/me` deletes the user; tasks and refresh tokens cascade. Access tokens stay
  valid until expiry, and user-scoped endpoints then return 404 `User not found`.
- Rate limits (`RATE_LIMITING_ENABLED`): login+register share a strict bucket, refresh+logout a
  lenient one, keyed by `call.request.origin.remoteHost`. `XForwardedHeaders` reads the client
  address `TRUSTED_PROXY_HOPS` entries from the end of `X-Forwarded-For` (0 disables it).

## Errors and logging

- Every error body is `ErrorResponse(error)`. Throw `ValidationException` (400, message returned)
  for client input problems; malformed JSON is 400, non-JSON 415, bodies over 1 MiB 413, and any
  other exception a generic 500 that is logged with its stack trace. Empty 401/404/429 responses
  get a generic body; explicit bodies such as `User not found` are preserved.
- `X-Request-ID` matching `[A-Za-z0-9._-]{8,128}` is accepted, otherwise a UUID is generated; it is
  echoed, exposed via CORS, and available as MDC `requestId` in `logback.xml`. Request logs hold
  status, method, path, and duration only; `/health` and `/ready` are not logged. Never log
  tokens, passwords, or authorization data.

## Implemented HTTP surface

| Method/path | Behavior |
|---|---|
| `GET /` | Service name and version |
| `GET /health` | Process liveness (no database) |
| `GET /ready` | Database readiness (503 when unavailable) |
| `GET /swagger` | Swagger UI for `resources/openapi/documentation.yaml` (`SWAGGER_ENABLED`) |
| `GET /api/v1/status` | API status and version |
| `GET /api/v1/config` | Remote app/version configuration (`MIN_APP_VERSION`, `LATEST_APP_VERSION`, `FORCE_UPDATE`, `MAINTENANCE_MODE`) |
| `POST /api/v1/auth/register` | Create user and issue tokens (201; 409 duplicate email) |
| `POST /api/v1/auth/login` | Authenticate and issue tokens (401 on any failure) |
| `POST /api/v1/auth/refresh` | Rotate the refresh token (401 invalid/expired/revoked/reused) |
| `POST /api/v1/auth/logout` | Revoke the given refresh token (204) |
| `GET, PUT /api/v1/users/me` | Read/update authenticated profile |
| `DELETE /api/v1/users/me` | Delete account, tasks, and refresh tokens (204) |
| `GET /api/v1/tasks` | Read authenticated user's tasks |
| `POST /api/v1/tasks/sync` | Reconcile task snapshots by timestamp |

Task sync is user-scoped (primary key `(user_id, id)`), uses soft deletion, and keeps the newest
`updatedAt` via `INSERT ... ON CONFLICT DO NOTHING` plus `UPDATE ... WHERE updated_at < incoming`,
so concurrent syncs are safe. Batches are capped at 500; ids must be UUIDs; titles are trimmed,
non-blank, and <=240 characters; timestamps are clamped to server time + 5 minutes. An invalid item
rejects the whole batch. Preserve this contract when changing backend DTOs or mobile `TaskDto`, and
update `openapi/documentation.yaml` with any route change: `OpenApiContractTest` fails when a served
route (method and path) is missing from the spec or the spec documents one that is not served.

## Database changes

Add an ordered `src/main/resources/db/migration/Vn__description.sql` file for every schema change
(current: V1 users, V2 tasks, V3 refresh tokens, V4 per-user task key). Do not modify a migration
that may have run outside your machine. Keep Exposed tables in `domain/` aligned with migrations,
and write SQL that runs on both PostgreSQL and H2 (for example, rebuild a table instead of dropping
a vendor-named constraint).

## Verification

```bash
./gradlew test detekt ktlintCheck koverVerify   # Kover: 80% line, 70% branch coverage
./gradlew ktlintFormat                                              # fix formatting
./gradlew buildFatJar installDist                       # build/libs/backend-all.jar, build/install/backend
```

CI also fails a pull request whose changed lines are below 80% covered (diff-cover on the Kover
report, `build/reports/kover/report.xml`), and builds the Docker image and checks `/health` when the Dockerfile or build files
change.

Route tests use Ktor `testApplication` with `startApp(testConfig(...))` from `TestSupport.kt`.
Cover authentication boundaries, validation, ownership, and conflict behavior. The fat jar must
keep merged `META-INF/services` (Flyway plugins). Container changes should additionally validate
the root `docker-compose.yml`, `Dockerfile`, `/health`, and `/ready` behavior.
