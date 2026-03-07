#!/usr/bin/env bash
set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

VERSION="${1:-${VERSION:-v0.0.0-local}}"
TENANTS="${TENANTS:-summit-care-2026,valley-health-2026}"
EXPECTED_PATIENTS_BY_TENANT="${EXPECTED_PATIENTS_BY_TENANT:-summit-care-2026=1200,valley-health-2026=1200}"
EXPECTED_CARE_GAPS_RANGE_BY_TENANT="${EXPECTED_CARE_GAPS_RANGE_BY_TENANT:-}"

REPORT_DIR="docs/releases/${VERSION}/validation"
mkdir -p "${REPORT_DIR}"
REPORT_FILE="${REPORT_DIR}/demo-seeding-count-validation-report.md"

run_ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

echo -e "${BLUE}Validating demo seeding counts...${NC}"
echo "  TENANTS=${TENANTS}"
echo "  EXPECTED_PATIENTS_BY_TENANT=${EXPECTED_PATIENTS_BY_TENANT}"
if [[ -n "${EXPECTED_CARE_GAPS_RANGE_BY_TENANT}" ]]; then
  echo "  EXPECTED_CARE_GAPS_RANGE_BY_TENANT=${EXPECTED_CARE_GAPS_RANGE_BY_TENANT}"
fi

set +e
verify_output="$(
  TENANTS="${TENANTS}" \
  EXPECTED_PATIENTS_BY_TENANT="${EXPECTED_PATIENTS_BY_TENANT}" \
  EXPECTED_CARE_GAPS_RANGE_BY_TENANT="${EXPECTED_CARE_GAPS_RANGE_BY_TENANT}" \
  ./scripts/verify-seeding-counts.sh 2>&1
)"
verify_exit=$?
set -e

status="FAILED"
if [[ ${verify_exit} -eq 0 ]]; then
  status="PASSED"
fi

cat > "${REPORT_FILE}" <<REPORT
# Demo Seeding Count Validation Report

- **Timestamp (UTC):** ${run_ts}
- **Version:** ${VERSION}
- **Status:** ${status}
- **Tenants:** \`${TENANTS}\`
- **Expected Patients:** \`${EXPECTED_PATIENTS_BY_TENANT}\`
- **Expected Care Gap Ranges:** \`${EXPECTED_CARE_GAPS_RANGE_BY_TENANT:-not-set}\`

## Command Output

\`\`\`text
${verify_output}
\`\`\`
REPORT

if [[ ${verify_exit} -eq 0 ]]; then
  echo -e "${GREEN}✓ Demo seeding count validation passed${NC}"
  echo "Report: ${REPORT_FILE}"
  exit 0
fi

echo -e "${RED}✗ Demo seeding count validation failed${NC}"
echo "Report: ${REPORT_FILE}"
exit ${verify_exit}
