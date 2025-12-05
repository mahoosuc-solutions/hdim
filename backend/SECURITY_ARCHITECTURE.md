# Security Architecture - Microservices Authentication

## Overview

The HealthData platform uses a **centralized authentication** model with **JWT-based** service-to-service authentication. Authentication logic is intended to be centralized in a Gateway service (planned), while individual microservices validate JWT tokens for inter-service communication.

## Current State (Post-WSL Recovery)

### Centralization Refactor (Commit 09a2299)
- Authentication logic moved from individual services to Gateway service (planned but not yet implemented)
- Removed local authentication components (SecurityConfig, Auth controllers/services) from CQL Engine and Quality Measure services
- Disabled shared authentication module components (TenantAccessFilter, RateLimitingFilter, audit logging, rate limiting)
- Retained JWT token validation for service-to-service communication

### Unified Security Pattern (Current Implementation)

All services now use a **consistent JWT-based security configuration** with two profiles:

#### Test Profile
- **Permits all requests** without authentication
- Enables integration testing without security overhead
- Active when Spring profile = `test`

#### Production Profile (Docker/Dev/Prod)
- **JWT-based authentication** via `JwtAuthenticationFilter`
- **Stateless sessions** (no server-side session storage)
- **Public endpoints:**
  - Health checks: `/actuator/health`, `/actuator/info`
  - API Documentation: `/swagger-ui/**`, `/v3/api-docs/**`
  - Service-specific public endpoints (e.g., `/fhir/metadata` for FHIR service)
- **Protected endpoints:** All other API routes require valid JWT

## Services Security Configuration

### 1. CQL Engine Service
**File:** `modules/services/cql-engine-service/src/main/java/com/healthdata/cql/config/CqlSecurityCustomizer.java`

**Status:** ✅ Active

**Pattern:** JWT-based authentication

**Features:**
- Test profile: permitAll()
- Prod profile: JWT authentication
- Public actuator and Swagger endpoints

---

### 2. Patient Service
**File:** `modules/services/patient-service/src/main/java/com/healthdata/patient/config/PatientSecurityConfig.java`

**Status:** ✅ Active (Re-enabled post-recovery)

**Pattern:** JWT-based authentication

**Previous:** Had TenantAccessFilter and RateLimitingFilter (disabled during centralization)

**TODO:** Re-implement tenant isolation when Gateway service is complete

---

### 3. Care Gap Service
**File:** `modules/services/care-gap-service/src/main/java/com/healthdata/caregap/config/CareGapSecurityConfig.java`

**Status:** ✅ Active (Re-enabled post-recovery)

**Pattern:** JWT-based authentication

**Previous:** Had TenantAccessFilter and RateLimitingFilter (disabled during centralization)

**TODO:** Re-implement tenant isolation when Gateway service is complete

---

### 4. Quality Measure Service
**File:** `modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/QualityMeasureSecurityConfig.java`

**Status:** ✅ Active (Re-enabled post-recovery)

**Pattern:** JWT-based authentication

**Previous:** Simple JWT-based config (no additional filters)

---

### 5. FHIR Service
**File:** `modules/services/fhir-service/src/main/java/com/healthdata/fhir/config/FhirSecurityConfig.java`

**Status:** ✅ Active (Updated post-recovery)

**Pattern:** JWT-based authentication

**Special:** Includes `/fhir/metadata` as public endpoint for FHIR conformance

**Previous:** Had TenantAccessFilter and RateLimitingFilter (removed during recovery fix)

**TODO:** Re-implement tenant isolation when Gateway service is complete

---

### 6. Gateway Service
**Location:** `modules/services/gateway-service/`

**Status:** ⚠️ **Placeholder Only** (No implementation)

**Planned Features:**
- User authentication (login, registration)
- Token issuance (JWT access + refresh tokens)
- Rate limiting
- Tenant access control
- Audit logging
- API routing to backend services

---

### 7-9. Other Services
**Services:** Analytics, Event Processing, Consent

**Status:** 🔴 No security configuration

**Note:** These services may not require authentication if they're internal/async services

---

## Authentication Flow (Planned)

```
┌─────────┐         ┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│ Client  │────────►│   Gateway   │────────►│  CQL Engine  │────────►│  Database   │
└─────────┘  HTTP   └─────────────┘  JWT    └──────────────┘         └─────────────┘
               │           │
               │           ├─ Authenticates user
               │           ├─ Issues JWT token
               │           ├─ Validates tenant access
               │           ├─ Rate limiting
               │           └─ Routes to services
               │
               ▼
         Services validate JWT
         (no local auth needed)
```

## Shared Authentication Module

**Location:** `modules/shared/infrastructure/authentication/`

### Active Components
- ✅ `JwtAuthenticationFilter` - Validates JWT tokens
- ✅ `JwtTokenService` - JWT creation and validation
- ✅ `JwtConfig` - JWT configuration properties
- ✅ `AuthenticationAutoConfiguration` - Spring auto-configuration

### Disabled Components (.disabled files)
- 🔴 `TenantAccessFilter` - Multi-tenant isolation (removed)
- 🔴 `RateLimitingFilter` - Brute force protection (removed)
- 🔴 `CustomUserDetailsService` - User authentication (moved to Gateway)
- 🔴 `AuditLoggingAspect` - Audit logging (moved to Gateway)
- 🔴 `RefreshTokenService` - Token refresh (moved to Gateway)
- 🔴 All auth database entities and repositories (moved to Gateway)

**Reason:** These components require database access and user management, which is centralized in Gateway

---

## JWT Configuration

### Application Properties (Test Profile)
All test configurations include JWT properties:

```yaml
jwt:
  secret: test_jwt_secret_key_for_testing_only_minimum_256_bits_required_for_hs512_algorithm_this_is_long_enough
  access-token-expiration: 1h
  refresh-token-expiration: 1d
  issuer: healthdata-in-motion-test
  audience: healthdata-api-test
```

