# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY modules/shared ./modules/shared
COPY platform ./platform
COPY modules/services/healthix-adapter-service ./modules/services/healthix-adapter-service

RUN gradle :modules:services:healthix-adapter-service:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="HDIM Platform Team"
LABEL service="healthix-adapter-service"
LABEL description="Healthix HIE/PDR Adapter for HDIM Healthcare Platform"

WORKDIR /app

RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata -h /app

RUN apk add --no-cache wget

COPY --from=builder /app/modules/services/healthix-adapter-service/build/libs/*.jar app.jar

RUN chown -R healthdata:healthdata /app

USER healthdata

ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8121

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8121/healthix-adapter/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
