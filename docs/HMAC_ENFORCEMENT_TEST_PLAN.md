# Phase 5: HMAC Enforcement Testing with Clinical Portal

## Overview

This document describes the testing plan for HMAC signature enforcement with the clinical portal. When enabled, the gateway will:

1. Validate JWT tokens from HttpOnly cookies
2. Extract user claims from JWT
3. Generate X-Auth-* headers (User-Id, Username, Tenant-Ids, Roles)
4. **Sign all X-Auth-* headers with HMAC-SHA256 using a shared secret**
5. Add `X-Auth-Validated` header containing the HMAC signature

Backend services will validate the HMAC signature to ensure headers weren't tampered with by proxies or unauthorized parties.

## Configuration

### Gateway Configuration (docker-compose.demo.yml)

```yaml
gateway-service:
  environment:
    # HMAC Signing Secret (256-bit base64 encoded)
    GATEWAY_AUTH_SIGNING_SECRET: '2J3YcbuHgPp0xyMq3GHYJ/MNolqB+Hvp4j/fsA6LQYM='
    
    # Gateway Authentication Settings
    GATEWAY_AUTH_ENFORCED: 'true'
    # Other settings remain the same
```

### Expected Behavior

**Dev Mode (No HMAC)** - Current state:
- Gateway generates X-Auth-* headers
- No signature validation
- Backend services trust headers implicitly
- Risk: Headers could be spoofed by unauthorized parties

**Production Mode (HMAC Enabled)** - Phase 5 state:
- Gateway generates X-Auth-* headers
- Adds X-Auth-Validated: HMAC-SHA256 signature
- Backend services validate signature
- Protection: Ensures headers come from trusted gateway

## Test Scenarios

### Test 1: Basic Login and Authentication (POST Phase 5)

**Objective**: Verify login still works with HMAC enforcement

**Steps**:
1. Start demo stack with HMAC enabled: `docker compose -f docker-compose.demo.yml up -d`
2. Navigate to clinical portal: http://localhost:4200
3. Login with demo credentials:
   - Email: demo_admin@hdim.ai
   - Password: demo123

**Expected Result**:
- ✅ Login succeeds
- ✅ HttpOnly cookies set (hdim_access_token, hdim_refresh_token)
- ✅ Redirect to dashboard
- ✅ No CORS errors
- ✅ No authentication errors in browser console

**Verification**:
```bash
docker logs hdim-demo-gateway | grep -i "hmac\|signature"
# Should show successful signature generation
```

### Test 2: Patient List Access (POST Phase 5)

**Objective**: Verify FHIR API calls work with HMAC validation

**Steps**:
1. After login, navigate to "Patients" page
2. Observe patient list loading
3. Click on a patient to view details

**Expected Result**:
- ✅ Patient list loads successfully
- ✅ No 403 Forbidden errors
- ✅ No signature validation failures
- ✅ Data displays correctly

**Verification in Logs**:
```bash
# Check gateway logs for signature generation
docker logs hdim-demo-gateway | grep -i "x-auth-validated"
# Should show: "Generated X-Auth-Validated signature for user: demo_admin"

# Check FHIR service logs for signature validation
docker logs hdim-demo-fhir | grep -i "x-auth-validated"
# Should show: "Signature validation successful"
```

### Test 3: Multi-Tenant Isolation (POST Phase 5)

**Objective**: Verify tenant isolation still enforced with HMAC

**Steps**:
1. Login as demo_admin (DEMO001 tenant)
2. Note patients visible
3. Logout
4. Create test user with different tenant (if available)
5. Login as new user
6. Verify different patient set

**Expected Result**:
- ✅ Tenant isolation maintained
- ✅ No cross-tenant data leakage
- ✅ HMAC validation includes tenant verification

### Test 4: Role-Based Access Control (POST Phase 5)

**Objective**: Verify RBAC still works with HMAC-signed headers

**Test User 1 - ADMIN Role**:
- User: demo_admin@hdim.ai
- Expected: Access to all pages, can edit patients
- HMAC signature includes: X-Auth-Roles: ADMIN,EVALUATOR

