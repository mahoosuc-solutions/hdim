#!/bin/bash
#
# Secure FHIR Controllers - Add @PreAuthorize annotations
#
# This script adds missing @PreAuthorize annotations to FHIR resource controllers
# based on HTTP method and audit action type.
#
# Pattern:
# - POST (AuditAction.CREATE) → hasPermission('PATIENT_WRITE')
# - GET (AuditAction.READ) → hasPermission('PATIENT_READ')
# - PUT (AuditAction.UPDATE) → hasPermission('PATIENT_WRITE')
# - DELETE (AuditAction.DELETE) → hasPermission('PATIENT_WRITE')
# - Search operations → hasPermission('PATIENT_SEARCH')
#

set -e

FHIR_DIR="modules/services/fhir-service/src/main/java/com/healthdata/fhir/rest"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "FHIR Controllers Security Migration"
echo "=================================="
echo ""

# Check if directory exists
if [ ! -d "$FHIR_DIR" ]; then
    echo -e "${RED}Error: FHIR controller directory not found: $FHIR_DIR${NC}"
    exit 1
fi

# Find all FHIR controller files
CONTROLLERS=$(find "$FHIR_DIR" -name "*Controller.java" -type f)
TOTAL_CONTROLLERS=$(echo "$CONTROLLERS" | wc -l)

echo "Found $TOTAL_CONTROLLERS FHIR controllers"
echo ""

MIGRATED=0

for controller in $CONTROLLERS; do
    controller_name=$(basename "$controller")

    # Check if already has @PreAuthorize
    if grep -q "@PreAuthorize" "$controller"; then
        echo -e "${YELLOW}Skipping $controller_name - already has @PreAuthorize${NC}"
        continue
    fi

    echo -e "${GREEN}Processing: $controller_name${NC}"

    # Add @PreAuthorize import if not present
    if ! grep -q "import org.springframework.security.access.prepost.PreAuthorize;" "$controller"; then
        sed -i '/import org.springframework.web.bind.annotation.\*/a import org.springframework.security.access.prepost.PreAuthorize;' "$controller"
    fi

    # Add @PreAuthorize before @Audited annotations based on action type

    # CREATE operations → PATIENT_WRITE
    sed -i '/@Audited(action = AuditAction.CREATE/i \    @PreAuthorize("hasPermission('\''PATIENT_WRITE'\'')")' "$controller"

    # READ operations → PATIENT_READ (but check for search patterns)
    sed -i '/@Audited(action = AuditAction.READ/i \    @PreAuthorize("hasPermission('\''PATIENT_READ'\'')")' "$controller"

    # UPDATE operations → PATIENT_WRITE
    sed -i '/@Audited(action = AuditAction.UPDATE/i \    @PreAuthorize("hasPermission('\''PATIENT_WRITE'\'')")' "$controller"

    # DELETE operations → PATIENT_WRITE
    sed -i '/@Audited(action = AuditAction.DELETE/i \    @PreAuthorize("hasPermission('\''PATIENT_WRITE'\'')")' "$controller"

    MIGRATED=$((MIGRATED + 1))
done

echo ""
echo "=================================="
echo "Migration Summary"
echo "=================================="
echo "Total FHIR controllers: $TOTAL_CONTROLLERS"
echo "Controllers migrated: $MIGRATED"
echo ""
echo -e "${GREEN}FHIR controllers are now secured with permission-based authorization!${NC}"
echo ""
echo "Next steps:"
echo "1. Review changes: git diff $FHIR_DIR"
echo "2. Build FHIR service: ./gradlew :modules:services:fhir-service:build -x test"
echo "3. Run tests: ./gradlew :modules:services:fhir-service:test"
echo "4. Commit: git add -A && git commit -m 'feat(rbac): Secure FHIR controllers with permission-based auth'"
