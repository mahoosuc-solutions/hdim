# Clinical Portal UI Audit Report

**Date**: January 5, 2026
**Auditor**: Claude Code Agent
**Purpose**: Compare actual Clinical Portal UI against demo script expectations

---

## Executive Summary

The Clinical Portal UI has significant differences from the demo script (`HEDIS_EVALUATION_SCRIPT.md`). The actual UI is designed as a **clinical workflow tool** focused on individual patient care coordination, while the demo script describes a **population health analytics platform** for payer quality directors.

**Key Finding**: The demo script describes functionality that does not exist in the current UI. A complete redesign of either the demo script or the UI is required.

---

## 1. Screenshots Captured

All screenshots saved to `/home/mahoosuc-solutions/projects/hdim-master/.playwright-mcp/`:

| Screenshot | Description |
|------------|-------------|
| `clinical-portal-login.png` | Login page with Demo Login button |
| `clinical-portal-dashboard.png` | Nurse Dashboard with care coordination view |
| `clinical-portal-patients.png` | Patient Management page |
| `clinical-portal-evaluations.png` | Quality Measure Evaluations - Individual patient focus |
| `clinical-portal-results.png` | Evaluation Results page |
| `clinical-portal-reports.png` | Quality Reports page |
| `clinical-portal-measure-builder.png` | Custom measure builder with CQL editor |
| `clinical-portal-live-monitor.png` | 3D Visualization Hub for batch evaluations |
| `clinical-portal-ai-assistant.png` | AI-Powered UI/UX insights tool |
| `clinical-portal-knowledge-base.png` | Documentation and help center |

---

## 2. Actual Navigation Structure

### Left Sidebar Navigation Items

| # | Nav Item | URL Path | Actual Purpose |
|---|----------|----------|----------------|
| 1 | Dashboard | `/dashboard` | Role-based clinical dashboard (Nurse/MA/Provider/Admin views) |
| 2 | Patients | `/patients` | Patient list with search, filters, duplicate detection |
| 3 | Evaluations | `/evaluations` | Submit individual patient quality measure evaluations |
| 4 | Results | `/results` | View evaluation results with filters and export |
| 5 | Reports | `/reports` | Generate Patient/Population/Comparative reports |
| 6 | Measure Builder | `/measure-builder` | Create custom quality measures with CQL editor |
| 7 | Live Monitor | `/visualization/live-monitor` | 3D batch evaluation visualization |
| 8 | AI Assistant | `/ai-assistant` | AI-powered UI/UX improvement recommendations |
| 9 | Knowledge Base | `/knowledge-base` | Documentation, guides, FAQ |

### Missing Pages (from script)

| Expected URL | Status |
|--------------|--------|
| `/quality-measures` | **DOES NOT EXIST** - Redirects to `/dashboard` |
| `/settings` | **DOES NOT EXIST** - Redirects to `/dashboard` |
| `/care-gaps` | **DOES NOT EXIST** - No dedicated page |

---

## 3. Side-by-Side Comparison: Script Step vs Actual UI

### STEP 1: Navigate to Quality Measures (Script: 0:30 - 1:00)

| Script Expects | Actual UI |
|----------------|-----------|
| URL: `/quality-measures` | URL does not exist - redirects to `/dashboard` |
| "Quality Measures" in left navigation | **NO** - Navigation shows "Evaluations" instead |
| List of 6 HEDIS measures (BCS, COL, CBP, CDC, EED, SPC) | Evaluations page has a dropdown, but shows "Loading measures..." |
| Search box with Filter dropdown | Evaluations has search, but for individual evaluations |
| Click measure to view details | Cannot click - it's a dropdown selector |

**Gap**: No dedicated Quality Measures library page exists.

---

### STEP 2: View Measure Details (Script: 1:00 - 1:30)

| Script Expects | Actual UI |
|----------------|-----------|
| Click "BCS - Breast Cancer Screening" | Measure is selected from dropdown, not clicked |
| Detailed measure card with: | **NONE OF THIS EXISTS** |
| - Measure Type, Steward, CMS Star Rating | Not shown |
| - Population Criteria (Women 50-74, enrolled) | Not shown |
| - Numerator definition | Not shown |
| - Last Evaluated date | Not shown |
| - [Run Evaluation] [View CQL Logic] buttons | Only has "Reset" button |

