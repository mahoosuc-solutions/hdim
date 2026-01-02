# Demo User Accounts - HealthData-in-Motion

This document provides information about the demo user accounts available in development and demo environments for testing authentication and authorization.

## ⚠️ Security Warning

**THESE ACCOUNTS ARE FOR DEVELOPMENT/DEMO PURPOSES ONLY**

- Never use these credentials in production
- Demo accounts are only created when Spring profiles `dev`, `demo`, or `local` are active
- All demo accounts have well-known passwords and should be disabled in production

## Demo Accounts Overview

The system creates 7 demo accounts representing different roles and access patterns:

### 1. Super Administrator

**Purpose**: Full system access across all tenants

| Field | Value |
|-------|-------|
| Username | `superadmin` |
| Password | `SuperAdmin123!` |
| Email | superadmin@healthdata.demo |
| Role(s) | `SUPER_ADMIN` |
| Tenants | `DEMO_TENANT_001`, `DEMO_TENANT_002` |

**Permissions**:
- Full system access
- User management across all tenants
- System configuration
- Tenant management
- All CRUD operations

**Use Cases**:
- Testing cross-tenant operations
- System-wide configuration testing
- User management workflows

---

### 2. Administrator

**Purpose**: Tenant-level administration

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `Admin123!` |
| Email | admin@healthdata.demo |
| Role(s) | `ADMIN` |
| Tenants | `DEMO_TENANT_001` |

**Permissions**:
- Tenant administration
- User management within tenant
- Measure configuration
- Value set management
- Library management
- All evaluation operations

**Use Cases**:
- Tenant-specific administrative tasks
- User management within a single tenant
- Configuration management

---

### 3. Evaluator

**Purpose**: Execute quality measure evaluations

| Field | Value |
|-------|-------|
| Username | `evaluator` |
| Password | `Evaluator123!` |
| Email | evaluator@healthdata.demo |
| Role(s) | `EVALUATOR` |
| Tenants | `DEMO_TENANT_001` |

**Permissions**:
- Create and execute CQL evaluations
- View evaluation results
- Manage own evaluations
- Access FHIR resources
- View measure definitions

**Use Cases**:
- Running quality measure evaluations
- Testing evaluation workflows
- FHIR resource access

---

### 4. Analyst

**Purpose**: View and analyze evaluation results (read-only)

| Field | Value |
|-------|-------|
| Username | `analyst` |
| Password | `Analyst123!` |
| Email | analyst@healthdata.demo |
| Role(s) | `ANALYST` |
| Tenants | `DEMO_TENANT_001` |

**Permissions**:
- View evaluation results
- Generate reports
- View analytics and dashboards
- Export data (read-only)
- NO write access

**Use Cases**:
- Testing read-only access
- Report generation
- Analytics workflows

---

### 5. Viewer

**Purpose**: Basic read-only access

| Field | Value |
|-------|-------|
| Username | `viewer` |
| Password | `Viewer123!` |
| Email | viewer@healthdata.demo |
| Role(s) | `VIEWER` |
| Tenants | `DEMO_TENANT_001` |

**Permissions**:
- View measure definitions
- View limited evaluation results
- Basic read-only access
- NO write or execute permissions

**Use Cases**:
- Testing minimal access
- Public-facing information display
- Guest access scenarios

---

### 6. Multi-Role User

**Purpose**: Testing multiple role assignments

| Field | Value |
|-------|-------|
| Username | `multiuser` |
| Password | `Multi123!` |
| Email | multi@healthdata.demo |
| Role(s) | `EVALUATOR`, `ANALYST` |
| Tenants | `DEMO_TENANT_001` |

**Permissions**:
- Combined permissions of EVALUATOR and ANALYST
- Execute evaluations + view analytics
- Full read/write for evaluations
- Report generation and analytics

**Use Cases**:
- Testing combined role permissions
- Role hierarchy validation
- Permission boundary testing

---

### 7. Multi-Tenant User

**Purpose**: Testing cross-tenant access

| Field | Value |
|-------|-------|
| Username | `multitenant` |
| Password | `MultiTenant123!` |
| Email | multitenant@healthdata.demo |
| Role(s) | `EVALUATOR` |
| Tenants | `DEMO_TENANT_001`, `DEMO_TENANT_002` |

**Permissions**:
- Evaluator access in multiple tenants
- Tenant-specific data isolation
- Cross-tenant workflow testing

**Use Cases**:
- Multi-tenant isolation testing
- Cross-tenant data access validation
- Tenant switching workflows

---

## Role Hierarchy

Roles are ordered from most to least privileged:

1. **SUPER_ADMIN** - Full system access
2. **ADMIN** - Tenant administration
3. **EVALUATOR** - Execute evaluations
4. **ANALYST** - View and analyze (read-only)
5. **VIEWER** - Basic read-only

## Testing Scenarios

### 1. Authentication Testing

```bash
# Test successful login
curl -X POST https://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "evaluator",
    "password": "Evaluator123!"
  }'

# Test failed login
curl -X POST https://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "evaluator",
    "password": "WrongPassword"
  }'
```

### 2. Authorization Testing

