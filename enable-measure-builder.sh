#!/bin/bash
set -e

# Container Name
DB_CONTAINER="hdim-demo-postgres"
DB_USER="healthdata"
DB_NAME="gateway_db"

echo "Checking if database container is running..."
if ! docker ps | grep -q "$DB_CONTAINER"; then
    echo "Error: Container $DB_CONTAINER is not running."
    exit 1
fi

echo "Generating BCrypt hash for demo123..."
HASH=$(python3 << 'PYEOF'
import bcrypt
password = b"demo123"
salt = bcrypt.gensalt(rounds=10)
hash_bytes = bcrypt.hashpw(password, salt)
print(hash_bytes.decode('utf-8'))
PYEOF
)

echo "Provisioning 'demo.developer' user..."

cat > /tmp/measure-developer.sql << SQLEOF
-- Update Constraint to allow MEASURE_DEVELOPER
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_role_check;
ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_check 
  CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER', 'MEASURE_DEVELOPER'));

-- Remove if exists
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'demo.developer');
DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username = 'demo.developer');
DELETE FROM users WHERE username = 'demo.developer';

-- Create User
WITH new_user AS (
  INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, email_verified, mfa_enabled, created_at, updated_at)
  VALUES (gen_random_uuid(), 'demo.developer', 'demo.developer@healthdata.com',
          '$HASH', 'Sarah', 'Dev', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
)
INSERT INTO user_roles (user_id, role)
SELECT id, 'MEASURE_DEVELOPER' FROM new_user;

WITH user_id AS (SELECT id FROM users WHERE username = 'demo.developer')
INSERT INTO user_tenants (user_id, tenant_id) 
SELECT id, 'demo-clinic' FROM user_id;
SQLEOF

# Execute
cat /tmp/measure-developer.sql | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME"

echo "✓ User 'demo.developer' created with role 'MEASURE_DEVELOPER'."
