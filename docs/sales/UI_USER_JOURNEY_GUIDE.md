# HDIM — UI User Journey Guide

> **Purpose:** Map every HDIM user type through their daily and episodic lifecycle, showing what they see, what they do, and the value they receive — all backed by implemented, production-ready features.
>
> **Audience:** Sales teams, prospects, implementation partners, and clinical stakeholders evaluating HDIM for value-based care operations.
>
> **Last Updated:** March 13, 2026

---

## Table of Contents

1. [Persona-to-Role Map](#1-persona-to-role-map)
2. [Journey Maps by Persona](#2-journey-maps-by-persona)
   - [CMO / VP Quality](#21-cmo--vp-quality)
   - [Quality Director](#22-quality-director)
   - [Care Coordinator](#23-care-coordinator)
   - [Provider (MD/DO/PA/NP)](#24-provider-mddonppa)
   - [Registered Nurse](#25-registered-nurse)
   - [Medical Assistant](#26-medical-assistant)
   - [Quality Analyst / Evaluator](#27-quality-analyst--evaluator)
   - [IT Administrator](#28-it-administrator)
   - [Compliance Auditor](#29-compliance-auditor)
3. [Feature Access Matrix](#3-feature-access-matrix)
4. [Value Summary by Role](#4-value-summary-by-role)
5. [Appendix: Technical References](#5-appendix-technical-references)

---

## 1. Persona-to-Role Map

HDIM implements a **13-role RBAC system** with **31 granular permissions** across 8 categories. The table below maps real-world healthcare personas to their system roles, default landing experience, and primary value drivers.

| # | Persona | Title Examples | System Role(s) | Default View | Primary Features | Key Metric |
|---|---------|---------------|----------------|-------------|-----------------|------------|
| 1 | **CMO / VP Quality** | Chief Medical Officer, VP Quality, Quality Director | `QUALITY_OFFICER`, `CLINICAL_ADMIN` | CMO Onboarding Scorecard | Executive KPIs, Star Ratings, Governance Signals, Quality Reports | Star Rating trajectory & gap closure rate |
| 2 | **Quality Director** | HEDIS Director, Quality Program Manager | `QUALITY_OFFICER` | Quality Measures | Measure Library, Evaluations, Star Ratings, Measure Comparison, Reports | Measure compliance % vs targets |
| 3 | **Care Coordinator** | Care Manager, Patient Navigator, Population Health Manager | `CARE_COORDINATOR` | Care Gap Manager | Care Gap Triage, Outreach Campaigns, Risk Stratification, Predictive Gaps | Gaps closed per week & outreach ROI |
| 4 | **Provider** | Physician, NP, PA, DO | `CLINICIAN` | Provider Dashboard | Schedule + Care Gaps, Results Review, Risk Stratification, Gap Closure | Quality score & gaps addressed per visit |
| 5 | **Registered Nurse** | RN, BSN, Care Management Nurse | `CLINICIAN` | RN Dashboard | Care Gap Coordination, Outreach, Med Reconciliation, Patient Education | Outreach completion rate |
| 6 | **Medical Assistant** | MA, Clinical Support Staff | `CLINICIAN` | MA Dashboard | Check-in, Vitals, Room Prep, Pre-Visit Gap Alerts | Pre-visit task completion rate |
| 7 | **Quality Analyst** | Data Analyst, Evaluation Specialist, BI Analyst | `EVALUATOR`, `ANALYST` | Evaluations | CQL Evaluation, Results, Custom Report Builder, Measure Analytics | Evaluations processed & report turnaround |
| 8 | **IT Administrator** | System Admin, Tenant Admin, Platform Engineer | `ADMIN`, `SUPER_ADMIN` | Admin Dashboard | User Management, Tenant Settings, System Health, Config Versions | System uptime & tenant provisioning time |
| 9 | **Compliance Auditor** | Compliance Officer, Security Auditor, Privacy Officer | `AUDITOR` | QA Audit Dashboard | Audit Logs, Clinical Audit, MPI Audit, Compliance Evidence | Audit trail completeness & HIPAA evidence |

---

## 2. Journey Maps by Persona

Each journey map shows the lifecycle stages a user moves through — from initial onboarding through daily operations, periodic reviews, and strategic milestones. Every feature referenced is **implemented and production-ready** in the HDIM Clinical Portal.

---

### 2.1 CMO / VP Quality

**Goal:** Achieve and maintain target CMS Star Ratings through data-driven quality improvement.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Onboarding** | Day 1–5 | Review organizational quality baseline, set targets, understand platform capabilities | CMO Onboarding Scorecard, Knowledge Base | All KPIs populated with current data; governance signals green | Time-to-insight: hours instead of weeks |
| **Morning Check** | Daily (5 min) | Scan executive KPI cards: gap closure rate, intervention completion, data freshness, compliance evidence | CMO Onboarding Scorecard → Governance Signals | All 4 KPIs trending positively; no red governance signals | Early warning on drift before it compounds |
| **Star Rating Review** | Weekly | Review current Star Rating projection, simulate gap closure scenarios, compare domain performance | Star Ratings, Measure Comparison | Projected rating meets bonus threshold; simulation shows achievable path | Quantified revenue impact of each half-star improvement |
| **Quality Committee** | Monthly | Generate compliance reports, review measure trends, present to board | Reports, Custom Report Builder, QRDA Export | Reports generated in <5 min; all measures above minimum threshold | Eliminates 40+ hours of manual report compilation per quarter |
| **CMS Submission** | Quarterly / Annual | Export submission-ready quality data, verify completeness, sign off | Reports → QRDA Export, Star Ratings → Historical Trend | 100% measure coverage; no data gaps flagged | Confidence in submission accuracy; reduced rework |

**Differentiating Experience:**
- **Executive KPI cards** show gap closure rate (68%), intervention completion (74%), data freshness SLA (99.1%), and compliance evidence (92%) — each with trend arrows
- **Governance signals** surface weekly active quality users, workflow SLA adherence, last data quality audit date, and open escalations
- **Top Actions** recommend specific next steps: "Escalate outreach for CBP measure", "Approve metric definitions", "Increase staffing for Q2 push"

---

### 2.2 Quality Director

**Goal:** Ensure all HEDIS/CMS quality measures meet compliance targets through systematic evaluation and gap closure.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Measure Setup** | Onboarding | Configure active quality measures, set compliance targets, define evaluation schedules | Quality Measures Library (50+ HEDIS/CMS measures across 12 categories) | All relevant measures activated with target thresholds | Purpose-built measure library eliminates manual CQL authoring |
| **Batch Evaluation** | Daily / Weekly | Run CQL evaluations across patient populations, review results | Evaluations (single + bulk mode), Results | Evaluation completes without errors; compliance rates calculated | Automated evaluation replaces days of manual chart review |
| **Performance Tracking** | Daily | Compare measure-by-measure performance against targets, identify lagging measures | Measure Comparison, Star Ratings | All measures trending toward target; lagging measures identified | Real-time visibility vs. quarterly retrospective surprises |
| **Gap Prioritization** | Weekly | Review care gap distribution, assign priorities, plan interventions | Care Gap Manager, Population Insights | Gaps prioritized by clinical impact and Star Rating contribution | AI-driven prioritization maximizes quality improvement per dollar |
| **Trend Analysis** | Monthly | Analyze performance trends over time, identify seasonal patterns, forecast compliance | Reports, Custom Report Builder, Measure Analytics | Month-over-month improvement documented; forecasts within 5% | Data-driven resource allocation for next measurement period |
| **Compliance Reporting** | Quarterly | Generate HEDIS/CMS submission packages, validate completeness | Reports → QRDA Export, Star Ratings → Domain Breakdown | Submission-ready exports with zero manual corrections | Submission prep reduced from 2 weeks to 2 days |

**Differentiating Experience:**
- **12 measure categories**: Preventive, Chronic Disease, Behavioral Health, Medication, Women's Health, Child & Adolescent, SDOH, Utilization, Care Coordination, Overuse, Custom, and more
- **Measure comparison** shows side-by-side performance: pass rate, execution time, population coverage, age/gender/payer breakdowns
- **AI assistant** embedded in evaluation workflow suggests evaluation parameters and explains results

---

### 2.3 Care Coordinator

**Goal:** Close the maximum number of care gaps through efficient, ROI-optimized outreach campaigns.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Gap Triage** | Daily AM | Review assigned care gaps by urgency, filter by type, plan today's outreach | Care Gap Manager (1,366-line component with full triage UI) | Today's priority gaps identified; outreach plan set | Intelligent triage replaces manual spreadsheet sorting |
| **Patient Outreach** | Daily | Execute interventions: calls, emails, appointment scheduling, referrals | Care Gap Manager → Quick Action Dialogs (call/email/appointment/referral/note) | 15–20 patient contacts completed; interventions documented | One-click intervention launch with auto-documentation |
| **Bulk Operations** | Daily | Process batch gap closures, send mass communications | Care Gap Manager → Bulk Action Dialog, CSV Export | Bulk operations completed in <5 min vs. hours manually | 10x throughput on administrative gap management |
| **Campaign Management** | Weekly | Create and monitor outreach campaigns with ROI projections | Outreach Campaigns (5 intervention types with cost/success data) | Campaign launched; projected close rate meets target | Built-in ROI calculator: Letter $12/32% vs. Coordinator Call $45/67% |
| **Risk Review** | Weekly | Review population risk stratification, identify rising-risk patients | Risk Stratification (configurable risk models, SDOH factors) | Rising-risk patients flagged for proactive outreach | Prevent costly events through early intervention |
| **Predictive Analysis** | Weekly | Review AI-predicted care gaps before they manifest | Population Insights → Predicted Care Gaps | Predicted gaps addressed before becoming actual gaps | ML-based prediction: Historical 40%, Adherence 25%, Refills 20%, Peer 15% |
| **Performance Review** | Monthly | Analyze gap closure rates, outreach ROI, campaign effectiveness | Reports, Care Gap Manager → Stats Dashboard | Month-over-month improvement; ROI-positive campaigns | Quantified proof of team effectiveness for leadership |

**Differentiating Experience:**
- **ROI-driven intervention recommendations**: Each intervention type shows success rate, average cost, time to close, and ROI multiplier
- **5 intervention channels** with per-unit economics: Letter ($12, 32%), Provider Alert ($0, 48%), Care Coordinator Call ($45, 67%), SMS ($2, 28%), Patient Portal ($1, 22%)
- **Predictive care gaps** use weighted factor model to surface gaps 30–90 days before they manifest

---

### 2.4 Provider (MD/DO/NP/PA)

**Goal:** Address quality gaps during patient encounters without adding workflow burden.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Morning Huddle** | Daily AM (5 min) | Review today's schedule with embedded care gap indicators, scan critical alerts | Provider Dashboard → Today's Schedule (timeline view with gap indicators) | All appointments reviewed; high-priority gaps noted | Zero-prep quality awareness — gaps surfaced automatically |
| **Results Review** | Daily (10 min) | Review pending lab/test results with severity highlighting, sign off or order follow-ups | Provider Dashboard → Results Awaiting Review (critical/high/moderate/normal severity) | All critical results reviewed; follow-up actions ordered | Severity-coded results prevent missed critical findings |
| **Patient Encounters** | Per visit | View patient care gaps during visit, close gaps through clinical actions, document | Patient Detail → Care Gaps, Care Gap Closure Dialog | Addressable gaps closed during visit; documentation complete | In-visit gap closure: no separate workflow needed |
| **Quality Check** | Weekly (5 min) | Check personal quality score, review measure performance vs. targets | Provider Dashboard → Quality Measures section, Measure Comparison | Quality score stable or improving; all measures at or above target | Self-service quality tracking without waiting for quarterly reports |
| **Risk Panel Review** | Weekly | Review high-risk patients, identify trending-worsening patients | Provider Dashboard → Risk Stratification (risk scores, HCC, hospitalizations) | Worsening patients identified; proactive orders placed | Risk pyramid highlights actionable patients |
| **Peer Comparison** | Monthly | Review quality performance relative to peers (leaderboard) | Provider Dashboard → Provider Leaderboard | Performance at or above peer median | Transparent, data-driven quality culture |

**Differentiating Experience:**
- **Drag-and-drop dashboard sections**: Providers customize their view by reordering Today's Schedule, Results, Care Gaps, Risk Stratification, and Quality Measures
- **Keyboard shortcuts** for rapid navigation (customizable, with shortcut dialog)
- **Guided tour** for new providers with contextual onboarding
- **Care Gap Closure Dialog** allows closing gaps directly from the dashboard with clinical documentation
- **Results review** includes trend indicators (↑/↓/→/NEW), current vs. previous values, reference ranges, and one-click actions: Review, Sign, View History, Contact Patient, Order Follow-Up, Refer

---

### 2.5 Registered Nurse

**Goal:** Coordinate care gap closure through patient outreach, education, and medication management.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Shift Start** | Daily AM | Review assigned care gaps by priority, check pending outreach calls, scan med reconciliations due | RN Dashboard → Care Gaps Assigned, Patient Calls Pending, Med Reconciliations, Patient Education Due | Today's priorities identified from 4 metric cards | Purpose-built nursing workflow replaces generic task lists |
| **Care Gap Work** | Daily | Address care gaps: provide education, coordinate referrals, assess patients | RN Dashboard → Care Gaps Tab (patient, gap type, priority, category, due date) | Assigned gaps addressed; appropriate interventions documented | Category-specific actions: education, medication, coordination, assessment |
| **Patient Outreach** | Daily | Make scheduled outreach calls, document outcomes | RN Dashboard → Outreach Tab (call/email/letter with status tracking) | Scheduled outreach completed; outcomes recorded | Structured outreach tracking with scheduled/completed/missed states |
| **Med Reconciliation** | Per patient | Reconcile medications for assigned patients | RN Dashboard → Quick Actions → Med Reconciliation | Reconciliations completed accurately; discrepancies flagged | Integrated workflow reduces separate EHR login |
| **Patient Education** | Per encounter | Deliver condition-specific education materials | RN Dashboard → Quick Actions → Patient Education | Education delivered and documented; patient understanding confirmed | One-click education delivery with documentation trail |
| **Care Plan Updates** | Weekly | Review and update patient care plans, coordinate referrals | RN Dashboard → Quick Actions → Update Care Plan, Coordinate Referral | Care plans current; referrals tracked to completion | Centralized care plan management across all assigned patients |

**Differentiating Experience:**
- **4 dedicated stat cards**: Care Gaps Assigned, Patient Calls Pending, Med Reconciliations Due, Patient Education Due
- **5 quick action buttons**: Update Care Plan, Patient Outreach, Med Reconciliation, Patient Education, Coordinate Referral
- **Tabbed workflow**: Care Gaps tab and Outreach tab provide focused views with inline actions
- Services: NurseWorkflowService, MedicationService, CarePlanService, WorkflowLauncherService

---

### 2.6 Medical Assistant

**Goal:** Ensure efficient patient flow through check-in, vitals, room prep, and pre-visit gap alerts.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Day Start** | Daily AM | Review today's schedule, check room readiness, identify pre-visit tasks | MA Dashboard → Scheduled Today, Checked In, Vitals Pending, Rooms Ready | Schedule reviewed; rooms prepped; pre-visit tasks started | 4 real-time stat cards eliminate whiteboard tracking |
| **Patient Check-in** | Per patient | Check in patients, verify demographics, note care gap alerts | MA Dashboard → Today's Schedule → Check-in Button | Patient checked in; demographics current; gaps flagged for provider | Gap alerts at check-in ensure provider sees quality opportunities |
| **Vitals Recording** | Per patient | Record vital signs for each patient | MA Dashboard → Vitals Button | Vitals recorded within 5 min of check-in | In-workflow vitals capture without context switching |
| **Room Preparation** | Per patient | Prepare exam room, stage supplies based on appointment type | MA Dashboard → Room Prep Button | Room ready before provider enters | Appointment-type-aware prep lists |
| **Pre-Visit Gaps** | Per patient | Review and stage pre-visit care gap alerts for the provider | Pre-Visit Planning (30-day lookahead with per-patient gap cards) | All care gaps surfaced before provider encounter | Proactive gap staging increases in-visit closure rates |

**Differentiating Experience:**
- **Task-driven schedule table**: Time, Patient (name + MRN), Task Type (check-in/vitals/prep), Room, Status, with inline action buttons
- **Pre-visit planning** shows per-patient expandable cards with: name, MRN, DOB, age, appointment time, primary conditions, and all open care gaps with urgency/days overdue/recommended actions
- **Preparation tracking**: Visual completion status per patient with summary counts (total patients, total care gaps, prepared count, high urgency count)

---

### 2.7 Quality Analyst / Evaluator

**Goal:** Execute quality measure evaluations accurately and deliver actionable reports on schedule.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Evaluation Setup** | Per request | Configure evaluation parameters: select measures, patient populations, date ranges | Evaluations (12 measure categories, single + bulk mode, AI assistant) | Evaluation configured correctly; parameters validated | 12-category measure library with AI-assisted parameter selection |
| **Batch Execution** | Daily / Weekly | Run CQL evaluations across populations, monitor progress | Evaluations → Bulk Mode with progress tracking, Data Flow Visualization | All evaluations complete without errors; results available for review | Bulk mode processes thousands of patients with real-time progress |
| **Results Analysis** | Per evaluation | Review compliance rates, identify non-compliant populations, drill into details | Results (date, patient, measure, category, outcome, compliance rate) | Results validated; anomalies investigated | Filterable results with category breakdown and compliance metrics |
| **Custom Reporting** | Weekly / Monthly | Build custom report templates, generate scheduled reports | Custom Report Builder, Reports | Reports delivered on time; stakeholder requirements met | Template-based reporting eliminates repetitive manual work |
| **Measure Analytics** | Monthly | Analyze per-measure performance: pass rates, execution times, population breakdowns | Measure Analytics (age/gender/payer breakdowns, trends over time) | Performance trends documented; improvement areas identified | Granular analytics by demographic reveal health equity patterns |
| **Data Export** | Quarterly | Export QRDA-formatted data for CMS/HEDIS submission | Reports → QRDA Export | Export validates against CMS requirements; no correction needed | Submission-ready exports from a single click |

**Differentiating Experience:**
- **Data flow visualization** shows the real-time evaluation pipeline from data ingestion through CQL processing to result storage
- **Measure favorites** for quick access to frequently-evaluated measures
- **Filter persistence** maintains analyst's preferred view across sessions
- **AI assistant integration** suggests evaluation parameters and explains complex results

---

### 2.8 IT Administrator

**Goal:** Ensure platform reliability, manage user access, and maintain tenant configurations.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **System Check** | Daily (5 min) | Monitor system health, check service availability, review real-time metrics | Admin Portal → System Health, Real-Time Metrics | All services UP; no degraded performance; metrics within bounds | Single-pane system health with real-time observability |
| **User Management** | As needed | Create users, assign roles, manage permissions, handle access requests | Admin Portal → Users (CRUD + role assignment) | Users provisioned within SLA; roles match job function | 13-role RBAC with 31 granular permissions — no over-provisioning |
| **Tenant Configuration** | As needed | Configure tenant settings, manage feature flags, adjust thresholds | Admin Portal → Tenants, Tenant Settings | Tenant isolated; features configured per contract | Multi-tenant isolation enforced at database level |
| **Audit Review** | Weekly | Review audit logs, investigate security events, verify compliance | Admin Portal → Audit Logs (enhanced with search/filter) | No unauthorized access; all PHI access logged | 100% API call audit coverage via HTTP interceptor (HIPAA §164.312(b)) |
| **Configuration Management** | Monthly | Review config versions, plan upgrades, manage integration settings | Admin Portal → Config Versions | Configurations versioned; rollback capability verified | Version-controlled config with diff and rollback |
| **Demo / Sandbox** | As needed | Seed demo data, manage sandbox environments for sales/training | Clinical Portal → Admin → Demo Seeding, Deployment Console | Demo environment provisioned in <20 min | Self-service demo provisioning with 200 patients, 56 care gaps |

**Differentiating Experience:**
- **Admin Portal** is a separate Angular application with dedicated routes for operational concerns
- **Real-time metrics** dashboard provides live system observability
- **Roadmap view** shows platform evolution timeline
- **Investor portal** with separate auth flow for stakeholder access

---

### 2.9 Compliance Auditor

**Goal:** Verify HIPAA compliance, audit PHI access patterns, and produce evidence for regulatory review.

| Stage | Frequency | What They Do | Features Used | Success Criteria | Value Delivered |
|-------|-----------|-------------|---------------|-----------------|-----------------|
| **Audit Trail Review** | Daily / Weekly | Review PHI access logs, identify anomalies, verify authorized access | QA Audit Dashboard, Clinical Audit Dashboard | All PHI access is authorized and documented; no anomalies | 100% HTTP audit coverage — every API call logged automatically |
| **Compliance Verification** | Monthly | Verify HIPAA controls, check session timeout enforcement, validate cache TTLs | Compliance Dashboard, Audit Logs | All HIPAA §164.312 controls verified; evidence documented | Built-in compliance verification against specific HIPAA sections |
| **MPI Audit** | Monthly | Review Master Patient Index integrity, identify duplicates, verify matching | MPI Audit Dashboard | MPI accuracy verified; duplicate rate within threshold | Dedicated MPI audit UI for data stewardship |
| **Evidence Collection** | Quarterly | Generate compliance evidence bundles, export audit trails, prepare for review | Audit Logs → Export, Reports → Compliance Reports | Evidence bundle complete for all required controls | Automated evidence generation replaces manual screenshot collection |

**Differentiating Experience:**
- **3 specialized audit dashboards**: QA Audit (quality process), Clinical Audit (PHI access), MPI Audit (data integrity)
- **Automatic audit interceptor** logs every backend API call with resource type, action, user, tenant, timestamp, and duration
- **Session timeout audit logging** tracks automatic logoff vs. explicit logout with idle duration (HIPAA §164.312(a)(2)(iii))

---

## 3. Feature Access Matrix

The matrix below maps every clinical portal page to the roles that can access it, showing what each user type can do in HDIM. Access is enforced by `AuthGuard`, `RoleGuard`, and `PermissionGuard` in the Angular route configuration.

### Legend

| Symbol | Meaning |
|--------|---------|
| ✅ | Full access — can view, create, update |
| 👁 | Read-only — can view and export |
| — | No access |

### Clinical Portal Pages

| Page | Permission / Role Required | CMO | Quality Dir. | Care Coord. | Provider | RN | MA | Analyst | IT Admin | Auditor |
|------|---------------------------|-----|-------------|-------------|----------|----|----|---------|----------|---------|
| **Dashboard** | Auth only | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| — Provider Dashboard | `CLINICIAN` UI role | — | — | — | ✅ | — | — | — | — | — |
| — RN Dashboard | `CLINICIAN` UI role | — | — | — | — | ✅ | — | — | — | — |
| — MA Dashboard | `CLINICIAN` UI role | — | — | — | — | — | ✅ | — | — | — |
| — Admin Dashboard | Default | ✅ | ✅ | ✅ | — | — | — | ✅ | ✅ | ✅ |
| **Patients** | `VIEW_PATIENTS` | ✅ | 👁 | ✅ | ✅ | ✅ | ✅ | — | ✅ | 👁 |
| **Patient Detail** | `VIEW_PATIENTS` | ✅ | 👁 | ✅ | ✅ | ✅ | ✅ | — | ✅ | 👁 |
| **Quality Measures** | `VIEW_EVALUATIONS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | — |
| **Quality Measure Detail** | `VIEW_EVALUATIONS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | — |
| **Measure Comparison** | `VIEW_EVALUATIONS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | — |
| **Evaluations** | `VIEW_EVALUATIONS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | — |
| **Results** | `VIEW_EVALUATIONS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | — |
| **Star Ratings** | `VIEW_EVALUATIONS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | — |
| **Reports** | `VIEW_REPORTS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | 👁 |
| **Custom Report Builder** | `VIEW_REPORTS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | 👁 |
| **Care Gaps** | `VIEW_CARE_GAPS` | ✅ | ✅ | ✅ | ✅ | ✅ | — | 👁 | ✅ | — |
| **Care Recommendations** | `VIEW_CARE_GAPS` | ✅ | ✅ | ✅ | ✅ | ✅ | — | 👁 | ✅ | — |
| **Outreach Campaigns** | `VIEW_CARE_GAPS` | ✅ | ✅ | ✅ | — | ✅ | — | 👁 | ✅ | — |
| **Risk Stratification** | `VIEW_PATIENTS` | ✅ | 👁 | ✅ | ✅ | ✅ | ✅ | — | ✅ | 👁 |
| **Population Insights** | `VIEW_PATIENTS` | ✅ | 👁 | ✅ | ✅ | ✅ | ✅ | — | ✅ | 👁 |
| **Patient Health Overview** | `VIEW_PATIENTS` | ✅ | 👁 | ✅ | ✅ | ✅ | ✅ | — | ✅ | 👁 |
| **CMO Onboarding** | `VIEW_REPORTS` | ✅ | ✅ | — | 👁 | — | — | ✅ | ✅ | 👁 |
| **Pre-Visit Planning** | `ADMIN`, `EVALUATOR` | — | — | — | — | — | ✅ | ✅ | ✅ | — |
| **Visualizations** (4 views) | Auth only | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **AI Assistant** | Auth only | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Knowledge Base** | Auth only | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Measure Builder** | `ADMIN`, `MEASURE_DEVELOPER` | — | — | — | — | — | — | — | ✅ | — |
| **Agent Builder** | `ADMIN`, `DEVELOPER` | — | — | — | — | — | — | — | ✅ | — |
| **User Management** | `ADMIN` | — | — | — | — | — | — | — | ✅ | — |
| **Tenant Settings** | `ADMIN` | — | — | — | — | — | — | — | ✅ | — |
| **Audit Logs** | `ADMIN` | — | — | — | — | — | — | — | ✅ | — |
| **Demo Seeding** | `ADMIN` | — | — | — | — | — | — | — | ✅ | — |
| **QA Audit Dashboard** | `ADMIN`, `QA_ANALYST`, `QUALITY_OFFICER`, `AUDITOR` | ✅ | ✅ | — | — | — | — | — | ✅ | ✅ |
| **Clinical Audit Dashboard** | `ADMIN`, `CLINICIAN`, `CLINICAL_ADMIN` | ✅ | — | — | ✅ | ✅ | ✅ | — | ✅ | — |
| **MPI Audit Dashboard** | `ADMIN`, `MPI_ANALYST`, `DATA_STEWARD` | — | — | — | — | — | — | — | ✅ | — |

### Admin Portal Pages

| Page | Guard | IT Admin | Sales | Investor |
|------|-------|----------|-------|----------|
| **Dashboard** | AuthGuard | ✅ | — | — |
| **Users** | AuthGuard | ✅ | — | — |
| **Tenants** | AuthGuard | ✅ | — | — |
| **System Health** | AuthGuard | ✅ | — | — |
| **Real-Time Metrics** | AuthGuard | ✅ | — | — |
| **Audit Logs** | AuthGuard | ✅ | — | — |
| **Config Versions** | AuthGuard | ✅ | — | — |
| **Roadmap** | AuthGuard | ✅ | — | — |
| **Sales Dashboard** | salesAuthGuard | — | ✅ | — |
| **Sales Leads** | salesAuthGuard | — | ✅ | — |
| **Sales Pipeline** | salesAuthGuard | — | ✅ | — |
| **Sales Activities** | salesAuthGuard | — | ✅ | — |
| **Sales Sequences** | salesAuthGuard | — | ✅ | — |
| **Sales LinkedIn** | salesAuthGuard | — | ✅ | — |
| **Investor Launch** | InvestorAuthGuard | — | — | ✅ |

---

## 4. Value Summary by Role

### Quantified Impact by Persona

| Persona | Without HDIM | With HDIM | Improvement | Key Driver |
|---------|-------------|-----------|-------------|------------|
| **CMO / VP Quality** | Quality data available quarterly; Star Rating surprises common | Real-time KPI dashboard with daily Star Rating projection | 90-day faster visibility → catch rating drift in weeks, not quarters | CMO Onboarding Scorecard + Star Ratings |
| **Quality Director** | Manual chart review for evaluations; 2-week submission prep | Automated CQL evaluation + one-click QRDA export | 40+ hours saved per quarter on compliance reporting | Evaluations (bulk mode) + Reports |
| **Care Coordinator** | Spreadsheet-based gap tracking; no ROI visibility on outreach | AI-prioritized gap triage + ROI-calculated intervention selection | 15–20% more gaps closed; every outreach dollar tracked to outcome | Care Gap Manager + Outreach Campaigns |
| **Provider** | Quality gaps invisible during patient encounters | Gap indicators embedded in appointment schedule + in-visit closure | 2–5 additional gaps addressed per provider per week | Provider Dashboard (drag-and-drop) |
| **Registered Nurse** | Generic task list; separate systems for outreach, meds, education | Purpose-built nursing dashboard with integrated workflows | Shift start to action in <2 minutes vs. 15 min setup | RN Dashboard (5 quick actions) |
| **Medical Assistant** | Whiteboard schedule tracking; no pre-visit gap awareness | Real-time digital schedule with pre-visit gap staging | 100% pre-visit gap visibility → higher in-visit closure | MA Dashboard + Pre-Visit Planning |
| **Quality Analyst** | Manual data extraction; ad-hoc report building | 12-category evaluation engine + template-based reporting | Evaluation turnaround: days → hours; report building: hours → minutes | Evaluations + Custom Report Builder |
| **IT Administrator** | Multi-tool monitoring; manual user provisioning | Single-pane admin portal with real-time metrics | System monitoring + user management in one platform | Admin Portal (8 operational views) |
| **Compliance Auditor** | Manual audit trail assembly; screenshot-based evidence | 100% automatic API audit coverage + evidence export | Audit prep: 2 weeks → 2 hours; 100% coverage vs. samples | 3 Audit Dashboards + HTTP Interceptor |

### Platform-Level Differentiators

| Capability | What It Means | Why It's Different |
|-----------|---------------|-------------------|
| **4 Role-Adaptive Dashboards** | MA, RN, Provider, and Admin each see a completely different UI — not just filtered views | Most platforms show the same interface with features hidden; HDIM rebuilds the experience per role |
| **AI-Powered Population Insights** | Risk pyramids, predicted care gaps with weighted factor models, and suggested actions | Not just a risk score — a predictive model with transparent factor weights and actionable recommendations |
| **ROI-Calculated Outreach** | Every intervention channel shows cost-per-unit, success rate, and ROI multiplier | Care coordinators can optimize their budget in real-time, not guess |
| **3D Quality Visualizations** | Live Batch Monitor, Quality Constellation, Flow Network, Measure Matrix — 4 WebGL-powered views | Executive-ready visual storytelling for board presentations and quality committees |
| **Drag-and-Drop Provider Dashboard** | Providers customize their daily view by reordering sections with keyboard shortcuts | Clinician adoption increases when they control their own workspace |
| **Embedded AI Assistant** | Context-aware AI that understands HEDIS measures, CQL logic, and clinical quality workflows | Not a generic chatbot — trained on healthcare quality measurement domain |
| **50+ HEDIS/CMS Measures** | Purpose-built CQL measure library across 12 clinical categories | Out-of-the-box measure coverage versus months of custom CQL development |
| **HIPAA-Native Architecture** | 100% API audit coverage, 15-min session timeout, PHI-filtered logging, no-cache headers | HIPAA compliance is architectural, not bolt-on — enforced at every layer |

---

## 5. Appendix: Technical References

### Source Components

| Feature | Component Source | Lines |
|---------|-----------------|-------|
| Provider Dashboard | `apps/clinical-portal/src/app/pages/dashboard/provider-dashboard/provider-dashboard.component.ts` | 2,446 |
| RN Dashboard | `apps/clinical-portal/src/app/pages/dashboard/rn-dashboard/rn-dashboard.component.ts` | 1,075 |
| MA Dashboard | `apps/clinical-portal/src/app/pages/dashboard/ma-dashboard/ma-dashboard.component.ts` | 864 |
| Care Gap Manager | `apps/clinical-portal/src/app/pages/care-gaps/care-gap-manager.component.ts` | 1,366 |
| Population Insights | `apps/clinical-portal/src/app/pages/insights/insights.component.ts` | 1,409 |
| Evaluations | `apps/clinical-portal/src/app/pages/evaluations/evaluations.component.ts` | 969 |
| Risk Stratification | `apps/clinical-portal/src/app/pages/risk-stratification/risk-stratification.component.ts` | 531 |
| Outreach Campaigns | `apps/clinical-portal/src/app/pages/outreach-campaigns/outreach-campaigns.component.ts` | 372 |
| Pre-Visit Planning | `apps/clinical-portal/src/app/pages/pre-visit-planning/pre-visit-planning.component.ts` | 341 |
| Route Configuration | `apps/clinical-portal/src/app/app.routes.ts` | 456 |

### Backend RBAC Sources

| File | Purpose |
|------|---------|
| `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/UserRole.java` | 13-role enum |
| `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/Permission.java` | 31 permissions |
| `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/domain/RolePermissions.java` | Role → permission mapping |

### Service Count

- **60+ Angular services** powering clinical, analytics, workflow, admin, and AI capabilities
- **51 backend microservices** across gateway, clinical, quality, event, and platform layers
- **29 independent databases** with tenant-level isolation

---

*This document reflects the implemented state of HDIM as of March 13, 2026. All features listed are production-ready unless explicitly marked otherwise.*
