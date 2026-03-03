# Dr. Martinez Family Medicine - Solo Practice

> The simplest path to quality measurement: CSV upload for a solo physician practice.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Solo Practice |
| **Size** | 1 physician, 1 MA, 1 front desk |
| **Patient Count** | 1,200 active patients |
| **Location** | Springfield, MA (suburban) |
| **EHR System** | DrChrono |
| **Quality Programs** | MIPS |
| **IT Capabilities** | None (physician manages own IT) |
| **HDIM Tier** | Community |
| **Monthly Cost** | $49/month |

## Challenge

### Current State

Dr. Sarah Martinez has been in private practice for 12 years. She chose DrChrono because it was affordable and had a mobile app. Her MA extracts quality data manually once per quarter by clicking through patient charts, tallying up who needs screenings and whose diabetes is controlled.

Quality reporting takes her MA 15 hours per quarter. The data is always 2-3 months stale by the time they submit it. Dr. Martinez received a 4% MIPS penalty last year because her quality scores fell below the threshold.

### Pain Points

- **Time burden:** 15 hours/quarter = 60 hours/year on manual data extraction
- **Data staleness:** Quality gaps identified months after the opportunity passed
- **MIPS penalties:** Lost $8,400 last year (4% penalty on $210K Medicare billing)
- **No care gap worklists:** MA has no systematic way to identify who needs what
- **Frustration:** Dr. Martinez went into medicine to help patients, not do data entry

### Why HDIM

Dr. Martinez heard about HDIM from a colleague at a local medical society meeting. The $49/month price point was compelling—less than her monthly coffee budget. The promise of "upload a CSV, see your quality scores in 5 minutes" seemed too good to be true.

After a 15-minute demo, she signed up and had her first quality dashboard within an hour.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Dr. Martinez Family Medicine                      │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────────┐                                              │
│  │    DrChrono      │                                              │
│  │   (Cloud EHR)    │                                              │
│  └────────┬─────────┘                                              │
│           │                                                         │
│           │ Manual CSV Export                                       │
│           │ (Monthly)                                               │
│           ▼                                                         │
│  ┌──────────────────┐                                              │
│  │   Export Files   │                                              │
│  │  • patients.csv  │                                              │
│  │  • conditions.csv│                                              │
│  │  • meds.csv      │                                              │
│  │  • labs.csv      │                                              │
│  └────────┬─────────┘                                              │
│           │                                                         │
└───────────┼─────────────────────────────────────────────────────────┘
            │ HTTPS Upload
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐    ┌──────────────────┐                      │
│  │   CSV Importer   │───►│  FHIR Transform  │                      │
│  └──────────────────┘    └────────┬─────────┘                      │
│                                   │                                 │
│                          ┌────────▼─────────┐                      │
│                          │   CQL Engine     │                      │
│                          │  (61 measures)   │                      │
│                          └────────┬─────────┘                      │
│                                   │                                 │
│                          ┌────────▼─────────┐                      │
│                          │ Quality Dashboard│                      │
│                          │ • Measure scores │                      │
│                          │ • Care gap lists │                      │
│                          │ • Patient details│                      │
│                          └──────────────────┘                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: CSV Upload

| Setting | Value |
|---------|-------|
| **Authentication** | Email/password login |
| **Sync Frequency** | Monthly (manual upload) |
| **Data Format** | CSV (HDIM templates) |
| **Direction** | Inbound only |

### Data Sources

| Source | Type | Data Elements | Upload Frequency |
|--------|------|---------------|------------------|
| DrChrono | EHR | Demographics, Conditions, Meds, Labs | Monthly |

### CSV Files Uploaded

| File | Records | Fields |
|------|---------|--------|
| patients.csv | 1,200 | MRN, name, DOB, gender, phone, address |
| conditions.csv | 3,400 | MRN, ICD-10, onset date, status |
| medications.csv | 4,800 | MRN, RxNorm, drug name, start date, status |
| labs.csv | 2,100 | MRN, LOINC, value, date |

---

## Sample Data Payload

### Patient Roster Export (patients.csv)

```csv
mrn,first_name,last_name,dob,gender,phone,address_line1,city,state,zip
P001,Maria,Garcia,1968-03-15,F,555-123-4567,123 Main Street,Springfield,MA,01101
P002,Robert,Johnson,1955-07-22,M,555-234-5678,456 Oak Avenue,Springfield,MA,01103
P003,Jennifer,Smith,1972-11-08,F,555-345-6789,789 Elm Street,Springfield,MA,01104
P004,Michael,Williams,1960-04-25,M,555-456-7890,321 Pine Road,Springfield,MA,01105
P005,Sarah,Brown,1978-09-12,F,555-567-8901,654 Maple Drive,Springfield,MA,01102
```

### Conditions Export (conditions.csv)

```csv
mrn,icd10_code,icd10_description,onset_date,status
P001,E11.9,Type 2 diabetes mellitus without complications,2018-06-15,active
P001,I10,Essential (primary) hypertension,2019-02-10,active
P002,J45.909,Unspecified asthma uncomplicated,2010-01-15,active
P002,F32.9,Major depressive disorder single episode unspecified,2023-01-15,active
P003,E78.5,Hyperlipidemia unspecified,2020-05-20,active
P004,E11.65,Type 2 diabetes with hyperglycemia,2015-03-10,active
P004,I10,Essential (primary) hypertension,2015-03-10,active
P004,N18.3,Chronic kidney disease stage 3,2021-08-15,active
P005,J45.20,Mild intermittent asthma,2019-07-01,active
```

