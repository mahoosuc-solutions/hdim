#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT_DIR="$ROOT/docs/review/evidence"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
OUT_FILE="$OUT_DIR/technical-review-evidence-$STAMP.md"

mkdir -p "$OUT_DIR"

services_count=$(find "$ROOT/backend/modules/services" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')
apps_count=$(find "$ROOT/apps" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')
code_lines=$(rg --files "$ROOT/backend" "$ROOT/apps" "$ROOT/libs" --glob '*.java' --glob '*.ts' --glob '*.tsx' --glob '*.js' --glob '*.kt' --glob '*.kts' | xargs wc -l | tail -n 1 | awk '{print $1}')

{
  echo "# Technical Review Evidence"
  echo
  echo "- Generated (UTC): $(date -u '+%Y-%m-%d %H:%M:%S')"
  echo "- Repository: $ROOT"
  echo "- Commit: $(git -C "$ROOT" rev-parse HEAD)"
  echo "- Branch: $(git -C "$ROOT" branch --show-current)"
  echo
  echo "## Inventory Metrics"
  echo
  echo "- Backend service directories: $services_count"
  echo "- App directories: $apps_count"
  echo "- Code line count (java/ts/js/kt/kts): $code_lines"
  echo
  echo "## Git Tags"
  echo
  echo '```text'
  git -C "$ROOT" tag --list | sort -V
  echo '```'
  echo
  echo "## GitHub Releases"
  echo
  echo '```text'
  gh release list --repo webemo-aaron/hdim --limit 50
  echo '```'
  echo
  echo "## v2.7 Release API Verification"
  echo
  echo '```json'
  gh api repos/webemo-aaron/hdim/releases/tags/v2.7.0
  echo '```'
  echo
  echo '```json'
  gh api repos/webemo-aaron/hdim/releases/tags/v2.7.1-rc2
  echo '```'
  echo
  echo "## Architecture References"
  echo
  echo "- docs/architecture/SYSTEM_ARCHITECTURE.md"
  echo "- yc-application-v2/TECHNICAL_ARCHITECTURE.md"
  echo "- docs/releases/v2.7.1-rc2/RC2_GO_NO_GO_CHECKLIST.md"
  echo
  echo "## Reviewer Commands"
  echo
  echo '```bash'
  echo './validate-system.sh'
  echo 'npm run e2e:clinical-portal:smoke'
  echo 'docker compose -f docker-compose.demo.yml up -d'
  echo './scripts/seed-all-demo-data.sh'
  echo '```'
} > "$OUT_FILE"

echo "Evidence written: $OUT_FILE"
