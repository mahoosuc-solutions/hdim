# Patient Health Overview System - Complete Implementation Guide

**Date**: 2025-11-20
**Status**: ✅ **READY FOR INTEGRATION**
**Purpose**: Comprehensive patient health assessment including physical, mental, and social determinants

---

## 🎯 Executive Summary

The Patient Health Overview system provides providers with a **holistic, 360-degree view** of patient health status, integrating:

- **Physical Health**: Vitals, labs, chronic conditions, medication adherence, functional status
- **Mental Health**: Depression/anxiety screenings (PHQ-9, GAD-7), diagnoses, substance use, suicide risk
- **Social Determinants of Health (SDOH)**: Food insecurity, housing, transportation, social isolation
- **Risk Stratification**: Predictive analytics for hospitalization, ED visits, readmissions
- **Care Gaps & Recommendations**: Evidence-based interventions and quality measure gaps

### Key Benefits for Providers

1. **Single View of Patient Health** - No more switching between multiple screens
2. **Mental Health Integration** - Validated screening tools (PHQ-9, GAD-7) with automated scoring
3. **Proactive Care** - Predictive analytics identify high-risk patients before crisis
4. **Actionable Insights** - Care gaps with specific recommended actions
5. **Whole-Person Care** - SDOH integration addresses root causes of health issues

---

## 📊 Overall Health Score

### Scoring Algorithm

The system calculates a **0-100 health score** with weighted components:

```typescript
Overall Health Score =
  Physical Health (40%) +
  Mental Health (30%) +
  Social Health (15%) +
  Preventive Care (15%)
```

### Health Status Categories

| Score | Status | Color | Icon |
|-------|--------|-------|------|
| 85-100 | Excellent | Green | sentiment_very_satisfied |
| 70-84 | Good | Light Green | sentiment_satisfied |
| 50-69 | Fair | Orange | sentiment_neutral |
| 0-49 | Poor | Red | sentiment_dissatisfied |

### Trend Analysis

- **Improving** ↗️ - Score increased >5 points in last 90 days
- **Stable** → - Score changed <5 points
- **Declining** ↘️ - Score decreased >5 points

---

## 🏥 Physical Health Assessment

### Vital Signs Monitoring

**Tracked Metrics**:
- Blood Pressure (mmHg)
- Heart Rate (bpm)
- Weight (lbs/kg)
- Height (in/cm)
- BMI (kg/m²)
- Temperature (°F/°C)
- Oxygen Saturation (%)

**Status Classification**:
- ✅ **Normal** - Within reference range
- ⚠️ **Abnormal** - Outside reference range, monitor
- 🚨 **Critical** - Immediate attention required

**Trending**: Each vital shows trend (improving/stable/worsening) based on last 3 measurements

### Lab Results

**Common Labs Tracked**:
- HbA1c (diabetes control)
- LDL Cholesterol
- HDL Cholesterol
- Triglycerides
- eGFR (kidney function)
- Creatinine
- TSH (thyroid)
- Vitamin D

**Reference Ranges**: Automatically flagged as normal/abnormal/critical

### Chronic Conditions Management

**Tracked Attributes**:
- Condition name and ICD-10 code
- Severity (mild/moderate/severe)
- Control status (controlled/not controlled)
- Onset date
- Last review date
- Complications

**Common Conditions**:
- Diabetes (Type 1, Type 2, Gestational)
- Hypertension
- Asthma/COPD
- Heart Failure
- Chronic Kidney Disease
- Arthritis
- Atrial Fibrillation

### Medication Adherence

**Metrics**:
- Overall adherence rate (0-100%)
- Status: Excellent (>80%), Good (60-80%), Poor (<60%)
- Problematic medications list

**Impact on Health Score**:
- Excellent adherence: No deduction
- Good adherence: -5 points
- Poor adherence: -15 points

### Functional Status

**Activities of Daily Living (ADL)** - 0-6 scale:
- Bathing
- Dressing
- Toileting
- Transferring
- Continence
- Feeding

**Instrumental ADL (IADL)** - 0-8 scale:
- Using telephone
- Shopping
- Food preparation
- Housekeeping
- Laundry
- Transportation
- Medication management
- Financial management

**Additional Metrics**:
- Mobility Score (0-100%)
- Pain Level (0-10)
- Fatigue Level (0-10)

---

## 🧠 Mental Health Assessment

### Validated Screening Tools

