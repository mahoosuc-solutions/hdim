#!/bin/bash
# Multi-Tenant Query Checker for HDIM
# Scans repository methods for missing tenant filters

set -e

ERRORS=0
WARNINGS=0

echo "🔍 HDIM Multi-Tenant Query Checker"
echo "==================================="
echo ""

# Check 1: @Query annotations without tenantId filter
echo "1️⃣  Checking @Query annotations for tenant filtering..."

QUERY_FILES=$(find backend/modules/services/*/src/main/java -name "*Repository.java" 2>/dev/null || true)

for file in $QUERY_FILES; do
    # Find @Query annotations
    QUERIES=$(grep -n "@Query" "$file" || true)

    if [ -n "$QUERIES" ]; then
        while IFS= read -r query_line; do
            LINE_NUM=$(echo "$query_line" | cut -d: -f1)

            # Get the query content (next few lines)
            QUERY_CONTENT=$(sed -n "${LINE_NUM},$((LINE_NUM + 10))p" "$file")

            # Check if query includes tenantId filter
            if ! echo "$QUERY_CONTENT" | grep -q "tenantId"; then
                # Exclude metadata queries and health checks
                if ! echo "$QUERY_CONTENT" | grep -qE "(metadata|health|actuator|liquibase)"; then
                    echo "❌ Missing tenantId filter:"
                    echo "   File: $file:$LINE_NUM"
                    echo "   Query: $(echo "$QUERY_CONTENT" | head -1)"
                    ERRORS=$((ERRORS + 1))
                fi
            fi
        done <<< "$QUERIES"
    fi
done

if [ "$ERRORS" -eq 0 ]; then
    echo "✅ All @Query annotations include tenant filtering"
fi
echo ""

# Check 2: Repository methods without "AndTenant" suffix
echo "2️⃣  Checking repository method naming..."

METHOD_VIOLATIONS=0
for file in $QUERY_FILES; do
    # Look for findBy methods that don't include "AndTenant"
    METHODS=$(grep -n "findBy.*(" "$file" | grep -v "AndTenant" | grep -v "//" || true)

    if [ -n "$METHODS" ]; then
        echo "⚠️  Repository methods without 'AndTenant' suffix in $(basename $file):"
        echo "$METHODS"
        METHOD_VIOLATIONS=$((METHOD_VIOLATIONS + 1))
    fi
done

if [ "$METHOD_VIOLATIONS" -gt 0 ]; then
    echo "   Found $METHOD_VIOLATIONS files with potential issues"
    WARNINGS=$((WARNINGS + 1))
else
    echo "✅ Repository method naming looks good"
fi
echo ""

# Check 3: JPA entities with tenantId field
echo "3️⃣  Checking entities for tenantId field..."

ENTITY_FILES=$(find backend/modules/services/*/src/main/java -name "*Entity.java" -o -name "*Domain.java" 2>/dev/null | \
    grep -E "domain/model|persistence" || true)

MISSING_TENANT=0
for file in $ENTITY_FILES; do
    if grep -q "@Entity" "$file"; then
        if ! grep -q "tenantId" "$file"; then
            echo "⚠️  Entity missing tenantId field: $(basename $file)"
            MISSING_TENANT=$((MISSING_TENANT + 1))
        fi
    fi
done

if [ "$MISSING_TENANT" -gt 0 ]; then
    echo "   Found $MISSING_TENANT entities without tenantId"
    WARNINGS=$((WARNINGS + 1))
else
    echo "✅ All entities include tenantId field"
fi
echo ""

# Check 4: Controllers with X-Tenant-ID header
echo "4️⃣  Checking controllers for X-Tenant-ID header..."

CONTROLLER_FILES=$(find backend/modules/services/*/src/main/java -name "*Controller.java" 2>/dev/null || true)

MISSING_HEADER=0
for file in $CONTROLLER_FILES; do
    # Check if controller has endpoints
    if grep -q "@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping" "$file"; then
        # Check if any method uses X-Tenant-ID header
        if ! grep -q "@RequestHeader.*X-Tenant-ID" "$file"; then
            echo "⚠️  Controller missing X-Tenant-ID header: $(basename $file)"
            MISSING_HEADER=$((MISSING_HEADER + 1))
        fi
    fi
done

if [ "$MISSING_HEADER" -gt 0 ]; then
    echo "   Found $MISSING_HEADER controllers without tenant header"
    WARNINGS=$((WARNINGS + 1))
else
    echo "✅ All controllers use X-Tenant-ID header"
fi
echo ""

# Summary
echo "==================================="
echo "📊 Summary:"
echo "   Errors:   $ERRORS (queries missing tenant filter)"
echo "   Warnings: $WARNINGS (naming/field issues)"
echo ""

if [ "$ERRORS" -gt 0 ]; then
    echo "❌ Multi-tenant isolation violations found!"
    echo "   All queries MUST filter by tenantId."
    exit 1
elif [ "$WARNINGS" -gt 0 ]; then
    echo "⚠️  Warnings found. Manual review recommended."
    exit 0
else
    echo "✅ Multi-tenant isolation looks good."
    exit 0
fi
