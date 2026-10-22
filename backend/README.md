# Backend

Ktor API for the mobile apps: service metadata, liveness, and remote app configuration, with a
uniform error shape, request IDs, and Swagger UI. It has no database and no authentication.
Agents and contributors: read [AGENTS.md](AGENTS.md) for contracts and conventions.

## Stack

Versions come from the shared catalog `../gradle/libs.versions.toml`; the release version comes
from `../app.properties` (`VERSION_NAME`, served by `/health`).

| Area | Library |
|---|---|
| Language / runtime | Kotlin 2.3, JDK 17 |
| Server | Ktor 3.3 (Netty, call-id, call-logging, CORS, forwarded headers, body limit, status pages, Swagger UI) |
| Logging | Logback 1.6 |
| Quality | kotlin-test + JUnit 5, Ktor `testApplication`, detekt 1.23, ktlint 1.8, Kover |

## Run locally

```bash
make backend                       # from the repository root
./gradlew run                      # or from backend/; both use APP_ENVIRONMENT=development, http://localhost:8085
```

Swagger UI: <http://localhost:8085/swagger> (spec: `src/main/resources/openapi/documentation.yaml`).
Nothing reads `.env` files; export variables in your shell.

## Test and verify

```bash
./gradlew test detekt ktlintCheck koverVerify   # coverage gate: 80% lines, 70% branches
./gradlew koverHtmlReport                      # build/reports/kover/html/index.html
```

Tests build their configuration explicitly, so exported `APP_ENVIRONMENT` or CORS values never
leak in.

## Build

```bash
./gradlew installDist    # build/install/backend/bin/backend (thin jar + lib/), used by the Dockerfile
./gradlew buildFatJar    # build/libs/backend-all.jar
APP_ENVIRONMENT=development java -jar build/libs/backend-all.jar
```

## Docker

```bash
# from the repository root
docker build -f backend/Dockerfile -t remit-backend .
docker compose up -d --build       # backend in development mode
```

The image defaults to `APP_ENVIRONMENT=production`, runs as a non-root user in UTC, and has a
`HEALTHCHECK` against `/health` on `$PORT`.

## Deploy

Any container platform works (Railway, Render, Fly.io, Cloud Run):

1. Build from the repository root with `backend/Dockerfile` (on Railway: set the Dockerfile path
   to `backend/Dockerfile`).
2. Keep `APP_ENVIRONMENT=production`. Set `CORS_ALLOWED_HOSTS` only if browser clients call the
   API.
3. Leave `TRUSTED_PROXY_HOPS=1` behind the platform's proxy. `PORT` is honored automatically.
4. Use `/health` for liveness.

## Configuration

All variables with defaults are documented in [.env.example](.env.example). The important ones:

| Variable | Default | Notes |
|---|---|---|
| `APP_ENVIRONMENT` | production | `development`, `staging`, `production`; unknown values refuse to start |
| `PORT` / `HOST` | 8085 / 0.0.0.0 | |
| `TRUSTED_PROXY_HOPS` | 1 | proxies appending to `X-Forwarded-For`; 0 when exposed directly |
| `CORS_ALLOWED_HOSTS` | localhost dev ports in development, none otherwise | browser clients only |
| `SWAGGER_ENABLED` | true except production | serves `/swagger` |
| `MIN_APP_VERSION` / `LATEST_APP_VERSION` | 0.0.0 / app version | served by `/api/v1/config` |
| `FORCE_UPDATE` / `MAINTENANCE_MODE` | false | served by `/api/v1/config` |
| `LOG_LEVEL` | INFO | root Logback level |

## Endpoints

| Method and path | Description |
|---|---|
| `GET /` | Service name and version |
| `GET /health` | Liveness |
| `GET /swagger` | Swagger UI (when enabled) |
| `GET /api/v1/status` | API status and version |
| `GET /api/v1/config` | Remote app configuration |

Errors always have the shape `{"error": "..."}`. Validation failures are 400 with a specific
message; unexpected failures are a generic 500 and are logged with the request id.

## Layout

```
src/main/kotlin/com/asterixorobelix/remit/
├── Application.kt        # main() and Application.module(config)
├── configuration/        # AppConfig (environment variables), Environment, BuildInfo
├── plugins/              # Ktor plugins: HTTP, logging, serialization, errors, routing, docs
├── routes/               # thin HTTP handlers
├── service/              # ValidationException (business logic goes here)
└── dto/                  # request/response bodies shared with the mobile client
src/main/resources/
├── openapi/documentation.yaml
└── logback.xml
```