**Gap**: No measure detail view exists. Measures are selected from a dropdown without details.

---

### STEP 3: Run Evaluation (Script: 1:30 - 2:00)

| Script Expects | Actual UI |
|----------------|-----------|
| "Run Evaluation" button on measure details | **NO** - Evaluation is per-patient, not population |
| Evaluates 5,000 patients in real-time | **NO** - UI is individual patient-focused |
| Progress bar: "Processing 3,350 / 5,000 patients..." | No batch progress indicator |
| Results showing Denominator/Numerator/HEDIS Rate | Not on Evaluations page |

**Gap**: The Evaluations page is designed for **individual patient evaluations**, not population-level batch processing.

### Actual Evaluations Page Workflow:
1. Select a Quality Measure (dropdown)
2. Select a Patient (search/dropdown)
3. View Results

---

### STEP 4: Highlight Results (Script: 2:00 - 2:30)

| Script Expects | Actual UI |
|----------------|-----------|
| Denominator: 873 eligible women | Results page shows: Compliant/Non-Compliant/Not Eligible counts |
| Numerator: 626 compliant patients | Shows "Overall Compliance" percentage |
| HEDIS Rate: 71.7% | Similar concept exists |
| National Benchmark comparison | **NOT SHOWN** |
| "247 patients need mammogram" care gaps | Not displayed this way |
| [View Care Gap List] button | **DOES NOT EXIST** |

**Gap**: Results page exists but lacks benchmark comparisons and care gap drill-down.

---

### STEP 5: Drill into Care Gap List (Script: 2:30 - 3:30)

| Script Expects | Actual UI |
|----------------|-----------|
| URL: Dedicated care gap list page | **DOES NOT EXIST** |
| Table with 247 patients needing intervention | Dashboard shows "Care Gaps Assigned" metric only |
| Columns: Name, Age, Risk, Last Visit, Overdue, Status | Not available |
| Filter by Risk Score | Not available |
| Hover over patient for details | Not available |

**Gap**: No dedicated care gap list page. Dashboard shows care gaps assigned to nurses, but not a population care gap list.

---

### STEP 6: View Patient Details (Script: 3:30 - 4:15)

| Script Expects | Actual UI |
|----------------|-----------|
| Patient record with HCC Risk Score | Patients page shows patient list |
| Care Gap card showing overdue status | Not visible from list view |
| Recommended Interventions with ROI | **DOES NOT EXIST** |
| - Member Outreach Letter (Est. Cost: $12) | Not available |
| - Provider Alert | Not available |
| - Care Coordinator Call (Est. Cost: $45) | Not available |
| [Execute Interventions] button | Not available |

**Gap**: No patient detail view with care gaps and intervention recommendations.

---

### STEP 7: Generate Outreach Campaign (Script: 4:15 - 4:45)

| Script Expects | Actual UI |
|----------------|-----------|
| "Generate Outreach" button on care gap list | **DOES NOT EXIST** |
| Campaign configuration modal | Not available |
| - Member Letters checkbox | Not available |
| - Provider Alerts checkbox | Not available |
| - Care Coordinator Calls checkbox | Not available |
| Projected Impact section with ROI | Not available |
| Export formats: CSV, HL7 v2.5, PDF | Reports page has Export button only |

**Gap**: No outreach campaign generation functionality.

---

### STEP 8: Show QRDA Export (Script: 4:45 - 5:00)

| Script Expects | Actual UI |
|----------------|-----------|
| Navigate to Reports > QRDA Export | Reports page exists |
| Measurement Period selector | Date From/To filters exist |
| List of measures to export with checkboxes | Not shown - would need to configure |
| QRDA Category I (Individual) radio | **NOT VISIBLE** |
| QRDA Category III (Aggregate) checkbox | **NOT VISIBLE** |
| [Generate QRDA Reports] button | Export button exists but format unclear |

**Gap**: QRDA export may exist but is not prominently featured. Reports page focuses on Patient/Population/Comparative reports.

---

## 4. Features in Script That Don't Exist in UI

