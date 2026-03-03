# Quality Measure Sets by Customer Segment

> Recommended HEDIS and quality measures organized by customer type, with expected baselines and improvement targets.

## Table of Contents

1. [Measure Selection Guide](#measure-selection-guide)
2. [Primary Care Measures](#primary-care-measures)
3. [FQHC / Safety Net Measures](#fqhc--safety-net-measures)
4. [Hospital Measures](#hospital-measures)
5. [ACO Measures](#aco-measures)
6. [Specialty-Specific Measures](#specialty-specific-measures)
7. [Pediatric Measures](#pediatric-measures)
8. [Behavioral Health Measures](#behavioral-health-measures)
9. [Measure Benchmarks](#measure-benchmarks)

---

## Measure Selection Guide

### By Organization Type

| Organization | Primary Focus | Measure Count | Key Categories |
|--------------|---------------|---------------|----------------|
| Solo Practice | MIPS survival | 6-10 | Preventive, Chronic |
| Small Practice | MIPS optimization | 10-15 | Preventive, Chronic |
| FQHC | UDS reporting | 15-20 | Access, Preventive, Chronic |
| Rural Hospital | HCAHPS + Quality | 12-18 | Chronic, Safety, Patient Exp |
| ACO | Shared savings | 20-30 | Population health |
| Health System | All of the above | 40+ | Comprehensive |
| IPA | Network performance | 15-25 | Provider comparison |

### By Quality Program

| Program | Measures | Reporting Period | Deadline |
|---------|----------|------------------|----------|
| MIPS | 6 required | Calendar year | March 31 |
| ACO REACH | 10 ACO measures | Calendar year | March 31 |
| UDS | 15+ clinical | Calendar year | February 15 |
| Medicare Stars | 40+ | Rolling | Continuous |
| HEDIS | 90+ available | Calendar year | June 15 |

---

## Primary Care Measures

### Essential 6 (MIPS Minimum)

| Measure | HEDIS Code | Category | Weight |
|---------|------------|----------|--------|
| Diabetes: HbA1c Control | CDC | Chronic | High |
| Hypertension: BP Control | CBP | Chronic | High |
| Breast Cancer Screening | BCS | Preventive | Medium |
| Colorectal Cancer Screening | COL | Preventive | Medium |
| Cervical Cancer Screening | CCS | Preventive | Medium |
| Depression Screening | DSF | Behavioral | Medium |

### Expanded Primary Care Set (15 measures)

#### Chronic Disease Management

| Measure | Code | Description | Target |
|---------|------|-------------|--------|
| Diabetes: HbA1c Poor Control | CDC | HbA1c >9% (lower is better) | <15% |
| Diabetes: Eye Exam | EED | Annual retinal exam | >65% |
| Diabetes: Nephropathy | NCS | Annual nephropathy screening | >85% |
| Blood Pressure Control | CBP | BP <140/90 | >70% |
| Statin Therapy | SPC | Statin adherence ASCVD | >80% |
| Asthma Medication Ratio | AMR | Controller:rescue ratio | >70% |

#### Preventive Care

| Measure | Code | Description | Target |
|---------|------|-------------|--------|
| Breast Cancer Screening | BCS | Mammogram 50-74 | >75% |
| Colorectal Cancer Screening | COL | FIT/colonoscopy 45-75 | >70% |
| Cervical Cancer Screening | CCS | Pap/HPV 21-64 | >70% |
| Flu Vaccine | FVA | Annual influenza | >55% |
| Pneumonia Vaccine | PNU | Pneumococcal 65+ | >60% |

#### Behavioral Health

| Measure | Code | Description | Target |
|---------|------|-------------|--------|
| Depression Screening | DSF | Annual screening | >75% |
| Depression Follow-up | DMS | Follow-up at 12 weeks | >35% |
| Tobacco Cessation | TSC | Screening and counseling | >80% |

---

## FQHC / Safety Net Measures

### UDS Core Measures

| Measure | UDS Table | Description | National Avg |
|---------|-----------|-------------|--------------|
| Hypertension Control | 6B | BP <140/90 | 62% |
| Diabetes Control | 6B | HbA1c <9% | 65% |
| Depression Screening | 6B | Screened for depression | 78% |
| Tobacco Screening | 6B | Screening + intervention | 85% |
| BMI Screening Adult | 6B | BMI documented + counseling | 72% |
| Colorectal Cancer Screen | 6B | Age-appropriate screening | 48% |
| Cervical Cancer Screen | 6B | Pap test 21-64 | 53% |
| HIV Screening | 6B | At least once 15-65 | 62% |
| Weight Assessment Child | 6B | BMI percentile + counseling | 75% |
| Childhood Immunization | 6B | Combo 10 by age 2 | 48% |

### FQHC-Specific Considerations

| Factor | Impact | HDIM Support |
|--------|--------|--------------|
| Sliding fee patients | Often sicker, lower compliance | Risk stratification |
| Grant requirements | Must show improvement | Trend reporting |
| Multi-site variation | Performance varies by site | Site-level dashboards |
| UDS deadlines | February 15 annually | Automated UDS extract |
| HRSA expectations | Top quartile goal | Benchmark comparison |

---

## Hospital Measures

### Critical Access Hospital (CAH) Set

| Measure | Category | Description |
|---------|----------|-------------|
| Readmission Rate | Outcome | 30-day all-cause |
| Medication Reconciliation | Safety | Post-discharge |
| Falls Prevention | Safety | Fall risk assessment |
| Flu Vaccination | Preventive | Inpatient flu shot |
| Pneumonia Vaccination | Preventive | Inpatient pneumonia shot |
| VTE Prophylaxis | Safety | DVT prevention |

### Rural Hospital Considerations

| Challenge | HDIM Solution |
|-----------|---------------|
| Limited IT staff | Managed integration |
| Legacy systems | n8n workflows |
| Small volumes | Reliable denominators |
| Staff turnover | Simple training |

---

## ACO Measures

### ACO REACH 2024 Measure Set

#### Population Health (8 measures)

| Measure | Domain | Weight |
|---------|--------|--------|
| Risk-Standardized All-Cause Readmissions | Utilization | 4x |
| All-Cause Unplanned Admissions | Utilization | 4x |
| Diabetes: HbA1c Poor Control | Chronic | 3x |
| Hypertension: Controlling High BP | Chronic | 3x |
| Depression: Screening & Follow-up | Behavioral | 2x |
| Colorectal Cancer Screening | Preventive | 2x |
| Breast Cancer Screening | Preventive | 2x |
| Statin Use in ASCVD | Medication | 2x |

#### Patient Experience (CAHPS)

| Measure | Domain |
|---------|--------|
| Getting Timely Care | Access |
| Provider Communication | Relationship |
| Care Coordination | Integration |
| Rating of Provider | Overall |

### ACO Financial Impact

| Quality Score | Shared Savings | Risk |
|---------------|----------------|------|
| >90th percentile | 75% of savings | Downside protected |
| 75-90th percentile | 60% of savings | Moderate risk |
| 50-75th percentile | 50% of savings | Full risk |
| <50th percentile | 40% of savings | Enhanced risk |

---

## Specialty-Specific Measures

### Cardiology

| Measure | Code | Description |
|---------|------|-------------|
| Beta-Blocker Post-MI | PBH | Beta-blocker after MI |
| Statin Therapy ASCVD | SPC | Statin in cardiovascular disease |
| ICD Stroke Prevention | AFIB | Anticoagulation in A-fib |
| Cardiac Rehab | CRH | Referral post-event |

### Endocrinology

| Measure | Code | Description |
|---------|------|-------------|
| Diabetes: HbA1c Testing | CDC | Annual testing |
| Diabetes: HbA1c Control | CDC | HbA1c <8% or <9% |
| Diabetes: Eye Exam | EED | Dilated exam |
| Diabetes: Foot Exam | DFE | Annual foot exam |
| Diabetes: Nephropathy | NCS | Kidney screening |

### Pulmonology

| Measure | Code | Description |
|---------|------|-------------|
| Asthma Medication Ratio | AMR | Controller ratio |
| COPD: Spirometry | SPR | Diagnosis confirmation |
| COPD: Bronchodilator | COPD-BRO | Appropriate treatment |
| Tobacco Cessation | TSC | Counseling |

---

## Pediatric Measures

### Well-Child Care

| Measure | Code | Ages | Description |
|---------|------|------|-------------|
| Well-Child 0-15 months | W15 | 0-15m | 6+ visits |
| Well-Child 15-30 months | W34 | 15-30m | 2+ visits |
| Child & Adolescent Well | AWC | 3-21y | Annual visit |
| Immunization Combo 10 | CIS | 2y | All vaccines |
| Lead Screening | LSC | 1-2y | Blood lead test |
| Developmental Screening | DEV | 1-3y | Standardized tool |

### Adolescent Health

| Measure | Code | Ages | Description |
|---------|------|------|-------------|
| Adolescent Immunizations | IMA | 13y | Tdap, MenACWY, HPV |
| Chlamydia Screening | CHL | 16-24y F | Annual screening |
| Depression Screening | DSF | 12-17y | Annual screening |
| Weight Assessment | WCC | 3-17y | BMI + counseling |

---

## Behavioral Health Measures

### Depression

| Measure | Code | Description | Target |
|---------|------|-------------|--------|
| Depression Screening | DSF | PHQ-2/PHQ-9 annual | >75% |
| Depression Remission 12mo | DRR | PHQ-9 <5 at 12 months | >25% |
| Antidepressant Med Mgmt | AMM | Acute + continuation phase | >60% |
| Follow-up After New Dx | DMS | 12-week follow-up | >35% |

### Substance Use

| Measure | Code | Description | Target |
|---------|------|-------------|--------|
| Unhealthy Alcohol Use | ASC | Screening + intervention | >70% |
| Opioid Use from Rx | OHD | High-dose monitoring | <5% |
| MAT for OUD | OUD-MAT | Medication-assisted treatment | >50% |

### ADHD

| Measure | Code | Description | Target |
|---------|------|-------------|--------|
| ADHD: Initiation Phase | ADD-I | Visit 30 days of Rx | >50% |
| ADHD: Continuation | ADD-C | 2+ visits in 10 months | >60% |

---

## Measure Benchmarks

### National Percentiles (2024)

| Measure | 25th %ile | 50th %ile | 75th %ile | 90th %ile |
|---------|-----------|-----------|-----------|-----------|
| Diabetes HbA1c >9% | 35% | 28% | 22% | 15% |
| Hypertension Control | 58% | 65% | 72% | 78% |
| Breast Cancer Screen | 68% | 75% | 82% | 88% |
| Colorectal Screen | 55% | 65% | 73% | 80% |
| Depression Screening | 60% | 72% | 82% | 90% |

### Improvement Expectations by Starting Point

| Starting Score | Realistic 1-Year Improvement |
|----------------|------------------------------|
| <25th percentile | +8-12 points |
| 25-50th percentile | +5-8 points |
| 50-75th percentile | +3-5 points |
| >75th percentile | +1-3 points |

### HDIM Impact on Measure Performance

| Measure | Typical Baseline | With HDIM (12 mo) | Improvement |
|---------|------------------|-------------------|-------------|
| Diabetes Control | 65% | 75% | +10% |
| BP Control | 60% | 72% | +12% |
| Cancer Screening | 55% | 68% | +13% |
| Depression Screen | 50% | 78% | +28% |
| Immunizations | 45% | 62% | +17% |

---

## Quick Reference by Customer Type

### Solo Practice (Dr. Martinez)

**Focus:** MIPS survival, avoid penalty
**Measures:** 6 (minimum required)
**Priority:** Diabetes, HTN, breast/colon cancer screening

### Small Practice (Riverside)

**Focus:** MIPS optimization, bonus capture
**Measures:** 10-12
**Priority:** Full chronic disease + preventive

### FQHC (Community Health Partners)

**Focus:** UDS excellence, HRSA recognition
**Measures:** 15-20
**Priority:** UDS clinical, health disparities

### Rural Hospital (Mountain View CAH)

**Focus:** CMS quality reporting, value-based payment
**Measures:** 12-15
**Priority:** Readmissions, chronic disease, safety

### ACO (Coastal Care / Metro Health)

**Focus:** Shared savings maximization
**Measures:** 20-30
**Priority:** ACO REACH measure set

### Health System (Regional Medical)

**Focus:** Comprehensive quality program
**Measures:** 40+
**Priority:** All domains, system-wide performance

### IPA (Valley Physicians)

**Focus:** Network performance, practice comparison
**Measures:** 15-25
**Priority:** Variation reduction, outlier intervention

---

*Measure Sets Version: 1.0*
*Based on: HEDIS MY 2024, CMS 2024*
*Last Updated: December 2025*
