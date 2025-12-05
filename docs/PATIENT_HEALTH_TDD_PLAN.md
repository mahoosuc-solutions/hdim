# Patient Health Service - TDD Implementation Plan

## Executive Summary

This document outlines the Test-Driven Development (TDD) plan for completing the patient health service features in the clinical portal application. Based on comprehensive analysis, we have identified 13 key features requiring implementation across 4 categories, with an estimated 11-sprint roadmap.

---

## Current Test Suite Status

- **Total Test Files**: 46
- **Passing**: 45 (98%)
- **Failing**: 1 (`patient-health.service.spec.ts`)

### Failing Test Categories

| Category | Tests | Root Cause |
|----------|-------|------------|
| FHIR Vital Signs | 6 | URL/HttpParams mismatch |
| FHIR Lab Results | 4 | Missing DIAGNOSTIC_REPORT endpoint |
| FHIR Caching | 2 | Inconsistent cache keys |
| FHIR Pagination | 2 | Non-standard URL patterns |
| Risk Stratification | 5+ | Missing HTTP mocks |
| SDOH Screening | 2 | Minor mock issues |

---

## Phase 1: Test Infrastructure Fixes (Sprint 1)

### Task 1.1: Fix API Configuration
**File**: `apps/clinical-portal/src/app/config/api.config.ts`

```typescript
// Add missing FHIR endpoint
export const FHIR_ENDPOINTS = {
  // ... existing endpoints
  DIAGNOSTIC_REPORT: '/DiagnosticReport',  // ADD
};
```

### Task 1.2: Fix Test URL Matching Pattern
**File**: `patient-health.service.spec.ts`

Replace URL string matching with HttpParams checking:
```typescript
// BEFORE
const req = httpMock.expectOne((request) =>
  request.url.includes('/fhir/Observation') &&
  request.url.includes(`patient=${testPatientId}`)
);

// AFTER
const req = httpMock.expectOne((request) =>
  request.url.includes('/fhir/Observation') &&
  request.params.get('patient') === testPatientId
);
```

### Task 1.3: Add HTTP Mocks for Risk Stratification Tests
Add `triggerRiskFallback()` helper similar to existing `triggerOverviewFallback()`.

### Task 1.4: Standardize Cache Key Generation
Ensure consistent pattern: `patientId` only (not `${patientId}-${options}`).

---

## Phase 2: FHIR Integration Completion (Sprints 2-4)

### Feature 2.1: Real-time Vital Signs
**Priority**: HIGH | **Complexity**: Medium

#### Test Cases (TDD)
```typescript
describe('Vital Signs FHIR Integration', () => {
  it('should fetch and map heart rate from FHIR Observation');
  it('should fetch and map blood pressure with systolic/diastolic');
  it('should map reference ranges for abnormal detection');
  it('should cache vital signs for 5 minutes');
  it('should handle missing observations gracefully');
  it('should support LOINC code filtering');
});
```

#### Implementation Steps
1. Wire `getVitalSignsFromFhir()` to `getPhysicalHealthSummary()`
2. Add WebSocket service for real-time updates
3. Create `VitalsChartComponent` for visualization

### Feature 2.2: Lab Results with Interpretations
**Priority**: HIGH | **Complexity**: Medium

#### Test Cases (TDD)
```typescript
describe('Lab Results FHIR Integration', () => {
  it('should fetch lab observations by category');
  it('should map FHIR interpretation codes (H, HH, L, LL)');
  it('should group results into panels (CBC, BMP, Lipid)');
  it('should calculate historical trends');
  it('should use DiagnosticReport for panel results');
});
```

#### Implementation Steps
1. Fix `getDiagnosticReportsFromFhir()` to use correct endpoint
2. Implement `mapFhirInterpretationCode()` method
3. Create panel grouping logic
4. Build `LabTrendChartComponent`

### Feature 2.3: Medication Adherence
**Priority**: HIGH | **Complexity**: High

#### Test Cases (TDD)
```typescript
describe('Medication Adherence', () => {
  it('should fetch active medications from MedicationRequest');
  it('should calculate PDC (Proportion of Days Covered)');
  it('should identify adherence gaps');
  it('should track refill history');
  it('should flag high-risk non-adherence');
});
```

