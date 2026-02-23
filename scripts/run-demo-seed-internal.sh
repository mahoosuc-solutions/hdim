#!/bin/bash
# Seed demo schedule data using internal container IPs
# Run from workspace root

FHIR_URL="http://172.28.0.8:8085/fhir"
SEED_SCHEDULE_MODE="both"
GATEWAY_URL="http://localhost:18080"
TENANT_ID="acme-health"

export FHIR_URL SEED_SCHEDULE_MODE GATEWAY_URL TENANT_ID

echo "Starting schedule seed..."
echo "FHIR_URL=$FHIR_URL"
echo "GATEWAY_URL=$GATEWAY_URL"
echo "TENANT_ID=$TENANT_ID"
echo ""

bash scripts/seed-fhir-schedule.sh
