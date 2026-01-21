#!/bin/bash
# Complete Demo Startup and Screenshot Capture Script
# Validates environment, seeds data, and captures screenshots

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "=========================================="
echo "HDIM Demo Screenshot Capture"
echo "=========================================="
echo ""

# Step 1: Start all services
echo "Step 1: Starting all services..."
docker compose -f docker-compose.demo.yml up -d

echo ""
echo "Waiting for services to initialize (120 seconds)..."
sleep 120

# Step 2: Validate environment
echo ""
echo "Step 2: Validating environment..."
node scripts/validate-demo-environment.js

if [ $? -ne 0 ]; then
    echo ""
    echo "⚠️  Environment validation had issues, but continuing..."
fi

# Step 3: Capture screenshots
echo ""
echo "Step 3: Capturing screenshots..."
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
