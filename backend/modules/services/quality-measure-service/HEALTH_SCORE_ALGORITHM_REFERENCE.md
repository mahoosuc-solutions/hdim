# Health Score Calculation Algorithm - Quick Reference

## Overview
This document provides a quick reference for the health score calculation algorithms implemented in Phase 3.2.

---

## Vital Sign Scoring Algorithms

### Blood Pressure - Systolic (mmHg)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| < 90 | -10 | Hypotension |
| 90-119 | +5 | Optimal |
| 120-129 | +2 | Elevated |
| 130-139 | -3 | Stage 1 Hypertension |
| 140-179 | -8 | Stage 2 Hypertension |
| ≥ 180 | -15 | Hypertensive Crisis |

**LOINC Code:** `8480-6`

### Blood Pressure - Diastolic (mmHg)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| < 60 | -10 | Hypotension |
| 60-79 | +5 | Optimal |
| 80-89 | -3 | Stage 1 Hypertension |
| 90-119 | -8 | Stage 2 Hypertension |
| ≥ 120 | -15 | Hypertensive Crisis |

**LOINC Code:** `8462-4`

### Heart Rate (bpm)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| < 40 | -10 | Severe Bradycardia |
| 40-59 | -3 | Bradycardia |
| 60-100 | +3 | Normal |
| 101-120 | -3 | Tachycardia |
| > 120 | -10 | Severe Tachycardia |

**LOINC Code:** `8867-4`

### Body Mass Index (kg/m²)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| < 16.0 | -15 | Severely Underweight |
| 16.0-18.4 | -8 | Underweight |
| 18.5-24.9 | +5 | Normal Weight |
| 25.0-29.9 | -3 | Overweight |
| 30.0-34.9 | -8 | Obese Class I |
| 35.0-39.9 | -12 | Obese Class II |
| ≥ 40.0 | -18 | Obese Class III |

**LOINC Code:** `39156-5`

### Blood Glucose (mg/dL)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| < 70 | -8 | Hypoglycemia |
| 70-100 | +5 | Normal |
| 101-125 | -2 | Prediabetes |
| 126-200 | -8 | Diabetes |
| > 200 | -15 | Severe Hyperglycemia |

**LOINC Codes:** `2339-0` (Random), `2345-7` (Fasting)

### Hemoglobin A1C (%)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| < 5.7 | +5 | Normal |
| 5.7-6.4 | -3 | Prediabetes |
| 6.5-6.9 | -6 | Diabetes (Controlled) |
| 7.0-8.9 | -10 | Diabetes (Poorly Controlled) |
| ≥ 9.0 | -15 | Diabetes (Very Poorly Controlled) |

**LOINC Code:** `4548-4`

### Oxygen Saturation (%)
| Range | Score Adjustment | Clinical Interpretation |
|-------|------------------|------------------------|
| ≥ 95 | +3 | Normal |
| 90-94 | -5 | Mild Hypoxemia |
| 85-89 | -10 | Moderate Hypoxemia |
| < 85 | -18 | Severe Hypoxemia |

**LOINC Code:** `2708-6`

---

## Chronic Condition Impact Scoring

### Condition Severity Multipliers
| Severity | SNOMED Code | Multiplier |
|----------|-------------|------------|
| Mild | 255604002 | 0.6x |
| Moderate | 6736007 | 1.0x |
| Severe | 24484000 | 1.5x |

### Base Condition Impacts (Before Severity Adjustment)

#### High Impact Conditions
| Condition | SNOMED CT | ICD-10 | Chronic Impact | Physical Impact |
|-----------|-----------|--------|----------------|-----------------|
| Cancer | 363406005 | C* | -25 | -22 |
| Congestive Heart Failure | 42343007 | I50 | -20 | -18 |
| Chronic Kidney Disease | 709044004 | N18 | -20 | -18 |
| Coronary Artery Disease | 53741008 | I25 | -18 | -15 |
| COPD | 13645005 | J44 | -18 | -15 |

#### Moderate Impact Conditions
| Condition | SNOMED CT | ICD-10 | Chronic Impact | Physical Impact |
|-----------|-----------|--------|----------------|-----------------|
| Diabetes Type 1 | 46635009 | E10 | -15 | -10 |
| Diabetes Type 2 | 44054006 | E11 | -12 | -8 |
| Asthma | 195967001 | J45 | -10 | -8 |
| Hypertension | 38341003 | I10 | -8 | -5 |

