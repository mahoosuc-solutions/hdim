#!/bin/bash
# Pre-Commit Check Workflow
# Runs all validation checks before committing code

set -e

echo "🔍 HDIM Pre-Commit Validation"
echo "=============================="
echo ""

ERRORS=0

# 1. HIPAA Compliance
echo "1️⃣  Running HIPAA compliance check..."
if bash .serena/tools/check-hipaa-compliance.sh; then
    echo "   ✅ HIPAA compliance passed"
else
    echo "   ❌ HIPAA compliance failed"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 2. Multi-Tenant Queries
echo "2️⃣  Running multi-tenant query check..."
if bash .serena/tools/check-multitenant-queries.sh; then
    echo "   ✅ Multi-tenant queries passed"
else
    echo "   ❌ Multi-tenant queries failed"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 3. Entity-Migration Sync
echo "3️⃣  Running entity-migration validation..."
if bash .serena/tools/validate-entity-migration-sync.sh; then
    echo "   ✅ Entity-migration sync passed"
else
    echo "   ❌ Entity-migration sync failed"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 4. Build Check
echo "4️⃣  Running build check..."
if (cd backend && ./gradlew build -x test --quiet); then
    echo "   ✅ Build successful"
else
    echo "   ❌ Build failed"
    ERRORS=$((ERRORS + 1))
fi
echo ""

# 5. Code Quality (optional - can add spotbugs, checkstyle, etc.)
echo "5️⃣  Code quality checks..."
echo "   ⏭️  Skipped (configure spotbugs/checkstyle if needed)"
echo ""

# Summary
echo "=============================="
echo "📊 Pre-Commit Summary:"
echo ""

if [ "$ERRORS" -eq 0 ]; then
    echo "✅ All checks passed! Safe to commit."
    echo ""
    echo "💡 Commit checklist:"
    echo "   [ ] Meaningful commit message"
    echo "   [ ] Co-authored-by Claude tag"
    echo "   [ ] No secrets in code"
    echo "   [ ] Tests updated"
    exit 0
else
    echo "❌ $ERRORS check(s) failed. Please fix before committing."
    echo ""
    echo "🔧 Common fixes:"
    echo "   - HIPAA: Add Cache-Control headers, verify TTL"
    echo "   - Multi-tenant: Add tenantId filter to queries"
    echo "   - Entity-migration: Create migration for entity changes"
    echo "   - Build: Fix compilation errors"
    exit 1
fi