#### PHQ-9 (Patient Health Questionnaire-9)
**Purpose**: Depression screening
**Score Range**: 0-27
**Interpretation**:
- 0-4: Minimal depression
- 5-9: Mild depression
- 10-14: Moderate depression ⚠️ (Positive screen)
- 15-19: Moderately severe depression 🚨
- 20-27: Severe depression 🚨

**Questions** (each scored 0-3):
1. Little interest or pleasure in doing things
2. Feeling down, depressed, or hopeless
3. Trouble falling/staying asleep, or sleeping too much
4. Feeling tired or having little energy
5. Poor appetite or overeating
6. Feeling bad about yourself
7. Trouble concentrating
8. Moving/speaking slowly or fidgety
9. Thoughts of self-harm

**Threshold**: Score ≥10 indicates positive screen, requires follow-up

#### GAD-7 (Generalized Anxiety Disorder-7)
**Purpose**: Anxiety screening
**Score Range**: 0-21
**Interpretation**:
- 0-4: Minimal anxiety
- 5-9: Mild anxiety
- 10-14: Moderate anxiety ⚠️ (Positive screen)
- 15-21: Severe anxiety 🚨

**Questions** (each scored 0-3):
1. Feeling nervous, anxious, or on edge
2. Not being able to stop or control worrying
3. Worrying too much about different things
4. Trouble relaxing
5. Being so restless it's hard to sit still
6. Becoming easily annoyed or irritable
7. Feeling afraid something awful might happen

**Threshold**: Score ≥10 indicates positive screen

#### PHQ-2 (Brief Depression Screening)
**Purpose**: Quick depression screen
**Score Range**: 0-6
**Interpretation**:
- 0-2: Negative screen
- ≥3: Positive screen → Recommend full PHQ-9

**Questions** (each scored 0-3):
1. Little interest or pleasure in doing things
2. Feeling down, depressed, or hopeless

**Use Case**: Annual screening or quick assessment

### Other Screening Tools (Future Implementation)

- **PSC-17**: Pediatric Symptom Checklist
- **AUDIT-C**: Alcohol Use Disorders Identification Test
- **DAST-10**: Drug Abuse Screening Test
- **PCL-5**: PTSD Checklist
- **MDQ**: Mood Disorder Questionnaire (Bipolar)
- **CAGE-AID**: Substance abuse screening

### Mental Health Diagnoses

**Categories**:
- **Mood Disorders**: Major Depression, Bipolar, Dysthymia
- **Anxiety Disorders**: GAD, Panic, Social Anxiety, OCD
- **Psychotic Disorders**: Schizophrenia, Schizoaffective
- **Substance Use Disorders**: Alcohol, Opioid, Stimulant
- **Trauma-Related**: PTSD, Acute Stress
- **Other**: Personality disorders, Eating disorders

**Tracked Attributes**:
- DSM-5/ICD-10 code
- Severity (mild/moderate/severe)
- Remission status
- Onset date
- Last review date

### Substance Use Assessment

**Substances Tracked**:
- Alcohol
- Tobacco/Nicotine
- Cannabis
- Opioids
- Stimulants (cocaine, methamphetamine)
- Benzodiazepines
- Other

**Attributes**:
- Frequency (daily/weekly/monthly/occasional/former)
- Severity (mild/moderate/severe)
- Treatment status
- Risk level (low/moderate/high)

### Suicide Risk Assessment

**Risk Levels**:
- 🟢 **Low**: No current ideation, strong protective factors
- 🟡 **Moderate**: Passive ideation, some risk factors
- 🔴 **High**: Active ideation with plan, multiple risk factors
- 🚨 **Critical**: Immediate danger, requires intervention

**Risk Factors Assessed**:
- Previous suicide attempts
- Family history of suicide
- Recent psychiatric hospitalization
- Access to lethal means
- Substance abuse
- Social isolation
- Recent major loss
- Chronic pain/illness
- Hopelessness

**Protective Factors**:
- Strong family/social support
- Engaged in treatment
- Problem-solving skills
- Religious/spiritual beliefs
- Reasons for living
- Future-oriented thinking

**Requires Intervention**:
- High or Critical risk level
- Recent attempt or gesture
- Specific plan with access to means
- Command hallucinations

### Social Support Assessment

**Metrics**:
- Support level (strong/moderate/weak)
- Has caregiver (yes/no)
- Living situation (alone/with others)
- Social isolation risk

**Impact**: Strong social support reduces mental health risk by 20-30%