**Test User 2 - ANALYST Role** (if available):
- User: demo_analyst@hdim.ai
- Expected: Can view reports but not edit patients
- HMAC signature includes: X-Auth-Roles: ANALYST

**Test User 3 - VIEWER Role** (if available):
- User: demo_viewer@hdim.ai
- Expected: Read-only access
- HMAC signature includes: X-Auth-Roles: VIEWER

**Expected Result**:
- ✅ Each role's permissions enforced
- ✅ Route guards prevent unauthorized access
- ✅ HMAC signatures include correct roles
- ✅ Backend validates role permissions

### Test 5: Token Refresh with HMAC (POST Phase 5)

**Objective**: Verify token refresh works with HMAC enforcement

**Steps**:
1. Login successfully
2. Wait for token refresh (happens automatically every 5 minutes)
3. Observe dashboard continues to work
4. Check logs for signature regeneration

**Expected Result**:
- ✅ Token refresh succeeds silently
- ✅ New HMAC signatures generated
- ✅ No user interruption
- ✅ Session continues normally

**Verification**:
```bash
docker logs hdim-demo-gateway | grep -i "token refresh"
# Should show successful refresh with new signature
```

### Test 6: Invalid Signature Rejection (Manual Verification)

**Objective**: Verify backend rejects tampered headers

**Test with cURL** (should fail with 403):
```bash
# Get valid token first
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "demo_admin", "password": "demo123"}' \
  | jq -r '.accessToken')

# Try with invalid signature (should be rejected)
curl -v http://localhost:8080/fhir/Patient \
  -H "X-Auth-User-Id: test-user" \
  -H "X-Auth-Tenant-Ids: DEMO001" \
  -H "X-Auth-Roles: ADMIN" \
  -H "X-Auth-Validated: invalid-signature-fake-hash"

# Expected: HTTP 403 Forbidden
```

**Expected Result**:
- ✅ Invalid signatures rejected with 403
- ✅ Error logs show signature validation failure
- ✅ No data leaked to unauthorized requests

## Implementation Checklist

### Pre-Testing
- [ ] Review GatewayAuthenticationFilter logic
- [ ] Review TrustedHeaderAuthFilter in backend services
- [ ] Understand HMAC-SHA256 algorithm details
- [ ] Generate strong signing secret (done: `2J3YcbuHgPp0xyMq3GHYJ/MNolqB+Hvp4j/fsA6LQYM=`)
- [ ] Update docker-compose.demo.yml with secret (done)
- [ ] Document expected HMAC flow (this document)

### Testing Phase
- [ ] Clean rebuild docker image: `docker compose -f docker-compose.demo.yml down && docker compose -f docker-compose.demo.yml up -d --build`
- [ ] Wait for all services to be healthy
- [ ] Run Test 1: Basic Login
- [ ] Run Test 2: Patient List Access
- [ ] Run Test 3: Multi-Tenant Isolation
- [ ] Run Test 4: Role-Based Access Control
- [ ] Run Test 5: Token Refresh
- [ ] Run Test 6: Invalid Signature Rejection
- [ ] Review all logs for errors

### Post-Testing
- [ ] Document any issues found
- [ ] Update CLAUDE.md if behavior changes
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)
- [ ] Create migration plan if issues discovered
- [ ] Prepare for Phase 6 (documentation)

## Expected Log Patterns

### Gateway Logs (Success)
```
2025-12-31 10:15:22.345 INFO GatewayAuthenticationFilter - Validating JWT token for user: demo_admin
2025-12-31 10:15:22.456 INFO GatewayAuthenticationFilter - JWT valid, extracting claims...
2025-12-31 10:15:22.567 INFO GatewayAuthenticationFilter - Generating X-Auth headers...
2025-12-31 10:15:22.678 INFO GatewayAuthenticationFilter - Signing X-Auth headers with HMAC-SHA256
2025-12-31 10:15:22.789 INFO GatewayAuthenticationFilter - Generated X-Auth-Validated signature
2025-12-31 10:15:22.890 INFO GatewayAuthenticationFilter - Forwarding request to FHIR service with signed headers
```

