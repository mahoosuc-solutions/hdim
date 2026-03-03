#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# linkedin-auth.sh — LinkedIn OAuth 2.0 Setup & Token Refresh
#
# Usage:
#   scripts/linkedin-auth.sh            # Initial setup or re-authorization
#   scripts/linkedin-auth.sh --status   # Check token status only
#   scripts/linkedin-auth.sh --refresh  # Force refresh (if refresh token valid)
#
# Writes to: .env.linkedin (gitignored)
# Required OAuth scopes: openid, profile, email, w_member_social
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_ROOT/.env.linkedin"

# ─── Color helpers ────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[OK]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ─── Dependency check ─────────────────────────────────────────────────────────
check_deps() {
  local missing=()
  for cmd in curl python3; do
    if ! command -v "$cmd" &>/dev/null; then
      missing+=("$cmd")
    fi
  done
  if [ ${#missing[@]} -gt 0 ]; then
    error "Missing required tools: ${missing[*]}"
    error "Install them and re-run."
    exit 1
  fi
}

# ─── Parse flags ──────────────────────────────────────────────────────────────
MODE="setup"  # setup | status | refresh
for arg in "$@"; do
  case "$arg" in
    --status)  MODE="status"  ;;
    --refresh) MODE="refresh" ;;
    --help|-h)
      echo "Usage: scripts/linkedin-auth.sh [--status|--refresh|--help]"
      echo ""
      echo "  (no args)    Full OAuth setup or re-authorization"
      echo "  --status     Check current token status"
      echo "  --refresh    Force refresh using stored refresh token"
      echo ""
      echo "LinkedIn tokens expire every 60 days."
      echo "Refresh tokens expire after 365 days (one re-auth per year)."
      exit 0
      ;;
  esac
done

