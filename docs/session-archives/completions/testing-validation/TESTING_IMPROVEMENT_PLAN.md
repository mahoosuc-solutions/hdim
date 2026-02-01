# Testing Improvement Plan - Action Items

**Date**: 2026-01-13  
**Based on**: Comprehensive Testing Review  
**Priority**: High

## Quick Summary

- **Total Test Files**: 454 across all services
- **Test Code**: ~150,000 lines
- **Current Status**: Good foundation, but gaps exist
- **New Audit Tests**: 4 files (2 services only)
- **Test Failures**: 3 failures in heavyweight tests

---

## Critical Issues to Fix (Week 1)

### Issue 1: Heavyweight Test Failures
**Services**: care-gap-service, cql-engine-service  
**Status**: ⚠️ 3/8 tests failing

**Problems:**
1. Kafka connectivity timeout in Testcontainers
2. Spring Boot configuration conflict (multiple @SpringBootConfiguration)
3. Error handling test expectations mismatch

**Actions:**
```bash
# Fix Kafka container setup
# Increase timeout: withStartupTimeout(Duration.ofMinutes(2))
# Fix Spring Boot config: exclude TestCqlEngineApplication
# Review error handling: align tests with implementation
```

**Files to Fix:**
- `CareGapAuditIntegrationHeavyweightTest.java`
- `CqlAuditIntegrationHeavyweightTest.java`
- `CqlAuditIntegrationTest.java` (error handling test)

---

## High Priority Improvements (Week 2-3)

### 1. Extend Audit Integration Tests

**Current**: Only 2 services have audit tests  
**Target**: All services that publish audit events

**Services Needing Audit Tests:**
- ✅ care-gap-service (done)
- ✅ cql-engine-service (done)
- ❌ quality-measure-service
- ❌ patient-service
- ❌ fhir-service
- ❌ Other services with audit integration

**Test Scenarios to Add:**
1. Concurrent event publishing (100+ events)
2. High-volume publishing (10,000+ events)
3. Event ordering verification
4. Partition distribution
5. Dead letter queue handling
6. Multi-tenant event isolation

### 2. Add Missing Test Scenarios

**Error Handling:**
- Network failures
- Timeout scenarios
- Partial failures
- Retry logic
- Circuit breakers

**Multi-Tenant:**
- Cross-tenant data leakage prevention
- Concurrent multi-tenant operations
- Tenant-specific audit isolation

**Performance:**
- Load tests (1000+ concurrent requests)
- Stress tests
- Memory leak detection
- Resource exhaustion

### 3. Standardize Testing Patterns

**Create Standard Base Classes:**
```java
@BaseIntegrationTest    // For integration tests
@BaseUnitTest          // For unit tests
@BaseAuditTest         // For audit tests
```

**Standardize Naming:**
- `*Test.java` - Unit tests
- `*IntegrationTest.java` - Integration tests
- `*HeavyweightTest.java` - Testcontainers tests
- `*E2ETest.java` - End-to-end tests
- `*AuditTest.java` - Audit-specific tests

---

## Implementation Roadmap

### Phase 1: Fix Failures (Week 1)
- [ ] Fix Kafka connectivity issues
- [ ] Fix Spring Boot configuration conflicts
- [ ] Fix error handling test expectations
- [ ] Verify all tests pass

### Phase 2: Extend Audit Tests (Week 2)
- [ ] Add concurrent event publishing tests
- [ ] Add high-volume publishing tests
- [ ] Add event ordering tests
- [ ] Add partition distribution tests
- [ ] Add dead letter queue tests

### Phase 3: Standardize (Week 3)
- [ ] Create standard base test classes
- [ ] Standardize test configuration
- [ ] Create shared test data builders
- [ ] Update all services to use standards

### Phase 4: Add Coverage (Week 4-6)
- [ ] Add error handling tests
- [ ] Add multi-tenant isolation tests
- [ ] Add performance tests
- [ ] Add security tests

---

## Test Coverage Targets

| Service | Current | Target | Priority |
|---------|---------|--------|----------|
| care-gap-service | ~70% | 80% | High |
| cql-engine-service | ~75% | 85% | High |
| quality-measure-service | ~85% | 90% | Medium |
| patient-service | ~60% | 80% | High |
| fhir-service | ~50% | 80% | High |

---

## Success Criteria

**Testing is sufficient when:**
- ✅ All services have audit integration tests
- ✅ 80%+ unit test coverage
- ✅ 75%+ integration test coverage
- ✅ All critical use cases have tests
- ✅ All error scenarios have tests
- ✅ Performance tests for critical paths
- ✅ Multi-tenant isolation verified
- ✅ Test execution time < 10 minutes

---

## Next Steps

1. **Review** `TESTING_COMPREHENSIVE_REVIEW.md` for full details
2. **Prioritize** which improvements to tackle first
3. **Assign** tasks to team members
4. **Track** progress against roadmap
5. **Review** weekly and adjust as needed

---

**See Also:**
- `TESTING_COMPREHENSIVE_REVIEW.md` - Full detailed review
- `backend/TESTING_ARCHITECTURE.md` - Testing architecture guide
- `docs/TDD_IMPLEMENTATION_PLAN.md` - TDD best practices

