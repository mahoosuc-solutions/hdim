#!/bin/bash

# Build and Deploy Test Harness
# This script builds the Angular testing dashboard and prepares the test harness for deployment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}HDIM Test Harness Build & Deploy${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Step 1: Build Angular Testing Dashboard
echo -e "${YELLOW}[1/4] Building Angular Testing Dashboard...${NC}"
cd "$PROJECT_ROOT"

if ! command -v npm &> /dev/null; then
    echo -e "${RED}✗ npm is not installed${NC}"
    exit 1
fi

if ! command -v npx &> /dev/null; then
    echo -e "${RED}✗ npx is not installed${NC}"
    exit 1
fi

echo "Building clinical-portal with testing dashboard..."
if npm run nx -- build clinical-portal --configuration=production; then
    echo -e "${GREEN}✓ Angular application built successfully${NC}"
    echo "  Output: dist/apps/clinical-portal/browser"
else
    echo -e "${RED}✗ Angular build failed${NC}"
    exit 1
fi

# Step 2: Build Test Harness Validation Scripts
echo ""
echo -e "${YELLOW}[2/4] Building Test Harness Validation Scripts...${NC}"
cd "$PROJECT_ROOT/test-harness/validation"

if [ ! -f "package.json" ]; then
    echo -e "${RED}✗ test-harness/validation/package.json not found${NC}"
    exit 1
fi

echo "Installing dependencies..."
if npm install --silent; then
    echo -e "${GREEN}✓ Dependencies installed${NC}"
else
    echo -e "${RED}✗ Failed to install dependencies${NC}"
    exit 1
fi

echo "Compiling TypeScript..."
if npx tsc --noEmit; then
    echo -e "${GREEN}✓ TypeScript compilation successful${NC}"
else
    echo -e "${YELLOW}⚠ TypeScript compilation warnings (continuing...)${NC}"
fi

# Step 3: Create Deployment Package
echo ""
echo -e "${YELLOW}[3/4] Creating Deployment Package...${NC}"
cd "$PROJECT_ROOT"

DEPLOY_DIR="dist/test-harness"
mkdir -p "$DEPLOY_DIR"

# Copy Angular build
echo "Copying Angular build..."
cp -r dist/apps/clinical-portal/browser "$DEPLOY_DIR/clinical-portal" || {
    echo -e "${RED}✗ Failed to copy Angular build${NC}"
    exit 1
}

# Copy test harness scripts
echo "Copying test harness scripts..."
mkdir -p "$DEPLOY_DIR/validation"
cp -r test-harness/validation/* "$DEPLOY_DIR/validation/" 2>/dev/null || true
cp test-harness/validation/package.json "$DEPLOY_DIR/validation/" 2>/dev/null || true
cp test-harness/validation/tsconfig.json "$DEPLOY_DIR/validation/" 2>/dev/null || true
cp test-harness/validation/run-validation.sh "$DEPLOY_DIR/validation/" 2>/dev/null || true
chmod +x "$DEPLOY_DIR/validation/run-validation.sh" 2>/dev/null || true

# Create deployment README
cat > "$DEPLOY_DIR/README.md" << 'EOF'
# HDIM Test Harness Deployment Package

## Contents

- `clinical-portal/` - Built Angular application with testing dashboard
- `validation/` - Test harness validation scripts

## Quick Start

### Serve Angular Application

```bash
# Using nginx (recommended)
docker run -d -p 8080:80 \
  -v $(pwd)/clinical-portal:/usr/share/nginx/html:ro \
  nginx:alpine

# Or using Node.js http-server
npx http-server clinical-portal -p 8080
```

### Run Validation Tests

```bash
cd validation
npm install
./run-validation.sh --tier smoke
```

## Access Testing Dashboard

Once the Angular app is running, access the testing dashboard at:
- URL: `http://localhost:8080/testing`
- Requires: DEVELOPER or ADMIN role

## Deployment Options

### Option 1: Docker Deployment
See `docker-compose.test-harness.yml` for containerized deployment.

### Option 2: Static Hosting
Upload `clinical-portal/` to any static hosting service:
- AWS S3 + CloudFront
- Azure Static Web Apps
- Google Cloud Storage
- Netlify
- Vercel

### Option 3: Nginx Server
Deploy to an Nginx server:
```bash
sudo cp -r clinical-portal/* /var/www/html/
sudo systemctl reload nginx
```

## Validation Scripts

The validation scripts can be run independently:
- Smoke tests: Quick health checks
- Full validation: Comprehensive testing
- See `validation/README.md` for details
EOF

echo -e "${GREEN}✓ Deployment package created${NC}"
echo "  Location: $DEPLOY_DIR"

# Step 4: Create Docker Compose for Test Harness
echo ""
echo -e "${YELLOW}[4/4] Creating Docker Deployment Configuration...${NC}"

cat > "$PROJECT_ROOT/docker-compose.test-harness.yml" << 'EOF'
version: '3.8'

services:
  test-harness-web:
    image: nginx:alpine
    container_name: hdim-test-harness-web
    ports:
      - "8080:80"
    volumes:
      - ./dist/test-harness/clinical-portal:/usr/share/nginx/html:ro
      - ./docker/nginx/test-harness.conf:/etc/nginx/conf.d/default.conf:ro
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost/"]
      interval: 30s
      timeout: 3s
      retries: 3
    networks:
      - test-harness

networks:
  test-harness:
    driver: bridge
EOF

mkdir -p "$PROJECT_ROOT/docker/nginx"
cat > "$PROJECT_ROOT/docker/nginx/test-harness.conf" << 'EOF'
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/x-javascript application/xml+rss application/json application/javascript;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # SPA routing - all routes go to index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Health check endpoint
    location /health {
        access_log off;
        return 200 "healthy\n";
        add_header Content-Type text/plain;
    }
}
EOF

echo -e "${GREEN}✓ Docker configuration created${NC}"
echo "  File: docker-compose.test-harness.yml"

# Summary
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Build Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Deployment package location: $DEPLOY_DIR"
echo ""
echo "Next steps:"
echo "  1. Review deployment package: ls -la $DEPLOY_DIR"
echo "  2. Deploy with Docker: docker-compose -f docker-compose.test-harness.yml up -d"
echo "  3. Or deploy manually: See $DEPLOY_DIR/README.md"
echo ""
echo -e "${BLUE}Testing Dashboard will be available at:${NC}"
echo "  http://localhost:8080/testing"
echo ""
