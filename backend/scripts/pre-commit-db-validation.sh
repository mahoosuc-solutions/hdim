#!/bin/bash
# Pre-commit hook for database validation
# Runs entity-migration validation tests when JPA entities are modified

set -e

# Check if JPA entities have been changed
changed_entities=$(git diff --cached --name-only --diff-filter=ACM | grep -E "Entity\.java$" || true)

if [ -z "$changed_entities" ]; then
  echo "✅ No JPA entities modified, skipping validation"
  exit 0
fi

echo "🔍 JPA entities changed, running entity-migration validation..."
echo ""
echo "Changed entities:"
echo "$changed_entities" | sed 's/^/  - /'
echo ""

# Run validation tests
cd "$(git rev-parse --show-toplevel)/backend"

if ./gradlew test --tests "*EntityMigrationValidationTest" -q --no-daemon 2>&1 | tee /tmp/validation-output.txt; then
  echo ""
  echo "✅ Entity-migration validation passed"
  exit 0
else
  echo ""
  echo "❌ Entity-migration validation failed!"
  echo ""
  echo "Your JPA entities don't match the database schema defined in Liquibase migrations."
  echo ""
  echo "To fix this:"
  echo "  1. Create a new Liquibase migration file in db/changelog/"
  echo "  2. Update db.changelog-master.xml to include it"
  echo "  3. Run validation: ./gradlew test --tests \"*EntityMigrationValidationTest\""
  echo ""
  echo "See: backend/docs/ENTITY_MIGRATION_GUIDE.md"
  echo ""
  exit 1
fi
