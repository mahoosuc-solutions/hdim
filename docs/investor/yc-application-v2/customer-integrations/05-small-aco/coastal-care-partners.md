# Coastal Care Partners - Small ACO

> Multi-FHIR integration for a 12-practice ACO with mixed EHR environments.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Accountable Care Organization (ACO) |
| **Size** | 12 practices, 45 providers, 8,500 attributed lives |
| **Structure** | Independent practices under shared savings agreement |
| **Location** | Coastal Maine (Portland metro + rural) |
| **EHR Systems** | Epic (3), Cerner (2), athenahealth (7) |
| **Quality Programs** | ACO REACH, MIPS |
| **IT Capabilities** | Moderate (ACO has 1 data analyst) |
| **HDIM Tier** | Enterprise |
| **Monthly Cost** | $999/month |

## Challenge

### Current State

Coastal Care Partners formed 3 years ago when 12 independent practices came together to participate in ACO REACH. The practices range from solo physicians to a 10-provider multispecialty group. They share in savings (or losses) based on total cost of care and quality scores for their attributed Medicare patients.

The ACO hired a data analyst, Jennifer, to manage quality reporting. She spends most of her time requesting data from 12 different practices, cleaning it in Excel, and trying to create a unified view. Each practice runs its own EHR: 3 use Epic (different instances), 2 use Cerner, and 7 use various flavors of athenahealth.

Last year, they achieved only 62% of potential shared savings because their quality score dragged down their multiplier. They have no visibility into performance until months after the measurement period ends.

### Pain Points

- **Data fragmentation:** 12 practices, 3 EHR platforms, no unified view
- **Attribution lag:** Don't know who their patients are until CMS tells them (3-month lag)
- **Quality score impact:** Lost $180,000 in shared savings due to quality multiplier
- **Provider engagement:** Practices don't see their individual contribution
- **No real-time feedback:** Performance data is always 60-90 days stale
- **Care coordination gaps:** Can't identify high-risk patients across the network
- **Resource constraints:** One data analyst trying to manage 12 data sources

### Why HDIM

The ACO's medical director saw HDIM at an ACO conference. The ability to connect multiple EHRs through a single platform was the key differentiator. Jennifer could stop being a data janitor and start being a data analyst.

A pilot with 3 practices proved the concept. The full rollout followed 60 days later.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Coastal Care Partners ACO                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  EPIC PRACTICES (3)                                                │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐                      │
│  │ Coastal    │ │ Harbor     │ │ Peninsula  │                      │
│  │ Primary    │ │ Medical    │ │ Family     │                      │
│  │ (Epic)     │ │ (Epic)     │ │ (Epic)     │                      │
│  └─────┬──────┘ └─────┬──────┘ └─────┬──────┘                      │
│        │              │              │                              │
│  CERNER PRACTICES (2)                                              │
│  ┌────────────┐ ┌────────────┐      │                              │
│  │ Lighthouse │ │ Bayview    │      │                              │
│  │ Cardiology │ │ Internal   │      │                              │
│  │ (Cerner)   │ │ (Cerner)   │      │                              │
│  └─────┬──────┘ └─────┬──────┘      │                              │
│        │              │              │                              │
│  ATHENAHEALTH PRACTICES (7)         │                              │
│  ┌────────────┐ ┌────────────┐      │                              │
│  │ Multiple   │ │ (7 total   │      │                              │
│  │ Practices  │ │ practices) │      │                              │
│  │ (athena)   │ │            │      │                              │
│  └─────┬──────┘ └─────┬──────┘      │                              │
│        │              │              │                              │
└────────┼──────────────┼──────────────┼──────────────────────────────┘
         │              │              │
         │    FHIR R4   │    FHIR R4   │    FHIR R4
         │              │              │
         ▼              ▼              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │               Multi-Source Data Aggregator                    │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                      │  │
│  │  │  Epic   │  │ Cerner  │  │ athena  │                      │  │
│  │  │Connector│  │Connector│  │Connector│                      │  │
│  │  └────┬────┘  └────┬────┘  └────┬────┘                      │  │
│  │       │            │            │                            │  │
│  │       └────────────┼────────────┘                            │  │
│  │                    │                                         │  │
│  │           ┌────────▼────────┐                                │  │
│  │           │ Patient Matching│                                │  │
│  │           │ & Deduplication │                                │  │
│  │           └────────┬────────┘                                │  │
│  └────────────────────┼─────────────────────────────────────────┘  │
│                       │                                             │
│              ┌────────▼────────┐                                   │
│              │   CQL Engine    │                                   │
│              │  (ACO REACH)    │                                   │
│              └────────┬────────┘                                   │
│                       │                                             │
│       ┌───────────────┼───────────────┐                            │
│       │               │               │                             │
│ ┌─────▼─────┐  ┌──────▼──────┐  ┌─────▼─────┐                     │
│ │   ACO     │  │  Practice   │  │ Attributed│                     │
│ │ Dashboard │  │ Scorecards  │  │ Patient   │                     │
│ │           │  │             │  │ Registry  │                     │
│ └───────────┘  └─────────────┘  └───────────┘                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: Multi-FHIR Aggregation