### Example Calculations

#### Mild Diabetes Type 2
```
Base Chronic Impact: -12 points
Base Physical Impact: -8 points
Severity Multiplier: 0.6 (mild)

Final Chronic Impact: -12 × 0.6 = -7.2 points
Final Physical Impact: -8 × 0.6 = -4.8 points
```

#### Severe Congestive Heart Failure
```
Base Chronic Impact: -20 points
Base Physical Impact: -18 points
Severity Multiplier: 1.5 (severe)

Final Chronic Impact: -20 × 1.5 = -30 points
Final Physical Impact: -18 × 1.5 = -27 points
```

---

## Overall Health Score Calculation

### Component Weights
```
Overall Score = (Physical Health × 0.30) +
                (Mental Health × 0.25) +
                (Social Determinants × 0.15) +
                (Preventive Care × 0.15) +
                (Chronic Disease Management × 0.15)
```

### Default Initial Scores
When no data exists for a patient:
- Physical Health: 75
- Mental Health: 75
- Social Determinants: 75
- Preventive Care: 75
- Chronic Disease Management: 75
- **Overall: 75**

### Score Ranges
All scores are bounded between 0-100:
- **85-100:** Excellent
- **70-84:** Good
- **55-69:** Fair
- **40-54:** Poor
- **0-39:** Critical

---

## Clinical Alert Triggers

### Condition-Based Alerts

#### HIGH Severity Alerts
Automatically triggered for:
- Congestive Heart Failure (any severity)
- Cancer (any severity)
- Chronic Kidney Disease (any severity)

#### MEDIUM Severity Alerts
Automatically triggered for:
- Coronary Artery Disease (any severity)
- COPD (any severity)
- Diabetes Type 1 (severe only)

### Score-Based Alerts
Automatically triggered when:
- Overall health score declines by ≥15 points
- Any component score drops below critical threshold

---

## Example Patient Scenarios

### Scenario 1: New Hypertension Diagnosis
**Initial State:**
- Overall Score: 75
- Physical Health: 75
- Chronic Disease: 75

**Event:** Moderate severity hypertension diagnosed (SNOMED: 38341003)

**Calculation:**
```
Chronic Impact: -8 × 1.0 (moderate) = -8 points
Physical Impact: -5 × 1.0 (moderate) = -5 points

New Chronic Score: 75 - 8 = 67
New Physical Score: 75 - 5 = 70

New Overall Score: (70 × 0.30) + (75 × 0.25) + (75 × 0.15) + (75 × 0.15) + (67 × 0.15)
                 = 21 + 18.75 + 11.25 + 11.25 + 10.05
                 = 72.3
```

**Result:** Score decreased from 75 → 72.3 (-2.7 points)

### Scenario 2: Elevated Blood Pressure Reading
**Initial State:**
- Overall Score: 78
- Physical Health: 80

**Event:** Systolic BP = 165 mmHg (Stage 2 Hypertension)

**Calculation:**
```
BP Impact: -8 points (140-179 range)
New Physical Score: 80 - 8 = 72

New Overall Score: (72 × 0.30) + (75 × 0.25) + (75 × 0.15) + (75 × 0.15) + (75 × 0.15)
                 = 21.6 + 18.75 + 11.25 + 11.25 + 11.25
                 = 74.1
```

**Result:** Score decreased from 78 → 74.1 (-3.9 points)

### Scenario 3: Multiple Conditions
**Initial State:**
- Overall Score: 75
- Physical Health: 75
- Chronic Disease: 75

**Events:**
1. Severe Diabetes Type 2 (SNOMED: 44054006, severity: 24484000)
2. Moderate Hypertension (SNOMED: 38341003, severity: 6736007)

**Calculation:**
```
Diabetes Impact:
  Chronic: -12 × 1.5 = -18
  Physical: -8 × 1.5 = -12

Hypertension Impact:
  Chronic: -8 × 1.0 = -8
  Physical: -5 × 1.0 = -5

Total Chronic Impact: -18 + -8 = -26
Total Physical Impact: -12 + -5 = -17

New Chronic Score: 75 - 26 = 49
New Physical Score: 75 - 17 = 58

New Overall Score: (58 × 0.30) + (75 × 0.25) + (75 × 0.15) + (75 × 0.15) + (49 × 0.15)
                 = 17.4 + 18.75 + 11.25 + 11.25 + 7.35
                 = 66.0
```

