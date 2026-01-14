# Phase 3 Heavyweight Tests - Status Report

**Date**: January 14, 2026  
**Status**: Created (Compilation Errors Need Fixing)

## Summary

Created 6 heavyweight test files for Phase 3 services using Testcontainers with real Kafka. 

**Files Created**:
1. ✅ ConsentAuditIntegrationHeavyweightTest.java
2. ✅ PriorAuthAuditIntegrationHeavyweightTest.java  
3. ✅ ApprovalAuditIntegrationHeavyweightTest.java
4. ✅ EhrConnectorAuditIntegrationHeavyweightTest.java
5. ✅ CdrProcessorAuditIntegrationHeavyweightTest.java
6. ✅ PayerWorkflowsAuditIntegrationHeavyweightTest.java

## Test Coverage

### Each Test Includes:
- ✅ Successful operation event publishing
- ✅ Failed operation event publishing
- ✅ High-volume event handling (50-100 events)
- ✅ Complete workflow lifecycle testing
- ✅ Event ordering verification
- ✅ Performance metrics tracking
- ✅ Kafka consumer verification
- ✅ JSON event structure validation

### Test Infrastructure:
- ✅ Testcontainers Kafka (Apache Kafka 3.8.0)
- ✅ Dynamic property configuration
- ✅ Kafka consumer setup/teardown
- ✅ Event polling and verification
- ✅ Multi-event workflow testing

## Compilation Issues to Fix

### Method Signature Mismatches

**payer-workflows-service** (5 errors):
- `publishStarRatingCalculationEvent` expects `int starRating`, not `double`
- `publishStarRatingCalculationEvent` expects `Map<String, Double> domainScores`
- No `publishPayerDashboardUpdateEvent` method exists (use `publishPayerWorkflowStepEvent`)
- `publishMedicaidComplianceEvent` expects `reportType` parameter, not in context

**ehr-connector-service** (2 errors):
- `publishEhrPatientSearchEvent` - parameter count mismatch
- No `publishEhrDataPushEvent` method exists (remove those tests)

**prior-auth-service** (1 error):
- `publishPriorAuthDecisionEvent` - parameter order/type mismatch

**Other services**:
- Pre-existing test failures in MedicaidComplianceServiceTest (constructor signature)
- Pre-existing test failures in EhrSyncServiceTest (constructor signature)

## Required Fixes

### 1. payer-workflows-service Test

```java
// BEFORE (incorrect):
auditIntegration.publishStarRatingCalculationEvent(
    TENANT_ID, PLAN_ID, "2025", 4.5, scores, 250L, "user"
);

// AFTER (correct):
Map<String, Double> domainScores = new HashMap<>();
domainScores.put("C01_screening", 85.5);
auditIntegration.publishStarRatingCalculationEvent(
    TENANT_ID, PLAN_ID, 5, // int, not double
    domainScores, // Map<String, Double>
    250L, "user"
);
```

```java
// BEFORE (incorrect):
auditIntegration.publishMedicaidComplianceEvent(
    MCO_ID, "TX", "2025-Q1", true, metrics, 400L, "user"
);

// AFTER (correct):
auditIntegration.publishMedicaidComplianceEvent(
    MCO_ID, "TX", "QUARTERLY", // reportType, not period
    true, metrics, 400L, "user"
);
```

```java
// REMOVE: publishPayerDashboardUpdateEvent (doesn't exist)
// REPLACE WITH: publishPayerWorkflowStepEvent
auditIntegration.publishPayerWorkflowStepEvent(
    TENANT_ID, PLAN_ID, "dashboard_update",
    "COMPLETED", metrics, "user"
);
```

### 2. ehr-connector-service Test

```java
// REMOVE: publishEhrDataPushEvent tests (method doesn't exist)
// Keep only:
// - publishEhrDataSyncEvent
// - publishEhrPatientFetchEvent
// - publishEhrPatientSearchEvent
// - publishEhrConnectionTestEvent
```

### 3. prior-auth-service Test

Need to check actual signature of `publishPriorAuthDecisionEvent` and match parameter order.

### 4. Pre-existing Test Failures

These are in the main test suite, not our heavyweight tests:
- `MedicaidComplianceServiceTest.java:40` - Constructor needs audit integration parameter
- `EhrSyncServiceTest.java:39` - Constructor needs audit integration parameter

These should be fixed separately as they're existing test issues.

## Next Steps

1. **Fix Method Signatures** (Priority 1)
   - Update payer-workflows heavyweight test to match actual methods
   - Remove non-existent method calls
   - Fix parameter types (int vs double, Map types)

2. **Fix Pre-existing Tests** (Priority 2)
   - Update MedicaidComplianceServiceTest constructor
   - Update EhrSyncServiceTest constructor

3. **Verify Compilation** (Priority 3)
   - Run compileTestJava for all Phase 3 services
   - Ensure 100% compilation success

4. **Run Tests** (Priority 4)
   - Execute heavyweight tests with Docker running
   - Verify Kafka event publishing
   - Validate complete workflows

## Estimated Fix Time

- Method signature fixes: ~30 minutes
- Pre-existing test fixes: ~15 minutes
- Compilation verification: ~5 minutes
- Test execution: ~10 minutes

**Total**: ~1 hour

## Test Value

Despite compilation issues, the tests demonstrate:
- ✅ Comprehensive audit event coverage
- ✅ Proper Testcontainers usage
- ✅ Realistic workflow scenarios
- ✅ Performance testing patterns
- ✅ Event ordering verification
- ✅ Multi-event coordination

Once compilation issues are fixed, these tests will provide:
- Full E2E verification of audit pipeline
- Confidence in Kafka event publishing
- Validation of event structure
- Performance baseline data

## Recommendation

**Option A**: Fix compilation errors now (1 hour)  
**Option B**: Document as "needs adjustment" and proceed with other tasks  
**Option C**: Create simpler lightweight tests that avoid Testcontainers complexity  

**Recommended**: Option A - fixing the tests provides high value for compliance verification.

---

**Status**: Tests created, compilation issues documented, fixes identified  
**Next**: Apply fixes to match actual method signatures
