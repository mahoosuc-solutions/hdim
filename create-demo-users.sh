#!/bin/bash

################################################################################
# Generate Demo Users with Correct BCrypt Hashes
################################################################################

set -e

DB_CONTAINER="healthdata-postgres"
DB_NAME="healthdata_cql"
DB_USER="healthdata"

# Using the same password hashing approach as admin user
# We'll use the Java BCrypt from the existing admin's hash as reference
# Password: demo123

echo "Generating BCrypt hash for demo123..."

# Create a temporary Java program to generate BCrypt hash
cat > /tmp/HashGenerator.java << 'EOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("demo123");
        System.out.println(hash);
    }
}
EOF

# Try to compile and run using Gateway container
HASH=$(docker exec healthdata-gateway /bin/sh -c "
cd /tmp
cat > HashGen.java << 'JAVAEOF'
public class HashGen {
    public static void main(String[] args) {
        // Using BCrypt via Spring Security
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        System.out.println(encoder.encode(\"demo123\"));
    }
}
JAVAEOF

java -cp '/app/app.jar:/app/lib/*' HashGen 2>/dev/null || echo 'FAILED'
" 2>/dev/null | grep '^\$2' | head -1)

if [ -z "$HASH" ] || [ "$HASH" = "FAILED" ]; then
    echo "Could not generate hash via Java, using Python htpasswd alternative..."
    # Fallback: Use Python to generate BCrypt hash
    HASH=$(python3 << 'PYEOF'
import bcrypt
password = b"demo123"
salt = bcrypt.gensalt(rounds=10)
hash_bytes = bcrypt.hashpw(password, salt)
print(hash_bytes.decode('utf-8'))
PYEOF
)
fi

if [ -z "$HASH" ]; then
    echo "Error: Could not generate password hash"
    exit 1
fi

echo "Generated hash: $HASH"
echo ""
echo "Creating demo users..."

# Insert users with generated hash
docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" << EOF
-- Delete existing demo users
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.%');
DELETE FROM user_tenants WHERE user_id IN (SELECT id FROM users WHERE username LIKE 'demo.%');
DELETE FROM users WHERE username LIKE 'demo.%';

-- 1. Clinical Doctor
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'demo.doctor', 'demo.doctor@healthdata.com', 
        '$HASH', 'Sarah', 'Chen', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
RETURNING id \gset doctor_
INSERT INTO user_roles (user_id, role) VALUES (:'doctor_id', 'CLINICAL_USER');
INSERT INTO user_roles (user_id, role) VALUES (:'doctor_id', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES (:'doctor_id', 'demo-clinic');

-- 2. Quality Manager
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'demo.quality', 'demo.quality@healthdata.com',
        '$HASH', 'Michael', 'Rodriguez', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
RETURNING id \gset quality_
INSERT INTO user_roles (user_id, role) VALUES (:'quality_id', 'QUALITY_MANAGER');
INSERT INTO user_roles (user_id, role) VALUES (:'quality_id', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES (:'quality_id', 'demo-clinic');

-- 3. Care Coordinator
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'demo.care', 'demo.care@healthdata.com',
        '$HASH', 'Jennifer', 'Thompson', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
RETURNING id \gset care_
INSERT INTO user_roles (user_id, role) VALUES (:'care_id', 'CARE_COORDINATOR');
INSERT INTO user_roles (user_id, role) VALUES (:'care_id', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES (:'care_id', 'demo-clinic');

-- 4. Admin User
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'demo.admin', 'demo.admin@healthdata.com',
        '$HASH', 'David', 'Johnson', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
RETURNING id \gset admin_
INSERT INTO user_roles (user_id, role) VALUES (:'admin_id', 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (:'admin_id', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES (:'admin_id', 'demo-clinic');

-- 5. Viewer User
INSERT INTO users (id, username, email, password_hash, first_name, last_name, active, created_at, updated_at)
VALUES (gen_random_uuid(), 'demo.viewer', 'demo.viewer@healthdata.com',
        '$HASH', 'Emily', 'Martinez', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
RETURNING id \gset viewer_
INSERT INTO user_roles (user_id, role) VALUES (:'viewer_id', 'VIEWER');
INSERT INTO user_roles (user_id, role) VALUES (:'viewer_id', 'USER');
INSERT INTO user_tenants (user_id, tenant_id) VALUES (:'viewer_id', 'demo-clinic');

-- Verify
SELECT username, first_name, last_name FROM users WHERE username LIKE 'demo.%' ORDER BY username;
EOF

echo ""
echo "✓ Demo users created successfully!"
