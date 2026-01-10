# Clinical Quality Workflow - Demo Script

**Scenario**: Clinical Quality Measure Evaluation & Care Coordination
**Duration**: 4-5 minutes
**Target Audience**: Clinical Quality Teams, Nurse Managers, Practice Administrators
**Value Proposition**: Streamline clinical quality workflows, automate measure evaluation, coordinate care gaps

---

## Pre-Recording Setup

### System State
- [ ] Demo environment running (`docker compose up -d`)
- [ ] Demo mode enabled
- [ ] Scenario loaded: `curl -X POST http://localhost:8098/demo/api/v1/demo/scenarios/hedis-evaluation`
- [ ] All services healthy (gateway, fhir, cql-engine, quality-measure, care-gap)

### Browser Setup
- [ ] URL: `http://localhost:4200?demo=true`
- [ ] Resolution: 1920x1080
- [ ] Zoom: 100%
- [ ] Full screen mode (F11)
- [ ] Clear browser cache

---

## Narration Script

### INTRO (0:00 - 0:30)

**[Screen: Login Page with Demo Login Button]**

> "Welcome to the HDIM Clinical Portal. I'm going to show you how clinical staff can efficiently manage quality measures and coordinate care for their patients.
>
> Let's log in using demo mode to see a typical nurse's workflow."

**Action**: Click "Demo Login" button

**Key Visuals**:
- Demo Login button clearly visible
- Clean, professional login interface

---

## Step-by-Step Actions

### STEP 1: Explore the Dashboard (0:30 - 1:15)

**[Screen: Nurse Dashboard after login]**

**Narration**:
> "This is the Clinical Dashboard. Notice it's role-based - we're viewing as a Registered Nurse. The dashboard shows actionable metrics: care gaps assigned, patient calls pending, medication reconciliations, and patient education tasks.
>
> Let me switch to the Administrator view to see practice-wide metrics."

**Action**: Click role selector dropdown, select "Administrator"

**Screen Elements to Highlight**:
```
┌─────────────────────────────────────────────────────┐
│ Clinical Portal Dashboard                           │
├─────────────────────────────────────────────────────┤
│ View Dashboard As: [Registered Nurse ▼]            │
│                                                     │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐│
│ │ Care Gaps│ │ Calls    │ │ Med Rec  │ │ Education││
│ │ Assigned │ │ Pending  │ │          │ │ Due      ││
│ │    15    │ │    8     │ │    5     │ │    7     ││
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘│
│                                                     │
│ Quick Actions:                                      │
│ [Update Care Plan] [Patient Outreach] [Med Rec]    │
│                                                     │
│ Care Gaps | Patient Outreach                        │
│ ─────────────────────────────────────────────────  │
│ Patient         Care Gap    Priority  Due Date     │
│ John Smith      BCS         High      Jan 15       │
│ Mary Johnson    COL         Medium    Jan 20       │
└─────────────────────────────────────────────────────┘
```

**Pause**: 5 seconds

---

### STEP 2: Navigate to Patient Management (1:15 - 1:45)

**Action**: Click "Patients" in left navigation

**Narration**:
> "The Patient Management page gives you a complete view of your patient population. Notice the summary cards showing total patients, active patients, and demographic breakdowns.
>
> We also have built-in duplicate detection to maintain data quality. Let's search for a specific patient."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ Patient Management                                   │
├─────────────────────────────────────────────────────┤
│ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐        │
│ │ Total  │ │ Active │ │ Avg Age│ │ M/F    │        │
│ │ 5,234  │ │ 4,892  │ │ 54.2   │ │ 48/52% │        │
│ └────────┘ └────────┘ └────────┘ └────────┘        │
│                                                     │
│ Search: [                    ] Gender: [All ▼]     │
│                                                     │
│ [Detect Duplicates] [Clear Links]                  │
│ □ Show Master Records Only                         │
│                                                     │
│ Name           MRN        DOB        Gender Status │
│ ──────────────────────────────────────────────────│
│ Adams, Alice   MRN-001    1968-04-12  F    Active │
│ Baker, Bob     MRN-002    1975-08-23  M    Active │
│ ...                                                │
└─────────────────────────────────────────────────────┘
```

**Pause**: 3 seconds

---

### STEP 3: Run Individual Evaluation (1:45 - 2:30)

**Action**: Click "Evaluations" in left navigation

**Narration**:
> "The Evaluations page is where clinical staff run quality measure evaluations for individual patients. This is a three-step wizard: select a measure, select a patient, and view the results.
>
> Let's evaluate the Breast Cancer Screening measure for a specific patient."

**Action**:
1. Select "BCS - Breast Cancer Screening" from measure dropdown
2. Search and select a patient
3. View results card

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ Quality Measure Evaluations                          │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Step 1: Select Measure                             │
│ ┌─────────────────────────────────────────────────┐│
│ │ [BCS - Breast Cancer Screening    ▼]           ││
│ └─────────────────────────────────────────────────┘│
│                                                     │
│ Step 2: Select Patient                             │
│ ┌─────────────────────────────────────────────────┐│
│ │ Search patient by name or MRN...               ││
│ └─────────────────────────────────────────────────┘│
│                                                     │
│ Step 3: View Results                               │
│ ┌─────────────────────────────────────────────────┐│
│ │ Selected Patient: Sarah Martinez (52, F)       ││
│ │ Measure: BCS - Breast Cancer Screening         ││
│ │ Status: NON-COMPLIANT                          ││
│ │ Last Mammogram: 02/14/2023 (23 months ago)     ││
│ │ Due Date: 05/14/2025                           ││
│ └─────────────────────────────────────────────────┘│
│                                                     │
│ [Reset]                                            │
└─────────────────────────────────────────────────────┘
```

