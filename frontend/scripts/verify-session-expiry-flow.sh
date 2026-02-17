#!/usr/bin/env bash
set -euo pipefail

APP_URL="${APP_URL:-http://localhost:5173}"
EVAL_URL="${APP_URL}/evaluations"
LOGIN_URL="${APP_URL}/login"

cat <<EOF
Session-expiry live verification checklist
========================================
1) Ensure frontend is running:
   npm --prefix frontend run dev

2) Ensure .env contains:
   VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK=true
   VITE_SESSION_EXPIRED_REDIRECT_URL=/login

3) Open:
   ${EVAL_URL}

4) Force /health/integrations to return HTTP 401 in your dev backend/proxy.
   Dev shortcut: click "Force 401: On" in the connection status area.

Expected results:
- Connection status transitions to "Session Expired"
- Repeated 401 polling stops
- localStorage authToken and refreshToken are removed
- Browser is redirected to: ${LOGIN_URL}
- Refreshing ${EVAL_URL} keeps you on ${LOGIN_URL} until sign-in
- Clicking "Sign in (dev)" returns you to ${EVAL_URL}
EOF
