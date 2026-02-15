#!/bin/bash
# Validates CX access admin API endpoints and auth enforcement.
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SERVICE_URL="${SERVICE_URL:-}"
ADMIN_BEARER_TOKEN="${ADMIN_BEARER_TOKEN:-}"
ALLOW_WRITE="${ALLOW_WRITE:-false}"
TEST_EMAIL="${TEST_EMAIL:-}"
VERSION="${1:-${VERSION:-}}"

if [ -z "$SERVICE_URL" ]; then
  echo -e "${RED}ERROR: SERVICE_URL is required${NC}"
  echo "Example:"
  echo "  SERVICE_URL=https://cx-api-xxxx.a.run.app ./scripts/release-validation/validate-cx-access-admin.sh"
  exit 1
fi

BASE_URL="${SERVICE_URL%/}"
OVERALL_STATUS=0
CHECKS=0
PASS=0
WARN=0
REPORT_FILE=""

if [ -n "$VERSION" ]; then
  REPORT_DIR="docs/releases/${VERSION}/validation"
  mkdir -p "$REPORT_DIR"
  REPORT_FILE="$REPORT_DIR/cx-access-admin-validation-report.md"
  {
    echo "# CX Access Admin Validation Report"
    echo ""
    echo "- Version: $VERSION"
    echo "- Service URL: $BASE_URL"
    echo "- Timestamp: $(date '+%Y-%m-%d %H:%M:%S')"
    echo ""
    echo "## Results"
    echo ""
  } > "$REPORT_FILE"
fi

echo "=========================================="
echo "CX Access Admin Validation"
echo "=========================================="
echo "SERVICE_URL: $BASE_URL"
echo "ALLOW_WRITE: $ALLOW_WRITE"
echo ""

check_status() {
  local name="$1"
  local url="$2"
  local expected="$3"
  local auth_header="${4:-}"
  CHECKS=$((CHECKS + 1))

  local code
  if [ -n "$auth_header" ]; then
    code="$(curl -sS -o /tmp/cx-access-admin-body.txt -w '%{http_code}' -H "$auth_header" "$url" || true)"
  else
    code="$(curl -sS -o /tmp/cx-access-admin-body.txt -w '%{http_code}' "$url" || true)"
  fi

  if [ "$code" = "$expected" ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}✓${NC} $name ($code)"
    if [ -n "$REPORT_FILE" ]; then
      echo "- ✅ $name (HTTP $code)" >> "$REPORT_FILE"
    fi
  else
    OVERALL_STATUS=1
    echo -e "${RED}✗${NC} $name expected=$expected got=$code"
    echo "  URL: $url"
    echo "  Response:"
    sed -n '1,5p' /tmp/cx-access-admin-body.txt || true
    if [ -n "$REPORT_FILE" ]; then
      echo "- ❌ $name (expected $expected, got $code)" >> "$REPORT_FILE"
    fi
  fi
}

check_status_any() {
  local name="$1"
  local url="$2"
  local expected_csv="$3"
  local auth_header="${4:-}"
  CHECKS=$((CHECKS + 1))

  local code
  if [ -n "$auth_header" ]; then
    code="$(curl -sS -o /tmp/cx-access-admin-body.txt -w '%{http_code}' -H "$auth_header" "$url" || true)"
  else
    code="$(curl -sS -o /tmp/cx-access-admin-body.txt -w '%{http_code}' "$url" || true)"
  fi

  local match=0
  IFS=',' read -r -a expected_codes <<< "$expected_csv"
  for expected in "${expected_codes[@]}"; do
    if [ "$code" = "$expected" ]; then
      match=1
      break
    fi
  done

  if [ $match -eq 1 ]; then
    PASS=$((PASS + 1))
    echo -e "${GREEN}✓${NC} $name ($code)"
    if [ -n "$REPORT_FILE" ]; then
      echo "- ✅ $name (HTTP $code)" >> "$REPORT_FILE"
    fi
  else
    OVERALL_STATUS=1
    echo -e "${RED}✗${NC} $name expected one of [$expected_csv] got=$code"
    echo "  URL: $url"
    echo "  Response:"
    sed -n '1,5p' /tmp/cx-access-admin-body.txt || true
    if [ -n "$REPORT_FILE" ]; then
      echo "- ❌ $name (expected one of [$expected_csv], got $code)" >> "$REPORT_FILE"
    fi
  fi
}

check_status "Health endpoint" "$BASE_URL/health" "200"
check_status "Auth config endpoint" "$BASE_URL/api/auth/config" "200"

# These admin endpoints must not be publicly accessible.
check_status_any \
  "Customer access endpoint requires auth" \
  "$BASE_URL/api/admin/customer-access" \
  "401,403"

check_status_any \
  "Identity access endpoint requires auth" \
  "$BASE_URL/api/admin/identity-access" \
  "401,403"

