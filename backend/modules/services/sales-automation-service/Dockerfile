# Runtime image for sales-automation-service
FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -S healthdata && adduser -S healthdata -G healthdata && \
    apk add --no-cache curl

WORKDIR /app

# Copy the pre-built JAR
COPY modules/services/sales-automation-service/build/libs/*.jar app.jar

# Change ownership
RUN chown -R healthdata:healthdata /app

# Switch to non-root user
USER healthdata

# Expose port
EXPOSE 8106

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=5 --start-period=60s \
    CMD wget --spider -q http://localhost:8106/sales-automation/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
