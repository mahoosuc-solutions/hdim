#!/usr/bin/env bash
set -euo pipefail

# Synthea wrapper — generates FHIR R4 bundles for each phenotype.
# Bundles are committed to git; CI does NOT need Synthea installed.
# Run this only when phenotype definitions change.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUNDLES_DIR="$SCRIPT_DIR/bundles"
MANIFEST="$SCRIPT_DIR/manifest.json"

if ! command -v java &> /dev/null; then
  echo "ERROR: Java 11+ required for Synthea" >&2
  exit 1
fi

SYNTHEA_JAR="${SYNTHEA_JAR:-$HOME/synthea/build/libs/synthea-with-dependencies.jar}"
if [ ! -f "$SYNTHEA_JAR" ]; then
  echo "ERROR: Synthea JAR not found at $SYNTHEA_JAR" >&2
  echo "Set SYNTHEA_JAR env var or install Synthea: https://github.com/synthetichealth/synthea" >&2
  exit 1
fi

echo "Generating synthetic patient bundles..."
echo "Manifest: $MANIFEST"
echo "Output: $BUNDLES_DIR"

PHENOTYPE_COUNT=$(node -e "console.log(require('$MANIFEST').phenotypes.length)")
echo "Phenotypes: $PHENOTYPE_COUNT"

for i in $(seq 0 $((PHENOTYPE_COUNT - 1))); do
  ID=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].id)")
  SEED=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.seed)")
  MODULE=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.module)")
  AGE=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.age)")
  GENDER=$(node -e "console.log(require('$MANIFEST').phenotypes[$i].synthea.gender)")

  echo "  Generating: $ID (seed=$SEED, module=$MODULE, age=$AGE, gender=$GENDER)"

  java -jar "$SYNTHEA_JAR" \
    --exporter.fhir.transaction_bundle true \
    --exporter.baseDirectory "$BUNDLES_DIR/tmp-$ID" \
    --generate.seed "$SEED" \
    -m "$MODULE" \
    -a "$AGE-$AGE" \
    -g "$GENDER" \
    -p 1 \
    --exporter.years_of_history 10

  mv "$BUNDLES_DIR/tmp-$ID/fhir/"*.json "$BUNDLES_DIR/$ID.json" 2>/dev/null || true
  rm -rf "$BUNDLES_DIR/tmp-$ID"
done

echo "Done! Generated $PHENOTYPE_COUNT bundles."
echo "Review bundles, apply overlays, then commit."
