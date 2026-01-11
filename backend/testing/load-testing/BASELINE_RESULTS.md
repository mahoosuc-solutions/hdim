# HDIM Performance Testing - Baseline Results & Test User Setup

**Date:** January 11, 2026
**Environment:** Local Docker Development
**Services:** Gateway (8080), PostgreSQL (5435), Redis (6380)

---

## ✅ Phase 2 - Testing Infrastructure: COMPLETE

### What Was Accomplished

1. **Created 9 Test Users** across all roles (SUPER_ADMIN, ADMIN, EVALUATOR, ANALYST, VIEWER)
2. **Verified Authentication** - All users can successfully login and receive JWT tokens
3. **Built User Management Tools** - SQL scripts + HTML UI for managing test users
4. **Resolved BCrypt Issues** - Used PostgreSQL pgcrypto for proper password hashing
5. **Documented Everything** - Complete setup guide and troubleshooting

---

## Test User Credentials

### All Test Users (Password: `password123`)

| Username | Email | Role | Tenant | Purpose |
|----------|-------|------|--------|---------|
| bootstrap_admin | admin@healthdata.local | SUPER_ADMIN | SYSTEM | Bootstrap admin for API calls |
| test_superadmin | superadmin@test.local | SUPER_ADMIN | SYSTEM | System-wide testing |
| test_admin | admin@test.local | ADMIN | TEST_TENANT_001 | Tenant admin testing |
| test_evaluator | evaluator@test.local | EVALUATOR | TEST_TENANT_001 | Evaluation testing |
| test_analyst | analyst@test.local | ANALYST | TEST_TENANT_001 | Analytics/reporting |
| test_viewer | viewer@test.local | VIEWER | TEST_TENANT_001 | Read-only access |
| test_admin_tenant2 | admin2@test.local | ADMIN | TEST_TENANT_002 | Multi-tenant testing |
| test_evaluator_tenant2 | evaluator2@test.local | EVALUATOR | TEST_TENANT_002 | Multi-tenant testing |
| perf_test_user | perftest@test.local | EVALUATOR | TEST_TENANT_001 | **Performance testing** |

✅ **All users verified working** - Successfully logged in and received JWT tokens

---

## User Management Tools

### 1. SQL Bootstrap Script ⭐ (Recommended)

**File:** `backend/testing/test-data/bootstrap-admin-user.sql`

Creates the initial SUPER_ADMIN user needed for API operations.

**Usage:**
```bash
docker exec -i healthdata-postgres psql -U healthdata -d gateway_db < \
  backend/testing/test-data/bootstrap-admin-user.sql
```

**Creates:** `bootstrap_admin` / `password123` / SUPER_ADMIN

---

### 2. SQL Bulk User Creation ⭐ (Recommended)

**File:** `backend/testing/test-data/create-test-users-sql.sh`

Creates all test users via direct database insertion. Most reliable method.

**Usage:**
```bash
cd backend/testing/test-data
./create-test-users-sql.sh
```

**Features:**
- Uses PostgreSQL pgcrypto for BCrypt hashing
- Checks if users already exist (safe to re-run)
- Verifies login after creation
- Creates 9 test users across all roles

---

### 3. API User Creation (Currently Blocked)

**File:** `backend/testing/test-data/create-test-users.sh`

Creates users via REST API (`POST /api/v1/auth/register`).

**Status:** ⚠️ **Blocked** due to configuration issue

**Issue:** The `/api/v1/auth/register` endpoint is marked as public but requires authentication, causing 403 Forbidden errors.

**Resolution Needed:** Remove `/api/v1/auth/register` from `PublicPathRegistry.DEFAULT_PUBLIC_PATHS`

---

### 4. HTML User Management UI

**File:** `backend/testing/test-data/user-management.html`

Browser-based user management interface with Tailwind CSS styling.

**Features:**
- Login with any admin user
- Create new users via web form
- Modern, responsive UI
- Secure (uses HttpOnly cookies)

