# HEDIS Quality Measure Evaluation - Demo Script

**Scenario**: HEDIS Evaluation & Care Gap Identification
**Duration**: 3-5 minutes
**Target Audience**: Healthcare Payer Quality Directors
**Value Proposition**: Automate quality measure evaluation, identify care gaps at scale, improve HEDIS scores

---

## Pre-Recording Setup

### System State
- [ ] Demo environment running
- [ ] Logged in as: `demo_evaluator@acmehealth.com`
- [ ] Tenant: "Acme Health Plan"
- [ ] Demo mode enabled
- [ ] Scenario loaded: `./demo-cli load-scenario hedis-evaluation`
- [ ] Cache warmed: Quality measures pre-loaded

### Browser Setup
- [ ] URL: `http://localhost:4200/quality-measures?demo=true`
- [ ] Resolution: 1920x1080
- [ ] Zoom: 100%
- [ ] Full screen mode (F11)
- [ ] Clear browser cache

---

## Narration Script

### INTRO (0:00 - 0:30)

**[Screen: HDIM Dashboard - Quality Measures Overview]**

> "Welcome to HealthData-in-Motion. I'm going to show you how we automate HEDIS quality measure evaluation and care gap identification for your entire attributed population.
>
> This is Acme Health Plan's dashboard. They have 5,000 attributed Medicare Advantage patients. Let's evaluate their Breast Cancer Screening measure to identify care gaps."

**Key Visuals**:
- Dashboard header showing "Acme Health Plan"
- Patient count: "5,000 Attributed Patients"
- Measure list with 6 HEDIS measures

---

## Step-by-Step Actions

### STEP 1: Navigate to Quality Measures (0:30 - 1:00)

**Action**: Click "Quality Measures" in left navigation

**Narration**:
> "Here's our quality measure library. We support all standard HEDIS measures, plus you can define custom CQL measures for specific value-based contracts.
>
> Today we'll evaluate BCS - Breast Cancer Screening - which requires women aged 50 to 74 to have had a mammogram in the past 27 months."

**Screen Elements to Highlight**:
```
┌─────────────────────────────────────────────────────┐
│ Quality Measures                                     │
├─────────────────────────────────────────────────────┤
│ Search: [                              ] Filter ▼   │
│                                                      │
│ ✓ BCS - Breast Cancer Screening             Active │
│   COL - Colorectal Cancer Screening          Active │
│   CBP - Blood Pressure Control               Active │
│   CDC - Diabetes HbA1c Control               Active │
│   EED - Eye Exam for Diabetics               Active │
│   SPC - Statin Therapy                       Active │
└─────────────────────────────────────────────────────┘
```

**Hover Tooltip** (Demo Mode):
> "Click BCS to view measure details and run evaluation"

**Pause**: 2 seconds

---

### STEP 2: View Measure Details (1:00 - 1:30)

**Action**: Click "BCS - Breast Cancer Screening"

**Narration**:
> "The measure details show the clinical logic. We're looking for women 50 to 74 who have had a mammogram - CPT codes 77067 or 77063 - in the measurement period.
>
> This measure is critically important for CMS Star Ratings. Let's run the evaluation."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ BCS - Breast Cancer Screening                       │
├─────────────────────────────────────────────────────┤
│ Measure Type: HEDIS 2025                            │
│ Steward: NCQA                                       │
│ CMS Star Rating Impact: ★★★★★ (High)                │
│                                                      │
│ Population Criteria:                                │
│ • Women aged 50-74                                  │
│ • Continuously enrolled                             │
│                                                      │
│ Numerator:                                          │
│ • Mammogram (CPT 77067, 77063)                     │
│ • Within 27 months                                  │
│                                                      │
│ Last Evaluated: Never                               │
│                                                      │
│ [Run Evaluation]  [View CQL Logic]                 │
└─────────────────────────────────────────────────────┘
```

**Pause**: 3 seconds to let viewers read

---

### STEP 3: Run Evaluation (1:30 - 2:00)

**Action**: Click "Run Evaluation" button

**Narration**:
> "Watch how fast this runs. We're evaluating 5,000 patients in real-time, checking FHIR resources for eligible procedures, and identifying gaps."

**Screen Transition**: Progress indicator appears

```
┌─────────────────────────────────────────────────────┐
│ Evaluating BCS Measure...                           │
├─────────────────────────────────────────────────────┤
│                                                      │
│     ████████████████████░░░░░░░░  67%               │
│                                                      │
│ Processing 3,350 / 5,000 patients...                │
│                                                      │
│ Estimated time remaining: 4 seconds                 │
└─────────────────────────────────────────────────────┘
```

**Wait**: 12 seconds (actual evaluation time)

**Results Screen Appears**:

```
┌─────────────────────────────────────────────────────┐
│ BCS Evaluation Results                    ✓ Complete│
├─────────────────────────────────────────────────────┤
│ Evaluation Time: 12.4 seconds                       │
│ Patients Evaluated: 5,000                           │
│                                                      │
│ ┌─────────────────────────────────────────────────┐ │
│ │  MEASURE PERFORMANCE                            │ │
│ │                                                 │ │
│ │  Denominator: 873 eligible women               │ │
│ │  Numerator: 626 compliant patients             │ │
│ │  HEDIS Rate: 71.7%                             │ │
│ │                                                 │ │
│ │  National Benchmark: 74.2%                     │ │
│ │  Gap to Benchmark: -2.5 points                 │ │
│ └─────────────────────────────────────────────────┘ │
│                                                      │
│ ┌─────────────────────────────────────────────────┐ │
│ │  CARE GAPS IDENTIFIED                           │ │
│ │                                                 │ │
│ │  247 patients need mammogram                   │ │
│ │                                                 │ │
│ │  [View Care Gap List] [Generate Outreach]      │ │
│ └─────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

