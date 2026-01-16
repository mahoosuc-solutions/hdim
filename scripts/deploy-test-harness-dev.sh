#!/bin/bash

# Deploy Test Harness - Development Mode
# This script starts the Angular development server for the testing dashboard

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HDIM Test Harness - Development Server${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

cd "$PROJECT_ROOT"

# Check if server is already running
if lsof -Pi :4200 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Port 4200 is already in use${NC}"
    echo "Stopping existing server..."
    pkill -f "nx serve clinical-portal" || true
    sleep 2
fi

echo -e "${GREEN}Starting Angular development server...${NC}"
echo ""
echo "The testing dashboard will be available at:"
echo -e "${BLUE}  http://localhost:4200/testing${NC}"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

# Start the development server
npm run nx -- serve clinical-portal --port=4200
