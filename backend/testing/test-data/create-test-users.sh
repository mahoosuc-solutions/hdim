#!/bin/bash
##############################################################################
# Create Test Users via API
#
# This script:
# 1. Logs in as bootstrap_admin to get JWT token
# 2. Creates standard test users for each role via API
# 3. Verifies users were created successfully
#
# Prerequisites:
# - Bootstrap admin user must exist (run bootstrap-admin-user.sql first)
# - Gateway service must be running on port 8080
#
# Usage:
#   ./create-test-users.sh
##############################################################################

set -e  # Exit on error

# Configuration
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
API_BASE="${GATEWAY_URL}/api/v1/auth"

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo "ℹ $1"; }

echo "========================================="
echo "HDIM Test User Creation Script"
echo "========================================="
echo ""

# Check if gateway is running
print_info "Checking gateway service..."
if ! curl -s -f "${GATEWAY_URL}/actuator/health" > /dev/null; then
    print_error "Gateway service is not responding at ${GATEWAY_URL}"
    print_info "Please ensure the gateway service is running"
    exit 1
fi
print_success "Gateway service is running"
echo ""

# Step 1: Login as bootstrap admin
print_info "Logging in as bootstrap_admin..."
LOGIN_RESPONSE=$(curl -s -X POST "${API_BASE}/login" \
    -H "Content-Type: application/json" \
    -d '{
        "username": "bootstrap_admin",
        "password": "password123"
    }')

# Extract access token
ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken // empty')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" == "null" ]; then
    print_error "Failed to login as bootstrap_admin"
    echo "Response: $LOGIN_RESPONSE"
    print_info "Please ensure bootstrap-admin-user.sql has been executed"
    exit 1
fi

print_success "Logged in successfully"
echo ""

# Step 2: Create test users
print_info "Creating test users..."
echo ""

# Function to create a user
create_user() {
    local username=$1
    local email=$2
    local first_name=$3
    local last_name=$4
    local role=$5
    local tenant_id=${6:-"TEST_TENANT_001"}

    print_info "Creating user: $username ($role)..."

    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${API_BASE}/register" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${ACCESS_TOKEN}" \
        -d "{
            \"username\": \"$username\",
            \"email\": \"$email\",
            \"password\": \"password123\",
            \"firstName\": \"$first_name\",
            \"lastName\": \"$last_name\",
            \"tenantIds\": [\"$tenant_id\"],
            \"roles\": [\"$role\"]
        }")

    # Split response body and status code
    HTTP_BODY=$(echo "$RESPONSE" | head -n -1)
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)

    if [ "$HTTP_CODE" -eq 201 ]; then
        print_success "Created: $username"
        return 0
    elif [ "$HTTP_CODE" -eq 409 ]; then
        print_warning "Already exists: $username"
        return 0
    else
        print_error "Failed to create $username (HTTP $HTTP_CODE)"
        echo "Response: $HTTP_BODY"
        return 1
    fi
}

# Create test users for each role
create_user "test_superadmin" "superadmin@test.local" "Test" "SuperAdmin" "SUPER_ADMIN" "SYSTEM"
create_user "test_admin" "admin@test.local" "Test" "Admin" "ADMIN" "TEST_TENANT_001"
create_user "test_evaluator" "evaluator@test.local" "Test" "Evaluator" "EVALUATOR" "TEST_TENANT_001"
create_user "test_analyst" "analyst@test.local" "Test" "Analyst" "ANALYST" "TEST_TENANT_001"
create_user "test_viewer" "viewer@test.local" "Test" "Viewer" "VIEWER" "TEST_TENANT_001"

# Create additional users for different tenants
echo ""
print_info "Creating multi-tenant test users..."
create_user "test_admin_tenant2" "admin2@test.local" "Test" "Admin2" "ADMIN" "TEST_TENANT_002"
create_user "test_evaluator_tenant2" "evaluator2@test.local" "Test" "Evaluator2" "EVALUATOR" "TEST_TENANT_002"

# Create performance testing user
echo ""
print_info "Creating performance testing users..."
create_user "perf_test_user" "perftest@test.local" "Performance" "Tester" "EVALUATOR" "TEST_TENANT_001"

echo ""
echo "========================================="
echo "Test User Creation Complete!"
echo "========================================="
echo ""
echo "Created Test Users:"
echo "┌─────────────────────────┬─────────────────┬──────────────────┐"
echo "│ Username                │ Password        │ Role             │"
echo "├─────────────────────────┼─────────────────┼──────────────────┤"
echo "│ bootstrap_admin         │ password123     │ SUPER_ADMIN      │"
echo "│ test_superadmin         │ password123     │ SUPER_ADMIN      │"
echo "│ test_admin              │ password123     │ ADMIN            │"
echo "│ test_evaluator          │ password123     │ EVALUATOR        │"
echo "│ test_analyst            │ password123     │ ANALYST          │"
echo "│ test_viewer             │ password123     │ VIEWER           │"
echo "│ test_admin_tenant2      │ password123     │ ADMIN            │"
echo "│ test_evaluator_tenant2  │ password123     │ EVALUATOR        │"
echo "│ perf_test_user          │ password123     │ EVALUATOR        │"
echo "└─────────────────────────┴─────────────────┴──────────────────┘"
echo ""

# Step 3: Verify users by logging in
print_info "Verifying test users can login..."
echo ""

verify_login() {
    local username=$1
    local password=$2

    LOGIN_TEST=$(curl -s -X POST "${API_BASE}/login" \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"$username\",
            \"password\": \"$password\"
        }")

    TEST_TOKEN=$(echo "$LOGIN_TEST" | jq -r '.accessToken // empty')

    if [ -n "$TEST_TOKEN" ] && [ "$TEST_TOKEN" != "null" ]; then
        print_success "Login verified: $username"
        return 0
    else
        print_error "Login failed: $username"
        return 1
    fi
}

verify_login "test_admin" "password123"
verify_login "test_evaluator" "password123"
verify_login "perf_test_user" "password123"

echo ""
print_success "All test users created and verified successfully!"
echo ""
print_info "You can now use these credentials for:"
print_info "  - Manual testing via Postman/curl"
print_info "  - Load/performance testing"
print_info "  - E2E test automation"
print_info "  - UI user management interface"
echo ""
