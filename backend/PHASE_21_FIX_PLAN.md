# Phase 21 Test Fixes & Redis Validation - Implementation Plan

**Date**: 2025-11-06
**Status**: PLANNING
**Priority**: HIGH - Required for production readiness
**Estimated Effort**: 8-12 hours

---

## Executive Summary

This plan addresses the 14 failing tests from Phase 21 and enables Redis-based testing for comprehensive platform validation. The fixes are organized into 4 focused workstreams that can be executed in parallel using the TDD Swarm approach.

### Current Status

**Test Results:**
- 87/118 passing (73.7%)
- 14 failing (11.9%)
- 17 skipped (14.4% - Redis tests)

**Build Status:** ✅ SUCCESS (all production code compiles)

**Issues to Address:**
1. **Audit Logging Test Failures** - 4 tests (authentication context issue)
2. **JWT Refresh Token Test Failures** - 7 tests (bean configuration issue)
3. **JWT Claims Test Failures** - 2 tests (test assertion bug)
4. **Redis Rate Limiting Tests** - 17 tests (skipped, need enablement)

---

## Issue Analysis

### Issue 1: Audit Logging Test Failures (4 tests)

**Failing Tests:**
1. `testSuccessfulLoginCreatesAuditLog()`
2. `testFailedLoginCreatesAuditLog()`
3. `testIpAddressCaptured()`
4. `testUserAgentCaptured()`

**Symptom:**
```
Login endpoint returns 401 Unauthorized instead of 200 OK
```

**Root Cause:**
`AuditLoggingIntegrationTest` test context doesn't properly configure authentication. The AuthController bean is loaded, but authentication is failing during login attempts.

**Likely Issues:**
- Missing or incorrect `@WithMockUser` configuration
- AuthenticationManager not properly configured for audit logging tests
- Test users not being created in the correct database context
- HttpServletRequest mock not providing required headers

**Impact:** LOW - Audit logging works in other test contexts and production

**Priority:** 2 (Medium)

---

### Issue 2: JWT Refresh Token Test Failures (7 tests)

**Failing Tests:**
1. Test 8: `testRefreshTokenShouldGenerateNewAccessToken()`
2. Test 12: `testLogoutShouldRevokeRefreshToken()`
3. Test 13: `testMultipleRefreshTokensPerUser()`
4. Test 14: `testRevokeAllTokensForUser()`
5. Test 22: `testRefreshTokenRotationWorks()`

**Symptom:**
```
Tests expecting refresh token functionality are failing
```

**Root Cause:**
RefreshTokenService or RefreshTokenRepository not properly configured in test context. Beans may not be getting auto-wired correctly, or database transactions are not being managed properly.

**Likely Issues:**
- RefreshToken entity not being scanned (already fixed with @EntityScan, but may need verification)
- RefreshTokenRepository not being auto-configured
- Transaction management not working for refresh token operations
- Test data not persisting correctly
- Refresh token cleanup scheduler interfering with tests

**Impact:** MEDIUM - Core refresh token functionality needs verification

**Priority:** 1 (High)

---

### Issue 3: JWT Claims Test Failures (2 tests)

**Failing Tests:**
1. Test 2: `testAccessTokenShouldContainCorrectClaims()`
2. Test 19: `testRolesShouldBeExtractedFromJwtCorrectly()`

**Symptom:**
```
Test expects role "USER" but JWT contains "VIEWER"
```

**Root Cause:**
**THIS IS A TEST BUG**, not a code bug. The test user is created with `UserRole.VIEWER` but the test assertion expects `"USER"`.

**Fix:**
Simple 1-line change in test file:
```java
// BEFORE
.andExpect(jsonPath("$.roles[0]").value("USER"))

// AFTER
.andExpect(jsonPath("$.roles[0]").value("VIEWER"))
```

**Impact:** NONE - This is purely a test data mismatch

**Priority:** 3 (Low - Easy fix)

---

### Issue 4: Redis Rate Limiting Tests Skipped (17 tests)

**Status:** Tests are correctly skipping when Redis is not available

