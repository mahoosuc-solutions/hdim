# Mountain View CAH - Rural Critical Access Hospital

> n8n workflow integration for a Critical Access Hospital with legacy Meditech system.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Critical Access Hospital (CAH) |
| **Size** | 25 beds, 35 providers, 180 staff |
| **Patient Count** | 8,000 active outpatient, 1,200 annual admits |
| **Location** | Mountain View, MT (rural, 45 min from nearest city) |
| **EHR System** | Meditech Expanse |
| **Quality Programs** | MIPS, Medicare Hospital Quality, State Rural Health |
| **IT Capabilities** | Limited (1 IT director, shared with county) |
| **HDIM Tier** | Professional (with rural discount) |
| **Monthly Cost** | $254/month (15% rural discount applied) |

## Challenge

### Current State

Mountain View CAH is the only hospital within 45 miles. They serve a rural community of 12,000 people, many of whom are elderly ranchers and farmers. The hospital has a small emergency department, a 25-bed inpatient unit, and outpatient clinics for primary care, general surgery, and women's health.

Their IT director, Tom, manages everything from the phone system to the EHR. Meditech Expanse is relatively modern, but Tom doesn't have time to configure complex reporting. Quality reporting is done by the CNO, Martha, who manually reviews charts for CMS quality measures.

They've been penalized for the last 3 years under the Hospital Readmissions Reduction Program (HRRP) and are at risk for their Medicare cost report adjustments.

### Pain Points

- **No dedicated quality staff:** CNO does quality reporting as a side duty
- **Legacy system complexity:** Meditech is powerful but hard to configure
- **IT bandwidth:** One IT director for entire hospital
- **CMS penalties:** $85,000/year in HRRP penalties
- **Rural challenges:** Patients travel far, making follow-up difficult
- **Workforce shortage:** Hard to recruit quality professionals to rural areas
- **State reporting:** Rural health grants require quality metrics

### Why HDIM

The hospital's CFO attended a rural health conference where HDIM presented. The 15% rural discount and the promise of "no IT work required" were compelling. The n8n approach meant they could connect their Meditech system without Tom having to learn FHIR APIs.

A 30-minute demo convinced the C-suite to move forward.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Mountain View CAH                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐            │
│  │  Inpatient   │   │  Outpatient  │   │   ED         │            │
│  │   (25 beds)  │   │   Clinics    │   │              │            │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘            │
│         │                  │                  │                     │
│         └──────────────────┼──────────────────┘                     │
│                            │                                        │
│                   ┌────────▼────────┐                               │
│                   │    Meditech     │                               │
│                   │    Expanse      │                               │
│                   └────────┬────────┘                               │
│                            │                                        │
│                   ┌────────▼────────┐                               │
│                   │ Scheduled Report│                               │
│                   │    Export       │                               │
│                   │  (Daily CSV)    │                               │
│                   └────────┬────────┘                               │
│                            │                                        │
│                   ┌────────▼────────┐                               │
│                   │   SFTP Server   │                               │
│                   │  (Hospital IT)  │                               │
│                   └────────┬────────┘                               │
│                            │                                        │
└────────────────────────────┼────────────────────────────────────────┘
                             │ TLS 1.3
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    HDIM-Managed n8n Cloud                           │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    n8n Workflow                               │  │
│  │                                                               │  │
│  │  ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐          │  │
│  │  │  SFTP  │──►│ Parse  │──►│Validate│──►│Transform│         │  │
│  │  │ Fetch  │   │  CSV   │   │  Data  │   │ to FHIR │         │  │
│  │  └────────┘   └────────┘   └────────┘   └────┬────┘         │  │
│  │                                              │               │  │
│  └──────────────────────────────────────────────┼───────────────┘  │
│                                                 │                   │
└─────────────────────────────────────────────────┼───────────────────┘
                                                  │
                                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐    ┌──────────────────┐                      │
│  │  FHIR Receiver   │───►│   CQL Engine     │                      │
│  └──────────────────┘    └────────┬─────────┘                      │
│                                   │                                 │
│                   ┌───────────────┼───────────────┐                │
│                   │               │               │                 │
│           ┌───────▼───────┐ ┌─────▼─────┐ ┌──────▼──────┐         │
│           │   Hospital    │ │Readmission│ │  Outpatient │         │
│           │   Dashboard   │ │ Tracking  │ │  Care Gaps  │         │
│           └───────────────┘ └───────────┘ └─────────────┘         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: n8n Workflow (SFTP → FHIR)

| Setting | Value |
|---------|-------|
| **Source** | Hospital SFTP (sftp.mountainviewcah.org) |
| **Authentication** | SSH key-based |
| **Sync Frequency** | Daily at 4:00 AM MT |
| **Data Format** | Meditech CSV export → FHIR R4 |
| **n8n Deployment** | HDIM-Managed Cloud |

