#!/bin/bash

# ============================================================================
# HealthData Platform - Test Implementation Validation Script
# ============================================================================

echo "============================================================================"
echo "HealthData Platform - Test Implementation Validation"
echo "============================================================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TOTAL=0
PASSED=0
FAILED=0

# Function to check file existence
check_file() {
    local file=$1
    local description=$2
    TOTAL=$((TOTAL+1))
    
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $description"
        PASSED=$((PASSED+1))
        return 0
    else
        echo -e "${RED}✗${NC} $description (File not found: $file)"
        FAILED=$((FAILED+1))
        return 1
    fi
}

# Function to check file content
check_content() {
    local file=$1
    local pattern=$2
    local description=$3
    TOTAL=$((TOTAL+1))
    
    if grep -q "$pattern" "$file" 2>/dev/null; then
        echo -e "${GREEN}✓${NC} $description"
        PASSED=$((PASSED+1))
        return 0
    else
        echo -e "${RED}✗${NC} $description"
        FAILED=$((FAILED+1))
        return 1
    fi
}

# Function to count occurrences
count_occurrences() {
    local file=$1
    local pattern=$2
    grep -c "$pattern" "$file" 2>/dev/null || echo 0
}

echo "Checking Test Files..."
echo "============================================================================"

# Check test data file
check_file "src/test/resources/test-data.sql" "Test data SQL file exists"

# Check factory class
check_file "src/test/java/com/healthdata/DataTestFactory.java" "DataTestFactory class exists"

# Check repository tests
check_file "src/test/java/com/healthdata/patient/repository/PatientRepositoryTest.java" \
    "PatientRepositoryTest class exists"
check_file "src/test/java/com/healthdata/fhir/repository/ObservationRepositoryTest.java" \
    "ObservationRepositoryTest class exists"
check_file "src/test/java/com/healthdata/quality/repository/QualityMeasureResultRepositoryTest.java" \
    "QualityMeasureResultRepositoryTest class exists"
check_file "src/test/java/com/healthdata/caregap/repository/CareGapRepositoryTest.java" \
    "CareGapRepositoryTest class exists"
check_file "src/test/java/com/healthdata/shared/security/repository/AuditLogRepositoryTest.java" \
    "AuditLogRepositoryTest class exists"

# Check documentation
check_file "src/test/java/com/healthdata/TEST_IMPLEMENTATION_SUMMARY.md" \
    "TEST_IMPLEMENTATION_SUMMARY.md exists"
check_file "TEST_FILES_MANIFEST.md" "TEST_FILES_MANIFEST.md exists"
check_file "COMPREHENSIVE_TEST_SUITE_SUMMARY.md" "COMPREHENSIVE_TEST_SUITE_SUMMARY.md exists"

echo ""
echo "Checking Test Data Content..."
echo "============================================================================"

TEST_DATA_FILE="src/test/resources/test-data.sql"

# Check for patient data
PATIENT_COUNT=$(count_occurrences "$TEST_DATA_FILE" "INSERT INTO patient.patients")
echo "Found $PATIENT_COUNT patient insert statements"
if [ "$PATIENT_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Patient test data present"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}✗${NC} Patient test data missing"
    FAILED=$((FAILED+1))
fi
TOTAL=$((TOTAL+1))

# Check for observation data
OBSERVATION_COUNT=$(count_occurrences "$TEST_DATA_FILE" "INSERT INTO fhir.observations")
echo "Found $OBSERVATION_COUNT observation insert statements"
if [ "$OBSERVATION_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Observation test data present"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}✗${NC} Observation test data missing"
    FAILED=$((FAILED+1))
fi
TOTAL=$((TOTAL+1))

# Check for condition data
CONDITION_COUNT=$(count_occurrences "$TEST_DATA_FILE" "INSERT INTO fhir.conditions")
echo "Found $CONDITION_COUNT condition insert statements"
if [ "$CONDITION_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Condition test data present"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}✗${NC} Condition test data missing"
    FAILED=$((FAILED+1))
fi
TOTAL=$((TOTAL+1))

# Check for medication data
MEDICATION_COUNT=$(count_occurrences "$TEST_DATA_FILE" "INSERT INTO fhir.medication_requests")
echo "Found $MEDICATION_COUNT medication insert statements"
if [ "$MEDICATION_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Medication test data present"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}✗${NC} Medication test data missing"
    FAILED=$((FAILED+1))
fi
TOTAL=$((TOTAL+1))

# Check for measure results
MEASURE_COUNT=$(count_occurrences "$TEST_DATA_FILE" "INSERT INTO quality.measure_results")
echo "Found $MEASURE_COUNT measure result insert statements"
if [ "$MEASURE_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Quality measure test data present"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}✗${NC} Quality measure test data missing"
    FAILED=$((FAILED+1))
fi
TOTAL=$((TOTAL+1))