### Treatment Engagement

**Metrics**:
- Currently in therapy (yes/no)
- Therapy adherence rate (0-100%)
- Medication compliance (0-100%)
- Last psychiatry visit date
- Last therapy session date

**Positive Indicators**:
- In active treatment (+10 health score points)
- High therapy adherence >80% (+5 points)
- Medication compliance >80% (+5 points)

---

## 🏠 Social Determinants of Health (SDOH)

### SDOH Categories

#### 1. Food Insecurity (Z59.4)
**Screening Questions**:
- "In the past 12 months, worried food would run out before getting money to buy more"
- "In the past 12 months, food bought didn't last and didn't have money to get more"

**Interventions**:
- Food bank referrals
- SNAP (food stamps) application assistance
- WIC program enrollment
- Community meal programs

#### 2. Housing Instability (Z59.0-Z59.9)
**Screening Questions**:
- Housing status (stable/temporary/homeless)
- Behind on rent/mortgage
- Housing quality issues
- Overcrowding

**Interventions**:
- Emergency housing assistance
- Rental assistance programs
- Housing authority referrals
- Homeless services

#### 3. Transportation (Z59.82)
**Screening Questions**:
- "Do you have reliable transportation to medical appointments?"
- "Have you missed appointments due to transportation?"

**Interventions**:
- Public transit passes
- Medical transportation services
- Ride-share vouchers
- Volunteer driver programs

#### 4. Utility Assistance (Z59.1)
**Issues**: Electricity, heat, water shut-offs

**Interventions**:
- LIHEAP (Low Income Home Energy Assistance)
- Utility payment assistance
- Weatherization programs

#### 5. Interpersonal Safety (Z69.-)
**Screening Questions**:
- "Do you feel safe at home?"
- Domestic violence screening

**Interventions**:
- Domestic violence hotlines
- Safety planning
- Shelter referrals
- Legal assistance

#### 6. Education (Z55.-)
**Issues**: Low literacy, educational barriers

**Interventions**:
- Adult education programs
- Health literacy materials
- Teach-back methods

#### 7. Employment (Z56.-)
**Issues**: Unemployment, job insecurity

**Interventions**:
- Job training programs
- Career counseling
- Disability accommodation

#### 8. Social Isolation (Z60.2)
**Screening**: "How often do you feel lonely?"

**Interventions**:
- Senior centers
- Support groups
- Volunteer opportunities
- Faith community connections

#### 9. Financial Strain (Z59.6)
**Screening**: "How hard is it to pay for basics like food, housing, medical care?"

**Interventions**:
- Financial counseling
- Medical billing assistance
- Prescription assistance programs
- Benefits enrollment (SSI, SSDI)

### ICD-10 Z-Codes

The system automatically generates appropriate Z-codes for billing:

- Z55: Education and literacy problems
- Z56: Employment problems
- Z57: Occupational exposure to risk factors
- Z59: Housing and economic circumstances
- Z60: Social environment problems
- Z62: Problems related to upbringing
- Z63: Family circumstances problems
- Z64: Unwanted pregnancy problems
- Z65: Other psychosocial circumstances
- Z69: Encounter for mental health services for victim/perpetrator

### SDOH Risk Scoring

**Severity Levels**:
- **Mild**: 1-2 SDOH needs, all addressed or in progress
- **Moderate**: 3-4 SDOH needs, or severe unaddressed need
- **Severe**: 5+ SDOH needs, or critical unaddressed needs (housing, safety)

**Impact on Health Score**:
- Each severe unaddressed need: -20 points
- Each moderate unaddressed need: -10 points
- Addressed needs: Minimal deduction

### Community Referral Tracking

**Referral Status**:
- **Pending**: Referral made, awaiting contact
- **Active**: Patient engaged with community resource
- **Completed**: Need addressed
- **Cancelled**: Patient declined or resource unavailable

**Follow-up**: System tracks referral outcomes and closes the loop

---

## 📈 Risk Stratification & Predictive Analytics

### Risk Score Components

**1. Clinical Complexity Score** (0-100)
Factors:
- Number of chronic conditions
- Number of medications
- Recent hospitalizations
- ED visits
- Comorbidity burden (Charlson/Elixhauser)

**2. Social Complexity Score** (0-100)
Factors:
- Number of SDOH needs
- Living situation
- Social support level
- Health literacy
- Insurance status

