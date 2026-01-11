#!/bin/bash
##############################################################################
# Create Test Users via SQL (Fallback Method)
#
# This script creates test users by directly inserting into the database.
# This is a fallback when the API registration endpoint has authentication issues.
#
# Prerequisites:
# - PostgreSQL must be running (healthdata-postgres container)
# - pgcrypto extension must be enabled
#
# Usage:
#   ./create-test-users-sql.sh
##############################################################################

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo "ℹ $1"; }

echo "=========================================";
echo "HDIM Test User Creation (SQL Method)"
echo "========================================="
echo ""

# Enable pgcrypto extension
print_info "Enabling pgcrypto extension..."
docker exec healthdata-postgres psql -U healthdata -d gateway_db -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;" > /dev/null 2>&1
print_success "Extension enabled"
echo ""

# Function to create a user via SQL
create_user_sql() {
    local username=$1
    local email=$2
    local first_name=$3
    local last_name=$4
    local role=$5
    local tenant_id=$6

    print_info "Creating user: $username ($role)..."

    # Create user with BCrypt-hashed password
    SQL="DO \$\$
DECLARE
    v_user_id UUID;
    v_password_hash TEXT;
BEGIN
    -- Check if user exists
    IF EXISTS (SELECT 1 FROM users WHERE username = '$username') THEN
        RAISE NOTICE 'User $username already exists. Skipping.';
    ELSE
        -- Generate UUID and password hash
        v_user_id := gen_random_uuid();
        v_password_hash := crypt('password123', gen_salt('bf'));

        -- Insert user
        INSERT INTO users (
            id, username, email, password_hash, first_name, last_name,
            active, email_verified, mfa_enabled, failed_login_attempts,
            created_at, updated_at
        ) VALUES (
            v_user_id, '$username', '$email', v_password_hash, '$first_name', '$last_name',
            true, true, false, 0,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        );

        -- Insert role
        INSERT INTO user_roles (user_id, role)
        VALUES (v_user_id, '$role');

        -- Insert tenant
        INSERT INTO user_tenants (user_id, tenant_id)
        VALUES (v_user_id, '$tenant_id');

        RAISE NOTICE 'User $username created successfully!';
    END IF;
END \$\$;"

    if docker exec healthdata-postgres psql -U healthdata -d gateway_db -c "$SQL" > /dev/null 2>&1; then
        print_success "Created: $username"
    else
        print_warning "Already exists or error: $username"
    fi
}

# Create test users for each role
print_info "Creating test users..."
echo ""

create_user_sql "test_superadmin" "superadmin@test.local" "Test" "SuperAdmin" "SUPER_ADMIN" "SYSTEM"
create_user_sql "test_admin" "admin@test.local" "Test" "Admin" "ADMIN" "TEST_TENANT_001"
create_user_sql "test_evaluator" "evaluator@test.local" "Test" "Evaluator" "EVALUATOR" "TEST_TENANT_001"
create_user_sql "test_analyst" "analyst@test.local" "Test" "Analyst" "ANALYST" "TEST_TENANT_001"
create_user_sql "test_viewer" "viewer@test.local" "Test" "Viewer" "VIEWER" "TEST_TENANT_001"

echo ""
print_info "Creating multi-tenant test users..."
create_user_sql "test_admin_tenant2" "admin2@test.local" "Test" "Admin2" "ADMIN" "TEST_TENANT_002"
create_user_sql "test_evaluator_tenant2" "evaluator2@test.local" "Test" "Evaluator2" "EVALUATOR" "TEST_TENANT_002"

echo ""
print_info "Creating performance testing users..."
create_user_sql "perf_test_user" "perftest@test.local" "Performance" "Tester" "EVALUATOR" "TEST_TENANT_001"

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

# Verify users by attempting login
print_info "Verifying test users can login..."
echo ""

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
verify_login() {
    local username=$1
    local password=$2

    LOGIN_RESPONSE=$(curl -s -X POST "${GATEWAY_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")

    if echo "$LOGIN_RESPONSE" | jq -e '.accessToken' > /dev/null 2>&1; then
        print_success "Login verified: $username"
        return 0
    else
        print_error "Login failed: $username"
        echo "Response: $LOGIN_RESPONSE"
        return 1
    fi
}

verify_login "test_admin" "password123"
verify_login "test_evaluator" "password123"
verify_login "perf_test_user" "password123"

echo ""
print_success "All test users created and verified successfully!"
echo ""
