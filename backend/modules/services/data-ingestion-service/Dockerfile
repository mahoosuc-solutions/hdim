# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and build files
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Copy shared modules (required for compilation)
COPY modules/shared ./modules/shared

# Copy data-ingestion-service source ONLY
COPY modules/services/data-ingestion-service ./modules/services/data-ingestion-service

# Build the service
RUN ./gradlew :modules:services:data-ingestion-service:bootJar --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Install wget for healthcheck
RUN apk add --no-cache wget

WORKDIR /app

# Copy only the built JAR
COPY --from=build /workspace/modules/services/data-ingestion-service/build/libs/*.jar app.jar

# Expose port 8080 (mapped to 8200 externally)
EXPOSE 8080

# Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Health check endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --spider --quiet http://localhost:8080/actuator/health || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--spring.profiles.active=docker"]
