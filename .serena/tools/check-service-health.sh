#!/bin/bash
# Service Health Checker for HDIM
# Checks health status of all running services

set -e

echo "🏥 HDIM Service Health Checker"
echo "=============================="
echo ""

SERVICES=(
    "gateway-service:8001"
    "cql-engine-service:8081"
    "patient-service:8084"
    "fhir-service:8085"
    "care-gap-service:8086"
    "quality-measure-service:8087"
)

INFRASTRUCTURE=(
    "postgres:5435"
    "redis:6380"
    "kafka:9094"
)

HEALTHY=0
UNHEALTHY=0
OFFLINE=0

echo "🔍 Checking Core Services..."
echo "---"

for service_port in "${SERVICES[@]}"; do
    SERVICE=$(echo "$service_port" | cut -d: -f1)
    PORT=$(echo "$service_port" | cut -d: -f2)

    printf "%-30s " "$SERVICE ($PORT):"

    # Check if port is open
    if nc -z localhost "$PORT" 2>/dev/null; then
        # Try actuator health endpoint
        HEALTH_RESPONSE=$(curl -s "http://localhost:$PORT/actuator/health" 2>/dev/null || echo "{}")

        if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
            echo "✅ HEALTHY"
            HEALTHY=$((HEALTHY + 1))
        else
            echo "⚠️  DEGRADED"
            UNHEALTHY=$((UNHEALTHY + 1))
        fi
    else
        echo "❌ OFFLINE"
        OFFLINE=$((OFFLINE + 1))
    fi
done

echo ""
echo "🔧 Checking Infrastructure..."
echo "---"

for infra_port in "${INFRASTRUCTURE[@]}"; do
    INFRA=$(echo "$infra_port" | cut -d: -f1)
    PORT=$(echo "$infra_port" | cut -d: -f2)

    printf "%-30s " "$INFRA ($PORT):"

    if nc -z localhost "$PORT" 2>/dev/null; then
        echo "✅ RUNNING"
        HEALTHY=$((HEALTHY + 1))
    else
        echo "❌ OFFLINE"
        OFFLINE=$((OFFLINE + 1))
    fi
done

echo ""
echo "=============================="
echo "📊 Health Summary:"
echo "   ✅ Healthy:  $HEALTHY"
echo "   ⚠️  Degraded: $UNHEALTHY"
echo "   ❌ Offline:  $OFFLINE"
echo ""

if [ "$OFFLINE" -gt 0 ]; then
    echo "⚠️  Some services are offline."
    echo ""
    echo "🔧 Quick fixes:"
    echo "   Start all: docker compose up -d"
    echo "   Check logs: docker compose logs -f [service-name]"
    echo "   Restart: docker compose restart [service-name]"
    exit 1
elif [ "$UNHEALTHY" -gt 0 ]; then
    echo "⚠️  Some services are degraded. Check logs for details."
    exit 1
else
    echo "✅ All services are healthy!"
    exit 0
fi
