#!/bin/bash
# Initialize Demo Users
# This script creates demo users after the gateway service has created the users table
# Run this after services have started and migrations have completed

set -e

POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_USER="${POSTGRES_USER:-healthdata}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-demo_password_2024}"
DB_NAME="gateway_db"

# Use local psql if available, otherwise fall back to docker exec.
if command -v psql >/dev/null 2>&1; then
    PSQL_CMD=(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$DB_NAME")
else
    PSQL_CMD=(docker compose -f demo/docker-compose.demo.yml exec -T postgres psql -U "$POSTGRES_USER" -d "$DB_NAME")
fi

# BCrypt hash for password "demo123"
# Generated using: BCryptPasswordEncoder.encode("demo123")
DEMO_PASSWORD_HASH='$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S'

echo "=========================================="
echo "HDIM Demo - User Initialization"
echo "=========================================="

# Wait for gateway_db to be ready and users table to exist
echo "Waiting for gateway_db and users table..."
for i in {1..30}; do
    if PGPASSWORD="$POSTGRES_PASSWORD" "${PSQL_CMD[@]}" -c "\d users" > /dev/null 2>&1; then
        echo "✅ Users table found!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Error: Users table not found after 30 attempts"
        exit 1
    fi
    sleep 2
done

# Create demo users
echo "Creating demo users..."

PGPASSWORD="$POSTGRES_PASSWORD" "${PSQL_CMD[@]}" <<-EOSQL
    -- Insert demo users (using ON CONFLICT to avoid duplicates)
    ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_role_check;
    ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_check
        CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'MEASURE_DEVELOPER', 'EVALUATOR', 'ANALYST', 'VIEWER'));

    INSERT INTO users (
        id, 
        username, 
        email, 
        password_hash, 
        first_name, 
        last_name, 
        active, 
        email_verified, 
        mfa_enabled,
        failed_login_attempts,
        created_at, 
        updated_at
    ) VALUES
    (
        '550e8400-e29b-41d4-a716-446655440010'::uuid,
        'demo_admin',
        'demo_admin@hdim.ai',
        '$DEMO_PASSWORD_HASH',
        'Demo',
        'Admin',
        true,
        true,
        false,
        0,
        NOW(),
        NOW()
    ),
    (
        '550e8400-e29b-41d4-a716-446655440011'::uuid,
        'demo_analyst',
        'demo_analyst@hdim.ai',
        '$DEMO_PASSWORD_HASH',
        'Demo',
        'Analyst',
        true,
        true,
        false,
        0,
        NOW(),
        NOW()
    ),
    (
        '550e8400-e29b-41d4-a716-446655440012'::uuid,
        'demo_viewer',
        'demo_viewer@hdim.ai',
        '$DEMO_PASSWORD_HASH',
        'Demo',
        'Viewer',
        true,
        true,
        false,
        0,
        NOW(),
        NOW()
    ),
    (
        '550e8400-e29b-41d4-a716-446655440013'::uuid,
        'demo_user',
        'demo_user@hdim.ai',
        '$DEMO_PASSWORD_HASH',
        'Demo',
        'User',
        true,
        true,
        false,
        0,
        NOW(),
        NOW()
    ),
    (
        '550e8400-e29b-41d4-a716-446655440014'::uuid,
        'demo.developer',
        'demo.developer@hdim.ai',
        '$DEMO_PASSWORD_HASH',
        'Demo',
        'Developer',
        true,
        true,
        false,
        0,
        NOW(),
        NOW()
    )
    ON CONFLICT (username) DO UPDATE SET
        email = EXCLUDED.email,
        password_hash = EXCLUDED.password_hash,
        first_name = EXCLUDED.first_name,
        last_name = EXCLUDED.last_name,
        active = EXCLUDED.active,
        email_verified = EXCLUDED.email_verified,
        updated_at = NOW();

    -- Create user roles
    INSERT INTO user_roles (user_id, role) VALUES
    ('550e8400-e29b-41d4-a716-446655440010'::uuid, 'ADMIN'),
    ('550e8400-e29b-41d4-a716-446655440010'::uuid, 'EVALUATOR'),
    ('550e8400-e29b-41d4-a716-446655440011'::uuid, 'ANALYST'),
    ('550e8400-e29b-41d4-a716-446655440011'::uuid, 'EVALUATOR'),
    ('550e8400-e29b-41d4-a716-446655440012'::uuid, 'VIEWER'),
    ('550e8400-e29b-41d4-a716-446655440013'::uuid, 'VIEWER'),
    ('550e8400-e29b-41d4-a716-446655440014'::uuid, 'MEASURE_DEVELOPER'),
    ('550e8400-e29b-41d4-a716-446655440014'::uuid, 'EVALUATOR')
    ON CONFLICT DO NOTHING;

    -- Create user tenants (multi-tenant support)
    INSERT INTO user_tenants (user_id, tenant_id) VALUES
    ('550e8400-e29b-41d4-a716-446655440010'::uuid, 'acme-health'),
    ('550e8400-e29b-41d4-a716-446655440011'::uuid, 'acme-health'),
    ('550e8400-e29b-41d4-a716-446655440012'::uuid, 'acme-health'),
    ('550e8400-e29b-41d4-a716-446655440013'::uuid, 'acme-health'),
    ('550e8400-e29b-41d4-a716-446655440014'::uuid, 'acme-health')
    ON CONFLICT DO NOTHING;
EOSQL

echo ""
echo "✅ Demo users created successfully!"
echo ""
echo "Demo Users:"
echo "  - demo_admin / demo123 (ADMIN, EVALUATOR)"
echo "  - demo_analyst / demo123 (ANALYST, EVALUATOR)"
echo "  - demo_viewer / demo123 (VIEWER)"
echo "  - demo_user / demo123 (VIEWER)"
echo "  - demo.developer / demo123 (MEASURE_DEVELOPER, EVALUATOR)"
echo ""
echo "All users belong to tenant: acme-health"
echo ""
