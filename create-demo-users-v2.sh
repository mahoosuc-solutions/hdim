#!/bin/bash

################################################################################
# Create Demo Users - Simplified Version
################################################################################

set -e

DB_CONTAINER="healthdata-postgres"
DB_NAME="healthdata_cql"
DB_USER="healthdata"

echo "Generating BCrypt hash for demo123..."
HASH=$(python3 << 'PYEOF'
import bcrypt
password = b"demo123"
salt = bcrypt.gensalt(rounds=10)
hash_bytes = bcrypt.hashpw(password, salt)
print(hash_bytes.decode('utf-8'))
PYEOF
)

echo "Generated hash: $HASH"
echo ""
echo "Creating demo users..."

# Create SQL file with the hash
cat > /tmp/demo-users.sql << SQLEOF
-- Delete existing demo users
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.%');
DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.%');
DELETE FROM users WHERE username LIKE 'demo.%';

-- 1. Clinical Doctor (Evaluator role - can view and assess quality measures)
WITH new_user AS (
  INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
  VALUES (gen_random_uuid(), 'demo.doctor', 'demo.doctor@healthdata.com', 
          '$HASH', 'Sarah', 'Chen', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
)
INSERT INTO user_roles (user_id, role)
SELECT id, 'EVALUATOR' FROM new_user;

WITH user_id AS (SELECT id FROM users WHERE username = 'demo.doctor')
INSERT INTO user_tenants (user_id, tenant_id) 
SELECT id, 'demo-clinic' FROM user_id;

-- 2. Quality Analyst (Analyst role - can analyze data and create reports)
WITH new_user AS (
  INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
  VALUES (gen_random_uuid(), 'demo.analyst', 'demo.analyst@healthdata.com',
          '$HASH', 'Michael', 'Rodriguez', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ANALYST' FROM new_user;

WITH user_id AS (SELECT id FROM users WHERE username = 'demo.analyst')
INSERT INTO user_tenants (user_id, tenant_id) 
SELECT id, 'demo-clinic' FROM user_id;

-- 3. Care Evaluator (Evaluator role - can assess care gaps)
WITH new_user AS (
  INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
  VALUES (gen_random_uuid(), 'demo.care', 'demo.care@healthdata.com',
          '$HASH', 'Jennifer', 'Thompson', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
)
INSERT INTO user_roles (user_id, role)
SELECT id, 'EVALUATOR' FROM new_user;

WITH user_id AS (SELECT id FROM users WHERE username = 'demo.care')
INSERT INTO user_tenants (user_id, tenant_id) 
SELECT id, 'demo-clinic' FROM user_id;

-- 4. Admin User (Full system access)
WITH new_user AS (
  INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
  VALUES (gen_random_uuid(), 'demo.admin', 'demo.admin@healthdata.com',
          '$HASH', 'David', 'Johnson', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM new_user;

WITH user_id AS (SELECT id FROM users WHERE username = 'demo.admin')
INSERT INTO user_tenants (user_id, tenant_id) 
SELECT id, 'demo-clinic' FROM user_id;

-- 5. Viewer User (Read-only access)
WITH new_user AS (
  INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, created_at, updated_at)
  VALUES (gen_random_uuid(), 'demo.viewer', 'demo.viewer@healthdata.com',
          '$HASH', 'Emily', 'Martinez', true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
)
INSERT INTO user_roles (user_id, role)
SELECT id, 'VIEWER' FROM new_user;

WITH user_id AS (SELECT id FROM users WHERE username = 'demo.viewer')
INSERT INTO user_tenants (user_id, tenant_id) 
SELECT id, 'demo-clinic' FROM user_id;

-- Verify
SELECT username, first_name, last_name, email, active, email_verified FROM users WHERE username LIKE 'demo.%' ORDER BY username;
SQLEOF

# Execute SQL
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" < /tmp/demo-users.sql

echo ""
echo "✓ Demo users created successfully!"
echo ""
echo "Testing login..."
curl -s -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}' | jq -r 'if .accessToken then "✓ Login test PASSED" else "✗ Login test FAILED: " + .message end'
