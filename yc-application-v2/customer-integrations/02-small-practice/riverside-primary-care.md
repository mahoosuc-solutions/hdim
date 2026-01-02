# Riverside Primary Care - Small Practice

> FHIR API integration for a 6-physician primary care practice using athenahealth.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Small Group Practice |
| **Size** | 6 physicians, 4 NPs, 8 MAs, 3 admin |
| **Patient Count** | 4,500 active patients |
| **Location** | Riverside, CA (suburban) |
| **EHR System** | athenahealth |
| **Quality Programs** | MIPS, Blue Shield P4P |
| **IT Capabilities** | Basic (office manager handles IT) |
| **HDIM Tier** | Professional |
| **Monthly Cost** | $299/month |

## Challenge

### Current State

Riverside Primary Care has grown from 2 physicians to 6 in the past 5 years. They chose athenahealth because of its cloud-based model and revenue cycle management. Their office manager, Lisa, handles quality reporting as a side responsibility alongside billing and credentialing.

Lisa spends 20+ hours per month pulling reports from athenahealth, manipulating data in Excel, and trying to identify care gaps. Each physician gets a printed list of "patients needing attention" but the lists are often outdated by the time they're distributed. The practice participates in Blue Shield's pay-for-performance program but consistently misses bonus thresholds.

### Pain Points

- **Fragmented data:** Each physician has different workflows, making consistent measurement difficult
- **No real-time visibility:** Quality data is always 30-60 days stale
- **Missed P4P bonuses:** Lost $45,000 last year by missing bonus thresholds
- **Provider variation:** Some physicians are at 80% on diabetes control, others at 55%
- **Staff burnout:** Lisa is overwhelmed with quality reporting on top of other duties
- **Growth bottleneck:** Can't add more providers without better quality infrastructure

### Why HDIM

The practice administrator heard about HDIM at a medical group management conference. The athenahealth integration meant they could connect without any IT work. The $299/month was cheaper than hiring a part-time quality coordinator ($2,500+/month).

After seeing a demo with real athenahealth data, they signed a 12-month contract.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Riverside Primary Care                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐                                              │
│  │   athenahealth   │                                              │
│  │   (Cloud EHR)    │                                              │
│  └────────┬─────────┘                                              │
│           │                                                         │
└───────────┼─────────────────────────────────────────────────────────┘
            │ OAuth 2.0 / FHIR R4
            │ Daily Sync (2 AM)
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐    ┌──────────────────┐                      │
│  │  athena FHIR     │───►│  Data Processing │                      │
│  │   Connector      │    │  & Validation    │                      │
│  └──────────────────┘    └────────┬─────────┘                      │
│                                   │                                 │
│                          ┌────────▼─────────┐                      │
│                          │   CQL Engine     │                      │
│                          │  (61 measures)   │                      │
│                          └────────┬─────────┘                      │
│                                   │                                 │
│                   ┌───────────────┼───────────────┐                │
│                   │               │               │                 │
│           ┌───────▼───────┐ ┌─────▼─────┐ ┌──────▼──────┐         │
│           │   Practice    │ │ Provider  │ │  Care Gap   │         │
│           │   Dashboard   │ │ Scorecards│ │  Worklists  │         │
│           └───────────────┘ └───────────┘ └─────────────┘         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: FHIR API (athenahealth)

| Setting | Value |
|---------|-------|
| **Authentication** | OAuth 2.0 (athenahealth IDP) |
| **Sync Frequency** | Daily at 2:00 AM PT |
| **Data Format** | FHIR R4 |
| **Direction** | Inbound only |
| **Bulk Export** | Enabled |

### FHIR Scopes Authorized

| Scope | Resource | Purpose |
|-------|----------|---------|
| patient/Patient.read | Patient | Demographics |
| patient/Condition.read | Condition | Diagnoses |
| patient/MedicationRequest.read | MedicationRequest | Prescriptions |
| patient/Observation.read | Observation | Labs, Vitals |
| patient/Immunization.read | Immunization | Vaccines |
| patient/Encounter.read | Encounter | Visits |

### Data Volume

| Resource | Daily Sync | Total Records |
|----------|------------|---------------|
| Patient | ~50 new/updated | 4,500 |
| Condition | ~120 new/updated | 12,800 |
| MedicationRequest | ~180 new/updated | 18,500 |
| Observation | ~300 new/updated | 45,000 |
| Immunization | ~40 new/updated | 8,200 |
| Encounter | ~80 new/updated | 32,000 |