**Pause**: 4 seconds

---

### STEP 4: Highlight Results (2:00 - 2:30)

**Narration**:
> "The evaluation completed in 12 seconds. We found 873 eligible women. 626 are compliant - they've had their mammogram. That's a 71.7% HEDIS rate.
>
> We're 2.5 points below the national benchmark. But here's the opportunity: We identified 247 care gaps - women who need a mammogram.
>
> If we can close just 50% of these gaps, we'd improve our HEDIS score by 2.8 points. That directly impacts your Star Rating and quality bonus payments."

**Visual Highlight** (Demo Mode):
- Animated pulse on "247 patients need mammogram"
- Tooltip: "Each care gap closed improves HEDIS score"

---

### STEP 5: Drill into Care Gap List (2:30 - 3:30)

**Action**: Click "View Care Gap List"

**Narration**:
> "Let's drill into the care gap list. Each row is a patient who needs intervention. We're showing their name, age, risk score, last visit, and outreach status.
>
> Notice we've prioritized by HCC risk score. High-risk patients get outreach first - they're more likely to have complications if we don't act.
>
> Let's look at Sarah Martinez. She's 52, overdue by 8 months, and hasn't had a visit in 14 months. Perfect candidate for outreach."

**Screen**:
```
┌──────────────────────────────────────────────────────────────────────────┐
│ BCS Care Gap List - 247 Patients                                         │
├──────────────────────────────────────────────────────────────────────────┤
│ Filter: [All]  Sort: [Risk Score ▼]    Export: [CSV] [HL7] [Outreach]   │
│                                                                           │
│ Name              Age  Risk  Last Visit   Overdue  Status                │
│ ────────────────────────────────────────────────────────────────────────│
│ Martinez, Sarah   52   1.8   14mo ago     8mo      📧 Pending Outreach  │
│ Johnson, Linda    67   2.3   6mo ago      12mo     ✓ Scheduled          │
│ Williams, Mary    59   1.2   3mo ago      4mo      📧 Pending Outreach  │
│ Davis, Patricia   71   2.7   18mo ago     18mo     ⚠ High Priority      │
│ Garcia, Maria     54   0.9   2mo ago      2mo      📧 Pending Outreach  │
│ ...                                                                       │
│                                                                           │
│ [Page 1 of 13]                                    247 total care gaps    │
└──────────────────────────────────────────────────────────────────────────┘
```

**Action**: Hover over "Martinez, Sarah" row

**Hover Tooltip**:
> "Click to view patient details and recommended interventions"

---

### STEP 6: View Patient Details (3:30 - 4:15)

**Action**: Click "Martinez, Sarah"

**Narration**:
> "Here's Sarah's patient record. She's 52, generally healthy, no chronic conditions. But she's 8 months overdue for her mammogram.
>
> The system recommends three intervention strategies:
> 1. Member outreach letter with clinic locations
> 2. Provider alert for next visit
> 3. Care coordinator follow-up call
>
> We can also see she had a preventive visit scheduled but cancelled it 6 months ago. Perfect opportunity to re-engage."