**Current Configuration:**
```yaml
# application-test.yml
rate-limiting:
  enabled: false  # Tests skip when disabled
  redis:
    enabled: false
```

**Why Skipped:**
Tests are designed to skip gracefully when Redis is not running, preventing CI/CD failures.

**To Enable:**
Need to:
1. Start Redis server (or use Testcontainers)
2. Enable rate limiting in test configuration
3. Ensure Redis connection details are correct
4. Run tests with Redis available

**Impact:** MEDIUM - Cannot validate distributed rate limiting without Redis tests

**Priority:** 1 (High - needed for production validation)

---

## Fix Plan - TDD Swarm Approach

### Workstream 1: Fix JWT Refresh Token Tests (Agent 1)

**Objective:** Get all 7 JWT refresh token tests passing

**Priority:** HIGH
**Estimated Time:** 3-4 hours
**Dependencies:** None

**Tasks:**

1. **Investigate RefreshToken Bean Configuration**
   - Read `JwtAuthenticationIntegrationTest.java`
   - Check if RefreshTokenService is being auto-wired
   - Verify RefreshTokenRepository is available
   - Check transaction management configuration

2. **Fix Entity/Repository Scanning**
   - Verify `@EntityScan` includes RefreshToken
   - Verify `@EnableJpaRepositories` includes RefreshTokenRepository
   - Check if Liquibase migrations are running in tests
   - Ensure refresh_tokens table is created

3. **Fix Test Data Persistence**
   - Ensure transactions are properly configured
   - Check if test data is rolling back unexpectedly
   - Verify database state between test steps
   - Add debugging/logging to understand failure points

4. **Fix Scheduler Interference**
   - Disable TokenCleanupScheduler in test profile
   - Ensure scheduled tasks don't interfere with tests
   - Update `application-test.yml`:
     ```yaml
     spring:
       task:
         scheduling:
           enabled: false  # Disable schedulers in tests
     ```

5. **Update Test Configuration**
   - Add any missing `@MockBean` annotations
   - Configure RefreshTokenService bean if needed
   - Ensure database is properly initialized

6. **Run and Validate**
   - Run `./gradlew :modules:shared:infrastructure:authentication:test --tests "JwtAuthenticationIntegrationTest"`
   - Verify all 30 tests pass
   - Document any remaining issues

**Success Criteria:**
- All 30 JWT tests passing
- RefreshToken operations working in test context
- No regression in other tests

**Deliverables:**
- Fixed test configuration
- Updated application-test.yml
- All JWT tests passing
- Summary of fixes applied

---

### Workstream 2: Fix Audit Logging Tests (Agent 2)

**Objective:** Get all 4 audit logging tests passing

**Priority:** MEDIUM
**Estimated Time:** 2-3 hours
**Dependencies:** None

**Tasks:**

1. **Investigate Authentication Context**
   - Read `AuditLoggingIntegrationTest.java`
   - Check how authentication is set up for tests
   - Compare with `AuthenticationEndpointsIntegrationTest.java` (which works)
   - Identify differences in test setup

2. **Fix Authentication Setup**
   - Ensure test users are created before login attempts
   - Verify password encoding matches expectations
   - Check if Basic Auth or JWT is being used in tests
   - Ensure SecurityContext is properly configured

3. **Fix Test User Creation**
   - Verify test users exist in database before login tests
   - Check password encoding (BCrypt)
   - Ensure users are in correct tenant
   - Add `@BeforeEach` setup if needed

4. **Fix MockMvc Configuration**
   - Ensure MockMvc is configured with security
   - Check if HttpServletRequest headers are provided
   - Verify IP address and User-Agent are available
   - Add `.with(httpBasic("username", "password"))` if needed

5. **Update Test Methods**
   - Add proper authentication to login test requests
   - Ensure test context matches production context
   - Fix any missing test setup
   - Example:
     ```java
     mockMvc.perform(post("/api/v1/auth/login")
         .with(httpBasic(testUser.getUsername(), PASSWORD))
         .contentType(MediaType.APPLICATION_JSON)
         .content(loginRequestJson))
         .andExpect(status().isOk());
     ```