| Feature | Script Description | UI Status |
|---------|-------------------|-----------|
| Quality Measures Library | Dedicated page at `/quality-measures` | **MISSING** |
| Population-Level Batch Evaluation | Evaluate 5,000 patients at once | **MISSING** - Individual patient only |
| Measure Detail Cards | Show steward, Star Rating, criteria | **MISSING** |
| Care Gap List Page | Table of patients with gaps | **MISSING** |
| Patient Intervention Cards | ROI, success rates, costs | **MISSING** |
| Outreach Campaign Generator | Member letters, provider alerts | **MISSING** |
| QRDA Export UI | Category I/III selection | **UNCLEAR** |
| National Benchmark Comparison | Compare to 74.2% benchmark | **MISSING** |
| Tenant Header | "Acme Health Plan" with patient count | **PARTIAL** - Shows clinic name only |

---

## 5. Features in UI Not Mentioned in Script

| Feature | Location | Purpose |
|---------|----------|---------|
| Role-Based Dashboard | Dashboard | Switch between Nurse/MA/Provider/Admin views |
| Care Gaps Assigned (to nurse) | Dashboard | Individual nurse's assigned care gaps |
| Patient Calls Pending | Dashboard | Nurse workflow metric |
| Med Reconciliations | Dashboard | Nurse workflow metric |
| Patient Education Due | Dashboard | Nurse workflow metric |
| Quick Actions | Dashboard | Update Care Plan, Patient Outreach, Med Reconciliation, etc. |
| Duplicate Detection | Patients | Detect and link duplicate patient records |
| Measure Builder | `/measure-builder` | Create custom quality measures with CQL |
| Live Monitor | `/visualization/live-monitor` | 3D batch evaluation visualization |
| AI Assistant | `/ai-assistant` | AI-powered UI/UX recommendations |
| Knowledge Base | `/knowledge-base` | Documentation and help center |

---

## 6. Root Cause Analysis

### Design Philosophy Mismatch

| Demo Script Target | Actual UI Target |
|--------------------|------------------|
| Healthcare Payer Quality Directors | Clinical staff (Nurses, MAs, Providers) |
| Population health management | Individual patient care coordination |
| Batch evaluation of thousands | One patient at a time |
| Strategic quality improvement | Tactical daily workflows |
| HEDIS reporting for CMS | Care gap closure for individual patients |

### The Disconnect

The demo script describes a **payer-facing analytics dashboard** for:
- Evaluating entire attributed populations
- Generating HEDIS rates and benchmarks
- Creating outreach campaigns
- Exporting for CMS submission

The actual UI is a **clinical-facing care coordination tool** for:
- Nurses managing assigned care gaps
- Running evaluations one patient at a time
- Daily clinical workflows (calls, education, reconciliation)

---

## 7. Recommendations

### Option A: Update the Demo Script
Rewrite `HEDIS_EVALUATION_SCRIPT.md` to accurately reflect the current UI:
- Focus on the nurse/clinical workflow
- Demo individual patient evaluation
- Highlight Measure Builder and Live Monitor features
- Change target audience to "Clinical Quality Teams"

### Option B: Build Missing UI Features
Implement the missing functionality:
1. Create `/quality-measures` page with measure library
2. Add population-level batch evaluation
3. Build dedicated care gap list with drill-down
4. Add patient intervention recommendations with ROI
5. Create outreach campaign generator
6. Enhance QRDA export UI
7. Add national benchmark comparisons

### Option C: Create Two Portals
- Keep Clinical Portal for care coordination
- Build separate "Quality Analytics Portal" for payers

---

## 8. Technical Notes

### Backend Services Status
During testing, several backend services returned 502 (Bad Gateway):
- `/fhir/Patient?_count=100`
- `/cql-engine/api/v1/cql/libraries/active`
- `/cql-engine/evaluate/measures`
- `/quality-measure/results`
- `/quality-measure/reports`
- `/quality-measure/custom-measures`

This prevented loading actual data, but the UI structure was still visible and documented.

### Demo Mode
- Demo login works correctly
- Tenant shown: "Main Street Clinic"
- Role defaults to: "Registered Nurse"

---

## Appendix: Page-by-Page Details

### A. Dashboard (`/dashboard`)

**Header**: "Clinical Portal Dashboard"

**Role Selector**: View Dashboard As:
- Medical Assistant
- Registered Nurse (default)
- Provider
- Administrator

