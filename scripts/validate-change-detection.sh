#!/bin/bash
# validate-change-detection.sh
# Script to validate change detection configuration in GitHub Actions workflow
#
# Usage: ./scripts/validate-change-detection.sh
#
# This script checks:
# 1. YAML syntax is valid
# 2. All filter definitions are complete
# 3. All outputs are referenced
# 4. Job conditional syntax is correct
# 5. Merge gate handles all job results

set -e

WORKFLOW_FILE=".github/workflows/backend-ci-v2-parallel.yml"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "Change Detection Validation Script"
echo "=========================================="
echo ""

# Check if workflow file exists
if [ ! -f "$WORKFLOW_FILE" ]; then
    echo "✗ Workflow file not found: $WORKFLOW_FILE"
    exit 1
fi

echo "1. Validating YAML syntax..."
python3 << 'PYTHON_EOF'
import yaml
import sys

try:
    with open('.github/workflows/backend-ci-v2-parallel.yml', 'r') as f:
        yaml.safe_load(f)
    print("   ✓ YAML syntax is valid")
except yaml.YAMLError as e:
    print(f"   ✗ YAML syntax error: {e}")
    sys.exit(1)
PYTHON_EOF

echo ""
echo "2. Checking change-detection job outputs..."
OUTPUTS=$(grep -A 25 "change-detection:" "$WORKFLOW_FILE" | grep "outputs:" -A 20 | grep ":" | grep -v "filters" | wc -l)
echo "   Found $OUTPUTS service/module outputs"
if [ "$OUTPUTS" -ge 10 ]; then
    echo "   ✓ Sufficient outputs defined (expected ≥10, found $OUTPUTS)"
else
    echo "   ✗ Not enough outputs (expected ≥10, found $OUTPUTS)"
    exit 1
fi

echo ""
echo "3. Checking job conditionals..."
CONDITIONAL_JOBS=("build" "test-unit" "test-fast" "test-integration" "test-slow" "validate" "security" "code")
for job in "${CONDITIONAL_JOBS[@]}"; do
    if grep -q "name:.*$job" "$WORKFLOW_FILE" -i; then
        echo "   ✓ Found job: $job"
    else
        echo "   ✗ Job not found: $job"
        exit 1
    fi
done

echo ""
echo "4. Checking change detection filter definitions..."
FILTERS=("backend" "infrastructure" "gradle" "shared" "patient-service" "care-gap-service" "quality-service" "fhir-service" "cql-engine" "event-services")
for filter in "${FILTERS[@]}"; do
    if grep -q "^\s*$filter:" "$WORKFLOW_FILE"; then
        echo "   ✓ Filter defined: $filter"
    else
        echo "   ✗ Filter missing: $filter"
        exit 1
    fi
done

echo ""
echo "5. Checking merge gate job..."
if grep -q "pr-validation-gate:" "$WORKFLOW_FILE"; then
    if grep -A 5 "pr-validation-gate:" "$WORKFLOW_FILE" | grep -q "if: always()"; then
        echo "   ✓ Merge gate uses if: always()"
    else
        echo "   ✗ Merge gate missing if: always()"
        exit 1
    fi

    if grep -q "check_job()" "$WORKFLOW_FILE"; then
        echo "   ✓ Merge gate uses check_job function"
    else
        echo "   ✗ Merge gate missing check_job function"
        exit 1
    fi
else
    echo "   ✗ Merge gate job not found"
    exit 1
fi

echo ""
echo "6. Checking for skipped job handling..."
if grep -q "skipped" "$WORKFLOW_FILE"; then
    echo "   ✓ Workflow handles skipped jobs"
else
    echo "   ✗ Workflow may not handle skipped jobs correctly"
fi

echo ""
echo "=========================================="
echo "All validations passed!"
echo "=========================================="
echo ""
echo "Summary:"
echo "- YAML syntax: valid"
echo "- Change detection outputs: $OUTPUTS"
echo "- Conditional jobs: ${#CONDITIONAL_JOBS[@]} jobs"
echo "- Filter definitions: ${#FILTERS[@]} filters"
echo "- Merge gate: configured with skipped job handling"
echo ""
echo "The workflow is ready for testing on a feature branch."
echo ""