**Usage:**
```bash
# Open in browser
open backend/testing/test-data/user-management.html

# Or with Python HTTP server
cd backend/testing/test-data
python3 -m http.server 8888
# Then open: http://localhost:8888/user-management.html
```

---

## Authentication Testing

### Login Endpoint Verification ✅

All test users successfully authenticated:

```bash
# Example: Login as test_admin
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_admin","password":"password123"}'
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "test_admin",
  "email": "admin@test.local",
  "roles": ["ADMIN"],
  "tenantIds": ["TEST_TENANT_001"],
  "message": "Login successful"
}
```

### JWT Token Structure

Verified tokens include:
- `sub`: Username
- `userId`: User UUID
- `tenantIds`: Comma-separated tenant list
- `roles`: Comma-separated roles
- `iss`: "healthdata-in-motion"
- `aud`: ["healthdata-api"]
- `iat`: Issued at timestamp
- `exp`: Expiration (15 minutes for access tokens)

---

## Performance Testing Setup

### Simple Performance Test Script

**File:** `backend/testing/load-testing/simple-perf-test.sh`

Bash-based performance testing (no Apache Bench required).

**Tests:**
1. **Health Endpoint** - 100 requests to `/actuator/health`
2. **Login Endpoint** - 50 requests to `/api/v1/auth/login` (uses test_admin)
3. **Concurrent Requests** - 10 simultaneous health checks

**Usage:**
```bash
cd backend/testing/load-testing
./simple-perf-test.sh
```

**Results:** Saved to `./results/simple_YYYYMMDD_HHMMSS/`

---

## Troubleshooting & Solutions

### Issue 1: BCrypt Hash Mismatch

**Problem:** Pre-generated BCrypt hashes don't match Spring Security's encoder

**Solution:** Use PostgreSQL's `pgcrypto` extension
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
SELECT crypt('password123', gen_salt('bf'));
```

This generates BCrypt hashes compatible with Spring's `BCryptPasswordEncoder`.

---

### Issue 2: Shell Special Characters in Passwords

**Problem:** Password "Admin@2026!" caused JSON parsing errors (`!` triggers shell history)

**Solution:** Use alphanumeric passwords for testing (e.g., "password123")

---

### Issue 3: Registration Endpoint 403 Forbidden

**Problem:** `/api/v1/auth/register` is public but requires authentication

**Location:**
- `backend/modules/shared/infrastructure/gateway-core/src/main/java/com/healthdata/gateway/auth/PublicPathRegistry.java`
- Line 46: `/api/v1/auth/register` in `DEFAULT_PUBLIC_PATHS`

**Current Behavior:**
- Path marked public → JWT filter doesn't run → No authentication context
- Method annotation `@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")` requires auth
- Result: 403 Forbidden

**Fix:** Remove `/api/v1/auth/register` from public paths list

---

### Issue 4: Rate Limiting (Solved)

**Problem:** Initial testing triggered IP blocking after 50 failed login attempts

**Solution:**
- Created valid test users
- Rate limiting now works as designed (10 attempts/min, 5 min block)

---

## Next Steps

### Immediate (Ready Now)

1. ✅ **Run Performance Baselines**
   ```bash
   cd backend/testing/load-testing
   ./simple-perf-test.sh
   ```

2. ✅ **Test Multi-Tenant Access**
   - Use test_admin (TENANT_001) and test_admin_tenant2 (TENANT_002)
   - Verify tenant isolation

3. ✅ **Test RBAC**
   - Verify role-based access with test_evaluator, test_analyst, test_viewer

### Short-Term

1. **Fix Registration Endpoint** - Remove from public paths
2. **E2E Authentication Tests** - Implement automated test suite
3. **Load Testing** - Install Apache Bench for advanced testing
4. **Performance Monitoring** - Integrate with Prometheus/Grafana

### Long-Term

1. **Test Data Generation** - Seed realistic FHIR data
2. **Security Auditing** - Penetration testing with test users
3. **Scalability Testing** - Multi-container load tests
4. **Documentation** - API testing guide

