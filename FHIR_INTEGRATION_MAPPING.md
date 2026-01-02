# FHIR Integration Mapping for Patient Health Overview

**Date**: November 20, 2025
**Purpose**: Map Patient Health Overview data models to FHIR R4 resources

---

## Executive Summary

This document provides the complete mapping between the Patient Health Overview system and FHIR R4 resources. It serves as a blueprint for replacing mock data with real FHIR server integration.

### FHIR Resources Required

| Component | FHIR Resources | Complexity |
|-----------|---------------|------------|
| Physical Health | Observation, Condition, MedicationStatement | Medium |
| Mental Health | QuestionnaireResponse, Observation, Condition | High |
| Social Determinants | Observation (SDOH), Condition (Z-codes), ServiceRequest | Medium |
| Risk Stratification | RiskAssessment, Observation | High |
| Care Gaps | DetectedIssue, CarePlan, Goal | Medium |

---

## 1. Physical Health Mapping

### 1.1 Vitals Signs

**TypeScript Interface**: `PhysicalHealthSummary.vitals`

**FHIR Resource**: `Observation` with vital signs category

```typescript
// Target Model
interface VitalSign<T> {
  value: T;
  unit: string;
  date: Date;
  status: 'normal' | 'abnormal' | 'critical';
  trend?: 'improving' | 'stable' | 'declining';
  referenceRange?: { low: number; high: number; };
}
```

**FHIR Query**:
```
GET /fhir/Observation?patient={patientId}&category=vital-signs&_sort=-date&_count=1
```

**FHIR to Model Mapping**:

```typescript
// Blood Pressure Example
{
  resourceType: "Observation",
  code: {
    coding: [{
      system: "http://loinc.org",
      code: "85354-9",  // Blood pressure
      display: "Blood pressure systolic & diastolic"
    }]
  },
  component: [
    {
      code: { coding: [{ code: "8480-6", display: "Systolic" }] },
      valueQuantity: { value: 120, unit: "mmHg" }
    },
    {
      code: { coding: [{ code: "8462-4", display: "Diastolic" }] },
      valueQuantity: { value: 80, unit: "mmHg" }
    }
  ],
  effectiveDateTime: "2025-11-20T10:00:00Z",
  interpretation: [{
    coding: [{
      system: "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
      code: "N"  // Normal
    }]
  }],
  referenceRange: [{
    low: { value: 90 },
    high: { value: 120 }
  }]
}

// Maps to:
bloodPressure: {
  value: "120/80",
  unit: "mmHg",
  date: new Date("2025-11-20T10:00:00Z"),
  status: "normal",  // from interpretation
  referenceRange: { low: 90, high: 120 }
}
```

**Vital Signs LOINC Codes**:

| Vital | LOINC Code | Unit |
|-------|------------|------|
| Blood Pressure | 85354-9 | mmHg |
| Heart Rate | 8867-4 | beats/min |
| Temperature | 8310-5 | °C or °F |
| Weight | 29463-7 | kg or lbs |
| Height | 8302-2 | cm or in |
| BMI | 39156-5 | kg/m² |
| Oxygen Saturation | 2708-6 | % |

### 1.2 Lab Results

**TypeScript Interface**: `PhysicalHealthSummary.labs`

**FHIR Query**:
```
GET /fhir/Observation?patient={patientId}&category=laboratory&_sort=-date&_count=20
```

**Common Lab LOINC Codes**:

| Lab Test | LOINC Code | Normal Range |
|----------|------------|--------------|
| HbA1c | 4548-4 | 4.0-5.6% |
| Glucose | 2345-7 | 70-100 mg/dL |
| Cholesterol (Total) | 2093-3 | <200 mg/dL |
| LDL | 18262-6 | <100 mg/dL |
| HDL | 2085-9 | >40 mg/dL |
| Triglycerides | 2571-8 | <150 mg/dL |
| Creatinine | 2160-0 | 0.7-1.3 mg/dL |

### 1.3 Chronic Conditions

**TypeScript Interface**: `PhysicalHealthSummary.chronicConditions`

**FHIR Resource**: `Condition` with `clinicalStatus = active`

**FHIR Query**:
```
GET /fhir/Condition?patient={patientId}&clinical-status=active&category=problem-list-item
```

**FHIR to Model Mapping**:

