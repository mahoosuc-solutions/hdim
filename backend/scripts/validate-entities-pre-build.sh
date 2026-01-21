#!/bin/bash
# Pre-build entity-migration validation script
#
# This script validates that all JPA entities match their Liquibase migrations
# BEFORE compilation and deployment. This prevents the build-deploy-fail cycle
# where entity-migration mismatches are discovered at runtime.
#
# Usage: ./validate-entities-pre-build.sh
# Exit codes: 0 = success, 1 = validation failed

set -e

echo "🔍 Running entity-migration validation..."
echo ""

cd "$(dirname "$0")/.."

echo "Step 1/2: Building with entity-migration validation tests..."
./gradlew test --tests "*EntityMigrationValidationTest" \
    --parallel \
    --no-daemon \
    --console=plain \
    2>&1 | tee /tmp/entity-validation.log

if [ ${PIPESTATUS[0]} -eq 0 ]; then
    echo ""
    echo "✅ All entity-migration validations passed"
    echo ""
    echo "Summary:"
    grep -c "entity-migration validation" /tmp/entity-validation.log || true
    exit 0
else
    echo ""
    echo "❌ Entity-migration validation FAILED"
    echo ""
    echo "Failures detected in:"
    grep -E "FAILED|ERROR" /tmp/entity-validation.log || true
    echo ""
    echo "Common fixes:"
    echo "  1. Create new Liquibase migration for new entity fields"
    echo "  2. Update migration if entity column type changed"
    echo "  3. Check entity annotation matches migration definition"
    echo ""
    echo "Documentation: backend/docs/ENTITY_MIGRATION_GUIDE.md"
    echo "Full log: /tmp/entity-validation.log"
    exit 1
fi