### Labs Export (labs.csv)

```csv
mrn,loinc_code,test_name,result_value,result_unit,result_date
P001,4548-4,Hemoglobin A1c,7.2,%,2024-09-15
P001,2089-1,LDL Cholesterol,118,mg/dL,2024-09-15
P002,4548-4,Hemoglobin A1c,5.4,%,2024-08-20
P004,4548-4,Hemoglobin A1c,9.1,%,2024-07-10
P004,33914-3,eGFR,42,mL/min/1.73m2,2024-07-10
```

---

## Implementation Steps

### Day 1: Discovery & Setup (1 hour)

- [x] Create HDIM account at app.healthdatainmotion.com
- [x] Complete organization profile
- [x] Download CSV templates
- [x] Review DrChrono export options
- [x] Schedule 30-minute onboarding call

### Day 1: First Data Export (30 minutes)

- [x] Export patient roster from DrChrono
- [x] Export active problem list
- [x] Export medication list
- [x] Export recent lab results (last 12 months)
- [x] Map DrChrono columns to HDIM template

### Day 1: Initial Upload (15 minutes)

- [x] Upload patients.csv
- [x] Upload conditions.csv
- [x] Upload medications.csv
- [x] Upload labs.csv
- [x] Verify upload success (1,200 patients imported)

### Day 1: Validation (15 minutes)

- [x] Review patient count matches DrChrono
- [x] Spot-check 5 patients for data accuracy
- [x] Review measure scores generated
- [x] Identify any data mapping issues

### Day 2+: Ongoing Usage

- [ ] Review quality dashboard weekly
- [ ] Export and upload fresh data monthly
- [ ] Use care gap worklists for patient outreach
- [ ] Track improvement over time

---

## Measures Enabled

### MIPS Quality Measures (6 Selected)

| Measure | HEDIS Code | Denominator | Baseline | Target | Revenue Impact |
|---------|------------|-------------|----------|--------|----------------|
| Diabetes: HbA1c Poor Control | CDC | 180 patients | 32% | <20% | $4,200 |
| Controlling High Blood Pressure | CBP | 420 patients | 58% | >70% | $3,500 |
| Breast Cancer Screening | BCS | 210 patients | 62% | >75% | $2,100 |
| Colorectal Cancer Screening | COL | 380 patients | 48% | >65% | $2,800 |
| Depression Screening | DSF | 1,200 patients | 35% | >75% | $1,800 |
| Tobacco Screening | TSC | 1,200 patients | 72% | >90% | $1,000 |

### Additional Measures Tracked

| Measure | Code | Denominator | Baseline |
|---------|------|-------------|----------|
| Cervical Cancer Screening | CCS | 280 patients | 55% |
| Flu Vaccination | FVA | 450 patients | 42% |
| Statin Therapy | SPC | 95 patients | 68% |

---

## Expected Outcomes

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| Quality data extraction | 15 hrs/quarter | 30 min/month | 13.5 hrs/quarter |
| Care gap identification | 4 hrs/month | Automated | 4 hrs/month |
| MIPS submission prep | 8 hrs/year | 2 hrs/year | 6 hrs/year |
| **Total** | **77 hrs/year** | **12 hrs/year** | **65 hrs/year** |

### Quality Improvement (12-month projection)

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Diabetes: HbA1c >9% | 32% | 18% | -14 points |
| BP Control | 58% | 72% | +14 points |
| Breast Cancer Screen | 62% | 78% | +16 points |
| Colorectal Screen | 48% | 68% | +20 points |
| Depression Screen | 35% | 85% | +50 points |
| **MIPS Quality Score** | **62/100** | **88/100** | **+26 points** |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **MIPS Penalty Avoided** | $8,400 (4% of $210K) |
| **MIPS Bonus Potential** | $6,300 (3% bonus at 88 score) |
| **Staff Time Savings** | $1,625 (65 hrs × $25/hr) |
| **Total Annual Value** | **$16,325** |

### ROI Calculation

```
Annual Value:        $16,325
Annual HDIM Cost:    $588 ($49 × 12)
Net Annual Benefit:  $15,737
ROI:                 27.8x
Payback Period:      <1 month
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Monthly subscription (Community tier) | $49/month |
| Integration setup | $0 (self-service CSV) |
| Training | Included (video + onboarding call) |
| **Total Monthly** | **$49/month** |
| **Annual Cost** | **$588/year** |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Data freshness | <30 days old | HDIM dashboard |
| Patient coverage | >95% of panel | Import validation |
| MIPS quality score | >85/100 | CMS QPP portal |
| Care gaps closed | +20% vs. baseline | HDIM trending |
| MA time on quality | <2 hrs/month | Time tracking |

---

## Dr. Martinez's Testimonial

> "I spent years dreading MIPS. My MA would spend days clicking through charts, and we'd still get penalized. With HDIM, I upload a file once a month, and I can see exactly which patients need attention. Last month, we closed 47 care gaps just by calling patients who were overdue for screenings. My MIPS score went from 62 to 88 in one year. The $49/month is the best money I spend on my practice."
>
> — Dr. Sarah Martinez, MD, FAAFP

---

## Related Resources

- [CSV Templates](../_shared/CSV_TEMPLATES.md)
- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md) (for future API upgrade)
- [Primary Care Measure Sets](../_shared/MEASURE_SETS.md#primary-care-measures)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