```typescript
{
  resourceType: "Condition",
  clinicalStatus: {
    coding: [{
      system: "http://terminology.hl7.org/CodeSystem/condition-clinical",
      code: "active"
    }]
  },
  verificationStatus: {
    coding: [{
      system: "http://terminology.hl7.org/CodeSystem/condition-ver-status",
      code: "confirmed"
    }]
  },
  severity: {
    coding: [{
      system: "http://snomed.info/sct",
      code: "6736007",  // moderate
      display: "Moderate"
    }]
  },
  code: {
    coding: [{
      system: "http://snomed.info/sct",
      code: "44054006",
      display: "Type 2 Diabetes Mellitus"
    }],
    text: "Type 2 Diabetes Mellitus"
  },
  onsetDateTime: "2020-05-15",
  recordedDate: "2025-10-01"
}

// Maps to:
{
  code: { system: "http://snomed.info/sct", code: "44054006", text: "Type 2 Diabetes Mellitus" },
  display: "Type 2 Diabetes Mellitus",
  severity: "moderate",
  onsetDate: new Date("2020-05-15"),
  controlled: true,  // Need additional logic based on recent observations
  lastReview: new Date("2025-10-01")
}
```

### 1.4 Medication Adherence

**FHIR Resource**: `MedicationStatement` with `adherence` extension

**FHIR Query**:
```
GET /fhir/MedicationStatement?patient={patientId}&status=active
```

**Adherence Calculation**:
```typescript
// Calculate PDC (Proportion of Days Covered)
const adherenceRate = (daysCovered / totalDays) * 100;

// Classification
const status = adherenceRate >= 80 ? 'excellent' :
               adherenceRate >= 60 ? 'good' : 'poor';
```

---

## 2. Mental Health Mapping

### 2.1 Mental Health Assessments (PHQ-9, GAD-7, PHQ-2)

**TypeScript Interface**: `MentalHealthSummary.assessments`

**FHIR Resource**: `QuestionnaireResponse` or `Observation`

**Option A: QuestionnaireResponse (Recommended)**

**FHIR Query**:
```
GET /fhir/QuestionnaireResponse?patient={patientId}&questionnaire=http://loinc.org|44249-1  // PHQ-9
GET /fhir/QuestionnaireResponse?patient={patientId}&questionnaire=http://loinc.org|69737-5  // GAD-7
GET /fhir/QuestionnaireResponse?patient={patientId}&questionnaire=http://loinc.org|55757-9  // PHQ-2
```

**PHQ-9 QuestionnaireResponse Example**:

```typescript
{
  resourceType: "QuestionnaireResponse",
  questionnaire: "http://loinc.org/44249-1",  // PHQ-9
  status: "completed",
  authored: "2025-11-15T14:30:00Z",
  item: [
    {
      linkId: "44250-9",  // Little interest or pleasure in doing things
      answer: [{ valueInteger: 2 }]  // 0=not at all, 1=several days, 2=more than half, 3=nearly every day
    },
    {
      linkId: "44255-8",  // Feeling down, depressed, or hopeless
      answer: [{ valueInteger: 2 }]
    },
    // ... 7 more questions
    {
      linkId: "44260-8",  // Total score
      answer: [{ valueInteger: 12 }]
    }
  ]
}

// Maps to:
{
  type: "PHQ-9",
  name: "Patient Health Questionnaire-9",
  score: 12,
  maxScore: 27,
  severity: "moderate",
  date: new Date("2025-11-15T14:30:00Z"),
  interpretation: "Moderate depression",
  positiveScreen: true,
  thresholdScore: 10,
  requiresFollowup: true
}
```

**LOINC Codes for Mental Health Assessments**:

| Assessment | LOINC Code | Questionnaire URL |
|------------|------------|-------------------|
| PHQ-9 | 44249-1 | http://loinc.org/q/44249-1 |
| GAD-7 | 69737-5 | http://loinc.org/q/69737-5 |
| PHQ-2 | 55757-9 | http://loinc.org/q/55757-9 |
| AUDIT-C | 75626-2 | http://loinc.org/q/75626-2 |
| PHQ-9 Total Score | 44261-6 | (observation) |
| GAD-7 Total Score | 70274-6 | (observation) |

**Option B: Observation for Total Scores**

