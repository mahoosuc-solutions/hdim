# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace/app

# Copy Gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Copy shared modules
COPY modules/shared modules/shared

# Copy platform modules
COPY platform platform

# Copy the service module
COPY modules/services/agent-runtime-service modules/services/agent-runtime-service

# Build the application
RUN chmod +x gradlew && \
    ./gradlew :modules:services:agent-runtime-service:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Add non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy the JAR file
COPY --from=builder /workspace/app/modules/services/agent-runtime-service/build/libs/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+OptimizeStringConcat"

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8088/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
