# Simple Dockerfile for Event Router Service - uses pre-built JAR
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HealthData-in-Motion Team"
LABEL service="event-router-service"
LABEL version="2.0.0"

# Install wget for health checks
RUN apk add --no-cache wget

WORKDIR /app

# Copy the pre-built JAR
COPY build/libs/event-router-service-1.0.0.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring:spring

# Expose service port
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8089/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
