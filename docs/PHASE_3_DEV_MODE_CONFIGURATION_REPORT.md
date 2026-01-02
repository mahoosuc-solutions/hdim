# Phase 3 Dev Mode Configuration - Investigation Report

**Status**: INVESTIGATION COMPLETE - ROOT CAUSE IDENTIFIED
**Date**: 2026-01-02
**Issue**: JWT signature validation failures preventing API access

## Executive Summary

An attempt was made to enable development mode in the gateway service to bypass JWT signature validation. While the configuration option exists in documentation, the compiled JAR file does not recognize the environment variables, preventing the dev mode from being activated.

**Root Cause**: The gateway service source code is not available in the repository. Without source code access, the JAR file cannot be modified to recognize development mode environment variables.

## Investigation Process

### Step 1: Identified Dev Mode Configuration Option
**Finding**: The Explore agent discovered that dev mode should be configurable via:
```bash
GATEWAY_AUTH_DEV_MODE=true  # Environment variable format
```

**Source**: Documented in `docs/administrator/IMPLEMENTATION-SETUP-GUIDE.md` (line 779)

**Configuration Property**: `gateway.auth.dev-mode=true` (YAML format)

### Step 2: Attempted Dev Mode Activation
**Method 1: Direct Environment Variable**
```bash
docker run ... -e GATEWAY_AUTH_DEV_MODE=true ...
```
**Result**: Not recognized by gateway service ❌

**Method 2: SPRING Prefix Format**
```bash
docker run ... -e SPRING_GATEWAY_AUTH_DEV_MODE=true ...
```
**Result**: Not recognized by gateway service ❌

**Method 3: Check Logs for Dev Mode Indicators**
```
grep -i "dev.*mode" gateway logs
```
**Result**: No indication of dev mode being loaded ❌

### Step 3: Diagnosed Root Cause

**Gateway Container Details:**
- Image: `healthdata-gateway-service:1.6.0`
- Status: ✅ Running and healthy
- Configuration: Compiled JAR file
- Source Code: ❌ Not available in repository

**Investigation Results:**
1. ❌ No `GatewayAuthenticationFilter.java` in backend source
2. ❌ No `JwtConfig.java` in backend source
3. ❌ No gateway service source directory found
4. ✅ Gateway documentation exists and references dev mode
5. ✅ Compiled JAR exists but won't recognize new env vars

**Conclusion**: The JAR file was compiled with a specific set of configuration properties. Since it doesn't recognize the `GATEWAY_AUTH_DEV_MODE` property, either:
- The property was never implemented in the source code
- The property exists but isn't being read from environment variables
- The JAR was built from old source code that predates this feature

## Token Expiration Issue Discovered

During testing, we also discovered the initial test token had **expired**. Generated fresh token with 2-hour validity:

```
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0LXVzZXJAZXhhbXBsZS5jb20iLCJuYW1lIjoiVGVzdCBVc2VyIiwiaXNzIjoiaGVhbHRoZGF0YS1pbi1tb3Rpb24iLCJhdWQiOiJoZWFsdGhkYXRhLWFwaSIsImV4cCI6MTc2NzMyMDc0NSwiaWF0IjoxNzY3MzEzNTQ1LCJ0ZW5hbnRfaWQiOiJURU5BTlQwMDEiLCJyb2xlcyI6WyJFVkFMVUFUT1IiLCJBTkFMWVNUIiwiQURNSU4iXX0.v7WPOHzUqn0ME74aHdNZoqSmLqXksCPvnqbuWOgZLJ8
```

**Result with Fresh Token**: Still returns JWT signature validation error ❌

This confirms the issue is not token expiration but genuine signature validation failure.

## Current API Access Status

### Gateway Service (Port 8080)
| Endpoint | Auth Required | Status |
|----------|---------------|--------|
| `/quality-measure/api/v1/measures` | Yes | ❌ 500 (JWT signature failed) |
| `/care-gap/api/v1/care-gaps` | Yes | ❌ 500 (JWT signature failed) |
| `/v3/api-docs` | No | ✅ 200 (OpenAPI docs available) |
| `/swagger-ui/index.html` | No | ✅ 200 (Swagger UI accessible) |
| `/actuator/health` | No | ✅ 200 (Health endpoint public) |
| `/oauth/token` | Yes | ❌ 401 (Requires auth) |
| `/auth/token` | Yes | ❌ 401 (Requires auth) |
| `/api/auth/login` | Yes | ❌ 401 (Requires auth) |