```typescript
{
  resourceType: "Observation",
  code: {
    coding: [{
      system: "http://loinc.org",
      code: "44261-6",  // PHQ-9 total score
      display: "Patient Health Questionnaire 9 item (PHQ-9) total score [Reported]"
    }]
  },
  valueInteger: 12,
  effectiveDateTime: "2025-11-15T14:30:00Z",
  interpretation: [{
    coding: [{
      system: "http://snomed.info/sct",
      code: "6736007",
      display: "Moderate"
    }],
    text: "Moderate depression"
  }]
}
```

### 2.2 Mental Health Diagnoses

**FHIR Resource**: `Condition` with mental health category

**FHIR Query**:
```
GET /fhir/Condition?patient={patientId}&category=http://snomed.info/sct|74732009  // Mental disorder
```

**SNOMED CT Codes for Mental Health**:

| Diagnosis | SNOMED CT Code | Category |
|-----------|----------------|----------|
| Major Depressive Disorder | 370143000 | mood |
| Generalized Anxiety Disorder | 21897009 | anxiety |
| PTSD | 47505003 | trauma |
| Bipolar Disorder | 13746004 | mood |
| Alcohol Use Disorder | 7200002 | substance |
| Opioid Use Disorder | 191816009 | substance |

### 2.3 Suicide Risk Assessment

**FHIR Resource**: `RiskAssessment`

**FHIR Query**:
```
GET /fhir/RiskAssessment?patient={patientId}&condition=http://snomed.info/sct|225444004  // At risk for suicide
```

**FHIR to Model Mapping**:

```typescript
{
  resourceType: "RiskAssessment",
  status: "final",
  code: {
    coding: [{
      system: "http://snomed.info/sct",
      code: "225444004",
      display: "At risk for suicide"
    }]
  },
  subject: { reference: "Patient/patient-123" },
  occurrenceDateTime: "2025-11-15",
  prediction: [{
    outcome: {
      coding: [{
        system: "http://snomed.info/sct",
        code: "44301001",
        display: "Suicide"
      }]
    },
    probabilityDecimal: 0.25,  // 25% risk
    qualitativeRisk: {
      coding: [{
        system: "http://terminology.hl7.org/CodeSystem/risk-probability",
        code: "moderate"
      }]
    }
  }],
  mitigation: "Weekly therapy sessions, medication management, crisis plan in place"
}

// Maps to:
suicideRisk: {
  level: "moderate",
  factors: [
    { factor: "Previous attempt", severity: "high", modifiable: false },
    { factor: "Social isolation", severity: "moderate", modifiable: true }
  ],
  protectiveFactors: ["Strong family support", "Engaged in treatment"],
  lastAssessed: new Date("2025-11-15"),
  requiresIntervention: true
}
```

---

## 3. Social Determinants of Health (SDOH) Mapping

### 3.1 SDOH Screening Observations

**FHIR Resource**: `Observation` with SDOH category

**FHIR Query**:
```
GET /fhir/Observation?patient={patientId}&category=http://terminology.hl7.org/CodeSystem/observation-category|social-history
```

**LOINC Codes for SDOH Screening**:

| SDOH Domain | LOINC Code | Question |
|-------------|------------|----------|
| Food Insecurity | 88122-7 | Within the past 12 months, you worried that your food would run out before you got money to buy more |
| Housing Instability | 71802-3 | Housing status |
| Transportation | 93030-5 | Has lack of transportation kept you from medical appointments |
| Utility Assistance | 93031-3 | In the past 12 months has the electric, gas, oil, or water company threatened to shut off services |
| Interpersonal Safety | 76501-6 | How often does anyone, including family, physically hurt you |
| Financial Strain | 96777-8 | Accountant of money you have available each month |

**Example SDOH Observation**:

```typescript
{
  resourceType: "Observation",
  status: "final",
  category: [{
    coding: [{
      system: "http://terminology.hl7.org/CodeSystem/observation-category",
      code: "social-history"
    }]
  }],
  code: {
    coding: [{
      system: "http://loinc.org",
      code: "88122-7",
      display: "Within the past 12 months we worried whether our food would run out before we got money to buy more"
    }]
  },
  valueCodeableConcept: {
    coding: [{
      system: "http://loinc.org",
      code: "LA28397-0",
      display: "Often true"
    }]
  },
  effectiveDateTime: "2025-11-01"
}

// Maps to:
{
  category: "food-insecurity",
  description: "Patient reports food insecurity - often worried food would run out",
  severity: "moderate",
  identified: new Date("2025-11-01"),
  addressed: false
}
```

