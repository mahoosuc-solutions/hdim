# Health Score Calculation Examples

## Overview

The Health Score Service calculates a comprehensive health score (0-100) from five weighted components:

| Component | Weight | Description |
|-----------|--------|-------------|
| Physical Health | 30% | Vitals, labs, chronic conditions |
| Mental Health | 25% | PHQ-9, GAD-7 scores |
| Social Determinants | 15% | SDOH screening results |
| Preventive Care | 15% | Screening compliance |
| Chronic Disease Management | 15% | Care plan adherence, gap closure |

**Formula:**
```
Overall Score = (Physical × 0.30) + (Mental × 0.25) + (Social × 0.15) + (Preventive × 0.15) + (Chronic × 0.15)
```

---

## Example 1: Optimal Health Patient

### Component Scores
- Physical Health: 95.0
- Mental Health: 90.0
- Social Determinants: 85.0
- Preventive Care: 92.0
- Chronic Disease: 88.0

### Calculation
```
Overall Score = (95.0 × 0.30) + (90.0 × 0.25) + (85.0 × 0.15) + (92.0 × 0.15) + (88.0 × 0.15)
              = 28.5 + 22.5 + 12.75 + 13.8 + 13.2
              = 90.75
```

### Interpretation
- **Score Level:** Excellent (≥90)
- **Message:** "Excellent overall health. Continue current health management practices."
- **Trend:** New (no previous score)

---

## Example 2: Patient with Moderate Depression

### Initial Scores
- Physical Health: 75.0
- Mental Health: 80.0
- Social Determinants: 75.0
- Preventive Care: 72.0
- Chronic Disease: 73.0

**Initial Overall Score:** 75.3

### Event: PHQ-9 Assessment (Score: 12/27 - Moderate Depression)

#### Mental Health Score Calculation
1. Convert to percentage: 12/27 = 44.4%
2. Invert (higher score = worse health): 100 - 44.4 = 55.6
3. Apply severity adjustment: Moderate → cap between 50-69
4. **Mental Health Score: 55.6**

### Updated Scores
- Physical Health: 75.0
- Mental Health: 55.6 ← Updated
- Social Determinants: 75.0
- Preventive Care: 72.0
- Chronic Disease: 73.0

### New Overall Score Calculation
```
Overall Score = (75.0 × 0.30) + (55.6 × 0.25) + (75.0 × 0.15) + (72.0 × 0.15) + (73.0 × 0.15)
              = 22.5 + 13.9 + 11.25 + 10.8 + 10.95
              = 69.4
```

### Change Analysis
- **Previous Score:** 75.3
- **New Score:** 69.4
- **Delta:** -5.9 points
- **Significant Change:** No (threshold is ±10 points)
- **Trend:** Declining
- **Interpretation:** Fair health status

---

## Example 3: Care Gap Addressed (Preventive Care)

### Initial Scores
- Physical Health: 75.0
- Mental Health: 70.0
- Social Determinants: 70.0
- Preventive Care: 50.0 ← Low due to missing screenings
- Chronic Disease: 75.0

**Initial Overall Score:** 69.0

### Event: Colorectal Cancer Screening Completed

#### Score Update
- Preventive Care: 50.0 + 10.0 = **60.0**

### Updated Scores
- Physical Health: 75.0
- Mental Health: 70.0
- Social Determinants: 70.0
- Preventive Care: 60.0 ← Improved
- Chronic Disease: 75.0

### New Overall Score Calculation
```
Overall Score = (75.0 × 0.30) + (70.0 × 0.25) + (70.0 × 0.15) + (60.0 × 0.15) + (75.0 × 0.15)
              = 22.5 + 17.5 + 10.5 + 9.0 + 11.25
              = 70.75
```

### Change Analysis
- **Previous Score:** 69.0
- **New Score:** 70.75
- **Delta:** +1.75 points
- **Significant Change:** No
- **Trend:** Improving
- **Change Reason:** Care gap addressed (Preventive Care)

---

## Example 4: Significant Health Decline

### Initial Scores
- Physical Health: 85.0
- Mental Health: 80.0
- Social Determinants: 75.0
- Preventive Care: 80.0
- Chronic Disease: 82.0

**Initial Overall Score:** 81.4

### Events:
1. New chronic condition diagnosed (Type 2 Diabetes)
2. PHQ-9 showing severe depression (Score: 22/27)

#### Score Updates
- Physical Health: 85.0 → 75.0 (chronic condition impact)
- Mental Health: 80.0 → 22.0 (severe depression)
- Chronic Disease: 82.0 → 60.0 (new condition, poor control)

### Updated Scores
- Physical Health: 75.0
- Mental Health: 22.0 ← Major decline
- Social Determinants: 75.0
- Preventive Care: 80.0
- Chronic Disease: 60.0 ← Decline

