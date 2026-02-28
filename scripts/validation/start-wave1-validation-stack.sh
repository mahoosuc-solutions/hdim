#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
NETWORK_NAME="${NETWORK_NAME:-hdim-demo-network}"
BUILD_WAVE1_IMAGES="${BUILD_WAVE1_IMAGES:-true}"

PAYER_IMAGE="${PAYER_IMAGE:-hdim-payer-workflows-service:wave1-local}"
INGEST_IMAGE="${INGEST_IMAGE:-hdim-data-ingestion-service:wave1-local}"

PAYER_CONTAINER="${PAYER_CONTAINER:-hdim-demo-payer-workflows}"
INGEST_CONTAINER="${INGEST_CONTAINER:-hdim-demo-data-ingestion}"

PAYER_ALIAS="${PAYER_ALIAS:-payer-workflows-service}"
INGEST_ALIAS="${INGEST_ALIAS:-data-ingestion-service}"

wait_for_url() {
  local url="$1"
  local label="$2"
  local attempts="${3:-60}"
  local sleep_seconds="${4:-5}"

  for i in $(seq 1 "$attempts"); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      echo "Ready: $label ($url)"
      return 0
    fi
    echo "Waiting for $label ($i/$attempts)"
    sleep "$sleep_seconds"
  done

  echo "Timed out waiting for $label ($url)"
  return 1
}

echo "Starting demo stack using $COMPOSE_FILE"
docker compose -f "$COMPOSE_FILE" up -d

wait_for_url "http://127.0.0.1:18080/actuator/health" "gateway-edge"

echo "Ensuring Wave-1 validation databases exist"
docker exec hdim-demo-postgres psql -U healthdata -d postgres -c "SELECT 'CREATE DATABASE payer_workflows_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payer_workflows_db')\\gexec" >/dev/null
docker exec hdim-demo-postgres psql -U healthdata -d postgres -c "SELECT 'CREATE DATABASE data_ingestion_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'data_ingestion_db')\\gexec" >/dev/null

if [[ "$BUILD_WAVE1_IMAGES" == "true" ]]; then
  echo "Building Wave-1 service images"
  docker build -t "$PAYER_IMAGE" -f backend/modules/services/payer-workflows-service/Dockerfile backend
  docker build -t "$INGEST_IMAGE" -f backend/modules/services/data-ingestion-service/Dockerfile backend
fi

echo "Replacing Wave-1 service containers"
docker rm -f "$PAYER_CONTAINER" "$INGEST_CONTAINER" >/dev/null 2>&1 || true

docker run -d \
  --name "$PAYER_CONTAINER" \
  --network "$NETWORK_NAME" \
  --network-alias "$PAYER_ALIAS" \
  -p 18098:8098 \
  -e SPRING_PROFILES_ACTIVE=wave1-local-validation \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/payer_workflows_db \
  -e SPRING_DATASOURCE_USERNAME=healthdata \
  -e SPRING_DATASOURCE_PASSWORD=demo_password_2024 \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  -e JWT_SECRET=DemoSecretKeyForHDIMShouldBeAtLeast256BitsLongForHS256Algorithm12345678 \
  "$PAYER_IMAGE" >/dev/null

docker run -d \
  --name "$INGEST_CONTAINER" \
  --network "$NETWORK_NAME" \
  --network-alias "$INGEST_ALIAS" \
  -p 18200:8080 \
  --entrypoint sh \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_USER=healthdata \
  -e DB_PASSWORD=demo_password_2024 \
  -e FHIR_SERVICE_URL=http://fhir-service:8085/fhir \
  -e CARE_GAP_SERVICE_URL=http://care-gap-service:8086/care-gap \
  -e QUALITY_MEASURE_SERVICE_URL=http://quality-measure-service:8087/quality-measure \
  "$INGEST_IMAGE" \
  -c 'java $JAVA_OPTS -jar /app/app.jar --spring.profiles.active=wave1-local-validation' >/dev/null

wait_for_url "http://127.0.0.1:18098/actuator/health" "$PAYER_CONTAINER"
wait_for_url "http://127.0.0.1:18200/actuator/health" "$INGEST_CONTAINER"

echo "Wave-1 validation stack is ready."
echo "Run: GATEWAY_URL=http://127.0.0.1:18080 ./scripts/validation/validate-wave1-edge-gateway-flow.sh"
