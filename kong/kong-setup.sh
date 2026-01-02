#!/bin/bash
set -e

#
# Kong API Gateway Setup Script for HealthData In Motion HIE Platform
#
# This script configures Kong with:
# - Service definitions for CQL Engine and Quality Measure services
# - Routes for API endpoints
# - OIDC authentication plugin
# - Rate limiting
# - CORS configuration
# - Request transformation
#

KONG_ADMIN_URL="${KONG_ADMIN_URL:-http://localhost:8001}"

echo "=========================================="
echo "Kong API Gateway Setup"
echo "=========================================="
echo ""

# Check if Kong is running
echo "Checking Kong connectivity..."
if ! curl -sf "${KONG_ADMIN_URL}" > /dev/null; then
    echo "ERROR: Cannot connect to Kong Admin API at ${KONG_ADMIN_URL}"
    echo "Please ensure Kong is running: docker compose -f kong/docker-compose-kong.yml up -d"
    exit 1
fi
echo "✓ Kong is running"
echo ""

#
# 1. Create Services
#
echo "Step 1: Creating Services..."

# CQL Engine Service
echo "  - Creating CQL Engine Service..."
curl -sf -X POST "${KONG_ADMIN_URL}/services" \
  -d "name=cql-engine-service" \
  -d "url=http://healthdata-cql-engine:8081" \
  -d "protocol=http" \
  -d "connect_timeout=60000" \
  -d "write_timeout=60000" \
  -d "read_timeout=60000" \
  -d "retries=3" > /dev/null || echo "    (Service may already exist)"

# Quality Measure Service
echo "  - Creating Quality Measure Service..."
curl -sf -X POST "${KONG_ADMIN_URL}/services" \
  -d "name=quality-measure-service" \
  -d "url=http://healthdata-quality-measure:8087" \
  -d "protocol=http" \
  -d "connect_timeout=60000" \
  -d "write_timeout=60000" \
  -d "read_timeout=60000" \
  -d "retries=3" > /dev/null || echo "    (Service may already exist)"

# FHIR Service
echo "  - Creating FHIR Service..."
curl -sf -X POST "${KONG_ADMIN_URL}/services" \
  -d "name=fhir-service" \
  -d "url=http://healthdata-fhir-mock:8080" \
  -d "protocol=http" \
  -d "connect_timeout=30000" \
  -d "write_timeout=30000" \
  -d "read_timeout=30000" \
  -d "retries=3" > /dev/null || echo "    (Service may already exist)"

# Clinical Portal Frontend
echo "  - Creating Clinical Portal Frontend Service..."
curl -sf -X POST "${KONG_ADMIN_URL}/services" \
  -d "name=clinical-portal" \
  -d "url=http://healthdata-clinical-portal:80" \
  -d "protocol=http" \
  -d "connect_timeout=5000" \
  -d "write_timeout=5000" \
  -d "read_timeout=5000" \
  -d "retries=3" > /dev/null || echo "    (Service may already exist)"

echo "✓ Services created"
echo ""

#
# 2. Create Routes
#
echo "Step 2: Creating Routes..."

# CQL Engine Routes
echo "  - Creating CQL Engine routes..."
curl -sf -X POST "${KONG_ADMIN_URL}/services/cql-engine-service/routes" \
  -d "name=cql-engine-api" \
  -d "paths[]=/api/cql" \
  -d "strip_path=true" \
  -d "preserve_host=false" \
  -d "request_buffering=true" \
  -d "response_buffering=true" > /dev/null || echo "    (Route may already exist)"

# Quality Measure Routes
echo "  - Creating Quality Measure routes..."
curl -sf -X POST "${KONG_ADMIN_URL}/services/quality-measure-service/routes" \
  -d "name=quality-measure-api" \
  -d "paths[]=/api/quality" \
  -d "strip_path=true" \
  -d "preserve_host=false" \
  -d "request_buffering=true" \
  -d "response_buffering=true" > /dev/null || echo "    (Route may already exist)"

# FHIR Routes
echo "  - Creating FHIR routes..."
curl -sf -X POST "${KONG_ADMIN_URL}/services/fhir-service/routes" \
  -d "name=fhir-api" \
  -d "paths[]=/api/fhir" \
  -d "strip_path=true" \
  -d "preserve_host=false" \
  -d "request_buffering=true" \
  -d "response_buffering=true" > /dev/null || echo "    (Route may already exist)"

