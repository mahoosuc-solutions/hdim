# Phase 3 Deployment - Authentication Issue Analysis

**Status**: CRITICAL BLOCKING ISSUE
**Date**: 2026-01-01
**Impact**: All API endpoints return authentication errors despite valid JWT tokens

## Executive Summary

Phase 3 services are successfully deployed in Docker and report healthy status on health check endpoints. However, all API endpoint access is blocked by authentication failures:

- **Direct service access** (ports 8087, 8086): HTTP 403 Forbidden
- **Gateway access** (port 8080): HTTP 401 Unauthorized / 500 Internal Server Error

Root cause: Gateway JWT signature validation is failing despite using the correct secret key and token structure.

## Architecture Overview

The system uses a **Gateway Trust Authentication** pattern:

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│   Client    │ JWT     │    Gateway   │ Trust   │ Backend Service │
│             │────────>│   (Port 80)  │────────>│   (Port 8087+)  │
└─────────────┘         └──────────────┘         └─────────────────┘
                        ✓ Validates JWT        ✓ Trusts X-Auth-*
                        ✓ Injects headers        ✓ Rejects bare JWT
```

**Key Points:**
- Gateway validates JWT and converts it to X-Auth-* headers
- Backend services trust these headers (don't re-validate JWT)
- Direct service access returns 403 because services don't validate JWT
- Gateway access requires valid JWT signature

## Deployment Status

### Services Running
| Service | Port | Status | Health |
|---------|------|--------|--------|
| Quality Measure | 8087 | ✅ Running | UP |
| Care Gap | 8086 | ✅ Running | UP |
| Gateway | 8080 | ✅ Running | UP |
| PostgreSQL | 5435 | ✅ Running | UP |
| Redis | 6380 | ✅ Running | UP |
| Kafka | 9094 | ✅ Running | UP |

### Health Checks Passing
All services report `{"status":"UP"}` on `/actuator/health` endpoints.

### API Endpoints Failing
All authenticated API endpoints return authentication errors.

## Problem Analysis

### Issue #1: Gateway JWT Signature Validation Failing

**Symptom:**
```
WARN c.h.a.service.JwtTokenService: Invalid JWT signature:
JWT signature does not match locally computed signature.
JWT validity cannot be asserted and should not be trusted.
```

**Test Data:**
```
JWT Secret:     mySecretKeyForJWTShouldBeAtLeast256BitsLongForHS256Algorithm123456
Algorithm:      HS256
Issuer:         healthdata-in-motion
Audience:       healthdata-api
Token created:  2026-01-01 23:24:16 UTC
Token expires:  2026-01-02 00:24:16 UTC
```

**Investigation:**
1. Token generated using PyJWT library with above secret → signature valid when validated with PyJWT
2. Same token sent to gateway → gateway rejects signature as invalid
3. Token structure is correct: `<header>.<payload>.<signature>`
4. All claims (iss, aud, exp, iat) match expected values

**Possible Root Causes:**
1. **Library Version Difference**: PyJWT (client) vs jjwt/nimbus-jose (gateway) may produce different signatures for the same input
2. **Secret Key Encoding**: Different character encoding (UTF-8 vs ASCII) could cause signature mismatch
3. **Signature Algorithm**: Different interpretation of HS256 algorithm parameters
4. **Key Derivation**: Gateway may be using a derived key rather than the secret directly
5. **Byte Order**: Endianness or byte ordering differences between implementations

### Issue #2: Null Pointer After Auth Failure

**Symptom:**
```
ERROR c.h.g.auth.GatewayAuthenticationFilter: Authentication error
for path /quality-measure/api/v1/measures:
Cannot invoke "String.length()" because "<parameter1>" is null
```

**Analysis:**
1. JWT signature validation fails (Issue #1)
2. Filter attempts to process authentication result
3. Null pointer encountered when accessing String property
4. Suggests defensive programming issues in error handling

**Impact:**
- Client receives HTTP 500 Internal Server Error
- Difficult to diagnose because error message doesn't indicate JWT validation failure

### Issue #3: Direct Service Correctly Rejects Requests

**Symptom:**
```
curl -H "Authorization: Bearer {JWT}" http://localhost:8087/...
Response: HTTP 403 Forbidden
```

**Analysis:**
This behavior is CORRECT per the architecture design:
- Services are configured to trust gateway headers, not validate JWT directly
- Without X-Auth-* headers from gateway, services reject request with 403
- This is proper security: prevents bypass of gateway validation

## Data Collection

### Test Results

#### Test 1: Gateway with JWT
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8080/quality-measure/api/v1/measures?page=1&pageSize=5

Response Headers:
  HTTP/1.1 401 Unauthorized
  Content-Type: application/json

Response Body:
  {"error":"unauthorized","message":"Authentication required"}
```

