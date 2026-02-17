#!/usr/bin/env bash
set -euo pipefail

WORKFLOW_FILE=".github/workflows/frontend-session-flow-e2e.yml"
REF="${1:-$(git rev-parse --abbrev-ref HEAD)}"

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI (gh) is not installed."
  echo "Run manually in GitHub Actions UI:"
  echo "  Workflow: Frontend Session Flow E2E"
  echo "  Modes: base, auth, all"
  exit 0
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated."
  echo "Run: gh auth login"
  exit 0
fi

echo "Dispatching Frontend Session Flow E2E workflow on ref: ${REF}"

for mode in base auth all; do
  echo ""
  echo "==> Dispatch mode: ${mode}"
  gh workflow run "${WORKFLOW_FILE}" --ref "${REF}" -f gate_mode="${mode}"
done

echo ""
echo "Dispatched all modes. Monitor with:"
echo "  gh run list --workflow \"Frontend Session Flow E2E\" --limit 10"