**Result:** Score decreased from 75 → 66.0 (-9 points)

**Alert:** No automatic alert (decline < 15 points)

### Scenario 4: Critical Condition
**Initial State:**
- Overall Score: 78
- Physical Health: 80
- Chronic Disease: 80

**Event:** Severe Congestive Heart Failure (SNOMED: 42343007, severity: 24484000)

**Calculation:**
```
CHF Impact:
  Chronic: -20 × 1.5 = -30
  Physical: -18 × 1.5 = -27

New Chronic Score: max(0, 80 - 30) = 50
New Physical Score: max(0, 80 - 27) = 53

New Overall Score: (53 × 0.30) + (75 × 0.25) + (75 × 0.15) + (75 × 0.15) + (50 × 0.15)
                 = 15.9 + 18.75 + 11.25 + 11.25 + 7.5
                 = 64.65
```

**Result:** Score decreased from 78 → 64.65 (-13.35 points)

**Alert:** HIGH severity alert triggered for CHF

---

## API Integration Examples

### Kafka Event: Observation Created
```json
{
  "tenantId": "org-abc",
  "patientId": "patient-123",
  "resource": {
    "resourceType": "Observation",
    "status": "final",
    "code": {
      "coding": [{
        "system": "http://loinc.org",
        "code": "8480-6",
        "display": "Systolic blood pressure"
      }]
    },
    "valueQuantity": {
      "value": 145,
      "unit": "mmHg",
      "system": "http://unitsofmeasure.org",
      "code": "mm[Hg]"
    },
    "subject": {
      "reference": "Patient/patient-123"
    }
  }
}
```

### Kafka Event: Condition Created
```json
{
  "tenantId": "org-abc",
  "patientId": "patient-123",
  "resource": {
    "resourceType": "Condition",
    "clinicalStatus": {
      "coding": [{
        "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
        "code": "active"
      }]
    },
    "severity": {
      "coding": [{
        "system": "http://snomed.info/sct",
        "code": "24484000",
        "display": "Severe"
      }]
    },
    "code": {
      "coding": [{
        "system": "http://snomed.info/sct",
        "code": "44054006",
        "display": "Diabetes mellitus type 2"
      }]
    },
    "subject": {
      "reference": "Patient/patient-123"
    }
  }
}
```

### Health Score Update Event (Published)
```json
{
  "patientId": "patient-123",
  "tenantId": "org-abc",
  "overallScore": 72.5,
  "previousScore": 75.0,
  "scoreDelta": -2.5,
  "calculatedAt": "2025-12-04T18:30:00Z"
}
```

### Condition Alert Event (Published)
```json
{
  "tenantId": "org-abc",
  "patientId": "patient-123",
  "conditionType": "DIABETES_TYPE_2",
  "conditionDisplay": "Diabetes mellitus type 2",
  "severity": "MEDIUM",
  "healthScore": 72.5
}
```

---

## Implementation Notes

### Score Boundaries
All score calculations enforce boundaries:
```java
double newScore = Math.max(0.0, Math.min(100.0, calculatedScore));
```

### Event Filtering
- Only processes observations with recognized LOINC codes
- Only processes conditions with "active" clinical status
- Only processes chronic conditions (not acute illnesses)

### Error Handling
- Missing fields: Skip processing, log warning
- Invalid values: Ignore adjustment
- Exceptions: Catch, log, continue processing

---

## Testing Checklist

### Unit Test Coverage
- [ ] BP systolic scoring (all ranges)
- [ ] BP diastolic scoring (all ranges)
- [ ] Heart rate scoring (all ranges)
- [ ] BMI scoring (all ranges)
- [ ] Glucose scoring (all ranges)
- [ ] A1C scoring (all ranges)
- [ ] Oxygen saturation scoring (all ranges)
- [ ] Condition impact calculation (all types)
- [ ] Severity multiplier application
- [ ] Alert triggering logic
- [ ] Score boundary enforcement

### Integration Test Coverage
- [ ] Full observation event flow
- [ ] Full condition event flow
- [ ] Multi-condition patient scenario
- [ ] Alert creation workflow
- [ ] Kafka event publishing
- [ ] Database persistence

---

This reference guide should be used in conjunction with the main implementation document for comprehensive understanding of the health score calculation system.
