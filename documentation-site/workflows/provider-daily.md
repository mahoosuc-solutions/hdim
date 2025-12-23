# Daily Provider Workflow

## Overview

This workflow describes the typical daily routine for a healthcare provider using the HDIM Clinical Portal. It covers reviewing quality metrics, addressing care gaps, and preparing for patient visits.

## Participants

- **Primary**: Provider (MD, DO, NP, PA)
- **Supporting**: RN, MA

## Trigger

- Start of clinic day
- Patient visit scheduled

## Time Estimate

- Morning Review: 15-30 minutes
- Per-Patient Prep: 2-5 minutes

---

## Workflow Steps

### Morning Review

```
┌─────────────────────────────────────────────────────────────┐
│  1. Login to Clinical Portal                                 │
│     ↓                                                        │
│  2. View Provider Dashboard                                  │
│     ↓                                                        │
│  3. Review High-Priority Care Gaps                           │
│     ↓                                                        │
│  4. Review Pending Results (Labs/Imaging)                    │
│     ↓                                                        │
│  5. Sign Abnormal Results                                    │
│     ↓                                                        │
│  6. Review Quality Measure Performance                       │
│     ↓                                                        │
│  7. Check Today's Schedule                                   │
└─────────────────────────────────────────────────────────────┘
```

#### Step 1: Login to Clinical Portal

1. Navigate to the Clinical Portal URL
2. Enter username and password
3. Select your organization (if multi-tenant)
4. Click "Login"

**Expected Outcome**: Dashboard loads with Provider view

---

#### Step 2: View Provider Dashboard

1. Dashboard displays automatically after login
2. If not, click "Dashboard" in navigation
3. Select "Provider" view from dropdown (if needed)

**Expected Outcome**: Provider-specific widgets displayed

---

#### Step 3: Review High-Priority Care Gaps

1. Locate "High-Priority Care Gaps" widget
2. Review gaps by risk level:
   - **Critical** (red): Requires immediate attention
   - **High** (orange): Address within 48 hours
   - **Moderate** (yellow): Address this week
3. For each gap, note patient and gap type
4. Click gap row to view details

**Expected Outcome**: Aware of urgent care gaps for today's patients

**Actions Available**:
- Click "View Patient" → Navigate to patient detail
- Click "Address Gap" → Open gap closure form

---

#### Step 4: Review Pending Results

1. Locate "Pending Results" widget
2. Review results requiring signature
3. Note abnormal values (highlighted in red)
4. Prioritize abnormal results

**Expected Outcome**: Aware of results needing action

---

#### Step 5: Sign Abnormal Results

1. Click on result row to open details
2. Review result values and reference ranges
3. Click "Sign Result" button
4. Confirm signing in dialog
5. Add clinical note if needed

**Expected Outcome**: Result marked as reviewed, removed from pending list

::: warning Abnormal Results
Abnormal results display a warning message. Consider documenting follow-up action.
:::

---

#### Step 6: Review Quality Measure Performance

1. Locate "Quality Measures" widget
2. Review top measures by performance
3. Note measures below target (red)
4. Click measure for detailed breakdown

**Expected Outcome**: Aware of quality improvement opportunities

**Key Metrics**:
- Current performance %
- Target %
- Trend (↑ improving, ↓ declining, → stable)

---

#### Step 7: Check Today's Schedule

1. Locate "Today's Schedule" widget
2. Review patient list with appointment times
3. Note any patients with care gaps
4. Click patient name for detail

**Expected Outcome**: Prepared for today's patient visits

---

### Per-Patient Preparation

For each scheduled patient:

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Patient Detail                               │
│     ↓                                                        │
│  2. Review Open Care Gaps                                    │
│     ↓                                                        │
│  3. Review Recent Evaluations                                │
│     ↓                                                        │
│  4. Review Clinical Data                                     │
│     ↓                                                        │
│  5. Address Care Gaps During Visit                           │
│     ↓                                                        │
│  6. Document Gap Closure                                     │
└─────────────────────────────────────────────────────────────┘
```

#### Step 1: Navigate to Patient Detail

1. Click patient name in schedule
2. Or search for patient in navigation bar
3. Patient detail page loads

**Route**: `/patients/{patientId}`

---

#### Step 2: Review Open Care Gaps

1. Click "Care Gaps" tab
2. Review all open gaps for patient
3. Note recommended actions
4. Plan to address during visit

---

#### Step 3: Review Recent Evaluations

1. Click "Evaluations" tab
2. Review evaluation history
3. Note compliance status
4. Identify improvement areas

---

#### Step 4: Review Clinical Data

1. Click "Clinical" tab
2. Review relevant observations (vitals, labs)
3. Review active conditions
4. Review current medications

---

#### Step 5: Address Care Gaps During Visit

1. Perform recommended interventions
2. Order tests/referrals as needed
3. Provide patient education
4. Document actions in EHR

---

#### Step 6: Document Gap Closure

1. Return to patient's care gaps
2. Click "Close Gap" on addressed gaps
3. Select closure reason (Completed)
4. Add documentation notes
5. Submit closure

---

## Expected Outcomes

| Metric | Target |
|--------|--------|
| Care gaps reviewed | 100% of critical/high |
| Abnormal results signed | Within 24 hours |
| Quality metrics reviewed | Daily |
| Patient prep completed | Before each visit |

## Troubleshooting

### Dashboard Not Loading

1. Check internet connection
2. Refresh browser (Ctrl+F5)
3. Clear browser cache
4. Contact support if persists

### Care Gaps Not Showing

1. Verify patient has evaluations
2. Check filter settings
3. Ensure data is synced
4. Report to administrator

## Related User Stories

- [US-DB-005: View High-Priority Care Gaps](/user-stories/dashboards#us-db-005)
- [US-DB-006: View Quality Measure Performance](/user-stories/dashboards#us-db-006)
- [US-DB-007: View Pending Results](/user-stories/dashboards#us-db-007)
- [US-CG-006: Close Care Gap](/user-stories/care-gaps#us-cg-006)
