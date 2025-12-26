# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy Gradle configuration
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy shared modules
COPY modules/shared ./modules/shared

# Copy platform modules
COPY platform ./platform

# Copy service source
COPY modules/services/analytics-service ./modules/services/analytics-service

# Build the service
RUN gradle :modules:services:analytics-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HDIM Platform Team"
LABEL service="analytics-service"
LABEL description="Analytics Service for HDIM Healthcare Platform"

WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata -h /app

# Install wget for health checks
RUN apk add --no-cache wget

# Copy the built JAR
COPY --from=builder /app/modules/services/analytics-service/build/libs/*.jar app.jar

# Set ownership
RUN chown -R healthdata:healthdata /app

USER healthdata

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8092

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8092/analytics/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
