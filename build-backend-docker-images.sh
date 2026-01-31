#!/bin/bash
set -e

echo "Building backend Docker images..."

cd backend

# Build CQL Engine Service
echo "Building CQL Engine Service..."
docker build -t healthdata/cql-engine-service:latest \
  -f modules/services/cql-engine-service/Dockerfile \
  .

# Build Quality Measure Service
echo "Building Quality Measure Service..."
docker build -t healthdata/quality-measure-service:latest \
  -f modules/services/quality-measure-service/Dockerfile \
  .

# Build FHIR Service
echo "Building FHIR Service..."
docker build -t healthdata/fhir-service-mock:latest \
  -f modules/services/fhir-service/Dockerfile \
  .

# Build Gateway services
echo "Building Gateway services..."
for service in gateway-admin-service gateway-fhir-service gateway-clinical-service; do
  docker build -t "healthdata/${service}:latest" \
    -f "modules/services/${service}/Dockerfile" \
    .
done

echo "✅ All backend images built successfully!"
docker images | grep healthdata