**Screen**:
```
┌─────────────────────────────────────────────────────┐
│ Sarah Martinez (DOB: 04/12/1974)              Age 52│
├─────────────────────────────────────────────────────┤
│                                                      │
│ HCC Risk Score: 1.8                                 │
│ Active Conditions: None                             │
│ Last Visit: 11/15/2024 (Preventive Care)           │
│                                                      │
│ ┌─────────────────────────────────────────────────┐ │
│ │ CARE GAP: Breast Cancer Screening (BCS)        │ │
│ │                                                 │ │
│ │ Status: OPEN                                    │ │
│ │ Overdue: 8 months                               │ │
│ │ Last Mammogram: 02/14/2023                     │ │
│ │ Next Due: 04/14/2025                           │ │
│ └─────────────────────────────────────────────────┘ │
│                                                      │
│ ┌─────────────────────────────────────────────────┐ │
│ │ RECOMMENDED INTERVENTIONS                       │ │
│ │                                                 │ │
│ │ ✉ Member Outreach Letter                       │ │
│ │   Priority: Medium | Est. Cost: $12            │ │
│ │   Success Rate: 32%                            │ │
│ │                                                 │ │
│ │ 🏥 Provider Alert                               │ │
│ │   Next visit: None scheduled                   │ │
│ │   Success Rate: 48%                            │ │
│ │                                                 │ │
│ │ 📞 Care Coordinator Call                        │ │
│ │   Priority: High | Est. Cost: $45              │ │
│ │   Success Rate: 67%                            │ │
│ │                                                 │ │
│ │ [Execute Interventions]                        │ │
│ └─────────────────────────────────────────────────┘ │
│                                                      │
│ Encounter History:                                  │
│ • 11/15/2024 - Preventive Care (cancelled)         │
│ • 08/22/2024 - Office Visit (completed)            │
│ • 02/14/2023 - Mammogram (completed)               │
└─────────────────────────────────────────────────────┘
```

**Pause**: 5 seconds

---

### STEP 7: Generate Outreach Campaign (4:15 - 4:45)

**Action**: Go back to care gap list, click "Generate Outreach"

**Narration**:
> "Now let's generate the outreach campaign for all 247 patients. The system creates personalized letters, member portalmessages, and care coordinator work lists.
>
> We can export this as HL7 messages for your outreach vendor, or as CSV for manual review. Based on historical data, we estimate a 45% close rate for this campaign - that's 111 care gaps closed, improving your BCS measure by 2.1 HEDIS points."

**Screen**:
```
┌─────────────────────────────────────────────────────┐
│ Generate Outreach Campaign - BCS Care Gaps          │
├─────────────────────────────────────────────────────┤
│                                                      │
│ Campaign Name: [BCS Q1 2026 Outreach           ]    │
│ Target Patients: 247                                │
│                                                      │
│ Intervention Mix:                                   │
│ ☑ Member Letters (247)       Est. Cost: $2,964     │
│ ☑ Provider Alerts (189)      Est. Cost: $0         │
│ ☑ Care Coordinator Calls(62) Est. Cost: $2,790     │
│                                                      │
│ Total Campaign Cost: $5,754                         │
│                                                      │
│ ┌─────────────────────────────────────────────────┐ │
│ │ PROJECTED IMPACT                                │ │
│ │                                                 │ │
│ │ Estimated Close Rate: 45%                       │ │
│ │ Care Gaps Closed: 111 patients                 │ │
│ │ HEDIS Score Improvement: +2.1 points           │ │
│ │ Star Rating Impact: 0.3 stars                  │ │
│ │ Estimated Quality Bonus: $127,000              │ │
│ │                                                 │ │
│ │ ROI: 22x ($5,754 → $127,000)                   │ │
│ └─────────────────────────────────────────────────┘ │
│                                                      │
│ Export Format:                                      │
│ ⚪ CSV  ⚫ HL7 v2.5  ⚪ PDF Report                   │
│                                                      │
│ [Generate Campaign]  [Schedule for Later]           │
└─────────────────────────────────────────────────────┘
```

**Pause**: 4 seconds

**Action**: Click "Generate Campaign"

**Screen**: Success confirmation

