# User Role Testing Plan - HealthData-in-Motion

## Overview

This document outlines the comprehensive testing plan for validating the 7 demo user accounts and their role-based access control (RBAC) permissions.

## Test Environment Setup

### Prerequisites
```bash
# 1. Start infrastructure
docker-compose -f docker-compose.local.yml up -d postgres redis

# 2. Ensure database schema is current
# Run V006__create_users_table.sql migration

# 3. Start CQL Engine Service with dev profile
docker run -d \
  --name healthdata-cql-test \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/healthdata_cql \
  ...
  healthdata/cql-engine-service:latest

# 4. Verify demo accounts created
docker logs healthdata-cql-test | grep "DEMO ACCOUNTS"
```

### Expected Demo Account Creation Output
```
==========================================
  Initializing Demo User Accounts
==========================================
⚠️  DEMO MODE: Creating test accounts with known passwords
⚠️  NEVER use these accounts in production!

✅ Created demo user: superadmin (superadmin@healthdata.demo) with roles: [SUPER_ADMIN]
✅ Created demo user: admin (admin@healthdata.demo) with roles: [ADMIN]
✅ Created demo user: evaluator (evaluator@healthdata.demo) with roles: [EVALUATOR]
✅ Created demo user: analyst (analyst@healthdata.demo) with roles: [ANALYST]
✅ Created demo user: viewer (viewer@healthdata.demo) with roles: [VIEWER]
✅ Created demo user: multiuser (multi@healthdata.demo) with roles: [EVALUATOR, ANALYST]
✅ Created demo user: multitenant (multitenant@healthdata.demo) with roles: [EVALUATOR]
```

---

## Test Suite 1: Authentication Tests

### Test 1.1: Successful Login
**Objective**: Verify each demo account can authenticate successfully

**Test Cases**:
```bash
# Test superadmin login
curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "superadmin",
    "password": "SuperAdmin123!"
  }'

# Expected: 200 OK with JWT token
# {
#   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#   "refreshToken": "...",
#   "username": "superadmin",
#   "roles": ["SUPER_ADMIN"],
#   "tenantIds": ["DEMO_TENANT_001", "DEMO_TENANT_002"]
# }
```

**Repeat for all 7 accounts**:
- ✅ superadmin / SuperAdmin123!
- ✅ admin / Admin123!
- ✅ evaluator / Evaluator123!
- ✅ analyst / Analyst123!
- ✅ viewer / Viewer123!
- ✅ multiuser / Multi123!
- ✅ multitenant / MultiTenant123!

### Test 1.2: Failed Login - Invalid Password
**Objective**: Verify failed login attempts are tracked

```bash
# Attempt login with wrong password
curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "evaluator",
    "password": "WrongPassword123!"
  }'

# Expected: 401 Unauthorized
# { "error": "Invalid credentials" }
```

**Verification**:
```sql
SELECT username, failed_login_attempts
FROM users
WHERE username = 'evaluator';
-- Should show failed_login_attempts = 1
```

### Test 1.3: Account Locking After 5 Failed Attempts
**Objective**: Verify account locks after 5 consecutive failed logins

```bash
# Make 5 failed login attempts
for i in {1..5}; do
  curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "usernameOrEmail": "evaluator",
      "password": "WrongPassword!"
    }'
done

# Attempt 6th login with CORRECT password
curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "evaluator",
    "password": "Evaluator123!"
  }'

# Expected: 401 Unauthorized
# { "error": "Account locked. Please try again in 15 minutes." }
```

**Verification**:
```sql
SELECT username, failed_login_attempts, account_locked_until
FROM users
WHERE username = 'evaluator';
-- Should show:
-- failed_login_attempts = 5
-- account_locked_until = [timestamp 15 minutes in future]
```

---

## Test Suite 2: SUPER_ADMIN Role Tests

### Test 2.1: Cross-Tenant Access
**Objective**: Verify SUPER_ADMIN can access all tenants

```bash
# Get token for superadmin
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"superadmin","password":"SuperAdmin123!"}' \
  | jq -r '.token')

# Access DEMO_TENANT_001
curl http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK

# Access DEMO_TENANT_002
curl http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_002"
# Expected: 200 OK
```

### Test 2.2: User Management
**Objective**: Verify SUPER_ADMIN can manage users

```bash
# List all users
curl http://localhost:8081/cql-engine/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 OK with list of 7 demo users

# Create new user
curl -X POST http://localhost:8081/cql-engine/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User",
    "roles": ["VIEWER"],
    "tenantIds": ["DEMO_TENANT_001"]
  }'
# Expected: 201 Created
```

---

## Test Suite 3: ADMIN Role Tests

### Test 3.1: Tenant-Limited Access
**Objective**: Verify ADMIN can only access assigned tenant

```bash
# Get token for admin
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"admin","password":"Admin123!"}' \
  | jq -r '.token')

# Access assigned tenant (DEMO_TENANT_001)
curl http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK

# Attempt to access non-assigned tenant (DEMO_TENANT_002)
curl http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_002"
# Expected: 403 Forbidden
# { "error": "Access denied to tenant: DEMO_TENANT_002" }
```

