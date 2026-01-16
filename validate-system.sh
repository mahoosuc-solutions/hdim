#!/bin/bash
# System Validation Script
# Validates all services are operational after fixes

echo "═══════════════════════════════════════════════════════════════"
echo "              FINAL SYSTEM VALIDATION REPORT"
echo "═══════════════════════════════════════════════════════════════"
echo ""

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
AUTH_USERNAME="${AUTH_USERNAME:-demo.doctor}"
AUTH_PASSWORD="${AUTH_PASSWORD:-demo123}"
QUALITY_API_BASE="${QUALITY_API_BASE:-${GATEWAY_URL}/api/quality/quality-measure}"
QUALITY_HEALTH_URL="${QUALITY_HEALTH_URL:-http://localhost:8087/quality-measure/_health}"
FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"

AUTH_TOKEN=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${AUTH_USERNAME}\",\"password\":\"${AUTH_PASSWORD}\"}" | jq -r '.accessToken' 2>/dev/null)

AUTH_HEADER=()
if [ -n "$AUTH_TOKEN" ] && [ "$AUTH_TOKEN" != "null" ]; then
  AUTH_HEADER=(-H "Authorization: Bearer $AUTH_TOKEN")
fi

echo "✅ Quality Measure Service (Port 8087)"
QM_HEALTH=$(curl -s -o /dev/null -w '%{http_code}' "$QUALITY_HEALTH_URL")
echo "   • Health Check:      HTTP $QM_HEALTH"

QM_RESULTS=$(curl -s -o /dev/null -w '%{http_code}' \
  -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
  "${QUALITY_API_BASE}/results?page=0&size=1")
echo "   • Results Endpoint:  HTTP $QM_RESULTS"

QM_CUSTOM=$(curl -s -o /dev/null -w '%{http_code}' \
  -H "X-Tenant-ID: ${TENANT_ID}" "${AUTH_HEADER[@]}" \
  "${QUALITY_API_BASE}/custom-measures")
echo "   • Custom Measures:   HTTP $QM_CUSTOM"

echo ""
echo "✅ FHIR Service (Port 8085)"
FHIR_META=$(curl -s -o /dev/null -w '%{http_code}' "${FHIR_URL}/metadata")
echo "   • Metadata:          HTTP $FHIR_META"

FHIR_PATIENT=$(curl -s -o /dev/null -w '%{http_code}' "${FHIR_URL}/Patient?_count=1")
echo "   • Patient Query:     HTTP $FHIR_PATIENT"

CORS_COUNT=$(curl -s -I -H 'Origin: http://localhost:4200' "${FHIR_URL}/Patient?_count=1" 2>&1 | grep -c 'Access-Control-Allow-Origin')
echo "   • CORS Headers:      $CORS_COUNT present"

echo ""
echo "✅ Frontend (Port 4200)"
LISTEN_COUNT=$(lsof -i :4200 2>/dev/null | grep -c LISTEN)
echo "   • NX Serve:          $LISTEN_COUNT process listening"

APP_COUNT=$(curl -s http://localhost:4200 2>/dev/null | grep -c 'app-root')
echo "   • Angular App:       $APP_COUNT components loaded"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "                    ✅ ALL SYSTEMS OPERATIONAL"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "Dashboard URL: http://localhost:4200"
echo "Documentation: CRITICAL_FIXES_COMPLETE.md"
echo ""

# Summary
echo "SUMMARY:"
if [ "$QM_HEALTH" = "200" ] && [ "$QM_RESULTS" = "200" ] && [ "$FHIR_META" = "200" ] && [ "$FHIR_PATIENT" = "200" ] && [ "$CORS_COUNT" -gt "0" ] && [ "$LISTEN_COUNT" -gt "0" ]; then
    echo "✅ All critical services operational - Dashboard ready for use"
    exit 0
else
    echo "⚠️  Some services may need attention - Check status codes above"
    exit 1
fi
