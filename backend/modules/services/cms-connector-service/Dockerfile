# Multi-stage build for CMS Connector Service
# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY .mvn .mvn

# Copy source code
COPY src src

# Build application
RUN ./mvn/bin/mvn clean package -DskipTests -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="HDIM Team"
LABEL description="CMS Connector Service - Medicare Claim Data Integration"
LABEL version="1.0.0"

# Install curl for health checks
RUN apk add --no-cache curl

# Create app user (security best practice)
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /sbin/nologin app

WORKDIR /app

# Copy compiled application from builder
COPY --from=builder /app/target/cms-connector-service-*.jar app.jar

# Set ownership
RUN chown -R app:app /app

# Switch to non-root user
USER app

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set JVM options for production
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
