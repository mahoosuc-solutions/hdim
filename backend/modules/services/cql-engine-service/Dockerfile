FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
# Security updates for base image vulnerabilities
RUN apk update && apk upgrade --no-cache && apk add --no-cache curl
COPY modules/services/cql-engine-service/build-user/libs/*.jar app.jar
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