---

## File Structure

```
backend/testing/
├── test-data/
│   ├── bootstrap-admin-user.sql           # Initial SUPER_ADMIN
│   ├── create-test-users-sql.sh          # ⭐ SQL bulk creation (recommended)
│   ├── create-test-users.sh               # API creation (blocked)
│   └── user-management.html               # Web UI
└── load-testing/
    ├── simple-perf-test.sh                # Performance testing tool
    ├── results/                           # Test results directory
    └── BASELINE_RESULTS.md                # This file
```

---

## Summary

### ✅ Successfully Completed

- Created 9 test users across all roles
- Verified authentication endpoints working
- Built 3 user management tools (SQL scripts + HTML UI)
- Documented complete setup and troubleshooting
- Ready for performance and integration testing

### ⚠️ Known Limitations

- API user registration blocked (configuration issue)
- Apache Bench not installed (using simple bash alternative)
- Performance baselines not yet established (ready to run)

### 🎯 Ready For

- ✅ Performance baseline testing
- ✅ Multi-tenant access control tests
- ✅ Role-based security tests
- ✅ E2E authentication flow tests

---

**Status:** Phase 2 Testing Infrastructure - **COMPLETE** ✅

*Generated: January 11, 2026*
*HDIM Platform Testing Documentation*
*Created by: Claude Code + Mahoosuc Solutions*
**Test Environment**: Development (Local Docker)
**Hardware**: Developer workstation
**Services**: All HDIM services running via docker-compose

---

## Executive Summary

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| Login RPS | > 100 | TBD | ⏳ Pending |
| Login P95 Latency | < 200ms | TBD | ⏳ Pending |
| Token Refresh RPS | > 200 | TBD | ⏳ Pending |
| Success Rate | > 99% | TBD | ⏳ Pending |
| Rate Limiting | Active | TBD | ⏳ Pending |

**Overall Status**: ⏳ Baseline establishment pending

---

## Test 1: Login Endpoint Performance

### Light Load (100 requests, 10 concurrent)

**Command**:
```bash
./run-load-tests.sh login
```

**Expected Results** (Development Environment):
```
Document Length:        450 bytes
Concurrency Level:      10
Time taken for tests:   1.234 seconds
Complete requests:      100
Failed requests:        0
Total transferred:      58000 bytes
HTML transferred:       45000 bytes
Requests per second:    81.03 [#/sec] (mean)
Time per request:       123.4 [ms] (mean)
Time per request:       12.3 [ms] (mean, across all concurrent requests)
Transfer rate:          45.89 [Kbytes/sec] received

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    1   0.5      1       2
Processing:    45  120  23.2    118     165
Waiting:       44  118  23.1    116     163
Total:         45  121  23.3    119     166

Percentage of the requests served within a certain time (ms)
  50%    119
  66%    128
  75%    135
  80%    140
  90%    152
  95%    158
  98%    162
  99%    165
 100%    166 (longest request)
```

**Actual Results**: ⏳ TBD - Run `./run-load-tests.sh login`

**Analysis Points**:
- RPS should be > 80 for development environment
- P95 latency should be < 200ms
- No failed requests expected
- Check for authentication bottlenecks if RPS < 50

---

### Medium Load (1,000 requests, 50 concurrent)

**Expected Results** (Development Environment):
```
Concurrency Level:      50
Time taken for tests:   12.456 seconds
Complete requests:      1000
Failed requests:        0
Requests per second:    80.28 [#/sec] (mean)
Time per request:       622.8 [ms] (mean)
Time per request:       12.5 [ms] (mean, across all concurrent requests)

Percentage of the requests served within a certain time (ms)
  50%    598
  66%    645
  75%    680
  80%    705
  90%    768
  95%    825
  98%    892
  99%    945
 100%   1124 (longest request)
```

**Actual Results**: ⏳ TBD - Run `./run-load-tests.sh login`

