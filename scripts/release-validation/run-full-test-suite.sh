#!/bin/bash
# Task 03: Test Suite Execution and Coverage Validation
# Runs full Gradle test suite and validates test pass rate and coverage metrics
set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get version from argument or environment
VERSION="${1:-${VERSION:-}}"
if [ -z "$VERSION" ]; then
    echo -e "${RED}ERROR: VERSION not specified${NC}"
    echo "Usage: $0 v1.3.0"
    exit 1
fi

echo "=========================================="
echo "Task 03: Test Suite & Coverage Validation"
echo "Version: $VERSION"
echo "=========================================="
echo ""

# Navigate to backend directory
cd "$(dirname "$0")/../../backend" || exit 1

# Create output directory
REPORT_DIR="../docs/releases/${VERSION}/validation"
ARTIFACTS_DIR="../docs/releases/${VERSION}/artifacts/test-results"
mkdir -p "$REPORT_DIR"
mkdir -p "$ARTIFACTS_DIR"
REPORT_FILE="$REPORT_DIR/test-coverage-report.md"

# Initialize report
cat > "$REPORT_FILE" <<EOF
# Test Suite Execution and Coverage Validation Report

**Release Version:** $VERSION
**Validation Date:** $(date '+%Y-%m-%d %H:%M:%S')
**Validator:** Test Suite Validation Script

---

## Overview

This report validates the HDIM platform test suite execution and coverage metrics to ensure production readiness.

**Target Metrics:**
- Test Pass Rate: 100% (0 failures, 0 errors)
- Service Layer Coverage: ≥80%
- Overall Coverage: ≥70%

---

EOF

# Track overall status
OVERALL_STATUS=0

echo "Running full Gradle test suite..."
echo "This may take 10-20 minutes..."
echo ""

# Run tests with JaCoCo coverage
START_TIME=$(date +%s)

if ./gradlew test jacocoTestReport -q > /tmp/gradle-test-output.log 2>&1; then
    echo -e "${GREEN}✓ Test suite completed successfully${NC}"
    BUILD_STATUS="PASSED"
else
    echo -e "${RED}✗ Test suite failed${NC}"
    BUILD_STATUS="FAILED"
    OVERALL_STATUS=1
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
DURATION_MIN=$((DURATION / 60))
DURATION_SEC=$((DURATION % 60))

echo ""
echo "Test execution time: ${DURATION_MIN}m ${DURATION_SEC}s"
echo ""

# Parse test results
echo "## Test Execution Results" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Extract test counts from Gradle output
TOTAL_TESTS=$(grep -o "[0-9]* tests" /tmp/gradle-test-output.log | head -1 | awk '{print $1}' || echo "0")
FAILED_TESTS=$(grep -o "[0-9]* failures" /tmp/gradle-test-output.log | head -1 | awk '{print $1}' || echo "0")
PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))

if [ "$TOTAL_TESTS" -eq 0 ]; then
    # Try alternative parsing method
    TOTAL_TESTS=$(find . -name "TEST-*.xml" 2>/dev/null | xargs grep -h "tests=" 2>/dev/null | grep -o 'tests="[0-9]*"' | cut -d'"' -f2 | awk '{s+=$1} END {print s}' || echo "0")
    FAILED_TESTS=$(find . -name "TEST-*.xml" 2>/dev/null | xargs grep -h "failures=" 2>/dev/null | grep -o 'failures="[0-9]*"' | cut -d'"' -f2 | awk '{s+=$1} END {print s}' || echo "0")
    PASSED_TESTS=$((TOTAL_TESTS - FAILED_TESTS))
fi

# Calculate pass rate
if [ "$TOTAL_TESTS" -gt 0 ]; then
    PASS_RATE=$(awk "BEGIN {printf \"%.2f\", ($PASSED_TESTS / $TOTAL_TESTS) * 100}")
else
    PASS_RATE="0.00"
fi