**3. Mental Health Risk Score** (0-100)
Factors:
- PHQ-9/GAD-7 scores
- Mental health diagnoses
- Substance use
- Suicide risk
- Treatment engagement

**4. Utilization Risk Score** (0-100)
Factors:
- ED visit frequency
- Hospital admissions
- Readmissions
- No-show rate
- ER "super-utilizer" status

**5. Cost Risk Score** (0-100)
Factors:
- Total cost of care
- High-cost medications
- Specialty care utilization
- Projected annual costs

### Predictive Analytics

#### 30-Day Hospitalization Risk
**Algorithm**: Machine learning model trained on:
- Demographics
- Chronic conditions
- Recent vitals/labs
- Medication adherence
- Prior utilization
- Functional status

**Risk Levels**:
- <10%: Low risk 🟢
- 10-20%: Moderate risk 🟡
- 20-40%: High risk 🔴
- >40%: Critical risk 🚨

**Actions**:
- High risk → Care management outreach
- Critical risk → Urgent provider review

#### 90-Day Hospitalization Risk
Similar algorithm, longer time horizon

#### 30-Day ED Visit Risk
**High-Risk Indicators**:
- Recent ED visits (>2 in 90 days)
- Uncontrolled chronic conditions
- Poor medication adherence
- Lack of primary care engagement
- SDOH barriers

#### Readmission Risk
**Calculated**: After each hospitalization
**Factors**:
- Reason for admission
- Length of stay
- Discharge medications
- Follow-up appointments scheduled
- Prior readmissions
- SDOH barriers

### Condition-Specific Risk Stratification

**Diabetes Complication Risk**:
- Retinopathy
- Nephropathy
- Neuropathy
- Cardiovascular disease
- Foot ulcers/amputation

**Cardiovascular Event Risk**:
- Myocardial infarction
- Stroke
- Heart failure exacerbation
- Based on ASCVD risk calculator

**Respiratory Complication Risk**:
- COPD exacerbation
- Asthma attacks
- Pneumonia

**Mental Health Crisis Risk**:
- Psychiatric hospitalization
- Suicide attempt
- Substance overdose

**Fall Risk** (for elderly):
- History of falls
- Gait/balance issues
- Medications (sedatives, antihypertensives)
- Visual impairment
- Home hazards

---

## 🎯 Care Gaps & Recommendations Engine

### Care Gap Types

#### 1. Preventive Care Gaps

**Cancer Screenings**:
- Breast cancer (mammogram)
- Cervical cancer (Pap smear/HPV)
- Colorectal cancer (colonoscopy/FIT/Cologuard)
- Lung cancer (LDCT for high-risk)
- Prostate cancer (PSA - shared decision-making)

**Immunizations**:
- Influenza (annual)
- Pneumococcal (PCV15/PCV20)
- Shingles (Shingrix)
- COVID-19
- Tdap

**Screenings**:
- Abdominal aortic aneurysm (AAA)
- Osteoporosis (DEXA scan)
- Depression (PHQ-9)
- Diabetes screening (HbA1c/fasting glucose)
- Lipid panel
- HIV screening
- Hepatitis C

#### 2. Chronic Disease Gaps

**Diabetes**:
- Annual eye exam (dilated retinal)
- Annual foot exam
- HbA1c every 3-6 months
- Microalbumin/creatinine ratio (kidney function)
- Statin therapy (if indicated)
- ACE-inhibitor/ARB (if indicated)
- Aspirin therapy (if indicated)

**Hypertension**:
- BP control (<130/80 or <140/90)
- Annual microalbumin (if diabetic)
- Statin therapy (ASCVD risk >7.5%)

**Asthma/COPD**:
- Asthma control assessment
- Spirometry
- Inhaler technique review
- Action plan

**Heart Failure**:
- LVEF assessment
- Guideline-directed medical therapy
- Diuretic adjustment
- Weight monitoring

#### 3. Mental Health Gaps

**Depression**:
- PHQ-9 follow-up (2-4 weeks after positive screen)
- Antidepressant trial (adequate dose/duration)
- Therapy referral
- Remission assessment

**Anxiety**:
- GAD-7 follow-up
- Treatment initiation
- Response assessment

**Substance Use**:
- SBIRT (Screening, Brief Intervention, Referral to Treatment)
- Medication-assisted treatment (MAT) for opioid use
- Counseling referral

#### 4. Medication Gaps

