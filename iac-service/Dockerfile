# ============================================================
# IAC Service — Multi-stage Dockerfile (multi-module build)
# Build context: project root (.)
#   Stage 1: Builds shared-lib → installs to local Maven repo
#            Then builds iac-service
#   Stage 2: Minimal JRE runtime
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# ── 1a. Build and install shared-lib into local Maven cache ──
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY shared-lib/src     shared-lib/src
RUN mvn -f shared-lib/pom.xml clean install -q

# ── 1b. Cache IAC service dependencies ───────────────────────
COPY iac-service/pom.xml iac-service/pom.xml
RUN mvn -f iac-service/pom.xml dependency:go-offline -q

# ── 1c. Build IAC service ─────────────────────────────────────
COPY iac-service/src iac-service/src
RUN mvn -f iac-service/pom.xml clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

# Non-root user for security
RUN groupadd -r iacgroup && useradd -r -g iacgroup iacuser
USER iacuser

# Copy the built jar
COPY --from=builder /build/iac-service/target/iac-service-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