**Pause**: 4 seconds

---

### STEP 4: View Evaluation Results (2:30 - 3:00)

**Action**: Click "Results" in left navigation

**Narration**:
> "The Results page shows all historical evaluations with summary statistics. You can see compliant, non-compliant, and not eligible counts at a glance.
>
> Use the filters to focus on specific measures, date ranges, or compliance status. The export button lets you download results for reporting."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ Evaluation Results                                   │
├─────────────────────────────────────────────────────┤
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐│
│ │Compliant │ │Non-Comp  │ │Not Elig  │ │Overall   ││
│ │   626    │ │   247    │ │  4,127   │ │  71.7%   ││
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘│
│                                                     │
│ Filters:                                           │
│ Date From: [        ] To: [        ]              │
│ Measure: [All ▼]  Status: [All ▼]                 │
│ [Apply] [Reset]                    [Export ▼]     │
│                                                     │
│ Results Table:                                     │
│ Date       Patient    Measure  Category  Outcome   │
│ ──────────────────────────────────────────────────│
│ 01/05/26   Martinez   BCS      Screening  Non-Comp │
│ 01/05/26   Johnson    COL      Screening  Compliant│
│ ...                                                │
└─────────────────────────────────────────────────────┘
```

**Pause**: 3 seconds

---

### STEP 5: Generate Reports (3:00 - 3:30)

**Action**: Click "Reports" in left navigation

**Narration**:
> "The Reports page provides three report types. Patient Reports show individual quality metrics. Population Reports give practice-wide analytics. Comparative Reports track period-over-period trends.
>
> Select the report type, configure your parameters, and generate. Reports can be exported in multiple formats including QRDA III for CMS submission."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ Quality Reports                                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Quick Actions:                                      │
│ ┌───────────────┐ ┌───────────────┐ ┌─────────────┐│
│ │ Patient Report│ │Population Rpt │ │Comparative  ││
│ │ Individual    │ │ Practice-wide │ │ Trends      ││
│ │ metrics       │ │ analytics     │ │             ││
│ └───────────────┘ └───────────────┘ └─────────────┘│
│                                                     │
│ Tabs: [Generate Reports] [Saved Reports] [Templates]│
│                                                     │
│ Report Configuration:                              │
│ Report Type: [Population Report ▼]                 │
│ Date Range: [Q4 2025] to [Q1 2026]                │
│ Measures: [All Selected]                           │
│ Format: [PDF ▼]                                    │
│                                                     │
│ [Generate Report]                                  │
└─────────────────────────────────────────────────────┘
```

**Pause**: 3 seconds

---

### STEP 6: Explore Measure Builder (3:30 - 4:00)

**Action**: Click "Measure Builder" in left navigation

