#!/bin/bash
# Wait for Docker to be ready, then execute complete demo screenshot workflow

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "=========================================="
echo "HDIM Demo Screenshot Capture"
echo "Waiting for Docker..."
echo "=========================================="
echo ""

# Wait for Docker to be accessible
MAX_WAIT=120  # 2 minutes
WAIT_INTERVAL=5
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
    if docker ps > /dev/null 2>&1; then
        echo "✅ Docker is accessible!"
        echo ""
        break
    else
        echo "⏳ Waiting for Docker... (${ELAPSED}s / ${MAX_WAIT}s)"
        sleep $WAIT_INTERVAL
        ELAPSED=$((ELAPSED + WAIT_INTERVAL))
    fi
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
    echo "❌ Docker did not become accessible within ${MAX_WAIT} seconds"
    echo "Please ensure Docker is running and try again"
    exit 1
fi

# Run pre-flight check
echo "Step 1: Running pre-flight check..."
echo ""
./scripts/pre-flight-check.sh

if [ $? -ne 0 ]; then
    echo ""
    echo "⚠️  Pre-flight check had issues, but continuing..."
    echo ""
fi

# Start all services
echo ""
echo "Step 2: Starting all services..."
echo ""
docker compose -f docker-compose.demo.yml up -d

echo ""
echo "Waiting for services to initialize (120 seconds)..."
sleep 120

# Validate environment
echo ""
echo "Step 3: Validating environment..."
echo ""
node scripts/validate-demo-environment.js

if [ $? -ne 0 ]; then
    echo ""
    echo "⚠️  Environment validation had issues, but continuing..."
    echo ""
fi

# Capture screenshots
echo ""
echo "Step 4: Capturing screenshots..."
echo ""
node scripts/capture-screenshots.js

if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ Screenshot capture complete!"
    echo "=========================================="
    echo ""
    echo "Screenshots saved to: docs/screenshots/"
    echo "Index file: docs/screenshots/INDEX.md"
    echo ""
    echo "To view screenshots:"
    echo "  cd docs/screenshots"
    echo "  ls -la"
    echo ""
else
    echo ""
    echo "❌ Screenshot capture failed"
    exit 1
fi
