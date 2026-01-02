# Rate Limiting Implementation - Phase 20

## Overview

Implemented comprehensive rate limiting with Bucket4j to protect against brute force attacks and API abuse across all microservices.

**Status**: COMPLETE
**Security Priority**: HIGH (Critical for brute force protection)
**Implementation Date**: 2025-11-06

---

## Implementation Summary

### 1. Core Components Created

#### RateLimitingFilter
**Location**: `/backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/filter/RateLimitingFilter.java`

- Implements token bucket algorithm using Bucket4j
- Provides per-IP rate limiting for all endpoints
- Supports X-Forwarded-For header for load balancer/proxy scenarios
- Returns 429 (Too Many Requests) with JSON error response
- Conditionally enabled via configuration property

**Key Features**:
- In-memory bucket storage for performance (ConcurrentHashMap)
- Unique buckets per IP:URI combination
- Automatic bucket cleanup through garbage collection
- Skips health check and actuator endpoints

#### RateLimitConfig
**Location**: `/backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/config/RateLimitConfig.java`

- Externalized configuration via Spring @ConfigurationProperties
- Validates configuration with Jakarta validation
- Default values optimized for security

---

## Rate Limit Policies

### Login Endpoint (`/api/v1/auth/login`)
- **Per Minute**: 5 requests per IP address
- **Per Hour**: 20 requests per IP address
- **Rationale**: Two-tier protection against rapid and distributed brute force attacks

### Register Endpoint (`/api/v1/auth/register`)
- **Per Hour**: 3 requests per IP address
- **Rationale**: Prevents automated account creation and spam

### General API Endpoints
- **Per Minute**: 100 requests per IP address
- **Rationale**: Allows legitimate application usage while preventing DoS

---

## Files Modified/Created

### Created Files:
1. `/backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/filter/RateLimitingFilter.java` - Main filter implementation
2. `/backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/config/RateLimitConfig.java` - Configuration class
3. `/backend/modules/shared/infrastructure/authentication/src/test/java/com/healthdata/authentication/filter/RateLimitingFilterTest.java` - Comprehensive tests

### Modified Files:
4. `/backend/modules/shared/infrastructure/authentication/build.gradle.kts` - Added Bucket4j dependencies
5. `/backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/config/SecurityConfig.java` - Registered filter
6. `/backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/SecurityConfig.java` - Registered filter
7. `/backend/modules/services/fhir-service/src/main/java/com/healthdata/fhir/config/SecurityConfig.java` - Registered filter
8. `/backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/config/SecurityConfig.java` - Registered filter
9. `/backend/modules/services/patient-service/src/main/java/com/healthdata/patient/config/SecurityConfig.java` - Registered filter
10. `/backend/modules/services/cql-engine-service/src/main/resources/application.yml` - Added configuration
11. `/backend/modules/services/quality-measure-service/src/main/resources/application.yml` - Added configuration
12. `/backend/modules/services/fhir-service/src/main/resources/application.yml` - Added configuration
13. `/backend/modules/services/care-gap-service/src/main/resources/application.yml` - Added configuration
14. `/backend/modules/services/patient-service/src/main/resources/application.yml` - Added configuration

---

## Configuration

### Application YAML Configuration

```yaml
rate-limiting:
  enabled: true
  login:
    per-minute: 5
    per-hour: 20
  register:
    per-hour: 3
  api:
    per-minute: 100
```

### Environment-Specific Configuration

- **Development**: Rate limiting enabled with default values
- **Test**: Rate limiting enabled (validates security controls)
- **Production**: Rate limiting enabled with configurable values

### Disabling Rate Limiting (NOT RECOMMENDED)

```yaml
rate-limiting:
  enabled: false
```

**WARNING**: Only disable in development environments. NEVER disable in production.

---

## Security Filter Chain Order

```
1. RateLimitingFilter (BEFORE authentication)
2. UsernamePasswordAuthenticationFilter (authentication)
3. BasicAuthenticationFilter (basic auth)
4. TenantAccessFilter (AFTER authentication)
```

**Critical**: RateLimitingFilter runs BEFORE authentication to protect against brute force attacks without consuming authentication resources.

---

## Test Results

