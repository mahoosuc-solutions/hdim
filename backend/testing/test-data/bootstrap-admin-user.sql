-- Bootstrap Initial SUPER_ADMIN User for Testing
-- This creates the initial admin user needed to create other users via API
--
-- Credentials:
--   Username: bootstrap_admin
--   Password: password123
--   Email: admin@healthdata.local
--
-- Usage:
--   docker exec healthdata-postgres psql -U healthdata -d gateway_db -f /path/to/bootstrap-admin-user.sql
--
-- SECURITY WARNING: This is for TESTING ONLY. Never use in production!

-- Check if user already exists
DO $$
DECLARE
    v_user_id UUID;
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE username = 'bootstrap_admin') THEN
        RAISE NOTICE 'User bootstrap_admin already exists. Skipping creation.';
    ELSE
        -- Generate UUID for the user
        v_user_id := gen_random_uuid();

        -- Insert bootstrap admin user
        -- Password: password123 (BCrypt hash with strength 10)
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
        ) VALUES (
            v_user_id,
            'bootstrap_admin',
            'admin@healthdata.local',
            '$2a$10$N9qo8uLOickgx2ZMRZoMy.iQCH.PknEdgPBEJP2vHFAP1phdxN8oG',  -- password123
            'Bootstrap',
            'Administrator',
            true,                             -- active
            true,                             -- email_verified
            false,                            -- mfa_enabled
            0,                                -- failed_login_attempts
            CURRENT_TIMESTAMP,                -- created_at
            CURRENT_TIMESTAMP                 -- updated_at
        );

        -- Insert role into user_roles table
        INSERT INTO user_roles (user_id, role)
        VALUES (v_user_id, 'SUPER_ADMIN');

        -- Insert tenant into user_tenants table
        INSERT INTO user_tenants (user_id, tenant_id)
        VALUES (v_user_id, 'SYSTEM');

        RAISE NOTICE 'Bootstrap admin user created successfully!';
        RAISE NOTICE 'Username: bootstrap_admin';
        RAISE NOTICE 'Password: password123';
    END IF;
END $$;

-- Verify the user was created
SELECT
    u.id,
    u.username,
    u.email,
    u.first_name,
    u.last_name,
    u.active,
    u.email_verified,
    u.created_at,
    array_agg(DISTINCT ur.role) as roles,
    array_agg(DISTINCT ut.tenant_id) as tenant_ids
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN user_tenants ut ON u.id = ut.user_id
WHERE u.username = 'bootstrap_admin'
GROUP BY u.id, u.username, u.email, u.first_name, u.last_name, u.active, u.email_verified, u.created_at;