### Meditech Export Configuration

Tom configured 4 scheduled reports in Meditech:

| Report | File | Schedule | Records |
|--------|------|----------|---------|
| Patient Demographics | patients_YYYYMMDD.csv | Daily 3 AM | ~100/day |
| Active Problems | conditions_YYYYMMDD.csv | Daily 3 AM | ~200/day |
| Medications | meds_YYYYMMDD.csv | Daily 3 AM | ~300/day |
| Lab Results | labs_YYYYMMDD.csv | Daily 3 AM | ~150/day |
| Admissions/Discharges | encounters_YYYYMMDD.csv | Daily 3 AM | ~15/day |

### n8n Workflow Steps

```
1. SFTP Trigger (4:00 AM MT)
   └─► Poll sftp.mountainviewcah.org:/exports/hdim/

2. For each file type:
   └─► Download CSV
   └─► Parse CSV rows
   └─► Validate required fields
   └─► Map Meditech codes → standard codes
   └─► Transform to FHIR resources
   └─► POST to HDIM FHIR API

3. Post-processing:
   └─► Archive processed files
   └─► Log success/failure counts
   └─► Alert on errors (email to Tom)
```

---

## Sample Data Payloads

### Meditech Export (encounters_20241015.csv)

```csv
MRN,PATIENT_NAME,DOB,GENDER,ADMIT_DATE,DISCHARGE_DATE,ADMIT_TYPE,DISCHARGE_DISP,PRIMARY_DX,LOS
MV-001234,JOHNSON ROBERT,1948-05-12,M,2024-10-10,2024-10-15,EMERGENCY,HOME,I50.9,5
MV-002345,SMITH MARY,1952-08-22,F,2024-10-08,2024-10-12,ELECTIVE,SNF,M17.11,4
MV-003456,WILLIAMS JAMES,1945-11-03,M,2024-10-12,2024-10-14,EMERGENCY,HOME,J44.1,2
MV-004567,BROWN PATRICIA,1958-03-17,F,2024-10-14,2024-10-15,OBSERVATION,HOME,R07.9,1
```

### Transformed FHIR Encounter

```json
{
  "resourceType": "Encounter",
  "id": "mv-enc-001234-20241010",
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "EMER",
    "display": "emergency"
  },
  "type": [{
    "coding": [{
      "system": "http://terminology.hl7.org/CodeSystem/encounter-type",
      "code": "emergency",
      "display": "Emergency"
    }]
  }],
  "subject": {
    "reference": "Patient/MV-001234",
    "display": "Robert Johnson"
  },
  "period": {
    "start": "2024-10-10",
    "end": "2024-10-15"
  },
  "hospitalization": {
    "dischargeDisposition": {
      "coding": [{
        "system": "http://terminology.hl7.org/CodeSystem/discharge-disposition",
        "code": "home",
        "display": "Home"
      }]
    }
  },
  "diagnosis": [{
    "condition": {
      "reference": "Condition/mv-cond-001234-I509"
    },
    "use": {
      "coding": [{
        "system": "http://terminology.hl7.org/CodeSystem/diagnosis-role",
        "code": "billing"
      }]
    }
  }],
  "length": {
    "value": 5,
    "unit": "days"
  }
}
```

---

## Implementation Steps

### Week 1: Discovery (Day 1-2)

- [x] Kickoff call with CFO, CNO, IT Director
- [x] Review current quality reporting processes
- [x] Document Meditech version and capabilities
- [x] Identify SFTP server and credentials
- [x] Map required data fields

### Week 1: Meditech Export Setup (Day 3-5)

- [x] Tom creates scheduled reports in Meditech
- [x] Configure SFTP folder structure
- [x] Test export with sample data
- [x] Verify data completeness

### Week 1: n8n Workflow Development (Day 3-5)

- [x] HDIM builds custom n8n workflow
- [x] Configure SFTP connection
- [x] Develop CSV parsing logic
- [x] Create Meditech → FHIR code mappings
- [x] Test with sample files

### Week 2: Integration Testing (Day 6-7)

- [x] End-to-end test with real data
- [x] Validate patient matching
- [x] Verify encounter data accuracy
- [x] Test error handling

### Week 2: Go-Live (Day 8)

- [x] Enable production workflow
- [x] Monitor first automated sync
- [x] Train Martha on dashboards
- [x] Configure alerting

---

## Measures Enabled

### Hospital Quality Measures

| Measure | Category | Denominator | Baseline | Target |
|---------|----------|-------------|----------|--------|
| 30-Day All-Cause Readmission | Outcome | 1,200 admits | 18.2% | <15% |
| Heart Failure Readmission | Outcome | 180 admits | 22.5% | <18% |
| COPD Readmission | Outcome | 95 admits | 21.0% | <17% |
| Pneumonia Readmission | Outcome | 75 admits | 19.5% | <16% |
| Medication Reconciliation | Process | 1,200 admits | 72% | >90% |

