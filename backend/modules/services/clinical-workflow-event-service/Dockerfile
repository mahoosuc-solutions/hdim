# Clinical Workflow Event Service - CQRS Read Model
# Port: 8113, Context: /clinical-workflow-event
# Database: clinical_workflow_event_db

FROM eclipse-temurin:21-alpine AS builder

WORKDIR /workspace

# Copy gradle wrapper and build configuration
COPY backend/gradle gradle
COPY backend/gradlew .
COPY backend/gradle.properties .
COPY backend/settings.gradle.kts .

# Copy shared modules
COPY backend/modules/shared modules/shared

# Copy service
COPY backend/modules/services/clinical-workflow-event-service modules/services/clinical-workflow-event-service

# Build
RUN ./gradlew :modules:services:clinical-workflow-event-service:bootJar -x test --no-daemon && \
    mkdir -p /workspace/app && \
    cp modules/services/clinical-workflow-event-service/build/libs/clinical-workflow-event-service-*.jar /workspace/app/app.jar && \
    cd /workspace/app && \
    jar xf app.jar && \
    rm app.jar

# Runtime
FROM eclipse-temurin:21-alpine

RUN apk add --no-cache dumb-init curl

# Non-root user
RUN addgroup -g 10000 appgroup && \
    adduser -u 10000 -G appgroup -s /sbin/nologin -D appuser

WORKDIR /app

# Copy from builder
COPY --from=builder --chown=appuser:appgroup /workspace/app .

USER appuser

# Configuration
ENV SERVER_PORT=8113 \
    SERVER_SERVLET_CONTEXT_PATH=/clinical-workflow-event \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8113

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8113/clinical-workflow-event/api/v1/workflow-projections/health || exit 1

ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-cp", ".", "org.springframework.boot.loader.launch.JarLauncher"]
