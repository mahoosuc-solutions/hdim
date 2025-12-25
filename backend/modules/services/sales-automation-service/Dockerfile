# Multi-stage build for sales-automation-service
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy gradle files first for better caching
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy shared modules
COPY modules/shared ./modules/shared

# Copy this service
COPY modules/services/sales-automation-service ./modules/services/sales-automation-service

# Build the service
RUN gradle :modules:services:sales-automation-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -S healthdata && adduser -S healthdata -G healthdata

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/modules/services/sales-automation-service/build/libs/*.jar app.jar

# Change ownership
RUN chown -R healthdata:healthdata /app

# Switch to non-root user
USER healthdata

# Expose port
EXPOSE 8106

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD wget --spider -q http://localhost:8106/sales-automation/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