### Backend Service Logs (Success)
```
2025-12-31 10:15:23.000 INFO TrustedHeaderAuthFilter - Received X-Auth headers from gateway
2025-12-31 10:15:23.111 INFO TrustedHeaderAuthFilter - Validating X-Auth-Validated signature...
2025-12-31 10:15:23.222 INFO TrustedHeaderAuthFilter - Signature validation successful
2025-12-31 10:15:23.333 INFO TrustedHeaderAuthFilter - User: demo_admin, Roles: ADMIN,EVALUATOR, Tenant: 550e8400...
2025-12-31 10:15:23.444 INFO FhirPatientController - Fetching patient list for tenant: 550e8400...
```

### Backend Service Logs (Failure - Invalid Signature)
```
2025-12-31 10:15:23.000 ERROR TrustedHeaderAuthFilter - Invalid X-Auth-Validated signature
2025-12-31 10:15:23.111 ERROR TrustedHeaderAuthFilter - Expected signature: abc123def456..., Got: fake-signature
2025-12-31 10:15:23.222 ERROR TrustedHeaderAuthFilter - Rejecting request with 403 Forbidden
```

## Troubleshooting Guide

### Issue: Login fails after enabling HMAC
**Root Cause**: Gateway configuration issue or gateway not rebuilt
**Solution**:
```bash
docker compose -f docker-compose.demo.yml down
docker system prune -f
docker compose -f docker-compose.demo.yml up -d --build
```

### Issue: 403 errors after login
**Root Cause**: Signature validation failure
**Solution**:
1. Verify GATEWAY_AUTH_SIGNING_SECRET is set correctly
2. Check both gateway and backend have same secret
3. Review logs for signature mismatch
4. Verify network connectivity between gateway and services

### Issue: Patient data loads but with errors
**Root Cause**: Partial HMAC implementation or version mismatch
**Solution**:
1. Check gateway version matches backend version
2. Verify TrustedHeaderAuthFilter is enabled in backend
3. Review X-Auth-* header forwarding in ApiGatewayController

## Rollback Plan

If HMAC enforcement causes critical issues:

1. **Disable HMAC** temporarily:
   ```bash
   # Update docker-compose.demo.yml
   # Remove or comment out GATEWAY_AUTH_SIGNING_SECRET
   docker compose -f docker-compose.demo.yml down
   docker compose -f docker-compose.demo.yml up -d
   ```

2. **Revert to dev mode**:
   ```bash
   # Set empty/dev secret
   GATEWAY_AUTH_SIGNING_SECRET: 'dev-signing-secret-change-in-production'
   ```

3. **Full rollback**:
   ```bash
   git checkout docker-compose.demo.yml
   docker compose -f docker-compose.demo.yml down
   docker compose -f docker-compose.demo.yml up -d --build
   ```

## Success Criteria

### Must-Have
- ✅ Clinical portal login works with HMAC enabled
- ✅ Patient data accessible without errors
- ✅ No 403 Forbidden errors for valid requests
- ✅ Backend logs show successful signature validation
- ✅ No security warnings in logs

### Should-Have
- ✅ All test scenarios pass
- ✅ Performance acceptable (no slowdown from HMAC)
- ✅ Multi-tenant isolation verified
- ✅ RBAC enforcement verified
- ✅ Invalid signatures properly rejected

### Nice-to-Have
- ✅ Comprehensive test documentation created
- ✅ Manual signature validation working
- ✅ Clear rollback procedure tested
- ✅ Troubleshooting guide verified

## Timeline

**Expected Duration**: 1-2 hours for full testing

- Pre-testing setup: 15 min
- Basic authentication tests: 15 min
- Data access tests: 15 min
- Security validation: 15 min
- Log review and verification: 30 min
- Issue resolution (if needed): 30 min

## References

- `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` - Architecture details
- `apps/clinical-portal/src/app/services/auth.service.ts` - Client-side auth
- `backend/modules/services/gateway-service/` - Gateway implementation
- `backend/modules/shared/infrastructure/authentication/` - Shared auth filters
