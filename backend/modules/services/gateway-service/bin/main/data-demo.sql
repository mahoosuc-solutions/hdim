-- Demo Mode User Accounts
-- Purpose: Easy-to-use accounts for video demonstrations
-- Password for ALL demo accounts: "demo123"
-- BCrypt hash for "demo123": $2a$10$rQ3K7sxF8zYqG8YvH7L8.eF4J6PqH8KZcVqL5nX9mYvZ8jQxL6HXK

-- Clear existing demo users (if any)
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.%');
DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.%');
DELETE FROM users WHERE username LIKE 'demo.%';

-- 1. CLINICAL USER - Primary Care Physician
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (
    'demo-clinical-001',
    'demo.doctor',
    'demo.doctor@healthdata.com',
    '$2a$10$rQ3K7sxF8zYqG8YvH7L8.eF4J6PqH8KZcVqL5nX9mYvZ8jQxL6HXK',
    'Sarah',
    'Chen',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role) VALUES ('demo-clinical-001', 'CLINICAL_USER');
INSERT INTO user_roles (user_id, role) VALUES ('demo-clinical-001', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-clinical-001', 'demo-clinic');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-clinical-001', 'acme-health');

-- 2. QUALITY MANAGER - Oversees quality measures
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (
    'demo-quality-001',
    'demo.quality',
    'demo.quality@healthdata.com',
    '$2a$10$rQ3K7sxF8zYqG8YvH7L8.eF4J6PqH8KZcVqL5nX9mYvZ8jQxL6HXK',
    'Michael',
    'Rodriguez',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role) VALUES ('demo-quality-001', 'QUALITY_MANAGER');
INSERT INTO user_roles (user_id, role) VALUES ('demo-quality-001', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-quality-001', 'demo-clinic');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-quality-001', 'acme-health');

-- 3. CARE COORDINATOR - Manages patient care gaps
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (
    'demo-care-001',
    'demo.care',
    'demo.care@healthdata.com',
    '$2a$10$rQ3K7sxF8zYqG8YvH7L8.eF4J6PqH8KZcVqL5nX9mYvZ8jQxL6HXK',
    'Jennifer',
    'Thompson',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role) VALUES ('demo-care-001', 'CARE_COORDINATOR');
INSERT INTO user_roles (user_id, role) VALUES ('demo-care-001', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-care-001', 'demo-clinic');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-care-001', 'acme-health');

-- 4. ADMIN USER - Full system access
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (
    'demo-admin-001',
    'demo.admin',
    'demo.admin@healthdata.com',
    '$2a$10$rQ3K7sxF8zYqG8YvH7L8.eF4J6PqH8KZcVqL5nX9mYvZ8jQxL6HXK',
    'David',
    'Johnson',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role) VALUES ('demo-admin-001', 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES ('demo-admin-001', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-admin-001', 'demo-clinic');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-admin-001', 'acme-health');

-- 5. READ ONLY USER - View-only access for stakeholders
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (
    'demo-viewer-001',
    'demo.viewer',
    'demo.viewer@healthdata.com',
    '$2a$10$rQ3K7sxF8zYqG8YvH7L8.eF4J6PqH8KZcVqL5nX9mYvZ8jQxL6HXK',
    'Emily',
    'Martinez',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO user_roles (user_id, role) VALUES ('demo-viewer-001', 'VIEWER');
INSERT INTO user_roles (user_id, role) VALUES ('demo-viewer-001', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-viewer-001', 'demo-clinic');
INSERT INTO user_tenants (user_id, tenant_id) VALUES ('demo-viewer-001', 'acme-health');
