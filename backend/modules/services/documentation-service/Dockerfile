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
COPY modules/services/documentation-service ./modules/services/documentation-service

# Build the service
RUN gradle :modules:services:documentation-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HDIM Platform Team"
LABEL service="documentation-service"
LABEL description="Clinical Documentation Service for HDIM Healthcare Platform - with OCR Support"

WORKDIR /app

# Install Tesseract OCR and dependencies
# Also install wget for healthcheck
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    wget \
    && tesseract --version

# Verify Tesseract installation
RUN echo "Tesseract OCR installed successfully" && \
    ls -la /usr/share/tessdata/ && \
    test -f /usr/share/tessdata/eng.traineddata || (echo "ERROR: eng.traineddata not found" && exit 1)

# Create non-root user
RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata -h /app

# Copy the built JAR
COPY --from=builder /app/modules/services/documentation-service/build/libs/*.jar app.jar

# Create document storage directory
RUN mkdir -p /var/lib/healthdata/documents && \
    chown -R healthdata:healthdata /var/lib/healthdata

# Set ownership
RUN chown -R healthdata:healthdata /app

USER healthdata

# Environment variables
ENV TESSDATA_PREFIX=/usr/share/tessdata \
    DOCUMENT_STORAGE_PATH=/var/lib/healthdata/documents

# JVM configuration for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8091

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8091/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
