# Audit Integration Implementation Guide

**Status**: 2/15 Services Integrated (13.3%)  
**Date**: January 14, 2026

---

## Overview

This guide documents the systematic integration of audit calls into all 15 services with audit integration classes. The goal is to wire the audit integration services into actual business logic so audit events are published in production.

---

## Integration Pattern

For each service, follow this 4-step pattern:

### Step 1: Add Audit Integration Field

Add the audit integration service as a constructor dependency (using Lombok `@RequiredArgsConstructor`):

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class MyService {
    
    // Existing dependencies
    private final MyRepository repository;
    
    // ADD THIS:
    private final MyAuditIntegration auditIntegration;
}
```

### Step 2: Identify Key Decision Points

Locate methods where:
- Clinical decisions are made
- PHI is accessed
- Workflows are executed
- Authorization decisions occur
- Data transformations happen

###Step 3: Add Audit Calls

Add audit calls **after** successful operations (but before returning):

```java
public MyEntity doSomething(String tenantId, UUID id, String user) {
    // Business logic
    MyEntity result = repository.save(entity);
    
    // Existing events (don't remove)
    publishKafkaEvent("my.event", result);
    
    // ADD AUDIT CALL HERE:
    auditIntegration.publishMyDecisionEvent(
        tenantId, 
        id, 
        result.getSomeField(),
        user
    );
    
    return result;
}
```

### Step 4: Verify Compilation

```bash
./gradlew :modules:services:my-service:compileJava --no-daemon
```

---

## Completed Services ✅

### 1. consent-service ✅

**File**: `backend/modules/services/consent-service/src/main/java/com/healthdata/consent/service/ConsentService.java`

**Changes**:
- Added `ConsentAuditIntegration` field
- Integrated in `createConsent()` → `publishConsentGrantEvent()`
- Integrated in `updateConsent()` → `publishConsentUpdateEvent()`
- Integrated in `revokeConsent()` → `publishConsentRevokeEvent()`

**Status**: ✅ Compiled successfully

---

### 2. prior-auth-service ✅

**File**: `backend/modules/services/prior-auth-service/src/main/java/com/healthdata/priorauth/service/PriorAuthService.java`

**Changes**:
- Added `PriorAuthAuditIntegration` field
- Integrated in `createRequest()` → `publishPriorAuthRequestEvent()`
- Integrated in `submitToPayerAsync()` → `publishPriorAuthSubmissionEvent()`

**Notes**:
- Urgency enum converted to string with `.name()`
- Submission timing calculated from created/submitted timestamps

**Status**: ✅ Compiled successfully

---

## Remaining Services (13)

### Phase 3 Services (4 remaining)

#### 3. ehr-connector-service ⏳
**File**: `backend/modules/services/ehr-connector-service/src/main/java/com/healthdata/ehr/service/EhrSyncService.java`

**Key Methods**:
- `syncPatientData()` → `publishEhrDataSyncEvent()`
- `getPatient()` → `publishEhrPatientFetchEvent()`
- `searchPatients()` → `publishEhrPatientSearchEvent()`

**Audit Integration**: `EhrConnectorAuditIntegration`

---

#### 4. cdr-processor-service ⏳
**File**: `backend/modules/services/cdr-processor-service/src/main/java/com/healthdata/cdr/service/Hl7v2ParserService.java`

**Key Methods**:
- `parseMessage()` → `publishHl7MessageIngestEvent()`
- `parseDocument()` (in `CdaParserService`) → `publishCdaDocumentIngestEvent()`

**Audit Integration**: `CdrProcessorAuditIntegration`

---

#### 5. approval-service ⏳
**File**: `backend/modules/services/approval-service/src/main/java/com/healthdata/approval/service/ApprovalService.java`

**Key Methods**:
- `createApprovalRequest()` → `publishApprovalRequestEvent()`
- `approve()` → `publishApprovalDecisionEvent(approved=true)`
- `reject()` → `publishApprovalDecisionEvent(approved=false)`
- `escalate()` → `publishApprovalEscalationEvent()`

**Audit Integration**: `ApprovalAuditIntegration`

---

#### 6. payer-workflows-service ⏳
**File**: `backend/modules/services/payer-workflows-service/src/main/java/com/healthdata/payer/service/StarRatingCalculator.java`

**Key Methods**:
- `calculateStarRating()` → `publishStarRatingCalculationEvent()`
- `validateMedicaidCompliance()` (hypothetical) → `publishMedicaidComplianceEvent()`

**Audit Integration**: `PayerWorkflowsAuditIntegration`

---

### Phase 2 Services (6 remaining)

#### 7. agent-runtime-service ⏳
**File**: `backend/modules/services/agent-runtime-service/src/main/java/com/healthdata/agent/core/AgentOrchestrator.java`

**Status**: ✅ **ALREADY INTEGRATED** (completed in Phase 2)

**Integrated Points**:
- `executeAgentLoop()` → `publishAgentExecutionEvent()`
- `executeToolCall()` → `publishToolExecutionEvent()`
- `checkGuardrails()` → `publishGuardrailBlockEvent()`

---

#### 8. predictive-analytics-service ⏳
**File**: `backend/modules/services/predictive-analytics-service/src/main/java/com/healthdata/predictive/service/ReadmissionPredictor.java`

**Key Methods**:
- `predictReadmission()` → `publishReadmissionPredictionEvent()`
- `stratifyRisk()` (in `RiskStratificationService`) → `publishRiskStratificationEvent()`

**Audit Integration**: `PredictiveAnalyticsAuditIntegration`

---

#### 9. hcc-service ⏳
**File**: `backend/modules/services/hcc-service/src/main/java/com/healthdata/hcc/service/RafCalculationService.java`

**Key Methods**:
- `calculateRafScore()` → `publishRafCalculationEvent()`
- `identifyDocumentationGaps()` (in `DocumentationGapService`) → `publishDocumentationGapEvent()`

**Audit Integration**: `HccAuditIntegration`

---

#### 10. quality-measure-service ⏳
**File**: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/QualityMeasureService.java`

