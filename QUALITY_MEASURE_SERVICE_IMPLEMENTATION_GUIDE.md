# Quality Measure Service - Authentication Implementation Guide

**Date:** 2025-11-06
**Priority:** HIGH
**Estimated Time:** 2 hours
**Status:** READY TO IMPLEMENT

## Quick Start

This guide provides step-by-step instructions to implement tenant isolation and authentication in quality-measure-service by duplicating the authentication infrastructure from cql-engine-service.

## Prerequisites

- ✅ cql-engine-service tenant isolation fix applied and tested
- ✅ care-gap-service database configuration updated
- ✅ PostgreSQL database running on port 5435 with healthdata_cql database
- ✅ User tables already exist from cql-engine-service migrations

## Implementation Steps

### Step 1: Copy Authentication Entity Classes (10 minutes)

Copy User and UserRole entities from cql-engine-service:

```bash
# Copy User entity
cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/entity/User.java \
   backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/entity/

# Copy UserRole entity
cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/entity/UserRole.java \
   backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/entity/
```

**Update package names** in both files:
```java
// Change from:
package com.healthdata.cql.entity;

// To:
package com.healthdata.quality.entity;
```

### Step 2: Copy Repository (5 minutes)

```bash
# Copy UserRepository
cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/repository/UserRepository.java \
   backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/repository/
```

**Update package names and imports:**
```java
// Change package:
package com.healthdata.quality.repository;

// Update imports:
import com.healthdata.quality.entity.User;
```

### Step 3: Create Security Package and Copy Security Classes (15 minutes)

```bash
# Create security directory if it doesn't exist
mkdir -p backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/security

# Copy CustomUserDetailsService
cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/security/CustomUserDetailsService.java \
   backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/security/

# Copy TenantAccessFilter
cp backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/security/TenantAccessFilter.java \
   backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/security/
```

**Update package names and imports** in both files:
```java
// CustomUserDetailsService.java
package com.healthdata.quality.security;

import com.healthdata.quality.entity.User;
import com.healthdata.quality.repository.UserRepository;

// TenantAccessFilter.java
package com.healthdata.quality.security;

import com.healthdata.quality.entity.User;
import com.healthdata.quality.security.CustomUserDetailsService;
```

### Step 4: Update SecurityConfig (20 minutes)

Edit `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/SecurityConfig.java`:

**Add imports:**
```java
import com.healthdata.quality.security.CustomUserDetailsService;
import com.healthdata.quality.security.TenantAccessFilter;
```

**Add fields to class:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    @Autowired
    private Environment environment;

    private final CustomUserDetailsService userDetailsService;  // ADD THIS
    private final TenantAccessFilter tenantAccessFilter;        // ADD THIS
```

**Update securityFilterChain method** to add tenant filter:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable);

    // Check if we're in test mode
    boolean isTestMode = Arrays.asList(environment.getActiveProfiles()).contains("test");

    if (isTestMode) {
        // Test mode: permit all requests
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    } else {
        // Production mode: require authentication
        http
            .authorizeHttpRequests(auth -> auth
                // Health endpoints - accessible without authentication
                .requestMatchers("/api/v1/health/**", "/actuator/health/**").permitAll()

                // Authentication endpoints - accessible without authentication
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/health").permitAll()

                // Actuator endpoints - require authentication
                .requestMatchers("/actuator/**").authenticated()

                // API documentation endpoints - accessible without authentication
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // All other API endpoints require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {})
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // ADD TENANT ACCESS FILTER (USE CORRECT FILTER CLASS!)
            .addFilterAfter(tenantAccessFilter, org.springframework.security.web.authentication.www.BasicAuthenticationFilter.class);
    }

    return http.build();
}
```

**Add authentication provider methods** (if not already present):
```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

@Bean
public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
}

@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
}
```

### Step 5: Update Database Configuration (5 minutes)

Edit `backend/modules/services/quality-measure-service/src/main/resources/application.yml`:

```yaml
datasource:
  url: jdbc:postgresql://localhost:5435/healthdata_cql  # Changed from healthdata_quality_measure
  username: healthdata
  password: ${DB_PASSWORD:dev_password}
```

