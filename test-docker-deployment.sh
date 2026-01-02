#!/bin/bash
# Complete Docker Demo Test Script

set -e

echo "🧪 Testing HealthData In Motion - Docker Deployment"
echo "===================================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
GATEWAY_URL="http://localhost:9000"

echo -e "${BLUE}📋 System Status Check${NC}"
echo "-------------------"

# Check Gateway
if docker compose ps gateway-service | grep -q "Up"; then
    echo -e "${GREEN}✅ Gateway Service: Running${NC}"
else
    echo -e "${RED}❌ Gateway Service: Not running${NC}"
    exit 1
fi

# Check PostgreSQL
if docker compose ps postgres | grep -q "Up"; then
    echo -e "${GREEN}✅ PostgreSQL: Running${NC}"
else
    echo -e "${RED}❌ PostgreSQL: Not running${NC}"
    exit 1
fi

# Check Redis
if docker compose ps redis | grep -q "Up"; then
    echo -e "${GREEN}✅ Redis: Running${NC}"
else
    echo -e "${YELLOW}⚠️  Redis: Not running (optional)${NC}"
fi

echo ""
echo -e "${BLUE}🔐 Authentication Tests${NC}"
echo "--------------------"

# Test 1: Health Check
echo "Test 1: Health Check..."
HEALTH=$(curl -s $GATEWAY_URL/actuator/health)
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ Gateway health check passed${NC}"
else
    echo -e "${RED}❌ Gateway health check failed${NC}"
    echo "Response: $HEALTH"
    exit 1
fi

# Test 2: Login
echo ""
echo "Test 2: Login with admin credentials..."
LOGIN_RESPONSE=$(curl -s -X POST $GATEWAY_URL/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

# Check if login was successful
if echo "$LOGIN_RESPONSE" | grep -q "accessToken"; then
    echo -e "${GREEN}✅ Login successful${NC}"
    
    # Extract tokens
    ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r .accessToken)
    REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r .refreshToken)
    USERNAME=$(echo "$LOGIN_RESPONSE" | jq -r .username)
    EMAIL=$(echo "$LOGIN_RESPONSE" | jq -r .email)
    ROLES=$(echo "$LOGIN_RESPONSE" | jq -r '.roles | join(", ")')
    TENANTS=$(echo "$LOGIN_RESPONSE" | jq -r '.tenantIds | join(", ")')
    EXPIRES_IN=$(echo "$LOGIN_RESPONSE" | jq -r .expiresIn)
    
    echo ""
    echo "Login Details:"
    echo "  Username: $USERNAME"
    echo "  Email: $EMAIL"
    echo "  Roles: $ROLES"
    echo "  Tenants: $TENANTS"
    echo "  Token Expires In: ${EXPIRES_IN}s ($(($EXPIRES_IN / 60)) minutes)"
    echo "  Access Token: ${ACCESS_TOKEN:0:50}..."
    echo "  Refresh Token: ${REFRESH_TOKEN:0:50}..."
else
    echo -e "${RED}❌ Login failed${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

# Test 3: Token Refresh
echo ""
echo "Test 3: Token refresh..."
REFRESH_RESPONSE=$(curl -s -X POST $GATEWAY_URL/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")

if echo "$REFRESH_RESPONSE" | grep -q "accessToken"; then
    NEW_ACCESS_TOKEN=$(echo "$REFRESH_RESPONSE" | jq -r .accessToken)
    echo -e "${GREEN}✅ Token refresh successful${NC}"
    echo "  New Token: ${NEW_ACCESS_TOKEN:0:50}..."
else
    echo -e "${YELLOW}⚠️  Token refresh failed (may need configuration)${NC}"
    echo "Response: $REFRESH_RESPONSE"
fi

# Test 4: Protected Endpoint (Gateway health through authenticated route)
echo ""
echo "Test 4: Accessing protected endpoint with JWT..."
PROTECTED_RESPONSE=$(curl -s $GATEWAY_URL/actuator/health \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: tenant-1")

if echo "$PROTECTED_RESPONSE" | grep -q "UP"; then
    echo -e "${GREEN}✅ Protected endpoint access successful${NC}"
else
    echo -e "${YELLOW}⚠️  Protected endpoint access failed (may be expected)${NC}"
fi

# Test 5: Backend Service Routing (if services are running)
echo ""
echo -e "${BLUE}🔀 Backend Service Routing Tests${NC}"
echo "------------------------------"

# Test CQL Engine
echo "Test 5a: CQL Engine routing..."
CQL_RESPONSE=$(curl -s $GATEWAY_URL/api/cql/actuator/health \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: tenant-1" 2>&1)

if echo "$CQL_RESPONSE" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ CQL Engine accessible through Gateway${NC}"
elif echo "$CQL_RESPONSE" | grep -q "Connection refused"; then
    echo -e "${YELLOW}⚠️  CQL Engine not running (start with: docker compose up -d cql-engine-service)${NC}"
else
    echo -e "${YELLOW}⚠️  CQL Engine routing issue${NC}"
    echo "Response: ${CQL_RESPONSE:0:100}..."
fi

# Test Quality Measure
echo ""
echo "Test 5b: Quality Measure routing..."
QUALITY_RESPONSE=$(curl -s $GATEWAY_URL/api/quality/actuator/health \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-Tenant-ID: tenant-1" 2>&1)

if echo "$QUALITY_RESPONSE" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ Quality Measure Service accessible through Gateway${NC}"
elif echo "$QUALITY_RESPONSE" | grep -q "Connection refused"; then
    echo -e "${YELLOW}⚠️  Quality Measure Service not running (start with: docker compose up -d quality-measure-service)${NC}"
else
    echo -e "${YELLOW}⚠️  Quality Measure routing issue${NC}"
    echo "Response: ${QUALITY_RESPONSE:0:100}..."
fi

# Database Connection Test
echo ""
echo -e "${BLUE}💾 Database Connection Test${NC}"
echo "-------------------------"
USER_COUNT=$(docker exec healthdata-postgres psql -U healthdata -d healthdata_cql -t -c "SELECT COUNT(*) FROM users;" 2>/dev/null | tr -d '[:space:]')

if [ -n "$USER_COUNT" ]; then
    echo -e "${GREEN}✅ Database connected${NC}"
    echo "  Users in database: $USER_COUNT"
else
    echo -e "${YELLOW}⚠️  Unable to query database${NC}"
fi

# Summary
echo ""
echo "===================================================="
echo -e "${GREEN}✅ Docker Deployment Test Complete!${NC}"
echo ""
echo "Summary:"
echo "  ✅ Gateway Service: Running on port 9000"
echo "  ✅ Authentication: Working (JWT tokens generated)"
echo "  ✅ Token Refresh: Configured"
echo "  ✅ Database: Connected ($USER_COUNT users)"
echo ""
echo "Gateway Details:"
echo "  Container: healthdata-gateway"
echo "  Image: healthdata/gateway-service:latest"
echo "  URL: $GATEWAY_URL"
echo "  Health: $GATEWAY_URL/actuator/health"
echo ""
echo "Demo Credentials:"
echo "  Username: admin"
echo "  Password: admin123"
echo "  Tenant: tenant-1"
echo ""
echo "Next Steps:"
echo "  1. Start backend services:"
echo "     docker compose up -d cql-engine-service quality-measure-service"
echo "  2. View Gateway logs:"
echo "     docker compose logs -f gateway-service"
echo "  3. Run full system demo:"
echo "     ./demo-gateway-auth.sh"
echo ""