### Test Coverage: 10 Tests (100% Pass Rate)

```
RateLimitingFilter Tests > Should handle X-Forwarded-For header correctly PASSED
RateLimitingFilter Tests > Should enforce rate limits when disabled is false PASSED
RateLimitingFilter Tests > Should skip rate limiting for health check endpoints PASSED
RateLimitingFilter Tests > Should allow requests within rate limit PASSED
RateLimitingFilter Tests > Should isolate rate limits by IP address PASSED
RateLimitingFilter Tests > Should enforce rate limit on register endpoint after 3 requests per hour PASSED
RateLimitingFilter Tests > Should enforce rate limit on login endpoint after 5 requests per minute PASSED
RateLimitingFilter Tests > Should apply different rate limits to different endpoints PASSED
RateLimitingFilter Tests > Should apply general API rate limit to non-auth endpoints PASSED
RateLimitingFilter Tests > Should return proper JSON error response when rate limited PASSED
```

### Build Results

```
Authentication module compiles successfully
All integration tests passing
10/10 rate limiting tests passing
```

---

## Dependencies Added

```kotlin
// Rate limiting (Bucket4j)
implementation("com.bucket4j:bucket4j-core:8.7.0")
implementation("javax.cache:cache-api:1.1.1")
```

---

## Rate Limit Response Format

When rate limit is exceeded, clients receive:

```json
{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please try again later.",
  "status": 429,
  "path": "/api/v1/auth/login"
}
```

**HTTP Status**: 429 Too Many Requests
**Content-Type**: application/json;charset=UTF-8

---

## Testing Rate Limits Manually

### Test Login Rate Limit (5 per minute)

```bash
# Should get 429 on 6th request
for i in {1..6}; do
  echo "Request $i:"
  curl -X POST http://localhost:8081/cql-engine/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}' \
    -w "\nHTTP Status: %{http_code}\n"
  echo "---"
done
```

### Test Register Rate Limit (3 per hour)

```bash
# Should get 429 on 4th request
for i in {1..4}; do
  echo "Request $i:"
  curl -X POST http://localhost:8081/cql-engine/api/v1/auth/register \
    -H "Content-Type: application/json" \
    -u "admin:password" \
    -d '{
      "username":"user'$i'",
      "email":"user'$i'@example.com",
      "password":"Test1234!",
      "firstName":"Test",
      "lastName":"User",
      "roles":["EVALUATOR"],
      "tenantIds":["tenant1"]
    }' \
    -w "\nHTTP Status: %{http_code}\n"
  echo "---"
done
```

### Test API Rate Limit (100 per minute)

```bash
# Should succeed (well under 100/minute)
for i in {1..10}; do
  curl -X GET http://localhost:8081/cql-engine/api/v1/health \
    -u "admin:password" \
    -w "\nHTTP Status: %{http_code}\n"
done
```

---

## Production Considerations

### 1. Monitoring

Monitor rate limit hits in logs:
```
WARN - Rate limit exceeded for IP: 203.0.113.45, URI: /api/v1/auth/login
```

### 2. Adjusting Limits

Rate limits can be adjusted per environment:

```yaml
# Production (stricter)
rate-limiting:
  login:
    per-minute: 3
    per-hour: 10
  register:
    per-hour: 2
  api:
    per-minute: 50

# Development (more lenient)
rate-limiting:
  login:
    per-minute: 10
    per-hour: 50
  register:
    per-hour: 10
  api:
    per-minute: 200
```

### 3. Load Balancer Configuration

Ensure X-Forwarded-For header is set by load balancer:

```
# Nginx example
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

# AWS ALB automatically sets X-Forwarded-For
```

### 4. Redis-Based Rate Limiting (Future Enhancement)

Current implementation uses in-memory buckets. For multi-instance deployments, consider:
- Redis-backed bucket storage
- Hazelcast distributed cache
- Database-backed rate limiting

### 5. Alerting

Set up alerts for:
- High rate of 429 responses (potential attack)
- Rate limit configuration changes
- Filter initialization failures

---

## Security Impact

### Threats Mitigated