**Medication Safety**:
- Anticoagulation monitoring (INR)
- Kidney function monitoring (ACE-I, ARB, metformin)
- Liver function monitoring (statins)
- Drug-drug interaction review

**Adherence**:
- Medication reconciliation
- Pill counts/refill tracking
- Barriers assessment

#### 5. Preventive Medications

**Aspirin**: ASCVD risk >10%, age 40-70
**Statins**: ASCVD risk >7.5%, LDL >190, diabetic age 40-75
**ACE-I/ARB**: Diabetic with albuminuria, heart failure, post-MI

### Care Recommendation Categories

#### Treatment Recommendations

**Example**: "Consider SGLT2 Inhibitor for Diabetes"
- **Evidence**: Multiple RCTs (EMPA-REG, CANVAS, DECLARE)
- **Guideline**: ADA Standards of Care 2025
- **Rationale**: HbA1c 7.2%, provides CV/renal benefits
- **Expected Benefit**: HbA1c ↓0.5-1%, ↓CV events, renal protection

#### Referral Recommendations

**Examples**:
- Endocrinology (uncontrolled diabetes)
- Cardiology (heart failure, arrhythmia)
- Nephrology (CKD stage 4+)
- Psychiatry (refractory depression)
- Nutrition (diabetes, obesity, eating disorder)
- Physical therapy (mobility, pain)
- Pain management
- Substance use treatment

#### Lifestyle Recommendations

**Examples**:
- Exercise prescription (150 min/week moderate activity)
- Mediterranean diet for diabetes/CVD
- DASH diet for hypertension
- Smoking cessation (nicotine replacement, varenicline, bupropion)
- Weight loss (if BMI >25)
- Sleep hygiene (if insomnia)
- Stress reduction (mindfulness, yoga)

#### Education Recommendations

**Topics**:
- Diabetes self-management education (DSME)
- Insulin injection technique
- Blood glucose monitoring
- Carbohydrate counting
- Medication adherence strategies
- Disease-specific education
- Mental health literacy

---

## 🔄 Integration Points

### FHIR R4 Resource Mapping

**Patient Health Overview Data Sources**:

#### Physical Health
- **Observation**: Vitals, labs, functional assessments
- **Condition**: Chronic conditions, diagnoses
- **MedicationStatement**: Current medications
- **MedicationRequest**: Prescriptions, refills
- **Procedure**: Surgeries, diagnostic procedures

#### Mental Health
- **QuestionnaireResponse**: PHQ-9, GAD-7, AUDIT-C responses
- **Observation**: Mental health screening scores
- **Condition**: Mental health diagnoses (DSM-5)
- **ServiceRequest**: Therapy, psychiatry referrals

#### Social Determinants
- **Observation**: SDOH screening (LOINC codes)
- **Condition**: Z-codes for SDOH
- **ServiceRequest**: Community referrals

#### Risk & Predictions
- **RiskAssessment**: Calculated risk scores
- **DetectedIssue**: Care gaps

### Quality Measure Integration

The system integrates with existing quality measure service to identify gaps:

**HEDIS Measures**:
- CDC-HbA1c: Diabetes HbA1c control
- CDC-EED: Eye exam for diabetes
- CBP: Controlling blood pressure
- BCS: Breast cancer screening
- COL: Colorectal cancer screening
- IMA: Immunizations for adults
- FUH: Follow-up after mental health hospitalization
- AMR: Antidepressant medication management

**CMS Measures**:
- CMS-122: Diabetes HbA1c control
- CMS-134: Diabetes eye exam
- CMS-138: Preventive care (tobacco screening)
- CMS-165: Controlling blood pressure
- CMS-2: Depression screening and follow-up

### Care Management Workflow Integration

**High-Risk Patient Identification**:
1. System calculates risk scores nightly
2. High-risk patients flagged in care management queue
3. Care manager reviews patient health overview
4. Outreach call scheduled
5. Care plan created based on gaps/recommendations
6. Follow-up tasks assigned

**Care Gaps Closure Workflow**:
1. Provider reviews care gaps during visit
2. Addresses gap (orders test, schedules appointment, prescribes)
3. Marks gap as "In Progress" or "Completed"
4. Quality measure updated
5. Health score recalculated

---

## 💻 Technical Implementation

### Files Created

**Models** (464 lines):
```
apps/clinical-portal/src/app/models/patient-health.model.ts
```

**Services** (854 lines):
```
apps/clinical-portal/src/app/services/patient-health.service.ts
```