```bash
# Test EVALUATOR role - should succeed
curl -X POST https://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer <evaluator-token>" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -H "Content-Type: application/json" \
  -d '{
    "libraryId": "library-uuid",
    "patientId": "Patient/123"
  }'

# Test VIEWER role - should fail (no execute permission)
curl -X POST https://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer <viewer-token>" \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  -H "Content-Type: application/json" \
  -d '{
    "libraryId": "library-uuid",
    "patientId": "Patient/123"
  }'
```

### 3. Tenant Isolation Testing

```bash
# Test access to own tenant - should succeed
curl https://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer <evaluator-token>" \
  -H "X-Tenant-ID: DEMO_TENANT_001"

# Test access to another tenant - should fail
curl https://localhost:8081/cql-engine/api/v1/cql/evaluations \
  -H "Authorization: Bearer <evaluator-token>" \
  -H "X-Tenant-ID: DEMO_TENANT_002"
```

### 4. Account Locking Testing

```bash
# Make 5 failed login attempts
for i in {1..5}; do
  curl -X POST https://localhost:8081/cql-engine/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "usernameOrEmail": "evaluator",
      "password": "WrongPassword"
    }'
done

# Attempt login with correct password - should fail (account locked)
curl -X POST https://localhost:8081/cql-engine/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "evaluator",
    "password": "Evaluator123!"
  }'
```

### 5. Multi-Role Permission Testing

```bash
# Test with multi-role user (EVALUATOR + ANALYST)
# Should have both execute and analytics permissions
curl https://localhost:8081/cql-engine/api/v1/analytics/dashboard \
  -H "Authorization: Bearer <multiuser-token>" \
  -H "X-Tenant-ID: DEMO_TENANT_001"
```

## Account Security Features

### Password Policy

All demo accounts use strong passwords that meet the following requirements:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character

### Account Locking

- Accounts lock after **5 consecutive failed login attempts**
- Lock duration: **15 minutes**
- Failed attempt counter resets on successful login

### Password Hashing

- Algorithm: **BCrypt**
- Strength: **12 rounds** (2^12 iterations)
- Automatic salting for each password

## API Endpoints for User Management

### Authentication Endpoints

```bash
# Login
POST /api/v1/auth/login
Body: { "usernameOrEmail": "string", "password": "string" }

# Logout
POST /api/v1/auth/logout
Headers: Authorization: Bearer <token>

# Refresh token
POST /api/v1/auth/refresh
Headers: Authorization: Bearer <refresh-token>
```

### User Management Endpoints (Admin only)

```bash
# List all users
GET /api/v1/users
Headers: Authorization: Bearer <admin-token>

# Get user by ID
GET /api/v1/users/{userId}
Headers: Authorization: Bearer <admin-token>

# Create user
POST /api/v1/users
Headers: Authorization: Bearer <admin-token>
Body: { user object }

# Update user
PUT /api/v1/users/{userId}
Headers: Authorization: Bearer <admin-token>
Body: { user object }

# Deactivate user
DELETE /api/v1/users/{userId}
Headers: Authorization: Bearer <admin-token>
```

## Enabling/Disabling Demo Accounts

### Enable Demo Accounts

Set the Spring profile to include `dev`, `demo`, or `local`:

```bash
# In application.yml or via environment variable
SPRING_PROFILES_ACTIVE=dev

# Or via command line
java -jar cql-engine-service.jar --spring.profiles.active=dev
```

### Disable Demo Accounts

For production, use the `production` profile:

```bash
SPRING_PROFILES_ACTIVE=production
```

Demo accounts will NOT be created when using the production profile.

## Database Schema

Demo accounts are stored in the following tables:

```sql
-- Users table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- User roles (many-to-many)
CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- User tenants (many-to-many)
CREATE TABLE user_tenants (
    user_id UUID REFERENCES users(id),
    tenant_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, tenant_id)
);
```

## Troubleshooting

### Demo accounts not created

**Check Spring profile:**
```bash
# Verify active profile
curl http://localhost:8081/cql-engine/actuator/env | jq '.activeProfiles'
```

**Solution**: Ensure profile is set to `dev`, `demo`, or `local`.

### Cannot login with demo credentials

**Possible causes:**
1. Account is locked due to failed attempts (wait 15 minutes)
2. Database migration not run (check Liquibase logs)
3. Wrong Spring profile (production profile doesn't create demo accounts)

**Check account status:**
```sql
SELECT username, active, account_locked_until, failed_login_attempts
FROM users
WHERE username = 'evaluator';
```

### Password not working

**Verify password:**
- Passwords are case-sensitive
- Copy-paste from this document to avoid typos
- Check for extra spaces

## Production Recommendations

1. **Never use demo accounts in production**
2. **Disable demo data initializer in production profile**
3. **Create production users with secure, unique passwords**
4. **Implement proper user onboarding and provisioning**
5. **Enable audit logging for all user operations**
6. **Regularly review and rotate credentials**
7. **Implement MFA for administrative accounts**

## Additional Resources

- [Security Configuration Guide](./PRODUCTION_SECURITY_GUIDE.md)
- [Authentication & Authorization Architecture](./AUTHENTICATION_ARCHITECTURE.md)
- [User Management API Documentation](./API_DOCUMENTATION.md)

---

**Last Updated**: 2025-01-06
**Version**: 1.0
**Maintainer**: HealthData Development Team