### 3.2 Z-Codes (ICD-10 Social Determinant Codes)

**FHIR Resource**: `Condition` with Z-code

**Common Z-Codes**:

| Z-Code | Description | SDOH Category |
|--------|-------------|---------------|
| Z59.0 | Homelessness | housing-instability |
| Z59.1 | Inadequate housing | housing-instability |
| Z59.4 | Lack of adequate food | food-insecurity |
| Z59.5 | Extreme poverty | financial-strain |
| Z59.6 | Low income | financial-strain |
| Z55.9 | Problems related to education | education |
| Z56.9 | Unspecified problems related to employment | employment |
| Z60.2 | Problems related to living alone | social-isolation |
| Z75.3 | Unavailability of health care facilities | transportation |

### 3.3 SDOH Referrals

**FHIR Resource**: `ServiceRequest` or `Task`

**FHIR Query**:
```
GET /fhir/ServiceRequest?patient={patientId}&category=http://snomed.info/sct|410606002  // Social service procedure
```

---

## 4. Risk Stratification Mapping

### 4.1 Risk Assessment Resource

**FHIR Resource**: `RiskAssessment`

**Example**:

```typescript
{
  resourceType: "RiskAssessment",
  status: "final",
  subject: { reference: "Patient/patient-123" },
  occurrenceDateTime: "2025-11-20",
  basis: [
    { reference: "Observation/hba1c-123" },
    { reference: "Condition/diabetes-456" },
    { reference: "MedicationStatement/metformin-789" }
  ],
  prediction: [
    {
      outcome: {
        text: "30-day hospital readmission"
      },
      probabilityDecimal: 0.18,  // 18%
      qualitativeRisk: {
        coding: [{
          code: "moderate"
        }]
      }
    }
  ]
}

// Maps to:
predictions: {
  hospitalizationRisk30Day: 18,
  hospitalizationRisk90Day: 35,
  edVisitRisk30Day: 22,
  readmissionRisk: 18
}
```

---

## 5. Care Gaps Mapping

### 5.1 Detected Issues

**FHIR Resource**: `DetectedIssue`

**Example**:

```typescript
{
  resourceType: "DetectedIssue",
  status: "final",
  code: {
    coding: [{
      system: "http://terminology.hl7.org/CodeSystem/v3-ActCode",
      code: "CAREGAP",
      display: "Care Gap"
    }]
  },
  severity: "high",
  patient: { reference: "Patient/patient-123" },
  identified: "2025-11-20",
  detail: "Patient due for HbA1c test - last test was 8 months ago",
  implicated: [
    { reference: "Condition/diabetes-456" }
  ]
}

// Maps to:
{
  id: "gap-001",
  category: "chronic-disease",
  title: "HbA1c Test Overdue",
  description: "Patient due for HbA1c test - last test was 8 months ago",
  priority: "high",
  dueDate: new Date("2025-10-15"),
  overdueDays: 36,
  measureId: "HEDIS_CDC",
  measureName: "Comprehensive Diabetes Care",
  recommendedActions: [
    "Schedule lab appointment for HbA1c",
    "Review diabetes management plan"
  ]
}
```

---

## 6. Implementation Strategy

### Phase 1: Read-Only Integration (Week 1-2)

1. **Create FHIR Service Layer**
   ```typescript
   // fhir-patient-health.service.ts
   export class FhirPatientHealthService {
     getVitals(patientId: string): Observable<VitalSign[]>
     getLabResults(patientId: string): Observable<LabResult[]>
     getChronicConditions(patientId: string): Observable<ChronicCondition[]>
     getMentalHealthAssessments(patientId: string): Observable<MentalHealthAssessment[]>
     // ...
   }
   ```

2. **Update PatientHealthService**
   ```typescript
   constructor(private fhirService: FhirPatientHealthService) {}

   getPatientHealthOverview(patientId: string): Observable<PatientHealthOverview> {
     return forkJoin({
       vitals: this.fhirService.getVitals(patientId),
       labs: this.fhirService.getLabResults(patientId),
       conditions: this.fhirService.getChronicConditions(patientId),
       // ...
     }).pipe(
       map(data => this.buildHealthOverview(patientId, data))
     );
   }
   ```

### Phase 2: Write Operations (Week 3-4)

