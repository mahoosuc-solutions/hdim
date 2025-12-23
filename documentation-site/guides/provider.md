# Provider Guide

This guide is designed for **Physicians, Nurse Practitioners, and Physician Assistants** using the HDIM Clinical Portal to improve patient quality outcomes.

## Overview

As a provider, you are responsible for:
- Delivering quality care during patient visits
- Addressing care gaps at point of care
- Reviewing and signing clinical results
- Monitoring your quality measure performance

---

## Your Dashboard

When you log in, you'll see the **Provider Dashboard** with these widgets:

### Today's Schedule Widget
Shows your scheduled patients with:
- Appointment time
- Patient name
- Visit type
- Care gap indicator (red dot = has open gaps)

**Quick Actions:**
- Click patient name → View patient details
- Click care gap indicator → View patient's care gaps

### High-Priority Care Gaps Widget
Displays urgent care gaps for your patients:

| Priority | Color | Meaning |
|----------|-------|---------|
| Critical | Red | Overdue >90 days |
| High | Orange | Overdue 31-90 days |
| Moderate | Yellow | Overdue ≤30 days |

**Quick Actions:**
- Click gap row → View gap details
- Click "Address Gap" → Open closure form

### Pending Results Widget
Shows lab and imaging results awaiting your review:
- Normal results (black text)
- Abnormal results (red text, highlighted)

**Quick Actions:**
- Click result → View details
- Click "Sign" → Sign result

### Quality Measures Widget
Your performance across key measures:
- Current rate vs target
- Trend indicator (↑ ↓ →)
- Click measure for detailed breakdown

---

## Daily Workflow

### Morning Preparation (15-30 minutes)

1. **Review High-Priority Care Gaps**
   - Navigate to Dashboard
   - Check "High-Priority Care Gaps" widget
   - Note critical/high priority items for today's patients

2. **Review Pending Results**
   - Check "Pending Results" widget
   - Sign abnormal results first
   - Document follow-up actions as needed

3. **Check Quality Performance**
   - Review "Quality Measures" widget
   - Note measures below target
   - Identify improvement opportunities

### Per-Patient Preparation (2-5 minutes)

Before each patient visit:

1. **Navigate to Patient**
   - Click patient name in schedule
   - Or search in navigation bar

2. **Review Care Gaps**
   - Click "Care Gaps" tab
   - Note recommended actions
   - Plan to address during visit

3. **Review Clinical Data**
   - Check recent labs/vitals
   - Review active conditions
   - Review current medications

### During Patient Visit

1. **Address Care Gaps**
   - Perform recommended screenings
   - Order necessary tests
   - Provide patient education

2. **Document Actions**
   - Document in your EHR
   - Note interventions performed

### After Patient Visit

1. **Close Addressed Gaps**
   - Navigate to patient's care gaps
   - Click "Close Gap" for each addressed item
   - Select reason: "Completed"
   - Add documentation notes

2. **Create Follow-up Tasks**
   - For items requiring follow-up
   - Assign to RN/MA as appropriate

---

## Running Evaluations

### Single Patient Evaluation

1. Navigate to **Evaluations** page
2. Search and select patient
3. Select quality measure
4. Click **"Evaluate"**
5. Review result:
   - 🟢 Compliant - meets criteria
   - 🔴 Non-Compliant - care gap identified
   - 🔵 Not Eligible - excluded from measure

### Batch Evaluation

For multiple patients:

1. Navigate to **Patients** page
2. Select patients (checkbox)
3. Click **"Batch Evaluation"**
4. Select measure(s)
5. Click **"Run Batch"**
6. Monitor progress
7. Review results summary

---

## Managing Care Gaps

### View Your Patients' Gaps

1. Navigate to **Care Gaps**
2. Filter by "My Patients" (if available)
3. Sort by urgency

### Close a Care Gap

1. Select the gap
2. Click **"Close Gap"**
3. Select reason:
   - **Completed** - Gap fully addressed
   - **Not Applicable** - Patient excluded
   - **Patient Declined** - Patient refused
4. Add notes
5. Submit

### Record Intervention

Before closing, document your intervention:

1. Click **"Record Intervention"**
2. Select type:
   - Clinical Note
   - Order Placed
   - Referral Made
   - Patient Education
3. Add details
4. Save

---

## Key Quality Measures

Common measures you'll encounter:

### Preventive Care
- **Breast Cancer Screening** (BCS)
- **Colorectal Cancer Screening** (COL)
- **Cervical Cancer Screening** (CCS)
- **Annual Wellness Visit** (AWV)

### Chronic Disease Management
- **Diabetes: HbA1c Control** (HBD)
- **Hypertension: Blood Pressure Control** (CBP)
- **Statin Therapy for CVD** (SPC)

### Behavioral Health
- **Depression Screening** (DSF)
- **Tobacco Screening/Cessation** (TSC)
- **Alcohol Screening** (ASC)

---

## Tips for Success

### Maximize Visit Efficiency

1. **Review before visit** - Know gaps beforehand
2. **Address during visit** - Don't defer unnecessarily
3. **Document immediately** - Close gaps same day
4. **Delegate appropriately** - Use RN/MA for follow-up

### Improve Quality Scores

1. **Focus on high-impact measures** - Those with most gaps
2. **Use standing orders** - For routine screenings
3. **Educate patients** - Explain importance of care
4. **Follow up** - Ensure orders are completed

### Common Pitfalls

| Issue | Solution |
|-------|----------|
| Gap reappears after closure | Ensure documentation is complete |
| Can't find patient | Try MRN or DOB search |
| Evaluation fails | Check patient data exists |
| Results not showing | Check sync status |

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+K` | Global search |
| `Ctrl+E` | New evaluation |
| `Esc` | Close dialog |

---

## Related Workflows

- [Daily Provider Workflow](/workflows/provider-daily)
- [Care Gap Closure](/workflows/care-gap-closure)
- [Quality Evaluation](/workflows/quality-evaluation)

## Related User Stories

- [US-DB-005: View High-Priority Care Gaps](/user-stories/dashboards#us-db-005)
- [US-DB-006: View Quality Measure Performance](/user-stories/dashboards#us-db-006)
- [US-QE-006: Submit Evaluation Request](/user-stories/quality-evaluations#us-qe-006)
- [US-CG-006: Close Care Gap](/user-stories/care-gaps#us-cg-006)
