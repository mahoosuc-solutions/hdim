#!/bin/bash
# Validates all services have correct database configuration

echo "🔍 Validating database configuration across all services..."
echo ""

ERRORS=0
WARNINGS=0

for service_dir in modules/services/*/; do
  service_name=$(basename "$service_dir")

  echo "📦 Checking $service_name..."

  # Check 1: ddl-auto must be validate
  ddl_auto=$(grep -A2 "jpa:" "$service_dir/src/main/resources/application.yml" 2>/dev/null | grep "ddl-auto:" | awk '{print $2}')

  if [ "$ddl_auto" != "validate" ]; then
    echo "  ❌ ddl-auto is '$ddl_auto' (must be 'validate')"
    ERRORS=$((ERRORS + 1))
  else
    echo "  ✅ ddl-auto: validate"
  fi

  # Check 2: Liquibase OR Flyway must be configured
  has_liquibase=$(grep -A1 "liquibase:" "$service_dir/src/main/resources/application.yml" 2>/dev/null | grep -q "enabled.*true" && echo "yes" || echo "no")
  has_flyway=$(grep -A1 "flyway:" "$service_dir/src/main/resources/application.yml" 2>/dev/null | grep -q "enabled.*true" && echo "yes" || echo "no")

  if [ "$has_liquibase" = "yes" ]; then
    echo "  ✅ Liquibase configured"

    # Verify changelog exists
    if [ ! -f "$service_dir/src/main/resources/db/changelog/db.changelog-master.xml" ]; then
      echo "  ⚠️  Warning: Liquibase enabled but db.changelog-master.xml not found"
      WARNINGS=$((WARNINGS + 1))
    fi
  elif [ "$has_flyway" = "yes" ]; then
    echo "  ⚠️  Flyway configured (should migrate to Liquibase)"
    WARNINGS=$((WARNINGS + 1))
  else
    echo "  ❌ No migration tool configured"
    ERRORS=$((ERRORS + 1))
  fi

  # Check 3: PostgreSQL driver in test dependencies
  if grep -q "testImplementation.*postgresql" "$service_dir/build.gradle.kts" 2>/dev/null; then
    echo "  ✅ PostgreSQL test driver configured"
  else
    echo "  ⚠️  Warning: PostgreSQL driver may be missing in test dependencies"
    WARNINGS=$((WARNINGS + 1))
  fi

  # Check 4: Entity-migration validation test exists
  if find "$service_dir/src/test/java" -name "*EntityMigrationValidationTest.java" 2>/dev/null | grep -q .; then
    echo "  ✅ Entity-migration validation test exists"
  else
    echo "  ⚠️  Warning: EntityMigrationValidationTest not found"
    WARNINGS=$((WARNINGS + 1))
  fi

  echo ""
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary:"
echo "  ❌ Errors: $ERRORS"
echo "  ⚠️  Warnings: $WARNINGS"
echo ""

if [ $ERRORS -gt 0 ]; then
  echo "❌ Validation failed - fix errors before proceeding"
  exit 1
elif [ $WARNINGS -gt 0 ]; then
  echo "⚠️  Validation passed with warnings"
  exit 0
else
  echo "✅ All validations passed!"
  exit 0
fi
