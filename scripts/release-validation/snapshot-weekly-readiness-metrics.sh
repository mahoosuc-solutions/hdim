#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-v0.0.0-test}"
STAMP="$(date -u +%Y%m%d)"
BASE_DIR="docs/releases/${VERSION}"
HIST_DIR="${BASE_DIR}/metrics-history"
mkdir -p "$HIST_DIR"

ROI_PACK="$(ls -1 docs/investor/ROI_DEFENSIBILITY_PACK_*.md 2>/dev/null | grep -v 'TEMPLATE' | sort | tail -n 1 || true)"

cp "${BASE_DIR}/validation/pilot-scorecard.md" "${HIST_DIR}/pilot-scorecard-${STAMP}.md"
if [[ -n "${ROI_PACK}" ]]; then
  cp "${ROI_PACK}" "${HIST_DIR}/roi-defensibility-${STAMP}.md"
else
  echo "No ROI defensibility pack found under docs/investor/ROI_DEFENSIBILITY_PACK_*.md"
  exit 1
fi

echo "Weekly readiness metric snapshots created:"
echo "- ${HIST_DIR}/pilot-scorecard-${STAMP}.md"
echo "- ${HIST_DIR}/roi-defensibility-${STAMP}.md"
