#!/bin/bash
# Intelligence API Authorization and Tenant Isolation Validation
# Validates expected 401/403/404 behavior for intelligence endpoints in a deployed environment.
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
  echo -e "${RED}ERROR: VERSION not specified${NC}"
  echo "Usage: $0 vX.Y.Z"
  exit 1
fi

cd "$(dirname "$0")/../.." || exit 1

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "$REPORT_DIR"
REPORT_FILE="$REPORT_DIR/intelligence-authz-validation-report.md"

EVENT_SERVICE_BASE_URL="${EVENT_SERVICE_BASE_URL:-http://localhost:8083/events}"
TENANT_ID="${TENANT_ID:-tenant-a}"
OTHER_TENANT_ID="${OTHER_TENANT_ID:-tenant-b}"
AUTH_USER_ID="${AUTH_USER_ID:-00000000-0000-0000-0000-000000000001}"
AUTH_USERNAME="${AUTH_USERNAME:-intelligence-validator}"
AUTH_VALIDATED="${AUTH_VALIDATED:-gateway-dev-mode}"

# Role sets used by intelligence controller @PreAuthorize checks
REVIEWER_ROLES="${REVIEWER_ROLES:-QUALITY_OFFICER}"
VIEWER_ROLES="${VIEWER_ROLES:-CLINICIAN}"

cat > "$REPORT_FILE" <<EOF_REPORT
# Intelligence Authorization Validation Report

## Overview
Validates role-based authorization and tenant-isolation behavior for intelligence APIs.

## Runtime Inputs
- VERSION: ${VERSION}
- EVENT_SERVICE_BASE_URL: ${EVENT_SERVICE_BASE_URL}
- TENANT_ID: ${TENANT_ID}
- OTHER_TENANT_ID: ${OTHER_TENANT_ID}
- REVIEWER_ROLES: ${REVIEWER_ROLES}
- VIEWER_ROLES: ${VIEWER_ROLES}

---
EOF_REPORT

OVERALL_STATUS=0

log_ok() {
  echo -e "${GREEN}✓${NC} $1"
  echo "- ✅ $1" >> "$REPORT_FILE"
}

log_fail() {
  echo -e "${RED}✗${NC} $1"
  echo "- ❌ $1" >> "$REPORT_FILE"
  OVERALL_STATUS=1
}

request_code() {
  local method="$1"
  local url="$2"
  local tenant="$3"
  local roles="$4"
  local body="${5:-}"
  local code

  if [ -n "$roles" ]; then
    if [ -n "$body" ]; then
      code="$(curl -sS -o /dev/null -w "%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $tenant" \
        -H "X-Auth-User-Id: $AUTH_USER_ID" \
        -H "X-Auth-Username: $AUTH_USERNAME" \
        -H "X-Auth-Tenant-Ids: $tenant" \
        -H "X-Auth-Roles: $roles" \
        -H "X-Auth-Validated: $AUTH_VALIDATED" \
        -d "$body" || true)"
    else
      code="$(curl -sS -o /dev/null -w "%{http_code}" -X "$method" "$url" \
        -H "X-Tenant-ID: $tenant" \
        -H "X-Auth-User-Id: $AUTH_USER_ID" \
        -H "X-Auth-Username: $AUTH_USERNAME" \
        -H "X-Auth-Tenant-Ids: $tenant" \
        -H "X-Auth-Roles: $roles" \
        -H "X-Auth-Validated: $AUTH_VALIDATED" || true)"
    fi
  else
    if [ -n "$body" ]; then
      code="$(curl -sS -o /dev/null -w "%{http_code}" -X "$method" "$url" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: $tenant" \
        -d "$body" || true)"
    else
      code="$(curl -sS -o /dev/null -w "%{http_code}" -X "$method" "$url" \
        -H "X-Tenant-ID: $tenant" || true)"
    fi
  fi

  echo "$code"
}

assert_code_one_of() {
  local name="$1"
  local got="$2"
  shift 2
  local expected=("$@")

  for value in "${expected[@]}"; do
    if [ "$got" = "$value" ]; then
      log_ok "$name -> HTTP $got"
      return
    fi
  done

  log_fail "$name -> expected one of [${expected[*]}], got HTTP $got"
}

echo "" >> "$REPORT_FILE"
echo "## Authorization and Isolation Checks" >> "$REPORT_FILE"

# 0) Service liveness
health_code="$(curl -sS -o /dev/null -w "%{http_code}" "${EVENT_SERVICE_BASE_URL}/actuator/health" || true)"
assert_code_one_of "Health endpoint" "$health_code" 200

# 1) Unauthenticated mutable endpoint must be rejected
random_rec_id="$(cat /proc/sys/kernel/random/uuid)"
unauth_review_code="$(request_code POST "${EVENT_SERVICE_BASE_URL}/api/v1/intelligence/recommendations/${random_rec_id}/review" "$TENANT_ID" "" '{"status":"TRIAGED","reviewedBy":"spoof","notes":"test"}')"
assert_code_one_of "Unauthenticated recommendation review rejected" "$unauth_review_code" 401 403

# 2) Authenticated viewer role on mutable endpoint must be forbidden
viewer_review_code="$(request_code POST "${EVENT_SERVICE_BASE_URL}/api/v1/intelligence/recommendations/${random_rec_id}/review" "$TENANT_ID" "$VIEWER_ROLES" '{"status":"TRIAGED","reviewedBy":"spoof","notes":"test"}')"
assert_code_one_of "Viewer role denied recommendation review" "$viewer_review_code" 403

# 3) Cross-tenant trust dashboard path/header mismatch must be hidden as 404
cross_tenant_dashboard_code="$(request_code GET "${EVENT_SERVICE_BASE_URL}/api/v1/intelligence/tenants/${OTHER_TENANT_ID}/trust-dashboard" "$TENANT_ID" "$REVIEWER_ROLES")"
assert_code_one_of "Cross-tenant dashboard hidden" "$cross_tenant_dashboard_code" 404

# 4) Authenticated reviewer can reach mutable endpoint authorization layer
reviewer_review_code="$(request_code POST "${EVENT_SERVICE_BASE_URL}/api/v1/intelligence/recommendations/${random_rec_id}/review" "$TENANT_ID" "$REVIEWER_ROLES" '{"status":"TRIAGED","reviewedBy":"spoof","notes":"test"}')"
# Unknown ID should return 404 after auth; 409 can occur on edge cases; 400 if payload/state fails earlier.
assert_code_one_of "Reviewer role reaches review endpoint" "$reviewer_review_code" 404 409 400

# 5) Authenticated reviewer can reach validation status endpoint authorization layer
random_finding_id="$(cat /proc/sys/kernel/random/uuid)"
reviewer_finding_code="$(request_code POST "${EVENT_SERVICE_BASE_URL}/api/v1/intelligence/validation-findings/${random_finding_id}/status" "$TENANT_ID" "$REVIEWER_ROLES" '{"status":"RESOLVED","actedBy":"spoof","notes":"test"}')"
assert_code_one_of "Reviewer role reaches validation status endpoint" "$reviewer_finding_code" 404 409 400

echo "" >> "$REPORT_FILE"
if [ "$OVERALL_STATUS" -eq 0 ]; then
  echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
  echo -e "${GREEN}Intelligence authz validation PASSED${NC}"
else
  echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
  echo -e "${RED}Intelligence authz validation FAILED${NC}"
fi

echo "Report: $REPORT_FILE"
exit "$OVERALL_STATUS"
