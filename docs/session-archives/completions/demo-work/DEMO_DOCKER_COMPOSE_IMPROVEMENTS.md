# Demo Docker Compose Improvements

**Date**: January 15, 2026  
**Status**: ✅ **IMPROVEMENTS APPLIED**

---

## Improvements Made

### 1. Enhanced Database Initialization Script

**File**: `demo/init-demo-db.sh`

**Improvements**:
- ✅ Uses `IF NOT EXISTS` pattern to avoid errors on restart
- ✅ Enables PostgreSQL extensions (uuid-ossp, pg_trgm) on all databases
- ✅ Proper error handling with `set -e`
- ✅ Clear output messages

**Databases Created**:
- `gateway_db` - Gateway service
- `cql_db` - CQL Engine service
- `patient_db` - Patient service
- `fhir_db` - FHIR service
- `caregap_db` - Care Gap service
- `quality_db` - Quality Measure service

**Extensions Enabled**:
- `uuid-ossp` - UUID generation
- `pg_trgm` - Fuzzy text search (for FHIR and patient services)

---

### 2. New Demo User Initialization Script

**File**: `demo/init-demo-users.sh`

**Purpose**: Creates demo users after gateway service has created the users table

**Features**:
- ✅ Waits for gateway service to be healthy
- ✅ Waits for users table to exist
- ✅ Creates 4 demo users with proper roles and tenants
- ✅ Uses BCrypt password hashing (password: `demo123`)
- ✅ Handles conflicts gracefully (ON CONFLICT)

**Demo Users Created**:
1. **demo_admin** / demo123
   - Roles: ADMIN, EVALUATOR
   - Tenant: acme-health

2. **demo_analyst** / demo123
   - Roles: ANALYST, EVALUATOR
   - Tenant: acme-health

3. **demo_viewer** / demo123
   - Roles: VIEWER
   - Tenant: acme-health

4. **demo_user** / demo123
   - Roles: USER
   - Tenant: acme-health

---

### 3. Updated Docker Compose Configuration

**File**: `demo/docker-compose.demo.yml`

**Improvements**:
- ✅ Added `init-demo-users` service that runs after gateway service is healthy
- ✅ Clinical portal depends on `init-demo-users` to ensure users exist before frontend starts
- ✅ Database initialization script mounted as read-only
- ✅ Proper service dependencies and health checks

**New Service**: `init-demo-users`
- Runs once after gateway service is healthy
- Creates demo users in gateway_db
- Uses postgres:16-alpine image with postgresql-client
- Automatically exits after completion

---

### 4. Fixed Gateway Service Migration

**File**: `backend/modules/services/gateway-service/src/main/resources/db/changelog/0001-create-auth-tables.xml`

**Fix**: Added missing columns to users table:
- `last_login_at` (TIMESTAMP WITH TIME ZONE)
- `account_locked_until` (TIMESTAMP WITH TIME ZONE)

This matches the User entity definition in the authentication module.

---

## Usage

### Starting the Demo Environment

```bash
cd demo

# Start all services (including user initialization)
docker compose -f docker-compose.demo.yml up -d

# Wait for services to be healthy (2-3 minutes)
docker compose -f docker-compose.demo.yml ps

# Seed demo data
./seed-demo-data.sh

# Access demo portal
# http://localhost:4200
# Login: demo_admin / demo123
```

### Manual User Initialization (if needed)

If the automatic user initialization fails, you can run it manually:

```bash
cd demo
./init-demo-users.sh
```

---

## Database Schema

### Gateway Database (gateway_db)

**Tables**:
- `users` - User accounts
- `user_roles` - User role assignments
- `user_tenants` - Multi-tenant user access
- `refresh_tokens` - JWT refresh tokens
- `audit_logs` - Audit trail

**Users Table Columns**:
- `id` (UUID)
- `username` (VARCHAR(50))
- `email` (VARCHAR(100))
- `password_hash` (VARCHAR(255))
- `first_name` (VARCHAR(100))
- `last_name` (VARCHAR(100))
- `active` (BOOLEAN)
- `email_verified` (BOOLEAN)
- `mfa_enabled` (BOOLEAN)
- `last_login_at` (TIMESTAMP)
- `failed_login_attempts` (INTEGER)
- `account_locked_until` (TIMESTAMP)
- `created_at` (TIMESTAMP)
- `updated_at` (TIMESTAMP)

---

## Verification

### Check Database Initialization

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c "\dt"
```

### Check Demo Users

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c "SELECT username, email, active FROM users;"
```

### Check User Roles

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c "SELECT u.username, ur.role FROM users u JOIN user_roles ur ON u.id = ur.user_id;"
```

---

## Next Steps

1. ✅ Database initialization improved
2. ✅ User initialization automated
3. ✅ Gateway service migration fixed
4. ⏳ Test full demo workflow
5. ⏳ Verify all services start correctly
6. ⏳ Test user authentication

---

**Status**: ✅ **IMPROVEMENTS COMPLETE - READY FOR TESTING**
