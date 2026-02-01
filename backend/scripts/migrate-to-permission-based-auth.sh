#!/bin/bash
#
# Migrate @PreAuthorize annotations from role-based to permission-based authorization
#
# This script performs bulk replacement of common hasAnyRole() patterns with hasPermission().
# It uses the RBAC permission model defined in Phase 1 (13 roles, 31 permissions).
#
# Usage: ./migrate-to-permission-based-auth.sh
#
# Dry run: DRY_RUN=1 ./migrate-to-permission-based-auth.sh
#

set -e

DRY_RUN=${DRY_RUN:-0}
CONTROLLER_DIR="modules"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "==================================="
echo "RBAC Phase 3: Permission Migration"
echo "==================================="
echo ""

if [ "$DRY_RUN" = "1" ]; then
    echo -e "${YELLOW}DRY RUN MODE - No files will be modified${NC}"
    echo ""
fi

# Migration mappings based on Permission enum and RolePermissions
# Format: "role_pattern|permission_name|description"

declare -a MIGRATIONS=(
    # Patient Data Permissions (PHI access)
    "hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')|PATIENT_READ|View patient demographics and clinical data"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Patient write|PATIENT_WRITE|Create and update patient records"
    "hasAnyRole('SUPER_ADMIN') # Patient delete|PATIENT_DELETE|Delete patient records"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Patient search|PATIENT_SEARCH|Search patient data across system"
    "hasAnyRole('ANALYST', 'ADMIN', 'SUPER_ADMIN') # Export|PATIENT_EXPORT|Export patient data for reporting"

    # Care Gap Permissions
    "hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN') # Care gap write|CARE_GAP_WRITE|Create and update care gap records"
    "hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN') # Close care gap|CARE_GAP_CLOSE|Close and resolve care gaps"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Assign care gap|CARE_GAP_ASSIGN|Assign care gaps to coordinators"

    # Quality Measure Permissions
    "hasAnyRole('VIEWER', 'ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')|MEASURE_READ|View quality measure definitions"
    "hasAnyRole('MEASURE_DEVELOPER', 'ADMIN', 'SUPER_ADMIN')|MEASURE_WRITE|Create and update measure definitions"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Measure delete|MEASURE_DELETE|Delete quality measure definitions"
    "hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')|MEASURE_EXECUTE|Run quality measure evaluations"
    "hasAnyRole('MEASURE_DEVELOPER', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')|MEASURE_EXECUTE|Run quality measure evaluations"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Publish measure|MEASURE_PUBLISH|Publish measures to production"

    # User Management Permissions
    "hasAnyRole('ADMIN', 'SUPER_ADMIN')|USER_READ|View user accounts and profiles"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # User write|USER_WRITE|Create and update user accounts"
    "hasAnyRole('SUPER_ADMIN') # User delete|USER_DELETE|Delete user accounts"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # User roles|USER_MANAGE_ROLES|Assign roles and permissions"

    # Audit & Compliance Permissions
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Audit|AUDIT_READ|View audit logs and access history"
    "hasAnyRole('SUPER_ADMIN') # Audit export|AUDIT_EXPORT|Export audit logs for compliance"
    "hasAnyRole('SUPER_ADMIN') # Audit review|AUDIT_REVIEW|Review and approve compliance reports"

    # Configuration Permissions
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Config read|CONFIG_READ|View system settings"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Config write|CONFIG_WRITE|Update system settings"
    "hasAnyRole('SUPER_ADMIN') # Tenant manage|TENANT_MANAGE|Manage tenant configuration"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # Integration|INTEGRATION_MANAGE|Configure integrations and APIs"

    # API & Integration Permissions
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # API write|API_WRITE|Use APIs for data modification"
    "hasAnyRole('ADMIN', 'SUPER_ADMIN') # API keys|API_MANAGE_KEYS|Generate and manage API keys"

    # Reporting Permissions
    "hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')|REPORT_READ|View quality reports and dashboards"
    "hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')|REPORT_READ|View quality reports and dashboards"
    "hasAnyRole('ADMIN', 'EVALUATOR')|REPORT_CREATE|Create and schedule custom reports"
    "hasAnyRole('ADMIN', 'ANALYST')|REPORT_EXPORT|Export reports for distribution"

    # Simple role patterns (common but generic)
    "hasAnyRole('ADMIN', 'EVALUATOR')|MEASURE_EXECUTE|Run quality measure evaluations"
    "hasRole('ADMIN')|CONFIG_WRITE|Update system settings"
)

# Statistics
TOTAL_FILES=0
MODIFIED_FILES=0
TOTAL_REPLACEMENTS=0

echo "Scanning for controller files..."
CONTROLLER_FILES=$(find "$CONTROLLER_DIR" -name "*Controller.java" -type f)
TOTAL_FILES=$(echo "$CONTROLLER_FILES" | wc -l)

echo "Found $TOTAL_FILES controller files"
echo ""

# Perform migrations
for mapping in "${MIGRATIONS[@]}"; do
    IFS='|' read -r role_pattern permission description <<< "$mapping"

    # Skip comments
    if [[ "$role_pattern" =~ ^# ]]; then
        continue
    fi

    # Escape special regex characters
    escaped_pattern=$(echo "$role_pattern" | sed 's/[()]/\\&/g')

    # Find files with this pattern
    matching_files=$(grep -l "@PreAuthorize(\"$role_pattern\")" $CONTROLLER_FILES 2>/dev/null || true)

    if [ -n "$matching_files" ]; then
        count=$(echo "$matching_files" | wc -l)
        echo -e "${GREEN}Migrating:${NC} $role_pattern"
        echo -e "  ${YELLOW}→${NC} hasPermission('$permission')"
        echo -e "  Description: $description"
        echo -e "  Files affected: $count"

        if [ "$DRY_RUN" = "0" ]; then
            for file in $matching_files; do
                sed -i "s/@PreAuthorize(\"$escaped_pattern\")/@PreAuthorize(\"hasPermission('$permission')\")/" "$file"
                MODIFIED_FILES=$((MODIFIED_FILES + 1))
                TOTAL_REPLACEMENTS=$((TOTAL_REPLACEMENTS + 1))
            done
        fi
        echo ""
    fi
done

echo "==================================="
echo "Migration Summary"
echo "==================================="
echo "Total controller files: $TOTAL_FILES"
echo "Files modified: $MODIFIED_FILES"
echo "Total replacements: $TOTAL_REPLACEMENTS"

if [ "$DRY_RUN" = "1" ]; then
    echo -e "${YELLOW}DRY RUN - No changes were made${NC}"
    echo ""
    echo "To apply changes, run: ./migrate-to-permission-based-auth.sh"
else
    echo -e "${GREEN}Migration complete!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Build project: ./gradlew build"
    echo "2. Run tests: ./gradlew test"
    echo "3. Review changes: git diff"
    echo "4. Commit: git add -A && git commit -m 'feat(rbac): Migrate controllers to permission-based authorization'"
fi
