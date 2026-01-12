# Authentication Integration Fix Guide

## Issue

Microservices were failing to start with error:
```
Error creating bean with name 'com.healthdata.authentication.filter.JwtAuthenticationFilter':
Unsatisfied dependency expressed through constructor parameter 1:
No qualifying bean of type 'com.healthdata.authentication.service.CookieService' that could not be found.
```

## Root Cause

The Two-Tier Authentication Architecture requires microservices to:
1. **Include** Tier 1 components: `JwtTokenService`, `JwtAuthenticationFilter`, `CookieService`
2. **Exclude** Tier 2 components: Controllers that require database repositories

When scanning `com.healthdata.authentication` package without exclusions, Spring was loading authentication controllers (`ApiKeyController`, `AuthController`, etc.) which require database-dependent services like `ApiKeyService`, `UserRepository`, etc.

## Solution Pattern

All microservices (except Gateway) must use this component scan pattern:

```java
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.healthdata.{service}",      // Service's own package
        "com.healthdata.authentication"   // For Tier 1: JwtAuthenticationFilter, JwtTokenService, CookieService
    },
    excludeFilters = {
        // Exclude Tier 2 authentication controllers (Gateway-only)
        // These require database repositories (UserRepository, ApiKeyRepository, etc.)
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.authentication\\.controller\\..*"
        )
    }
)
```

### Required Imports

```java
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
```

## What Gets Loaded (Tier 1 Components)

✅ **Included** from `com.healthdata.authentication`:
- `JwtTokenService` - Token parsing and validation
- `JwtAuthenticationFilter` - HTTP request authentication filter
- `CookieService` - Secure cookie management for JWT tokens
- `JwtConfig` - JWT configuration properties
- `AuthenticationAutoConfiguration` - Auto-configuration

❌ **Excluded** from `com.healthdata.authentication.controller`:
- `ApiKeyController` - requires `ApiKeyService` + `ApiKeyRepository`
- `AuthController` - requires `UserRepository`, `RefreshTokenService`
- `MfaController` - requires `MfaService` + `UserRepository`
- `OAuth2Controller` - Gateway-only OAuth flows
- `SmartOnFhirController` - SMART-on-FHIR authentication

## Implementation Steps

### 1. Update Service Application Class

**File**: `src/main/java/com/healthdata/{service}/{Service}ServiceApplication.java`

**Before**:
```java
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.{service}"
    }
)
```

**After**:
```java
@SpringBootApplication
@ComponentScan(
    basePackages = {
        "com.healthdata.{service}",
        "com.healthdata.authentication"
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.healthdata\\.authentication\\.controller\\..*"
        )
    }
)
```

### 2. Rebuild Service

```bash
./gradlew :modules:services:{service}-service:bootJar
```

### 3. Rebuild Docker Image

```bash
docker compose build {service}-service
```

### 4. Restart Service

```bash
docker compose up -d {service}-service
```

### 5. Verify Startup

```bash
# Check logs for successful startup
docker logs healthdata-{service}-service --tail 50

# Should see:
#   "Started {Service}ServiceApplication in X.XXX seconds"

# Verify no bean errors:
docker logs healthdata-{service}-service 2>&1 | \
  grep -E "UnsatisfiedDependency|No qualifying bean|could not be found"

# Should return nothing (empty output)
```

### 6. Test Health Endpoint

```bash
curl http://localhost:{port}/{context-path}/actuator/health
```

## Services Already Fixed

- ✅ quality-measure-service (port 8087)

## Services That May Need This Fix

Any service that uses `JwtAuthenticationFilter` but doesn't explicitly configure authentication scanning:

- analytics-service
- care-gap-service
- consent-service
- cql-engine-service
- fhir-service
- patient-service
- sdoh-service
- hcc-service
- prior-auth-service
- qrda-export-service
- predictive-analytics-service
- sales-automation-service
- agent-builder-service
- payer-workflows-service
- event-router-service

## How to Check if Service Needs Fix

