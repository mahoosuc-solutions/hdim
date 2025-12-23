# Medical Assistant Guide

This guide is designed for **Medical Assistants** using the HDIM Clinical Portal to support patient care and quality improvement at the point of care.

## Overview

As a Medical Assistant, you are responsible for:
- Preparing patients for visits
- Documenting interventions and vitals
- Scheduling appointments and follow-ups
- Supporting care gap closure
- Assisting with patient outreach

---

## Your Dashboard

When you log in, you'll see the **MA Dashboard** with these widgets:

### Today's Schedule Widget
Shows today's patient appointments:
- Appointment time
- Patient name
- Visit type
- Care gap indicator

**Quick Actions:**
- Click patient → View patient details
- View care gaps for each patient

### Outreach Tasks Widget
Patient outreach assigned to you:
- Patient name and phone
- Gap type
- Due date
- Last attempt

**Quick Actions:**
- Click to log outreach attempt
- Click patient → View details

### Quick Stats Widget
Daily metrics:
- Patients seen today
- Care gaps closed
- Outreach completed
- Appointments scheduled

---

## Daily Workflow

### Start of Day (10-15 minutes)

1. **Review Schedule**
   - Check "Today's Schedule" widget
   - Note patients with care gaps (red indicator)
   - Print or note relevant gap information

2. **Check Outreach Tasks**
   - Review assigned outreach
   - Note follow-ups due today
   - Prioritize by urgency

### Patient Preparation

Before each patient visit:

1. **Review Patient Record**
   - Navigate to patient (click name in schedule)
   - Review demographics
   - Check care gaps

2. **Identify Opportunities**
   - Note screenings due
   - Prepare patient education materials
   - Alert provider to open gaps

3. **Rooming the Patient**
   - Collect vitals
   - Update demographics if needed
   - Document chief complaint

### During Patient Visit

Support the provider by:
- Documenting interventions
- Preparing supplies for procedures
- Scheduling follow-up appointments
- Providing patient education

### After Patient Visit

1. **Schedule Follow-ups**
   - Book necessary appointments
   - Schedule tests/procedures
   - Confirm patient understanding

2. **Document in Portal**
   - Log interventions
   - Close addressed care gaps
   - Update outreach status

---

## Patient Management

### Finding a Patient

1. **Quick Search**
   - Use global search bar (top of page)
   - Type patient name or MRN
   - Click result to open patient

2. **Patient List**
   - Navigate to **Patients**
   - Use filters to narrow list
   - Click patient row to view details

### Patient Detail Page

The patient page shows:

**Demographics Tab:**
- Name, DOB, gender
- Contact information
- Insurance information
- Primary provider

**Care Gaps Tab:**
- Open care gaps
- Gap urgency and type
- Recommended actions

**Clinical Tab:**
- Recent vitals
- Active conditions
- Current medications

**Evaluations Tab:**
- Evaluation history
- Compliance status

---

## Care Gap Support

### View Patient Care Gaps

1. Navigate to patient detail
2. Click **"Care Gaps"** tab
3. Review open gaps:
   - Gap type (Screening, Lab, etc.)
   - Urgency (High/Medium/Low)
   - Recommended action
   - Days overdue

### Record Intervention

When action is taken:

1. Click **"Record Intervention"** on gap
2. Select intervention type:
   - Appointment Scheduled
   - Patient Education
   - Referral Placed
   - Order Placed
3. Select outcome:
   - Action Completed
   - Follow-up Needed
   - Patient Declined
4. Add notes
5. Save

### Close Care Gap

After gap is addressed:

1. Click **"Close Gap"**
2. Select reason:
   - **Completed** - Action taken
   - **Not Applicable** - Patient excluded
   - **Patient Declined** - Patient refused
3. Add closure notes
4. Submit

::: tip
Always record an intervention before closing a gap to maintain proper documentation.
:::

---

## Scheduling

### Schedule Follow-up Appointment

