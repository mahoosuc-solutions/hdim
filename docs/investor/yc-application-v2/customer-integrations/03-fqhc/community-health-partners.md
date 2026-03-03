# Community Health Partners - FQHC

> Multi-site Federally Qualified Health Center with FHIR + n8n hybrid integration for complex data sources.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Federally Qualified Health Center (FQHC) |
| **Size** | 5 sites, 18 providers, 45 staff |
| **Patient Count** | 22,000 active patients |
| **Location** | Central Valley, CA (rural/suburban mix) |
| **EHR System** | NextGen (primary), Legacy lab system (Quest interface) |
| **Quality Programs** | UDS, MIPS, Medi-Cal Managed Care P4P |
| **IT Capabilities** | Moderate (1 IT coordinator, managed services) |
| **HDIM Tier** | Enterprise (with FQHC discount) |
| **Monthly Cost** | $799/month (20% FQHC discount applied) |

## Challenge

### Current State

Community Health Partners serves a predominantly low-income, agricultural community. 65% of patients are on Medicaid, 20% are uninsured, and 15% are on Medicare. The patient population is 70% Hispanic/Latino with significant language barriers.

Their quality team (2 FTEs) spends enormous effort on UDS reporting. NextGen generates some reports, but they don't align with UDS specifications. Lab data from Quest comes via a separate interface and doesn't always match patient records. Different sites have different workflows, leading to massive variation in quality scores.

HRSA site visits have noted their quality scores are below the 50th percentile on most measures. They're at risk of losing FQHC designation if they can't demonstrate improvement.

### Pain Points

- **UDS complexity:** 2 FTEs spend 40% of time on UDS reporting
- **Data fragmentation:** NextGen + Quest + paper records don't integrate
- **Site variation:** Scores range from 45% to 75% on the same measure
- **HRSA pressure:** Below 50th percentile on most measures
- **Staff turnover:** High turnover means constant retraining
- **Social determinants:** Patients face barriers that standard measures don't capture
- **Language barriers:** 40% of patients prefer Spanish

### Why HDIM

Community Health Partners' CEO attended an FQHC conference where HDIM presented. The FQHC-specific features (UDS alignment, site-level comparison, SDOH tracking) and the 20% FQHC discount made it attractive. The ability to integrate their legacy Quest lab data via n8n was the deciding factor—no other vendor could handle their fragmented systems.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Community Health Partners                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐            │
│  │   Site 1     │   │   Site 2     │   │   Sites 3-5  │            │
│  │   (Main)     │   │   (Satellite)│   │  (Satellites)│            │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘            │
│         │                  │                  │                     │
│         └──────────────────┼──────────────────┘                     │
│                            │                                        │
│                   ┌────────▼────────┐                               │
│                   │     NextGen     │                               │
│                   │   (Cloud EHR)   │                               │
│                   └────────┬────────┘                               │
│                            │                                        │
│  ┌─────────────────────────┼─────────────────────────┐             │
│  │                         │                         │              │
│  │                  ┌──────▼──────┐                 │              │
│  │                  │  FHIR API   │                 │              │
│  │                  └──────┬──────┘                 │              │
│  │                         │                         │              │
│  │  ┌──────────────┐      │                         │              │
│  │  │   Quest Lab  │      │                         │              │
│  │  │  Interface   │      │                         │              │
│  │  └──────┬───────┘      │                         │              │
│  │         │               │                         │              │
│  │  ┌──────▼───────┐      │                         │              │
│  │  │  SFTP Drop   │      │                         │              │
│  │  │  (Daily CSV) │      │                         │              │
│  │  └──────┬───────┘      │                         │              │
│  │         │               │                         │              │
└──┼─────────┼───────────────┼─────────────────────────┼──────────────┘
   │         │               │                         │
   │         │               │                         │
   │    ┌────▼───────────────▼─────┐                  │
   │    │    n8n Workflow Hub      │                  │
   │    │  (HDIM-Managed Cloud)    │                  │
   │    │  ┌─────────────────────┐ │                  │
   │    │  │ Lab CSV → FHIR      │ │                  │
   │    │  └─────────────────────┘ │                  │
   │    └────────────┬─────────────┘                  │
   │                 │                                 │
   └─────────────────┼─────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐        │
│  │ NextGen FHIR   │  │ n8n Lab Data   │  │ Data Aggregator│        │
│  │   Connector    │  │   Receiver     │  │ & Deduplicator │        │
│  └───────┬────────┘  └───────┬────────┘  └───────┬────────┘        │
│          │                   │                   │                  │
│          └───────────────────┼───────────────────┘                  │
│                              │                                      │
│                     ┌────────▼────────┐                            │
│                     │   CQL Engine    │                            │
│                     │ (UDS-aligned)   │                            │
│                     └────────┬────────┘                            │
│                              │                                      │
│          ┌───────────────────┼───────────────────┐                 │
│          │                   │                   │                  │
│   ┌──────▼──────┐    ┌───────▼───────┐   ┌──────▼──────┐          │
│   │ UDS Report  │    │ Site Compare  │   │ Care Gap    │          │
│   │  Generator  │    │  Dashboard    │   │ Worklists   │          │
│   └─────────────┘    └───────────────┘   └─────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: Hybrid (FHIR + n8n)