### Outpatient Quality Measures

| Measure | HEDIS Code | Denominator | Baseline | Target |
|---------|------------|-------------|----------|--------|
| Diabetes HbA1c Control | CDC | 1,400 pts | 68% | >80% |
| Hypertension Control | CBP | 2,800 pts | 62% | >75% |
| Breast Cancer Screening | BCS | 1,200 pts | 58% | >72% |
| Colorectal Cancer Screening | COL | 2,100 pts | 42% | >60% |
| Flu Vaccination | FVA | 3,500 pts | 38% | >55% |
| Pneumonia Vaccination 65+ | PNU | 2,200 pts | 52% | >70% |

---

## Readmission Tracking Dashboard

### Real-Time Readmission Risk

```
┌─────────────────────────────────────────────────────────────────┐
│           READMISSION RISK DASHBOARD                            │
│           Mountain View CAH | Updated: 10/15/24 6:00 AM         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  CURRENT INPATIENTS: 18                                        │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ HIGH RISK (Score >70)               3 patients          │   │
│  │ ├── Johnson, Robert (CHF) - Score: 82 - Day 5          │   │
│  │ ├── Williams, James (COPD) - Score: 75 - Day 2         │   │
│  │ └── Anderson, Helen (Pneumonia) - Score: 71 - Day 3    │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ MEDIUM RISK (Score 40-70)           8 patients          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ LOW RISK (Score <40)                7 patients          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  RECENT DISCHARGES (7 days): 12                                │
│  └── Pending follow-up calls: 5                                │
│  └── Follow-up completed: 7                                    │
│                                                                 │
│  30-DAY READMISSION RATE (Rolling):                            │
│  └── Current: 15.8% (Target: <15%)                            │
│  └── Last Month: 17.2%                                         │
│  └── Last Year: 18.2%                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## Expected Outcomes

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| Quality data abstraction | 20 hrs/month | 2 hrs/month | 18 hrs/month |
| Readmission tracking | 8 hrs/month | Automated | 8 hrs/month |
| CMS quality reporting | 40 hrs/quarter | 4 hrs/quarter | 36 hrs/quarter |
| Care transition calls | Manual identification | Automated worklist | 4 hrs/month |
| **Total** | **42 hrs/month** | **8 hrs/month** | **34 hrs/month** |

### Quality Improvement (12-month projection)

| Metric | Baseline | Target | Projected Savings |
|--------|----------|--------|-------------------|
| 30-Day Readmission Rate | 18.2% | 14.5% | $52,000 |
| Heart Failure Readmit | 22.5% | 17.0% | $18,000 |
| COPD Readmit | 21.0% | 16.5% | $8,000 |
| HRRP Penalty | $85,000 | $25,000 | $60,000 |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **HRRP Penalty Reduction** | $60,000 |
| **State Rural Health Bonus** | $35,000 |
| **MIPS Bonus** | $18,000 |
| **Staff Time Savings** | $17,000 (34 hrs × $42/hr × 12) |
| **Total Annual Value** | **$130,000** |

### ROI Calculation

```
Annual Value:        $130,000
Annual HDIM Cost:    $3,048 ($254 × 12)
n8n Setup:          $1,500 (one-time)
Year 1 Total Cost:  $4,548
Net Year 1 Benefit: $125,452
ROI:                28.6x
Payback Period:     12 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Professional tier (base) | $299/month |
| Rural discount (15%) | -$45/month |
| **Monthly after discount** | **$254/month** |
| n8n workflow development | $1,500 one-time |
| Meditech export configuration | Customer responsibility |
| Training | Included |
| **Year 1 Total** | **$4,548** |
| **Year 2+ Annual** | **$3,048** |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Data sync success rate | >99% | n8n workflow logs |
| Data freshness | <24 hours | HDIM dashboard |
| 30-day readmission rate | <15% | CMS comparison |
| Outpatient quality scores | >75th percentile | MIPS feedback |
| CNO time on quality | <10 hrs/month | Time tracking |

---

## CNO Testimonial

> "I used to spend my weekends doing chart reviews. Now I check the dashboard over my morning coffee. Last month, we caught 3 high-risk heart failure patients before discharge and set up home health visits. None of them bounced back. Our readmission rate dropped from 18% to 15% in 8 months. The state rural health program just gave us a $35,000 bonus for quality improvement. Tom spent maybe 2 hours setting up the exports—that's the only IT work we did."
>
> — Martha Henderson, RN, MSN, CNO

---

## Related Resources

- [n8n Workflows](../_shared/N8N_WORKFLOWS.md)
- [CSV Templates](../_shared/CSV_TEMPLATES.md)
- [Hospital Measure Sets](../_shared/MEASURE_SETS.md#hospital-measures)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
