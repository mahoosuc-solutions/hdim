#!/bin/bash
# Add JWT_SECRET to all service configurations in docker-compose.yml

JWT_SECRET="JWT_SECRET: mySecretKeyForJWTShouldBeAtLeast256BitsLongForHS256Algorithm123456"

# Update each service
for service in quality-measure-service cql-engine-service gateway-admin-service gateway-fhir-service gateway-clinical-service patient-service care-gap-service event-processing-service; do
  echo "Updating $service"
  sed -i "/healthdata-$service/,/ports:/{/SERVER_PORT/a\\      $JWT_SECRET
}" docker-compose.yml
done

echo "JWT_SECRET added to all services"
