# Backend

Ktor API for the mobile apps: JWT auth with rotating refresh tokens, account deletion,
offline-first task sync, Flyway migrations, and PostgreSQL (H2 for development and tests).
Agents and contributors: read [AGENTS.md](AGENTS.md) for contracts and conventions.

## Stack

Versions come from the shared catalog `../gradle/libs.versions.toml`; the release version comes
from `../app.properties` (`VERSION_NAME`, served by `/health`).

| Area | Library |
|---|---|
| Language / runtime | Kotlin 2.3, JDK 17 |
| Server | Ktor 3.3 (Netty, auth-jwt, rate-limit, call-id, call-logging, CORS, Swagger UI) |
| Persistence | Exposed 1.5, HikariCP 7.1, Flyway 13.8, PostgreSQL JDBC 42.7, H2 2.3 |
| Security | java-jwt 4.6 (HS256), jBCrypt 0.4 |
| Logging | Logback 1.6 |
| Quality | kotlin-test + JUnit 5, Ktor `testApplication`, detekt 1.23, ktlint 1.8, Kover |

## Run locally

```bash
./gradlew run                      # H2 in memory, APP_ENVIRONMENT=development, http://localhost:8085
```

With PostgreSQL from the repository's `docker-compose.yml`:

```bash
docker compose up -d db            # from the repository root
DATABASE_URL=jdbc:postgresql://localhost:5432/remit \
  DATABASE_USER=dev DATABASE_PASSWORD=dev ./gradlew run
```

Swagger UI: <http://localhost:8085/swagger> (spec: `src/main/resources/openapi/documentation.yaml`).
Nothing reads `.env` files; export variables in your shell.

## Test and verify

```bash
./gradlew test detekt ktlintCheck koverVerify   # coverage gate: 80% lines, 70% branches
./gradlew koverHtmlReport                      # build/reports/kover/html/index.html
```

Tests build their configuration explicitly and use a private in-memory H2 database each, so
exported `DATABASE_URL` / `APP_ENVIRONMENT` values never leak in.

## Build

```bash
./gradlew installDist    # build/install/backend/bin/backend (thin jar + lib/), used by the Dockerfile
./gradlew buildFatJar    # build/libs/backend-all.jar (META-INF/services merged for Flyway)
APP_ENVIRONMENT=development java -jar build/libs/backend-all.jar
```

## Docker

```bash
# from the repository root
docker build -f backend/Dockerfile -t remit-backend .
docker compose up -d --build       # PostgreSQL + backend in development mode
```

The image defaults to `APP_ENVIRONMENT=production`, runs as a non-root user in UTC, and has a
`HEALTHCHECK` against `/health` on `$PORT`.

## Deploy

Any container platform works (Railway, Render, Fly.io, Cloud Run):

1. Build from the repository root with `backend/Dockerfile` (on Railway: set the Dockerfile path
   to `backend/Dockerfile`).
2. Add PostgreSQL and set `DATABASE_URL` (provider URLs such as `postgres://user:pass@host/db`
   work; on Railway use `${{Postgres.DATABASE_URL}}`).
3. Set `JWT_SECRET` (`openssl rand -base64 48`), `JWT_ISSUER`, and `JWT_AUDIENCE` (your API URL).
   Keep `APP_ENVIRONMENT=production`; startup fails fast on unsafe values.
4. Leave `TRUSTED_PROXY_HOPS=1` behind the platform's proxy. `PORT` is honored automatically.
5. Use `/health` for liveness and `/ready` (checks the database) for readiness. Migrations run on
   startup before traffic is served.

## Configuration

All variables with defaults are documented in [.env.example](.env.example). The important ones:

| Variable | Default | Notes |
|---|---|---|
| `APP_ENVIRONMENT` | production | `development`, `staging`, `production`; non-development is validated |
| `PORT` / `HOST` | 8085 / 0.0.0.0 | |
| `DATABASE_URL` | in-memory H2 | PostgreSQL required outside development |
| `DATABASE_USER` / `DATABASE_PASSWORD` | from URL | PostgreSQL only |
| `JWT_SECRET` | dev-only value | ≥32 chars, no placeholders outside development |
| `JWT_ISSUER` / `JWT_AUDIENCE` | http://localhost:8085 | must not be localhost outside development |
| `JWT_EXPIRATION_MS` / `JWT_REFRESH_EXPIRATION_MS` | 1 h / 30 d | |
| `TRUSTED_PROXY_HOPS` | 1 | proxies appending to `X-Forwarded-For`; 0 when exposed directly |
| `CORS_ALLOWED_HOSTS` | localhost dev ports in development, none otherwise | browser clients only |
| `SWAGGER_ENABLED` | true except production | serves `/swagger` |
| `RATE_LIMITING_ENABLED` | true | login+register 10/min, refresh+logout 30/min per client |
| `MIN_APP_VERSION` / `LATEST_APP_VERSION` | 0.0.0 / app version | served by `/api/v1/config` |
| `FORCE_UPDATE` / `MAINTENANCE_MODE` | false | served by `/api/v1/config` |
| `LOG_LEVEL` | INFO | root Logback level |

## Endpoints

| Method and path | Auth | Description |
|---|---|---|
| `GET /` | | Service name and version |
| `GET /health` | | Liveness (no database access) |
| `GET /ready` | | Readiness; 503 when the database is unavailable |
| `GET /swagger` | | Swagger UI (when enabled) |
| `GET /api/v1/status` | | API status and version |
| `GET /api/v1/config` | | Remote app configuration |
| `POST /api/v1/auth/register` | | Create account, returns tokens (201); 409 if the email exists |
| `POST /api/v1/auth/login` | | Returns tokens; 401 for any bad credential |
| `POST /api/v1/auth/refresh` | | Rotates the refresh token; 401 if invalid, expired, revoked, or reused |
| `POST /api/v1/auth/logout` | | Revokes the given refresh token; always 204 |
| `GET /api/v1/users/me` | Bearer | Profile |
| `PUT /api/v1/users/me` | Bearer | Update `displayName` |
| `DELETE /api/v1/users/me` | Bearer | Delete account, tasks, and sessions (204) |
| `GET /api/v1/tasks` | Bearer | All tasks, including soft-deleted |
| `POST /api/v1/tasks/sync` | Bearer | Last-write-wins sync of up to 500 tasks |

Errors always have the shape `{"error": "..."}`. Validation failures are 400 with a specific
message; unexpected failures are a generic 500 and are logged with the request id.

## Layout

```
src/main/kotlin/com/asterixorobelix/remit/
├── Application.kt        # main() and Application.module(config)
├── configuration/        # AppConfig (environment variables), Environment, BuildInfo
├── plugins/              # Ktor plugins: database, security, HTTP, logging, errors, routing, docs
├── routes/               # thin HTTP handlers
├── service/              # AuthService, JwtTokens, TaskService, input validation
├── domain/               # Exposed tables and domain models
└── dto/                  # request/response bodies shared with the mobile client
src/main/resources/
├── db/migration/         # Flyway Vn__description.sql (never edit an applied migration)
├── openapi/documentation.yaml
└── logback.xml
```
