# Simple Dockerfile for Gateway FHIR Service - uses pre-built JAR
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HDIM Platform Team"
LABEL service="gateway-fhir-service"
LABEL description="Gateway FHIR Service for HDIM Healthcare Platform"

# Install wget for health checks
RUN apk add --no-cache wget

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata -h /app

# Copy the pre-built JAR
COPY modules/services/gateway-fhir-service/build/libs/gateway-fhir-service.jar app.jar

# Set ownership
RUN chown -R healthdata:healthdata /app

USER healthdata

# JVM configuration optimized for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
