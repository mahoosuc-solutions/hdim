# Multi-stage Docker build for Java microservices
# NOTE: Build the JAR locally first: ./gradlew :modules:services:<SERVICE_NAME>:bootJar

# Runtime stage - uses pre-built JAR
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 healthdata && \
    adduser -D -u 1001 -G healthdata healthdata

WORKDIR /app

# Build argument for service name
ARG SERVICE_NAME
ENV SERVICE_NAME=${SERVICE_NAME}

# Copy the pre-built JAR from backend root
COPY app.jar app.jar

# Change ownership
RUN chown -R healthdata:healthdata /app

# Switch to non-root user
USER healthdata

# Expose port (will be overridden by docker-compose)
EXPOSE 8080

# JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -q --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