---

## Sample Data Payload

### FHIR Patient Bundle (from athenahealth)

```json
{
  "resourceType": "Bundle",
  "type": "searchset",
  "total": 3,
  "entry": [
    {
      "resource": {
        "resourceType": "Patient",
        "id": "athena-12345",
        "identifier": [
          {
            "system": "urn:oid:2.16.840.1.113883.3.666.5.3",
            "value": "12345"
          }
        ],
        "name": [{"family": "Garcia", "given": ["Carlos", "Miguel"]}],
        "gender": "male",
        "birthDate": "1962-08-14",
        "address": [
          {
            "line": ["456 Palm Drive"],
            "city": "Riverside",
            "state": "CA",
            "postalCode": "92501"
          }
        ],
        "telecom": [
          {"system": "phone", "value": "951-555-0123"}
        ]
      }
    },
    {
      "resource": {
        "resourceType": "Condition",
        "id": "cond-67890",
        "clinicalStatus": {
          "coding": [{"code": "active"}]
        },
        "code": {
          "coding": [
            {
              "system": "http://hl7.org/fhir/sid/icd-10-cm",
              "code": "E11.65",
              "display": "Type 2 diabetes with hyperglycemia"
            }
          ]
        },
        "subject": {"reference": "Patient/athena-12345"},
        "onsetDateTime": "2015-03-20"
      }
    },
    {
      "resource": {
        "resourceType": "Observation",
        "id": "obs-hba1c-001",
        "status": "final",
        "code": {
          "coding": [
            {
              "system": "http://loinc.org",
              "code": "4548-4",
              "display": "Hemoglobin A1c"
            }
          ]
        },
        "subject": {"reference": "Patient/athena-12345"},
        "effectiveDateTime": "2024-10-01T10:30:00Z",
        "valueQuantity": {
          "value": 8.4,
          "unit": "%",
          "system": "http://unitsofmeasure.org"
        }
      }
    }
  ]
}
```

---

## Implementation Steps

### Day 1: Discovery (2 hours)

- [x] HDIM account creation and practice profile
- [x] Identify athenahealth practice IDs (all 3 locations)
- [x] Determine OAuth administrator (Lisa)
- [x] Review current quality reporting workflow
- [x] Define success metrics with practice administrator

### Day 2: athenahealth Authorization (1 hour)

- [x] Lisa logs into athenahealth Marketplace
- [x] Locates HDIM application listing
- [x] Reviews and approves requested scopes
- [x] Completes OAuth authorization flow
- [x] HDIM receives access tokens

### Day 3: Initial Sync (Automated)

- [x] HDIM initiates bulk FHIR export
- [x] 4,500 patients synchronized
- [x] ~120,000 clinical resources imported
- [x] Data validation completed
- [x] Initial measure calculations run

### Day 4-5: Validation (2 hours)

- [x] Review patient count matches athenahealth
- [x] Validate 20 patients across 3 providers
- [x] Confirm measure calculations are accurate
- [x] Identify and resolve data mapping issues
- [x] Adjust measure configurations if needed

### Day 6-7: Training & Go-Live (3 hours)

- [x] Train Lisa on dashboard navigation
- [x] Walk through provider scorecards
- [x] Demonstrate care gap worklists
- [x] Set up email alerts for sync failures
- [x] Schedule 30-day check-in

---

## Measures Enabled

### MIPS Quality Measures (12 Selected)

| Measure | HEDIS Code | Denominator | Baseline | Target |
|---------|------------|-------------|----------|--------|
| Diabetes: HbA1c Poor Control | CDC | 620 pts | 28% | <18% |
| Diabetes: Eye Exam | EED | 620 pts | 42% | >60% |
| Controlling High Blood Pressure | CBP | 1,450 pts | 62% | >75% |
| Breast Cancer Screening | BCS | 720 pts | 68% | >80% |
| Colorectal Cancer Screening | COL | 1,280 pts | 52% | >70% |
| Cervical Cancer Screening | CCS | 680 pts | 58% | >75% |
| Depression Screening | DSF | 4,500 pts | 45% | >80% |
| Tobacco Screening | TSC | 4,500 pts | 78% | >90% |
| Flu Vaccination | FVA | 1,850 pts | 48% | >65% |
| Statin Therapy | SPC | 380 pts | 72% | >85% |
| Asthma Medication Ratio | AMR | 220 pts | 58% | >70% |
| Depression Follow-up | DMS | 180 pts | 22% | >40% |