```bash
# 1. Check if service scans authentication package
grep -A 10 "@SpringBootApplication" \
  backend/modules/services/{service}-service/src/main/java/*/{Service}ServiceApplication.java | \
  grep "com.healthdata.authentication"

# 2. If YES, check if it has exclude filters
grep -A 10 "@ComponentScan" \
  backend/modules/services/{service}-service/src/main/java/*/{Service}ServiceApplication.java | \
  grep "excludeFilters"

# 3. If authentication is scanned but NO exclude filters → NEEDS FIX
```

## Verification Checklist

After applying fix:

- [ ] Service starts successfully (no bean dependency errors)
- [ ] `JwtAuthenticationFilter` is loaded (check logs or debug)
- [ ] Health endpoint returns HTTP 200
- [ ] Service can authenticate JWT tokens
- [ ] No authentication controllers are loaded (Gateway-only)
- [ ] Service can communicate with dependencies (CQL Engine, FHIR, etc.)
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

## Common Issues

### Issue: "No qualifying bean of type 'com.healthdata.authentication.service.CookieService'"

**Cause**: Not scanning `com.healthdata.authentication` package

**Fix**: Add to `scanBasePackages`

### Issue: "No qualifying bean of type 'com.healthdata.authentication.service.ApiKeyService'"

**Cause**: Scanning authentication controllers without exclusion

**Fix**: Add `excludeFilters` for controller package

### Issue: Service starts but authentication doesn't work

**Cause**: `AuthenticationAutoConfiguration` not loading

**Fix**: Verify `spring.factories` or `@Import(AuthenticationAutoConfiguration.class)`

## Gateway Service Exception

**Important**: The Gateway service DOES NOT use this pattern.

Gateway uses explicit bean configuration in `GatewayAuthenticationConfig`:

```java
@Configuration
@ConditionalOnProperty(name = "authentication.controller.enabled", havingValue = "true")
public class GatewayAuthenticationConfig {

    @Bean
    public LogoutService logoutService(UserRepository userRepository, ...) {
        return new LogoutService(userRepository, ...);
    }

    @Bean
    public MfaService mfaService(UserRepository userRepository) {
        return new MfaService(userRepository);
    }

    // ... other Tier 2 beans
}
```

This allows Gateway to have full authentication stack (Tier 1 + Tier 2) without violating the architecture.

## Architecture Reference

See: `/backend/AUTHENTICATION-ARCHITECTURE.md`

**Key principles**:
- **Tier 1** (All services): JWT validation only, no database
- **Tier 2** (Gateway only): Full auth operations with database
- **Component Scan**: Include Tier 1, exclude Tier 2 controllers

## Testing the Fix

### Unit Test

```java
@SpringBootTest
class AuthenticationIntegrationTest {

    @Autowired(required = false)
    private JwtTokenService jwtTokenService;

    @Autowired(required = false)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired(required = false)
    private CookieService cookieService;

    @Autowired(required = false)
    private ApiKeyController apiKeyController;  // Should NOT be loaded

    @Test
    void shouldLoadTier1Components() {
        assertThat(jwtTokenService).isNotNull();
        assertThat(jwtAuthenticationFilter).isNotNull();
        assertThat(cookieService).isNotNull();
    }

    @Test
    void shouldNotLoadTier2Controllers() {
        assertThat(apiKeyController).isNull();
    }
}
```

### Integration Test

```bash
# Start service
docker compose up -d {service}-service

# Wait for startup
sleep 10

# Test authenticated request
curl -X GET http://localhost:{port}/{context}/api/v1/test \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."

# Should return 401 for invalid token
# Should return 200 for valid token
```

## Rollback Plan

If this fix causes issues:

```bash
# 1. Revert source code change
git checkout HEAD -- backend/modules/services/{service}-service/src/main/java/*/{Service}ServiceApplication.java

# 2. Rebuild
./gradlew :modules:services:{service}-service:bootJar

# 3. Rebuild image
docker compose build {service}-service

# 4. Restart
docker compose up -d {service}-service
```

---

*Last Updated*: December 28, 2025
*Applies To*: HDIM v1.6.0+
*Related*: AUTHENTICATION-ARCHITECTURE.md, CQL_ENGINE_DIAGNOSTIC_PLAN.md