# ─── Token status check ───────────────────────────────────────────────────────
check_status() {
  if [ ! -f "$ENV_FILE" ]; then
    warn ".env.linkedin not found."
    info "Run: scripts/linkedin-auth.sh"
    return 1
  fi

  # shellcheck source=/dev/null
  source "$ENV_FILE"

  TODAY=$(date +%Y-%m-%d)
  echo ""
  echo "LinkedIn Token Status"
  echo "─────────────────────"

  if [ -z "${LINKEDIN_ACCESS_TOKEN:-}" ]; then
    error "No access token found in .env.linkedin"
    return 1
  fi

  if [[ "${LINKEDIN_TOKEN_EXPIRY:-}" < "$TODAY" ]]; then
    error "Token EXPIRED on ${LINKEDIN_TOKEN_EXPIRY}"
    info "Run: scripts/linkedin-auth.sh --refresh"
    info "If refresh fails, run: scripts/linkedin-auth.sh (full re-auth)"
  else
    success "Token valid until: ${LINKEDIN_TOKEN_EXPIRY}"

    # Calculate days remaining
    if command -v python3 &>/dev/null; then
      DAYS_LEFT=$(python3 -c "
from datetime import date, datetime
expiry = datetime.strptime('${LINKEDIN_TOKEN_EXPIRY}', '%Y-%m-%d').date()
today = date.today()
print((expiry - today).days)
")
      if [ "$DAYS_LEFT" -lt 7 ]; then
        warn "Token expires in $DAYS_LEFT days. Refresh soon."
      else
        info "$DAYS_LEFT days remaining."
      fi
    fi
  fi

  echo "Person URN: ${LINKEDIN_PERSON_URN:-NOT SET}"
  echo ""
}

# ─── Token refresh ────────────────────────────────────────────────────────────
do_refresh() {
  if [ ! -f "$ENV_FILE" ]; then
    error ".env.linkedin not found. Run full setup first."
    exit 1
  fi

  # shellcheck source=/dev/null
  source "$ENV_FILE"

  if [ -z "${LINKEDIN_REFRESH_TOKEN:-}" ]; then
    error "No refresh token in .env.linkedin. Run full setup."
    exit 1
  fi

  if [ -z "${LINKEDIN_CLIENT_ID:-}" ] || [ -z "${LINKEDIN_CLIENT_SECRET:-}" ]; then
    error "LINKEDIN_CLIENT_ID or LINKEDIN_CLIENT_SECRET missing from .env.linkedin"
    exit 1
  fi

  info "Refreshing access token..."

  REFRESH_RESPONSE=$(curl -s -X POST https://www.linkedin.com/oauth/v2/accessToken \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=refresh_token&refresh_token=${LINKEDIN_REFRESH_TOKEN}&client_id=${LINKEDIN_CLIENT_ID}&client_secret=${LINKEDIN_CLIENT_SECRET}")

  NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('access_token',''))" 2>/dev/null || echo "")
  EXPIRES_IN=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('expires_in',0))" 2>/dev/null || echo "0")

  if [ -z "$NEW_TOKEN" ]; then
    ERROR_MSG=$(echo "$REFRESH_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('error_description', d.get('error','Unknown error')))" 2>/dev/null || echo "Parse error")
    error "Refresh failed: $ERROR_MSG"
    info "If your refresh token is expired (365 days), run full re-auth:"
    info "  scripts/linkedin-auth.sh"
    exit 1
  fi

  # Calculate expiry date
  NEW_EXPIRY=$(python3 -c "
from datetime import datetime, timedelta
expiry = datetime.now() + timedelta(seconds=${EXPIRES_IN})
print(expiry.strftime('%Y-%m-%d'))
")

  # Update .env.linkedin in place
  sed -i "s|^LINKEDIN_ACCESS_TOKEN=.*|LINKEDIN_ACCESS_TOKEN=${NEW_TOKEN}|" "$ENV_FILE"
  sed -i "s|^LINKEDIN_TOKEN_EXPIRY=.*|LINKEDIN_TOKEN_EXPIRY=${NEW_EXPIRY}|" "$ENV_FILE"

  success "Token refreshed. Valid until: $NEW_EXPIRY"
}

# ─── Full OAuth setup ─────────────────────────────────────────────────────────
do_setup() {
  echo ""
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "  LinkedIn OAuth 2.0 Setup"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "You need a LinkedIn Developer App with these OAuth scopes:"
  echo "  • openid"
  echo "  • profile"
  echo "  • email"
  echo "  • w_member_social  ← required for posting"
  echo ""
  echo "Create app at: https://www.linkedin.com/developers/apps"
  echo "Add redirect URI: http://localhost:8765/callback"
  echo ""

  # Collect credentials
  read -rp "LinkedIn Client ID: " CLIENT_ID
  read -rsp "LinkedIn Client Secret: " CLIENT_SECRET
  echo ""

  if [ -z "$CLIENT_ID" ] || [ -z "$CLIENT_SECRET" ]; then
    error "Client ID and Secret are required."
    exit 1
  fi

  # Build authorization URL
  STATE=$(python3 -c "import secrets; print(secrets.token_urlsafe(16))")
  SCOPE="openid%20profile%20email%20w_member_social"
  REDIRECT_URI="http://localhost:8765/callback"
  AUTH_URL="https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}&scope=${SCOPE}&state=${STATE}"

  echo ""
  info "Opening authorization URL in your browser..."
  echo ""
  echo "If it doesn't open automatically, copy this URL:"
  echo ""
  echo "  $AUTH_URL"
  echo ""

  # Try to open browser
  if command -v xdg-open &>/dev/null; then
    xdg-open "$AUTH_URL" 2>/dev/null || true
  elif command -v open &>/dev/null; then
    open "$AUTH_URL" 2>/dev/null || true
  fi

  echo "─────────────────────────────────────────────────────"
  echo "After authorizing, LinkedIn will redirect to:"
  echo "  http://localhost:8765/callback?code=AUTH_CODE&state=..."
  echo ""
  echo "Copy the FULL redirect URL and paste it below."
  echo "─────────────────────────────────────────────────────"
  echo ""
  read -rp "Paste the redirect URL: " REDIRECT_URL

  # Extract authorization code
  AUTH_CODE=$(echo "$REDIRECT_URL" | python3 -c "
import sys
from urllib.parse import urlparse, parse_qs
url = sys.stdin.read().strip()
parsed = urlparse(url)
params = parse_qs(parsed.query)
print(params.get('code', [''])[0])
" 2>/dev/null || echo "")

  RETURNED_STATE=$(echo "$REDIRECT_URL" | python3 -c "
import sys
from urllib.parse import urlparse, parse_qs
url = sys.stdin.read().strip()
parsed = urlparse(url)
params = parse_qs(parsed.query)
print(params.get('state', [''])[0])
" 2>/dev/null || echo "")

  if [ -z "$AUTH_CODE" ]; then
    error "Could not extract authorization code from URL."
    error "URL received: $REDIRECT_URL"
    exit 1
  fi

  # Validate state to prevent CSRF
  if [ "$RETURNED_STATE" != "$STATE" ]; then
    error "State mismatch! Possible CSRF. Expected: $STATE, Got: $RETURNED_STATE"
    exit 1
  fi

  info "Exchanging authorization code for tokens..."

  TOKEN_RESPONSE=$(curl -s -X POST https://www.linkedin.com/oauth/v2/accessToken \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=authorization_code&code=${AUTH_CODE}&redirect_uri=${REDIRECT_URI}&client_id=${CLIENT_ID}&client_secret=${CLIENT_SECRET}")

  ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('access_token',''))" 2>/dev/null || echo "")
  REFRESH_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('refresh_token',''))" 2>/dev/null || echo "")
  EXPIRES_IN=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('expires_in',5184000))" 2>/dev/null || echo "5184000")

  if [ -z "$ACCESS_TOKEN" ]; then
    ERROR_DESC=$(echo "$TOKEN_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('error_description', d.get('error','Unknown')))" 2>/dev/null || echo "Parse error")
    error "Token exchange failed: $ERROR_DESC"
    error "Response: $TOKEN_RESPONSE"
    exit 1
  fi

  TOKEN_EXPIRY=$(python3 -c "
from datetime import datetime, timedelta
expiry = datetime.now() + timedelta(seconds=${EXPIRES_IN})
print(expiry.strftime('%Y-%m-%d'))
")

  # Fetch person URN
  info "Fetching your LinkedIn Person URN..."
  PROFILE_RESPONSE=$(curl -s https://api.linkedin.com/v2/me \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-Restli-Protocol-Version: 2.0.0")

  PERSON_ID=$(echo "$PROFILE_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || echo "")

  if [ -z "$PERSON_ID" ]; then
    warn "Could not fetch person ID. You'll need to set LINKEDIN_PERSON_URN manually."
    warn "Response: $PROFILE_RESPONSE"
    PERSON_URN="urn:li:person:REPLACE_ME"
  else
    PERSON_URN="urn:li:person:${PERSON_ID}"
    success "Person URN: $PERSON_URN"
  fi

  # Write .env.linkedin
  cat > "$ENV_FILE" <<EOF
# LinkedIn OAuth Credentials
# DO NOT COMMIT — this file is gitignored
# Re-run scripts/linkedin-auth.sh to refresh or re-authorize
# Tokens expire every 60 days; refresh tokens expire after 365 days

LINKEDIN_CLIENT_ID=${CLIENT_ID}
LINKEDIN_CLIENT_SECRET=${CLIENT_SECRET}
LINKEDIN_ACCESS_TOKEN=${ACCESS_TOKEN}
LINKEDIN_REFRESH_TOKEN=${REFRESH_TOKEN}
LINKEDIN_TOKEN_EXPIRY=${TOKEN_EXPIRY}
LINKEDIN_PERSON_URN=${PERSON_URN}
EOF

  chmod 600 "$ENV_FILE"

  echo ""
  success "Setup complete!"
  echo ""
  echo "  Token valid until: $TOKEN_EXPIRY"
  echo "  Person URN:        $PERSON_URN"
  echo "  Credentials file:  .env.linkedin (600 permissions, gitignored)"
  echo ""
  info "Test with: /social:linkedin --topic 'your topic' --mode thought-leadership"
  echo ""

  # Reminder for refresh
  warn "Set a calendar reminder to re-run this script before $TOKEN_EXPIRY"
  warn "Or use: scripts/linkedin-auth.sh --refresh (before expiry)"
}

# ─── Main ─────────────────────────────────────────────────────────────────────
check_deps

case "$MODE" in
  status)  check_status ;;
  refresh) do_refresh ;;
  setup)   do_setup ;;
esac
