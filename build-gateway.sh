#!/bin/bash
# Build split Gateway services to Docker

set -e

echo "🔨 Building Gateway services for Docker..."
echo "=========================================="

# Navigate to backend directory
cd "$(dirname "$0")/backend"

# Clean and build the Gateway services
echo ""
echo "📦 Step 1: Building Gateway JARs..."
GATEWAY_SERVICES=(gateway-admin-service gateway-fhir-service gateway-clinical-service)
for service in "${GATEWAY_SERVICES[@]}"; do
  ./gradlew ":modules:services:${service}:clean" ":modules:services:${service}:build" -x test
done

# Check if build was successful
missing_jars=()
for service in "${GATEWAY_SERVICES[@]}"; do
  if ! ls "modules/services/${service}/build/libs/"*.jar >/dev/null 2>&1; then
    missing_jars+=("$service")
  fi
done

if [ ${#missing_jars[@]} -gt 0 ]; then
    echo "❌ Build failed - JAR(s) not found for: ${missing_jars[*]}"
    exit 1
fi

echo "✅ Gateway JARs built successfully"
echo ""

# Build Docker images
echo "🐳 Step 2: Building Docker images..."
for service in "${GATEWAY_SERVICES[@]}"; do
  (cd "modules/services/${service}" && docker build -t "healthdata/${service}:latest" .)
done

echo ""
echo "📊 Step 3: Docker image info..."
docker images | grep gateway-.*-service:latest

cd ../../..
echo ""
echo "=========================================="
echo "✅ Build complete!"
echo ""
echo "Next steps:"
echo "  1. Stop running Gateways: pkill -f gateway-.*-service:bootRun"
echo "  2. Start with Docker Compose: docker compose up -d gateway-admin-service gateway-fhir-service gateway-clinical-service gateway-edge"
echo "  3. View logs: docker compose logs -f gateway-admin-service gateway-fhir-service gateway-clinical-service"
echo "  4. Test (edge): curl http://localhost:18080"
echo ""