if [ -z "$ADMIN_BEARER_TOKEN" ]; then
  WARN=$((WARN + 1))
  echo -e "${YELLOW}!${NC} Skipping authenticated checks (set ADMIN_BEARER_TOKEN to enable)."
  if [ -n "$REPORT_FILE" ]; then
    echo "- ⚠️ Authenticated checks skipped (missing ADMIN_BEARER_TOKEN)" >> "$REPORT_FILE"
  fi
else
  AUTH_HEADER="Authorization: Bearer $ADMIN_BEARER_TOKEN"

  check_status "Customer access list (admin token)" "$BASE_URL/api/admin/customer-access" "200" "$AUTH_HEADER"
  check_status "Identity access list (admin token)" "$BASE_URL/api/admin/identity-access" "200" "$AUTH_HEADER"

  if [ "$ALLOW_WRITE" = "true" ]; then
    if [ -z "$TEST_EMAIL" ]; then
      TEST_EMAIL="cx-access-test-$(date +%s)@example.com"
    fi
    CHECKS=$((CHECKS + 1))
    write_code="$(curl -sS -o /tmp/cx-access-admin-body.txt -w '%{http_code}' \
      -X POST \
      -H "$AUTH_HEADER" \
      -H 'Content-Type: application/json' \
      -d "{\"email\":\"$TEST_EMAIL\",\"principal_type\":\"staff\",\"staff_role\":\"internal\",\"active\":true}" \
      "$BASE_URL/api/admin/identity-access" || true)"

    if [ "$write_code" = "200" ] || [ "$write_code" = "201" ]; then
      PASS=$((PASS + 1))
      echo -e "${GREEN}✓${NC} Identity access write test ($write_code)"
      if [ -n "$REPORT_FILE" ]; then
        echo "- ✅ Identity access write test (HTTP $write_code)" >> "$REPORT_FILE"
      fi
    else
      OVERALL_STATUS=1
      echo -e "${RED}✗${NC} Identity access write test expected 200/201 got=$write_code"
      sed -n '1,5p' /tmp/cx-access-admin-body.txt || true
      if [ -n "$REPORT_FILE" ]; then
        echo "- ❌ Identity access write test (expected 200/201, got $write_code)" >> "$REPORT_FILE"
      fi
    fi

    CHECKS=$((CHECKS + 1))
    delete_code="$(curl -sS -o /tmp/cx-access-admin-body.txt -w '%{http_code}' \
      -X DELETE \
      -H "$AUTH_HEADER" \
      "$BASE_URL/api/admin/identity-access/$(python3 -c "import urllib.parse,sys; print(urllib.parse.quote(sys.argv[1]))" "$TEST_EMAIL")" || true)"

    if [ "$delete_code" = "200" ] || [ "$delete_code" = "204" ]; then
      PASS=$((PASS + 1))
      echo -e "${GREEN}✓${NC} Identity access cleanup delete ($delete_code)"
      if [ -n "$REPORT_FILE" ]; then
        echo "- ✅ Identity access cleanup delete (HTTP $delete_code)" >> "$REPORT_FILE"
      fi
    else
      WARN=$((WARN + 1))
      echo -e "${YELLOW}!${NC} Identity access cleanup returned $delete_code for $TEST_EMAIL"
      if [ -n "$REPORT_FILE" ]; then
        echo "- ⚠️ Identity access cleanup returned HTTP $delete_code for $TEST_EMAIL" >> "$REPORT_FILE"
      fi
    fi
  else
    WARN=$((WARN + 1))
    echo -e "${YELLOW}!${NC} Skipping write-path validation (set ALLOW_WRITE=true to enable)."
    if [ -n "$REPORT_FILE" ]; then
      echo "- ⚠️ Write-path validation skipped (ALLOW_WRITE != true)" >> "$REPORT_FILE"
    fi
  fi
fi

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo "Checks:  $CHECKS"
echo "Passed:  $PASS"
echo "Warnings:$WARN"

if [ $OVERALL_STATUS -eq 0 ]; then
  echo -e "${GREEN}Result: PASS${NC}"
  if [ -n "$REPORT_FILE" ]; then
    {
      echo ""
      echo "## Summary"
      echo ""
      echo "- Checks: $CHECKS"
      echo "- Passed: $PASS"
      echo "- Warnings: $WARN"
      echo "- Result: PASS"
    } >> "$REPORT_FILE"
    echo "Report: $REPORT_FILE"
  fi
else
  echo -e "${RED}Result: FAIL${NC}"
  if [ -n "$REPORT_FILE" ]; then
    {
      echo ""
      echo "## Summary"
      echo ""
      echo "- Checks: $CHECKS"
      echo "- Passed: $PASS"
      echo "- Warnings: $WARN"
      echo "- Result: FAIL"
    } >> "$REPORT_FILE"
    echo "Report: $REPORT_FILE"
  fi
fi

exit $OVERALL_STATUS
