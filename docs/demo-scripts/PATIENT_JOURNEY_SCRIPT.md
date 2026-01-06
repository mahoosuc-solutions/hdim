# Patient Care Journey - Demo Script

**Scenario**: 360° Patient View & Clinical Decision Support
**Duration**: 4-6 minutes
**Target Audience**: ACO Care Managers, Medical Directors
**Value Proposition**: Comprehensive patient view with actionable care gaps and risk insights

---

## Pre-Recording Setup

### System State
- [ ] Demo environment running
- [ ] Logged in as: `demo_provider@acmehealth.com`
- [ ] Tenant: "Acme Health Plan"
- [ ] Demo mode enabled
- [ ] Scenario loaded: `./demo-cli load-scenario patient-journey`
- [ ] Pre-selected patient: Michael Chen (Complex Diabetic)

### Browser Setup
- [ ] URL: `http://localhost:4200/patients?demo=true`
- [ ] Resolution: 1920x1080
- [ ] Full screen mode (F11)

---

## Narration Script

### INTRO (0:00 - 0:30)

**[Screen: Patient Search]**

> "Today I'll show you how HDIM provides a comprehensive 360-degree view of your patients, enabling proactive care management and closing care gaps efficiently."

---

## Step-by-Step Actions

### STEP 1: Search for Patient (0:30 - 1:00)

**Action**: Type "Michael Chen" in patient search

**Narration**:
> "Let's look at Michael Chen, a 58-year-old male with Type 2 Diabetes. He's a complex patient with multiple comorbidities that we need to manage carefully."

**Key Visuals**:
- Search results showing patient card
- Risk score indicator (High Risk: 2.3)
- Quick preview of conditions

---

### STEP 2: Patient Overview (1:00 - 2:00)

**Action**: Click patient card to open detail view

**Narration**:
> "Here's Michael's comprehensive patient overview. On the left, we see his demographics and primary conditions. On the right, his open care gaps and risk score trend.
>
> Notice he has 4 open care gaps - let's prioritize the ones with the highest impact."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────────────┐
│ Michael Chen                              Risk Score: 2.3   │
│ DOB: 04/15/1967 | Medicare ID: 1EG4-TE5-MK72               │
├─────────────────────────────────────────────────────────────┤
│ PRIMARY CONDITIONS          │ OPEN CARE GAPS                │
│ • Type 2 Diabetes (E11.9)   │ ⚠ HbA1c Test (Overdue 45d)   │
│ • Essential Hypertension    │ ⚠ Eye Exam (Overdue 8mo)     │
│ • CKD Stage 3               │ ⚠ Nephrology Referral        │
│ • Hyperlipidemia            │ ⚠ Statin Adherence Review    │
│                             │                               │
│ RECENT ENCOUNTERS           │ RISK TREND                    │
│ 12/15 - PCP Visit           │ ████████████░░ ↑ 15% (90d)  │
│ 11/20 - Lab Work            │                               │
│ 10/05 - Urgent Care         │                               │
└─────────────────────────────────────────────────────────────┘
```

---

### STEP 3: Review Care Gap Details (2:00 - 3:00)

**Action**: Click "HbA1c Test (Overdue 45d)"

**Narration**:
> "Let's dive into his HbA1c care gap. The system shows Michael's last A1c was 8.2% - above target. His test is now 45 days overdue.
>
> HDIM automatically recommends interventions based on evidence-based guidelines and shows the projected ROI for closing this gap."

**Screen Elements**:
- Last A1c value: 8.2% (Target: <7.0%)
- Days overdue: 45
- Recommended action: Schedule lab work
- Projected impact: Quality measure compliance + cost savings

---

### STEP 4: View Risk Factors (3:00 - 4:00)

**Action**: Click "Risk Stratification" tab

**Narration**:
> "Michael's risk score of 2.3 puts him in the top 10% of our population. Let's see what's driving this risk.
>
> The HCC model identifies his primary risk drivers: diabetes with complications, CKD stage 3, and hypertension. Combined, these put him at elevated risk for hospitalization."

**Key Metrics**:
- HCC Risk Score: 2.3
- Predicted annual cost: $24,500
- 90-day hospitalization risk: 18%
- Primary risk drivers with weighted contributions

---

### STEP 5: Create Care Plan (4:00 - 5:00)

**Action**: Click "Create Care Plan"

**Narration**:
> "Now let's create a proactive care plan. HDIM suggests interventions prioritized by impact and urgency.
>
> We'll schedule his A1c test, refer him to nephrology, and enroll him in our diabetes management program. The system will track completion and remind us if gaps remain open."

**Actions to demonstrate**:
1. Select recommended interventions
2. Assign to care coordinator
3. Set follow-up reminders
4. Generate patient outreach letter

---

### STEP 6: Patient Timeline (5:00 - 5:30)

**Action**: Click "Timeline" tab

**Narration**:
> "Finally, the timeline view shows Michael's complete care journey - every encounter, lab result, medication change, and care gap closure in one chronological view.
>
> This gives your care team complete visibility into the patient's history without switching between systems."

---

### CLOSING (5:30 - 6:00)

**Narration**:
> "This comprehensive patient view enables proactive, personalized care management at scale. You can identify high-risk patients, close care gaps efficiently, and demonstrate measurable improvements in quality and outcomes.
>
> That's the power of HealthData-in-Motion."

---

## Key Demo Patients

| Name | Profile | Key Conditions | Care Gaps |
|------|---------|----------------|-----------|
| Michael Chen | Complex Diabetic | T2DM, CKD3, HTN | HbA1c, Eye Exam, Statin |
| Sarah Martinez | Preventive Gap | Healthy, Age 52 | Mammogram (8mo overdue) |
| Emma Johnson | High-Risk Multi-Morbid | CHF, COPD, DM | Multiple quality gaps |
| Carlos Rodriguez | SDOH Barriers | DM, Depression | Transportation barriers |

---

## Performance Metrics

| Action | Target Time |
|--------|-------------|
| Patient search | < 500ms |
| Patient detail load | < 1s |
| Risk calculation | < 2s |
| Timeline render | < 1s |

---

**Last Updated**: January 2026
