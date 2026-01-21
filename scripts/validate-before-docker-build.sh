#!/bin/bash
# Pre-Docker build validation script
#
# Comprehensive validation that ensures data model consistency BEFORE building
# Docker images. This prevents runtime failures and reduces build-deploy-fail cycles.
#
# Validations:
# 1. Database configuration (ddl-auto=validate, Liquibase enabled)
# 2. Entity-migration synchronization (all entities match migrations)
# 3. Liquibase rollback coverage (all migrations have rollback SQL)
#
# Usage: ./validate-before-docker-build.sh
# Exit codes: 0 = all validations passed, 1 = any validation failed

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR/.."

echo "========================================="
echo "Pre-Docker Build Validation"
echo "========================================="
echo ""

# Step 1: Database configuration validation
echo "Step 1/3: Database configuration..."
echo "─────────────────────────────────────────"

if "$PROJECT_ROOT/backend/scripts/validate-database-config.sh"; then
    echo "✅ Database configuration validated"
else
    echo "❌ Database configuration validation failed"
    exit 1
fi

echo ""

# Step 2: Entity-migration synchronization
echo "Step 2/3: Entity-migration synchronization..."
echo "─────────────────────────────────────────"

if "$PROJECT_ROOT/backend/scripts/validate-entities-pre-build.sh"; then
    echo "✅ Entity-migration synchronization validated"
else
    echo "❌ Entity-migration synchronization validation failed"
    exit 1
fi

echo ""

# Step 3: Liquibase rollback coverage
echo "Step 3/3: Liquibase rollback coverage..."
echo "─────────────────────────────────────────"

if "$PROJECT_ROOT/backend/scripts/test-liquibase-rollback.sh"; then
    echo "✅ Liquibase rollback coverage validated"
else
    echo "❌ Liquibase rollback coverage validation failed"
    exit 1
fi

echo ""
echo "========================================="
echo "✅ All pre-Docker build validations passed"
echo "========================================="
echo ""
echo "Ready to run: docker compose build"
echo ""

exit 0