echo "- **Build Status:** $BUILD_STATUS" >> "$REPORT_FILE"
echo "- **Total Tests:** $TOTAL_TESTS" >> "$REPORT_FILE"
echo "- **Passed:** $PASSED_TESTS" >> "$REPORT_FILE"
echo "- **Failed:** $FAILED_TESTS" >> "$REPORT_FILE"
echo "- **Pass Rate:** ${PASS_RATE}%" >> "$REPORT_FILE"
echo "- **Execution Time:** ${DURATION_MIN}m ${DURATION_SEC}s" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Validate pass rate
if [ "$FAILED_TESTS" -gt 0 ]; then
    echo -e "${RED}✗ Test failures detected: $FAILED_TESTS failed${NC}"
    echo "### ❌ Test Failures Detected" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "$FAILED_TESTS test(s) failed. Review failure details in build logs:" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    grep -A 5 "FAILED" /tmp/gradle-test-output.log | head -50 >> "$REPORT_FILE" 2>/dev/null || echo "See build/reports/tests/test/index.html for details" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    OVERALL_STATUS=1
else
    echo -e "${GREEN}✓ All tests passed (100% pass rate)${NC}"
    echo "### ✅ All Tests Passed" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "100% test pass rate achieved ($TOTAL_TESTS/$TOTAL_TESTS tests passing)." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
fi

# Copy test reports to artifacts
echo "Copying test reports to artifacts directory..."
if [ -d "build/reports/tests/test" ]; then
    cp -r build/reports/tests/test "$ARTIFACTS_DIR/" 2>/dev/null || true
fi

echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Parse JaCoCo coverage reports
echo "## Code Coverage Analysis" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "Analyzing JaCoCo coverage reports..."

# Find JaCoCo XML reports
JACOCO_REPORTS=$(find . -name "jacocoTestReport.xml" -type f 2>/dev/null || true)

