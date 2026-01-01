# HDIM Authentication Flow - Troubleshooting & Testing Guide

**Last Updated**: January 1, 2026
**Status**: 🔴 CRITICAL - Gateway-Trust Authentication Issue Identified
**Severity**: Blocks all user logins

---

## Executive Summary

The clinical portal login flow was previously working but has been broken by changes to the gateway-trust authentication implementation.

**Current State**:
- ✅ JSON parsing error FIXED (LoginRequest now accepts both "email" and "username")
- ❌ HTTP 401 errors on login attempt (authentication service cannot find/authenticate users)
- ❌ All API calls fail because authentication isn't working

**Root Cause**: The gateway-trust authentication implementation appears to have changed how user authentication works, but the backend authentication service isn't finding the demo users.

---

## Key Issue: HTTP 401 Unauthorized on Login

### Symptom
```
POST /api/v1/auth/login
{
  "email": "demo_admin@hdim.ai",
  "password": "demo123"
}

Response:
HTTP 401 UNAUTHORIZED
{
  "status": 401,
  "error": "Unauthorized",
  "errorCode": "HDIM-401",
  "message": "Authentication failed"
}
```

### What Changed
The LoginRequest DTO was changed from accepting `email` field to `username` field. This has been **FIXED** with `@JsonAlias("email")` annotation.

### What's Still Broken
The authentication service itself (the code that validates credentials) is not finding users or failing to authenticate them.

### Files Modified to Fix JSON Issue
- `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/LoginRequest.java`
  - Added `@JsonAlias("email")` to allow both "email" and "username" fields
  - Added `@JsonIgnoreProperties(ignoreUnknown = true)` for flexibility

---

## Testing Scripts Created

Three automated testing scripts have been created for sub-agents to execute:

### 1. **test-authentication-flow.sh** - Main Test Suite
```bash
./scripts/test-authentication-flow.sh

# With verbose output
VERBOSE=1 ./scripts/test-authentication-flow.sh

# Save JSON report
./scripts/test-authentication-flow.sh test-report.json
```

**What it tests**:
- Service health (port connectivity)
- Docker service status and health
- Database connectivity and schema
- Login endpoint (all 3 demo users)
- Cookie configuration (Path, HttpOnly, SameSite)
- Authenticated API requests (FHIR, Patient, Quality Measure)
- Gateway authentication logs
- Provides text and JSON reports

**Exit Codes**:
- `0` = All tests passed
- `1` = Some tests failed

---

### 2. **test-with-logs.sh** - Comprehensive Logging Test
```bash
./scripts/test-with-logs.sh
# Output: ./test-results-YYYYMMDD-HHMMSS/
```

**Generated Logs**:
- `test-results.log` - Full test output
- `gateway-service.log` - Gateway service logs
- `fhir-service.log` - FHIR service logs
- `patient-service.log` - Patient service logs
- `quality-measure-service.log` - Quality measure service logs
- `care-gap-service.log` - Care gap service logs
- `api-requests.log` - All API request logs with HTTP codes
- `summary.log` - Test summary and statistics

---

### 3. **test-auth-reporter.py** - Report Generator
```bash
python3 scripts/test-auth-reporter.py --all-outputs

# Or individual formats:
python3 scripts/test-auth-reporter.py --json-output report.json
python3 scripts/test-auth-reporter.py --html-output report.html
python3 scripts/test-auth-reporter.py --markdown-output report.md
```

**Output Formats**:
- JSON (machine-readable)
- HTML (browser-viewable)
- Markdown (text-based)

---

## Current Diagnosis

### What IS Working
✅ **Database & Schema**
- PostgreSQL container healthy
- `gateway_db` exists with all auth tables
- Demo users present in `users` table
```sql
SELECT email, username FROM users;
-- Returns:
-- demo_admin@hdim.ai    | demo_admin
-- demo_analyst@hdim.ai  | demo_analyst
-- demo_viewer@hdim.ai   | demo_viewer
```

✅ **JSON Parsing**
- LoginRequest now accepts both "email" and "username" fields
- No more `UnrecognizedPropertyException`

✅ **Network & Services**
- All Docker services running and healthy
- Gateway service listening on port 8080
- API endpoints accessible

### What IS NOT Working
❌ **User Authentication**
- Login endpoint returns HTTP 401
- Gateway logs: "Authentication failed"
- No indication of which step fails (user lookup? password validation? token generation?)

❌ **Gateway Logs Are Opaque**
- Logs don't show detailed authentication flow steps
- Need more logging in authentication service

---

## For Sub-Agents: Next Steps to Fix

### Step 1: Identify the Exact Failure Point
The authentication is failing somewhere in this flow:
```
1. Parse LoginRequest (FIXED - no more JSON errors)
2. Look up user by email/username (❓ UNKNOWN)
3. Validate password (❓ UNKNOWN)
4. Generate JWT token (❓ UNKNOWN)
5. Return token in Set-Cookie (❓ UNKNOWN)
```

**To diagnose**:
1. Find the login endpoint handler in gateway-service
2. Add detailed logging at each step:
   ```java
   log.debug("Looking up user: {}", loginRequest.getUsername());
   log.debug("User found: {}", user);
   log.debug("Password match: {}", passwordMatches);
   log.debug("Generating token for user: {}", user.getId());
   ```
3. Re-run tests and check logs

### Step 2: Check User Database Lookup
The most likely issue is that the authentication service is:
- Looking up by wrong field name (expecting "username" in email column?)
- Looking in wrong database
- Using wrong query

**To check**:
- Find `UserRepository` or `UserService` in authentication module
- Verify it's querying `gateway_db.users` table correctly
- Verify it's handling both email and username lookups

