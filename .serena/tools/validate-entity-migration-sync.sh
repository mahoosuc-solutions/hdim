#!/bin/bash
# Entity-Migration Synchronization Validator
# Runs validation tests for all services to ensure JPA entities match Liquibase migrations

set -e

echo "🔍 HDIM Entity-Migration Synchronization Validator"
echo "=================================================="
echo ""

SERVICES=(
    "authentication"
    "patient-service"
    "quality-measure-service"
    "care-gap-service"
    "fhir-service"
)

PASSED=0
FAILED=0
SKIPPED=0

for service in "${SERVICES[@]}"; do
    echo "📦 Validating: $service"
    echo "---"

    # Check if service exists
    if [ "$service" = "authentication" ]; then
        SERVICE_PATH="backend/modules/shared/infrastructure/authentication"
    else
        SERVICE_PATH="backend/modules/services/$service"
    fi

    if [ ! -d "$SERVICE_PATH" ]; then
        echo "⏭️  Skipped (not found)"
        SKIPPED=$((SKIPPED + 1))
        echo ""
        continue
    fi

    # Check for validation test
    VALIDATION_TEST=$(find "$SERVICE_PATH" -name "*EntityMigrationValidationTest.java" 2>/dev/null || true)

    if [ -z "$VALIDATION_TEST" ]; then
        echo "⚠️  No validation test found"
        SKIPPED=$((SKIPPED + 1))
        echo ""
        continue
    fi

    # Run validation test
    if [ "$service" = "authentication" ]; then
        GRADLE_PATH=":modules:shared:infrastructure:authentication"
    else
        GRADLE_PATH=":modules:services:$service"
    fi

    echo "   Running: ./gradlew $GRADLE_PATH:test --tests \"*EntityMigrationValidationTest\""

    if (cd backend && ./gradlew $GRADLE_PATH:test --tests "*EntityMigrationValidationTest" --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"); then
        echo "✅ PASSED - Entities match migrations"
        PASSED=$((PASSED + 1))
    else
        echo "❌ FAILED - Schema mismatch detected"
        echo "   Run for details: cd backend && ./gradlew $GRADLE_PATH:test --tests \"*EntityMigrationValidationTest\""
        FAILED=$((FAILED + 1))
    fi

    echo ""
done

# Summary
echo "=================================================="
echo "📊 Validation Summary:"
echo "   ✅ Passed:  $PASSED"
echo "   ❌ Failed:  $FAILED"
echo "   ⏭️  Skipped: $SKIPPED"
echo ""

if [ "$FAILED" -gt 0 ]; then
    echo "❌ Entity-migration synchronization issues detected!"
    echo ""
    echo "🔧 How to fix:"
    echo "   1. Check entity @Column annotations match migration types"
    echo "   2. Ensure ddl-auto: validate in application.yml"
    echo "   3. Create new migration if schema changes needed"
    echo "   4. See: .serena/memories/entity-migration-sync.md"
    exit 1
elif [ "$PASSED" -eq 0 ]; then
    echo "⚠️  No services validated. Ensure validation tests exist."
    exit 0
else
    echo "✅ All entity-migration validations passed!"
    exit 0
fi