if [ -z "$JACOCO_REPORTS" ]; then
    echo -e "${YELLOW}⚠ WARNING: No JaCoCo coverage reports found${NC}"
    echo "- **Coverage Reports:** Not found" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Run \`./gradlew jacocoTestReport\` to generate coverage reports." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    # Aggregate coverage metrics
    TOTAL_INSTRUCTIONS_COVERED=0
    TOTAL_INSTRUCTIONS=0
    TOTAL_BRANCHES_COVERED=0
    TOTAL_BRANCHES=0

    for REPORT in $JACOCO_REPORTS; do
        # Extract covered and total instructions
        INSTRUCTIONS_COVERED=$(grep -o 'type="INSTRUCTION" missed="[0-9]*" covered="[0-9]*"' "$REPORT" | head -1 | grep -o 'covered="[0-9]*"' | cut -d'"' -f2 || echo "0")
        INSTRUCTIONS_MISSED=$(grep -o 'type="INSTRUCTION" missed="[0-9]*" covered="[0-9]*"' "$REPORT" | head -1 | grep -o 'missed="[0-9]*"' | cut -d'"' -f2 || echo "0")

        TOTAL_INSTRUCTIONS_COVERED=$((TOTAL_INSTRUCTIONS_COVERED + INSTRUCTIONS_COVERED))
        TOTAL_INSTRUCTIONS=$((TOTAL_INSTRUCTIONS + INSTRUCTIONS_COVERED + INSTRUCTIONS_MISSED))

        # Extract branch coverage
        BRANCHES_COVERED=$(grep -o 'type="BRANCH" missed="[0-9]*" covered="[0-9]*"' "$REPORT" | head -1 | grep -o 'covered="[0-9]*"' | cut -d'"' -f2 || echo "0")
        BRANCHES_MISSED=$(grep -o 'type="BRANCH" missed="[0-9]*" covered="[0-9]*"' "$REPORT" | head -1 | grep -o 'missed="[0-9]*"' | cut -d'"' -f2 || echo "0")

        TOTAL_BRANCHES_COVERED=$((TOTAL_BRANCHES_COVERED + BRANCHES_COVERED))
        TOTAL_BRANCHES=$((TOTAL_BRANCHES + BRANCHES_COVERED + BRANCHES_MISSED))
    done

    # Calculate percentages
    if [ $TOTAL_INSTRUCTIONS -gt 0 ]; then
        OVERALL_COVERAGE=$(awk "BEGIN {printf \"%.2f\", ($TOTAL_INSTRUCTIONS_COVERED / $TOTAL_INSTRUCTIONS) * 100}")
    else
        OVERALL_COVERAGE="0.00"
    fi

    if [ $TOTAL_BRANCHES -gt 0 ]; then
        BRANCH_COVERAGE=$(awk "BEGIN {printf \"%.2f\", ($TOTAL_BRANCHES_COVERED / $TOTAL_BRANCHES) * 100}")
    else
        BRANCH_COVERAGE="0.00"
    fi

    echo "- **Overall Coverage:** ${OVERALL_COVERAGE}% (Target: ≥70%)" >> "$REPORT_FILE"
    echo "- **Branch Coverage:** ${BRANCH_COVERAGE}%" >> "$REPORT_FILE"
    echo "- **Instructions Covered:** $TOTAL_INSTRUCTIONS_COVERED / $TOTAL_INSTRUCTIONS" >> "$REPORT_FILE"
    echo "- **Branches Covered:** $TOTAL_BRANCHES_COVERED / $TOTAL_BRANCHES" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"

    # Validate coverage thresholds
    COVERAGE_INT=$(printf "%.0f" "$OVERALL_COVERAGE")

    if [ "$COVERAGE_INT" -ge 70 ]; then
        echo -e "${GREEN}✓ Overall coverage ${OVERALL_COVERAGE}% meets target (≥70%)${NC}"
        echo "### ✅ Coverage Target Met" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "Overall coverage of ${OVERALL_COVERAGE}% meets the minimum threshold of 70%." >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    else
        echo -e "${RED}✗ Overall coverage ${OVERALL_COVERAGE}% below target (≥70%)${NC}"
        echo "### ❌ Coverage Below Target" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "Overall coverage of ${OVERALL_COVERAGE}% is below the minimum threshold of 70%." >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        echo "**Remediation:**" >> "$REPORT_FILE"
        echo "- Add unit tests for uncovered service methods" >> "$REPORT_FILE"
        echo "- Focus on service layer (business logic) coverage" >> "$REPORT_FILE"
        echo "- Review JaCoCo HTML reports: build/reports/jacoco/test/html/index.html" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
        OVERALL_STATUS=1
    fi

    # Copy JaCoCo reports to artifacts
    if [ -d "build/reports/jacoco/test/html" ]; then
        cp -r build/reports/jacoco/test/html "$ARTIFACTS_DIR/jacoco-html" 2>/dev/null || true
    fi
fi

echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo "Build Status: $BUILD_STATUS"
echo "Test Pass Rate: ${PASS_RATE}%"
echo "Overall Coverage: ${OVERALL_COVERAGE}%"
echo ""

echo "## Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| Metric | Value | Target | Status |" >> "$REPORT_FILE"
echo "|--------|-------|--------|--------|" >> "$REPORT_FILE"
echo "| Build Status | $BUILD_STATUS | SUCCESS | $([ "$BUILD_STATUS" = "PASSED" ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| Test Pass Rate | ${PASS_RATE}% | 100% | $([ "$FAILED_TESTS" -eq 0 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "| Overall Coverage | ${OVERALL_COVERAGE}% | ≥70% | $([ "$COVERAGE_INT" -ge 70 ] && echo "✅" || echo "❌") |" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

echo "## Artifact Locations" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "- Test Results: \`build/reports/tests/test/index.html\`" >> "$REPORT_FILE"
echo "- JaCoCo Coverage: \`build/reports/jacoco/test/html/index.html\`" >> "$REPORT_FILE"
echo "- Artifacts Copy: \`docs/releases/${VERSION}/artifacts/test-results/\`" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

if [ $OVERALL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✓ VALIDATION PASSED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ✅ Overall Status: PASSED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "All test suite and coverage validation checks passed. Test quality is production-ready." >> "$REPORT_FILE"
else
    echo -e "${RED}✗ VALIDATION FAILED${NC}"
    echo "" >> "$REPORT_FILE"
    echo "### ❌ Overall Status: FAILED" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "Test suite or coverage issues detected. Review failures above and remediate before release." >> "$REPORT_FILE"
fi

echo ""
echo "Report generated: $REPORT_FILE"
echo "Artifacts copied to: $ARTIFACTS_DIR"
echo ""

# Cleanup
rm -f /tmp/gradle-test-output.log

exit $OVERALL_STATUS
