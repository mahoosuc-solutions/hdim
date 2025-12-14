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
COPY modules/services/agent-builder-service ./modules/services/agent-builder-service

# Build the service
RUN gradle :modules:services:agent-builder-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HDIM Platform Team"
LABEL service="agent-builder-service"
LABEL description="No-Code AI Agent Builder Service for HDIM"

WORKDIR /app

# Create non-root user
RUN addgroup -g 1000 hdim && \
    adduser -u 1000 -G hdim -D -h /app hdim

# Copy the built JAR
COPY --from=builder /app/modules/services/agent-builder-service/build/libs/*.jar app.jar

# Set ownership
RUN chown -R hdim:hdim /app

USER hdim

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8096

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8096/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
