#!/bin/bash
set -e

# OCR Service Deployment Script
# Purpose: Automate deployment of documentation-service with OCR to Docker
# Usage: ./scripts/deploy-ocr-service.sh [--skip-deps] [--no-cache] [--verify]

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SKIP_DEPS=false
NO_CACHE=false
RUN_VERIFICATION=false

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-deps)
            SKIP_DEPS=true
            shift
            ;;
        --no-cache)
            NO_CACHE=true
            shift
            ;;
        --verify)
            RUN_VERIFICATION=true
            shift
            ;;
        --help|-h)
            echo "OCR Service Deployment Script"
            echo ""
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --skip-deps   Skip dependency pre-caching (faster, but may cause build issues)"
            echo "  --no-cache    Rebuild Docker image without cache (slower, but ensures fresh build)"
            echo "  --verify      Run verification tests after deployment"
            echo "  --help, -h    Show this help message"
            echo ""
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}======================================${NC}"
echo -e "${BLUE}  OCR Service Deployment to Docker   ${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""

# Step 1: Pre-flight checks
echo -e "${YELLOW}Step 1/5: Pre-flight checks...${NC}"

# Check Docker is running
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Docker is not running${NC}"
    echo "Please start Docker Desktop and try again"
    exit 1
fi
echo -e "${GREEN}✓${NC} Docker is running"

# Check Java version
if ! java -version 2>&1 | grep -q "version \"21"; then
    echo -e "${RED}ERROR: Java 21 is required${NC}"
    echo "Current Java version:"
    java -version
    exit 1
fi
echo -e "${GREEN}✓${NC} Java 21 detected"

# Check we're in the right directory
if [ ! -f "backend/settings.gradle.kts" ]; then
    echo -e "${RED}ERROR: Not in HDIM project root directory${NC}"
    echo "Please run from: /mnt/wdblack/dev/projects/hdim-master"
    exit 1
fi
echo -e "${GREEN}✓${NC} In correct directory"

# Check PostgreSQL is running
if ! docker ps | grep -q "hdim-postgres"; then
    echo -e "${YELLOW}WARNING: PostgreSQL container not running${NC}"
    echo "Starting PostgreSQL..."
    docker compose up -d postgres
    sleep 5
fi
echo -e "${GREEN}✓${NC} PostgreSQL is running"

echo ""

# Step 2: Pre-cache dependencies (optional)
if [ "$SKIP_DEPS" = false ]; then
    echo -e "${YELLOW}Step 2/5: Pre-caching dependencies...${NC}"
    echo "This may take 5-10 minutes on first run..."
    echo "Using Gradle build to download and cache dependencies..."

    cd backend
    # Use 'dependencies' task to resolve and download all dependencies
    # This caches them for subsequent builds
    ./gradlew :modules:services:documentation-service:dependencies --no-daemon --quiet

    if [ $? -ne 0 ]; then
        echo -e "${YELLOW}WARNING: Dependency resolution had issues, but continuing...${NC}"
        echo "Dependencies will be downloaded during JAR build"
    else
        echo -e "${GREEN}✓${NC} Dependencies cached successfully"
    fi

    cd ..
else
    echo -e "${YELLOW}Step 2/5: Skipping dependency cache (--skip-deps)${NC}"
fi

echo ""

# Step 3: Build service JAR
echo -e "${YELLOW}Step 3/5: Building service JAR...${NC}"
echo "This may take 2-3 minutes..."

cd backend
./gradlew :modules:services:documentation-service:bootJar -x test --no-daemon

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Failed to build JAR${NC}"
    exit 1
fi

# Verify JAR exists
JAR_FILE="modules/services/documentation-service/build/libs/documentation-service-1.0.0-SNAPSHOT.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}ERROR: JAR file not found: $JAR_FILE${NC}"
    exit 1
fi

JAR_SIZE=$(du -h "$JAR_FILE" | cut -f1)
echo -e "${GREEN}✓${NC} JAR built successfully ($JAR_SIZE)"
cd ..