#### New Service Method
```typescript
getMedicationAdherence(patientId: string): Observable<MedicationAdherenceDetail[]>
```

### Feature 2.4: Procedure History
**Priority**: MEDIUM | **Complexity**: Low

#### Test Cases (TDD)
```typescript
describe('Procedure History', () => {
  it('should fetch procedures from FHIR');
  it('should categorize procedures (surgical, diagnostic)');
  it('should sort by date descending');
  it('should include provider information');
});
```

---

## Phase 3: Patient Health Overview Enhancement (Sprints 5-6)

### Feature 3.1: Complete Health Score Backend Integration
**Priority**: MEDIUM | **Complexity**: Low

#### Test Cases (TDD)
```typescript
describe('Health Score Calculation', () => {
  it('should calculate score from backend response');
  it('should compute weighted average from components');
  it('should derive status from score ranges');
  it('should track historical scores for trend');
});
```

### Feature 3.2: Physical Health Summary - Full FHIR
**Priority**: HIGH | **Complexity**: Medium

#### Implementation
Replace all mock data calls with FHIR queries:
```typescript
getPhysicalHealthSummary(patientId: string): Observable<PhysicalHealthSummary> {
  return forkJoin({
    vitals: this.getVitalSignsFromFhir(patientId),
    labs: this.getLabResultsFromFhir(patientId),
    conditions: this.getConditionsFromFhir(patientId),
    medications: this.getMedicationAdherence(patientId),
  }).pipe(
    map(data => this.buildPhysicalHealthSummary(data))
  );
}
```

### Feature 3.3: Mental Health - Complete Backend Integration
**Priority**: HIGH | **Complexity**: Low

Already 90% complete. Remaining:
1. Replace `getMentalHealthSummary()` mock with backend call
2. Implement assessment history retrieval
3. Add trend calculation from historical data

### Feature 3.4: SDOH Screening - FHIR QuestionnaireResponse
**Priority**: HIGH | **Complexity**: Medium

#### Test Cases (TDD)
```typescript
describe('SDOH FHIR Integration', () => {
  it('should fetch QuestionnaireResponse for SDOH screening');
  it('should map responses to SDOHNeed objects');
  it('should extract ICD-10 Z-codes');
  it('should calculate risk level from needs');
});
```

---

## Phase 4: Risk Stratification Advanced Features (Sprints 7-8)

### Feature 4.1: Multi-factor Risk Score
**Priority**: HIGH | **Complexity**: Medium

#### Test Cases (TDD)
```typescript
describe('Multi-factor Risk Calculation', () => {
  it('should calculate clinical complexity score');
  it('should incorporate SDOH factors');
  it('should include mental health risk');
  it('should weight factors appropriately');
  it('should produce score 0-100');
});
```

### Feature 4.2: Hospitalization Predictions
**Priority**: MEDIUM | **Complexity**: High

#### New Backend API Required
```
POST /quality-measure/patient-health/predictions/{patientId}/hospitalization
Response: { probability30Day, probability90Day, confidence, factors }
```

#### Test Cases (TDD)
```typescript
describe('Hospitalization Predictions', () => {
  it('should return 30-day risk probability');
  it('should return 90-day risk probability');
  it('should include contributing factors');
  it('should provide confidence interval');
});
```

### Feature 4.3: Risk Category Assessments
**Priority**: MEDIUM | **Complexity**: Medium

#### Test Cases (TDD)
```typescript
describe('Category-Specific Risk', () => {
  it('should calculate diabetes risk from HbA1c trends');
  it('should calculate cardiovascular risk (ASCVD)');
  it('should assess mental health crisis risk');
  it('should identify high-risk respiratory patients');
});
```

### Feature 4.4: Risk Trend Tracking
**Priority**: LOW | **Complexity**: Medium

#### New Backend API Required
```
GET /quality-measure/patient-health/risk/{patientId}/history
```

---

## Phase 5: Care Management Features (Sprints 9-11)

### Feature 5.1: Care Gap Completion
**Priority**: HIGH | **Complexity**: Low

Already 85% complete. Verify backend connectivity and edge cases.

