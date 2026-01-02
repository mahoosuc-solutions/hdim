#!/bin/bash
set -e

# Get the directory of this script
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Start the backend container
docker run -d --name healthdata-platform \
  --network healthdata-platform_default \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://healthdata-postgres:5432/healthdata" \
  -e SPRING_DATASOURCE_USERNAME="healthdata" \
  -e SPRING_DATASOURCE_PASSWORD="healthdata_password" \
  -e SERVER_PORT="8080" \
  -v "$DIR/build/libs/healthdata-platform-2.0.0.jar:/app/healthdata-platform-2.0.0.jar" \
  -p 8080:8080 \
  eclipse-temurin:21-jre-alpine \
  java -jar /app/healthdata-platform-2.0.0.jar

echo "✅ Container started, waiting for application to initialize..."
sleep 25

echo ""
echo "=== HEALTH CHECK ==="
curl -s http://localhost:8080/actuator/health | jq '.' || echo "Health endpoint not yet available"
