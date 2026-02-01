#!/bin/bash
# Demo stack reset cycle: drop, rebuild, reseed, revalidate, optional screenshots/e2e.

set -e

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.demo.yml}"
SKIP_BUILD="${SKIP_BUILD:-false}"
RUN_SCREENSHOTS="${RUN_SCREENSHOTS:-false}"
RUN_E2E="${RUN_E2E:-false}"
RUN_DEMO_FLOW="${RUN_DEMO_FLOW:-false}"

log_info() { echo "[demo-cycle] $1"; }

log_info "Stopping demo stack and removing volumes..."
docker compose -f "$COMPOSE_FILE" down -v --remove-orphans

if [ "$SKIP_BUILD" != "true" ]; then
    log_info "Building demo images..."
    bash ./build-backend-docker-images.sh
else
    log_info "Skipping image build (SKIP_BUILD=true)."
fi

log_info "Starting demo stack..."
docker compose -f "$COMPOSE_FILE" up -d

log_info "Seeding demo data..."
bash ./load-fhir-demo-data.sh
bash ./load-demo-patient-data.sh
bash ./load-demo-clinical-data.sh

log_info "Ensuring demo users have tenant mappings..."
docker exec -i hdim-demo-postgres psql -U healthdata -d gateway_db -c \
  "INSERT INTO user_tenants (user_id, tenant_id) VALUES
  ('550e8400-e29b-41d4-a716-446655440010','acme-health'),
  ('550e8400-e29b-41d4-a716-446655440011','acme-health'),
  ('550e8400-e29b-41d4-a716-446655440012','acme-health')
  ON CONFLICT DO NOTHING;"

log_info "Seeding CQL libraries for acme-health..."
docker exec -i hdim-demo-postgres psql -U healthdata -d cql_db -c \
  "INSERT INTO cql_libraries (
      id, tenant_id, name, version, status, cql_content, elm_json, description, publisher,
      created_at, updated_at, created_by, library_name, elm_xml, fhir_library_id, active,
      measure_class, category
    )
    SELECT
      gen_random_uuid(), 'acme-health', name, version, status, cql_content, elm_json, description, publisher,
      created_at, updated_at, created_by, library_name, elm_xml, fhir_library_id, active,
      measure_class, category
    FROM cql_libraries source
    WHERE source.tenant_id = 'demo-tenant'
      AND NOT EXISTS (
        SELECT 1 FROM cql_libraries target
        WHERE target.tenant_id = 'acme-health'
          AND target.name = source.name
          AND target.version = source.version
      );"

log_info "Validating demo system..."
bash ./validate-system.sh
bash ./validate-fhir-data.sh
bash ./scripts/validate-all-services-data.sh

if [ "$RUN_SCREENSHOTS" = "true" ]; then
    log_info "Capturing demo screenshots..."
    bash ./scripts/run-demo-screenshots.sh
fi

if [ "$RUN_E2E" = "true" ]; then
    log_info "Running clinical portal E2E suite..."
    npm run e2e:clinical-portal:cli
fi

if [ "$RUN_DEMO_FLOW" = "true" ]; then
    log_info "Launching interactive demo walkthrough..."
    bash ./demo-full-system.sh
fi

log_info "Demo cycle complete."
