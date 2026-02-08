#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# HDIM Platform — Build Customer-Simulation Docker Images
# ─────────────────────────────────────────────────────────────────────────────
# These images simulate a customer's PRE-EXISTING infrastructure that the HDIM
# platform layers on top of.  They are NOT part of the product — they exist
# solely for demos, integration tests, and local development.
#
# Image naming:  hdim/simulation-<role>:<version>
# Version:       git describe --tags --always
#
# Services built:
#   hdim/simulation-customer-ehr      HAPI FHIR R4 (simulates Epic/Cerner/etc.)
#   hdim/simulation-customer-cdr      PostgreSQL + clinical schema
#
# Usage:
#   ./scripts/build-simulation-images.sh              # build all
#   ./scripts/build-simulation-images.sh --push       # build + push
#   REGISTRY=ghcr.io/myorg ./scripts/build-simulation-images.sh --push
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SIM_DIR="$ROOT_DIR/docker/simulation"
REGISTRY="${REGISTRY:-}"
VERSION="${VERSION:-$(git -C "$ROOT_DIR" describe --tags --always 2>/dev/null || echo 'latest')}"
PUSH="${1:-}"

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  HDIM Customer-Simulation Image Builder                         ║"
echo "║  Version : $VERSION"
echo "║  Registry: ${REGISTRY:-<local>}"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

built=0
failed=0

# ── Customer EHR (HAPI FHIR R4) ──────────────────────────────────────────────
echo "── Building hdim/simulation-customer-ehr:${VERSION} ──"
ehr_tag="hdim/simulation-customer-ehr:${VERSION}"
ehr_latest="hdim/simulation-customer-ehr:latest"

if docker build \
  -t "$ehr_tag" \
  -t "$ehr_latest" \
  -f "$SIM_DIR/customer-ehr/Dockerfile" \
  "$SIM_DIR/customer-ehr"; then
  echo "   ✓ ${ehr_tag}"
  ((built++))

  if [[ "$PUSH" == "--push" && -n "$REGISTRY" ]]; then
    docker tag "$ehr_tag"     "${REGISTRY}/${ehr_tag}"
    docker tag "$ehr_latest"  "${REGISTRY}/${ehr_latest}"
    docker push "${REGISTRY}/${ehr_tag}"
    docker push "${REGISTRY}/${ehr_latest}"
    echo "   ✓ pushed → ${REGISTRY}/${ehr_tag}"
  fi
else
  echo "   ✗ FAILED: ${ehr_tag}"
  ((failed++))
fi
echo ""

# ── Customer CDR (PostgreSQL + clinical schema) ──────────────────────────────
echo "── Building hdim/simulation-customer-cdr:${VERSION} ──"
cdr_tag="hdim/simulation-customer-cdr:${VERSION}"
cdr_latest="hdim/simulation-customer-cdr:latest"

if docker build \
  -t "$cdr_tag" \
  -t "$cdr_latest" \
  -f "$SIM_DIR/customer-cdr/Dockerfile" \
  "$SIM_DIR/customer-cdr"; then
  echo "   ✓ ${cdr_tag}"
  ((built++))

  if [[ "$PUSH" == "--push" && -n "$REGISTRY" ]]; then
    docker tag "$cdr_tag"     "${REGISTRY}/${cdr_tag}"
    docker tag "$cdr_latest"  "${REGISTRY}/${cdr_latest}"
    docker push "${REGISTRY}/${cdr_tag}"
    docker push "${REGISTRY}/${cdr_latest}"
    echo "   ✓ pushed → ${REGISTRY}/${cdr_tag}"
  fi
else
  echo "   ✗ FAILED: ${cdr_tag}"
  ((failed++))
fi

echo ""
echo "════════════════════════════════════════════════════════════════════"
echo "  Simulation images: ${built} built, ${failed} failed"
echo "  Version: ${VERSION}"
echo "════════════════════════════════════════════════════════════════════"

if [[ $failed -gt 0 ]]; then
  exit 1
fi
