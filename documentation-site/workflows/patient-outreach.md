# Patient Outreach Workflow

## Overview

This workflow describes the process of conducting patient outreach to address care gaps and improve quality compliance.

## Participants

- **Primary**: RN, Care Manager, MA
- **Supporting**: Provider

## Trigger

- Care gap identified
- Upcoming preventive care due
- Chronic condition management
- Outreach campaign assignment

## Time Estimate

- Per patient: 5-15 minutes
- Outreach campaign: varies by population size

---

## Outreach Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  1. Review Outreach List                                     │
│     ↓                                                        │
│  2. Prepare Patient Information                              │
│     ↓                                                        │
│  3. Conduct Outreach                                         │
│     ↓                                                        │
│  4. Document Attempt                                         │
│     ↓                                                        │
│  5. Schedule Follow-up (if needed)                           │
│     ↓                                                        │
│  6. Close Gap (if resolved)                                  │
└─────────────────────────────────────────────────────────────┘
```

### Step 1: Review Outreach List

1. Navigate to your role dashboard (RN or MA)
2. Locate "Outreach Tasks" widget
3. Review pending outreach items
4. Prioritize by urgency and due date

**Outreach Types**:
- Phone call
- Email
- Letter/mail
- Patient portal message
- Text message (if enabled)

---

### Step 2: Prepare Patient Information

Before contacting patient:

1. Click patient name to view details
2. Review:
   - Demographics and contact info
   - Preferred contact method
   - Best time to reach
   - Language preference
   - Care gaps and recommended actions
3. Prepare talking points

---

### Step 3: Conduct Outreach

#### Phone Call

1. Dial patient's phone number
2. Introduce yourself and organization
3. Verify patient identity (DOB, last 4 SSN)
4. Explain purpose of call
5. Discuss care gap and recommended action
6. Schedule appointment if needed
7. Answer patient questions
8. Confirm next steps

**Script Example**:
> "Hello, this is [Name] from [Organization]. I'm calling to follow up on your healthcare needs. We noticed you're due for [screening/test]. Would you like to schedule an appointment?"

#### Email/Letter

1. Use approved template
2. Personalize with patient name
3. Clearly state care gap
4. Include call to action
5. Provide contact information
6. Send via approved channel

---

### Step 4: Document Attempt

1. Return to patient's care gap or outreach task
2. Click "Record Intervention" or "Log Attempt"
3. Select outreach type
4. Select outcome:

| Outcome | Description |
|---------|-------------|
| **Reached** | Spoke with patient |
| **Left Message** | Voicemail left |
| **No Answer** | No response, no voicemail |
| **Wrong Number** | Number incorrect |
| **Appointment Scheduled** | Visit booked |
| **Declined** | Patient refused |

5. Add detailed notes
6. Save intervention

---

### Step 5: Schedule Follow-up (if needed)

If patient not reached or action pending:

1. Create follow-up task
2. Set follow-up date
3. Assign to self or colleague
4. Add context notes

**Follow-up Guidelines**:
- First attempt failed: Follow up within 48 hours
- Second attempt failed: Try different method
- Third attempt failed: Escalate to supervisor

---

### Step 6: Close Gap (if resolved)

If outreach successful and action completed:

1. Navigate to patient's care gaps
2. Select the addressed gap
3. Click "Close Gap"
4. Select reason: "Completed"
5. Reference the scheduled appointment
6. Submit closure

---

## Outreach Best Practices

### Timing

| Time | Success Rate |
|------|-------------|
| 9-11 AM | High |
| 12-2 PM | Low (lunch) |
| 2-5 PM | Medium |
| 5-7 PM | High (after work) |

### Communication Tips

1. **Be Clear**: State purpose upfront
2. **Be Concise**: Respect patient's time
3. **Be Empathetic**: Acknowledge barriers
4. **Be Helpful**: Offer assistance with scheduling
5. **Be Professional**: Represent organization well

### Handling Common Objections

| Objection | Response |
|-----------|----------|
| "I'm too busy" | Offer flexible scheduling options |
| "I don't have insurance" | Discuss payment options, assistance programs |
| "I feel fine" | Explain preventive care importance |
| "I'll do it later" | Offer to schedule now, emphasize urgency |

---

## Expected Outcomes

| Metric | Target |
|--------|--------|
| First contact rate | >60% |
| Appointment scheduled | >40% of contacts |
| Outreach documented | 100% |
| Follow-up completed | Within 48 hours |

## Troubleshooting

### Can't Reach Patient

1. Try different times of day
2. Use alternative contact method
3. Check if contact info is current
4. Send written communication
5. Document all attempts

### Patient Declined

1. Document refusal reason
2. Provide educational materials
3. Close gap as "Patient Declined"
4. Schedule future outreach if appropriate

## Related User Stories

- [US-DB-010: View Outreach Tasks](/user-stories/dashboards#us-db-010)
- [US-CG-008: Record Intervention](/user-stories/care-gaps#us-cg-008)
- [US-CG-006: Close Care Gap](/user-stories/care-gaps#us-cg-006)
