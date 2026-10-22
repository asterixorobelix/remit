# Backend Agent Guide

Scope: `backend/**`. Read `../docs/AI_AGENT_GUIDE.md` for cross-stack contracts and
[README.md](README.md) for run/build/deploy commands.

## Architecture and runtime

- `Application.kt`: `main()` loads `AppConfig.load()` (environment variables) and starts Netty;
  `Application.module(config)` installs everything. Tests call `module(testConfig())` and never
  read the process environment.
- Configuration fails closed: an unset `APP_ENVIRONMENT` means production, which disables Swagger
  UI and allows no CORS origins unless `CORS_ALLOWED_HOSTS` is set. An unknown value refuses to
  start. `./gradlew run`, `make backend`, and docker-compose set `development`; the Docker image
  sets `production`.
- There is no database and no authentication. Routes live in `routes/`, are wired in
  `plugins/Routing.kt`, and must stay thin; put validation and business logic in `service/`. If
  adding a DI container, Koin is the required choice.
- The JVM runs in UTC (`main()`, the Gradle `run` task, tests, and the Docker image).
- The version shown by `/`, `/health`, and `/api/v1/status` comes from `../app.properties`
  through the generated `build-info.properties` (`BuildInfo.version`).
- `XForwardedHeaders` resolves the client address (`call.request.origin`) from the entry
  `TRUSTED_PROXY_HOPS` positions from the end of `X-Forwarded-For` (0 disables it).

## Errors and logging

- Every error body is `ErrorResponse(error)`. Throw `service.ValidationException` (400, message
  returned) for client input problems; malformed JSON or a missing field is 400, non-JSON 415,
  bodies over 1 MiB 413, and any other exception a generic 500 that is logged with its stack
  trace. Empty 4xx responses (404, 405, ...) get a generic body; explicit bodies are preserved.
- `X-Request-ID` matching `[A-Za-z0-9._-]{8,128}` is accepted, otherwise a UUID is generated; it is
  echoed, exposed via CORS, and available as MDC `requestId` in `logback.xml`. Request logs hold
  status, method, path, and duration only; `/health` is not logged. Never log credentials or
  personal data.

## Implemented HTTP surface

| Method/path | Behavior |
|---|---|
| `GET /` | Service name and version |
| `GET /health` | Process liveness |
| `GET /swagger` | Swagger UI for `resources/openapi/documentation.yaml` (`SWAGGER_ENABLED`) |
| `GET /api/v1/status` | API status and version |
| `GET /api/v1/config` | Remote app/version configuration (`MIN_APP_VERSION`, `LATEST_APP_VERSION`, `FORCE_UPDATE`, `MAINTENANCE_MODE`) |

Update `openapi/documentation.yaml` with any route change: `OpenApiContractTest` fails when a
served route (method and path) is missing from the spec or the spec documents one that is not
served.

## Verification

```bash
./gradlew test detekt ktlintCheck koverVerify   # Kover: 80% line, 70% branch coverage
./gradlew ktlintFormat                          # fix formatting
./gradlew buildFatJar installDist               # build/libs/backend-all.jar, build/install/backend
```

CI also fails a pull request whose changed lines are below 80% covered (diff-cover on the Kover
report, `build/reports/kover/report.xml`), and builds the Docker image and checks `/health` when
the Dockerfile or build files change.

Route tests use Ktor `testApplication` with `startApp(testConfig(...))` from `TestSupport.kt`.
`ErrorHandlingTest` installs test-only routes to exercise the error mapping. Container changes
should additionally validate the root `docker-compose.yml`, `Dockerfile`, and `/health`.