**Analysis Points**:
- P95 latency increase expected under higher concurrency
- Watch for connection pool exhaustion
- Monitor database query performance
- Check JWT generation performance

---

## Test 2: Token Refresh Performance

**Command**:
```bash
./run-load-tests.sh token-refresh
```

**Expected Results** (100 requests, 10 concurrent):
```
Requests per second:    165.42 [#/sec] (mean)
Time per request:       60.4 [ms] (mean)
Time per request:       6.0 [ms] (mean, across all concurrent requests)

Percentage of the requests served within a certain time (ms)
  50%     58
  66%     62
  75%     65
  80%     67
  90%     75
  95%     82
  98%     89
  99%     95
 100%    112 (longest request)
```

**Actual Results**: ⏳ TBD

**Analysis Points**:
- Token refresh should be faster than login (no password hashing)
- Redis cache hit should improve performance
- P95 should be < 100ms
- Watch for JWT signature validation overhead

---

## Test 3: Rate Limiting Behavior

**Command**:
```bash
./run-load-tests.sh rate-limit
```

**Expected Results** (200 requests, 50 concurrent):
```
Complete requests:      200
Failed requests:        0
Non-2xx responses:      45  (HTTP 429 - Rate Limited)
2xx responses:          155 (HTTP 200 - Success)

HTTP Status Codes:
  200: 155
  429: 45

Rate limiting threshold: ~150-160 requests (varies by tier)
```

**Actual Results**: ⏳ TBD

**Analysis Points**:
- HTTP 429 responses indicate rate limiting is active
- First N requests should succeed (based on tier)
- Subsequent requests should be rate limited
- Rate limits should reset after time window

**Verification**:
```bash
# Check Redis rate limit keys
docker exec hdim-redis redis-cli KEYS "rate-limit:*"

# Check rate limit bucket state
docker exec hdim-redis redis-cli GET "rate-limit:tenant:test_viewer"
```

---

## Test 4: Concurrent User Load

**Command**:
```bash
./run-load-tests.sh concurrent
```

**Expected Results** (10,000 requests, 100 concurrent):
```
Time taken for tests:   125.678 seconds
Complete requests:      10000
Failed requests:        0
Requests per second:    79.57 [#/sec] (mean)
Time per request:       1256.8 [ms] (mean)
Time per request:       12.6 [ms] (mean, across all concurrent requests)

Connection Times (ms)
              min  mean[+/-sd] median   max
Connect:        0    2   1.2      2       8
Processing:   145 1245 125.5   1230    1850
Waiting:      144 1243 125.4   1228    1848
Total:        145 1247 125.6   1232    1852

Percentage of the requests served within a certain time (ms)
  50%   1232
  66%   1298
  75%   1345
  80%   1378
  90%   1456
  95%   1524
  98%   1605
  99%   1678
 100%   1852 (longest request)
```

**Actual Results**: ⏳ TBD

**Analysis Points**:
- Sustained throughput ~80 RPS acceptable for dev environment
- P95 latency increase expected under sustained load
- Monitor for connection leaks
- Check database connection pool saturation
- Watch for memory pressure

---

## Test 5: System Health Under Load

**Command**:
```bash
./run-load-tests.sh health
```

**Expected Behavior**:
- `/actuator/health` should remain UP under load
- Response time should stay < 50ms
- No service degradation after 10 seconds of continuous load

**Actual Results**: ⏳ TBD

**Health Check Output**:
```
[1/10] Health Status: UP
[2/10] Health Status: UP
[3/10] Health Status: UP
[4/10] Health Status: UP
[5/10] Health Status: UP
[6/10] Health Status: UP
[7/10] Health Status: UP
[8/10] Health Status: UP
[9/10] Health Status: UP
[10/10] Health Status: UP
```

---

## Environment Baselines

### Development Environment

**Hardware**:
- CPU: Intel i7/AMD Ryzen 7 (8 cores)
- RAM: 16GB
- Storage: SSD

