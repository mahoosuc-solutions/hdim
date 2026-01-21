#!/bin/bash
#
# HealthData In Motion - RHEL 7 Deployment Script
#
# This script automates the complete deployment of the platform on RHEL 7
# Can be run on actual RHEL 7 or in a test container
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

function log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

function log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

function log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

function log_step() {
    echo -e "${BLUE}==>${NC} $1"
}

echo "=========================================="
echo "HealthData In Motion Platform"
echo "RHEL 7 Deployment Script"
echo "=========================================="
echo ""

#
# Pre-flight checks
#
log_step "Running pre-flight checks..."

# Check if running as root
if [ "$EUID" -eq 0 ]; then
    log_warn "Running as root. Consider using a non-root user with sudo."
fi

# Check OS version
if [ -f /etc/redhat-release ]; then
    OS_VERSION=$(cat /etc/redhat-release)
    log_info "Detected OS: $OS_VERSION"
else
    log_warn "Not running on RHEL/CentOS. Proceed with caution."
fi

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    log_error "Docker is not installed. Please install Docker first."
    echo "  See: https://docs.docker.com/engine/install/centos/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    log_error "Docker Compose is not installed. Please install Docker Compose first."
    echo "  Install: sudo curl -L 'https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)' -o /usr/local/bin/docker-compose"
    echo "  Chmod:   sudo chmod +x /usr/local/bin/docker-compose"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    log_error "Docker daemon is not running"
    echo "  Start: sudo systemctl start docker"
    echo "  Enable: sudo systemctl enable docker"
    exit 1
fi

log_info "Pre-flight checks passed"
echo ""

#
# Step 1: Set up environment
#
log_step "Step 1: Setting up environment..."

# Create necessary directories
mkdir -p logs
mkdir -p data/postgres
mkdir -p data/redis
mkdir -p data/kafka
mkdir -p kong/certs

# Set permissions
chmod 755 kong/kong-setup.sh
chmod 755 deploy-rhel7.sh

log_info "Environment setup complete"
echo ""

#
# Step 2: Build Docker images (if needed)
#
log_step "Step 2: Building Docker images..."

# For Java services, we'll use gradle to build, then docker to containerize
if [ -d "backend" ]; then
    log_info "Building Java backend services..."
    cd backend

    # Check Java version
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}')
        log_info "Java version: $JAVA_VERSION"
    else
        log_error "Java not found. Install OpenJDK 21:"
        echo "  sudo yum install java-21-openjdk java-21-openjdk-devel"
        exit 1
    fi

    # Build with Gradle
    ./gradlew clean build -x test || {
        log_warn "Build completed with some failures. Continuing..."
    }

    cd ..
    log_info "Java services built"
else
    log_warn "Backend directory not found, skipping Java build"
fi

# Build Angular application
if [ -d "apps/clinical-portal" ]; then
    log_info "Building Angular Clinical Portal..."

    # Check Node.js version
    if command -v node &> /dev/null; then
        NODE_VERSION=$(node --version)
        log_info "Node.js version: $NODE_VERSION"
    else
        log_error "Node.js not found. Install Node.js 20.x:"
        echo "  curl -fsSL https://rpm.nodesource.com/setup_20.x | sudo bash -"
        echo "  sudo yum install -y nodejs"
        exit 1
    fi

    # Install dependencies if needed
    if [ ! -d "node_modules" ]; then
        log_info "Installing Node.js dependencies (this may take a while)..."
        npm install
    fi

    # Build for production
    log_info "Building Angular app for production..."
    npx nx build clinical-portal --configuration production || {
        log_warn "Angular build had issues, will use development mode"
    }

    log_info "Angular application built"
else
    log_warn "Angular app directory not found"
fi

echo ""

#
# Step 3: Start infrastructure services
#
log_step "Step 3: Starting infrastructure services (PostgreSQL, Redis, Kafka)..."

docker-compose up -d postgres redis zookeeper kafka

log_info "Waiting for infrastructure to be ready (30 seconds)..."
sleep 30

# Check PostgreSQL
log_info "Checking PostgreSQL..."
docker-compose exec -T postgres pg_isready -U healthdata || log_warn "PostgreSQL not ready yet"

log_info "Infrastructure services started"
echo ""

#
# Step 4: Start backend microservices
#
log_step "Step 4: Starting backend microservices..."

docker-compose up -d cql-engine quality-measure fhir-mock

log_info "Waiting for backend services to start (30 seconds)..."
sleep 30

# Check service health
log_info "Checking backend service health..."
curl -sf http://localhost:8081/cql-engine/actuator/health > /dev/null && \
    log_info "  ✓ CQL Engine: healthy" || log_warn "  ✗ CQL Engine: not responding"

curl -sf http://localhost:8087/quality-measure/actuator/health > /dev/null && \
    log_info "  ✓ Quality Measure: healthy" || log_warn "  ✗ Quality Measure: not responding"

curl -sf http://localhost:8083/fhir/metadata > /dev/null && \
    log_info "  ✓ FHIR Server: healthy" || log_warn "  ✗ FHIR Server: not responding"

echo ""

