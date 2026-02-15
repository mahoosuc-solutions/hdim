#!/bin/bash
# Generate release image manifest with immutable digests.
#
# Usage:
#   ./scripts/release-validation/generate-image-manifest.sh v1.2.3
#
# Required environment variables:
#   REGISTRY (default: ghcr.io)
#   REPOSITORY_OWNER (required)
#   IMAGE_PREFIX (default: hdim)
#   SERVICES_JSON (optional; defaults to core service list)

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
  echo -e "${RED}ERROR: VERSION not specified${NC}"
  echo "Usage: $0 v1.2.3"
  exit 1
fi

REGISTRY="${REGISTRY:-ghcr.io}"
REPOSITORY_OWNER="${REPOSITORY_OWNER:-}"
IMAGE_PREFIX="${IMAGE_PREFIX:-hdim}"
SERVICES_JSON="${SERVICES_JSON:-}"

if [ -z "$REPOSITORY_OWNER" ]; then
  echo -e "${RED}ERROR: REPOSITORY_OWNER is required${NC}"
  exit 1
fi

if [ -z "$SERVICES_JSON" ]; then
  SERVICES_JSON='["gateway-service","cql-engine-service","consent-service","event-processing-service","patient-service","fhir-service","care-gap-service","quality-measure-service","agent-runtime-service","data-enrichment-service","ai-assistant-service","documentation-service","analytics-service","predictive-analytics-service","sdoh-service","event-router-service","agent-builder-service","approval-service","payer-workflows-service","cdr-processor-service","ehr-connector-service"]'
fi

cd "$(dirname "$0")/../.." || exit 1

ARTIFACT_DIR="docs/releases/${VERSION}/artifacts"
mkdir -p "$ARTIFACT_DIR"
MANIFEST_FILE="${ARTIFACT_DIR}/docker-image-manifest.json"
LATEST_MANIFEST_FILE="docs/releases/latest-docker-image-manifest.json"

TMP_MANIFEST="$(mktemp)"

echo "{\"version\":\"${VERSION}\",\"generated_at\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"registry\":\"${REGISTRY}\",\"repository_owner\":\"${REPOSITORY_OWNER}\",\"image_prefix\":\"${IMAGE_PREFIX}\",\"images\":[]}" > "$TMP_MANIFEST"

mapfile -t SERVICES < <(echo "$SERVICES_JSON" | jq -r '.[]')

for service in "${SERVICES[@]}"; do
  image_ref="${REGISTRY}/${REPOSITORY_OWNER}/${IMAGE_PREFIX}/${service}:${VERSION}"

  echo "Resolving digest for ${image_ref}"
  docker pull "$image_ref" >/dev/null

  repo_digest="$(docker inspect --format '{{index .RepoDigests 0}}' "$image_ref" 2>/dev/null || true)"
  if [ -z "$repo_digest" ] || [[ "$repo_digest" != *"@sha256:"* ]]; then
    echo -e "${RED}ERROR: Could not resolve digest for ${image_ref}${NC}"
    exit 1
  fi

  digest="${repo_digest##*@}"

  jq \
    --arg service "$service" \
    --arg image_ref "$image_ref" \
    --arg repo_digest "$repo_digest" \
    --arg digest "$digest" \
    '.images += [{"service":$service,"image_ref":$image_ref,"repo_digest":$repo_digest,"digest":$digest}]' \
    "$TMP_MANIFEST" > "${TMP_MANIFEST}.next"
  mv "${TMP_MANIFEST}.next" "$TMP_MANIFEST"
done

jq '.image_count = (.images | length)' "$TMP_MANIFEST" > "$MANIFEST_FILE"
cp "$MANIFEST_FILE" "$LATEST_MANIFEST_FILE"

rm -f "$TMP_MANIFEST"

echo -e "${GREEN}Generated manifest:${NC} ${MANIFEST_FILE}"
echo -e "${GREEN}Updated latest:${NC} ${LATEST_MANIFEST_FILE}"
