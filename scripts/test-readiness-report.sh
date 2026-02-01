#!/bin/bash
# Test Readiness Report - Comprehensive validation with detailed report
# Generates a markdown report of all container and service status
# Usage: ./scripts/test-readiness-report.sh [--output report.md]

set -e

OUTPUT_FILE=""
if [ "$1" == "--output" ] && [ -n "$2" ]; then
    OUTPUT_FILE="$2"
fi

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Report data
REPORT=""
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

append_report() {
    REPORT+="$1\n"
}

generate_report() {
    append_report "# HDIM Test Readiness Report"
    append_report ""
    append_report "**Generated:** $TIMESTAMP"
    append_report "**Platform:** HealthData-in-Motion (HDIM)"
    append_report ""
    append_report "---"
    append_report ""
    append_report "## Executive Summary"
    append_report ""

    # Run validation and capture output
    VALIDATION_OUTPUT=$(./scripts/validate-containers.sh 2>&1)
    
    # Parse results
    TOTAL=$(echo "$VALIDATION_OUTPUT" | grep -oP 'Total Checks: \K\d+' || echo "0")
    PASSED=$(echo "$VALIDATION_OUTPUT" | grep -oP 'Passed: \K\d+' || echo "0")
    WARNINGS=$(echo "$VALIDATION_OUTPUT" | grep -oP 'Warnings: \K\d+' || echo "0")
    FAILED=$(echo "$VALIDATION_OUTPUT" | grep -oP 'Failed: \K\d+' || echo "0")

    append_report "| Metric | Count | Status |"
    append_report "|--------|-------|--------|"
    
    if [ "$FAILED" -eq 0 ]; then
        append_report "| Total Checks | $TOTAL | ✅ |"
        append_report "| Passed | $PASSED | ✅ |"
        append_report "| Warnings | $WARNINGS | ⚠️ |"
        append_report "| Failed | $FAILED | ✅ |"
        append_report ""
        append_report "**Status:** ✅ **READY FOR TESTING**"
    else
        append_report "| Total Checks | $TOTAL | ⚠️ |"
        append_report "| Passed | $PASSED | ✅ |"
        append_report "| Warnings | $WARNINGS | ⚠️ |"
        append_report "| Failed | $FAILED | ❌ |"
        append_report ""
        append_report "**Status:** ❌ **NOT READY** - $FAILED critical issue(s) need attention"
    fi

    append_report ""
    append_report "---"
    append_report ""
    append_report "## Detailed Validation Results"
    append_report ""
    append_report "\`\`\`"
    append_report "$VALIDATION_OUTPUT"
    append_report "\`\`\`"

    append_report ""
    append_report "---"
    append_report ""
    append_report "## Container Status"
    append_report ""

    # Get container status
    append_report "| Container | Status | Health | Ports |"
    append_report "|-----------|--------|--------|-------|"

    while IFS= read -r container; do
        if [ -n "$container" ]; then
            STATUS=$(docker inspect --format='{{.State.Status}}' "$container" 2>/dev/null || echo "not found")
            HEALTH=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null || echo "N/A")
            PORTS=$(docker inspect --format='{{range \$p, \$conf := .NetworkSettings.Ports}}{{range \$conf}}\$p{{end}}{{end}}' "$container" 2>/dev/null | tr '\n' ',' | sed 's/,$//' || echo "N/A")
            
            if [ "$STATUS" == "running" ]; then
                STATUS_ICON="✅"
            else
                STATUS_ICON="❌"
            fi
            
            if [ "$HEALTH" == "healthy" ]; then
                HEALTH_ICON="✅"
            elif [ "$HEALTH" == "N/A" ]; then
                HEALTH_ICON="⚠️"
            else
                HEALTH_ICON="❌"
            fi
            
            append_report "| $container | $STATUS_ICON $STATUS | $HEALTH_ICON $HEALTH | $PORTS |"
        fi
    done < <(docker ps -a --format '{{.Names}}' | grep -E '^healthdata-')

    append_report ""
    append_report "---"
    append_report ""
    append_report "## Service Health Endpoints"
    append_report ""

    SERVICES=(
        "Gateway Service:http://localhost:8080/actuator/health"
        "CQL Engine:http://localhost:8081/cql-engine/actuator/health"
        "Consent Service:http://localhost:8082/consent/actuator/health"
        "Event Processing:http://localhost:8083/events/actuator/health"
        "Patient Service:http://localhost:8084/patient/actuator/health"
        "FHIR Service:http://localhost:8085/fhir/actuator/health"
        "Care Gap Service:http://localhost:8086/care-gap/actuator/health"
        "Quality Measure:http://localhost:8087/quality-measure/actuator/health"
        "Clinical Portal:http://localhost:4200"
    )

    append_report "| Service | Endpoint | Status |"
    append_report "|---------|----------|--------|"

    for service_info in "${SERVICES[@]}"; do
        IFS=':' read -r name url <<< "$service_info"
        HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")
        
        if [ "$HTTP_CODE" == "200" ]; then
            STATUS="✅ Healthy"
        elif [ "$HTTP_CODE" == "000" ]; then
            STATUS="❌ Unreachable"
        else
            STATUS="⚠️ HTTP $HTTP_CODE"
        fi
        
        append_report "| $name | \`$url\` | $STATUS |"
    done

    append_report ""
    append_report "---"
    append_report ""
    append_report "## Next Steps"
    append_report ""

    if [ "$FAILED" -eq 0 ]; then
        append_report "✅ All systems are ready for testing!"
        append_report ""
        append_report "### Recommended Test Commands:"
        append_report ""
        append_report "\`\`\`bash"
        append_report "# Run integration tests"
        append_report "npm test"
        append_report ""
        append_report "# Run E2E tests"
        append_report "npm run test:e2e"
        append_report ""
        append_report "# Check service logs"
        append_report "docker compose logs -f <service-name>"
        append_report "\`\`\`"
    else
        append_report "❌ Please fix the following issues before testing:"
        append_report ""
        append_report "1. Review failed checks above"
        append_report "2. Check container logs: \`docker logs <container-name>\`"
        append_report "3. Restart failed services: \`docker compose restart <service-name>\`"
        append_report "4. Run validation again: \`./scripts/validate-containers.sh\`"
    fi

    # Output report
    if [ -n "$OUTPUT_FILE" ]; then
        echo -e "$REPORT" > "$OUTPUT_FILE"
        echo -e "${GREEN}Report generated: $OUTPUT_FILE${NC}"
    else
        echo -e "$REPORT"
    fi
}

# Run report generation
generate_report