6. **Run and Validate**
   - Run `./gradlew :modules:shared:infrastructure:authentication:test --tests "AuditLoggingIntegrationTest"`
   - Verify all 22 tests pass
   - Check audit logs are being created correctly

**Success Criteria:**
- All 22 audit logging tests passing
- Audit logs created for successful logins
- Audit logs created for failed logins
- IP address and user agent captured correctly

**Deliverables:**
- Fixed audit logging tests
- Updated test setup
- All tests passing
- Summary of fixes applied

---

### Workstream 3: Enable Redis Rate Limiting Tests (Agent 3)

**Objective:** Enable and validate all 17 Redis rate limiting tests

**Priority:** HIGH
**Estimated Time:** 3-4 hours
**Dependencies:** Redis must be available

**Tasks:**

1. **Set Up Test Redis Instance**

   **Option A: Use Testcontainers (Recommended)**
   - Add Testcontainers dependency to `build.gradle.kts`:
     ```kotlin
     testImplementation("org.testcontainers:testcontainers:1.19.3")
     testImplementation("org.testcontainers:junit-jupiter:1.19.3")
     testImplementation("com.redis.testcontainers:testcontainers-redis:1.6.4")
     ```
   - Create `RedisTestContainer.java`:
     ```java
     @TestConfiguration
     public class RedisTestContainer {
         @Bean
         @ServiceConnection
         public GenericContainer<?> redisContainer() {
             return new GenericContainer<>("redis:7-alpine")
                 .withExposedPorts(6379)
                 .withReuse(true);
         }
     }
     ```
   - Update tests to use `@Import(RedisTestContainer.class)`

   **Option B: Use Docker Compose Redis**
   - Ensure Redis from docker-compose.yml is running
   - Update `application-test.yml` to point to Docker Redis:
     ```yaml
     spring:
       redis:
         host: localhost
         port: 6379
         password: ""  # Or from docker-compose
     ```

   **Option C: Use Embedded Redis (Fallback)**
   - Keep existing `EmbeddedRedisTestConfig.java`
   - Start embedded Redis on random port
   - Configure test to use embedded instance

2. **Enable Rate Limiting in Test Configuration**
   - Update `application-test.yml`:
     ```yaml
     rate-limiting:
       enabled: true  # ENABLE
       redis:
         enabled: true  # ENABLE
         database: 15  # Separate DB for tests
         key-prefix: "test-rate-limit:"
       login:
         per-minute: 5
         per-hour: 20
       register:
         per-hour: 3
       api:
         per-minute: 100
     ```

3. **Create Separate Test Profile**
   - Create `application-test-redis.yml` for Redis tests:
     ```yaml
     spring:
       config:
         activate:
           on-profile: test-redis
       redis:
         host: localhost
         port: 6379

     rate-limiting:
       enabled: true
       redis:
         enabled: true
     ```
   - Update test class:
     ```java
     @ActiveProfiles({"test", "test-redis"})
     class RedisRateLimitingIntegrationTest {
     ```

4. **Fix Test Cleanup**
   - Ensure Redis keys are cleaned up between tests
   - Add `@BeforeEach` to flush test Redis database:
     ```java
     @BeforeEach
     void setUp() {
         // Flush Redis test database
         redisTemplate.getConnectionFactory()
             .getConnection()
             .flushDb();
     }
     ```

5. **Fix Redis Connection Configuration**
   - Ensure RedisTemplate beans are available
   - Configure Lettuce connection factory
   - Set connection pool settings for tests
   - Add connection timeout settings

6. **Run and Validate**
   - Start Redis (Docker or Testcontainers)
   - Run `./gradlew :modules:shared:infrastructure:authentication:test --tests "RedisRateLimitingIntegrationTest"`
   - Verify all 17 tests pass
   - Check Redis keys are being created correctly
   - Validate rate limit bucket behavior

**Success Criteria:**
- Redis available for tests (Testcontainers preferred)
- All 17 Redis rate limiting tests passing
- Rate limits working in distributed mode
- Admin API functions validated
- No interference with other tests

**Deliverables:**
- Testcontainers configuration OR Docker Redis setup
- Updated test configuration
- All Redis tests passing
- Summary of Redis testing approach

