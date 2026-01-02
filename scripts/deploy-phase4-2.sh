#!/bin/bash

# Phase 4.2 Docker Deployment Script
# Deploys Prometheus metrics collection and Grafana dashboard to Docker Compose environment
#
# Usage: ./scripts/deploy-phase4-2.sh [staging|production]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT="${1:-staging}"
DOCKER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../docker" && pwd)"
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}Phase 4.2: Docker Deployment - Prometheus & Grafana${NC}"
echo -e "${BLUE}Environment: ${ENVIRONMENT}${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
echo

# Function to print status
print_status() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Step 1: Verify Docker is running
echo -e "${BLUE}Step 1: Checking Docker availability${NC}"
if ! command -v docker &> /dev/null; then
    print_error "Docker is not installed"
    exit 1
fi
print_status "Docker found"

if ! docker ps &> /dev/null; then
    print_error "Docker daemon is not running"
    exit 1
fi
print_status "Docker daemon is running"
echo

# Step 2: Verify configuration files exist
echo -e "${BLUE}Step 2: Verifying configuration files${NC}"

if [ ! -f "$DOCKER_DIR/prometheus/prometheus.yml" ]; then
    print_error "prometheus.yml not found at $DOCKER_DIR/prometheus/prometheus.yml"
    exit 1
fi
print_status "prometheus.yml exists"

if [ ! -f "$DOCKER_DIR/prometheus/rules/security-alerts.yml" ]; then
    print_error "security-alerts.yml not found at $DOCKER_DIR/prometheus/rules/security-alerts.yml"
    exit 1
fi
print_status "security-alerts.yml exists"

if [ ! -f "$DOCKER_DIR/grafana/dashboards/hdim-authentication-security.json" ]; then
    print_error "hdim-authentication-security.json not found"
    exit 1
fi
print_status "Grafana dashboard JSON exists"
echo

# Step 3: Validate Prometheus configuration
echo -e "${BLUE}Step 3: Validating Prometheus configuration${NC}"

# Check if prometheus-compat if available (optional)
if command -v promtool &> /dev/null; then
    promtool check config "$DOCKER_DIR/prometheus/prometheus.yml"
    print_status "Prometheus config is valid"
else
    print_warning "promtool not available, skipping config validation"
fi

# Count scrape configs
SCRAPE_COUNT=$(grep -c "job_name:" "$DOCKER_DIR/prometheus/prometheus.yml")
echo "   Found $SCRAPE_COUNT scrape job configurations"
if [ "$SCRAPE_COUNT" -ge 18 ]; then
    print_status "All backend services configured"
else
    print_warning "Expected 18+ services, found $SCRAPE_COUNT"
fi

# Check rules are referenced
if grep -q 'rules/\*.yml' "$DOCKER_DIR/prometheus/prometheus.yml"; then
    print_status "Alert rules are configured"
else
    print_error "Alert rules not configured in prometheus.yml"
    exit 1
fi
echo

# Step 4: Prepare environment
echo -e "${BLUE}Step 4: Preparing deployment${NC}"

# Create Grafana provisioning directories if needed
mkdir -p "$DOCKER_DIR/grafana/provisioning/dashboards"
mkdir -p "$DOCKER_DIR/grafana/provisioning/datasources"
print_status "Grafana directories prepared"

# Create datasources provisioning file
cat > "$DOCKER_DIR/grafana/provisioning/datasources/prometheus.yml" << 'EOF'
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: 15s
EOF
print_status "Prometheus datasource provisioning configured"

# Create dashboards provisioning file
cat > "$DOCKER_DIR/grafana/provisioning/dashboards/dashboards.yml" << 'EOF'
apiVersion: 1

providers:
  - name: 'HDIM Dashboards'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF
print_status "Grafana dashboard provisioning configured"

# Copy dashboard to provisioning directory
cp "$DOCKER_DIR/grafana/dashboards/hdim-authentication-security.json" \
   "$DOCKER_DIR/grafana/provisioning/dashboards/"
print_status "Dashboard copied to provisioning directory"
echo

# Step 5: Check current services
echo -e "${BLUE}Step 5: Checking current Docker services${NC}"

RUNNING_SERVICES=$(docker-compose ps 2>/dev/null | grep -c "Up" || echo "0")
echo "   Currently running services: $RUNNING_SERVICES"

if [ "$RUNNING_SERVICES" -eq 0 ]; then
    print_warning "No services appear to be running"
    print_warning "Make sure docker-compose is running with: docker-compose up -d"
else
    print_status "Backend services are running"
fi
echo

# Step 6: Health checks
echo -e "${BLUE}Step 6: Running health checks${NC}"

# Check Prometheus health
if docker ps | grep -q "prometheus"; then
    if docker exec healthdata-prometheus wget -q -O- http://localhost:9090/-/healthy &> /dev/null 2>&1 || \
       curl -s http://localhost:9090/-/healthy &> /dev/null; then
        print_status "Prometheus is healthy"
    else
        print_warning "Prometheus health check failed (may not be running yet)"
    fi
else
    print_warning "Prometheus container not found"
fi

# Check Grafana health
if docker ps | grep -q "grafana"; then
    if curl -s http://localhost:3001/api/health &> /dev/null; then
        print_status "Grafana is healthy"
    else
        print_warning "Grafana health check failed (may not be running yet)"
    fi
else
    print_warning "Grafana container not found"
fi
echo

# Step 7: Deployment summary
echo -e "${BLUE}Step 7: Deployment Summary${NC}"
echo
echo "✓ Configuration verified"
echo "✓ Alert rules configured ($SCRAPE_COUNT services)"
echo "✓ Grafana dashboard provisioned"
echo "✓ Datasources configured"
echo

# Step 8: Next steps
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}Phase 4.2 Docker Deployment Ready${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
echo
echo "Next steps for $ENVIRONMENT deployment:"
echo
echo "1. Start/restart Docker Compose services:"
echo "   docker-compose down"
echo "   docker-compose up -d"
echo
echo "2. Verify services are running:"
echo "   docker-compose ps"
echo
echo "3. Wait for Prometheus to start (30 seconds):"
echo "   sleep 30"
echo
echo "4. Check Prometheus targets:"
echo "   open http://localhost:9090/targets"
echo
echo "5. Check Grafana dashboard:"
echo "   open http://localhost:3001"
echo "   Login: admin / admin"
echo "   Navigate to: Dashboards → HDIM - Authentication & Security Metrics"
echo
echo "6. Generate test traffic to verify metrics collection:"
echo "   for i in {1..50}; do"
echo "     curl -X GET http://localhost:8102/actuator/health \\\\  "
echo "       -H 'X-Auth-User-Id: test-user' \\\\  "
echo "       -H 'X-Auth-Tenant-Ids: TENANT001' \\\\  "
echo "       -H 'X-Auth-Roles: ADMIN'"
echo "     sleep 0.1"
echo "   done"
echo
echo "7. Monitor metrics in Prometheus:"
echo "   open http://localhost:9090/graph"
echo "   Query: rate(auth_success_total[5m])"
echo
echo "Documentation:"
echo "  - Monitoring Guide: docs/PHASE4_MONITORING_GUIDE.md"
echo "  - Docker Deployment: docs/PHASE4_2_DOCKER_DEPLOYMENT.md"
echo "  - Alert Rules: docker/prometheus/rules/security-alerts.yml"
echo