**Files Updated:**
- `cql-engine-service/src/test/resources/application-test.yml`
- `patient-service/src/test/resources/application-test.yml`
- `care-gap-service/src/test/resources/application-test.yml`
- `quality-measure-service/src/test/resources/application-test.yml`
- `fhir-service/src/test/resources/application-test.yml`
- `authentication/src/test/resources/application-test.yml`

### Bean Creation Fix (Commit e69b096)

**Problem:** `JwtTokenService` bean was not being created because `AuthenticationAutoConfiguration` had incorrect ComponentScan path.

**Before:**
```java
@ComponentScan(basePackages = {
    "com.healthdata.authentication.filter",
    "com.healthdata.authentication.service.JwtTokenService"  // WRONG - specific class
})
```

**After:**
```java
@ComponentScan(basePackages = {
    "com.healthdata.authentication.filter",
    "com.healthdata.authentication.service",  // FIXED - package scan
    "com.healthdata.authentication.config"
})
```

---

## Migration History

### Phase 18 (Part 4) - Original Authentication Implementation
- Added authentication to patient, FHIR, and care-gap services
- Implemented TenantAccessFilter for multi-tenant isolation
- Implemented RateLimitingFilter for brute force protection
- Full local authentication in each service

### Commit 09a2299 - Centralization Refactor
- Removed local authentication from CQL Engine and Quality Measure services
- Disabled shared authentication module components
- Retained JWT validation for service-to-service auth
- Planned Gateway service for centralized authentication

### Commit e69b096 - WSL Recovery Fixes
- Fixed JWT configuration in test profiles
- Fixed `AuthenticationAutoConfiguration` component scan
- Simplified FHIR service security config

### Current Commit - Unified Security Pattern
- Re-enabled security configs for patient, care-gap, quality-measure services
- Updated all services to use consistent JWT-based pattern
- Removed references to disabled filters (TenantAccessFilter, RateLimitingFilter)
- Unified pattern across all services

---

## Security Features

### ✅ Currently Active
1. **JWT Authentication** - Service-to-service communication
2. **Stateless Sessions** - No server-side session storage
3. **Public Endpoints** - Health checks, Swagger documentation
4. **Test Profile** - Authentication disabled for integration tests
5. **CORS Configuration** - (Service-specific)

### 🔴 Disabled (Awaiting Gateway Implementation)
1. **User Authentication** - Login, registration endpoints
2. **Tenant Access Control** - Multi-tenant isolation filter
3. **Rate Limiting** - Brute force protection
4. **Audit Logging** - Authentication event logging
5. **Refresh Tokens** - Long-lived session tokens

### 📋 Planned (Future Gateway Service)
1. **API Gateway** - Single entry point for all client requests
2. **Token Issuance** - JWT creation and management
3. **User Management** - User registration, password reset
4. **Role-Based Access Control (RBAC)** - Fine-grained permissions
5. **OAuth2 Support** - External identity providers

---

## Testing

### Test Profile Behavior
When `spring.profiles.active=test`:
- **All authentication bypassed** - permitAll() for all endpoints
- **No JWT validation** - Filter chain skips JWT filter
- **Faster test execution** - No security overhead

### Production Testing
To test with JWT authentication:
1. Set profile to `docker`, `dev`, or `prod`
2. Ensure JWT secret is configured
3. Obtain valid JWT token from Gateway (when implemented)
4. Include `Authorization: Bearer <token>` header in requests

---

## Security Best Practices

### ✅ Implemented
- Stateless JWT authentication
- CSRF disabled for REST APIs (JWT is CSRF-resistant)
- Consistent security configuration across services
- Environment-specific configuration (test vs prod)
- Public health check endpoints for orchestration

### ⚠️ Missing (TODO)
- Tenant isolation enforcement
- Rate limiting for brute force protection
- Audit logging for security events
- Token refresh mechanism
- User session management

---

## Next Steps

### High Priority
1. **Implement Gateway Service**
   - User authentication endpoints
   - JWT token issuance
   - API routing to backend services

2. **Re-implement Tenant Access Control**
   - Create new `TenantAccessFilter` in Gateway
   - Validate tenant claims in JWT
   - Enforce isolation at Gateway level

3. **Re-implement Rate Limiting**
   - Create new `RateLimitingFilter` in Gateway
   - Redis-based rate limiting
   - Configurable limits per endpoint

### Medium Priority
4. **Add Security Integration Tests**
   - Test JWT validation
   - Test unauthorized access rejection
   - Test token expiration

5. **Implement Token Refresh**
   - Refresh token storage
   - Token rotation
   - Revocation mechanism

### Low Priority
6. **Add Role-Based Access Control (RBAC)**
   - User roles and permissions
   - Endpoint-level authorization
   - Admin vs user access

7. **OAuth2 Integration**
   - External identity providers
   - Social login support
   - Enterprise SSO

---

## References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://datatracker.ietf.org/doc/html/rfc8725)
- [API Gateway Pattern](https://microservices.io/patterns/apigateway.html)
- [OAuth 2.0 Framework](https://datatracker.ietf.org/doc/html/rfc6749)

---

## Related Files

### Audit System
- See `AUDIT_ARCHITECTURE.md` for audit logging architecture (currently disabled)
- See `modules/services/cql-engine-service/AUDIT_SYSTEM_README.md` for CQL audit implementation

### Deployment
- See `BUILD_DEPLOYMENT_SUMMARY.md` for deployment procedures
- See `DEPLOYMENT_COMPLETE.md` for infrastructure setup

---

*Last Updated: 2025-11-08 (Post-WSL Recovery)*
*Author: Claude Code*
