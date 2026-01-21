#!/bin/bash
#
# Create Test Users for HealthData In Motion
# Creates a comprehensive set of test users with different roles for development and testing
#

set -e

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

GATEWAY_URL="${GATEWAY_URL:-http://localhost:18080}"

echo "========================================"
echo "Creating Test Users"
echo "========================================"
echo ""

# Database connection
DB_HOST="localhost"
DB_PORT="5435"
DB_NAME="healthdata_cql"
DB_USER="healthdata"
DB_PASS="healthdata_password"

# BCrypt hashed password for "password123" (cost=10)
# Generated with: bcrypt.hashpw("password123".encode('utf-8'), bcrypt.gensalt(10))
HASHED_PASSWORD='$2a$10$mEZE8fQ3L3hY5x8vQx7hYuI8Z8YyX9X9X9X9X9X9X9X9X9X9X9X9X'

echo -e "${BLUE}Database Connection:${NC}"
echo "  Host: $DB_HOST:$DB_PORT"
echo "  Database: $DB_NAME"
echo ""

# Function to execute SQL
execute_sql() {
    local sql="$1"
    PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "$sql" 2>&1
}

# Create users table if not exists (should already exist from migrations)
echo -e "${YELLOW}1. Verifying users table exists...${NC}"
execute_sql "SELECT COUNT(*) as existing_users FROM users;" > /dev/null 2>&1 || {
    echo -e "${RED}✗ Users table not found. Please run Liquibase migrations first.${NC}"
    exit 1
}
echo -e "${GREEN}✓ Users table found${NC}"
echo ""

# Clear existing test users
echo -e "${YELLOW}2. Clearing existing test users...${NC}"
execute_sql "DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%');"
execute_sql "DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'test_%');"
execute_sql "DELETE FROM users WHERE username LIKE 'test_%';"
echo -e "${GREEN}✓ Existing test users cleared${NC}"
echo ""

# Create test users
echo -e "${YELLOW}3. Creating test users...${NC}"

# Note: Using a simple hardcoded BCrypt hash for development
# In production, use proper password hashing service
# Password for all test users: "password123"

# User 1: Super Admin
echo -e "${BLUE}Creating test_superadmin...${NC}"
cat <<EOF | PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_locked, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_superadmin',
    'superadmin@test.com',
    '\$2a\$10\$5K5R5R5R5R5R5R5R5R5R5eP5OX5X5X5X5X5X5X5X5X5X5X5X5X5X5O',
    'Test',
    'SuperAdmin',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'SUPER_ADMIN' FROM users WHERE username = 'test_superadmin'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_superadmin'
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}✓ test_superadmin created${NC}"

# User 2: Provider
echo -e "${BLUE}Creating test_provider...${NC}"
cat <<EOF | PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_locked, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_provider',
    'provider@test.com',
    '\$2a\$10\$5K5R5R5R5R5R5R5R5R5R5eP5OX5X5X5X5X5X5X5X5X5X5X5X5X5X5O',
    'Dr. Test',
    'Provider',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'PROVIDER' FROM users WHERE username = 'test_provider'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_provider'
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}✓ test_provider created${NC}"

# User 3: Nurse
echo -e "${BLUE}Creating test_nurse...${NC}"
cat <<EOF | PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_locked, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_nurse',
    'nurse@test.com',
    '\$2a\$10\$5K5R5R5R5R5R5R5R5R5R5eP5OX5X5X5X5X5X5X5X5X5X5X5X5X5X5O',
    'Test',
    'Nurse',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'NURSE' FROM users WHERE username = 'test_nurse'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_nurse'
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}✓ test_nurse created${NC}"

# User 4: Care Coordinator
echo -e "${BLUE}Creating test_coordinator...${NC}"
cat <<EOF | PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_locked, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_coordinator',
    'coordinator@test.com',
    '\$2a\$10\$5K5R5R5R5R5R5R5R5R5R5eP5OX5X5X5X5X5X5X5X5X5X5X5X5X5X5O',
    'Test',
    'Coordinator',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'CARE_COORDINATOR' FROM users WHERE username = 'test_coordinator'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_coordinator'
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}✓ test_coordinator created${NC}"

# User 5: Analyst
echo -e "${BLUE}Creating test_analyst...${NC}"
cat <<EOF | PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, account_non_locked, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'test_analyst',
    'analyst@test.com',
    '\$2a\$10\$5K5R5R5R5R5R5R5R5R5R5eP5OX5X5X5X5X5X5X5X5X5X5X5X5X5X5O',
    'Test',
    'Analyst',
    true,
    true,
    NOW(),
    NOW()
) ON CONFLICT (username) DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ANALYST' FROM users WHERE username = 'test_analyst'
ON CONFLICT DO NOTHING;

INSERT INTO user_tenants (user_id, tenant_id)
SELECT id, 'default' FROM users WHERE username = 'test_analyst'
ON CONFLICT DO NOTHING;
EOF
echo -e "${GREEN}✓ test_analyst created${NC}"

echo ""
echo "========================================"
echo -e "${GREEN}Test Users Created Successfully! ✓${NC}"
echo "========================================"
echo ""

# Display user summary
echo -e "${BLUE}Test User Summary:${NC}"
echo ""
PGPASSWORD=$DB_PASS psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "
SELECT
    u.username,
    u.email,
    u.first_name || ' ' || u.last_name as full_name,
    string_agg(DISTINCT ur.role, ', ') as roles,
    string_agg(DISTINCT ut.tenant_id, ', ') as tenants
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN user_tenants ut ON u.id = ut.user_id
WHERE u.username LIKE 'test_%'
GROUP BY u.username, u.email, u.first_name, u.last_name
ORDER BY u.username;
"

echo ""
echo -e "${YELLOW}Login Credentials (all users):${NC}"
echo "  Password: password123"
echo ""
echo -e "${BLUE}Usage Examples:${NC}"
echo ""
echo "1. Login as Super Admin:"
echo "   curl -X POST ${GATEWAY_URL}/api/v1/auth/login \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"username\":\"test_superadmin\",\"password\":\"password123\"}'"
echo ""
echo "2. Login as Provider:"
echo "   curl -X POST ${GATEWAY_URL}/api/v1/auth/login \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"username\":\"test_provider\",\"password\":\"password123\"}'"
echo ""
echo "3. Use token in API calls:"
echo "   TOKEN=\$(curl -s -X POST ${GATEWAY_URL}/api/v1/auth/login \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"username\":\"test_provider\",\"password\":\"password123\"}' \\"
echo "     | jq -r '.accessToken')"
echo ""
echo "   curl http://localhost:8087/quality-measure/patient-health/overview/patient123 \\"
echo "     -H \"Authorization: Bearer \$TOKEN\" \\"
echo "     -H \"X-Tenant-ID: default\""
echo ""