**Components**:
```
apps/clinical-portal/src/app/pages/patient-health-overview/
├── patient-health-overview.component.ts (336 lines)
├── patient-health-overview.component.html (1,147 lines)
└── patient-health-overview.component.scss (676 lines)
```

**Total Lines**: ~3,477 lines of code

### Usage

#### Standalone Component

```typescript
import { PatientHealthOverviewComponent } from './pages/patient-health-overview/patient-health-overview.component';

// In template:
<app-patient-health-overview [patientId]="patientId"></app-patient-health-overview>
```

#### Integrated into Patient Detail

```typescript
// In patient-detail.component.html:
<mat-tab label="Health Overview">
  <app-patient-health-overview [patientId]="patientId"></app-patient-health-overview>
</mat-tab>
```

### API Integration (Future)

Currently using mock data. To integrate with real FHIR server:

1. **Replace mock methods in `PatientHealthService`**:
   - `getMockPhysicalHealth()` → Query FHIR Observations, Conditions
   - `getMockMentalHealth()` → Query FHIR QuestionnaireResponses
   - `getMockSDOHSummary()` → Query FHIR SDOH Observations
   - `getMockRiskStratification()` → Call risk scoring API
   - `getMockCareGaps()` → Query quality measure service
   - `getMockCareRecommendations()` → Call clinical decision support API

2. **FHIR Queries**:

```typescript
// Get vitals
GET /Observation?patient={id}&category=vital-signs&_sort=-date&_count=1

// Get labs
GET /Observation?patient={id}&category=laboratory&_sort=-date&_count=50

// Get conditions
GET /Condition?patient={id}&clinical-status=active

// Get mental health screenings
GET /QuestionnaireResponse?patient={id}&questionnaire=PHQ-9

// Get SDOH observations
GET /Observation?patient={id}&category=social-history
```

3. **Risk Scoring API**:

```typescript
POST /api/risk-stratification/calculate
{
  "patientId": "123",
  "includePredictions": true
}
```

4. **Care Gaps API**:

```typescript
GET /api/quality-measures/gaps?patientId={id}
```

---

## 📱 User Interface

### Dashboard Layout

```
┌─────────────────────────────────────────────────────────┐
│  Overall Health Score Card                               │
│  ┌────────────────────────────────────────────────────┐ │
│  │  😊  Overall Health Score: 72 (Good)  → Stable    │ │
│  │                                                     │ │
│  │  Physical:  70%  ████████████░░░░░                │ │
│  │  Mental:    65%  ███████████░░░░░░                │ │
│  │  Social:    80%  ██████████████░░░                │ │
│  │  Preventive: 75% ████████████░░░░                 │ │
│  └────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│  [Physical Health] [Mental Health] [Social Health]       │
│  [Risk Stratification] [Care Gaps]                       │
└─────────────────────────────────────────────────────────┘

Physical Health Tab:
├── Recent Vitals (grid of vital cards)
├── Lab Results (list with status indicators)
├── Chronic Conditions (expandable cards)
├── Medication Adherence (progress bar)
└── Functional Status (ADL/IADL scores)

Mental Health Tab:
├── Screening Assessments (PHQ-9, GAD-7 with scores)
├── Mental Health Diagnoses
├── Substance Use (if applicable)
├── Suicide Risk Assessment
├── Social Support
└── Treatment Engagement

Social Health Tab:
├── SDOH Risk Overview
├── Identified Needs (food, housing, transportation)
├── Active Community Referrals
└── ICD-10 Z-Codes

Risk Stratification Tab:
├── Overall Risk Level
├── Risk Scores (5 components with progress bars)
├── Predictive Analytics (4 predictions with %)
└── Condition-Specific Risks

Care Gaps Tab:
├── Care Gaps (expandable accordion, sorted by priority)
└── Clinical Recommendations (expandable accordion)
```

### Color Coding

