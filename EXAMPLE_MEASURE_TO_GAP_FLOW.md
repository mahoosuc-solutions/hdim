# Example: Complete Measure → Care Gap Transformation

## Scenario: Breast Cancer Screening Gap for Low-Risk Patient

### Step 1: Quality Measure Calculation Result

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "healthsystem-001",
  "patientId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "measureId": "CMS125",
  "measureName": "Breast Cancer Screening",
  "measureCategory": "HEDIS",
  "measureYear": 2024,
  "denominatorEligible": true,    // ✅ Patient is female, age 52
  "numeratorCompliant": false,    // ❌ No mammogram in past 2 years
  "complianceRate": 0.0,
  "score": 0.0,
  "calculationDate": "2024-11-25",
  "cqlLibrary": "BreastCancerScreening-v1.0.0",
  "cqlResult": "{\"denominator\": true, \"numerator\": false, \"lastMammogram\": \"2021-08-15\"}"
}
```

### Step 2: Patient Risk Assessment Lookup

```sql
SELECT * FROM risk_assessments 
WHERE tenant_id = 'healthsystem-001' 
  AND patient_id = 'a1b2c3d4-5678-90ab-cdef-1234567890ab'
ORDER BY assessment_date DESC 
LIMIT 1;
```

**Result:**
```json
{
  "id": "risk-assessment-123",
  "tenantId": "healthsystem-001",
  "patientId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "riskScore": 22,
  "riskLevel": "LOW",
  "chronicConditionCount": 0,
  "riskFactors": [
    {"factor": "age_50_64", "impact": "low"},
    {"factor": "no_chronic_conditions", "impact": "protective"}
  ],
  "assessmentDate": "2024-11-01"
}
```

### Step 3: Kafka Event Received

**Topic:** `measure-calculated`
**Event:**
```json
{
  "measureResultId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "healthsystem-001",
  "patientId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "measureId": "CMS125",
  "measureName": "Breast Cancer Screening",
  "numeratorCompliant": false,
  "denominatorEligible": true
}
```

### Step 4: CareGapDetectionService Analysis

**Detection Logic:**
```java
// 1. Check if gap should be created
shouldCreateGap(measureResult)
  → denominatorEligible: true ✅
  → numeratorCompliant: false ✅
  → RESULT: GAP SHOULD BE CREATED

// 2. Check for duplicate
existsOpenCareGap("healthsystem-001", "patient-id", "measure-gap-cms125")
  → RESULT: false (no existing gap)

// 3. Get measure metadata
measureMetadata = MEASURE_METADATA.get("CMS125")
  → name: "Breast Cancer Screening"
  → category: SCREENING
  → recommendation: "Schedule mammography screening..."

// 4. Determine priority
prioritizationService.determinePriority(
  tenantId: "healthsystem-001",
  patientId: "patient-id",
  measureId: "CMS125",
  category: SCREENING
)
  → Risk Level: LOW
  → Category: SCREENING
  → PRIORITY: MEDIUM

// 5. Calculate due date
calculateDueDate(MEDIUM)
  → MEDIUM = 30 days
  → DUE DATE: 2024-12-25
