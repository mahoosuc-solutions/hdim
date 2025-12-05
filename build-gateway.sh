#!/bin/bash
# Build and deploy Gateway Service to Docker

set -e

echo "🔨 Building Gateway Service for Docker..."
echo "=========================================="

# Navigate to backend directory
cd "$(dirname "$0")/backend"

# Clean and build the Gateway service
echo ""
echo "📦 Step 1: Building Gateway JAR..."
./gradlew :modules:services:gateway-service:clean :modules:services:gateway-service:build -x test

# Check if build was successful
if [ ! -f "modules/services/gateway-service/build/libs/gateway-service.jar" ]; then
    echo "❌ Build failed - JAR not found"
    exit 1
fi

echo "✅ Gateway JAR built successfully"
echo ""

# Build Docker image
echo "🐳 Step 2: Building Docker image..."
cd modules/services/gateway-service
docker build -t healthdata/gateway-service:latest .

if [ $? -eq 0 ]; then
    echo "✅ Docker image built successfully"
else
    echo "❌ Docker build failed"
    exit 1
fi

echo ""
echo "📊 Step 3: Docker image info..."
docker images healthdata/gateway-service:latest

cd ../../..
echo ""
echo "=========================================="
echo "✅ Build complete!"
echo ""
echo "Next steps:"
echo "  1. Stop running Gateway: pkill -f gateway-service:bootRun"
echo "  2. Start with Docker Compose: docker compose up -d gateway-service"
echo "  3. View logs: docker compose logs -f gateway-service"
echo "  4. Test: curl http://localhost:9000/actuator/health"
echo ""
