# Clinical Workflow Event Service - Multi-stage Dockerfile
# Optimized for production with Alpine Linux and minimal image size

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Gradle wrapper and configuration
COPY gradle ./gradle
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .
COPY gradle/libs.versions.toml ./gradle/

# Copy source code
COPY modules/shared ./modules/shared
COPY modules/services/clinical-workflow-event-handler-service ./modules/services/clinical-workflow-event-handler-service
COPY modules/services/clinical-workflow-event-service ./modules/services/clinical-workflow-event-service

# Pre-download dependencies to avoid snapshot issues in clean Docker build
RUN ./gradlew --no-daemon --refresh-dependencies dependencies

# Build the application JAR
RUN ./gradlew --no-daemon :modules:services:clinical-workflow-event-service:bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install health check dependencies
RUN apk add --no-cache wget ca-certificates

# Create application user (non-root for security)
RUN addgroup -S healthdata && adduser -S healthdata -G healthdata

# Set working directory
WORKDIR /app

# Copy JAR from builder
COPY --from=builder /workspace/modules/services/clinical-workflow-event-service/build/libs/clinical-workflow-event-service.jar app.jar

# Change ownership to healthdata user
RUN chown -R healthdata:healthdata /app

# Switch to non-root user
USER healthdata

# Expose port (8093 - no conflict)
EXPOSE 8093

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UnlockExperimentalVMOptions -XX:G1NewCollectionHeuristicPercent=30 -XX:+DisableExplicitGC -Dcom.sun.management.jmxremote=false -XX:+ParallelRefProcEnabled"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8093/actuator/health || exit 1

# Start application
CMD ["java", "-jar", "app.jar"]
