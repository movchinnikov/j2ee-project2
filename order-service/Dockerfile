# ============================================================
# Order Service — Multi-stage Dockerfile (multi-module build)
# Build context: project root (.)
#   Stage 1: Builds shared-lib → installs to local Maven repo
#            Then builds order-service
#   Stage 2: Minimal JRE runtime
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /build

# ── 1a. Build and install shared-lib into local Maven cache ──
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY shared-lib/src     shared-lib/src
RUN mvn -f shared-lib/pom.xml clean install -q

# ── 1b. Cache order-service dependencies ─────────────────────
COPY order-service/pom.xml order-service/pom.xml
RUN mvn -f order-service/pom.xml dependency:go-offline -q

# ── 1c. Build order-service ───────────────────────────────────
COPY order-service/src order-service/src
RUN mvn -f order-service/pom.xml clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

RUN groupadd -r svcgrp && useradd -r -g svcgrp svcuser
USER svcuser

COPY --from=builder /build/order-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=docker", \
    "-jar", "app.jar"]