**Nurse Dashboard Components**:
1. Metric Cards:
   - Care Gaps Assigned: 15
   - Patient Calls Pending: 8
   - Med Reconciliations: 5
   - Patient Education Due: 7

2. Quick Actions:
   - Update Care Plan
   - Patient Outreach
   - Med Reconciliation
   - Patient Education
   - Coordinate Referral

3. Tabs:
   - Care Gaps (table with Patient, Care Gap, Priority, Category, Due Date, Actions)
   - Patient Outreach

---

### B. Patients (`/patients`)

**Header**: "Patient Management"

**Summary Cards**:
- Total Patients
- Active Patients
- Average Age
- M/F ratio
- Master Records
- Linked Duplicates

**Search/Filter**:
- Search by name or MRN
- Gender filter
- Status filter
- Age From/To
- Reset button

**Actions**:
- Detect Duplicates
- Clear Links
- Show Master Records Only checkbox

---

### C. Evaluations (`/evaluations`)

**Header**: "Quality Measure Evaluations"

**Workflow (3 steps)**:
1. Select Measure (dropdown with search)
2. Select Patient (search autocomplete)
3. View Results

**Evaluation History Table**:
- Columns: Evaluation Date, Patient ID, Measure, Category, Outcome, Compliance Rate, Actions
- Pagination controls

---

### D. Results (`/results`)

**Header**: "Evaluation Results"

**Summary Cards**:
- Compliant: 0
- Non-Compliant: 0
- Not Eligible: 0
- Overall Compliance: 0.0%

**Filters**:
- Date From/To
- Measure Type
- Status
- Apply/Reset buttons
- Export button

---

### E. Reports (`/reports`)

**Header**: "Quality Reports"

**Quick Actions**:
- Patient Report (Individual quality metrics)
- Population Report (Practice-wide analytics)
- Comparative Report (Period-over-period trends)

**Tabs**:
- Generate Reports
- Saved Reports
- Report Templates

**Report Types** (in Generate Reports tab):
1. Patient Report - Individual quality scores, care gap ID, compliance tracking
2. Population Report - Practice-wide compliance, measure summaries, YoY trends
3. Comparative Report - Period comparison, trend analysis

---

### F. Measure Builder (`/measure-builder`)

**Header**: "Measure Builder"
**Subtitle**: Create and manage custom quality measures using FHIR data and CQL

**Actions**: "New Measure" button

**Measures Table**:
- Columns: Name, Category, Status, Version, Last Modified, Actions

**Feature Cards**:
1. CQL Editor - Syntax highlighting, auto-completion, full screen
2. Value Sets - Browse, search, bind FHIR terminology
3. Test & Publish - Test against sample patients, validate, publish

---

### G. Live Monitor (`/visualization/live-monitor`)

**Header**: "Visualization Hub - 3D Quality Analytics"

**Sidebar Controls**:
- Visualization Modes: Live Batch Monitor, Quality Constellation, (Coming Soon: Evaluation Flow Network, Measure Matrix)
- Scene Transition: Preserve Camera toggle
- Controls: Reset, Full, Capture, FPS, VR (disabled)

**Batch Evaluation Controls**:
- Select Measure Library dropdown
- Number of Patients spinner (default: 100)
- Start Evaluation button
- Test Simulation button
- WebSocket status indicator

**Legend**: Pending, Processing, Success, Failed

---

### H. AI Assistant (`/ai-assistant`)

**Header**: "AI-Powered Insights"
**Subtitle**: Automated UI/UX improvement recommendations

**Actions**: Run Analysis, AI Chat

**Stats**:
- Total Interactions
- Error Rate
- Recommendations
- Critical Issues

**Chat Interface**:
- Message history
- Input textbox
- Quick action buttons: Improve measure builder, Accessibility tips, Performance optimization, Testing recommendations

---

### I. Knowledge Base (`/knowledge-base`)

**Header**: "Knowledge Base"

**Search**: Full-text search for articles

**Categories**:
- Getting Started (1 article)
- Page Guides (7 articles)
- Domain Knowledge (4 articles)
- How-To Guides (2 articles)
- Troubleshooting (1 article)
- FAQ (1 article)
- Advanced Topics (0 articles)

**Tabs**: Popular, Recently Updated, Recently Viewed

---

*Report Generated: January 5, 2026*
