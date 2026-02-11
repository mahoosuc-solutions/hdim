# Runtime image for sales-automation-service
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -S healthdata && adduser -S healthdata -G healthdata && \
    apk add --no-cache curl wget

WORKDIR /app

# Copy the pre-built JAR
COPY modules/services/sales-automation-service/build/libs/*.jar app.jar

# Change ownership
RUN chown -R healthdata:healthdata /app

# Switch to non-root user
USER healthdata

# Expose port
EXPOSE 8106

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD wget --spider -q http://localhost:8106/sales-automation/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