```
┌─────────────────────────────────────────────────────┐
│ ✓ Campaign Generated Successfully                   │
├─────────────────────────────────────────────────────┤
│                                                      │
│ BCS Q1 2026 Outreach Campaign created.              │
│                                                      │
│ • 247 member letters queued for print              │
│ • 189 provider alerts sent                         │
│ • 62 care coordinator calls assigned               │
│                                                      │
│ HL7 messages exported to: /exports/bcs-q1-2026.hl7 │
│                                                      │
│ Campaign tracking: dashboard.hdim.io/campaigns/4728 │
│                                                      │
│ [View Campaign Dashboard] [Close]                   │
└─────────────────────────────────────────────────────┘
```

---

### STEP 8: Show QRDA Export (4:45 - 5:00)

**Action**: Navigate to Reports > QRDA Export

**Narration**:
> "Finally, when it's time to submit to CMS, we generate QRDA Category I and III reports with one click. These are ready for your HEDIS vendor or direct CMS submission.
>
> That's HDIM's quality measure workflow: Evaluate in seconds, identify every care gap, generate targeted outreach, and export for CMS submission. All automated."

**Screen**:
```
┌─────────────────────────────────────────────────────┐
│ QRDA Export                                          │
├─────────────────────────────────────────────────────┤
│                                                      │
│ Measurement Period: 01/01/2025 - 12/31/2025        │
│ Reporting Period: Q1 2026                           │
│                                                      │
│ Measures to Export:                                 │
│ ☑ BCS - Breast Cancer Screening                    │
│ ☑ COL - Colorectal Cancer Screening                │
│ ☑ CBP - Blood Pressure Control                     │
│ ☑ CDC - Diabetes HbA1c Control                     │
│ ☑ EED - Eye Exam for Diabetics                     │
│ ☑ SPC - Statin Therapy                             │
│                                                      │
│ Export Format:                                      │
│ ⚫ QRDA Category I (Individual)                     │
│ ☑ QRDA Category III (Aggregate)                    │
│                                                      │
│ [Generate QRDA Reports]                             │
└─────────────────────────────────────────────────────┘
```

**Action**: Click "Generate QRDA Reports"

**Final Screen**: Download confirmation

---

## OUTRO (5:00 - 5:15)

**Narration**:
> "Questions about HDIM's quality measure capabilities? Contact us for a personalized demo with your own data."

**Screen**: Contact information slide (add in post-production)

---

## Technical Notes

### Timing Checkpoints
- 0:30 - At quality measures list
- 1:00 - Viewing BCS measure details
- 1:30 - Clicking "Run Evaluation"
- 2:00 - Results displayed
- 2:30 - Viewing care gap list
- 3:30 - Patient detail view
- 4:15 - Generate outreach screen
- 4:45 - QRDA export screen
- 5:00 - End

### Performance Requirements
- Evaluation must complete in 10-15 seconds
- Page loads must be < 1 second
- No loading spinners visible for > 2 seconds

### Visual Highlights (Demo Mode)
- Tooltips on hover
- Subtle glow on key metrics (247 care gaps, 71.7% rate, ROI 22x)
- Progress bar animation during evaluation
- Success checkmark animation on campaign generation

### Backup Plan
If evaluation takes > 20 seconds:
- Cut to "Evaluation in progress..." screen
- Show time-lapse acceleration effect
- Resume at results screen

### Common Issues
- **Slow evaluation**: Pre-warm cache before recording
- **Missing patient**: Reload scenario: `./demo-cli load-scenario hedis-evaluation`
- **UI not responding**: Clear Redis cache, restart services

---

## Post-Production Edits

### Add Graphics
- [ ] Intro title slide (0:00-0:05)
- [ ] Key metric callouts:
  - "5,000 patients evaluated in 12 seconds" at 2:00
  - "247 care gaps identified" at 2:15
  - "22x ROI" at 4:30
- [ ] Outro contact slide (5:00-5:15)

### Audio
- [ ] Normalize audio levels
- [ ] Add background music (subtle, professional)
- [ ] Sound effect on "evaluation complete" (2:00)
- [ ] Sound effect on "campaign generated" (4:45)

### Final Touches
- [ ] Color correction (ensure brand colors)
- [ ] Smooth transitions between screens
- [ ] Zoom in on small text if needed
- [ ] Export at 1080p, 30fps

---

## Success Criteria

**Demo is successful if**:
- [ ] Total duration: 3:00 - 5:00
- [ ] No technical errors visible
- [ ] All key metrics clearly shown
- [ ] Value proposition clear in first 30 seconds
- [ ] Professional, polished pacing
- [ ] Viewer can follow without confusion

---

**Last Updated**: January 3, 2026
**Script Version**: 1.0
**Reviewed By**: Sales Engineering Team