### New Overall Score Calculation
```
Overall Score = (75.0 × 0.30) + (22.0 × 0.25) + (75.0 × 0.15) + (80.0 × 0.15) + (60.0 × 0.15)
              = 22.5 + 5.5 + 11.25 + 12.0 + 9.0
              = 60.25
```

### Change Analysis
- **Previous Score:** 81.4
- **New Score:** 60.25
- **Delta:** -21.15 points
- **Significant Change:** ✓ Yes (exceeds 10-point threshold)
- **Change Reason:** "Significant decline in health score: 21.2 points (81.4 → 60.2)"
- **Trend:** Declining
- **Interpretation:** Fair health status
- **Alert:** Published `health-score.significant-change` event

---

## Example 5: Progressive Improvement

### Baseline (Week 0)
- Overall Score: 55.0
- All components: ~55.0 average

### Week 4: Mental Health Treatment Started
- Mental Health: 55.0 → 70.0 (PHQ-9 improved to mild)
- **Overall Score:** 59.75 (+4.75)

### Week 8: Preventive Screenings Completed
- Preventive Care: 55.0 → 75.0
- **Overall Score:** 62.75 (+3.0)

### Week 12: Chronic Disease Control Improved
- Chronic Disease: 55.0 → 75.0
- Physical Health: 55.0 → 70.0 (better vitals)
- **Overall Score:** 69.75 (+7.0)

### Total Progress
- **Starting Score:** 55.0
- **Ending Score:** 69.75
- **Total Improvement:** +14.75 points
- **Significant Change:** Yes (at week 12)
- **Trend:** Improving consistently

---

## Mental Health Score Conversion Table

### PHQ-9 (Depression) - Max Score: 27

| Score Range | Severity | Mental Health Score | Interpretation |
|-------------|----------|---------------------|----------------|
| 0-4 | Minimal | 85-100 | No depression |
| 5-9 | Mild | 70-84 | Mild depression |
| 10-14 | Moderate | 50-69 | Moderate depression |
| 15-19 | Moderately Severe | 30-49 | Moderately severe |
| 20-27 | Severe | 0-29 | Severe depression |

### GAD-7 (Anxiety) - Max Score: 21

| Score Range | Severity | Mental Health Score | Interpretation |
|-------------|----------|---------------------|----------------|
| 0-4 | Minimal | 85-100 | No anxiety |
| 5-9 | Mild | 70-84 | Mild anxiety |
| 10-14 | Moderate | 50-69 | Moderate anxiety |
| 15-21 | Severe | 30-49 | Severe anxiety |

### PHQ-2 (Brief Depression Screen) - Max Score: 6

| Score Range | Result | Mental Health Score | Action |
|-------------|--------|---------------------|--------|
| 0-2 | Negative | 85-100 | No follow-up needed |
| 3-6 | Positive | 50-84 | Full PHQ-9 recommended |

---

## Score Level Classification

| Overall Score | Level | Color | Interpretation |
|---------------|-------|-------|----------------|
| 90-100 | Excellent | Green | Continue current practices |
| 75-89 | Good | Light Green | Minor improvements beneficial |
| 60-74 | Fair | Yellow | Several areas need attention |
| 40-59 | Poor | Orange | Multiple care gaps require attention |
| 0-39 | Critical | Red | Urgent intervention recommended |

---

## Significant Change Detection

### Threshold: ±10 points

**Triggers:**
- Absolute change ≥ 10 points from previous score
- Either improvement or decline

**Events Published:**
1. `health-score.updated` (always published)
2. `health-score.significant-change` (only when threshold exceeded)

**Event Payload:**
```json
{
  "patientId": "Patient/123",
  "tenantId": "tenant-abc",
  "overallScore": 60.25,
  "previousScore": 81.4,
  "scoreDelta": -21.15,
  "calculatedAt": "2025-11-25T10:30:00Z",
  "significantChange": true,
  "changeReason": "Significant decline in health score: 21.2 points (81.4 → 60.2)"
}
```

---

## Component Score Calculation Guidelines

### Physical Health (30%)
**Factors:**
- Vital signs (BP, HR, temp, resp rate)
- Lab results (A1C, cholesterol, creatinine)
- BMI
- Active chronic conditions
- Medication adherence

**Scoring:**
- 90-100: All vitals optimal, no uncontrolled conditions
- 75-89: Minor variations, well-controlled conditions
- 60-74: Some abnormal values, partially controlled
- 40-59: Multiple abnormal values, poorly controlled
- 0-39: Critical values, uncontrolled conditions

### Mental Health (25%)
**Factors:**
- PHQ-9 score (depression)
- GAD-7 score (anxiety)
- Other mental health assessments
- Treatment adherence

**Scoring:** See conversion tables above

