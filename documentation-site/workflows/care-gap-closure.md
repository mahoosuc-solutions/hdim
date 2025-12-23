# Care Gap Closure Workflow

## Overview

This workflow describes the process of identifying care gaps, documenting interventions, and closing gaps when addressed.

## Participants

- **Primary**: RN, Care Manager
- **Supporting**: Provider, MA

## Trigger

- Care gap identified from quality evaluation
- Patient visit scheduled
- Outreach campaign initiated

## Time Estimate

- Single gap closure: 3-10 minutes
- Bulk closure: 5-15 minutes

---

## Workflow Steps

### Individual Gap Closure

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Care Gaps                                    │
│     ↓                                                        │
│  2. Filter and Prioritize Gaps                               │
│     ↓                                                        │
│  3. Select Gap to Address                                    │
│     ↓                                                        │
│  4. Review Gap Details                                       │
│     ↓                                                        │
│  5. Perform Intervention                                     │
│     ↓                                                        │
│  6. Document Intervention                                    │
│     ↓                                                        │
│  7. Close Gap                                                │
│     ↓                                                        │
│  8. Verify Closure                                           │
└─────────────────────────────────────────────────────────────┘
```

#### Step 1: Navigate to Care Gaps

1. Click "Care Gaps" in navigation sidebar
2. Care Gap Manager page loads
3. All open gaps displayed in list

**Route**: `/care-gaps`

---

#### Step 2: Filter and Prioritize Gaps

1. **By Urgency**: Select High/Medium/Low
   - High (>90 days overdue)
   - Medium (31-90 days overdue)
   - Low (≤30 days overdue)

2. **By Type**: Select gap categories
   - Screening
   - Medication
   - Lab
   - Assessment
   - Follow-up

3. **By Patient**: Search by name or MRN

**Expected Outcome**: Filtered list showing relevant gaps

---

#### Step 3: Select Gap to Address

1. Review gap list
2. Click on gap row to select
3. Or click "View Details" button

**Information Displayed**:
- Patient name and MRN
- Gap type and measure
- Days overdue
- Urgency level

---

#### Step 4: Review Gap Details

1. Gap detail panel opens
2. Review:
   - Recommended action
   - Last evaluation date
   - Intervention history
   - Patient contact info

**Expected Outcome**: Understand what action is needed

---

#### Step 5: Perform Intervention

Based on gap type, perform appropriate action:

| Gap Type | Typical Intervention |
|----------|---------------------|
| Screening | Schedule procedure, order test |
| Medication | Prescription review, refill |
| Lab | Order lab work |
| Assessment | Conduct assessment |
| Follow-up | Schedule appointment |

::: tip Intervention Recording
Always record interventions before closing gaps to maintain audit trail.
:::

---

#### Step 6: Document Intervention

1. Click "Record Intervention" button
2. Select intervention type:
   - Phone Call
   - Email
   - Letter
   - Appointment Scheduled
   - Referral Placed
   - Clinical Note
3. Record outcome:
   - Reached patient
   - Left message
   - No answer
   - Appointment confirmed
4. Add notes
5. Click "Save Intervention"

**Expected Outcome**: Intervention recorded in history

---

#### Step 7: Close Gap

1. Click "Close Gap" button
2. Select closure reason:

| Reason | When to Use |
|--------|-------------|
| **Completed** | Gap fully addressed |
| **Not Applicable** | Patient excluded from measure |
| **Patient Declined** | Patient refused intervention |
| **Other** | Document custom reason |

3. Enter closure date (defaults to today)
4. Add closure notes
5. Reference document (optional)
6. Click "Submit Closure"

**Expected Outcome**: Gap marked as closed, removed from open list

---

#### Step 8: Verify Closure

1. Gap no longer appears in open list
2. Check patient's gap list to confirm
3. Verify closure appears in gap history

---

### Bulk Gap Closure

For closing multiple gaps efficiently:

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Care Gaps                                    │
│     ↓                                                        │
│  2. Filter Gaps to Close                                     │
│     ↓                                                        │
│  3. Select Multiple Gaps                                     │
│     ↓                                                        │
│  4. Click Bulk Close                                         │
│     ↓                                                        │
│  5. Confirm and Apply Reason                                 │
│     ↓                                                        │
│  6. Review Results                                           │
└─────────────────────────────────────────────────────────────┘
```

#### Step 1-2: Navigate and Filter

Same as individual closure steps 1-2.

---

#### Step 3: Select Multiple Gaps

1. Check checkbox on each gap to close
2. Or click "Select All" for visible gaps
3. Selection count displayed in toolbar

---

#### Step 4: Click Bulk Close

1. Click "Bulk Close" button in toolbar
2. Confirmation dialog appears
3. Shows count of gaps to close

---

#### Step 5: Confirm and Apply Reason

1. Select closure reason (applies to all)
2. Add notes (optional)
3. Click "Close All"
4. Progress indicator shows status

---

#### Step 6: Review Results

1. Success message with count closed
2. Any failures listed
3. Gaps removed from list

---

## Expected Outcomes

| Metric | Target |
|--------|--------|
| High urgency gaps | Addressed within 48 hours |
| Medium urgency gaps | Addressed within 7 days |
| Intervention documented | 100% before closure |
| Closure notes | Required for all closures |

## Troubleshooting

### Gap Won't Close

1. Verify all required fields completed
2. Check closure reason selected
3. Verify permissions (RN or higher)
4. Try refreshing page

### Gap Reappears After Closure

1. Check if new evaluation ran
2. Verify closure was submitted
3. Contact administrator

## Related User Stories

- [US-CG-001: View All Care Gaps](/user-stories/care-gaps#us-cg-001)
- [US-CG-006: Close Care Gap](/user-stories/care-gaps#us-cg-006)
- [US-CG-007: Bulk Close Care Gaps](/user-stories/care-gaps#us-cg-007)
- [US-CG-008: Record Intervention](/user-stories/care-gaps#us-cg-008)
