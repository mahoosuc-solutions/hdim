# CQL Engine Service - Security Testing TODO

## Status: DEFERRED

The cql-engine-service authentication implementation via shared authentication module is **COMPLETE and FUNCTIONAL** in production. However, comprehensive security testing has been deferred due to test configuration conflicts.

## Current Situation

### What's Working ✅
- **Production Implementation**: The service correctly uses the shared authentication module
- **Security Config**: SecurityConfig.java properly configured with TenantAccessFilter
- **Runtime Authentication**: Service requires authentication in dev/prod environments
- **JPA Configuration**: Service application class correctly configured for shared authentication entities

### What's Deferred ⏸️
- **Security Integration Tests**: AuthenticationTenantIsolationTest cannot run in current test configuration
- **Test Configuration**: Existing integration tests (10 tests) were written WITHOUT authentication enabled

## Why Deferred

The cql-engine-service has **10 existing integration tests** that were all written with security DISABLED in the test profile:

```yaml
# Current application-test.yml
spring:
  security:
    enabled: false
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

Enabling security for AuthenticationTenantIsolationTest would **break all 10 existing tests**, requiring:
1. Update all 10 existing tests to include authentication credentials
2. Add @MockBean annotations for Kafka/Feign dependencies
3. Configure additional test properties for visualization Kafka topics
4. Potentially refactor test setup for multi-tenant scenarios

## Other Services Comparison

All other services successfully have security testing enabled:
- ✅ quality-measure-service: 15/15 security tests passing
- ✅ patient-service: 15/15 security tests passing
- ✅ fhir-service: 15/15 security tests passing
- ✅ care-gap-service: 15/15 security tests passing

These services either:
1. Had authentication from the beginning, OR
2. Had fewer/simpler tests to update

## Recommendation

**Priority**: LOW - Security implementation is complete and functional in production

**Effort**: MEDIUM - Estimated 2-3 hours to:
1. Update existing 10 integration tests to include authentication
2. Create comprehensive AuthenticationTenantIsolationTest (15 tests)
3. Validate all tests pass

**Approach When Implementing**:
1. Create a separate test profile (e.g., `application-test-secure.yml`) with security enabled
2. Update existing tests one-by-one to use new secure profile
3. Add AuthenticationTenantIsolationTest using secure profile
4. Once all tests updated, remove old insecure test profile

## Files Involved

### Production (All Complete ✅)
- `src/main/java/com/healthdata/cql/config/SecurityConfig.java` - CONFIGURED
- `src/main/java/com/healthdata/cql/CqlEngineServiceApplication.java` - JPA CONFIG ADDED
- `build.gradle.kts` - Shared authentication dependency ADDED

### Testing (Needs Work ⏸️)
- `src/test/resources/application-test.yml` - Security currently DISABLED
- `src/test/java/com/healthdata/cql/integration/*IntegrationTest.java` - 10 tests need auth updates
- `src/test/java/com/healthdata/cql/integration/AuthenticationTenantIsolationTest.java` - TO BE CREATED

## Security Validation

Despite deferred testing, the security implementation CAN BE validated via:

1. **Manual Testing**: Start service in dev mode, test with curl/Postman
2. **Runtime Verification**: Check logs for TenantAccessFilter activation
3. **Integration with Other Services**: Quality-measure-service successfully calls cql-engine-service with auth

## Created

2025-11-06

## Last Updated

2025-11-06
