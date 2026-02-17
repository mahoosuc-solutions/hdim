#!/usr/bin/env bash
set -euo pipefail

BASE_REF="${1:-origin/main}"

if ! command -v git >/dev/null 2>&1; then
  echo "git is required"
  exit 1
fi

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Run this script inside a git repository"
  exit 1
fi

if git rev-parse --verify "$BASE_REF" >/dev/null 2>&1; then
  CHANGED_FILES="$(git diff --name-only "$BASE_REF"...HEAD)"
else
  echo "Base ref '$BASE_REF' not found locally; falling back to staged+working tree diff."
  CHANGED_FILES="$(git diff --name-only --cached; git diff --name-only)"
fi

AUTH_GLOB_REGEX='^frontend/(src/components/LoginPage\.tsx|src/components/ExternalAuthMockPage\.tsx|src/components/ConnectionStatus\.tsx|src/components/AppShell\.tsx|src/components/AuthCallbackPage\.tsx|src/app/integrations/|e2e/tests/session-expiry-external-auth\.spec\.ts|e2e/tests/auth-callback\.spec\.ts|e2e/playwright\.session\.external-auth\.config\.ts|e2e/playwright\.auth-callback\.config\.ts|\.env\.example|package\.json|src/main\.tsx)$|^\.github/workflows/frontend-session-flow-e2e\.yml$'

if echo "$CHANGED_FILES" | rg -q '^frontend/'; then
  RUN_SESSION_FLOW=true
else
  RUN_SESSION_FLOW=false
fi

if echo "$CHANGED_FILES" | rg -q "$AUTH_GLOB_REGEX"; then
  RUN_EXTERNAL_AUTH=true
else
  RUN_EXTERNAL_AUTH=false
fi

RUN_AUTH_CALLBACK="$RUN_EXTERNAL_AUTH"

echo "Session flow gate prediction"
echo "============================"
echo "Base ref: $BASE_REF"
echo
echo "Changed files:"
if [ -n "${CHANGED_FILES}" ]; then
  echo "$CHANGED_FILES"
else
  echo "(none detected)"
fi
echo
echo "Expected checks:"
echo "- Frontend Session Flow E2E / session-flow: ${RUN_SESSION_FLOW}"
echo "- Frontend Session Flow E2E / session-flow-external-auth: ${RUN_EXTERNAL_AUTH}"
echo "- Frontend Session Flow E2E / auth-callback: ${RUN_AUTH_CALLBACK}"