---

### Workstream 4: Quick Fixes & Validation (Agent 4)

**Objective:** Fix simple test bugs and validate overall platform

**Priority:** LOW (but quick wins)
**Estimated Time:** 1 hour
**Dependencies:** None

**Tasks:**

1. **Fix JWT Claims Test Assertion (5 minutes)**
   - Location: `JwtAuthenticationIntegrationTest.java`
   - Find test expecting role "USER"
   - Change to expect "VIEWER"
   - Verify tests pass

2. **Disable Schedulers in Tests (10 minutes)**
   - Update `application-test.yml`:
     ```yaml
     spring:
       task:
         scheduling:
           enabled: false
     ```
   - Prevents TokenCleanupScheduler, RateLimitCleanupScheduler from running during tests

3. **Run Full Test Suite (30 minutes)**
   - Run all authentication tests:
     ```bash
     ./gradlew :modules:shared:infrastructure:authentication:test
     ```
   - Collect results
   - Document pass/fail status

4. **Create Test Summary Report (15 minutes)**
   - Document all test results
   - List any remaining failures
   - Provide recommendations for next steps

**Success Criteria:**
- JWT claims tests passing (2 tests)
- Full test suite report available
- Clear understanding of remaining issues

**Deliverables:**
- Fixed JWT claims tests
- Disabled schedulers in tests
- Comprehensive test report
- Recommendations

---

## Execution Plan

### Phase 1: Parallel Agent Execution (3-4 hours)

**Execute all 4 agents in parallel using TDD Swarm:**

```bash
# Agent 1: JWT Refresh Token Fixes
# Agent 2: Audit Logging Fixes
# Agent 3: Redis Rate Limiting Enablement
# Agent 4: Quick Fixes & Validation
```

**Expected Outcome:**
- Agent 1: 7 JWT tests fixed
- Agent 2: 4 audit logging tests fixed
- Agent 3: 17 Redis tests enabled and passing
- Agent 4: 2 claims tests fixed + full report

**Total Expected:** 118/118 tests passing (100%)

### Phase 2: Integration Testing (1 hour)

**After all agents complete:**

1. **Run Full Test Suite**
   ```bash
   ./gradlew :modules:shared:infrastructure:authentication:test
   ```

2. **Verify Build**
   ```bash
   ./gradlew :modules:shared:infrastructure:authentication:build
   ```

3. **Run Service-Level Tests**
   ```bash
   ./gradlew :modules:services:cql-engine-service:test
   ./gradlew :modules:services:quality-measure-service:test
   ```

4. **Check Integration**
   - Verify all services compile
   - Check no regression in existing tests
   - Validate JWT works across services

### Phase 3: Load Testing (2-3 hours)

**With all tests passing, perform load testing:**

1. **JWT Authentication Load Test**
   ```bash
   # Use Apache Bench or JMeter
   ab -n 10000 -c 100 -H "Authorization: Bearer <token>" \
      http://localhost:8081/api/v1/cql/libraries
   ```

2. **Rate Limiting Validation**
   - Test rate limits under load
   - Verify Redis-backed rate limiting works
   - Check failover to in-memory

3. **Audit Logging Performance**
   - Measure async logging overhead
   - Verify logs don't slow down requests
   - Check database performance

4. **Create Performance Report**
   - Document throughput (requests/sec)
   - Measure latency (p50, p95, p99)
   - Check memory usage
   - Monitor Redis operations

### Phase 4: Documentation & Deployment (1 hour)

1. **Update Documentation**
   - Add test configuration guide
   - Document Redis setup for tests
   - Update PHASE_21_COMPLETE.md with fixes

2. **Create Deployment Guide**
   - Document production Redis setup
   - Provide JWT configuration examples
   - Add audit logging configuration guide

3. **Prepare for Staging**
   - Create staging deployment checklist
   - Document environment variables needed
   - Provide troubleshooting guide

---

## Risk Assessment

### High-Risk Items

1. **RefreshToken Bean Configuration**
   - **Risk:** May require significant test context changes
   - **Mitigation:** Review similar working tests first
   - **Contingency:** Mock RefreshTokenService if needed

