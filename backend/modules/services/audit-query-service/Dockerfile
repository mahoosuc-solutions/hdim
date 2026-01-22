# Multi-stage Dockerfile for Audit Query Service
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install wget for healthcheck
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Set working directory
WORKDIR /app

# Copy the built JAR from Gradle build
ARG JAR_FILE=modules/services/audit-query-service/build/libs/*-boot.jar
COPY --chown=spring:spring ${JAR_FILE} app.jar

# Expose service port
EXPOSE 8088

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --spider -q http://localhost:8088/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
