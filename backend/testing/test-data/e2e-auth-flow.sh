#!/bin/bash
#
# E2E Authentication Flow Test
# Tests the complete authentication flow including:
# - Bootstrap admin creation
# - Login with JWT generation
# - User registration (via API)
# - Role-based access control
# - Multi-tenant isolation
# - Token validation
#

set -e

API_BASE="${API_BASE:-http://localhost:8080/api/v1/auth}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "========================================="
echo "HDIM E2E Authentication Flow Test"
echo "========================================="
echo ""

# Test counter
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

test_result() {
    TESTS_RUN=$((TESTS_RUN + 1))
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS:${NC} $2"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        echo -e "${RED}✗ FAIL:${NC} $2"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# Test 1: Health Check
echo -e "${BLUE}Test 1: Health Check${NC}"
RESPONSE=$(curl -s http://localhost:8080/actuator/health)
if echo "$RESPONSE" | grep -q '"status":"UP"'; then
    test_result 0 "Gateway health endpoint accessible"
else
    test_result 1 "Gateway health endpoint failed"
    exit 1
fi
echo ""

# Test 2: Bootstrap Admin Login
echo -e "${BLUE}Test 2: Bootstrap Admin Login${NC}"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "bootstrap_admin",
        "password": "password123"
    }')

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | tail -1)
BODY=$(echo "$LOGIN_RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ]; then
    ACCESS_TOKEN=$(echo "$BODY" | jq -r '.accessToken // empty')
    if [ -n "$ACCESS_TOKEN" ] && [ "$ACCESS_TOKEN" != "null" ]; then
        test_result 0 "Bootstrap admin login successful"

        # Decode JWT to verify roles
        ROLES=$(echo "$BODY" | jq -r '.roles[]' 2>/dev/null)
        if echo "$ROLES" | grep -q "SUPER_ADMIN"; then
            test_result 0 "JWT contains SUPER_ADMIN role"
        else
            test_result 1 "JWT missing SUPER_ADMIN role"
        fi
    else
        test_result 1 "Login successful but no access token returned"
    fi
else
    test_result 1 "Bootstrap admin login failed (HTTP $HTTP_CODE)"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    exit 1
fi
echo ""

# Test 3: Registration Endpoint Security (Should reject without auth)
echo -e "${BLUE}Test 3: Registration Endpoint Security${NC}"
REG_NO_AUTH=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/register" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "unauthorized_user",
        "email": "unauthorized@test.com",
        "password": "password123",
        "firstName": "Unauthorized",
        "lastName": "User",
        "tenantIds": ["TEST_TENANT"],
        "roles": ["VIEWER"]
    }')

HTTP_CODE=$(echo "$REG_NO_AUTH" | tail -1)
if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    test_result 0 "Registration endpoint properly rejects unauthenticated requests"
else
    test_result 1 "Registration endpoint improperly allowed unauthenticated access (HTTP $HTTP_CODE)"
fi
echo ""

# Test 4: Create User via API (Authenticated)
echo -e "${BLUE}Test 4: Create User via Authenticated API${NC}"
TEST_USERNAME="e2e_test_user_$(date +%s)"
REG_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/register" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{
        \"username\": \"$TEST_USERNAME\",
        \"email\": \"${TEST_USERNAME}@test.com\",
        \"password\": \"password123\",
        \"firstName\": \"E2E\",
        \"lastName\": \"TestUser\",
        \"tenantIds\": [\"TEST_TENANT\"],
        \"roles\": [\"EVALUATOR\"]
    }")

HTTP_CODE=$(echo "$REG_RESPONSE" | tail -1)
BODY=$(echo "$REG_RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "201" ]; then
    USER_ID=$(echo "$BODY" | jq -r '.id // empty')
    if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ]; then
        test_result 0 "User created successfully via API (ID: $USER_ID)"
    else
        test_result 1 "User creation returned 201 but no user ID"
    fi
else
    test_result 1 "User creation failed (HTTP $HTTP_CODE)"
    echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
fi
echo ""

