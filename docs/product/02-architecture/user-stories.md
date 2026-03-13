# HDIM — User Stories by Role

> **Purpose:** Comprehensive user story reference for every HDIM feature, organized by role and workflow phase. Each story is traced to its implementing component and tagged with implementation status.
>
> **Format:** `As a [role], I want [capability], so that [value].`
>
> **Status Tags:** ✅ Implemented | 🔄 Partial | 🔲 Planned
>
> **Last Updated:** March 13, 2026

---

## Table of Contents

1. [Provider (MD/DO/PA/NP)](#1-provider-mddonppa)
2. [Registered Nurse](#2-registered-nurse)
3. [Medical Assistant](#3-medical-assistant)
4. [Care Coordinator](#4-care-coordinator)
5. [CMO / VP Quality](#5-cmo--vp-quality)
6. [Quality Director](#6-quality-director)
7. [Quality Analyst / Evaluator](#7-quality-analyst--evaluator)
8. [IT Administrator](#8-it-administrator)
9. [Compliance Auditor](#9-compliance-auditor)
10. [Cross-Role Stories](#10-cross-role-stories)

---

## 1. Provider (MD/DO/PA/NP)

**System Role:** `CLINICIAN`
**Dashboard:** Provider Dashboard (drag-and-drop, keyboard shortcuts, guided tour)
**Component:** `provider-dashboard.component.ts` (2,446 lines)

### Daily — Patient Encounters

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| PRV-001 | As a **Provider**, I want to see today's appointment schedule with care gap indicators embedded in each time slot, so that I know which quality gaps to address before entering the room. | Schedule displays all today's appointments; each row shows patient name, time, and colored gap indicator badges (critical/high/moderate); clicking a gap opens patient detail. | ✅ | `provider-dashboard.component.ts` → Today's Schedule section |
| PRV-002 | As a **Provider**, I want to review pending lab/test results with severity-coded highlighting (critical/high/moderate/normal), so that I don't miss urgent findings. | Results section shows all pending results; severity color-coded (red/orange/yellow/green); each row shows current value, previous value, reference range, and trend indicator (↑/↓/→/NEW). | ✅ | `provider-dashboard.component.ts` → Results Awaiting Review section |
| PRV-003 | As a **Provider**, I want to close care gaps directly from my dashboard during a patient visit, so that quality improvement happens within my existing workflow — not as a separate task. | Care gap closure dialog opens from dashboard; captures clinical evidence, intervention type, and outcome; gap status updates immediately on closure; audit trail created. | ✅ | `care-gap-closure-dialog.component.ts` |
| PRV-004 | As a **Provider**, I want to see my high-priority care gaps ranked by clinical urgency, so that I address the most impactful gaps first. | High Priority Care Gaps section shows patient, gap type, clinical context, risk level (critical/high/moderate); actions: Address, View Patient. | ✅ | `provider-dashboard.component.ts` → High Priority Care Gaps section |
| PRV-005 | As a **Provider**, I want to view my patient panel's risk stratification with HCC scores, chronic conditions, and recent hospitalizations, so that I can proactively manage high-risk patients. | Risk Stratification section shows risk score badges (Critical/High/Moderate/Low counts); patient cards with risk scores, HCC scores, conditions, hospitalizations, trending indicators (improving/stable/worsening). | ✅ | `provider-dashboard.component.ts` → Risk Stratification section |
| PRV-006 | As a **Provider**, I want to take actions on results with one click — Review, Sign, View History, Contact Patient, Order Follow-Up, Refer — so that I can act immediately without navigating away. | Each result row offers action buttons; clicking triggers the appropriate workflow (sign, order, refer); confirmation toast displayed; audit logged. | ✅ | `provider-dashboard.component.ts` → Results actions |

### Daily — Dashboard Customization

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| PRV-007 | As a **Provider**, I want to drag and drop dashboard sections to reorder them based on my personal workflow preference, so that my most-used views are at the top. | All 5 dashboard sections (Schedule, Results, Gaps, Risk, Quality) are drag-reorderable; order persists across sessions; visual drag handle visible. | ✅ | `provider-dashboard.component.ts` → DragDropModule, `CdkDragDrop` |
| PRV-008 | As a **Provider**, I want keyboard shortcuts for rapid navigation between dashboard sections, so that I can work efficiently during busy clinic days. | Keyboard shortcuts service registered; shortcut dialog accessible; shortcuts customizable; shortcut hints displayed on hover. | ✅ | `keyboard-shortcuts.service.ts`, `keyboard-shortcuts-dialog.component.ts`, `shortcut-hint.directive.ts` |
| PRV-009 | As a **Provider**, I want a guided tour on my first login that walks me through each dashboard section, so that I can learn the platform without separate training. | Guided tour activates for new users; tour overlay highlights each section with contextual explanation; user can skip or complete; completion tracked. | ✅ | `guided-tour.service.ts`, `tour-overlay.component.ts` |
| PRV-010 | As a **Provider**, I want to see my quality performance relative to peers via a leaderboard, so that I have transparent, data-driven motivation to improve. | Provider leaderboard dialog shows ranked quality scores; current user highlighted; peer median visible; anonymization option available. | ✅ | `provider-leaderboard-dialog.component.ts` |

### Weekly — Quality Review

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| PRV-011 | As a **Provider**, I want to see my quality measure performance with numerator/denominator counts and target comparison, so that I know exactly how I'm tracking. | Quality Measures section shows each measure with performance bar, numerator/denominator, target line; color indicates above/below target. | ✅ | `provider-dashboard.component.ts` → Quality Measures section |
| PRV-012 | As a **Provider**, I want to view detailed patient records including demographics, conditions, medications, and care history, so that I have full clinical context during encounters. | Patient detail page shows all patient data; care gap history; medication list; encounter timeline; linked FHIR resources. | ✅ | `patient-detail.component.ts` |

---

## 2. Registered Nurse

**System Role:** `CLINICIAN` (RN UI variant)
**Dashboard:** RN Dashboard
**Component:** `rn-dashboard.component.ts` (1,075 lines)

### Daily — Shift Operations

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| RN-001 | As a **Registered Nurse**, I want to see my assigned care gaps organized by priority and category (education/medication/coordination/assessment), so that I can plan my shift effectively. | Care Gaps tab shows patient, gap type, priority badge, category label, due date, status; filterable by priority and category. | ✅ | `rn-dashboard.component.ts` → Care Gaps tab |
| RN-002 | As a **Registered Nurse**, I want to see pending outreach calls with patient name, type (call/email/letter), reason, and scheduled date, so that I can execute outreach in order. | Outreach tab shows all pending outreach items; sorted by date; status tracking (scheduled/completed/missed); inline completion action. | ✅ | `rn-dashboard.component.ts` → Outreach tab |
| RN-003 | As a **Registered Nurse**, I want to perform medication reconciliation through a dedicated workflow, so that discrepancies are caught and documented systematically. | Med Reconciliation quick action opens workflow dialog; medication list displayed; discrepancy flagging; completion tracked; audit logged. | ✅ | `medication.service.ts`, `workflow-launcher.service.ts` |
| RN-004 | As a **Registered Nurse**, I want to deliver patient education and document delivery, so that education touchpoints are tracked for quality measure compliance. | Patient Education quick action opens education workflow; topic selection; delivery method documented; completion creates audit record. | ✅ | `nurse-workflow.service.ts`, `workflow-launcher.service.ts` |
| RN-005 | As a **Registered Nurse**, I want to update patient care plans and coordinate referrals from the dashboard, so that I don't need to switch to separate systems. | Update Care Plan and Coordinate Referral quick actions available; care plan service integration; referral tracking to completion. | ✅ | `care-plan.service.ts`, `nurse-workflow.service.ts` |
| RN-006 | As a **Registered Nurse**, I want 4 summary stat cards (Care Gaps Assigned, Patient Calls Pending, Med Reconciliations, Patient Education Due) at the top of my dashboard, so that I have an instant read on today's workload. | 4 stat cards display real-time counts; counts update as tasks are completed; clicking a stat card filters the relevant tab below. | ✅ | `rn-dashboard.component.ts` → stat cards |

---

## 3. Medical Assistant

**System Role:** `CLINICIAN` (MA UI variant)
**Dashboard:** MA Dashboard
**Component:** `ma-dashboard.component.ts` (864 lines)

### Daily — Patient Flow Management

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| MA-001 | As a **Medical Assistant**, I want to see today's schedule with stat cards for Scheduled Today, Checked In, Vitals Pending, and Rooms Ready, so that I have real-time visibility into patient flow. | 4 stat cards with live counts; schedule table shows time, patient (name + MRN), task type, room, status; counts update as tasks complete. | ✅ | `ma-dashboard.component.ts` → stat cards + schedule table |
| MA-002 | As a **Medical Assistant**, I want to check in patients with a single button click on the schedule table, so that check-in is fast and documented. | Check-in button on each scheduled patient row; clicking updates status to checked-in; stat cards update immediately; timestamp recorded. | ✅ | `ma-dashboard.component.ts` → Check-in action |
| MA-003 | As a **Medical Assistant**, I want to record patient vitals from the schedule table, so that vitals capture is integrated into my workflow. | Vitals button opens vitals entry; vital signs recorded; status updates to vitals-complete; data flows to patient record. | ✅ | `ma-dashboard.component.ts` → Vitals action |
| MA-004 | As a **Medical Assistant**, I want to mark room preparation complete for each appointment, so that room readiness is tracked visually. | Room prep button on each row; clicking marks room ready; Rooms Ready stat card increments; visual indicator changes. | ✅ | `ma-dashboard.component.ts` → Room Prep action |
| MA-005 | As a **Medical Assistant**, I want to see pre-visit care gap alerts with per-patient expandable cards showing gaps, urgency, days overdue, and recommended actions, so that providers are prepared. | Pre-visit planning page shows expandable patient cards; each card lists all open care gaps with urgency badge, days overdue count, and recommended clinical action; preparation status tracked per patient. | ✅ | `pre-visit-planning.component.ts` (341 lines) |
| MA-006 | As a **Medical Assistant**, I want a preparation summary showing total patients, total care gaps, prepared count, and high urgency count, so that I know my completion status at a glance. | Summary bar at top shows 4 computed counts; prepared count increments as patients are marked ready; high urgency count highlights outstanding items. | ✅ | `pre-visit-planning.component.ts` → summary section |

---

## 4. Care Coordinator

**System Role:** `CARE_COORDINATOR`
**Primary View:** Care Gap Manager + Outreach Campaigns
**Component:** `care-gap-manager.component.ts` (1,366 lines)

### Daily — Gap Triage & Intervention

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| CC-001 | As a **Care Coordinator**, I want to filter care gaps by urgency (high/medium/low), type (screening/medication/followup/lab/assessment), and days overdue, so that I triage my workload effectively. | Filter controls present for urgency, type, overdue; patient search with debounce; paginated results; filters persist across sessions. | ✅ | `care-gap-manager.component.ts` → filter section |
| CC-002 | As a **Care Coordinator**, I want to see summary stats (total gaps, high/medium/low urgency counts, by type distribution) at the top of my view, so that I understand the scale of work. | Stats dashboard shows total gaps, urgency distribution, type breakdown; counts update in real-time as gaps are closed. | ✅ | `care-gap-manager.component.ts` → CareGapStatsDashboardComponent |
| CC-003 | As a **Care Coordinator**, I want quick-action dialogs for documenting interventions (call, email, appointment, referral, note), so that every patient touchpoint is captured immediately. | Quick action buttons on each gap row; dialog opens for selected intervention type; fields captured per type; saves to gap record; audit trail created. | ✅ | `care-gap-manager.component.ts` → Quick Action Dialogs |
| CC-004 | As a **Care Coordinator**, I want to perform bulk operations — bulk closure, bulk assignment, mass communications — on selected care gaps, so that administrative overhead is minimized. | Checkbox selection on gap rows; select-all option; bulk action dialog with operation type selection; progress indicator during execution; summary of results. | ✅ | `care-gap-manager.component.ts` → Bulk Action Dialog |
| CC-005 | As a **Care Coordinator**, I want to see intervention recommendations with ROI metrics (success rate, average cost, time to close, ROI multiplier), so that I choose the most effective intervention for each situation. | Recommendation panel shows per-intervention economics; ranked by ROI; includes success rate percentage, average dollar cost, average days to close, ROI multiplier. | ✅ | `care-gap-manager.component.ts` → Intervention Recommendations |
| CC-006 | As a **Care Coordinator**, I want to export care gap data as CSV, so that I can share reports with stakeholders who don't have platform access. | Export button generates CSV with all visible gaps and their current status, filters applied; download triggers immediately. | ✅ | `care-gap-manager.component.ts` → CSV export |

### Weekly — Outreach Campaigns

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| CC-007 | As a **Care Coordinator**, I want to create outreach campaigns by selecting target measures and intervention channels, so that I can run structured gap closure initiatives. | Campaign creation wizard with measure selection; 5 intervention types available; cost/success rate shown per channel. | ✅ | `outreach-campaigns.component.ts` (372 lines) |
| CC-008 | As a **Care Coordinator**, I want to see cost-per-unit and success rate for each outreach channel (Letter $12/32%, Provider Alert $0/48%, Coordinator Call $45/67%, SMS $2/28%, Portal Message $1/22%), so that I can optimize my outreach budget. | Intervention selector shows 5 channels with unit cost and historical success rate; total projected cost computed; estimated close rate calculated as weighted average. | ✅ | `outreach-campaigns.component.ts` → intervention config |
| CC-009 | As a **Care Coordinator**, I want to track campaign lifecycle (Draft → Scheduled → In Progress → Completed), so that I can manage multiple campaigns simultaneously. | Campaign list shows lifecycle status badge; status transitions tracked; in-progress campaigns show real-time metrics. | ✅ | `outreach-campaigns.component.ts` → campaign lifecycle |
| CC-010 | As a **Care Coordinator**, I want to export campaign data in CSV, HL7, and PDF formats, so that I can integrate with other systems and share with leadership. | Export dropdown with 3 format options; CSV for data analysis, HL7 for EHR integration, PDF for presentations. | ✅ | `outreach-campaigns.component.ts` → export |

### Weekly — Predictive Analytics

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| CC-011 | As a **Care Coordinator**, I want to see AI-predicted care gaps with risk tiers (LOW/MODERATE/HIGH/VERY_HIGH) and prediction factors, so that I can intervene before gaps manifest. | Predicted Care Gaps tab shows predicted gaps with risk tier badge; factor breakdown (Historical Pattern 40%, Appointment Adherence 25%, Medication Refills 20%, Similar Patient Behavior 15%); intervention success rate per predicted gap. | ✅ | `predictive-care-gap.service.ts`, `insights.component.ts` |
| CC-012 | As a **Care Coordinator**, I want to see population risk pyramids showing critical/high/medium/low distribution with proportional bars, so that I understand my panel's risk profile. | Risk Pyramid visualization shows 4 tiers with patient counts and proportional bar widths; clicking a tier filters to those patients. | ✅ | `insights.component.ts` (1,409 lines) → Risk Pyramid |
| CC-013 | As a **Care Coordinator**, I want AI-suggested actions (Batch Outreach, Schedule Visits, Order Labs, Medication Review, Refer Specialist, Patient Education) linked to each insight, so that I can act immediately on analytical findings. | Each insight card shows 1–3 suggested actions as buttons; clicking triggers the appropriate workflow; estimated impact displayed per action. | ✅ | `insights.component.ts` → suggested actions |

---

## 5. CMO / VP Quality

**System Role:** `QUALITY_OFFICER`, `CLINICAL_ADMIN`
**Primary View:** CMO Onboarding Scorecard
**Component:** `cmo-onboarding.component.ts`

### Daily — Executive Monitoring

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| CMO-001 | As a **CMO**, I want to see 4 executive KPI cards (Care Gap Closure Rate, High-Risk Intervention Completion, Data Freshness SLA, Compliance Evidence Completion) with trend arrows, so that I have a 30-second daily health check. | 4 KPI cards displayed with current values and trend direction (↑/↓/→); threshold alerts if any KPI drops below target. | ✅ | `cmo-onboarding.component.ts` → KPI cards |
| CMO-002 | As a **CMO**, I want governance signals showing weekly active quality users, workflow SLA adherence, last data quality audit date, and open escalations, so that I can ensure organizational engagement. | Governance Signals section shows 4 metrics with status indicators; escalation count highlighted if >0. | ✅ | `cmo-onboarding.component.ts` → Governance Signals |
| CMO-003 | As a **CMO**, I want recommended top actions (e.g., "Escalate outreach for CBP measure", "Approve metric definitions", "Review Q2 staffing plan"), so that I know exactly what decisions need my attention. | Top Actions section shows 3–5 prioritized recommendations; each with action type, description, and urgency; actionable links to relevant pages. | ✅ | `cmo-onboarding.component.ts` → Top Actions |

### Weekly — Star Rating Management

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| CMO-004 | As a **CMO**, I want to see my organization's current CMS Star Rating with domain-level breakdown, so that I understand rating composition. | Star Ratings page shows overall rating with domain scores; each domain expandable to individual measures; comparison to prior period. | ✅ | `star-ratings.component.ts` |
| CMO-005 | As a **CMO**, I want to simulate care gap closure scenarios and see projected Star Rating impact, so that I can make data-driven resource allocation decisions. | Simulation mode allows selecting gaps to close; projected rating recalculates in real-time; delta from current rating displayed; ROI estimate per simulation. | ✅ | `star-ratings.component.ts` → simulation |

### Monthly — Quality Reporting

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| CMO-006 | As a **CMO**, I want to generate quality reports with compliance trend charts and measure performance bars, so that I can present to the board without manual formatting. | Reports page shows compliance trend line chart and measure performance bar chart; export to PDF/CSV; date range selection; auto-generated narrative summaries. | ✅ | `reports.component.ts`, `dashboard.component.ts` → charts |
| CMO-007 | As a **CMO**, I want to compare measure performance side-by-side with pass rates, population sizes, and demographic breakdowns, so that I identify health equity issues. | Measure comparison page shows 2+ measures with pass rate, execution time, population, age/gender/payer breakdowns; visual comparison charts. | ✅ | `measure-comparison.component.ts`, `measure-analytics.service.ts` |

---

## 6. Quality Director

**System Role:** `QUALITY_OFFICER`
**Primary View:** Quality Measures Library
**Component:** `quality-measures.component.ts`

### Daily — Measure Management

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| QD-001 | As a **Quality Director**, I want to browse a library of 50+ HEDIS/CMS quality measures organized by 12 categories, so that I can activate the measures relevant to my organization. | Measure library shows all available measures; categories: Preventive, Chronic Disease, Behavioral Health, Medication, Women's Health, Child & Adolescent, SDOH, Utilization, Care Coordination, Overuse, Custom, and more; search and filter by category. | ✅ | `quality-measures.component.ts` |
| QD-002 | As a **Quality Director**, I want to view detailed measure specifications including CQL logic, denominator/numerator criteria, and compliance targets, so that I understand what each measure evaluates. | Measure detail page shows full specification; CQL logic preview; population criteria; historical performance; target threshold; related measures. | ✅ | `quality-measure-detail.component.ts` |
| QD-003 | As a **Quality Director**, I want to run both single-patient and batch CQL evaluations with real-time progress tracking, so that I can evaluate compliance across different population sizes. | Evaluations page offers single and bulk mode; bulk mode shows progress bar with patient count; evaluation completes and results become available for review. | ✅ | `evaluations.component.ts` (969 lines) |

### Weekly — Performance Analysis

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| QD-004 | As a **Quality Director**, I want to see per-measure analytics (pass rate, execution time, population breakdown by age/gender/payer, trends over time), so that I can identify underperforming areas. | Measure analytics dashboard shows per-measure metrics; trend charts; demographic breakdown tables; performance vs. target comparison. | ✅ | `measure-analytics.service.ts` |
| QD-005 | As a **Quality Director**, I want to manage care gap assignments (assign to coordinators, set priorities, track closure), so that gap work is distributed effectively. | Care gap assignment UI allows selecting coordinator; priority setting; deadline; assignment notification; tracking to closure. | ✅ | `care-gap-manager.component.ts` → assignment |

### Quarterly — Compliance Submission

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| QD-006 | As a **Quality Director**, I want to export QRDA-formatted quality data for CMS/HEDIS submission, so that I can submit without manual data transformation. | QRDA export generates CMS-compliant XML; includes all evaluated measures; validates against CMS schema before download. | ✅ | `qrda-export.service.ts` |
| QD-007 | As a **Quality Director**, I want to review historical Star Rating snapshots for compliance audit, so that I can demonstrate continuous improvement to regulators. | Star Ratings trend view shows historical snapshots by date; rollover shows exact values; exportable for compliance documentation. | ✅ | `star-ratings.component.ts` → historical trend |

---

## 7. Quality Analyst / Evaluator

**System Role:** `EVALUATOR`, `ANALYST`
**Primary View:** Evaluations + Custom Report Builder
**Components:** `evaluations.component.ts` (969 lines), `custom-report-builder.component.ts`

### Daily — Evaluation Execution

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| QA-001 | As a **Quality Analyst**, I want to configure evaluation parameters with AI-assisted suggestions, so that I set up evaluations correctly the first time. | Evaluation form with patient/measure selection; AI assistant suggests parameters based on measure type; validation before execution. | ✅ | `evaluations.component.ts` → AI assistant integration |
| QA-002 | As a **Quality Analyst**, I want to see a data flow visualization showing the evaluation pipeline from data ingestion through CQL processing to result storage, so that I understand how results are computed. | Data flow visualization component shows pipeline stages with data volume indicators; real-time during batch execution; static for completed evaluations. | ✅ | `evaluations.component.ts` → DataFlowVisualizationComponent |
| QA-003 | As a **Quality Analyst**, I want to favorite frequently-used measures and have my filter preferences persist across sessions, so that repeated evaluation setup is fast. | Star/favorite toggle on measures; favorites appear first in selection; filter state saved to local storage; restored on next login. | ✅ | `evaluations.component.ts` → favorites + filter persistence |
| QA-004 | As a **Quality Analyst**, I want to review evaluation results with compliance rate, outcome (compliant/non-compliant/not-eligible), and category breakdown, so that I can validate results before reporting. | Results page shows evaluation results table; sortable by date, patient, measure, outcome; compliance rate calculation; category filter. | ✅ | `results.component.ts` |

### Weekly — Reporting

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| QA-005 | As a **Quality Analyst**, I want to build custom report templates with drag-and-drop sections, so that I create standard reports once and reuse them. | Report builder allows adding/removing/reordering report sections; template save/load; scheduled generation; parameter binding. | ✅ | `custom-report-builder.component.ts`, `report-builder.service.ts`, `report-templates.service.ts` |
| QA-006 | As a **Quality Analyst**, I want to generate reports on demand or on a schedule, so that stakeholders receive reports without manual intervention. | Generate button produces report from template; scheduling interface for recurring generation; delivery notification. | ✅ | `reports.component.ts` |

---

## 8. IT Administrator

**System Role:** `ADMIN`, `SUPER_ADMIN`
**Primary View:** Admin Portal
**Portal:** `apps/admin-portal/`

### Daily — Platform Operations

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| IT-001 | As an **IT Administrator**, I want to view system health for all microservices with UP/DOWN status, so that I can identify and resolve outages quickly. | System health page shows service grid with status badges; health check polling; alert on status change. | ✅ | Admin Portal → `system-health.component.ts` |
| IT-002 | As an **IT Administrator**, I want to view real-time metrics (request rates, error rates, latency), so that I can detect performance degradation proactively. | Real-time metrics dashboard with live updating charts; configurable time ranges; threshold alerts. | ✅ | Admin Portal → `real-time-metrics.component.ts` |
| IT-003 | As an **IT Administrator**, I want to manage users (create, update, delete, assign roles, reset passwords), so that I can control platform access per organizational policy. | User management CRUD interface; role assignment from 13-role RBAC; status toggle (active/disabled); search and filter. | ✅ | Admin Portal → `users.component.ts`, Clinical Portal → `admin-users.component.ts` |
| IT-004 | As an **IT Administrator**, I want to manage tenant configurations (tenant creation, settings, feature flags), so that each customer organization is properly isolated and configured. | Tenant management interface with create/edit; feature flag toggles; isolation verification; configuration preview. | ✅ | Admin Portal → `tenants.component.ts`, Clinical Portal → `admin-tenant-settings.component.ts` |

### Weekly — Governance

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| IT-005 | As an **IT Administrator**, I want to review enhanced audit logs with search, filter, and export capabilities, so that I can investigate security events and meet compliance requirements. | Audit log viewer with full-text search; filter by user, action, resource, date range; export to CSV; pagination for large datasets. | ✅ | Admin Portal → `audit-logs-enhanced.component.ts` |
| IT-006 | As an **IT Administrator**, I want to view configuration version history with diffs, so that I can track changes and roll back if needed. | Config versions page shows version list with timestamps, authors, and change descriptions; diff view between any two versions; rollback action. | ✅ | Admin Portal → `config-versions.component.ts` |
| IT-007 | As an **IT Administrator**, I want to seed demo data for sales demonstrations and training, so that I can provision realistic environments on demand. | Demo seeding UI triggers seeding process; progress tracking; produces 200 patients, 56 care gaps, 29K+ observations; completion notification. | ✅ | Clinical Portal → `admin-demo-seeding.component.ts`, `operations.service.ts` |
| IT-008 | As an **IT Administrator**, I want to author custom CQL quality measures using the Measure Builder, so that my organization can evaluate measures not in the standard library. | Measure Builder with CQL editor; measure metadata entry; validation against FHIR profiles; test execution against sample patients; publish workflow. | ✅ | `measure-builder.component.ts`, `create-measure-page.component.ts`, `custom-measure.service.ts` |
| IT-009 | As an **IT Administrator**, I want to configure AI agents using the Agent Builder, so that I can customize assistant behavior for my organization. | Agent Builder with configuration editor; parameter tuning; test execution; deployment to production. | ✅ | `agent-builder.component.ts` |

---

## 9. Compliance Auditor

**System Role:** `AUDITOR`
**Primary View:** QA Audit Dashboard + Clinical Audit Dashboard
**Components:** `qa-audit-dashboard.component.ts`, `clinical-audit-dashboard.component.ts`, `mpi-audit-dashboard.component.ts`

### Daily — Audit Monitoring

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| AUD-001 | As a **Compliance Auditor**, I want to review all PHI access events via the QA Audit Dashboard, so that I can verify audit trail completeness for HIPAA §164.312(b). | QA Audit Dashboard shows all PHI access events; filterable by user, resource type, action, and date; 100% coverage via HTTP audit interceptor. | ✅ | `qa-audit-dashboard.component.ts`, `audit.service.ts` |
| AUD-002 | As a **Compliance Auditor**, I want to review clinical audit events (patient access, care gap modifications, evaluation executions), so that I can verify clinical data integrity. | Clinical Audit Dashboard shows clinical-specific events; action categorization; patient-level audit trail; anomaly detection. | ✅ | `clinical-audit-dashboard.component.ts` |
| AUD-003 | As a **Compliance Auditor**, I want to review MPI audit events (matching decisions, merge actions, identity resolutions), so that I can verify patient identity integrity. | MPI Audit Dashboard shows identity-related events; match quality metrics; duplicate detection results; merge history. | ✅ | `mpi-audit-dashboard.component.ts` |

### Monthly — Compliance Evidence

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| AUD-004 | As a **Compliance Auditor**, I want automatic audit trail coverage for 100% of backend API calls, so that no PHI access goes unlogged. | HTTP Audit Interceptor captures all API calls with resource type, action, user, tenant, timestamp, duration, and success/failure; fire-and-forget batching; offline resilience. | ✅ | `audit.interceptor.ts` |
| AUD-005 | As a **Compliance Auditor**, I want audit logging for all session timeouts that differentiates automatic idle timeout from explicit logout, so that I can verify HIPAA §164.312(a)(2)(iii) automatic logoff compliance. | Session timeout audit records include: reason (IDLE_TIMEOUT vs EXPLICIT_LOGOUT), idle duration in minutes, warning shown status, user ID, and timestamp. | ✅ | `app.ts` → session timeout audit |
| AUD-006 | As a **Compliance Auditor**, I want to export audit trails for specified date ranges and resource types, so that I can produce evidence for regulatory review. | Export function generates audit trail document; filterable by date range, user, resource; formats: CSV, PDF; includes metadata headers. | ✅ | `audit-log.service.ts` → export |

---

## 10. Cross-Role Stories

These stories apply to multiple user types and represent platform-wide capabilities.

### Authentication & Security

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| XR-001 | As **any user**, I want 15-minute idle session timeout with a 2-minute warning and "Stay Logged In" option, so that my session is secured per HIPAA requirements. | Activity listeners on click, keypress, mousemove, scroll; 2-minute warning dialog with countdown; "Stay Logged In" resets timer; automatic logoff after 15 min idle; audit logged. | ✅ | `app.ts` → session timeout |
| XR-002 | As **any user**, I want to log in with JWT-based authentication through the API gateway, so that my session is secure and tokens are validated at entry. | Login page → Gateway validates JWT → trusted headers injected → services authorize; multi-tenant session; role extracted from token. | ✅ | `auth.service.ts`, `login.component.ts` |
| XR-003 | As **any user**, I want to register a new tenant organization, so that my organization can begin using the platform with proper isolation. | Tenant registration form captures org details; tenant ID generated; database schema created; admin user provisioned. | ✅ | `tenant-registration.component.ts` |

### AI & Knowledge

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| XR-004 | As **any user**, I want to ask the AI assistant questions about HEDIS measures, CQL logic, and clinical quality workflows, so that I can get domain-specific help without leaving the platform. | AI assistant chat interface; context-aware responses; understands HEDIS/CQL domain; code examples; measure explanations. | ✅ | `ai-dashboard.component.ts`, `ai-assistant.service.ts` |
| XR-005 | As **any user**, I want to access a knowledge base with role-specific articles across 7 categories (getting-started, page-guides, domain-knowledge, how-to, troubleshooting, faq, advanced), so that I can self-serve learning. | Knowledge base with category navigation; role-based article visibility (clinician/admin/quality-manager/analyst); markdown rendering; code examples; related articles; helpfulness ratings. | ✅ | `knowledge-base.component.ts`, `knowledge-base.service.ts` |

### Visualization & Analytics

| ID | User Story | Acceptance Criteria | Status | Component |
|----|-----------|---------------------|--------|-----------|
| XR-006 | As **any user**, I want to view quality data through 3D visualizations (Live Batch Monitor, Quality Constellation, Flow Network, Measure Matrix), so that I can present data in engaging, executive-ready formats. | 4 visualization views under `/visualization/`; WebGL-powered rendering; interactive navigation; data refreshes in near real-time. | ✅ | `visualization-layout.component.ts`, `live-batch-monitor.component.ts`, `quality-constellation.scene.ts`, `flow-network.component.ts`, `measure-matrix.component.ts` |
| XR-007 | As **any user**, I want application errors handled gracefully with friendly messages instead of crashes, so that I can continue working even when something goes wrong. | Global error handler catches all unhandled exceptions; user sees friendly message; error logged with PHI filtering; application does NOT crash; security events audited. | ✅ | Global Error Handler |
| XR-008 | As **any user**, I want all logs filtered for PHI before output, so that patient data never appears in browser DevTools or log files. | LoggerService filters all output; ESLint `no-console` rule enforced; build fails if `console.log` detected; production builds verified PHI-free. | ✅ | `logger.service.ts`, ESLint config |

---

## Story Count Summary

| Role | Daily Stories | Weekly Stories | Monthly Stories | Quarterly Stories | Total |
|------|-------------|---------------|----------------|-------------------|-------|
| Provider | 6 | 6 | — | — | 12 |
| Registered Nurse | 6 | — | — | — | 6 |
| Medical Assistant | 6 | — | — | — | 6 |
| Care Coordinator | 6 | 4 | 3 | — | 13 |
| CMO / VP Quality | 3 | 2 | 2 | — | 7 |
| Quality Director | 3 | 2 | 2 | — | 7 |
| Quality Analyst | 4 | 2 | — | — | 6 |
| IT Administrator | 4 | 5 | — | — | 9 |
| Compliance Auditor | 3 | — | 3 | — | 6 |
| Cross-Role | — | — | — | — | 8 |
| **Total** | **41** | **21** | **10** | **—** | **80** |

**Implementation Status:** 80/80 stories tagged **✅ Implemented** (100%)

---

*This document reflects the implemented state of HDIM as of March 13, 2026. All 80 user stories are traceable to production-ready Angular components and backend services.*
