FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle :modules:services:ihe-gateway-service:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 -S healthdata && adduser -u 1001 -S healthdata -G healthdata
WORKDIR /app
COPY --from=builder /app/modules/services/ihe-gateway-service/build/libs/*.jar app.jar
RUN chown -R healthdata:healthdata /app
USER healthdata
EXPOSE 8125
ENTRYPOINT ["java", "-jar", "app.jar"]