#
# Step 5: Start Kong API Gateway
#
log_step "Step 5: Starting Kong API Gateway..."

docker-compose -f kong/docker-compose-kong.yml up -d

log_info "Waiting for Kong to be ready (45 seconds)..."
sleep 45

# Wait for Kong Admin API
KONG_READY=0
for i in {1..20}; do
    if curl -sf http://localhost:8001/ > /dev/null 2>&1; then
        KONG_READY=1
        log_info "Kong Admin API is ready"
        break
    fi
    log_info "  Waiting for Kong... ($i/20)"
    sleep 3
done

if [ $KONG_READY -eq 0 ]; then
    log_error "Kong failed to start. Check logs: docker logs healthdata-kong"
    exit 1
fi

# Configure Kong
log_info "Configuring Kong routes and plugins..."
./kong/kong-setup.sh

# Verify Kong routes
log_info "Verifying Kong routes..."
curl -sf -H "X-Tenant-ID: default" "http://localhost:8000/api/cql/api/v1/cql/evaluations?page=0&size=1" > /dev/null && \
    log_info "  ✓ CQL Engine via Kong: working" || log_warn "  ✗ CQL Engine via Kong: not working"

curl -sf -H "X-Tenant-ID: default" "http://localhost:8000/api/quality/results?page=0&size=1" > /dev/null && \
    log_info "  ✓ Quality Measure via Kong: working" || log_warn "  ✗ Quality Measure via Kong: not working"

curl -sf "http://localhost:8000/api/fhir/Patient?_count=1" > /dev/null && \
    log_info "  ✓ FHIR via Kong: working" || log_warn "  ✗ FHIR via Kong: not working"

echo ""

#
# Step 6: Start Angular frontend
#
log_step "Step 6: Starting Angular Clinical Portal..."

# Check if production build exists
if [ -d "dist/apps/clinical-portal" ]; then
    log_info "Serving production build on port 4200..."

    # Install http-server if not present
    if ! command -v http-server &> /dev/null; then
        npm install -g http-server
    fi

    # Serve production build in background
    nohup http-server dist/apps/clinical-portal -p 4200 --proxy http://localhost:8000 > logs/angular.log 2>&1 &
    echo $! > logs/angular.pid
else
    log_info "Starting development server on port 4200..."
    nohup npx nx serve clinical-portal --host 0.0.0.0 --port 4200 > logs/angular.log 2>&1 &
    echo $! > logs/angular.pid
fi

log_info "Waiting for Angular to start (20 seconds)..."
sleep 20

# Check if Angular is responding
if curl -sf http://localhost:4200 > /dev/null 2>&1; then
    log_info "  ✓ Angular Clinical Portal: running"
else
    log_warn "  ✗ Angular may not be ready yet. Check logs/angular.log"
fi

echo ""

#
# Step 7: Load sample data (optional)
#
if [ -f "load-demo-data.sh" ]; then
    log_step "Step 7: Loading sample data (optional)..."
    read -p "Load sample data? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ./load-demo-data.sh || log_warn "Sample data load had issues"
    else
        log_info "Skipping sample data load"
    fi
else
    log_info "No sample data loader found, skipping"
fi

echo ""

#
# Deployment complete
#
echo "=========================================="
log_info "Deployment Complete!"
echo "=========================================="
echo ""

echo "Container Status:"
echo "-----------------"
docker-compose ps | grep -v "Exit"
echo ""

echo "Access Points:"
echo "--------------"
echo "  ${GREEN}Clinical Portal:${NC}     http://localhost:4200"
echo "  ${GREEN}Kong API Gateway:${NC}    http://localhost:8000"
echo "  ${GREEN}Kong Admin API:${NC}      http://localhost:8001"
echo "  ${GREEN}Konga Admin UI:${NC}      http://localhost:1337"
echo ""

echo "API Endpoints (via Kong):"
echo "-------------------------"
echo "  CQL Engine:          http://localhost:8000/api/cql"
echo "  Quality Measure:     http://localhost:8000/api/quality"
echo "  FHIR Server:         http://localhost:8000/api/fhir"
echo ""

echo "Logs:"
echo "-----"
echo "  Angular:         tail -f logs/angular.log"
echo "  Docker Compose:  docker-compose logs -f"
echo "  Kong:            docker logs -f healthdata-kong"
echo ""

echo "Management Commands:"
echo "--------------------"
echo "  Stop all:        docker-compose down && docker-compose -f kong/docker-compose-kong.yml down"
echo "  Restart all:     docker-compose restart && docker-compose -f kong/docker-compose-kong.yml restart"
echo "  View logs:       docker-compose logs -f [service-name]"
echo ""

log_info "Platform is ready for testing!"
echo ""
echo "Next Steps:"
echo "-----------"
echo "1. Open browser: http://localhost:4200"
echo "2. Review logs: tail -f logs/angular.log"
echo "3. Test APIs via Kong: curl -H 'X-Tenant-ID: default' http://localhost:8000/api/cql/evaluations"
echo "4. Configure OIDC (optional): ./kong/kong-oidc-setup.sh"
echo ""