| Component | Method | Frequency |
|-----------|--------|-----------|
| NextGen Clinical | FHIR R4 API | Daily at 3 AM |
| Quest Lab Results | n8n (SFTP → FHIR) | Every 4 hours |

### NextGen FHIR Configuration

| Setting | Value |
|---------|-------|
| **Authentication** | OAuth 2.0 (NextGen IDP) |
| **Sync Frequency** | Daily bulk export |
| **Data Format** | FHIR R4 |
| **Population Filter** | Active patients, all 5 sites |

### n8n Lab Workflow Configuration

| Setting | Value |
|---------|-------|
| **Source** | Quest SFTP (sftp.questdiagnostics.com) |
| **File Pattern** | CHP_LABS_*.csv |
| **Poll Interval** | Every 4 hours |
| **Transform** | CSV → FHIR Observation |
| **Destination** | HDIM FHIR API |

---

## Sample Data Payloads

### Quest Lab CSV (Input to n8n)

```csv
PATIENT_ID,FIRST_NAME,LAST_NAME,DOB,TEST_CODE,TEST_NAME,RESULT,UNIT,REF_LOW,REF_HIGH,RESULT_DATE,STATUS
CHP-12345,Maria,Rodriguez,1975-06-20,83036,HEMOGLOBIN A1C,8.2,%,4.0,5.6,2024-10-15,F
CHP-12345,Maria,Rodriguez,1975-06-20,2089-1,LDL CHOLESTEROL,142,mg/dL,0,100,2024-10-15,F
CHP-67890,Juan,Hernandez,1968-03-08,83036,HEMOGLOBIN A1C,7.1,%,4.0,5.6,2024-10-14,F
CHP-11111,Rosa,Garcia,1982-11-25,36916-5,HIV SCREEN,Negative,,,,2024-10-13,F
```

### n8n Transform Output (FHIR Observation)

```json
{
  "resourceType": "Observation",
  "id": "quest-lab-83036-12345-20241015",
  "status": "final",
  "category": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/observation-category",
      "code": "laboratory"
    }]
  }],
  "code": {
    "coding": [
      {
        "system": "http://loinc.org",
        "code": "4548-4",
        "display": "Hemoglobin A1c"
      }
    ]
  },
  "subject": {
    "reference": "Patient/CHP-12345",
    "display": "Maria Rodriguez"
  },
  "effectiveDateTime": "2024-10-15",
  "valueQuantity": {
    "value": 8.2,
    "unit": "%",
    "system": "http://unitsofmeasure.org",
    "code": "%"
  },
  "referenceRange": [{
    "low": {"value": 4.0, "unit": "%"},
    "high": {"value": 5.6, "unit": "%"}
  }],
  "interpretation": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation",
      "code": "H",
      "display": "High"
    }]
  }]
}
```

---

## Implementation Steps

### Week 1: Discovery & Planning

- [x] HDIM kickoff call with CEO, CMO, IT, Quality
- [x] Document all data sources (NextGen, Quest, paper)
- [x] Map site IDs and provider NPIs
- [x] Identify UDS reporting requirements
- [x] Review current UDS scores and HRSA feedback
- [x] Define success metrics

### Week 1: NextGen FHIR Setup

- [x] Register HDIM in NextGen Partner Portal
- [x] Configure FHIR API access
- [x] Complete OAuth authorization
- [x] Test connectivity with sandbox
- [x] Configure production access

### Week 1-2: n8n Lab Workflow

- [x] Provision HDIM-managed n8n instance
- [x] Configure Quest SFTP credentials
- [x] Build CSV parsing workflow
- [x] Develop LOINC code mapping
- [x] Create FHIR transformation logic
- [x] Test with sample lab files
- [x] Deploy to production

### Week 2: Initial Data Load

- [x] Bulk export from NextGen (22,000 patients)
- [x] Historical lab data import (18 months)
- [x] Data validation and deduplication
- [x] Initial measure calculations
- [x] Site-level breakdown verification

### Week 2: Validation & Training

- [x] Validate patient counts by site
- [x] Compare measures to last UDS submission
- [x] Train quality team on dashboards
- [x] Configure site-level access controls
- [x] Set up automated UDS extract

---

## Measures Enabled

### UDS Clinical Quality Measures

| Measure | UDS Table | Denominator | Baseline | Target | Percentile Goal |
|---------|-----------|-------------|----------|--------|-----------------|
| Hypertension Control | 6B | 5,800 pts | 58% | >70% | 75th |
| Diabetes HbA1c <9% | 6B | 3,200 pts | 62% | >75% | 75th |
| Depression Screening | 6B | 22,000 pts | 68% | >85% | 90th |
| Tobacco Screening | 6B | 22,000 pts | 75% | >90% | 75th |
| Colorectal Cancer Screening | 6B | 4,800 pts | 38% | >55% | 75th |
| Cervical Cancer Screening | 6B | 5,200 pts | 48% | >65% | 75th |
| HIV Screening | 6B | 14,000 pts | 55% | >75% | 90th |
| BMI Screening Adult | 6B | 18,000 pts | 65% | >80% | 75th |
| Childhood Immunization | 6B | 1,800 pts | 42% | >55% | 75th |
| Weight Assessment Child | 6B | 4,200 pts | 58% | >75% | 75th |

