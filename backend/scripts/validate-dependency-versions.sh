#!/bin/bash
# Validates no hardcoded dependency versions remain in service build files

ISSUES=0

echo "🔍 Validating dependency versions across all services..."
echo ""

# Check for hardcoded Spring Cloud versions
echo "Checking for hardcoded Spring Cloud versions..."
SPRING_CLOUD_ISSUES=$(find modules/services -name "build.gradle.kts" -exec grep -l "spring-cloud-dependencies:" {} \; 2>/dev/null)
if [ -n "$SPRING_CLOUD_ISSUES" ]; then
    echo "❌ Found hardcoded Spring Cloud versions:"
    echo "$SPRING_CLOUD_ISSUES"
    ISSUES=$((ISSUES + 1))
else
    echo "✅ No hardcoded Spring Cloud versions"
fi
echo ""

# Check for hardcoded springdoc-openapi versions
echo "Checking for hardcoded springdoc-openapi versions..."
SPRINGDOC_ISSUES=$(find modules/services -name "build.gradle.kts" -exec grep -l 'springdoc-openapi-starter-webmvc-ui:[0-9]' {} \; 2>/dev/null)
if [ -n "$SPRINGDOC_ISSUES" ]; then
    echo "❌ Found hardcoded springdoc-openapi versions:"
    echo "$SPRINGDOC_ISSUES"
    ISSUES=$((ISSUES + 1))
else
    echo "✅ No hardcoded springdoc-openapi versions"
fi
echo ""

# Check for hardcoded Resilience4j versions
echo "Checking for hardcoded Resilience4j versions..."
RESILIENCE_ISSUES=$(find modules/services -name "build.gradle.kts" -exec grep -l 'resilience4j.*:[0-9]' {} \; 2>/dev/null)
if [ -n "$RESILIENCE_ISSUES" ]; then
    echo "❌ Found hardcoded Resilience4j versions:"
    echo "$RESILIENCE_ISSUES"
    ISSUES=$((ISSUES + 1))
else
    echo "✅ No hardcoded Resilience4j versions"
fi
echo ""

# Check for hardcoded PostgreSQL versions
echo "Checking for hardcoded PostgreSQL driver versions..."
POSTGRES_ISSUES=$(find modules/services -name "build.gradle.kts" -exec grep -l 'postgresql:postgresql:[0-9]' {} \; 2>/dev/null)
if [ -n "$POSTGRES_ISSUES" ]; then
    echo "❌ Found hardcoded PostgreSQL versions:"
    echo "$POSTGRES_ISSUES"
    ISSUES=$((ISSUES + 1))
else
    echo "✅ No hardcoded PostgreSQL versions"
fi
echo ""

# Check for Jackson force() overrides
echo "Checking for Jackson force() overrides..."
JACKSON_ISSUES=$(find modules/services -name "build.gradle.kts" -exec grep -l 'force("com.fasterxml.jackson' {} \; 2>/dev/null)
if [ -n "$JACKSON_ISSUES" ]; then
    echo "⚠️  Found Jackson force() overrides (may be intentional):"
    echo "$JACKSON_ISSUES"
    echo "    Please verify these are documented and necessary"
else
    echo "✅ No Jackson force() overrides"
fi
echo ""

# Summary
echo "========================================"
if [ $ISSUES -eq 0 ]; then
    echo "✅ All services comply with version catalog standards"
    exit 0
else
    echo "❌ Found $ISSUES issue(s) - please fix before merging"
    exit 1
fi