| EHR | Practices | Patients | Auth Method | Sync |
|-----|-----------|----------|-------------|------|
| Epic | 3 | 2,800 | SMART on FHIR | Daily |
| Cerner | 2 | 1,200 | OAuth 2.0 | Daily |
| athenahealth | 7 | 4,500 | OAuth 2.0 | Daily |

### FHIR Connection Details

| Connection | Endpoint | Scopes |
|------------|----------|--------|
| Epic (3 instances) | [instance].epic.com/fhir/r4 | Patient, Condition, MedicationRequest, Observation, Encounter |
| Cerner (2 instances) | fhir.[org].cerner.com | Same as above |
| athenahealth (7 practices) | api.athenahealth.com/fhir/r4 | Same as above |

### Attribution Management

| Source | Frequency | Use |
|--------|-----------|-----|
| CMS ACO File | Quarterly | Official attribution |
| Claims Prediction | Monthly | Prospective identification |
| HDIM Matching | Real-time | Patient-practice linkage |

---

## Sample Data Flow

### Patient Seen at Multiple Practices

```
Patient: John Smith, DOB 1952-04-18, Medicare ID: 1EG4-TE5-MK72

Source 1: Coastal Primary (Epic)
├── Patient ID: CPM-12345
├── Last Visit: 2024-10-01
├── Conditions: E11.9 (Diabetes), I10 (HTN)
├── Meds: Metformin, Lisinopril
└── Labs: HbA1c 7.8% (2024-09-15)

Source 2: Lighthouse Cardiology (Cerner)
├── Patient ID: LC-67890
├── Last Visit: 2024-09-20
├── Conditions: I25.10 (CAD), I10 (HTN)
├── Meds: Atorvastatin, Aspirin
└── Labs: LDL 98 (2024-09-20)

HDIM Unified Record:
├── HDIM ID: hdim-js-19520418
├── Attributed Practice: Coastal Primary
├── All Conditions: E11.9, I10, I25.10
├── All Medications: Metformin, Lisinopril, Atorvastatin, Aspirin
├── Latest HbA1c: 7.8%
├── Latest LDL: 98
└── Care Gaps: Flu vaccine (overdue), Eye exam (overdue)
```

### FHIR Bundle (Aggregated)

```json
{
  "resourceType": "Bundle",
  "type": "collection",
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "hdim-js-19520418",
        "identifier": [
          {"system": "http://medicare.gov", "value": "1EG4-TE5-MK72"},
          {"system": "http://coastal-primary.org", "value": "CPM-12345"},
          {"system": "http://lighthouse-cardio.org", "value": "LC-67890"}
        ],
        "name": [{"family": "Smith", "given": ["John"]}],
        "birthDate": "1952-04-18",
        "gender": "male"
      }
    },
    {
      "resource": {
        "resourceType": "Condition",
        "code": {"coding": [{"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "E11.9"}]},
        "subject": {"reference": "Patient/hdim-js-19520418"}
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4"}]},
        "valueQuantity": {"value": 7.8, "unit": "%"},
        "effectiveDateTime": "2024-09-15",
        "subject": {"reference": "Patient/hdim-js-19520418"}
      }
    }
  ]
}
```

---

## Implementation Steps

### Phase 1: Epic Practices (Week 1-2)

- [x] Epic App Orchard registration (if not existing)
- [x] Configure SMART on FHIR for 3 Epic instances
- [x] Complete OAuth authorization at each practice
- [x] Initial data sync for 2,800 patients
- [x] Validate patient matching

### Phase 2: Cerner Practices (Week 2-3)

- [x] Cerner CODE program registration
- [x] Configure Cerner FHIR access for 2 instances
- [x] Complete OAuth authorization
- [x] Sync 1,200 patients
- [x] Validate cross-EHR matching

### Phase 3: athenahealth Practices (Week 3-4)

- [x] athenahealth Marketplace authorization
- [x] Configure access for 7 practices
- [x] Complete OAuth for each practice
- [x] Sync 4,500 patients
- [x] Final deduplication and matching