### Test 3.2: User Management Within Tenant
**Objective**: Verify ADMIN can manage users in their tenant only

```bash
# List users in own tenant
curl http://localhost:8081/cql-engine/api/v1/users?tenantId=DEMO_TENANT_001 \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200 OK with users for DEMO_TENANT_001

# Attempt to list users in other tenant
curl http://localhost:8081/cql-engine/api/v1/users?tenantId=DEMO_TENANT_002 \
  -H "Authorization: Bearer $TOKEN"
# Expected: 403 Forbidden
```

---

## Test Suite 4: EVALUATOR Role Tests

### Test 4.1: Create and Execute Evaluation
**Objective**: Verify EVALUATOR can create and run evaluations

```bash
# Get token for evaluator
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"evaluator","password":"Evaluator123!"}' \
  | jq -r '.token')

# Create evaluation
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -H "Content-Type: application/json" \
  -d '{
    "libraryId": "cql-library-uuid",
    "patientId": "Patient/test-123",
    "parameters": {}
  }'
# Expected: 201 Created with evaluation result
```

### Test 4.2: View Own Evaluations
**Objective**: Verify EVALUATOR can view evaluations they created

```bash
# List evaluations
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK with list of evaluations
```

### Test 4.3: Cannot Manage Users
**Objective**: Verify EVALUATOR cannot perform admin functions

```bash
# Attempt to list users
curl http://localhost:8081/cql-engine/api/v1/users \
  -H "Authorization: Bearer $TOKEN"
# Expected: 403 Forbidden
# { "error": "Insufficient permissions" }
```

---

## Test Suite 5: ANALYST Role Tests (Read-Only)

### Test 5.1: View Evaluation Results
**Objective**: Verify ANALYST can view results but not create

```bash
# Get token for analyst
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"analyst","password":"Analyst123!"}' \
  | jq -r '.token')

# View evaluations (read-only)
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK

# Attempt to create evaluation (should fail)
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -H "Content-Type: application/json" \
  -d '{
    "libraryId": "cql-library-uuid",
    "patientId": "Patient/test-123"
  }'
# Expected: 403 Forbidden
# { "error": "Insufficient permissions. ANALYST role is read-only." }
```

### Test 5.2: Access Analytics Endpoints
**Objective**: Verify ANALYST can access reporting/analytics

```bash
# Access analytics dashboard
curl http://localhost:8081/cql-engine/api/v1/analytics/dashboard \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK with analytics data

# Export results
curl http://localhost:8081/cql-engine/api/v1/analytics/export?format=csv \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK with CSV download
```

---

## Test Suite 6: VIEWER Role Tests (Basic Read-Only)

### Test 6.1: Minimal Read Access
**Objective**: Verify VIEWER has basic read-only access

```bash
# Get token for viewer
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"viewer","password":"Viewer123!"}' \
  | jq -r '.token')

# View measure definitions
curl http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK (limited fields)

# Attempt to view evaluations (should fail or show limited data)
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 403 Forbidden or very limited data
```

### Test 6.2: No Write or Analytics Access
**Objective**: Verify VIEWER cannot perform any write operations

```bash
# Attempt to create evaluation
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -d '{}'
# Expected: 403 Forbidden

# Attempt to access analytics
curl http://localhost:8081/cql-engine/api/v1/analytics/dashboard \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 403 Forbidden
```

---

## Test Suite 7: Multi-Role User Tests

### Test 7.1: Combined Permissions
**Objective**: Verify multiuser (EVALUATOR + ANALYST) has both sets of permissions

```bash
# Get token for multiuser
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"multiuser","password":"Multi123!"}' \
  | jq -r '.token')

# Execute evaluation (EVALUATOR permission)
curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -d '{...}'
# Expected: 201 Created

# Access analytics (ANALYST permission)
curl http://localhost:8081/cql-engine/api/v1/analytics/dashboard \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK

# Verify both roles in token
echo $TOKEN | base64 -d | jq '.roles'
# Expected: ["EVALUATOR", "ANALYST"]
```

---

## Test Suite 8: Multi-Tenant User Tests

### Test 8.1: Cross-Tenant Access for Assigned Tenants
**Objective**: Verify multitenant user can access both assigned tenants

```bash
# Get token for multitenant
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"usernameOrEmail":"multitenant","password":"MultiTenant123!"}' \
  | jq -r '.token')

# Access DEMO_TENANT_001
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
# Expected: 200 OK

# Access DEMO_TENANT_002
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_002"
# Expected: 200 OK

# Attempt to access non-assigned tenant
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_003"
# Expected: 403 Forbidden
```

### Test 8.2: Data Isolation Between Tenants
**Objective**: Verify data from one tenant isn't visible in another

```bash
# Create evaluation in TENANT_001
EVAL_ID=$(curl -X POST http://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -d '{...}' | jq -r '.id')

# Attempt to access from TENANT_002 context
curl http://localhost:8081/cql-engine/api/v1/cql/evaluations/$EVAL_ID \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO_TENANT_002"
# Expected: 404 Not Found (tenant isolation working)
```

---

## Test Suite 9: Security Tests

