#!/bin/bash
# HIPAA Compliance Checker for HDIM
# Scans code for common HIPAA compliance violations

set -e

ERRORS=0
WARNINGS=0

echo "🔍 HDIM HIPAA Compliance Checker"
echo "================================"
echo ""

# Check 1: Cache TTL configuration
echo "1️⃣  Checking cache TTL configuration..."
if grep -r "time-to-live:" backend/modules/services/*/src/main/resources/application*.yml 2>/dev/null | grep -v "300000" | grep -v "#"; then
    echo "❌ ERROR: Found cache TTL > 5 minutes (300000ms)"
    ERRORS=$((ERRORS + 1))
else
    echo "✅ Cache TTL configuration looks good"
fi
echo ""

# Check 2: Cache-Control headers on PHI endpoints
echo "2️⃣  Checking for Cache-Control headers on PHI endpoints..."
MISSING_HEADERS=$(grep -r "@GetMapping.*patient" backend/modules/services/*/src/main/java --include="*Controller.java" -A 20 | \
    grep -B 20 "ResponseEntity" | \
    grep -L "Cache-Control" || true)

if [ -n "$MISSING_HEADERS" ]; then
    echo "⚠️  WARNING: Some patient endpoints may be missing Cache-Control headers"
    echo "$MISSING_HEADERS"
    WARNINGS=$((WARNINGS + 1))
else
    echo "✅ Cache-Control headers appear to be in use"
fi
echo ""

# Check 3: PHI in log statements
echo "3️⃣  Checking for PHI in log statements..."
PHI_LOGS=$(grep -r "log\.(info|debug|warn|error).*patient\." backend/modules/services/*/src/main/java --include="*.java" || true)

if [ -n "$PHI_LOGS" ]; then
    echo "⚠️  WARNING: Potential PHI in log statements detected"
    echo "$PHI_LOGS" | head -5
    WARNINGS=$((WARNINGS + 1))
else
    echo "✅ No obvious PHI in log statements"
fi
echo ""

# Check 4: @Cacheable without proper configuration
echo "4️⃣  Checking @Cacheable annotations..."
CACHEABLE_COUNT=$(grep -r "@Cacheable" backend/modules/services/*/src/main/java --include="*.java" | wc -l)
echo "   Found $CACHEABLE_COUNT @Cacheable annotations"

if [ "$CACHEABLE_COUNT" -gt 0 ]; then
    echo "   ⚠️  Verify each has TTL ≤ 5 minutes in application.yml"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Check 5: Audit annotations
echo "5️⃣  Checking for @Audited annotations on PHI access..."
PATIENT_METHODS=$(grep -r "getPatient\|findPatient" backend/modules/services/*/src/main/java --include="*Service.java" | wc -l)
AUDITED_METHODS=$(grep -B 2 "getPatient\|findPatient" backend/modules/services/*/src/main/java --include="*Service.java" | grep "@Audited" | wc -l || true)

echo "   Found $PATIENT_METHODS patient access methods"
echo "   Found $AUDITED_METHODS with @Audited annotation"

if [ "$AUDITED_METHODS" -lt "$PATIENT_METHODS" ]; then
    echo "   ⚠️  WARNING: Some patient access methods may be missing @Audited"
    WARNINGS=$((WARNINGS + 1))
fi
echo ""

# Summary
echo "================================"
echo "📊 Summary:"
echo "   Errors:   $ERRORS"
echo "   Warnings: $WARNINGS"
echo ""

if [ "$ERRORS" -gt 0 ]; then
    echo "❌ HIPAA compliance issues found. Please review."
    exit 1
elif [ "$WARNINGS" -gt 0 ]; then
    echo "⚠️  Warnings found. Manual review recommended."
    exit 0
else
    echo "✅ No obvious HIPAA compliance issues detected."
    exit 0
fi