#### Test 2: Direct Service with JWT
```bash
curl -H "Authorization: Bearer <TOKEN>" \
  -H "X-Tenant-ID: TENANT001" \
  http://localhost:8087/quality-measure/api/v1/measures?page=1&pageSize=5

Response Headers:
  HTTP/1.1 403 Forbidden

Response Body:
  (empty)
```

### Gateway Configuration

From environment inspection:
```
JWT_SECRET=mySecretKeyForJWTShouldBeAtLeast256BitsLongForHS256Algorithm123456
Auth Rate Limit: login=10/min, refresh=20/min, block=300s
Public Paths: 13 default + 5 configured
Gateway Security: enabled=true, enforced=true
OAuth2: disabled
SMART on FHIR: disabled
```

### JWT Configuration (from logs)
```
Issuer: healthdata-in-motion
Audience: healthdata-api
Access Token Expiration: PT15M (15 minutes)
Refresh Token Expiration: PT168H (7 days)
Authentication Provider: authenticationProvider
```

## Options for Resolution

### Option 1: Fix JWT Library Compatibility (Recommended)
**Approach:**
- Identify the specific JWT library used by gateway (likely jjwt or nimbus-jose-jwt)
- Check if gateway is using a custom key derivation function
- Regenerate tokens using the same library/settings as gateway
- Verify signature validation with test vectors

**Pros:**
- Permanent fix
- Allows legitimate API access
- Maintains security model

**Cons:**
- Requires source code access to gateway
- May need to patch JAR file
- Could be complex if custom key derivation exists

### Option 2: Enable Development Mode (If Available)
**Approach:**
- Look for environment variable like `GATEWAY_AUTH_DEV_MODE=true`
- Check if gateway supports JWT signature verification bypass
- Enable dev mode to allow token-based testing

**Pros:**
- Quick workaround for testing
- No code changes needed

**Cons:**
- May not be available
- Reduces security
- Not suitable for production

### Option 3: Use Gateway's Token Endpoint (If Available)
**Approach:**
- Find gateway's `/oauth/token` or `/auth/token` endpoint
- Exchange credentials for valid JWT
- Use the token returned by gateway (guaranteed to be valid)

**Pros:**
- Tokens would be valid (generated by gateway itself)
- Simpler than rebuilding JWT

**Cons:**
- Need credentials for test account
- May not be exposed/available

### Option 4: Disable Gateway JWT Validation in Docker Compose
**Approach:**
- Modify docker-compose.yml to disable gateway authentication
- Restart gateway service
- Test APIs through gateway

**Pros:**
- Quick temporary fix
- Allows API testing to proceed

**Cons:**
- Disables security
- Not suitable for production
- May hide real issues

### Option 5: Access Public Endpoints
**Approach:**
- Identify the 13 default public paths registered by gateway
- Find which endpoints don't require authentication
- Use public endpoints for testing

**Pros:**
- No code changes needed
- Maintains security model
- Legitimate API access

**Cons:**
- Limited functionality
- May not exist for core business logic

## Current Workarounds

### Health Checks (Working)
```bash
curl http://localhost:8087/quality-measure/actuator/health
# Returns: {"status":"UP", ...}

curl http://localhost:8086/care-gap/actuator/health
# Returns: {"status":"UP", ...}
```