### Social Determinants (15%)
**Factors:**
- Food security
- Housing stability
- Transportation access
- Social isolation
- Financial strain

**Scoring:**
- 90-100: No social barriers identified
- 75-89: 1-2 minor barriers
- 60-74: 2-3 moderate barriers
- 40-59: Multiple significant barriers
- 0-39: Severe social barriers affecting health

### Preventive Care (15%)
**Factors:**
- Age-appropriate screenings completed
- Immunizations up to date
- Annual wellness visits
- Dental/vision care

**Scoring:**
- 90-100: All screenings current
- 75-89: 1 screening overdue
- 60-74: 2-3 screenings overdue
- 40-59: Multiple screenings significantly overdue
- 0-39: No preventive care in >2 years

### Chronic Disease Management (15%)
**Factors:**
- Care plan adherence
- Open care gaps
- Disease control metrics
- Specialist follow-up

**Scoring:**
- 90-100: All conditions well-controlled, no gaps
- 75-89: Conditions controlled, 1 minor gap
- 60-74: Some control issues, 2-3 gaps
- 40-59: Poor control, multiple gaps
- 0-39: Uncontrolled conditions, urgent gaps

---

## API Response Example

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "patientId": "Patient/123",
  "tenantId": "tenant-abc",
  "overallScore": 69.4,
  "physicalHealthScore": 75.0,
  "mentalHealthScore": 55.6,
  "socialDeterminantsScore": 75.0,
  "preventiveCareScore": 72.0,
  "chronicDiseaseScore": 73.0,
  "calculatedAt": "2025-11-25T10:30:00Z",
  "previousScore": 75.3,
  "scoreDelta": -5.9,
  "significantChange": false,
  "changeReason": null,
  "scoreLevel": "fair",
  "interpretation": "Fair health status. Several areas could benefit from attention.",
  "trend": "declining",
  "componentScores": {
    "physical": 75,
    "mental": 56,
    "social": 75,
    "preventive": 72,
    "chronicDisease": 73
  }
}
```

---

## Testing Scenarios

### Test 1: Optimal Health
- **Input:** All components 85-95
- **Expected:** Overall score 88-92, "excellent" or "good"
- **Test Coverage:** Basic weighted calculation

### Test 2: Mental Health Impact
- **Input:** PHQ-9 moderate (12/27)
- **Expected:** Mental score 50-69, overall score decreased
- **Test Coverage:** Mental health scoring algorithm

### Test 3: Care Gap Closure
- **Input:** Preventive care gap addressed
- **Expected:** Preventive score +10, overall score improved
- **Test Coverage:** Event-driven updates

### Test 4: Significant Change
- **Input:** Score drops >10 points
- **Expected:** significantChange=true, event published
- **Test Coverage:** Threshold detection

### Test 5: Multi-tenant Isolation
- **Input:** Same patient ID, different tenants
- **Expected:** Separate scores maintained
- **Test Coverage:** Data isolation

### Test 6: History Tracking
- **Input:** Multiple score updates
- **Expected:** Full history maintained, trend analysis
- **Test Coverage:** Historical data

---

## Database Schema

### health_scores Table
```sql
CREATE TABLE health_scores (
    id UUID PRIMARY KEY,
    patient_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    overall_score DECIMAL(5,2) NOT NULL,
    physical_health_score DECIMAL(5,2) NOT NULL,
    mental_health_score DECIMAL(5,2) NOT NULL,
    social_determinants_score DECIMAL(5,2) NOT NULL,
    preventive_care_score DECIMAL(5,2) NOT NULL,
    chronic_disease_score DECIMAL(5,2) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    previous_score DECIMAL(5,2),
    significant_change BOOLEAN DEFAULT FALSE,
    change_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hs_patient_calc ON health_scores(patient_id, calculated_at DESC);
CREATE INDEX idx_hs_tenant_patient ON health_scores(tenant_id, patient_id);
CREATE INDEX idx_hs_significant_change ON health_scores(significant_change, calculated_at DESC);
```

### health_score_history Table
```sql
CREATE TABLE health_score_history (
    id UUID PRIMARY KEY,
    patient_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    overall_score DECIMAL(5,2) NOT NULL,
    physical_health_score DECIMAL(5,2) NOT NULL,
    mental_health_score DECIMAL(5,2) NOT NULL,
    social_determinants_score DECIMAL(5,2) NOT NULL,
    preventive_care_score DECIMAL(5,2) NOT NULL,
    chronic_disease_score DECIMAL(5,2) NOT NULL,
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    previous_score DECIMAL(5,2),
    score_delta DECIMAL(6,2),
    change_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hsh_patient_date ON health_score_history(patient_id, calculated_at DESC);
CREATE INDEX idx_hsh_tenant ON health_score_history(tenant_id, calculated_at DESC);
```
