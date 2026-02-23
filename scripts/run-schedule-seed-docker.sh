#!/bin/bash
# Run FHIR schedule seed inside the Docker network using Alpine
# This avoids the host port access problems since 172.28.x.x is not host-accessible

set -e

WORKSPACE="/mnt/wdblack/dev/projects/hdim-master"
NETWORK="demo_hdim-demo-network"
FHIR_URL="http://fhir-service:8085/fhir"
GATEWAY_URL="http://gateway-edge:8080"
TENANT_ID="acme-health"
SEED_SCHEDULE_MODE="both"

echo "Starting FHIR schedule seed in Docker network: $NETWORK"
echo "FHIR_URL=$FHIR_URL"
echo "GATEWAY_URL=$GATEWAY_URL"
echo "TENANT_ID=$TENANT_ID"
echo "SEED_SCHEDULE_MODE=$SEED_SCHEDULE_MODE"
echo ""

docker run --rm \
  --network "$NETWORK" \
  -e FHIR_URL="$FHIR_URL" \
  -e GATEWAY_URL="$GATEWAY_URL" \
  -e TENANT_ID="$TENANT_ID" \
  -e SEED_SCHEDULE_MODE="$SEED_SCHEDULE_MODE" \
  -v "$WORKSPACE/scripts:/scripts:ro" \
  alpine sh -c "apk add -q curl jq python3 bash 2>/dev/null && bash /scripts/seed-fhir-schedule.sh"

echo ""
echo "=== Schedule seed complete ==="
