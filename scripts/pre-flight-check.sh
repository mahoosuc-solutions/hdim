#!/bin/bash
# Pre-flight check before running demo screenshot capture
# Verifies all prerequisites are met

set -e

echo "=========================================="
echo "HDIM Demo Pre-Flight Check"
echo "=========================================="
echo ""

ERRORS=0

# Check Docker
echo "Checking Docker..."
if command -v docker &> /dev/null; then
    if docker ps &> /dev/null; then
        echo "  ✓ Docker is installed and accessible"
        DOCKER_VERSION=$(docker --version)
        echo "    Version: $DOCKER_VERSION"
    else
        echo "  ✗ Docker daemon is not running"
        echo "    Please start Docker:"
        echo "      - On Linux: sudo systemctl start docker"
        echo "      - On WSL: sudo service docker start"
        echo "      - On Mac: Open Docker Desktop"
        ERRORS=$((ERRORS + 1))
    fi
else
    echo "  ✗ Docker is not installed"
    ERRORS=$((ERRORS + 1))
fi

# Check Docker Compose
echo ""
echo "Checking Docker Compose..."
if command -v docker-compose &> /dev/null || docker compose version &> /dev/null; then
    echo "  ✓ Docker Compose is available"
else
    echo "  ✗ Docker Compose is not available"
    ERRORS=$((ERRORS + 1))
fi

# Check Node.js
echo ""
echo "Checking Node.js..."
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo "  ✓ Node.js is installed"
    echo "    Version: $NODE_VERSION"
else
    echo "  ✗ Node.js is not installed"
    ERRORS=$((ERRORS + 1))
fi

# Check npm
echo ""
echo "Checking npm..."
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo "  ✓ npm is installed"
    echo "    Version: $NPM_VERSION"
else
    echo "  ✗ npm is not installed"
    ERRORS=$((ERRORS + 1))
fi

# Check Playwright
echo ""
echo "Checking Playwright..."
if [ -d "node_modules/playwright" ] || npm list playwright &> /dev/null 2>&1; then
    echo "  ✓ Playwright is installed"
else
    echo "  ⚠ Playwright may not be installed"
    echo "    Run: npm install playwright && npx playwright install chromium"
fi

# Check required files
echo ""
echo "Checking required files..."
REQUIRED_FILES=(
    "docker-compose.demo.yml"
    "scripts/validate-demo-environment.js"
    "scripts/capture-screenshots.js"
    "scripts/run-demo-screenshots.sh"
)

for file in "${REQUIRED_FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✓ $file exists"
    else
        echo "  ✗ $file is missing"
        ERRORS=$((ERRORS + 1))
    fi
done

# Check port availability
echo ""
echo "Checking port availability..."
PORTS=(5435 6380 9094 16686 4200 18080 8080 8081 8083 8084 8085 8086 8087 8098)
PORT_CONFLICTS=0

for port in "${PORTS[@]}"; do
    if command -v netstat &> /dev/null; then
        if netstat -tuln 2>/dev/null | grep -q ":$port "; then
            echo "  ⚠ Port $port is in use"
            PORT_CONFLICTS=$((PORT_CONFLICTS + 1))
        fi
    elif command -v ss &> /dev/null; then
        if ss -tuln 2>/dev/null | grep -q ":$port "; then
            echo "  ⚠ Port $port is in use"
            PORT_CONFLICTS=$((PORT_CONFLICTS + 1))
        fi
    fi
done

if [ $PORT_CONFLICTS -eq 0 ]; then
    echo "  ✓ All required ports appear available"
fi

# Summary
echo ""
echo "=========================================="
if [ $ERRORS -eq 0 ]; then
    echo "✅ Pre-flight check passed!"
    echo "   Ready to run: ./scripts/run-demo-screenshots.sh"
    exit 0
else
    echo "❌ Pre-flight check failed with $ERRORS error(s)"
    echo "   Please fix the issues above before proceeding"
    exit 1
fi