**Narration**:
> "For organizations with custom quality contracts, the Measure Builder lets you create your own quality measures using CQL - Clinical Quality Language.
>
> You get a full CQL editor with syntax highlighting, value set binding, and the ability to test measures against sample patients before publishing."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ Measure Builder                                      │
│ Create custom quality measures using FHIR and CQL   │
├─────────────────────────────────────────────────────┤
│                                          [New Measure]│
│                                                     │
│ Measures:                                          │
│ Name              Category    Status   Version     │
│ ──────────────────────────────────────────────────│
│ Custom-BCS-2025   Screening   Active   1.0        │
│ ACO-Diabetes      Chronic     Draft    0.9        │
│                                                     │
│ Features:                                          │
│ ┌───────────────┐ ┌───────────────┐ ┌─────────────┐│
│ │ CQL Editor   │ │ Value Sets   │ │Test/Publish ││
│ │ Syntax       │ │ Browse, bind │ │ Validate    ││
│ │ highlighting │ │ terminology  │ │ measures    ││
│ └───────────────┘ └───────────────┘ └─────────────┘│
└─────────────────────────────────────────────────────┘
```

**Pause**: 3 seconds

---

### STEP 7: Live Batch Monitor (4:00 - 4:30)

**Action**: Click "Live Monitor" in left navigation

**Narration**:
> "For bulk evaluations, the Live Monitor provides a 3D visualization of batch processing. You can select a measure library, specify the number of patients, and watch evaluations process in real-time.
>
> This is particularly useful for end-of-quarter HEDIS runs where you need to evaluate thousands of patients."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ Visualization Hub - 3D Quality Analytics            │
├─────────────────────────────────────────────────────┤
│                                                     │
│ Visualization Mode:                                │
│ ⚫ Live Batch Monitor                              │
│ ○ Quality Constellation                            │
│ ○ Evaluation Flow Network (Coming Soon)            │
│                                                     │
│ Batch Evaluation:                                  │
│ Measure Library: [HEDIS 2025 Core ▼]              │
│ Number of Patients: [100    ]                     │
│                                                     │
│ [Start Evaluation] [Test Simulation]              │
│                                                     │
│ WebSocket: ● Connected                             │
│                                                     │
│ Legend: ○ Pending  ● Processing  ✓ Success  ✗ Failed│
│                                                     │
│ ┌─────────────────────────────────────────────────┐│
│ │                                                 ││
│ │            [3D Visualization Canvas]           ││
│ │                                                 ││
│ └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

**Pause**: 4 seconds

---

### STEP 8: AI Assistant (4:30 - 4:50)

**Action**: Click "AI Assistant" in left navigation

**Narration**:
> "Finally, the AI Assistant provides intelligent insights about your clinical workflows. It can analyze usage patterns, suggest UI improvements, and help optimize your quality processes."

**Screen Elements**:
```
┌─────────────────────────────────────────────────────┐
│ AI-Powered Insights                                  │
│ Automated UI/UX improvement recommendations         │
├─────────────────────────────────────────────────────┤
│                                 [Run Analysis] [Chat]│
│                                                     │
│ Stats:                                             │
│ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐│
│ │Interacts │ │Error Rate│ │Recommen- │ │Critical  ││
│ │   1,234  │ │   2.3%   │ │  dations │ │  Issues  ││
│ │          │ │          │ │    15    │ │    2     ││
│ └──────────┘ └──────────┘ └──────────┘ └──────────┘│
│                                                     │
│ Quick Actions:                                     │
│ [Improve measure builder] [Accessibility tips]     │
│ [Performance optimization] [Testing recommendations]│
│                                                     │
│ Chat Interface:                                    │
│ ┌─────────────────────────────────────────────────┐│
│ │ How can I help with your clinical workflows?   ││
│ │                                                 ││
│ │ [Type your question...]              [Send]    ││
│ └─────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────┘
```

---

## OUTRO (4:50 - 5:00)

**Narration**:
> "That's the HDIM Clinical Portal - a complete platform for clinical quality management. From individual patient evaluations to bulk processing, custom measures to AI-powered insights.
>
> Contact us for a personalized demo with your own patient data."

**Screen**: Return to Dashboard

---

## Technical Notes

### Timing Checkpoints
- 0:00 - Login page
- 0:30 - Dashboard (Nurse view)
- 1:15 - Patient Management
- 1:45 - Evaluations page
- 2:30 - Results page
- 3:00 - Reports page
- 3:30 - Measure Builder
- 4:00 - Live Monitor
- 4:30 - AI Assistant
- 5:00 - End

### Performance Requirements
- Page loads must be < 1 second
- Role switch must be instant
- No loading spinners visible for > 2 seconds

### Visual Highlights (Demo Mode)
- Tooltips on hover for key actions
- Subtle animations on metric cards
- WebSocket status indicator in Live Monitor

### Backup Plan
If services are slow:
- Pre-load each page before recording
- Use screenshots for slow-loading sections
- Cut between pages in post-production

### Common Issues
- **502 errors**: Ensure all services are healthy
- **No data**: Load demo scenario first
- **Slow UI**: Clear Redis cache, restart services

---

## Post-Production Edits

### Add Graphics
- [ ] Intro title slide (0:00-0:05)
- [ ] Feature callouts at each step
- [ ] Outro contact slide (4:50-5:00)

### Audio
- [ ] Normalize audio levels
- [ ] Add subtle background music
- [ ] Sound effects on page transitions

### Final Touches
- [ ] Color correction (brand colors)
- [ ] Smooth transitions between pages
- [ ] Zoom in on small text if needed
- [ ] Export at 1080p, 30fps

---

## Success Criteria

**Demo is successful if**:
- [ ] Total duration: 4:00 - 5:00
- [ ] No technical errors visible
- [ ] All 9 navigation items demonstrated
- [ ] Role-based dashboard shown
- [ ] Value proposition clear in first 30 seconds
- [ ] Professional, polished pacing

---

**Last Updated**: January 5, 2026
**Script Version**: 2.0 (Updated to match current UI)
**Note**: This script reflects the actual Clinical Portal UI as of January 2026