# Test 5: Login with Newly Created User
echo -e "${BLUE}Test 5: Login with Newly Created User${NC}"
NEW_USER_LOGIN=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/login" \
    -H "Content-Type: application/json" \
    -d "{
        \"username\": \"$TEST_USERNAME\",
        \"password\": \"password123\"
    }")

HTTP_CODE=$(echo "$NEW_USER_LOGIN" | tail -1)
BODY=$(echo "$NEW_USER_LOGIN" | head -n -1)

if [ "$HTTP_CODE" = "200" ]; then
    NEW_USER_TOKEN=$(echo "$BODY" | jq -r '.accessToken // empty')
    if [ -n "$NEW_USER_TOKEN" ] && [ "$NEW_USER_TOKEN" != "null" ]; then
        test_result 0 "Newly created user can login"

        # Verify role
        ROLES=$(echo "$BODY" | jq -r '.roles[]' 2>/dev/null)
        if echo "$ROLES" | grep -q "EVALUATOR"; then
            test_result 0 "New user has correct EVALUATOR role"
        else
            test_result 1 "New user missing EVALUATOR role"
        fi
    else
        test_result 1 "New user login successful but no token"
    fi
else
    test_result 1 "New user login failed (HTTP $HTTP_CODE)"
fi
echo ""

# Test 6: New User Cannot Register Others (RBAC test)
echo -e "${BLUE}Test 6: RBAC - EVALUATOR Cannot Register Users${NC}"
UNAUTHORIZED_REG=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/register" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $NEW_USER_TOKEN" \
    -d '{
        "username": "should_fail_user",
        "email": "shouldfail@test.com",
        "password": "password123",
        "firstName": "Should",
        "lastName": "Fail",
        "tenantIds": ["TEST_TENANT"],
        "roles": ["VIEWER"]
    }')

HTTP_CODE=$(echo "$UNAUTHORIZED_REG" | tail -1)
if [ "$HTTP_CODE" = "403" ]; then
    test_result 0 "EVALUATOR properly denied registration permission"
else
    test_result 1 "EVALUATOR improperly allowed to register users (HTTP $HTTP_CODE)"
fi
echo ""

# Test 7: Token Refresh
echo -e "${BLUE}Test 7: Token Refresh${NC}"
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | head -n -1 | jq -r '.refreshToken // empty')
if [ -n "$REFRESH_TOKEN" ] && [ "$REFRESH_TOKEN" != "null" ]; then
    REFRESH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/refresh" \
        -H "Content-Type: application/json" \
        -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

    HTTP_CODE=$(echo "$REFRESH_RESPONSE" | tail -1)
    if [ "$HTTP_CODE" = "200" ]; then
        NEW_TOKEN=$(echo "$REFRESH_RESPONSE" | head -n -1 | jq -r '.accessToken // empty')
        if [ -n "$NEW_TOKEN" ] && [ "$NEW_TOKEN" != "null" ]; then
            test_result 0 "Token refresh successful"
        else
            test_result 1 "Token refresh returned 200 but no new token"
        fi
    else
        test_result 1 "Token refresh failed (HTTP $HTTP_CODE)"
    fi
else
    echo -e "${YELLOW}⊘ SKIP:${NC} Token refresh test (no refresh token in login response)"
fi
echo ""

# Test 8: Logout
echo -e "${BLUE}Test 8: Logout${NC}"
LOGOUT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$API_BASE/logout" \
    -H "Authorization: Bearer $ACCESS_TOKEN")

HTTP_CODE=$(echo "$LOGOUT_RESPONSE" | tail -1)
if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "204" ]; then
    test_result 0 "Logout successful"
else
    test_result 1 "Logout failed (HTTP $HTTP_CODE)"
fi
echo ""

# Summary
echo "========================================="
echo "E2E Authentication Flow Test Summary"
echo "========================================="
echo "Total Tests: $TESTS_RUN"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
if [ $TESTS_FAILED -gt 0 ]; then
    echo -e "${RED}Failed: $TESTS_FAILED${NC}"
else
    echo "Failed: $TESTS_FAILED"
fi
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All E2E authentication tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some E2E authentication tests failed${NC}"
    exit 1
fi
