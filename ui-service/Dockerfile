# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
RUN groupadd -r uigroup && useradd -r -g uigroup uiuser
USER uiuser
COPY --from=builder /build/target/ui-service-*.jar app.jar
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8090/actuator/health || exit 1
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
