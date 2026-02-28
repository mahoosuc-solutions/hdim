#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="$ROOT_DIR/test-results"
STAMP="$(date -u +%Y-%m-%dT%H%M%SZ)"
MANIFEST_MD="$OUT_DIR/local-evidence-manifest-${STAMP}.md"
SHA_FILE="$OUT_DIR/local-evidence-sha256-${STAMP}.txt"
FILES_TMP="$OUT_DIR/.local-evidence-files-${STAMP}.tmp"

mkdir -p "$OUT_DIR"

{
  ls -1 "$OUT_DIR"/wave1-local-assurance-*.json 2>/dev/null || true
  ls -1 "$OUT_DIR"/wave1-edge-gateway-smoke-*.json 2>/dev/null || true
  ls -1 "$OUT_DIR"/dependency-check-report-*.html 2>/dev/null || true
  ls -1 "$OUT_DIR"/dependency-check-report-*.json 2>/dev/null || true
  ls -1 "$OUT_DIR"/dependency-check-report-*.sarif 2>/dev/null || true
  ls -1 "$OUT_DIR"/gradle-dependency-check-aggregate-*.log 2>/dev/null || true
  ls -1 "$OUT_DIR"/backend-cve-artifacts-manifest-*.md 2>/dev/null || true
  ls -1 "$OUT_DIR"/compliance-evidence-gate*.log 2>/dev/null || true
  ls -1 "$OUT_DIR"/zap-local-2026-02-27/* 2>/dev/null || true
} | sort -u > "$FILES_TMP"

if [[ ! -s "$FILES_TMP" ]]; then
  echo "No matching evidence artifacts found in $OUT_DIR"
  rm -f "$FILES_TMP"
  exit 1
fi

while IFS= read -r file; do
  sha256sum "$file"
done < "$FILES_TMP" > "$SHA_FILE"

COUNT="$(wc -l < "$FILES_TMP" | tr -d ' ')"
{
  echo "# Local Immutable Evidence Manifest"
  echo
  echo "- Generated (UTC): $STAMP"
  echo "- Artifact count: $COUNT"
  echo "- SHA256 file: $(basename "$SHA_FILE")"
  echo
  echo "## Files"
  while IFS= read -r file; do
    echo "- ${file#$ROOT_DIR/}"
  done < "$FILES_TMP"
  echo
  echo "## Verification"
  echo '```bash'
  echo "cd $ROOT_DIR"
  echo "sha256sum -c test-results/$(basename "$SHA_FILE")"
  echo '```'
} > "$MANIFEST_MD"

rm -f "$FILES_TMP"

echo "Manifest: $MANIFEST_MD"
echo "Checksums: $SHA_FILE"
