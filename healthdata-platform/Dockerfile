# Multi-stage build for optimal image size

# Stage 1: Build
FROM gradle:8.11-jdk21 AS builder
WORKDIR /app

# Copy build files
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Build the application
RUN gradle build -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata

# Install health check dependencies
RUN apk add --no-cache curl

# Copy JAR from builder (exclude plain JAR)
COPY --from=builder /app/build/libs/*[!plain].jar app.jar

# Change ownership
RUN chown -R healthdata:healthdata /app

# Switch to non-root user
USER healthdata

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]