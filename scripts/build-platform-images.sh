#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# HDIM Platform — Build Platform Docker Images
# ─────────────────────────────────────────────────────────────────────────────
# Builds the HDIM reference-implementation images.  These are the services a
# customer actually deploys — they contain zero demo/simulation code.
#
# Image naming:  hdim/platform-<service>:<version>
# Version:       git describe --tags --always  (e.g. v1.2.3 or abc1234)
#
# Usage:
#   ./scripts/build-platform-images.sh              # build all
#   ./scripts/build-platform-images.sh --push       # build + push to registry
#   REGISTRY=ghcr.io/myorg ./scripts/build-platform-images.sh --push
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
REGISTRY="${REGISTRY:-}"                     # optional: ghcr.io/org, gcr.io/project, etc.
VERSION="${VERSION:-$(git -C "$ROOT_DIR" describe --tags --always 2>/dev/null || echo 'latest')}"
PUSH="${1:-}"

# ── Platform Services ────────────────────────────────────────────────────────
# Each entry: <image-suffix>|<dockerfile-relative-to-backend>|<build-context>
PLATFORM_SERVICES=(
  "gateway-admin|modules/services/gateway-admin-service/Dockerfile|$BACKEND_DIR/modules/services/gateway-admin-service"
  "gateway-fhir|modules/services/gateway-fhir-service/Dockerfile|$BACKEND_DIR/modules/services/gateway-fhir-service"
  "gateway-clinical|modules/services/gateway-clinical-service/Dockerfile|$BACKEND_DIR/modules/services/gateway-clinical-service"
  "fhir|modules/services/fhir-service/Dockerfile|$BACKEND_DIR"
  "cql-engine|modules/services/cql-engine-service/Dockerfile|$BACKEND_DIR"
  "quality-measure|modules/services/quality-measure-service/Dockerfile|$BACKEND_DIR"
  "patient|modules/services/patient-service/Dockerfile|$BACKEND_DIR"
  "care-gap|modules/services/care-gap-service/Dockerfile|$BACKEND_DIR"
  "event-processing|modules/services/event-processing-service/Dockerfile|$BACKEND_DIR"
  "audit-query|modules/services/audit-query-service/Dockerfile|$BACKEND_DIR"
  "hcc|modules/services/hcc-service/Dockerfile|$BACKEND_DIR"
  "demo-seeding|modules/services/demo-seeding-service/Dockerfile|$BACKEND_DIR"
)

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  HDIM Platform Image Builder                                    ║"
echo "║  Version : $VERSION"
echo "║  Registry: ${REGISTRY:-<local>}"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

built=0
failed=0

for entry in "${PLATFORM_SERVICES[@]}"; do
  IFS='|' read -r suffix dockerfile context <<< "$entry"
  image_name="hdim/platform-${suffix}"
  full_tag="${image_name}:${VERSION}"
  latest_tag="${image_name}:latest"

  echo "── Building ${full_tag} ──"

  if docker build \
    -t "$full_tag" \
    -t "$latest_tag" \
    -f "$BACKEND_DIR/$dockerfile" \
    "$context"; then
    echo "   ✓ ${full_tag}"
    ((built++))

    if [[ "$PUSH" == "--push" && -n "$REGISTRY" ]]; then
      remote_tag="${REGISTRY}/${full_tag}"
      remote_latest="${REGISTRY}/${latest_tag}"
      docker tag "$full_tag" "$remote_tag"
      docker tag "$latest_tag" "$remote_latest"
      docker push "$remote_tag"
      docker push "$remote_latest"
      echo "   ✓ pushed → ${remote_tag}"
    fi
  else
    echo "   ✗ FAILED: ${full_tag}"
    ((failed++))
  fi
  echo ""
done

# ── Portal (Angular / nginx) ─────────────────────────────────────────────────
echo "── Building hdim/platform-clinical-portal:${VERSION} ──"
portal_tag="hdim/platform-clinical-portal:${VERSION}"
portal_latest="hdim/platform-clinical-portal:latest"

if docker build \
  -t "$portal_tag" \
  -t "$portal_latest" \
  -f "$ROOT_DIR/apps/clinical-portal/Dockerfile" \
  "$ROOT_DIR"; then
  echo "   ✓ ${portal_tag}"
  ((built++))

  if [[ "$PUSH" == "--push" && -n "$REGISTRY" ]]; then
    remote_tag="${REGISTRY}/${portal_tag}"
    remote_latest="${REGISTRY}/${portal_latest}"
    docker tag "$portal_tag" "$remote_tag"
    docker tag "$portal_latest" "$remote_latest"
    docker push "$remote_tag"
    docker push "$remote_latest"
    echo "   ✓ pushed → ${remote_tag}"
  fi
else
  echo "   ✗ FAILED: ${portal_tag}"
  ((failed++))
fi

# ── Gateway Edge (nginx config only, no build needed — uses stock image) ─────
echo ""
echo "ℹ  gateway-edge uses nginx:1.27-alpine stock image (no custom build)."

echo ""
echo "════════════════════════════════════════════════════════════════════"
echo "  Platform images: ${built} built, ${failed} failed"
echo "  Version: ${VERSION}"
echo "════════════════════════════════════════════════════════════════════"

if [[ $failed -gt 0 ]]; then
  exit 1
fi