Add comment explaining shared database:
```yaml
datasource:
  # Shared database with cql-engine-service for user authentication
  url: jdbc:postgresql://localhost:5435/healthdata_cql
  username: healthdata
  password: ${DB_PASSWORD:dev_password}
```

### Step 6: Build and Verify (10 minutes)

```bash
# Build the service
cd backend
./gradlew :modules:services:quality-measure-service:build

# Check for compilation errors
# Should compile successfully with no errors
```

### Step 7: Start Service and Run Migrations (15 minutes)

```bash
# Start quality-measure-service
cd backend
SPRING_PROFILES_ACTIVE=dev,local \
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/healthdata_cql \
SPRING_DATASOURCE_USERNAME=healthdata \
SPRING_DATASOURCE_PASSWORD=dev_password \
./gradlew :modules:services:quality-measure-service:bootRun
```

**Expected output:**
- Service starts on port 8087
- Liquibase runs and creates quality_measure_results table
- No errors about missing user tables (they already exist)
- Log shows: "Started QualityMeasureServiceApplication in X seconds"

### Step 8: Test Authentication (20 minutes)

**Test 1: No authentication → 401**
```bash
curl -i http://localhost:8087/quality-measure/api/v1/quality-measures

# Expected: HTTP/1.1 401 Unauthorized
```

**Test 2: Valid authentication → 200**
```bash
curl -i -u viewer:Viewer123! \
  -H "X-Tenant-ID: DEMO_TENANT_001" \
  http://localhost:8087/quality-measure/api/v1/quality-measures

# Expected: HTTP/1.1 200 OK (or 404 if no quality measures exist)
```

**Test 3: Unauthorized tenant → 403**
```bash
curl -i -u viewer:Viewer123! \
  -H "X-Tenant-ID: UNAUTHORIZED_TENANT" \
  http://localhost:8087/quality-measure/api/v1/quality-measures

# Expected: HTTP/1.1 403 Forbidden
# Message: "Access denied to tenant: UNAUTHORIZED_TENANT"
```

**Test 4: Wrong authorized tenant → 403**
```bash
curl -i -u viewer:Viewer123! \
  -H "X-Tenant-ID: DEMO_TENANT_002" \
  http://localhost:8087/quality-measure/api/v1/quality-measures

# Expected: HTTP/1.1 403 Forbidden
# Message: "Access denied to tenant: DEMO_TENANT_002"
```

**Test 5: Multi-tenant user → 200**
```bash
curl -i -u multitenant:MultiTenant123! \
  -H "X-Tenant-ID: DEMO_TENANT_002" \
  http://localhost:8087/quality-measure/api/v1/quality-measures

# Expected: HTTP/1.1 200 OK
```

### Step 9: Verify Logs (5 minutes)

Check service logs for tenant access filter execution:

```bash
# Should see logs like:
# DEBUG c.h.quality.security.TenantAccessFilter - Request: GET /quality-measure/api/v1/quality-measures
# DEBUG c.h.quality.security.TenantAccessFilter - Authenticated user: viewer
# DEBUG c.h.quality.security.TenantAccessFilter - Tenant ID from header: DEMO_TENANT_001
# DEBUG c.h.quality.security.TenantAccessFilter - User viewer has access to tenant DEMO_TENANT_001

# For unauthorized access:
# WARN c.h.quality.security.TenantAccessFilter - SECURITY: User viewer attempted to access unauthorized tenant: UNAUTHORIZED_TENANT
```

### Step 10: Commit Changes (10 minutes)