1. **Mental Health Assessment Submission**
   ```typescript
   submitMentalHealthAssessment(
     patientId: string,
     type: MentalHealthAssessmentType,
     responses: Record<string, number>
   ): Observable<MentalHealthAssessment> {
     const questionnaireResponse = this.buildQuestionnaireResponse(patientId, type, responses);
     return this.fhirClient.create(questionnaireResponse);
   }
   ```

2. **Care Gap Updates**
   ```typescript
   markCareGapAddressed(gapId: string): Observable<void> {
     // Update DetectedIssue status
     // Create CarePlan or ServiceRequest as needed
   }
   ```

### Phase 3: Advanced Features (Week 5-6)

1. **Real-time Risk Calculation**
2. **Predictive Analytics Integration**
3. **Care Recommendation Engine**

---

## 7. FHIR Server Configuration

### Required FHIR Server Capabilities

```json
{
  "resourceType": "CapabilityStatement",
  "rest": [{
    "mode": "server",
    "resource": [
      {
        "type": "Patient",
        "interaction": ["read", "search-type"]
      },
      {
        "type": "Observation",
        "interaction": ["read", "search-type", "create"],
        "searchParam": [
          { "name": "patient", "type": "reference" },
          { "name": "category", "type": "token" },
          { "name": "code", "type": "token" },
          { "name": "_sort", "type": "string" }
        ]
      },
      {
        "type": "Condition",
        "interaction": ["read", "search-type", "create"]
      },
      {
        "type": "QuestionnaireResponse",
        "interaction": ["read", "search-type", "create"]
      },
      {
        "type": "RiskAssessment",
        "interaction": ["read", "search-type", "create"]
      },
      {
        "type": "MedicationStatement",
        "interaction": ["read", "search-type"]
      },
      {
        "type": "ServiceRequest",
        "interaction": ["read", "search-type", "create"]
      },
      {
        "type": "DetectedIssue",
        "interaction": ["read", "search-type", "create", "update"]
      }
    ]
  }]
}
```

---

## 8. Testing Strategy

### 8.1 Unit Tests

```typescript
describe('FhirPatientHealthService', () => {
  it('should map FHIR Observation to VitalSign', () => {
    const fhirObs = createMockBloodPressureObservation();
    const vitalSign = service.mapObservationToVitalSign(fhirObs);

    expect(vitalSign.value).toBe("120/80");
    expect(vitalSign.status).toBe("normal");
  });

  it('should calculate PHQ-9 score from QuestionnaireResponse', () => {
    const qr = createMockPHQ9Response();
    const assessment = service.mapQuestionnaireResponseToAssessment(qr);

    expect(assessment.score).toBe(12);
    expect(assessment.severity).toBe("moderate");
    expect(assessment.requiresFollowup).toBe(true);
  });
});
```

### 8.2 Integration Tests

Test against FHIR test server with synthetic patient data.

---

## 9. Security Considerations

1. **FHIR OAuth 2.0 / SMART on FHIR**
   - Implement SMART launch sequence
   - Store access tokens securely
   - Refresh tokens before expiration

2. **HIPAA Compliance**
   - Audit all FHIR resource access
   - Encrypt data in transit (TLS 1.2+)
   - Implement proper consent management

3. **Data Minimization**
   - Only request necessary FHIR resources
   - Use `_elements` parameter to limit fields
   - Implement pagination for large result sets

---

## 10. Performance Optimization

1. **Batch Requests**
   ```
   POST /fhir
   Bundle: batch
   - GET Observation?patient=X&category=vital-signs
   - GET Observation?patient=X&category=laboratory
   - GET Condition?patient=X&clinical-status=active
   ```

2. **Caching Strategy**
   - Cache vital signs for 5 minutes
   - Cache conditions for 1 hour
   - Invalidate cache on updates

3. **Lazy Loading**
   - Load basic health overview first
   - Lazy load mental health tab on click
   - Lazy load SDOH data on demand

---

## Summary

This mapping document provides a complete blueprint for integrating the Patient Health Overview with a FHIR R4 server. The phased approach ensures:

1. ✅ Standards-compliant implementation
2. ✅ Gradual migration from mock to real data
3. ✅ Comprehensive test coverage
4. ✅ Security and performance considerations
5. ✅ Scalable architecture for future enhancements

**Next Steps**: Begin Phase 1 implementation by creating the `FhirPatientHealthService` and implementing read-only operations for vital signs and lab results.