```

### Step 5: Care Gap Created

**Database Insert:**
```sql
INSERT INTO care_gaps (
  id, tenant_id, patient_id, category, gap_type, title, description,
  priority, status, quality_measure, measure_result_id, created_from_measure,
  recommendation, evidence, due_date, identified_date, created_at, updated_at
) VALUES (
  'gap-123-456-789',
  'healthsystem-001',
  'a1b2c3d4-5678-90ab-cdef-1234567890ab',
  'SCREENING',
  'measure-gap-cms125',
  'Breast Cancer Screening - Care Gap Identified',
  'Patient is eligible for Breast Cancer Screening (CMS125) but does not meet compliance criteria. This quality measure was calculated on 2024-11-25 and identified a care gap requiring clinical action.',
  'MEDIUM',
  'OPEN',
  'CMS125',
  '550e8400-e29b-41d4-a716-446655440000',
  true,
  'Schedule mammography screening:
1. Order bilateral mammogram
2. Provide patient education on importance of screening
3. Address any barriers to screening
4. Schedule appointment within 30 days',
  'Quality Measure: Breast Cancer Screening (CMS125)
Measure Year: 2024
Calculation Date: 2024-11-25
Denominator Eligible: true
Numerator Compliant: false
CQL Library: BreastCancerScreening-v1.0.0',
  '2024-12-25',
  '2024-11-25 00:00:00',
  '2024-11-25 10:30:00',
  '2024-11-25 10:30:00'
);
```

### Step 6: Care Gap JSON (API Response)

```json
{
  "id": "gap-123-456-789",
  "patientId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "category": "screening",
  "gapType": "measure-gap-cms125",
  "title": "Breast Cancer Screening - Care Gap Identified",
  "description": "Patient is eligible for Breast Cancer Screening (CMS125) but does not meet compliance criteria. This quality measure was calculated on 2024-11-25 and identified a care gap requiring clinical action.",
  "priority": "medium",
  "status": "open",
  "qualityMeasure": "CMS125",
  "recommendation": "Schedule mammography screening:\n1. Order bilateral mammogram\n2. Provide patient education on importance of screening\n3. Address any barriers to screening\n4. Schedule appointment within 30 days",
  "evidence": "Quality Measure: Breast Cancer Screening (CMS125)\nMeasure Year: 2024\nCalculation Date: 2024-11-25\nDenominator Eligible: true\nNumerator Compliant: false\nCQL Library: BreastCancerScreening-v1.0.0",
  "dueDate": "2024-12-25T00:00:00Z",
  "identifiedDate": "2024-11-25T00:00:00Z",
  "createdAt": "2024-11-25T10:30:00Z",
  "updatedAt": "2024-11-25T10:30:00Z"
}
```

### Step 7: UI Display (Clinical Portal)

**Care Gaps Dashboard:**

```
┌─────────────────────────────────────────────────────────────────┐
│  Patient: Jane Doe (DOB: 1972-03-15)                          │
│  MRN: 123456                                                   │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  OPEN CARE GAPS                                     Total: 1   │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  🟡 MEDIUM PRIORITY                       Due: Dec 25, 2024    │
│  Breast Cancer Screening - Care Gap Identified                 │
│  ────────────────────────────────────────────────────────────  │
│  Category: Screening  |  Measure: CMS125                       │
│  Identified: Nov 25, 2024                                      │
│                                                                 │
│  📋 RECOMMENDED ACTIONS:                                        │
│  1. Order bilateral mammogram                                  │
│  2. Provide patient education on importance of screening       │
│  3. Address any barriers to screening                          │
│  4. Schedule appointment within 30 days                        │
│                                                                 │
│  📊 EVIDENCE:                                                   │
│  Quality Measure: Breast Cancer Screening (CMS125)             │
│  Last mammogram: Aug 15, 2021 (3+ years ago)                  │
│  Patient is eligible (female, age 52)                          │
│                                                                 │
│  [Address Gap] [Dismiss] [View Details]                        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Scenario 2: High-Risk Diabetes Patient (URGENT Priority)

### Measure Result
```json
{
  "measureId": "CMS134",
  "measureName": "Diabetes: HbA1c Control",
  "denominatorEligible": true,  // Patient has diabetes
  "numeratorCompliant": false   // HbA1c > 9%
}
```

### Patient Risk
```json
{
  "riskLevel": "HIGH",
  "riskScore": 87,
  "chronicConditionCount": 4
}
```

### Priority Calculation
```
HIGH risk + CHRONIC_DISEASE category
→ PRIORITY: URGENT
→ DUE DATE: 7 days (Dec 2, 2024)
```

### Created Gap
```json
{
  "title": "Diabetes: HbA1c Control - Care Gap Identified",
  "priority": "urgent",
  "dueDate": "2024-12-02",
  "recommendation": "Diabetes HbA1c management:\n1. Order HbA1c test immediately\n2. Review current diabetes medications\n3. Assess adherence and barriers\n4. Consider medication adjustment if HbA1c >9%\n5. Schedule diabetes education if needed\n6. Follow up in 2 weeks to review results"
}
```

---

## Deduplication Example

### First Calculation (Nov 25)
```
Measure CMS125 calculated → Gap created: "measure-gap-cms125"
Status: OPEN
```

### Recalculation (Nov 30)
```
Measure CMS125 recalculated → Check for existing gap
existsOpenCareGap("tenant", "patient", "measure-gap-cms125")
→ Returns: true
→ SKIP: Gap already exists
```

### After Gap Addressed
```
Care Manager addresses gap → Status changed to ADDRESSED
```

### Next Calculation (Jan 15)
```
Measure CMS125 calculated again → Check for existing gap
existsOpenCareGap(...) only checks OPEN/IN_PROGRESS
→ Returns: false (previous gap is ADDRESSED)
→ CREATE: New gap if still non-compliant
```

---

## Summary

**Input:** Quality measure calculation shows patient eligible but not compliant
**Processing:** 
1. Kafka event triggers detection
2. Risk assessment determines priority
3. Deduplication prevents duplicates
4. Care gap created with clinical recommendations

**Output:** Actionable care gap in clinical workflow with:
- ✅ Risk-based priority
- ✅ Calculated due date
- ✅ Measure-specific recommendations
- ✅ Evidence trail
- ✅ Multi-tenant isolation
- ✅ No duplicates
