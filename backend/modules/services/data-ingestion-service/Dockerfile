# =====================================================================
# Data Ingestion Service - SEPARATE DOCKER IMAGE for Load Testing
# =====================================================================
# CRITICAL ARCHITECTURE REQUIREMENT: This Dockerfile creates a completely
# separate Docker container to ensure accurate performance measurement of
# the core HDIM platform without load testing contamination.
#
# Key Isolation Guarantees:
# - Independent multi-stage build (no shared base layers with core services)
# - Separate profile: "ingestion" (NOT in "core", "ai", "analytics", or "full")
# - Resource limits enforced: 2 CPU cores max, 2GB RAM max
# - Manual start only (restart: "no" in docker-compose.yml)
# - Performance monitoring: Separate metrics in Prometheus/Grafana
# - Network: Shared healthdata-network for HTTP integration but isolated container
# =====================================================================

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Install required build tools
RUN apk add --no-cache bash

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Copy shared modules (required for compilation)
COPY modules/shared ./modules/shared

# Copy data-ingestion-service source ONLY
COPY modules/services/data-ingestion-service ./modules/services/data-ingestion-service

# Make gradlew executable
RUN chmod +x gradlew

# Build the service
RUN ./gradlew :modules:services:data-ingestion-service:bootJar --no-daemon --no-build-cache

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install wget for healthcheck
RUN apk add --no-cache wget curl

WORKDIR /app

# Copy only the built JAR
COPY --from=build /workspace/modules/services/data-ingestion-service/build/libs/*.jar app.jar

# Expose port 8080 (mapped to 8200 externally)
EXPOSE 8080

# Run as non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --spider --quiet http://localhost:8080/api/v1/ingestion/health || exit 1

# Environment variable to identify this as load testing container
ENV SERVICE_TYPE=load-testing
ENV SERVICE_PURPOSE="Data ingestion and load testing - separate from core platform"

# Start the application with Docker profile
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--spring.profiles.active=docker"]
