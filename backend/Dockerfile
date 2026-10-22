# Build context is the repository root: docker build -f backend/Dockerfile .
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace/backend
# The backend build reads ../app.properties (version) and ../gradle/libs.versions.toml (catalog).
COPY app.properties /workspace/app.properties
COPY gradle/libs.versions.toml /workspace/gradle/libs.versions.toml
COPY backend/gradlew backend/settings.gradle.kts backend/build.gradle.kts ./
COPY backend/gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --version
COPY backend/src ./src
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:17-jre
# Distro package versions roll with security updates; pinning them breaks rebuilds.
# hadolint ignore=DL3008
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home --home-dir /home/app app
WORKDIR /app
COPY --from=build /workspace/backend/build/install/backend/ ./

# Secure by default: production disables Swagger UI and allows no CORS origins unless configured.
# docker-compose.yml overrides APP_ENVIRONMENT=development for local use.
# The JVM always runs in UTC (also set in the start script).
ENV APP_ENVIRONMENT=production \
    PORT=8085 \
    TZ=UTC \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=UTC"

# A numeric UID lets orchestrators verify the container does not run as root.
USER 10001
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD ["sh", "-c", "curl --fail --silent --show-error \"http://127.0.0.1:${PORT}/health\" > /dev/null || exit 1"]
ENTRYPOINT ["./bin/backend"]
