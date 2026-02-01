# Event Store Service Dockerfile
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HDIM Platform Team"
LABEL service="event-store-service"
LABEL description="Event Store Service (Immutable Event Log) for HDIM Event Sourcing Architecture"

# Install wget for health checks
RUN apk add --no-cache wget

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata -h /app

# Copy the pre-built JAR
COPY modules/services/event-store-service/build/libs/event-store-service.jar app.jar

# Set ownership
RUN chown -R healthdata:healthdata /app

USER healthdata

# JVM configuration optimized for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.net.preferIPv4Stack=true"

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8090/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