### Site-Level Comparison

| Measure | Site 1 | Site 2 | Site 3 | Site 4 | Site 5 | Gap |
|---------|--------|--------|--------|--------|--------|-----|
| Hypertension | 68% | 62% | 52% | 55% | 58% | 16 pts |
| Diabetes | 72% | 65% | 55% | 58% | 60% | 17 pts |
| Depression Screen | 82% | 75% | 58% | 62% | 68% | 24 pts |
| Colorectal | 52% | 42% | 28% | 32% | 38% | 24 pts |

---

## Expected Outcomes

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| UDS data preparation | 320 hrs/year | 40 hrs/year | 280 hrs/year |
| Monthly quality reports | 20 hrs/month | 2 hrs/month | 18 hrs/month |
| Site-level analysis | 12 hrs/month | 1 hr/month | 11 hrs/month |
| Care gap identification | 24 hrs/month | Automated | 24 hrs/month |
| **Total** | **636 hrs/year** | **76 hrs/year** | **560 hrs/year** |

### Quality Improvement (12-month projection)

| Metric | Baseline | 12-Month Target | Improvement |
|--------|----------|-----------------|-------------|
| Hypertension Control | 58% | 72% | +14 pts |
| Diabetes Control | 62% | 76% | +14 pts |
| Colorectal Screening | 38% | 58% | +20 pts |
| Site Score Variation | 24 pt spread | 8 pt spread | -16 pts |
| UDS Percentile Rank | 42nd | 72nd | +30 percentiles |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **330 Grant Continuation** | Preserved ($2.1M grant at risk) |
| **Medi-Cal P4P Bonus** | $185,000 (new, wasn't qualifying) |
| **MIPS Bonus** | $42,000 (4% of $1.05M Medicare) |
| **Quality Staff Efficiency** | $28,000 (560 hrs × $50/hr) |
| **Avoided HRSA Corrective Action** | Priceless (designation preserved) |
| **Total Quantifiable Value** | **$255,000+/year** |

### ROI Calculation

```
Annual Value:        $255,000+
Annual HDIM Cost:    $9,588 ($799 × 12)
Net Annual Benefit:  $245,412+
ROI:                 26.6x
Payback Period:      14 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Enterprise tier (base) | $999/month |
| FQHC discount (20%) | -$200/month |
| **Monthly after discount** | **$799/month** |
| n8n Lab workflow setup | $2,500 one-time |
| Training (2 sessions) | Included |
| **Year 1 Total** | **$12,088** |
| **Year 2+ Annual** | **$9,588** |

---

## UDS Reporting Integration

### Automated UDS Extract

HDIM generates UDS-ready data extracts:

```
UDS Table 6B Export
Generated: 2024-12-15
Reporting Period: 2024

Measure                          | Denominator | Numerator | Rate
---------------------------------|-------------|-----------|------
Hypertension (BP <140/90)        | 5,842       | 4,090     | 70.0%
Diabetes Poor Control (A1c >9%)  | 3,218       | 772       | 24.0%
Depression Screening             | 21,856      | 18,578    | 85.0%
Tobacco Screening                | 21,856      | 19,671    | 90.0%
Colorectal Cancer Screening      | 4,812       | 2,694     | 56.0%
Cervical Cancer Screening        | 5,198       | 3,379     | 65.0%
HIV Screening                    | 14,023      | 10,517    | 75.0%
BMI Screening Adult              | 17,892      | 14,314    | 80.0%
Childhood Immunization (Combo 10)| 1,823       | 1,003     | 55.0%
Weight Assessment Child          | 4,186       | 3,140     | 75.0%
```

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Data sync uptime | >99% | HDIM monitoring |
| Lab data latency | <6 hours | n8n workflow logs |
| UDS percentile rank | >70th | HRSA comparison |
| Site score variation | <10 pt spread | HDIM site comparison |
| HRSA site visit rating | No findings | HRSA review |

---

## CEO Testimonial

> "HRSA told us we were at risk. Our quality scores were in the bottom half on nearly every measure. We tried hiring more quality staff, but they couldn't keep up with the data complexity. HDIM changed everything. Within 6 months, we went from 42nd to 72nd percentile. Our last HRSA visit was the best we've ever had. The board was ready to spend $200K on a new EHR—HDIM cost us $10K and actually solved the problem."
>
> — Roberto Sanchez, CEO, Community Health Partners

---

## Related Resources

- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [n8n Workflows](../_shared/N8N_WORKFLOWS.md)
- [FQHC Measure Sets](../_shared/MEASURE_SETS.md#fqhc--safety-net-measures)
- [CSV Templates](../_shared/CSV_TEMPLATES.md)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