### Step 3: Verify Password Validation
The bcrypt hash in database:
```
$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S
```

**To check**:
- Verify password encoder is comparing against stored hash correctly
- Test with simple BCrypt encoder:
  ```java
  BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  boolean matches = encoder.matches("demo123", "$2a$10$ZMMI78ekTFEdm4fZqaKS.OFWaa.kLBFFydORaLLhUq4LLGtTKE96S");
  // Should return: true
  ```

### Step 4: Check JWT Generation
- Verify JWT token is being generated successfully
- Verify claims (user_id, tenant_id, roles) are being set
- Verify token expiration is reasonable

### Step 5: Enable Gateway Auth Logging
Update `docker-compose.demo.yml` gateway-service environment:
```yaml
gateway-service:
  environment:
    LOG_LEVEL: DEBUG
    LOGGING_LEVEL_COM_HEALTHDATA_AUTHENTICATION: DEBUG
    LOGGING_LEVEL_COM_HEALTHDATA_GATEWAY_AUTH: DEBUG
```

Then restart and re-run tests to get detailed logs.

---

## How Sub-Agents Should Run Tests

### Recommended Workflow

```bash
# 1. Run comprehensive test suite
./scripts/test-authentication-flow.sh

# 2. If tests fail, capture detailed logs
./scripts/test-with-logs.sh

# 3. Generate reports
python3 scripts/test-auth-reporter.py --json-output report.json
python3 scripts/test-auth-reporter.py --html-output report.html

# 4. Review logs to identify failure point:
cat test-results-*/api-requests.log
cat test-results-*/gateway-service.log

# 5. Check database status
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c "SELECT COUNT(*) FROM users;"

# 6. Test login directly
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo_admin@hdim.ai","password":"demo123"}' \
  -v

# 7. Check gateway is healthy
docker compose ps | grep gateway
```

### Success Criteria
When authentication is fixed, you should see:
```bash
✓ Login endpoint: admin (demo_admin@hdim.ai) - HTTP 200
✓ Login response: user.id present - PASS
✓ Login response: user.email present - PASS
✓ Cookie set: hdim_access_token - PASS
✓ Cookie path correct: hdim_access_token (Path=/) - PASS
✓ Authenticated API: GET /fhir/metadata - HTTP 200
✓ Gateway logs: JWT validated - PASS
```

---

## Files to Investigate

### Critical Files
1. **Authentication Service** (lookup & validation logic)
   - `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/`
   - Look for: `UserService`, `UserRepository`, `AuthenticationService`

2. **Gateway Auth Filter** (entry point)
   - `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/auth/GatewayAuthenticationFilter.java`
   - Already modified to support cookie extraction ✅

3. **Login Controller**
   - `backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/api/` (or similar)
   - Look for: `AuthenticationController`, `LoginEndpoint`

4. **User Entity**
   - `backend/modules/shared/domain/` (or shared/infrastructure)
   - Look for: `User.java`, `UserEntity.java`

### Configuration Files
- `docker-compose.demo.yml` - Service environment variables
- `backend/modules/services/gateway-service/src/main/resources/application.yml` - Auth config

---

## Known Issues & Workarounds

### Issue 1: Docker Build Failures
**Problem**: Docker build fails with credential errors
**Workaround**: Use `docker cp` to update JAR and `docker restart` to apply changes
```bash
cd backend && ./gradlew :modules:services:gateway-service:build -x test
docker cp backend/modules/services/gateway-service/build/libs/gateway-service.jar hdim-demo-gateway:/app/app.jar
docker compose restart gateway-service
```

### Issue 2: OpenTelemetry/Jaeger Errors in Logs
**Problem**: Lots of "Failed to export spans" errors to Jaeger
**Status**: Harmless - not related to authentication
**Ignore**: These can be safely ignored during debugging

### Issue 3: Database Locale Warnings
**Problem**: PostgreSQL warnings about locales
**Status**: Harmless - database works fine
**Ignore**: Safe to ignore

---

## Quick Reference: Important Endpoints

| Endpoint | Method | Purpose | Expected |
|----------|--------|---------|----------|
| `/api/v1/auth/login` | POST | Login with credentials | HTTP 200 with JWT |
| `/fhir/metadata` | GET | FHIR capability statement | HTTP 200 (needs auth) |
| `/patient/api/v1/patients` | GET | List patients | HTTP 200 (needs auth) |
| `/quality-measure/api/v1/measures` | GET | List measures | HTTP 200 (needs auth) |
| `/actuator/health` | GET | Service health | HTTP 200 |

---

## Log File Locations

### In Running Containers
```bash
# Gateway logs
docker logs hdim-demo-gateway

# Database logs
docker logs hdim-demo-postgres

# All service logs
docker logs hdim-demo-fhir
docker logs hdim-demo-patient
docker logs hdim-demo-quality-measure
docker logs hdim-demo-care-gap
```

### From Test Runs
```bash
test-results-20260101-150000/
├── test-results.log
├── gateway-service.log
├── postgres.log
├── api-requests.log
└── summary.log
```

---

## Contact & Escalation

When reporting the authentication issue, include:
1. Output of `./scripts/test-authentication-flow.sh`
2. Log file: `test-results-*/gateway-service.log` (last 100 lines)
3. Log file: `test-results-*/api-requests.log`
4. Output of `docker compose ps`
5. Output of `docker logs hdim-demo-gateway | grep -i "auth\|login\|error" | tail -50`

---

## Related Documentation

- `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` - Gateway trust auth design
- `AUTHENTICATION_GUIDE.md` - High-level authentication flow
- `docker-compose.demo.yml` - Demo environment configuration
- `backend/modules/shared/infrastructure/authentication/` - Auth module source