### Test 9.1: JWT Token Expiration
**Objective**: Verify tokens expire after configured time

```bash
# Get token
TOKEN=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -d '{"usernameOrEmail":"evaluator","password":"Evaluator123!"}' | jq -r '.token')

# Wait for token expiration (configured as 1 hour in dev)
# In test, could temporarily set JWT_EXPIRATION_MS=60000 (1 minute)
sleep 70

# Attempt to use expired token
curl http://localhost:8081/cql-engine/api/v1/cql/libraries \
  -H "Authorization: Bearer $TOKEN"
# Expected: 401 Unauthorized
# { "error": "Token expired" }
```

### Test 9.2: Token Refresh
**Objective**: Verify refresh tokens work correctly

```bash
# Get tokens
RESPONSE=$(curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
  -d '{"usernameOrEmail":"evaluator","password":"Evaluator123!"}')

ACCESS_TOKEN=$(echo $RESPONSE | jq -r '.token')
REFRESH_TOKEN=$(echo $RESPONSE | jq -r '.refreshToken')

# Use refresh token to get new access token
curl -X POST http://localhost:8081/cql-engine/api/v1/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"
# Expected: 200 OK with new access token
```

---

## Test Suite 10: Database Verification

### Test 10.1: User Records Created Correctly
```sql
SELECT
  username,
  email,
  active,
  email_verified,
  created_at,
  failed_login_attempts,
  account_locked_until
FROM users
ORDER BY username;
```

**Expected Results**:
- 7 users created
- All active = true
- All email_verified = true
- failed_login_attempts = 0 (initially)
- account_locked_until = NULL

### Test 10.2: Roles Assigned Correctly
```sql
SELECT
  u.username,
  STRING_AGG(r.role, ', ' ORDER BY r.role) as roles
FROM users u
JOIN user_roles r ON u.id = r.user_id
GROUP BY u.username
ORDER BY u.username;
```

**Expected Results**:
| Username | Roles |
|----------|-------|
| admin | ADMIN |
| analyst | ANALYST |
| evaluator | EVALUATOR |
| multiuser | ANALYST, EVALUATOR |
| multitenant | EVALUATOR |
| superadmin | SUPER_ADMIN |
| viewer | VIEWER |

### Test 10.3: Tenant Assignments
```sql
SELECT
  u.username,
  STRING_AGG(t.tenant_id, ', ' ORDER BY t.tenant_id) as tenants
FROM users u
JOIN user_tenants t ON u.id = t.user_id
GROUP BY u.username
ORDER BY u.username;
```

**Expected Results**:
| Username | Tenants |
|----------|---------|
| admin | DEMO_TENANT_001 |
| analyst | DEMO_TENANT_001 |
| evaluator | DEMO_TENANT_001 |
| multiuser | DEMO_TENANT_001 |
| multitenant | DEMO_TENANT_001, DEMO_TENANT_002 |
| superadmin | DEMO_TENANT_001, DEMO_TENANT_002 |
| viewer | DEMO_TENANT_001 |

---

## Summary Checklist

### Authentication ✅
- [ ] All 7 accounts can log in successfully
- [ ] Invalid passwords are rejected
- [ ] Failed attempts are tracked
- [ ] Account locks after 5 failures
- [ ] Lock duration is 15 minutes

### SUPER_ADMIN Role ✅
- [ ] Access to all tenants
- [ ] User management permissions
- [ ] System configuration access

### ADMIN Role ✅
- [ ] Tenant-limited access
- [ ] User management within tenant
- [ ] Cannot access other tenants

### EVALUATOR Role ✅
- [ ] Can create evaluations
- [ ] Can view own evaluations
- [ ] Cannot manage users
- [ ] Tenant isolation enforced

### ANALYST Role ✅
- [ ] Can view evaluations (read-only)
- [ ] Can access analytics
- [ ] Cannot create/modify evaluations
- [ ] Cannot manage users

### VIEWER Role ✅
- [ ] Basic read-only access
- [ ] Cannot view evaluations
- [ ] Cannot access analytics
- [ ] Cannot perform any writes

### Multi-Role User ✅
- [ ] Has combined permissions
- [ ] Both roles functional

### Multi-Tenant User ✅
- [ ] Can access both assigned tenants
- [ ] Cannot access unassigned tenants
- [ ] Data isolation enforced

### Security ✅
- [ ] JWT tokens expire correctly
- [ ] Refresh tokens work
- [ ] Password hashing with BCrypt
- [ ] All sensitive data encrypted
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

---

## Test Execution Log Template

```
Test Date: __________
Tester: __________
Environment: __________

| Test ID | Test Name | Status | Notes |
|---------|-----------|--------|-------|
| 1.1 | Successful Login | ☐ Pass ☐ Fail | |
| 1.2 | Failed Login | ☐ Pass ☐ Fail | |
| 1.3 | Account Locking | ☐ Pass ☐ Fail | |
| 2.1 | SUPER_ADMIN Cross-Tenant | ☐ Pass ☐ Fail | |
| ... | ... | ... | ... |
```

---

**Last Updated**: 2025-11-06
**Version**: 1.0
**Status**: Ready for Testing
