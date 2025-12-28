FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY modules/services/ehr-connector-service/build/libs/ehr-connector-service.jar /app/ehr-connector-service.jar

EXPOSE 8095

ENTRYPOINT ["java", "-jar", "/app/ehr-connector-service.jar"]