**Health Status**:
- 🟢 Excellent/Good: Green (#4caf50, #8bc34a)
- 🟡 Fair: Orange (#ff9800)
- 🔴 Poor: Red (#f44336)

**Risk Levels**:
- 🟢 Low: Green (#4caf50)
- 🟡 Moderate: Orange (#ff9800)
- 🔴 High: Red (#f44336)
- 🚨 Critical: Dark Red (#d32f2f)

**Vital Status**:
- ✅ Normal: Green check circle
- ⚠️ Abnormal: Orange warning
- 🚨 Critical: Red error

**Priority**:
- Low: Green flag
- Medium: Orange flag
- High: Red priority_high
- Urgent: Dark red emergency

---

## 🎓 Clinical Workflows

### Workflow 1: Annual Wellness Visit

**Pre-Visit Preparation**:
1. Provider opens Patient Health Overview
2. Reviews overall health score and trends
3. Identifies care gaps (colorectal screening overdue, flu shot)
4. Notes mental health screening due (PHQ-9)
5. Reviews SDOH needs (food insecurity identified)

**During Visit**:
1. Administers PHQ-9 screening (score: 12 - moderate depression)
2. Orders colorectal cancer screening (colonoscopy vs FIT)
3. Administers flu vaccine
4. Addresses food insecurity (food bank referral)
5. Discusses depression treatment options

**Post-Visit**:
1. Care gaps automatically updated
2. Health score recalculated (improved from 68 to 73)
3. Follow-up PHQ-9 scheduled in 4 weeks
4. Food bank referral tracked

### Workflow 2: High-Risk Patient Outreach

**Care Manager Review**:
1. Patient flagged with 35% 30-day hospitalization risk
2. Opens Health Overview to understand risk factors:
   - Uncontrolled diabetes (HbA1c 9.2%)
   - Moderate depression (PHQ-9: 14)
   - Food insecurity
   - Poor medication adherence (58%)
3. Identifies root causes (can't afford medications)

**Intervention**:
1. Schedules urgent provider visit
2. Refers to pharmacy assistance programs
3. Connects with food bank
4. Schedules diabetes education
5. Arranges medication delivery

**Outcome**:
- 30-day hospitalization risk ↓ from 35% to 18%
- Health score ↑ from 52 to 64
- Patient avoids $45,000 hospital admission

### Workflow 3: Mental Health Crisis Prevention

**Identified Risk**:
1. Patient has high suicide risk (GAD-7: 18, PHQ-9: 21)
2. Recent stressors (job loss, divorce)
3. Substance use (alcohol, daily)
4. Social isolation (lives alone, weak support)

**Safety Planning**:
1. Provider reviews risk assessment
2. Conducts thorough safety evaluation
3. Removes access to lethal means
4. Creates safety plan with crisis contacts
5. Refers to intensive outpatient program (IOP)
6. Schedules follow-up in 2 days

**Monitoring**:
- PHQ-9 reassessed every 2 weeks
- Treatment engagement tracked
- Suicide risk level updated with each visit
- Care manager checks in weekly

---

## 📊 Reporting & Analytics

### Provider Dashboard Metrics

**Population Health Metrics**:
- Average health score by panel
- % patients with excellent/good health status
- % patients with care gaps
- % patients at high risk for hospitalization
- SDOH needs prevalence

**Mental Health Metrics**:
- % patients screened for depression (PHQ-9)
- % positive depression screens
- % in treatment for depression/anxiety
- Average PHQ-9/GAD-7 scores for panel
- Suicide risk distribution

**Quality Measure Performance**:
- % patients meeting quality measures
- Care gaps by category
- Trend over time

### Patient-Level Reports

**Printable Summary**:
- Overall health score and components
- Vital signs table
- Lab results with trends
- Chronic conditions list
- Mental health screening results
- Care gaps and recommendations

**Longitudinal Report**:
- Health score trend (12 months)
- HbA1c trend (diabetes)
- BP trend (hypertension)
- PHQ-9 trend (depression)
- Weight trend
- Medication adherence trend

---

## 🚀 Implementation Roadmap

### Phase 1: Core Health Overview (Weeks 1-2)
- ✅ Data models created
- ✅ Patient Health Service implemented
- ✅ Health Overview Component built
- ✅ Overall health score calculation
- ✅ Physical health tab
- ⏳ Integration with existing patient detail page

### Phase 2: Mental Health Integration (Weeks 3-4)
- ✅ PHQ-9, GAD-7, PHQ-2 scoring algorithms
- ✅ Mental health assessment UI
- ⏳ Interactive screening questionnaire component
- ⏳ Mental health data entry forms
- ⏳ Suicide risk documentation

### Phase 3: SDOH & Risk (Weeks 5-6)
- ✅ SDOH data model
- ✅ Risk stratification algorithms
- ⏳ SDOH screening questionnaire
- ⏳ Community referral workflow
- ⏳ Risk prediction model integration

### Phase 4: Care Gaps & Recommendations (Weeks 7-8)
- ✅ Care gap identification logic
- ✅ Recommendations engine
- ⏳ Integration with quality measure service
- ⏳ Clinical decision support rules
- ⏳ Care gap closure workflow

### Phase 5: Real Data Integration (Weeks 9-10)
- ⏳ FHIR observation queries (vitals, labs)
- ⏳ FHIR condition queries
- ⏳ FHIR medication queries
- ⏳ QuestionnaireResponse integration
- ⏳ Risk scoring API integration

### Phase 6: Advanced Features (Weeks 11-12)
- ⏳ Trend visualization charts
- ⏳ Health metric history graphs
- ⏳ Predictive analytics dashboard
- ⏳ Care plan builder
- ⏳ Patient portal view (simplified)

---

## 📚 Resources & References

### Clinical Guidelines

**Depression**:
- APA Practice Guideline for Major Depressive Disorder (2010)
- VA/DoD Clinical Practice Guideline for MDD (2022)

**Diabetes**:
- ADA Standards of Medical Care in Diabetes (2025)
- AACE/ACE Diabetes Guidelines

**Hypertension**:
- ACC/AHA Hypertension Guidelines (2017)

**SDOH**:
- CMMS SDOH Framework
- Protocol for Responding to and Assessing Patients' Assets, Risks, and Experiences (PRAPARE)

### Screening Tools

**Mental Health**:
- PHQ-9: Kroenke K, Spitzer RL, Williams JB. JGIM 2001
- GAD-7: Spitzer RL, Kroenke K, Williams JB, Löwe B. Arch Intern Med 2006
- PHQ-2: Kroenke K, Spitzer RL, Williams JB. Med Care 2003

**SDOH**:
- PRAPARE Screening Tool: https://www.nachc.org/research-and-data/prapare/
- Health Leads Screening Toolkit: https://healthleadsusa.org/

### Coding References

**ICD-10 Z-Codes for SDOH**:
- Z55: Educational and literacy problems
- Z56: Employment problems
- Z59: Housing and economic circumstances
- Z60: Social environment problems
- Z62-Z65: Other psychosocial circumstances

**LOINC Codes for Screenings**:
- 44249-1: PHQ-9 total score
- 69737-5: GAD-7 total score
- 93030-5: PRAPARE screening panel

---

## ✅ Testing Checklist

### Unit Tests
- [ ] Health score calculation logic
- [ ] PHQ-9 scoring algorithm
- [ ] GAD-7 scoring algorithm
- [ ] Risk level determination
- [ ] Care gap identification
- [ ] Mock data generators

### Integration Tests
- [ ] FHIR observation query and parsing
- [ ] FHIR condition query and parsing
- [ ] QuestionnaireResponse submission
- [ ] Quality measure service integration
- [ ] Care gap API integration

### UI Tests
- [ ] Overall health score card displays correctly
- [ ] All tabs load without errors
- [ ] Vital signs display with correct status icons
- [ ] Mental health assessments render properly
- [ ] Care gaps expandable panels work
- [ ] Risk stratification metrics display
- [ ] Responsive design (mobile, tablet, desktop)

### Clinical Validation
- [ ] PHQ-9 score interpretation matches published guidelines
- [ ] GAD-7 score interpretation matches published guidelines
- [ ] Health score reflects clinical reality
- [ ] Care gaps align with quality measures
- [ ] Recommendations are evidence-based

---

## 🎯 Success Metrics

**Provider Adoption**:
- 80%+ providers use Health Overview weekly
- Average 5+ minutes per patient reviewing dashboard
- 90%+ providers find it valuable (survey)

**Clinical Outcomes**:
- 20% increase in depression screening rates
- 30% increase in care gap closure
- 15% reduction in high-risk patient hospitalizations
- 25% improvement in diabetes HbA1c control

**Patient Experience**:
- 40% increase in SDOH needs addressed
- 30% increase in mental health treatment engagement
- Improved patient satisfaction scores

**Quality Measures**:
- 10-15% improvement in HEDIS/CMS measure rates
- Reduced quality measure gaps per patient
- Higher provider quality scores

---

## 📖 Additional Documentation

For more information, see:
- **KNOWLEDGE_BASE_GUIDE.md** - System architecture and user guides
- **AI_AGENT_TRAINING_GUIDE.md** - AI/ML integration strategies
- **CLINICAL_PORTAL_DESIGN.md** - Overall portal design patterns

---

*This guide will be updated as implementation progresses and additional features are added.*