1. During patient visit, determine need
2. Check provider availability
3. Book appointment in EHR/scheduling system
4. Document in Clinical Portal:
   - Record intervention
   - Note appointment date
   - Update care gap status

### Schedule Tests/Procedures

1. Provider places order
2. Contact appropriate facility
3. Schedule patient
4. Document:
   - Date scheduled
   - Facility name
   - Any prep instructions given

---

## Outreach Tasks

### View Assigned Outreach

1. Navigate to Dashboard
2. Check "Outreach Tasks" widget
3. Or: **Care Gaps** → Filter "Assigned to Me"

### Make Outreach Call

1. Review patient info first
2. Call patient
3. Verify identity (DOB, last 4 SSN)
4. Explain purpose
5. Schedule appointment if appropriate
6. Document outcome immediately

### Log Outreach Attempt

1. Click **"Log Attempt"** on task
2. Select contact method:
   - Phone Call
   - Text Message (if enabled)
3. Select outcome:

| Outcome | When to Use |
|---------|-------------|
| Reached | Spoke with patient |
| Left Message | Left voicemail |
| No Answer | No response |
| Wrong Number | Contact incorrect |
| Scheduled | Appointment booked |
| Declined | Patient refused |

4. Add notes
5. Save

### Follow-up Rules

| Attempt Result | Next Step |
|----------------|-----------|
| Not reached | Retry in 48 hours |
| Left message | Wait 48 hours, try again |
| Second failure | Try different method |
| Third failure | Escalate to RN |

---

## Running Evaluations

### Single Patient Evaluation

1. Navigate to **Evaluations**
2. Search for patient
3. Select quality measure
4. Click **"Evaluate"**
5. Note result for provider

### When to Run Evaluations

- Before patient visit (preparation)
- After new lab results arrive
- When provider requests
- After care gap addressed

---

## Common Tasks Quick Reference

### Prepare for Patient Visit

1. Open patient in portal
2. Review care gaps
3. Note items for provider
4. Prepare supplies/materials

### Process Lab Results

1. Review results in pending list
2. Alert provider to abnormals
3. Route for provider signature
4. Schedule follow-up if ordered

### Schedule Appointment

1. Check provider availability
2. Book in scheduling system
3. Document in portal
4. Give patient confirmation

### Close Care Gap

1. Verify action completed
2. Record intervention
3. Close gap with reason
4. Add documentation notes

---

## Tips for Success

### Be Proactive

1. **Review before visits** - Know gaps beforehand
2. **Prepare materials** - Have education ready
3. **Alert provider** - Note urgent gaps
4. **Document immediately** - Capture details

### Efficient Outreach

1. **Batch similar calls** - Group by gap type
2. **Call at optimal times** - 9-11 AM, 5-7 PM
3. **Be prepared** - Know patient info before calling
4. **Document immediately** - Don't wait

### Common Issues

| Issue | Solution |
|-------|----------|
| Can't find patient | Try MRN search |
| Gap won't close | Check required fields |
| No phone number | Check for alternate contact |
| System slow | Check internet, refresh page |

---

## Escalation Guidelines

### When to Escalate

**To RN:**
- Complex patient questions
- Multiple failed outreach attempts
- Patient concerns/complaints
- Scheduling conflicts

**To Provider:**
- Clinical questions
- Abnormal results
- Medication questions
- Patient requests for provider

**To Administrator:**
- System issues
- Access problems
- Data discrepancies

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+K` | Global search |
| `Esc` | Close dialog |

---

## Related Workflows

- [Care Gap Closure](/workflows/care-gap-closure)
- [Patient Outreach](/workflows/patient-outreach)
- [Daily Provider Workflow](/workflows/provider-daily)

## Related User Stories

- [US-PM-003: Search for Patient](/user-stories/patient-management#us-pm-003)
- [US-CG-008: Record Intervention](/user-stories/care-gaps#us-cg-008)
- [US-CG-006: Close Care Gap](/user-stories/care-gaps#us-cg-006)
- [US-DB-010: View Outreach Tasks](/user-stories/dashboards#us-db-010)