### Feature 5.2: Care Recommendations Engine
**Priority**: MEDIUM | **Complexity**: High

#### New Backend Service Required
Clinical Decision Support Service with evidence-based guidelines.

#### Test Cases (TDD)
```typescript
describe('Care Recommendations', () => {
  it('should generate recommendations from clinical guidelines');
  it('should prioritize by urgency');
  it('should support patient-specific personalization');
  it('should track implementation outcomes');
});
```

### Feature 5.3: SDOH Referral Management
**Priority**: MEDIUM | **Complexity**: High

#### New Data Models
```typescript
interface ReferralWorkflow {
  referralId: string;
  patientId: string;
  category: SDOHCategory;
  organization: CommunityResource;
  status: 'draft' | 'sent' | 'accepted' | 'completed' | 'cancelled';
  outcome?: ReferralOutcome;
}
```

#### Test Cases (TDD)
```typescript
describe('SDOH Referral Management', () => {
  it('should create referral with required fields');
  it('should track referral status transitions');
  it('should record referral outcomes');
  it('should search community resources by category');
});
```

---

## Implementation Roadmap

| Sprint | Focus | Features | Est. Tests |
|--------|-------|----------|-----------|
| 1 | Test Fixes | Infrastructure, configs | 20 fixes |
| 2-3 | FHIR Vitals/Labs | 2.1, 2.2 | 30 new |
| 4 | FHIR Meds/Procs | 2.3, 2.4 | 25 new |
| 5 | Health Score | 3.1, 3.2 | 20 new |
| 6 | Mental/SDOH | 3.3, 3.4 | 20 new |
| 7 | Risk Multi-factor | 4.1 | 15 new |
| 8 | Risk Predictions | 4.2, 4.3, 4.4 | 25 new |
| 9 | Care Gaps | 5.1 | 10 new |
| 10-11 | Recommendations/Referrals | 5.2, 5.3 | 35 new |

**Total New Tests**: ~200
**Total Test Count Target**: ~1,500

---

## TDD Workflow Guidelines

### For Each Feature:

1. **Write Failing Tests First**
   ```bash
   npx nx test clinical-portal --testNamePattern="Feature Name"
   # Expect failures
   ```

2. **Implement Minimum Code to Pass**
   - Focus on making tests green
   - Don't over-engineer

3. **Refactor with Confidence**
   - Tests provide safety net
   - Improve code quality

4. **Verify Full Suite**
   ```bash
   npx nx test clinical-portal
   # All tests should pass
   ```

### Test File Organization

```
src/app/services/
  patient-health.service.ts
  patient-health.service.spec.ts        # Unit tests
  patient-health.service.integration.spec.ts  # Integration tests (new)

src/testing/factories/
  patient-health.factory.ts             # Test data factories
  fhir-observation.factory.ts           # FHIR mock data
  risk-assessment.factory.ts            # Risk calculation mocks
```

---

## Success Criteria

### Phase 1 Complete When:
- [ ] All 46 test files pass
- [ ] patient-health.service.spec.ts has 0 failures
- [ ] API configuration includes all FHIR endpoints

### Phase 2 Complete When:
- [ ] Vital signs fetched from real FHIR
- [ ] Lab results include interpretations
- [ ] Medication adherence calculated
- [ ] 100% test coverage on FHIR integration

### Phase 3 Complete When:
- [ ] Health overview uses live backend data
- [ ] No mock data in production paths
- [ ] Mental health assessments persisted

### Phase 4 Complete When:
- [ ] Risk scores calculated from multiple factors
- [ ] Predictions available for 30/90 day horizons
- [ ] Risk trends visualized

### Phase 5 Complete When:
- [ ] Care gaps auto-detected
- [ ] Recommendations evidence-based
- [ ] SDOH referrals tracked end-to-end

---

## Appendix: Key Files Reference

| File | Purpose |
|------|---------|
| `patient-health.service.ts` | Core service (1900+ lines) |
| `patient-health.model.ts` | Type definitions |
| `api.config.ts` | Endpoint configuration |
| `fhir.model.ts` | FHIR type definitions |
| `care-gap.service.ts` | Care gap management |
| `patient-health-overview.component.ts` | Main UI component |