### Blue Shield P4P Measures

| Measure | Target | Bonus/Patient |
|---------|--------|---------------|
| Diabetes Control (HbA1c <9%) | >82% | $15 |
| BP Control (<140/90) | >75% | $12 |
| Breast Cancer Screening | >80% | $10 |
| Colorectal Cancer Screening | >70% | $10 |
| Medication Adherence | >80% | $8 |

---

## Provider Scorecards

### Sample Provider Comparison

| Measure | Dr. Chen | Dr. Patel | Dr. Kim | Dr. Lopez | Dr. Williams | Dr. Adams |
|---------|----------|-----------|---------|-----------|--------------|-----------|
| Diabetes HbA1c >9% | 18% | 22% | 35% | 28% | 24% | 31% |
| BP Control | 78% | 72% | 58% | 65% | 68% | 62% |
| Breast Cancer Screen | 82% | 75% | 68% | 72% | 78% | 65% |
| Patient Panel | 850 | 720 | 680 | 790 | 810 | 650 |

### Insights

- Dr. Chen consistently outperforms on chronic disease management
- Dr. Kim has opportunity for improvement on diabetes and BP
- Dr. Adams has smallest panel but lowest screening rates

---

## Expected Outcomes

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| Quality data extraction | 20 hrs/month | 0 (automated) | 20 hrs/month |
| Care gap identification | 8 hrs/month | 1 hr/month | 7 hrs/month |
| P4P reporting | 12 hrs/quarter | 2 hrs/quarter | 10 hrs/quarter |
| Provider scorecards | 6 hrs/month | 0 (automated) | 6 hrs/month |
| **Total** | **47 hrs/month** | **4 hrs/month** | **43 hrs/month** |

### Quality Improvement (12-month projection)

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Diabetes: HbA1c >9% | 28% | 16% | -12 points |
| BP Control | 62% | 78% | +16 points |
| Breast Cancer Screen | 68% | 82% | +14 points |
| Colorectal Screen | 52% | 72% | +20 points |
| Provider Score Variation | 20 pt spread | 8 pt spread | -12 pt spread |
| **MIPS Quality Score** | **72/100** | **92/100** | **+20 points** |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **MIPS Bonus** | $28,000 (4% of $700K Medicare) |
| **Blue Shield P4P Bonus** | $67,500 (based on measure attainment) |
| **Staff Time Savings** | $21,500 (43 hrs/mo × $41/hr × 12) |
| **Avoided Quality Coordinator** | $30,000 (didn't need to hire) |
| **Total Annual Value** | **$147,000** |

### ROI Calculation

```
Annual Value:        $147,000
Annual HDIM Cost:    $3,588 ($299 × 12)
Net Annual Benefit:  $143,412
ROI:                 41x
Payback Period:      9 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Monthly subscription (Professional tier) | $299/month |
| athenahealth integration setup | Included |
| Additional EHR connection | N/A |
| Training | Included |
| **Total Monthly** | **$299/month** |
| **Annual Cost** | **$3,588/year** |

---

## Success Metrics

| Metric | Target | Actual (Month 6) |
|--------|--------|------------------|
| Data sync uptime | >99% | 99.8% |
| Data freshness | <24 hours | 18 hours avg |
| MIPS quality score | >85/100 | 89/100 |
| Provider score variation | <10 pt spread | 8 pt spread |
| Care gaps closed/month | >100 | 127 avg |
| Staff time on quality | <5 hrs/month | 4 hrs/month |

---

## Lisa's Testimonial

> "Before HDIM, I was drowning in spreadsheets. I'd spend entire weekends pulling reports and still couldn't tell our docs who needed what. Now, every morning I get an email with yesterday's care gaps. Our MAs pull up worklists during rooming and close gaps while patients are in the exam room. We went from 52% to 72% on colon cancer screening in 6 months. The Blue Shield bonus alone covers HDIM for 18 years."
>
> — Lisa Chen, Practice Administrator

---

## Related Resources

- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [Primary Care Measure Sets](../_shared/MEASURE_SETS.md#primary-care-measures)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