2. **Redis Testcontainers**
   - **Risk:** May have Docker/Testcontainers compatibility issues
   - **Mitigation:** Have fallback to Docker Compose Redis
   - **Contingency:** Use embedded Redis as last resort

3. **Test Interference**
   - **Risk:** Fixed tests may break other tests
   - **Mitigation:** Run full suite after each fix
   - **Contingency:** Use separate test profiles

### Medium-Risk Items

1. **Audit Logging Authentication**
   - **Risk:** May require test restructuring
   - **Mitigation:** Copy patterns from working tests
   - **Contingency:** Accept lower test coverage if needed

2. **Scheduler Interference**
   - **Risk:** Schedulers may still run despite configuration
   - **Mitigation:** Use @MockBean to disable schedulers
   - **Contingency:** Set very long intervals for test profile

### Low-Risk Items

1. **JWT Claims Test Fix**
   - **Risk:** Minimal - simple assertion change
   - **Mitigation:** None needed
   - **Contingency:** None needed

---

## Success Criteria

### Must Have (Before Production)

- ✅ All 30 JWT authentication tests passing
- ✅ All 22 audit logging tests passing
- ✅ All 17 Redis rate limiting tests passing
- ✅ 118/118 tests passing (100%)
- ✅ Build successful without errors
- ✅ Load testing completed successfully

### Should Have (Before Production)

- ✅ Testcontainers configured for Redis tests
- ✅ Performance testing documented
- ✅ Deployment guide created
- ✅ All known issues resolved

### Nice to Have (Can defer)

- ⭕ Additional edge case tests
- ⭕ Integration tests with all 5 services
- ⭕ OWASP ZAP security scan
- ⭕ Penetration testing

---

## Estimated Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: Agent Execution | 3-4 hours | None |
| Phase 2: Integration Testing | 1 hour | Phase 1 complete |
| Phase 3: Load Testing | 2-3 hours | Phase 2 complete |
| Phase 4: Documentation | 1 hour | Phase 3 complete |
| **TOTAL** | **8-12 hours** | Sequential phases |

**With TDD Swarm (Parallel):** 4-6 hours wall-clock time

---

## Post-Fix Validation Checklist

### Functional Validation

- [ ] All 118 tests passing
- [ ] Build successful
- [ ] JWT login works end-to-end
- [ ] Refresh token flow works
- [ ] Token revocation works
- [ ] Audit logs created for all events
- [ ] Redis rate limiting functional
- [ ] Rate limit failover works
- [ ] Admin APIs functional
- [ ] Prometheus metrics working

### Performance Validation

- [ ] Load testing completed
- [ ] JWT authentication: >1000 req/sec
- [ ] Audit logging: <10ms overhead
- [ ] Redis rate limiting: <5ms overhead
- [ ] Memory usage acceptable
- [ ] No memory leaks detected

### Security Validation

- [ ] Invalid JWT tokens rejected
- [ ] Expired tokens rejected
- [ ] Rate limits enforced
- [ ] Audit logs immutable
- [ ] Admin APIs require SUPER_ADMIN
- [ ] Sensitive data not logged
- [ ] Token signing secure (HS512)

### Documentation Validation

- [ ] All fixes documented
- [ ] Test setup guide created
- [ ] Deployment guide updated
- [ ] Known issues documented
- [ ] Troubleshooting guide available

---

## Recommendation

**Execute this plan using TDD Swarm approach:**

1. Launch 4 agents in parallel
2. Each agent focuses on one workstream
3. Agents report back with results
4. Integrate and validate
5. Proceed to load testing
6. Deploy to staging

**Expected Outcome:**
- ✅ 118/118 tests passing (100%)
- ✅ Full platform validation
- ✅ Production-ready authentication infrastructure
- ✅ Comprehensive documentation

**Next Phase After Fixes:**
- Load testing and performance optimization
- OWASP ZAP security scan
- Staging deployment
- Production deployment planning

---

**Priority:** EXECUTE IMMEDIATELY
**Estimated Completion:** 8-12 hours (4-6 hours with parallel agents)
**Blocker for:** Production deployment