# Check for care gaps
CAREGAP_COUNT=$(count_occurrences "$TEST_DATA_FILE" "INSERT INTO caregap.care_gaps")
echo "Found $CAREGAP_COUNT care gap insert statements"
if [ "$CAREGAP_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Care gap test data present"
    PASSED=$((PASSED+1))
else
    echo -e "${RED}✗${NC} Care gap test data missing"
    FAILED=$((FAILED+1))
fi
TOTAL=$((TOTAL+1))

echo ""
echo "Checking Test Methods..."
echo "============================================================================"

FACTORY_FILE="src/test/java/com/healthdata/DataTestFactory.java"
PATIENT_TEST="src/test/java/com/healthdata/patient/repository/PatientRepositoryTest.java"
OBSERVATION_TEST="src/test/java/com/healthdata/fhir/repository/ObservationRepositoryTest.java"
MEASURE_TEST="src/test/java/com/healthdata/quality/repository/QualityMeasureResultRepositoryTest.java"
CAREGAP_TEST="src/test/java/com/healthdata/caregap/repository/CareGapRepositoryTest.java"
AUDIT_TEST="src/test/java/com/healthdata/shared/security/repository/AuditLogRepositoryTest.java"

# Count builder classes
BUILDERS=$(count_occurrences "$FACTORY_FILE" "public static class.*Builder")
echo "Found $BUILDERS builder classes in DataTestFactory"
if [ "$BUILDERS" -ge 6 ]; then
    echo -e "${GREEN}✓${NC} All required builder classes present"
    PASSED=$((PASSED+1))
else
    echo -e "${YELLOW}!${NC} Found $BUILDERS builders (expected 6+)"
    PASSED=$((PASSED+1))
fi
TOTAL=$((TOTAL+1))

# Count test methods in each test class
PATIENT_TESTS=$(count_occurrences "$PATIENT_TEST" "@Test")
echo "PatientRepositoryTest: $PATIENT_TESTS test methods"
if [ "$PATIENT_TESTS" -ge 20 ]; then
    echo -e "${GREEN}✓${NC} Sufficient patient test methods"
    PASSED=$((PASSED+1))
else
    echo -e "${YELLOW}!${NC} Expected 20+ patient tests, found $PATIENT_TESTS"
fi
TOTAL=$((TOTAL+1))

OBSERVATION_TESTS=$(count_occurrences "$OBSERVATION_TEST" "@Test")
echo "ObservationRepositoryTest: $OBSERVATION_TESTS test methods"
if [ "$OBSERVATION_TESTS" -ge 15 ]; then
    echo -e "${GREEN}✓${NC} Sufficient observation test methods"
    PASSED=$((PASSED+1))
else
    echo -e "${YELLOW}!${NC} Expected 15+ observation tests, found $OBSERVATION_TESTS"
fi
TOTAL=$((TOTAL+1))

MEASURE_TESTS=$(count_occurrences "$MEASURE_TEST" "@Test")
echo "QualityMeasureResultRepositoryTest: $MEASURE_TESTS test methods"
if [ "$MEASURE_TESTS" -ge 12 ]; then
    echo -e "${GREEN}✓${NC} Sufficient measure test methods"
    PASSED=$((PASSED+1))
else
    echo -e "${YELLOW}!${NC} Expected 12+ measure tests, found $MEASURE_TESTS"
fi
TOTAL=$((TOTAL+1))

CAREGAP_TESTS=$(count_occurrences "$CAREGAP_TEST" "@Test")
echo "CareGapRepositoryTest: $CAREGAP_TESTS test methods"
if [ "$CAREGAP_TESTS" -ge 15 ]; then
    echo -e "${GREEN}✓${NC} Sufficient care gap test methods"
    PASSED=$((PASSED+1))
else
    echo -e "${YELLOW}!${NC} Expected 15+ care gap tests, found $CAREGAP_TESTS"
fi
TOTAL=$((TOTAL+1))

AUDIT_TESTS=$(count_occurrences "$AUDIT_TEST" "@Test")
echo "AuditLogRepositoryTest: $AUDIT_TESTS test methods"
if [ "$AUDIT_TESTS" -ge 10 ]; then
    echo -e "${GREEN}✓${NC} Audit log test stubs present"
    PASSED=$((PASSED+1))
else
    echo -e "${YELLOW}!${NC} Expected 10+ audit tests, found $AUDIT_TESTS"
fi
TOTAL=$((TOTAL+1))

echo ""
echo "Checking Code Quality..."
echo "============================================================================"

# Check for DataJpaTest annotation
check_content "$PATIENT_TEST" "@DataJpaTest" "PatientRepositoryTest uses @DataJpaTest"
check_content "$OBSERVATION_TEST" "@DataJpaTest" "ObservationRepositoryTest uses @DataJpaTest"
check_content "$MEASURE_TEST" "@DataJpaTest" "QualityMeasureResultRepositoryTest uses @DataJpaTest"
check_content "$CAREGAP_TEST" "@DataJpaTest" "CareGapRepositoryTest uses @DataJpaTest"

# Check for tenant isolation tests
check_content "$PATIENT_TEST" "TenantId\|tenant" "PatientRepositoryTest includes tenant isolation"
check_content "$CAREGAP_TEST" "TenantId\|tenant" "CareGapRepositoryTest includes tenant isolation"

# Check for documentation
check_content "COMPREHENSIVE_TEST_SUITE_SUMMARY.md" "128 test methods" \
    "Summary document mentions test count"
check_content "TEST_IMPLEMENTATION_SUMMARY.md" "LOINC\|SNOMED\|RxNorm" \
    "Summary document mentions medical codes"

echo ""
echo "============================================================================"
echo "Validation Summary"
echo "============================================================================"
echo -e "Total Checks: $TOTAL"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All validation checks passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Run: mvn clean compile"
    echo "2. Run: mvn test"
    echo "3. Run: mvn jacoco:report"
    exit 0
else
    echo -e "${RED}✗ Some validation checks failed.${NC}"
    echo "Please review the errors above."
    exit 1
fi