### Direct Service Access (Port 8087/8086)
| Endpoint | Auth Required | Status |
|----------|---------------|--------|
| `/quality-measure/api/v1/measures` | Yes | ❌ 403 (Missing trust headers) |
| `/quality-measure/actuator/health` | No | ✅ 200 (Health endpoint public) |
| `/care-gap/api/v1/care-gaps` | Yes | ❌ 403 (Missing trust headers) |
| `/care-gap/actuator/health` | No | ✅ 200 (Health endpoint public) |

## Configuration Analysis

### Gateway JWT Configuration (From Logs)
```
Issuer: healthdata-in-motion
Audience: healthdata-api
Access Token Expiration: PT15M (15 minutes)
Refresh Token Expiration: PT168H (7 days)
```

### JWT Token Generated
```json
{
  "sub": "test-user@example.com",
  "iss": "healthdata-in-motion",
  "aud": "healthdata-api",
  "exp": 1767320745,
  "iat": 1767313545,
  "tenant_id": "TENANT001",
  "roles": ["EVALUATOR", "ANALYST", "ADMIN"]
}
```

**Signature Algorithm**: HS256
**Secret**: `mySecretKeyForJWTShouldBeAtLeast256BitsLongForHS256Algorithm123456`

**Result**: Token signature validated by PyJWT library ✅, rejected by gateway ❌

## Possible Explanations for Signature Mismatch

1. **Different JWT Libraries**
   - PyJWT (Python): Correct signature
   - jjwt or nimbus-jose-jwt (Java): Rejects signature
   - Possible: Different HS256 implementation or header encoding

2. **Secret Key Transformation**
   - Gateway might apply transformation to secret key
   - Gateway might use different encoding (UTF-8 vs ASCII)
   - Gateway might derive key using PBKDF2 or similar

3. **Source Code Version Mismatch**
   - JAR compiled from different source code
   - Feature branch code not merged
   - Version 1.6.0 might not have dev mode support

4. **Corrupted or Patched JAR**
   - JAR file might have been modified
   - Signature validation code might be broken

## Options for Resolution

### Option 1: Rebuild Gateway Service (RECOMMENDED)
**Requirements:**
- Access to gateway service source code
- Java 21, Gradle 8.11+
- Build environment with JWT configuration property exposed

**Steps:**
1. Clone/access gateway service source code
2. Find `application.yml` with `gateway.auth` section
3. Verify `dev-mode` property is defined
4. Rebuild JAR with `./gradlew bootJar`
5. Stop old container, deploy new JAR
6. Set `GATEWAY_AUTH_DEV_MODE=true`

**Effort**: Medium (4-8 hours)
**Risk**: Low - controlled rebuild
**Success Probability**: High

### Option 2: Fix JWT Library Compatibility (RESEARCH)
**Requirements:**
- Inspect gateway JAR file
- Identify JWT library and version
- Reverse-engineer signature algorithm
- Create token generator matching library behavior

**Effort**: High (8-16 hours)
**Risk**: High - requires library reverse engineering
**Success Probability**: Medium

### Option 3: Patch JAR File (NOT RECOMMENDED)
**Requirements:**
- Decompile gateway JAR
- Find authentication filter code
- Modify signature validation logic
- Recompile and sign JAR

**Effort**: Very High (16-40 hours)
**Risk**: Very High - could break other functionality
**Success Probability**: Low
**Issues**: Difficult to maintain, security concerns

### Option 4: Use Swagger/Testing Tools (WORKAROUND)
**Requirements:**
- Generate valid token through different method
- Find documentation for test credentials
- Use Swagger UI to test endpoints

**Effort**: Low (1-2 hours)
**Risk**: Low - read-only testing
**Success Probability**: Medium

### Option 5: Configure Alternative Auth (RESEARCH)
**Requirements:**
- Check if OAuth2 or SMART on FHIR available
- Look for OIDC configuration
- Determine if public test account exists

