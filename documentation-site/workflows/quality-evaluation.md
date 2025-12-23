# Quality Evaluation Workflow

## Overview

This workflow describes how to run quality measure evaluations for individual patients or patient populations.

## Participants

- **Primary**: Provider, RN, MA
- **Supporting**: Quality Analyst

## Trigger

- Patient visit
- Periodic quality assessment
- Compliance reporting requirement

## Time Estimate

- Single evaluation: 2-5 minutes
- Batch evaluation: 5-30 minutes (depending on size)

---

## Single Patient Evaluation

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Evaluations                                  │
│     ↓                                                        │
│  2. Select Patient                                           │
│     ↓                                                        │
│  3. Select Quality Measure                                   │
│     ↓                                                        │
│  4. Submit Evaluation                                        │
│     ↓                                                        │
│  5. View Result                                              │
│     ↓                                                        │
│  6. Take Action (if non-compliant)                           │
└─────────────────────────────────────────────────────────────┘
```

### Step 1: Navigate to Evaluations

1. Click "Evaluations" in navigation sidebar
2. Evaluations page loads
3. Evaluation form displayed

**Route**: `/evaluations`

---

### Step 2: Select Patient

1. Click patient search field
2. Type patient name or MRN
3. Select patient from autocomplete dropdown
4. Patient details displayed below

**Search Tips**:
- Partial name matching supported
- Case-insensitive search
- Minimum 2 characters to search

---

### Step 3: Select Quality Measure

1. Click measure dropdown
2. Browse or search measures
3. Filter by category (optional):
   - Preventive
   - Chronic Disease
   - Behavioral Health
   - Medication
   - Women's Health
4. Select desired measure

**Quick Access**:
- **Favorites**: Click star to use favorited measures
- **Recent**: Recently used measures shown at top

::: tip Evaluable Measures
Only measures with CQL libraries (evaluable) are shown. Measures without implementation display "Not evaluable".
:::

---

### Step 4: Submit Evaluation

1. Review patient and measure selection
2. Click "Evaluate" button
3. Loading indicator displays
4. Wait for CQL engine processing

**Processing Time**: Typically 2-5 seconds

---

### Step 5: View Result

Result displayed with outcome:

| Outcome | Badge | Meaning |
|---------|-------|---------|
| **Compliant** | 🟢 Green | Patient meets measure criteria |
| **Non-Compliant** | 🔴 Red | Patient does not meet criteria |
| **Not Eligible** | 🔵 Blue | Patient excluded from measure |

**Result Details**:
- Evaluation ID
- Measure name and version
- Calculation date
- Duration (processing time)
- Numerator/Denominator status
- Compliance rate

---

### Step 6: Take Action (if non-compliant)

If patient is non-compliant:

1. Review reason for non-compliance
2. Check care gap recommendations
3. Document intervention plan
4. Navigate to Care Gaps to track

---

## Batch Evaluation Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Patients                                     │
│     ↓                                                        │
│  2. Select Multiple Patients                                 │
│     ↓                                                        │
│  3. Click Batch Evaluation                                   │
│     ↓                                                        │
│  4. Select Measure(s)                                        │
│     ↓                                                        │
│  5. Submit Batch                                             │
│     ↓                                                        │
│  6. Monitor Progress                                         │
│     ↓                                                        │
│  7. View Batch Results                                       │
└─────────────────────────────────────────────────────────────┘
```

### Step 1: Navigate to Patients

1. Click "Patients" in navigation
2. Patient list loads

---

### Step 2: Select Multiple Patients

1. Check checkbox for each patient
2. Or click "Select All" for visible patients
3. Selection count shown in toolbar

---

### Step 3: Click Batch Evaluation

1. Click "Batch Evaluation" button
2. Batch evaluation dialog opens

---

### Step 4: Select Measure(s)

1. Choose measures to evaluate
2. Can select multiple measures
3. Enable "Detect Care Gaps" (optional)

---

### Step 5: Submit Batch

1. Review configuration
2. Click "Run Batch"
3. Batch processing starts

---

### Step 6: Monitor Progress

Real-time progress via WebSocket:

- Progress bar with percentage
- Count: X of Y patients
- Estimated time remaining
- Cancel button to abort

**Monitoring Options**:
- Stay on dialog to watch
- Navigate away (runs in background)
- View in Visualization → Live Monitor

---

### Step 7: View Batch Results

1. Summary displayed on completion:
   - Total evaluated
   - Compliant count
   - Non-compliant count
   - Not eligible count
   - Overall compliance rate

2. Click "View Details" for full results
3. Export to CSV for analysis

---

## Expected Outcomes

| Metric | Target |
|--------|--------|
| Single evaluation | Complete in <5 seconds |
| Batch evaluation | 1,000+ patients/minute |
| Result accuracy | 100% match to CQL logic |
| Care gap creation | Automatic for non-compliant |

## Troubleshooting

### Evaluation Fails

1. Check patient has required data
2. Verify measure has CQL library
3. Check network connection
4. Review error message details
5. Contact support if persists

### Unexpected Result

1. Verify patient data is current
2. Check measure version
3. Review evaluation details
4. Compare to manual calculation
5. Report discrepancy to quality team

### Batch Times Out

1. Reduce batch size
2. Check system load
3. Try during off-peak hours
4. Contact administrator

## Related User Stories

- [US-QE-005: Select Patient for Evaluation](/user-stories/quality-evaluations#us-qe-005)
- [US-QE-006: Submit Evaluation Request](/user-stories/quality-evaluations#us-qe-006)
- [US-QE-007: View Evaluation Result](/user-stories/quality-evaluations#us-qe-007)
- [US-QE-012: Run Batch Evaluation](/user-stories/quality-evaluations#us-qe-012)