**Key Methods**:
- `evaluateMeasure()` → `publishMeasureCalculationEvent()`
- `generateCdsRecommendation()` (in `CdsService`) → `publishCdsRecommendationEvent()`

**Audit Integration**: `QualityMeasureAuditIntegration`

---

#### 11. patient-service ⏳
**File**: `backend/modules/services/patient-service/src/main/java/com/healthdata/patient/service/PatientService.java`

**Key Methods**:
- `getPatient()` → `publishPatientDataAccessEvent()`
- `calculateRiskScore()` (in `RiskScoreService`) → `publishPatientRiskScoreEvent()`

**Audit Integration**: `PatientAuditIntegration`

---

#### 12. fhir-service ⏳
**File**: `backend/modules/services/fhir-service/src/main/java/com/healthdata/fhir/service/FhirResourceService.java`

**Key Methods**:
- `searchResources()` → `publishFhirQueryEvent()`
- `createResource()` → `publishFhirResourceCreateEvent()`
- `updateResource()` → `publishFhirResourceUpdateEvent()`
- `deleteResource()` → `publishFhirResourceDeleteEvent()`

**Audit Integration**: `FhirAuditIntegration`

---

### Phase 1 Services (2 remaining)

#### 13. care-gap-service ⏳
**File**: `backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/service/CareGapService.java`

**Key Methods**:
- `identifyCareGaps()` → `publishCareGapIdentificationEvent()`
- `closeCareGap()` → `publishCareGapClosureEvent()`

**Audit Integration**: `CareGapAuditIntegration`

---

#### 14. cql-engine-service ⏳
**File**: `backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/service/CqlEngineService.java`

**Key Methods**:
- `evaluateMeasure()` → `publishMeasureEvaluationEvent()`
- `evaluateBatch()` → `publishBatchEvaluationEvent()`