These endpoints are public and work correctly, confirming:
- Services are running
- Network connectivity is valid
- Basic HTTP access works

### What Doesn't Work
- All `/api/v1/*` endpoints through gateway (401)
- All `/api/v1/*` endpoints directly (403)
- Token endpoints (`/auth/token`, `/oauth/token`)
- Swagger UI endpoints (if protected)

## Recommendations

### Immediate (Today)
1. **Document the Issue** ✅ (This document)
2. **Test Public Endpoints** - Identify what's accessible without auth
3. **Check Gateway Logs** - Look for JWT library info in startup logs
4. **Investigate Token Endpoint** - Try different token generation approaches

### Short Term (This Week)
1. **Fix JWT Library Compatibility** - Identify the gateway's JWT library and replicate its behavior
2. **Enable Development Mode** - If available, use for testing
3. **Build Token Generator** - Create tool that generates valid JWTs matching gateway expectations

### Medium Term (Before Production)
1. **Security Audit** - Review authentication/authorization implementation
2. **Integration Tests** - Add tests that verify full auth flow (client → gateway → service)
3. **Documentation** - Document the authentication model clearly for developers

## Files to Review

### Gateway Related
- `backend/platform/auth/*` - Authentication module
- Gateway service logs (currently accessible via Docker)
- Gateway JAR file (for library inspection)

### Services Related
- `backend/modules/shared/infrastructure/authentication/*` - TrustedHeaderAuthFilter
- `backend/modules/services/*/src/main/java/*/config/SecurityConfig.java` - Service security config

### Deployment Related
- Docker compose configuration (location: TBD)
- Service startup logs
- Network configuration

## Testing Commands for Reference

```bash
# Generate fresh JWT
python3 << 'EOF'
import jwt
from datetime import datetime, timedelta

secret = "mySecretKeyForJWTShouldBeAtLeast256BitsLongForHS256Algorithm123456"
payload = {
    "sub": "test@example.com",
    "iss": "healthdata-in-motion",
    "aud": "healthdata-api",
    "exp": datetime.utcnow() + timedelta(hours=1),
    "iat": datetime.utcnow(),
    "tenant_id": "TENANT001",
    "roles": ["ADMIN", "EVALUATOR", "ANALYST"]
}
print(jwt.encode(payload, secret, algorithm="HS256"))
EOF

# Test gateway
TOKEN=$(python3 ... as above ...)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/quality-measure/api/v1/measures?page=1&pageSize=5

# Test direct service
curl -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001" \
  http://localhost:8087/quality-measure/api/v1/measures?page=1&pageSize=5

# Check service health (no auth)
curl http://localhost:8087/quality-measure/actuator/health
curl http://localhost:8086/care-gap/actuator/health
```

## Impact Assessment

**Critical**: API testing cannot proceed
**Blocks**: Phase 3 demonstration and validation
**Affects**: Quality Measure Service, Care Gap Service (all authenticated endpoints)

## Status Tracking

| Item | Status | Notes |
|------|--------|-------|
| Services deployed | ✅ Complete | All services running, healthy |
| Services built | ✅ Complete | Docker images built successfully |
| Health checks | ✅ Complete | All services report UP |
| API Testing | ❌ Blocked | Authentication failures on all endpoints |
| Public endpoints | ⚠️ Partial | Health check endpoints work |
| Gateway JWT validation | ❌ Failing | Signature validation rejects valid tokens |
| Backend service security | ✅ Working | Correctly rejecting unauthenticated requests |

## Next Steps

1. Review this analysis with the team
2. Identify JWT library used by gateway from source code or JAR inspection
3. Determine if gateway has JWT signature validation issue or our token generation is wrong
4. Implement fix (Options 1-5 above)
5. Retest all endpoints
6. Generate updated test report

---

**Generated**: 2026-01-01 23:30 UTC
**Analyzed by**: Claude Code
**Ticket**: Phase 3 Authentication Issue