```bash
git add backend/modules/services/quality-measure-service/
git commit -m "Phase 17: Implement tenant isolation and authentication in quality-measure-service

Duplicates authentication infrastructure from cql-engine-service to enable
tenant isolation and secure multi-tenant access control.

Changes:
- Added User and UserRole entities (shared database model)
- Added UserRepository for database access
- Added CustomUserDetailsService for Spring Security integration
- Added TenantAccessFilter for tenant isolation enforcement
- Updated SecurityConfig to:
  - Inject CustomUserDetailsService and TenantAccessFilter
  - Add tenant filter AFTER BasicAuthenticationFilter
  - Configure authentication provider with BCrypt password encoder
- Updated application.yml to use shared healthdata_cql database

Database:
- Uses shared database with cql-engine-service (healthdata_cql)
- User tables already exist from cql-engine-service migrations
- quality-measure-service migrations create only measure tables
- Single source of truth for user authentication

Testing:
- ✅ Authentication required (401 without credentials)
- ✅ Tenant isolation enforced (403 for unauthorized tenants)
- ✅ Multi-tenant users can access all authorized tenants
- ✅ Service starts successfully and runs migrations

All services now have production-ready tenant isolation.

Related:
- PHASE_17_SECURITY_AUDIT.md
- PHASE_17_INFRASTRUCTURE_REQUIREMENTS.md
- QUALITY_MEASURE_SERVICE_IMPLEMENTATION_GUIDE.md

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"
```

## Troubleshooting

### Issue: Compilation Errors

**Symptom:** Package does not exist errors
**Solution:** Verify package names are updated correctly in all copied files

### Issue: No bean named 'userDetailsService'

**Symptom:** Spring boot startup fails
**Solution:** Ensure `@RequiredArgsConstructor` is present on SecurityConfig class and CustomUserDetailsService is annotated with `@Service`

### Issue: Filter runs before authentication

**Symptom:** TenantAccessFilter logs show "No authentication"
**Solution:** Verify filter is added AFTER `BasicAuthenticationFilter.class` (not `UsernamePasswordAuthenticationFilter.class`)

### Issue: Database connection fails

**Symptom:** Cannot connect to healthdata_care_gap database
**Solution:** Database URL should be `healthdata_cql` not `healthdata_care_gap`

### Issue: Table 'users' doesn't exist

**Symptom:** SQL error about missing users table
**Solution:**
1. Ensure cql-engine-service has run and created user tables
2. Verify database URL points to healthdata_cql
3. Check Liquibase changelog lock table

## Verification Checklist

- [ ] All files copied successfully
- [ ] Package names updated in all files
- [ ] SecurityConfig updated with tenant filter
- [ ] Database URL points to healthdata_cql
- [ ] Service builds without errors
- [ ] Service starts successfully
- [ ] Liquibase migrations run
- [ ] 401 returned without authentication
- [ ] 403 returned for unauthorized tenants
- [ ] 200 returned for authorized tenants
- [ ] Multi-tenant users can access all their tenants
- [ ] Logs show TenantAccessFilter executing after authentication
- [ ] Changes committed to git

## Files Created/Modified Summary

**New Files:**
```
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/
├── entity/
│   ├── User.java (copied, package updated)
│   └── UserRole.java (copied, package updated)
├── repository/
│   └── UserRepository.java (copied, package updated)
└── security/
    ├── CustomUserDetailsService.java (copied, package updated)
    └── TenantAccessFilter.java (copied, package updated)
```

**Modified Files:**
```
backend/modules/services/quality-measure-service/
├── src/main/java/com/healthdata/quality/config/
│   └── SecurityConfig.java (updated with tenant filter)
└── src/main/resources/
    └── application.yml (database URL updated)
```

## Time Breakdown

- File copying: 10 minutes
- Package name updates: 15 minutes
- SecurityConfig updates: 20 minutes
- Database configuration: 5 minutes
- Build and verify: 10 minutes
- Start service: 15 minutes
- Testing: 20 minutes
- Commit: 10 minutes
- **Total: ~2 hours**

## Post-Implementation

After successful implementation:

1. **Update Service Status Matrix**
   - quality-measure-service: ✅ PRODUCTION READY

2. **Plan Phase 18**
   - Add automated security tests
   - Implement security monitoring
   - Begin planning centralized authentication service

3. **Documentation**
   - Update PHASE_17_SECURITY_AUDIT.md with completion status
   - Document any issues encountered
   - Share learnings with team

---

**Implementation Status:** READY TO START
**Last Updated:** 2025-11-06
**Owner:** Development Team
