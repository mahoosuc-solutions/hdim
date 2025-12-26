FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

RUN addgroup -g 1001 -S healthdata && \
    adduser -u 1001 -S healthdata -G healthdata -h /app

RUN apk add --no-cache curl wget

COPY modules/services/quality-measure-service/build/libs/*.jar app.jar

RUN chown -R healthdata:healthdata /app

USER healthdata

ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport"

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/quality-measure/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