echo ""

# Step 4: Build Docker image
echo -e "${YELLOW}Step 4/5: Building Docker image...${NC}"
echo "This may take 3-5 minutes..."

BUILD_ARGS=""
if [ "$NO_CACHE" = true ]; then
    BUILD_ARGS="--no-cache"
    echo "Building without cache (slower, but ensures fresh build)..."
fi

docker compose build $BUILD_ARGS documentation-service

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Failed to build Docker image${NC}"
    exit 1
fi

# Verify image was created
if ! docker images | grep -q "documentation-service"; then
    echo -e "${RED}ERROR: Docker image not found after build${NC}"
    exit 1
fi

IMAGE_SIZE=$(docker images hdim-master-documentation-service:latest --format "{{.Size}}")
echo -e "${GREEN}✓${NC} Docker image built successfully ($IMAGE_SIZE)"

# Verify Tesseract installation
echo -n "Verifying Tesseract OCR... "
TESSERACT_VERSION=$(docker run --rm hdim-master-documentation-service tesseract --version 2>&1 | head -1)
if [[ "$TESSERACT_VERSION" == *"tesseract"* ]]; then
    echo -e "${GREEN}✓${NC} $TESSERACT_VERSION"
else
    echo -e "${RED}ERROR: Tesseract not found in image${NC}"
    exit 1
fi

echo ""

# Step 5: Start service
echo -e "${YELLOW}Step 5/5: Starting documentation-service...${NC}"

# Stop existing service if running
if docker compose ps documentation-service | grep -q "Up"; then
    echo "Stopping existing service..."
    docker compose down documentation-service
fi

# Start service
docker compose up -d documentation-service

if [ $? -ne 0 ]; then
    echo -e "${RED}ERROR: Failed to start service${NC}"
    exit 1
fi

# Wait for startup (max 60 seconds)
echo -n "Waiting for service startup"
for i in {1..30}; do
    if curl -s http://localhost:8091/actuator/health > /dev/null 2>&1; then
        echo ""
        echo -e "${GREEN}✓${NC} Service started successfully"
        break
    fi
    echo -n "."
    sleep 2
done

# Verify service is running
if ! curl -s http://localhost:8091/actuator/health | grep -q "UP"; then
    echo -e "${RED}ERROR: Service failed to start${NC}"
    echo ""
    echo "Recent logs:"
    docker compose logs --tail=50 documentation-service
    exit 1
fi

echo ""
echo -e "${BLUE}======================================${NC}"
echo -e "${GREEN}  Deployment Successful!${NC}"
echo -e "${BLUE}======================================${NC}"
echo ""
echo "Service Details:"
echo "  • Health:  http://localhost:8091/actuator/health"
echo "  • API:     http://localhost:8091/api/documents/clinical"
echo "  • Metrics: http://localhost:8091/actuator/metrics"
echo ""
echo "View logs:"
echo "  docker compose logs -f documentation-service"
echo ""

# Optional verification
if [ "$RUN_VERIFICATION" = true ]; then
    echo -e "${YELLOW}Running verification tests...${NC}"
    echo ""

    if [ -f "scripts/verify-ocr-deployment.sh" ]; then
        chmod +x scripts/verify-ocr-deployment.sh
        ./scripts/verify-ocr-deployment.sh
    else
        echo -e "${YELLOW}WARNING: Verification script not found${NC}"
        echo "Skipping automated verification"
    fi
fi

echo ""
echo -e "${GREEN}Next steps:${NC}"
echo "  1. View deployment plan: docs/OCR_DOCKER_DEPLOYMENT_PLAN.md"
echo "  2. Run smoke tests: See Phase 5 in deployment plan"
echo "  3. Monitor logs: docker compose logs -f documentation-service"
echo "  4. Test OCR: Upload a PDF document and verify text extraction"
echo ""