**Docker Resources**:
- CPU: 4 cores
- Memory: 8GB
- Swap: 2GB

**Expected Performance**:
- Login RPS: 70-100
- Token Refresh RPS: 150-200
- P95 Latency: 150-250ms

---

### Staging Environment (AWS t3.medium)

**Hardware**:
- vCPU: 2
- RAM: 4GB
- Network: Up to 5 Gbps

**Expected Performance**:
- Login RPS: 150-250
- Token Refresh RPS: 300-400
- P95 Latency: 100-150ms

---

### Production Environment (AWS c5.xlarge)

**Hardware**:
- vCPU: 4
- RAM: 8GB
- Network: Up to 10 Gbps

**Target Performance**:
- Login RPS: > 500
- Token Refresh RPS: > 1000
- P95 Latency: < 100ms
- P99 Latency: < 200ms
- Success Rate: > 99.9%

---

## Performance Analysis

### Bottleneck Identification

| Component | Typical Bottleneck | Mitigation |
|-----------|-------------------|------------|
| Password Hashing | BCrypt rounds too high | Reduce to 10-12 rounds |
| Database | Connection pool exhausted | Increase pool size (20-50) |
| Redis | Connection pool exhausted | Increase Lettuce pool size |
| JWT Signing | RSA signature overhead | Use HMAC-SHA256 for speed |
| Network | High latency | Use persistent connections |

### Optimization Recommendations

#### If RPS < 50:
1. Check database connection pool settings
2. Verify Redis cache is working
3. Profile JWT generation code
4. Check for N+1 query problems

#### If P95 > 500ms:
1. Enable database query logging
2. Check for missing indexes
3. Profile authentication service
4. Review slow query logs

#### If Failed Requests > 1%:
1. Check connection pool exhaustion
2. Verify timeout settings
3. Review error logs for exceptions
4. Check for circuit breaker activation

---

## Running the Tests

### Prerequisites

```bash
# Start all services
docker compose up -d

# Wait for services to be ready
sleep 30

# Verify health
curl http://localhost:8001/actuator/health
```

### Execute Full Test Suite

```bash
cd backend/testing/load-testing
./run-load-tests.sh all
```

### Review Results

```bash
# Latest results directory
cd results/$(ls -t results | head -1)

# View performance report
cat PERFORMANCE_REPORT.md

# Check detailed results
less login_light.txt
less login_medium.txt
less concurrent.txt
```

---

## Continuous Monitoring

### Metrics to Track

1. **Response Time Trends**
   - Plot P50, P95, P99 over time
   - Alert if P95 > 500ms

2. **Throughput Trends**
   - Plot RPS over time
   - Alert if RPS drops > 20%

3. **Error Rate**
   - Plot failed requests %
   - Alert if > 1%

4. **Resource Utilization**
   - CPU usage
   - Memory usage
   - Database connections
   - Redis connections

### Grafana Dashboard Queries

```promql
# Request rate
rate(http_requests_total{endpoint="/api/v1/auth/login"}[5m])

# P95 latency
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{endpoint="/api/v1/auth/login"}[5m]))

# Error rate
rate(http_requests_total{endpoint="/api/v1/auth/login",status=~"5.."}[5m])
/ rate(http_requests_total{endpoint="/api/v1/auth/login"}[5m])
```

---

## Action Items

- [ ] Run baseline tests in development environment
- [ ] Document actual results in this file
- [ ] Run tests in staging environment
- [ ] Compare staging vs development performance
- [ ] Set up Grafana dashboards for continuous monitoring
- [ ] Configure alerts for performance degradation
- [ ] Schedule monthly performance regression tests

---

**Test Execution Instructions**:

1. Ensure services are running: `docker compose up -d`
2. Run tests: `./run-load-testing.sh all`
3. Update this file with actual results
4. Commit results: `git add BASELINE_RESULTS.md && git commit -m "docs: Update load test baseline results"`

**Next Review**: February 2026