### Phase 4: Go-Live (Week 5)

- [x] Upload CMS attribution file
- [x] Match attributed lives to clinical data
- [x] Calculate baseline quality scores
- [x] Train ACO staff and practice champions
- [x] Launch practice scorecards

---

## Measures Enabled

### ACO REACH Quality Measures

| Measure | Domain | Weight | Baseline | Target |
|---------|--------|--------|----------|--------|
| All-Cause Readmission | Utilization | 4x | 15.2% | <13% |
| Diabetes: HbA1c Poor Control | Chronic | 3x | 28% | <18% |
| Controlling High Blood Pressure | Chronic | 3x | 62% | >75% |
| Depression Screening & Follow-up | Behavioral | 2x | 48% | >70% |
| Colorectal Cancer Screening | Preventive | 2x | 55% | >70% |
| Breast Cancer Screening | Preventive | 2x | 68% | >78% |
| Statin Therapy for ASCVD | Medication | 2x | 72% | >85% |
| Tobacco Screening | Preventive | 1x | 78% | >90% |

### Practice-Level Scorecard

| Practice | Diabetes | BP | Colorectal | Depression | Composite |
|----------|----------|-----|------------|------------|-----------|
| Coastal Primary | 75% | 72% | 62% | 58% | 67% |
| Harbor Medical | 82% | 78% | 68% | 72% | 75% |
| Peninsula Family | 68% | 65% | 48% | 45% | 57% |
| Lighthouse Cardio | N/A | 82% | N/A | 52% | 67% |
| Bayview Internal | 78% | 75% | 58% | 62% | 68% |
| athena Practice 1 | 72% | 68% | 52% | 55% | 62% |
| athena Practice 2 | 65% | 62% | 45% | 48% | 55% |
| ... | ... | ... | ... | ... | ... |
| **ACO Average** | **72%** | **70%** | **55%** | **55%** | **63%** |

---

## Expected Outcomes

### Quality Score Impact on Shared Savings

| Scenario | Quality Score | Savings Multiplier | Shared Savings |
|----------|---------------|-------------------|----------------|
| **Baseline (2023)** | 63% | 0.62 | $310,000 |
| **With HDIM (2024)** | 82% | 0.82 | $410,000 |
| **Improvement** | +19 pts | +0.20 | **+$100,000** |

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| Data collection from practices | 40 hrs/month | 0 (automated) | 40 hrs/month |
| Data cleaning and normalization | 30 hrs/month | 2 hrs/month | 28 hrs/month |
| Practice scorecard generation | 20 hrs/month | 1 hr/month | 19 hrs/month |
| Attribution matching | 15 hrs/quarter | 2 hrs/quarter | 13 hrs/quarter |
| **Total** | **95 hrs/month** | **7 hrs/month** | **88 hrs/month** |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **Additional Shared Savings** | $100,000 |
| **MIPS Bonus (aggregate)** | $45,000 |
| **Staff Time Savings** | $52,800 (88 hrs × $50/hr × 12) |
| **Avoided Data Analyst Hire** | $65,000 |
| **Total Annual Value** | **$262,800** |

### ROI Calculation

```
Annual Value:        $262,800
Annual HDIM Cost:    $11,988 ($999 × 12)
Net Annual Benefit:  $250,812
ROI:                 22x
Payback Period:      17 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Enterprise tier | $999/month |
| 12 FHIR connections | Included (3 standard) |
| Additional connections (9 × $300) | $2,700/month |
| **Total Monthly** | **$3,699/month** |
| **Annual Cost** | **$44,388/year** |

*Note: With 8,500 attributed lives, cost is $0.44/patient/month*

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Data sync uptime (all 12) | >99% | HDIM monitoring |
| Patient matching accuracy | >98% | Manual audit |
| ACO quality score | >80% | CMS reporting |
| Practice score variation | <15 pt spread | HDIM dashboard |
| Attribution coverage | >95% | CMS file match |

---

## Jennifer's Testimonial

> "I went from being a data janitor to being a data strategist. Before HDIM, I spent 90% of my time just getting data into Excel. Now I spend 90% of my time analyzing it and helping practices improve. We identified that Peninsula Family was struggling with depression screening—turns out they didn't have the PHQ-9 built into their workflow. We fixed it in a week and their score went from 45% to 72%. Our quality score went from 63% to 82%, which meant an extra $100K in shared savings. The practices actually want to see their scorecards now."
>
> — Jennifer Walsh, ACO Data Analyst

---

## Related Resources

- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [ACO Measure Sets](../_shared/MEASURE_SETS.md#aco-measures)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