1. **Brute Force Password Attacks** - Login rate limiting prevents automated password guessing
2. **Account Enumeration** - Rate limiting makes username enumeration impractical
3. **Automated Account Creation** - Register rate limiting prevents bot registration
4. **API Abuse** - General API rate limiting prevents resource exhaustion
5. **Denial of Service (DoS)** - Rate limiting prevents single-source DoS attacks

### Attack Scenarios Prevented

#### Scenario 1: Credential Stuffing
- **Attack**: Attacker tries 10,000 username/password combinations
- **Prevention**: After 5 attempts in 1 minute, IP is blocked for remainder of minute
- **Result**: Attack significantly slowed, allowing detection and blocking

#### Scenario 2: Distributed Brute Force
- **Attack**: Attacker uses 100 IPs to try passwords
- **Prevention**: Each IP limited to 20 attempts/hour
- **Result**: Attack requires 500+ IPs to maintain high rate

#### Scenario 3: Automated Account Spam
- **Attack**: Bot creates hundreds of fake accounts
- **Prevention**: 3 registrations/hour per IP
- **Result**: Spammer needs 33+ IPs per 100 accounts

---

## Future Enhancements

1. **Per-User Rate Limiting** - Add authenticated user-based rate limits
2. **Dynamic Rate Adjustment** - Adjust limits based on traffic patterns
3. **IP Whitelisting** - Bypass rate limits for trusted IPs
4. **Geographic Rate Limiting** - Different limits per country/region
5. **Redis Integration** - Distributed rate limiting for horizontal scaling
6. **Rate Limit Headers** - Return X-RateLimit-* headers in responses
7. **Admin Override API** - Allow admins to temporarily adjust limits
8. **Rate Limit Dashboard** - Real-time visualization of rate limit metrics

---

## Compliance Notes

- **HIPAA**: Rate limiting helps meet access control requirements (§164.312(a)(1))
- **NIST 800-53**: Implements AC-2 (Account Management) and AC-7 (Unsuccessful Logon Attempts)
- **OWASP Top 10**: Mitigates A07:2021 - Identification and Authentication Failures
- **PCI DSS**: Requirement 8.1.6 - Limit repeated access attempts

---

## Troubleshooting

### Issue: Legitimate Users Getting Rate Limited

**Symptoms**: Users report being blocked after few login attempts

**Solutions**:
1. Increase per-minute limit for login endpoint
2. Check if multiple users share same public IP (corporate network)
3. Implement per-user rate limiting in addition to per-IP
4. Add IP whitelist for corporate offices

### Issue: Rate Limits Not Working

**Symptoms**: No 429 responses even after many requests

**Solutions**:
1. Verify `rate-limiting.enabled=true` in application.yml
2. Check filter is registered in SecurityConfig
3. Ensure Bucket4j dependency is present
4. Check logs for filter initialization errors

### Issue: Different IPs Getting Same Bucket

**Symptoms**: Users with different IPs sharing rate limits

**Solutions**:
1. Verify X-Forwarded-For header parsing
2. Check load balancer configuration
3. Review IP extraction logic in getClientIP()

---

## Metrics and Monitoring

### Key Metrics to Track

1. **Rate Limit Hit Rate**: Percentage of requests that hit rate limits
2. **429 Response Rate**: Number of 429 responses per minute
3. **Top Rate-Limited IPs**: IPs hitting rate limits most frequently
4. **Rate Limit by Endpoint**: Which endpoints are most rate-limited

### Log Monitoring Queries

```bash
# Count rate limit hits per IP
grep "Rate limit exceeded" logs/app.log | \
  grep -oP 'IP: \K[0-9.]+' | \
  sort | uniq -c | sort -rn

# Count rate limits by endpoint
grep "Rate limit exceeded" logs/app.log | \
  grep -oP 'URI: \K[^\s]+' | \
  sort | uniq -c | sort -rn
```

---

## Conclusion

Rate limiting is now fully implemented across all microservices, providing critical protection against brute force attacks and API abuse. The implementation is production-ready, thoroughly tested, and configurable per environment.

**Next Steps**:
- Monitor rate limit metrics in production
- Fine-tune limits based on legitimate usage patterns
- Consider implementing per-user rate limiting for authenticated endpoints
- Evaluate Redis integration for distributed deployments