**Audit Integration**: `CqlAuditIntegration`

---

## Implementation Checklist

Use this checklist when integrating each service:

- [ ] Read service README to understand key decision points
- [ ] Locate main service class(es)
- [ ] Add audit integration field to constructor
- [ ] Identify 2-5 key methods for audit integration
- [ ] Add audit calls after successful operations
- [ ] Handle enum→string conversions if needed
- [ ] Verify compilation with `./gradlew :modules:services:SERVICE:compileJava`
- [ ] Run service-specific tests to ensure no regressions
- [ ] Document integration points in this file

---

## Common Patterns & Tips

### Pattern 1: Enum to String Conversion
```java
// ❌ Wrong - passing enum directly
audit.publishEvent(tenantId, entity.getStatus(), user);

// ✅ Correct - convert to string
audit.publishEvent(tenantId, entity.getStatus().name(), user);
```

### Pattern 2: Calculating Processing Time
```java
long startTime = System.currentTimeMillis();
// ... operation ...
long processingTime = System.currentTimeMillis() - startTime;

audit.publishEvent(tenantId, id, processingTime, user);
```

### Pattern 3: Handling Optional Fields
```java
audit.publishEvent(
    tenantId,
    entity.getId(),
    entity.getOptionalField() != null ? entity.getOptionalField() : "default",
    user
);
```

### Pattern 4: Success/Failure Tracking
```java
try {
    // Operation
    result = doSomething();
    audit.publishEvent(tenantId, id, true, null, user);
} catch (Exception e) {
    audit.publishEvent(tenantId, id, false, e.getMessage(), user);
    throw e;
}
```

---

## Verification Strategy

### Per-Service Verification
1. **Compilation**: `./gradlew :modules:services:SERVICE:compileJava`
2. **Unit Tests**: `./gradlew :modules:services:SERVICE:test`
3. **Manual Review**: Check audit calls are after successful operations

### System-Wide Verification
1. **Full Compilation**: `./gradlew compileJava --parallel`
2. **All Tests**: `./gradlew test` (expect pre-existing failures)
3. **Service Count**: Verify 15/15 services have audit calls

---

## Progress Tracking

| Service | Status | Compiler | Tests | Notes |
|---------|--------|----------|-------|-------|
| consent-service | ✅ Done | ✅ Pass | ⏳ Pending | 3 methods integrated |
| prior-auth-service | ✅ Done | ✅ Pass | ⏳ Pending | 2 methods integrated |
| ehr-connector-service | ⏳ TODO | - | - | - |
| cdr-processor-service | ⏳ TODO | - | - | - |
| approval-service | ⏳ TODO | - | - | - |
| payer-workflows-service | ⏳ TODO | - | - | - |
| agent-runtime-service | ✅ Done | ✅ Pass | ✅ Pass | Already integrated |
| predictive-analytics-service | ⏳ TODO | - | - | - |
| hcc-service | ⏳ TODO | - | - | - |
| quality-measure-service | ⏳ TODO | - | - | - |
| patient-service | ⏳ TODO | - | - | - |
| fhir-service | ⏳ TODO | - | - | - |
| care-gap-service | ⏳ TODO | - | - | - |
| cql-engine-service | ⏳ TODO | - | - | - |

**Progress**: 3/15 (20%) - Note: agent-runtime already done in Phase 2

---

## Estimated Effort

- **Per Service**: 15-30 minutes (depending on complexity)
- **Total Remaining**: 12 services × 20 min avg = **4 hours**
- **Verification**: 30 minutes
- **Total**: ~4.5 hours

---

## Next Steps

1. Continue systematic integration of remaining 12 services
2. Follow the 4-step pattern for each
3. Verify compilation after each service
4. Run full system verification at the end
5. Update progress tracking table
6. Create Part C: Phase 3 Heavyweight Tests

---

**Last Updated**: January 14, 2026  
**Completed By**: In Progress
