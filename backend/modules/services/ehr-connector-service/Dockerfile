FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/ehr-connector-service.jar /app/ehr-connector-service.jar

EXPOSE 8095

ENTRYPOINT ["java", "-jar", "/app/ehr-connector-service.jar"]
