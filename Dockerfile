# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy dependency manifests first for Docker layer caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src/ src/
RUN ./mvnw clean package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Privacy Gateway Team <gateway@hackathon.com>"
LABEL org.opencontainers.image.title="Privacy-Preserving Data Sharing Gateway"
LABEL org.opencontainers.image.description="PS26SCS211 — Consent-aware, policy-gated patient data sharing API"
LABEL org.opencontainers.image.version="1.0.0"

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /workspace/target/*.jar app.jar

# Switch to non-root
USER appuser

EXPOSE 8080

# JVM tuning for containers: GC optimisation, bounded heap, graceful shutdown
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
