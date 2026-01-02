#!/bin/bash

# ============================================================
# HealthData Platform - Monitoring Health Check Script
# Verifies all monitoring services are running correctly
# ============================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Service endpoints
PROMETHEUS_URL="http://localhost:9090"
GRAFANA_URL="http://localhost:3000"
ALERTMANAGER_URL="http://localhost:9093"
ELASTICSEARCH_URL="http://localhost:9200"
KIBANA_URL="http://localhost:5601"
LOGSTASH_URL="http://localhost:9600"

echo "=================================================="
echo "HealthData Platform - Monitoring Health Check"
echo "=================================================="
echo ""

# Function to check service health
check_service() {
    local service_name=$1
    local url=$2
    local expected_status=${3:-200}

    echo -n "Checking $service_name... "

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")

    if [ "$response" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ OK${NC} (HTTP $response)"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC} (HTTP $response)"
        return 1
    fi
}

# Function to check Docker service
check_docker_service() {
    local service_name=$1

    echo -n "Checking Docker service: $service_name... "

    if docker ps | grep -q "$service_name"; then
        status=$(docker inspect --format='{{.State.Health.Status}}' "$service_name" 2>/dev/null || echo "unknown")
        if [ "$status" = "healthy" ] || [ "$status" = "unknown" ]; then
            echo -e "${GREEN}✓ RUNNING${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠ UNHEALTHY${NC} (Status: $status)"
            return 1
        fi
    else
        echo -e "${RED}✗ NOT RUNNING${NC}"
        return 1
    fi
}

# Counter for failed checks
failed_checks=0

echo "=== Docker Services ==="
echo ""

# Check Docker services
check_docker_service "healthdata-prometheus" || ((failed_checks++))
check_docker_service "healthdata-grafana" || ((failed_checks++))
check_docker_service "healthdata-alertmanager" || ((failed_checks++))
check_docker_service "healthdata-elasticsearch" || ((failed_checks++))
check_docker_service "healthdata-kibana" || ((failed_checks++))
check_docker_service "healthdata-logstash" || ((failed_checks++))
check_docker_service "healthdata-node-exporter" || ((failed_checks++))
check_docker_service "healthdata-postgres-exporter" || ((failed_checks++))
check_docker_service "healthdata-redis-exporter" || ((failed_checks++))
check_docker_service "healthdata-kafka-exporter" || ((failed_checks++))

echo ""
echo "=== HTTP Endpoints ==="
echo ""

# Check HTTP endpoints
check_service "Prometheus" "$PROMETHEUS_URL/-/healthy" || ((failed_checks++))
check_service "Prometheus API" "$PROMETHEUS_URL/api/v1/status/config" || ((failed_checks++))
check_service "Grafana" "$GRAFANA_URL/api/health" || ((failed_checks++))
check_service "Alertmanager" "$ALERTMANAGER_URL/-/healthy" || ((failed_checks++))
check_service "Elasticsearch" "$ELASTICSEARCH_URL/_cluster/health" || ((failed_checks++))
check_service "Kibana" "$KIBANA_URL/api/status" || ((failed_checks++))
check_service "Logstash" "$LOGSTASH_URL" || ((failed_checks++))

echo ""
echo "=== Prometheus Targets ==="
echo ""

# Check Prometheus targets
echo -n "Checking Prometheus targets... "
targets=$(curl -s "$PROMETHEUS_URL/api/v1/targets" | grep -o '"health":"up"' | wc -l)
total_targets=$(curl -s "$PROMETHEUS_URL/api/v1/targets" | grep -o '"health":' | wc -l)

if [ "$targets" -gt 0 ]; then
    echo -e "${GREEN}✓ OK${NC} ($targets/$total_targets targets up)"
else
    echo -e "${RED}✗ FAILED${NC} (No targets up)"
    ((failed_checks++))
fi

echo ""
echo "=== Prometheus Alerts ==="
echo ""

# Check active alerts
echo -n "Checking active alerts... "
firing_alerts=$(curl -s "$PROMETHEUS_URL/api/v1/alerts" | grep -o '"state":"firing"' | wc -l)

if [ "$firing_alerts" -eq 0 ]; then
    echo -e "${GREEN}✓ No firing alerts${NC}"
else
    echo -e "${YELLOW}⚠ $firing_alerts alerts firing${NC}"
fi

echo ""
echo "=== Elasticsearch Indices ==="
echo ""

# Check Elasticsearch indices
echo -n "Checking Elasticsearch indices... "
indices=$(curl -s "$ELASTICSEARCH_URL/_cat/indices?format=json" 2>/dev/null | grep -o '"index":' | wc -l)

if [ "$indices" -gt 0 ]; then
    echo -e "${GREEN}✓ OK${NC} ($indices indices found)"
else
    echo -e "${YELLOW}⚠ No indices${NC}"
fi

# Check cluster health
echo -n "Checking Elasticsearch cluster health... "
cluster_status=$(curl -s "$ELASTICSEARCH_URL/_cluster/health" 2>/dev/null | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

case "$cluster_status" in
    "green")
        echo -e "${GREEN}✓ GREEN${NC}"
        ;;
    "yellow")
        echo -e "${YELLOW}⚠ YELLOW${NC}"
        ;;
    "red")
        echo -e "${RED}✗ RED${NC}"
        ((failed_checks++))
        ;;
    *)
        echo -e "${RED}✗ UNKNOWN${NC}"
        ((failed_checks++))
        ;;
esac

echo ""
echo "=== Storage Usage ==="
echo ""

# Check Docker volumes
echo "Docker volume usage:"
docker system df -v | grep "healthdata-" | awk '{print $1, $2}'

echo ""
echo "=== Resource Usage ==="
echo ""

# Check container resource usage
echo "Top 5 containers by memory usage:"
docker stats --no-stream --format "table {{.Name}}\t{{.MemUsage}}" | grep healthdata | head -5

echo ""
echo "=================================================="
echo "Health Check Summary"
echo "=================================================="

if [ $failed_checks -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "Monitoring stack is healthy and operational."
    exit 0
else
    echo -e "${RED}✗ $failed_checks check(s) failed${NC}"
    echo ""
    echo "Please review the output above and address any issues."
    exit 1
fi
