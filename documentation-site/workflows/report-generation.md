# Report Generation Workflow

## Overview

This workflow describes how to generate quality compliance reports for individuals and populations.

## Participants

- **Primary**: Quality Analyst, Administrator, Provider
- **Supporting**: Care Manager

## Trigger

- Monthly/quarterly reporting cycle
- Payer submission deadline
- Leadership request
- Quality improvement initiative

## Time Estimate

- Report generation: 1-2 minutes
- Report review: 5-15 minutes

---

## Report Types

| Report Type | Purpose | Audience |
|-------------|---------|----------|
| **Patient Report** | Individual compliance status | Provider, Care Team |
| **Population Report** | Aggregate compliance metrics | Leadership, Payers |
| **Trend Report** | Performance over time | Quality Team |
| **Comparison Report** | Benchmark comparison | Executive Team |

---

## Patient Report Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Reports                                      │
│     ↓                                                        │
│  2. Select "Generate Reports" Tab                            │
│     ↓                                                        │
│  3. Click "Patient Report"                                   │
│     ↓                                                        │
│  4. Select Patient                                           │
│     ↓                                                        │
│  5. Generate Report                                          │
│     ↓                                                        │
│  6. Review and Export                                        │
└─────────────────────────────────────────────────────────────┘
```

### Step 1: Navigate to Reports

1. Click "Reports" in navigation sidebar
2. Reports page loads
3. Two tabs available: "Generate Reports" and "Saved Reports"

**Route**: `/reports`

---

### Step 2: Select "Generate Reports" Tab

1. Click "Generate Reports" tab (default)
2. Report type cards displayed

---

### Step 3: Click "Patient Report"

1. Locate "Patient Report" card
2. Click card or "Generate" button
3. Patient selection dialog opens

---

### Step 4: Select Patient

1. Search for patient by name or MRN
2. Select patient from dropdown
3. Patient details displayed

---

### Step 5: Generate Report

1. Click "Generate Report" button
2. Processing indicator displays
3. Report generated in seconds

---

### Step 6: Review and Export

**Report Contents**:
- Patient demographics
- Overall compliance rate
- Measure-by-measure results
- Care gap summary
- Recommendations
- Trend vs previous period

**Export Options**:
- CSV: Click "Export CSV"
- Excel: Click "Export Excel"
- PDF: Click "Print/PDF"

---

## Population Report Workflow

```
┌─────────────────────────────────────────────────────────────┐
│  1. Navigate to Reports                                      │
│     ↓                                                        │
│  2. Click "Population Report"                                │
│     ↓                                                        │
│  3. Select Reporting Period                                  │
│     ↓                                                        │
│  4. Generate Report                                          │
│     ↓                                                        │
│  5. Review Results                                           │
│     ↓                                                        │
│  6. Save and Export                                          │
└─────────────────────────────────────────────────────────────┘
```

### Step 3: Select Reporting Period

1. Year dropdown displayed
2. Select reporting year
3. Default: Current year

---

### Step 5: Review Results

**Population Report Contents**:

| Section | Data |
|---------|------|
| **Summary** | Total patients, evaluations, overall compliance |
| **By Measure** | Compliance rate per quality measure |
| **By Category** | Aggregate by measure category |
| **Trends** | Comparison to previous periods |
| **Gaps** | Open care gaps summary |
| **Benchmarks** | Performance vs national/regional benchmarks |

---

## Managing Saved Reports

### View Saved Reports

1. Click "Saved Reports" tab
2. List of previously generated reports
3. Shows: Title, type, date, creator

### View Report Details

1. Click report row
2. Detail dialog opens
3. Full report displayed
4. Export options available

### Delete Report

1. Click delete icon on report row
2. Confirmation dialog appears
3. Click "Delete" to confirm
4. Report removed from list

---

## Report Scheduling (Admin)

For recurring reports:

1. Navigate to Admin → Report Scheduling
2. Click "Create Schedule"
3. Select report type
4. Configure:
   - Frequency: Daily, Weekly, Monthly
   - Day/time
   - Recipients (email)
   - Format (PDF, Excel)
5. Save schedule

---

## Expected Outcomes

| Metric | Target |
|--------|--------|
| Report generation | <30 seconds |
| Data accuracy | 100% match to evaluations |
| Export success | All formats work |
| Saved report retention | 90 days default |

## Troubleshooting

### Report Won't Generate

1. Check patient/period selection
2. Verify data exists for period
3. Try refreshing page
4. Check network connection
5. Contact support

### Export Fails

1. Check browser popup blocker
2. Try different format
3. Check available disk space
4. Try smaller date range

### Data Looks Wrong

1. Verify reporting period
2. Check evaluation dates
3. Compare to source data
4. Report discrepancy to quality team

## Related User Stories

- [US-RP-001: Generate Patient Report](/user-stories/reports#us-rp-001)
- [US-RP-002: Generate Population Report](/user-stories/reports#us-rp-002)
- [US-RP-004: View Saved Reports](/user-stories/reports#us-rp-004)
- [US-RP-007: Export Report to CSV](/user-stories/reports#us-rp-007)