**Note**: Logs show: "OAuth2 authentication is disabled", "SMART on FHIR authorization is disabled"

**Success Probability**: Very Low

## Recommendation

### Immediate (Today)
1. **Document the Issue** ✅ (This document)
2. **Document Workarounds** - Create guide for testing with public endpoints
3. **Identify Source Code Location** - Find where gateway service source is stored

### Short Term (This Week)
1. **Obtain Gateway Source Code** - Critical blocker
2. **Verify Dev Mode Implementation** - Check if feature exists in source
3. **Rebuild Gateway Service** - If source code available
4. **Test with Fresh JWT** - Once gateway rebuilt

### Medium Term (Before Production)
1. **Enable Security Testing Mode** - Instead of dev mode
2. **Create Test Account System** - For automated testing
3. **Document Token Generation** - For developer reference
4. **Implement JWT Validation Tests** - To prevent regressions

## Files Reviewed

| File | Status | Notes |
|------|--------|-------|
| `backend/modules/services/gateway-service/build/resources/main/application.yml` | ✅ Found | JWT configuration |
| `backend/modules/services/gateway-service/build/resources/main/application-prod.yml` | ✅ Found | Production config |
| `backend/modules/services/gateway-service/build/resources/test/application-test.yml` | ✅ Found | Test config |
| `backend/modules/services/gateway-service/src/main/java/**` | ❌ Not Found | Source code |
| `docs/administrator/IMPLEMENTATION-SETUP-GUIDE.md` | ✅ Found | Dev mode documented |

## References

### JWT Configuration Details
- **Location**: `/backend/modules/services/gateway-service/build/resources/main/application.yml`
- **Properties**: JWT secret, issuer, audience, token expiration times

### Gateway Configuration Details
- **Location**: Same file as above
- **Properties**: Auth enabled, enforced, header signing, rate limits

### Authentication Architecture
- **Reference**: `/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Pattern**: Gateway validates JWT → injects trust headers → services trust headers

### Documentation
- **Setup Guide**: `/docs/administrator/IMPLEMENTATION-SETUP-GUIDE.md` (line 779)
- **Environment Variables**: Lists GATEWAY_AUTH_DEV_MODE and GATEWAY_AUTH_SIGNING_SECRET

## Test Results Summary

| Test | Before | After | Status |
|------|--------|-------|--------|
| Token generation | ✅ Success | ✅ Success | N/A |
| Gateway health | ✅ UP | ✅ UP | N/A |
| Service health | ✅ UP | ✅ UP | N/A |
| Gateway JWT validation | ❌ Signature failed | ❌ Signature failed | **UNRESOLVED** |
| Dev mode activation | N/A | ❌ Not recognized | **UNRESOLVED** |
| API access through gateway | ❌ 500 error | ❌ 500 error | **UNRESOLVED** |
| API access direct | ❌ 403 error | ❌ 403 error | Expected (by design) |

## Impact on Phase 3 Testing

**Current Status**: 🔴 BLOCKED
- Cannot test APIs through gateway (500 errors)
- Cannot test APIs directly (403 errors on purpose)
- Can only access public endpoints (health, docs, Swagger)

**Workaround**: Use public endpoints for health verification
```bash
curl http://localhost:8080/v3/api-docs  # Get API documentation
curl http://localhost:8087/quality-measure/actuator/health  # Health check
curl http://localhost:8086/care-gap/actuator/health  # Health check
```

**Full Testing**: Requires resolving JWT signature validation issue

## Next Steps

1. **Find Gateway Source Code**
   ```bash
   find . -type f -name "GatewayAuthenticationFilter.java"
   find . -type f -name "application.yml" -path "*/gateway*"
   ```

2. **Verify Dev Mode Support**
   - Look for `dev-mode` or `devMode` in configuration
   - Check if property is bound to environment variables
   - Verify it's enabled in the build

3. **Rebuild if Possible**
   - If source code found and dev mode property exists
   - Rebuild with `./gradlew bootJar`
   - Deploy new image

4. **Document Finding**
   - Keep this report for reference
   - Update deployment guide with workaround
   - Add troubleshooting section

---

**Generated**: 2026-01-02 00:35 UTC
**Analyzed by**: Claude Code
**Status**: Investigation Complete - Awaiting Source Code Access
