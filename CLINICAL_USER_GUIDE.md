# Patient Health Overview - Clinical User Guide

**For**: Physicians, Nurse Practitioners, Physician Assistants, Care Coordinators
**Version**: 1.0
**Date**: November 20, 2025

---

## Table of Contents

1. [Introduction](#introduction)
2. [Accessing the Health Overview](#accessing-the-health-overview)
3. [Understanding the Overall Health Score](#understanding-the-overall-health-score)
4. [Physical Health Tab](#physical-health-tab)
5. [Mental Health Tab](#mental-health-tab)
6. [Social Determinants Tab](#social-determinants-tab)
7. [Risk Stratification Tab](#risk-stratification-tab)
8. [Care Gaps Tab](#care-gaps-tab)
9. [Clinical Workflows](#clinical-workflows)
10. [Mental Health Screening Quick Reference](#mental-health-screening-quick-reference)

---

## Introduction

The Patient Health Overview provides a comprehensive, at-a-glance view of your patient's overall health status, integrating:

- **Physical Health**: Vital signs, lab results, chronic conditions, medication adherence
- **Mental Health**: Screening assessments (PHQ-9, GAD-7), diagnoses, suicide risk
- **Social Determinants**: Food insecurity, housing, transportation, social support
- **Risk Stratification**: Predictive analytics for hospitalization, ED visits, readmissions
- **Care Gaps**: Quality measure deficiencies and evidence-based recommendations

---

## Accessing the Health Overview

### Navigation Path

1. From the main menu, click **Patients**
2. Select a patient from the patient list
3. In the patient detail page, click the **Health Overview** tab

![Navigation Example](navigation-example.png)

---

## Understanding the Overall Health Score

### What is the Health Score?

A **composite score from 0-100** that represents the patient's overall health status, combining four weighted components:

| Component | Weight | Description |
|-----------|--------|-------------|
| Physical Health | 40% | Vital signs, labs, chronic condition control |
| Mental Health | 30% | Mental health assessments, diagnoses, treatment engagement |
| Social Determinants | 15% | SDOH needs, social support, resource access |
| Preventive Care | 15% | Quality measures, preventive screenings |

### Score Interpretation

| Score Range | Health Status | Visual Indicator | Clinical Significance |
|-------------|---------------|------------------|----------------------|
| 85-100 | **Excellent** | Green | Patient is thriving, minimal intervention needed |
| 70-84 | **Good** | Light Green | Patient is stable, maintain current care plan |
| 50-69 | **Fair** | Orange | Moderate concerns, requires attention |
| 0-49 | **Poor** | Red | Significant health issues, urgent action needed |

### Trend Icons

- **↗ Improving**: Health metrics are getting better over time
- **→ Stable**: Health metrics remain consistent
- **↘ Declining**: Health metrics are worsening - **requires intervention**

---

## Physical Health Tab

### 1. Recent Vitals

Displays the most recent measurement for each vital sign:

- **Blood Pressure**
- **Heart Rate**
- **Temperature**
- **Weight & BMI**
- **Oxygen Saturation**

#### Status Indicators

- ✅ **Green checkmark**: Normal range
- ⚠️ **Yellow warning**: Abnormal (outside normal range)
- 🔴 **Red error**: Critical value - **immediate attention required**

#### Reference Ranges

Hover over any vital sign to see the normal reference range.

### 2. Recent Lab Results

Shows significant lab values from the past 90 days:

- **HbA1c** (diabetes management)
- **Lipid panel** (cardiovascular risk)
- **Kidney function** (creatinine, eGFR)
- **Liver function** (ALT, AST)

**Action Items**: Abnormal or critical lab values are highlighted in yellow/red. Click for details and historical trends.

### 3. Chronic Conditions

Lists active chronic conditions with:

- **Severity**: Mild, Moderate, Severe
- **Control Status**: Controlled ✅ | Not Controlled ⚠️
- **Last Review Date**
- **Complications** (if any)

**Best Practice**: Review "Not Controlled" conditions during each visit and update control status based on recent clinical data.

### 4. Medication Adherence

- **Overall Adherence Rate**: Percentage of medications taken as prescribed
- **Status**: Excellent (≥80%) | Good (60-79%) | Poor (<60%)
- **Problematic Medications**: Lists specific medications with adherence issues

**Workflow Tip**: Poor adherence (< 60%) should trigger a medication reconciliation and barriers discussion.

---

## Mental Health Tab

### 1. Screening Assessments

Displays results from validated mental health screening tools.

#### PHQ-9 (Depression Screening)

**Score Range**: 0-27

| Score | Severity | Action Required |
|-------|----------|-----------------|
| 0-4 | Minimal | No action needed |
| 5-9 | Mild | Monitor, consider counseling referral |
| 10-14 | **Moderate** | **Requires follow-up within 30 days** |
| 15-19 | Moderately Severe | Consider psychiatry referral, treatment initiation |
| 20-27 | Severe | **Urgent**: Same-day assessment, safety planning |

**Positive Screen**: Score ≥ 10

**Required Action**: Document follow-up plan within 30 days (CMS2 quality measure)

#### GAD-7 (Anxiety Screening)

**Score Range**: 0-21

| Score | Severity | Action Required |
|-------|----------|-----------------|
| 0-4 | Minimal | No action needed |
| 5-9 | Mild | Monitor, lifestyle interventions |
| 10-14 | **Moderate** | **Requires follow-up within 30 days** |
| 15-21 | Severe | Consider psychiatry referral, treatment initiation |

**Positive Screen**: Score ≥ 10

#### PHQ-2 (Brief Depression Screen)

**Score Range**: 0-6

**Positive Screen**: Score ≥ 3

**Action**: If positive, administer full PHQ-9 assessment

### 2. Suicide Risk Assessment

**Risk Levels**: Low | Moderate | High | **Critical**

#### High or Critical Risk - Immediate Actions:

1. **Do Not Leave Patient Alone**
2. Conduct comprehensive suicide risk assessment
3. Remove access to lethal means
4. Consult psychiatry immediately
5. Consider hospitalization
6. Document safety plan

**Risk Factors Displayed**:
- Previous suicide attempt (non-modifiable)
- Current suicidal ideation
- Lack of social support (modifiable)
- Substance use
- Access to lethal means

**Protective Factors**:
- Strong family support
- Engaged in treatment
- Religious/spiritual beliefs
- Reasons for living

### 3. Treatment Engagement

- **In Therapy**: Yes/No
- **Therapy Adherence**: % of sessions attended
- **Medication Compliance**: % of doses taken
- **Last Psychiatry Visit**: Date

**Best Practice**: For patients with moderate-severe mental health conditions not in treatment, discuss referral options and barriers to care.

---

## Social Determinants of Health Tab

### SDOH Categories

The system screens for 9 SDOH domains:

1. **Food Insecurity**
   - "Often" or "Sometimes" worried about running out of food
   - **Action**: Referral to food bank, SNAP enrollment

2. **Housing Instability**
   - Homelessness, inadequate housing, housing cost burden
   - **Action**: Social work referral, housing resources

3. **Transportation**
   - Lack of transportation affecting medical appointments
   - **Action**: Transportation vouchers, telehealth options

4. **Utility Assistance**
   - Threatened utility shut-off
   - **Action**: LIHEAP (Low Income Home Energy Assistance Program)

5. **Interpersonal Safety**
   - Domestic violence, unsafe living situation
   - **Action**: Safety planning, domestic violence resources

6. **Education & Employment**
   - Unemployment, low educational attainment
   - **Action**: Job training programs, GED resources

7. **Financial Strain**
   - Difficulty paying for basics (food, housing, healthcare)
   - **Action**: Financial counseling, assistance programs

8. **Social Isolation**
   - Living alone, lack of social connections
   - **Action**: Senior center, support groups, community programs

### SDOH Workflow

1. **Identify**: Review SDOH needs flagged in the system
2. **Assess Severity**: Mild | Moderate | Severe
3. **Refer**: Use "Active Community Referrals" section to track outreach
4. **Follow-up**: Monitor referral status and patient engagement
5. **Document**: Use Z-codes for billing (see Z-codes section below)

### Z-Codes for SDOH

Document SDOH in problem list using ICD-10 Z-codes:

| Z-Code | Description |
|--------|-------------|
| Z59.0 | Homelessness |
| Z59.1 | Inadequate housing |
| Z59.4 | Lack of adequate food |
| Z59.5 | Extreme poverty |
| Z59.6 | Low income |
| Z55.9 | Problems related to education |
| Z56.9 | Unspecified problems related to employment |
| Z60.2 | Problems related to living alone |
| Z75.3 | Unavailability of health care facilities |

**Billing Note**: Z-codes support value-based care initiatives and may affect risk adjustment.

---

## Risk Stratification Tab

### Overall Risk Level

| Risk Level | Meaning | Action |
|------------|---------|--------|
| Low | Minimal risk of adverse events | Standard care, routine follow-up |
| Moderate | Some risk factors present | Enhanced monitoring, care coordination |
| High | Multiple risk factors, complex case | Intensive case management, frequent touch-points |
| Critical | **Imminent risk of poor outcomes** | **Urgent intervention, multidisciplinary care** |

### Predictive Analytics

#### 30-Day Hospitalization Risk

- **< 10%**: Low risk (green)
- **10-20%**: Moderate risk (yellow)
- **> 20%**: High risk (red) - **Consider proactive outreach**

**High-Risk Interventions**:
- Weekly nurse check-in calls
- Medication reconciliation
- Care transition planning
- Specialist coordination

#### 30-Day ED Visit Risk

- **< 15%**: Low risk
- **15-25%**: Moderate risk
- **> 25%**: High risk - **Address barriers to primary care access**

**Interventions**:
- Same-day sick visit availability
- Telehealth options
- Patient education on when to seek ED vs. urgent care

#### Readmission Risk

For patients recently discharged from hospital:

- **< 10%**: Low risk
- **10-20%**: Moderate risk
- **> 20%**: High risk - **Requires transition of care protocol**

**Best Practice**: Schedule follow-up appointment within 7 days of discharge for high-risk patients.

### Condition-Specific Risks

- **Diabetes Complications**: Risk of diabetic ketoacidosis, neuropathy, retinopathy
- **Cardiovascular Events**: Risk of MI, stroke, heart failure exacerbation
- **Respiratory Complications**: Risk of COPD exacerbation, pneumonia
- **Mental Health Crisis**: Risk of psychiatric hospitalization, self-harm
- **Fall Risk**: Risk of falls requiring medical attention

---

## Care Gaps Tab

### What are Care Gaps?

Deficiencies in evidence-based care that may affect patient outcomes and quality measure performance.

### Priority Levels

| Priority | Meaning | Action Timeframe |
|----------|---------|------------------|
| 🔴 **Urgent** | Immediate patient safety concern | **Same day** |
| 🟠 **High** | Quality measure due soon, significant clinical impact | **Within 30 days** |
| 🟡 **Medium** | Quality measure approaching due date | **Within 90 days** |
| 🟢 **Low** | Preventive care, not yet due | **Next scheduled visit** |

### Common Care Gaps

1. **Mental Health Follow-up After Positive Screen**
   - **Trigger**: PHQ-9 ≥ 10 or GAD-7 ≥ 10
   - **Required Action**: Document follow-up plan within 30 days
   - **Quality Measure**: CMS2

2. **Diabetes Care (HbA1c Testing)**
   - **Trigger**: No HbA1c in past 6 months for diabetic patient
   - **Required Action**: Order HbA1c lab
   - **Quality Measure**: HEDIS CDC

3. **Blood Pressure Control**
   - **Trigger**: BP ≥ 140/90 for patient with hypertension
   - **Required Action**: Medication adjustment or lifestyle counseling
   - **Quality Measure**: HEDIS CBP

4. **Colorectal Cancer Screening**
   - **Trigger**: Patient 50-75 years old, no screening documented
   - **Required Action**: Order colonoscopy or FIT test
   - **Quality Measure**: HEDIS COL

### Care Gap Actions

Each care gap includes:

- **Recommended Actions**: Step-by-step clinical interventions
- **Barriers**: Known obstacles to completing the gap (e.g., transportation, cost)
- **Associated Quality Measure**: HEDIS/CMS measure linked to the gap

**Workflow**:

1. Review gaps at start of visit
2. Prioritize by urgency and patient preference
3. Address gaps during visit
4. Click "Mark as Addressed" and document interventions
5. System will update quality measure compliance

---

## Clinical Workflows

### Workflow 1: Annual Wellness Visit

**Before Visit**:
1. Open Patient Health Overview
2. Review Overall Health Score and trend
3. Identify urgent care gaps (red priority)
4. Review mental health screening dates
5. Check SDOH needs

**During Visit**:
1. Address urgent/high-priority care gaps
2. Administer mental health screenings (PHQ-9, GAD-7) if due
3. Screen for SDOH needs
4. Review medication adherence
5. Update chronic condition control status

**After Visit**:
1. Document addressed care gaps
2. Order missing labs/screenings
3. Create referrals for positive screens
4. Update care plan

### Workflow 2: Positive Mental Health Screen Response

**PHQ-9 ≥ 10 or GAD-7 ≥ 10**:

**Immediate Actions**:
1. ✅ Assess suicide risk (if not already done)
2. ✅ Discuss results with patient
3. ✅ Explore treatment preferences (therapy, medication, both)
4. ✅ Document follow-up plan

**Follow-up Plan Options**:
- Schedule appointment with integrated behavioral health provider (preferred)
- Provide psychiatry/therapy referral
- Initiate medication management if appropriate
- Schedule 2-week follow-up to reassess symptoms

**Documentation**:
- System automatically creates care gap for CMS2 compliance
- Document plan in clinical notes
- Mark care gap as "Addressed" once plan is in place

### Workflow 3: High-Risk Patient Outreach

**For patients with high/critical overall risk**:

**Weekly Care Coordination**:
1. Review risk factors and recent utilization
2. Nurse makes weekly check-in call
3. Address barriers to care
4. Medication adherence check
5. Symptom monitoring
6. Document encounter

**Monthly Case Review**:
1. Multidisciplinary team meeting
2. Review care plan effectiveness
3. Adjust interventions as needed
4. Update risk stratification

---

## Mental Health Screening Quick Reference

### PHQ-9 Scoring

| Q# | Question | Scoring |
|----|----------|---------|
| 1 | Little interest or pleasure in doing things | 0-3 |
| 2 | Feeling down, depressed, or hopeless | 0-3 |
| 3 | Trouble falling/staying asleep or sleeping too much | 0-3 |
| 4 | Feeling tired or having little energy | 0-3 |
| 5 | Poor appetite or overeating | 0-3 |
| 6 | Feeling bad about yourself or that you're a failure | 0-3 |
| 7 | Trouble concentrating | 0-3 |
| 8 | Moving or speaking slowly or being fidgety/restless | 0-3 |
| 9 | Thoughts that you would be better off dead or hurting yourself | 0-3 |

**Response Options**:
- 0 = Not at all
- 1 = Several days
- 2 = More than half the days
- 3 = Nearly every day

**Total Score**: Sum of all responses (0-27)

**Question 9 Positive = Immediate Suicide Risk Assessment Required**

### GAD-7 Scoring

| Q# | Question | Scoring |
|----|----------|---------|
| 1 | Feeling nervous, anxious, or on edge | 0-3 |
| 2 | Not being able to stop or control worrying | 0-3 |
| 3 | Worrying too much about different things | 0-3 |
| 4 | Trouble relaxing | 0-3 |
| 5 | Being so restless that it's hard to sit still | 0-3 |
| 6 | Becoming easily annoyed or irritable | 0-3 |
| 7 | Feeling afraid, as if something awful might happen | 0-3 |

**Total Score**: Sum of all responses (0-21)

### Safety Planning for Suicide Risk

**Red Flags** (Require immediate intervention):
- Recent suicide attempt
- Current suicidal ideation with plan
- Recent significant loss (job, relationship, loved one)
- Substance intoxication
- Access to lethal means

**Safety Plan Components**:
1. **Warning Signs**: Personal signs that crisis may be developing
2. **Internal Coping Strategies**: Activities to do alone without contacting others
3. **People/Social Settings**: List of people and social settings that provide distraction
4. **People to Ask for Help**: Trusted contacts
5. **Professionals to Contact**: Therapist, psychiatrist, crisis line (988)
6. **Making Environment Safe**: Remove access to lethal means

---

## Tips for Maximum Effectiveness

### 1. Review Before Every Visit
Make the Health Overview part of your pre-visit chart review. Spend 2-3 minutes scanning:
- Overall health score trend
- Urgent care gaps (red flags)
- Recent mental health screens
- Current SDOH needs

### 2. Use Visual Indicators
Pay attention to color coding:
- **Red** = Urgent action needed
- **Yellow** = Requires attention
- **Green** = Stable, no immediate concerns

### 3. Leverage Care Gap Recommendations
Each care gap includes evidence-based recommended actions. Use these as clinical decision support.

### 4. Track Trends Over Time
Use the trend indicators to identify:
- Patients declining despite treatment (need plan adjustment)
- Patients improving (reinforce current interventions)

### 5. Document as You Go
Mark care gaps as addressed during the visit to:
- Improve quality measure compliance
- Reduce end-of-quarter documentation burden
- Ensure continuity across care team

---

## Support & Training

### Video Tutorials
- **Getting Started with Health Overview** (3 minutes)
- **Mental Health Screening Workflow** (5 minutes)
- **Addressing SDOH Needs** (4 minutes)

### Live Training Sessions
- Weekly "Lunch & Learn" sessions every Thursday 12-1pm
- 1:1 training available upon request

### Technical Support
- Help Desk: support@healthdata.com
- Phone: 1-800-HEALTH1
- Live Chat: Available 8am-6pm ET

---

## Frequently Asked Questions

**Q: How often is the health score updated?**
A: The health score recalculates automatically whenever new data is added (new vital signs, lab results, assessments, etc.).

**Q: Can I override the risk level if I disagree with the assessment?**
A: Yes. Click the risk level indicator and select "Override Risk Assessment". Document your clinical rationale.

**Q: What if a patient refuses mental health screening?**
A: Document the refusal and patient's stated reason. The system will flag this as a care gap until screening is completed or patient preference is documented.

**Q: How do SDOH referrals get tracked?**
A: When you create a community referral, the system tracks its status. Partner organizations can update status (pending → active → completed).

**Q: Can I print the Health Overview for my patient?**
A: Yes. Click the print icon to generate a patient-friendly version with educational content.

---

## Conclusion

The Patient Health Overview integrates clinical, behavioral, and social data to provide a holistic view of patient health. By using this tool effectively, you can:

✅ Identify at-risk patients proactively
✅ Close care gaps efficiently
✅ Improve mental health screening and follow-up
✅ Address social determinants of health
✅ Enhance quality measure performance
✅ Provide better coordinated, patient-centered care

**Remember**: The goal is not just better data, but better outcomes. Use these insights to have meaningful conversations with your patients about their health and partner with them to achieve their wellness goals.

---

**Document Version**: 1.0
**Last Updated**: November 20, 2025
**Next Review Date**: February 20, 2026