# Clinical Portal Frontend Routes
echo "  - Creating Clinical Portal Frontend routes..."
# Root path for the Angular SPA
curl -sf -X POST "${KONG_ADMIN_URL}/services/clinical-portal/routes" \
  -d "name=clinical-portal-root" \
  -d "paths[]=/" \
  -d "strip_path=false" \
  -d "preserve_host=true" \
  -d "request_buffering=false" \
  -d "response_buffering=false" > /dev/null || echo "    (Route may already exist)"

echo "✓ Routes created"
echo ""

#
# 3. Configure CORS Plugin
#
echo "Step 3: Configuring CORS..."

curl -sf -X POST "${KONG_ADMIN_URL}/plugins" \
  -d "name=cors" \
  -d "config.origins=http://localhost:4200" \
  -d "config.origins=http://localhost:4201" \
  -d "config.origins=http://localhost:4202" \
  -d "config.methods=GET" \
  -d "config.methods=POST" \
  -d "config.methods=PUT" \
  -d "config.methods=PATCH" \
  -d "config.methods=DELETE" \
  -d "config.methods=OPTIONS" \
  -d "config.headers=Accept" \
  -d "config.headers=Accept-Version" \
  -d "config.headers=Content-Length" \
  -d "config.headers=Content-MD5" \
  -d "config.headers=Content-Type" \
  -d "config.headers=Date" \
  -d "config.headers=X-Auth-Token" \
  -d "config.headers=X-Tenant-ID" \
  -d "config.headers=Authorization" \
  -d "config.exposed_headers=X-Auth-Token" \
  -d "config.credentials=true" \
  -d "config.max_age=3600" > /dev/null || echo "  (CORS plugin may already exist)"

echo "✓ CORS configured"
echo ""

#
# 4. Configure Rate Limiting
#
echo "Step 4: Configuring Rate Limiting..."

# Global rate limiting
curl -sf -X POST "${KONG_ADMIN_URL}/plugins" \
  -d "name=rate-limiting" \
  -d "config.second=100" \
  -d "config.minute=1000" \
  -d "config.hour=10000" \
  -d "config.policy=local" \
  -d "config.fault_tolerant=true" \
  -d "config.hide_client_headers=false" > /dev/null || echo "  (Rate limiting plugin may already exist)"

echo "✓ Rate limiting configured"
echo ""

#
# 5. Configure Request/Response Logging
#
echo "Step 5: Configuring Logging..."

curl -sf -X POST "${KONG_ADMIN_URL}/plugins" \
  -d "name=file-log" \
  -d "config.path=/tmp/kong-access.log" \
  -d "config.reopen=true" > /dev/null || echo "  (Logging plugin may already exist)"

echo "✓ Logging configured"
echo ""

#
# 6. Configure Security Headers
#
echo "Step 6: Configuring Security Headers..."

curl -sf -X POST "${KONG_ADMIN_URL}/plugins" \
  -d "name=response-transformer" \
  -d "config.add.headers[]=X-Frame-Options:DENY" \
  -d "config.add.headers[]=X-Content-Type-Options:nosniff" \
  -d "config.add.headers[]=X-XSS-Protection:1; mode=block" \
  -d "config.add.headers[]=Strict-Transport-Security:max-age=31536000; includeSubDomains" \
  -d "config.add.headers[]=Content-Security-Policy:default-src 'self'" > /dev/null || echo "  (Security headers plugin may already exist)"

echo "✓ Security headers configured"
echo ""

#
# 7. Configure IP Restriction (Optional - for internal HIE network)
#
echo "Step 7: Configuring IP Restrictions (Optional)..."
echo "  Skipping - configure manually based on HIE network topology"
echo ""

#
# Summary
#
echo "=========================================="
echo "Kong Configuration Complete!"
echo "=========================================="
echo ""
echo "Kong Admin API:     http://localhost:8001"
echo "Kong Admin UI:      http://localhost:8002"
echo "Konga Admin UI:     http://localhost:1337"
echo ""
echo "API Endpoints (via Kong):"
echo "  Clinical Portal:  http://localhost:8000/"
echo "  CQL Engine:       http://localhost:8000/api/cql"
echo "  Quality Measure:  http://localhost:8000/api/quality"
echo "  FHIR Server:      http://localhost:8000/api/fhir"
echo ""
echo "Next Steps:"
echo "  1. Configure OIDC plugin for authentication"
echo "  2. Set up SSL/TLS certificates for HTTPS (port 8443)"
echo "  3. Configure upstream health checks"
echo "  4. Set up monitoring and alerting"
echo ""
echo "For OIDC configuration, see: kong/kong-oidc-setup.sh"
echo ""
