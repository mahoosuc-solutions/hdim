// ─────────────────────────────────────────────────────────────────────────────
// HDIM Capabilities Explorer — Structured Data
// Source of truth: docs/sales/UI_USER_JOURNEY_GUIDE.md + docs/product/02-architecture/user-stories.md
// ─────────────────────────────────────────────────────────────────────────────

// ── Persona Definitions ─────────────────────────────────────────────────────

export type PersonaId =
  | 'cmo'
  | 'quality-director'
  | 'care-coordinator'
  | 'provider'
  | 'rn'
  | 'ma'
  | 'analyst'
  | 'it-admin'
  | 'auditor'

export interface Persona {
  id: PersonaId
  title: string
  titleExamples: string
  systemRoles: string[]
  defaultView: string
  primaryFeatures: string[]
  keyMetric: string
  color: string
  icon: string // lucide icon name
  description: string
}

export const PERSONAS: Persona[] = [
  {
    id: 'cmo',
    title: 'CMO / VP Quality',
    titleExamples: 'Chief Medical Officer, VP Quality',
    systemRoles: ['QUALITY_OFFICER', 'CLINICAL_ADMIN'],
    defaultView: 'CMO Onboarding Scorecard',
    primaryFeatures: ['Executive KPIs', 'Star Ratings', 'Governance Signals', 'Quality Reports'],
    keyMetric: 'Star Rating trajectory & gap closure rate',
    color: '#7C3AED',
    icon: 'Crown',
    description:
      'Achieve and maintain target CMS Star Ratings through data-driven quality improvement with executive-level KPI dashboards and governance signals.',
  },
  {
    id: 'quality-director',
    title: 'Quality Director',
    titleExamples: 'HEDIS Director, Quality Program Manager',
    systemRoles: ['QUALITY_OFFICER'],
    defaultView: 'Quality Measures Library',
    primaryFeatures: ['Measure Library', 'Evaluations', 'Star Ratings', 'Measure Comparison', 'Reports'],
    keyMetric: 'Measure compliance % vs targets',
    color: '#2563EB',
    icon: 'Target',
    description:
      'Ensure all HEDIS/CMS quality measures meet compliance targets through systematic evaluation, gap closure, and 50+ measure library management.',
  },
  {
    id: 'care-coordinator',
    title: 'Care Coordinator',
    titleExamples: 'Care Manager, Patient Navigator',
    systemRoles: ['CARE_COORDINATOR'],
    defaultView: 'Care Gap Manager',
    primaryFeatures: ['Care Gap Triage', 'Outreach Campaigns', 'Risk Stratification', 'Predictive Gaps'],
    keyMetric: 'Gaps closed per week & outreach ROI',
    color: '#059669',
    icon: 'HeartHandshake',
    description:
      'Close the maximum number of care gaps through AI-prioritized triage, ROI-calculated intervention selection, and predictive gap analysis.',
  },
  {
    id: 'provider',
    title: 'Provider',
    titleExamples: 'Physician, NP, PA, DO',
    systemRoles: ['CLINICIAN'],
    defaultView: 'Provider Dashboard',
    primaryFeatures: ['Schedule + Care Gaps', 'Results Review', 'Risk Stratification', 'Gap Closure'],
    keyMetric: 'Quality score & gaps addressed per visit',
    color: '#0D4F8B',
    icon: 'Stethoscope',
    description:
      'Address quality gaps during patient encounters without added workflow burden, with drag-and-drop dashboards and in-visit gap closure.',
  },
  {
    id: 'rn',
    title: 'Registered Nurse',
    titleExamples: 'RN, BSN, Care Management Nurse',
    systemRoles: ['CLINICIAN'],
    defaultView: 'RN Dashboard',
    primaryFeatures: ['Care Gap Coordination', 'Outreach', 'Med Reconciliation', 'Patient Education'],
    keyMetric: 'Outreach completion rate',
    color: '#0891B2',
    icon: 'UserCheck',
    description:
      'Coordinate care gap closure through patient outreach, education, medication management, and care plan updates from a purpose-built nursing workflow.',
  },
  {
    id: 'ma',
    title: 'Medical Assistant',
    titleExamples: 'MA, Clinical Support Staff',
    systemRoles: ['CLINICIAN'],
    defaultView: 'MA Dashboard',
    primaryFeatures: ['Check-in', 'Vitals', 'Room Prep', 'Pre-Visit Gap Alerts'],
    keyMetric: 'Pre-visit task completion rate',
    color: '#D97706',
    icon: 'ClipboardCheck',
    description:
      'Ensure efficient patient flow through check-in, vitals, room prep, and pre-visit gap alerts with task-driven scheduling.',
  },
  {
    id: 'analyst',
    title: 'Quality Analyst',
    titleExamples: 'Data Analyst, BI Analyst',
    systemRoles: ['EVALUATOR', 'ANALYST'],
    defaultView: 'Evaluations',
    primaryFeatures: ['CQL Evaluation', 'Results', 'Custom Report Builder', 'Measure Analytics'],
    keyMetric: 'Evaluations processed & report turnaround',
    color: '#4F46E5',
    icon: 'BarChart3',
    description:
      'Execute quality measure evaluations accurately with AI-assisted parameter selection, data flow visualization, and template-based custom reports.',
  },
  {
    id: 'it-admin',
    title: 'IT Administrator',
    titleExamples: 'System Admin, Platform Engineer',
    systemRoles: ['ADMIN', 'SUPER_ADMIN'],
    defaultView: 'Admin Dashboard',
    primaryFeatures: ['User Management', 'Tenant Settings', 'System Health', 'Config Versions'],
    keyMetric: 'System uptime & tenant provisioning time',
    color: '#64748B',
    icon: 'Settings',
    description:
      'Ensure platform reliability, manage users and tenants, monitor system health, and maintain configurations across the 51-service architecture.',
  },
  {
    id: 'auditor',
    title: 'Compliance Auditor',
    titleExamples: 'Compliance Officer, Privacy Officer',
    systemRoles: ['AUDITOR'],
    defaultView: 'QA Audit Dashboard',
    primaryFeatures: ['Audit Logs', 'Clinical Audit', 'MPI Audit', 'Compliance Evidence'],
    keyMetric: 'Audit trail completeness & HIPAA evidence',
    color: '#DC2626',
    icon: 'ShieldCheck',
    description:
      'Verify HIPAA compliance, audit PHI access patterns, and produce evidence for regulatory review with 100% automatic API audit coverage.',
  },
]

// ── Journey Stage Data ──────────────────────────────────────────────────────

export interface JourneyStage {
  stage: string
  frequency: string
  whatTheyDo: string
  featuresUsed: string
  successCriteria: string
  valueDelivered: string
}

export interface PersonaJourney {
  personaId: PersonaId
  goal: string
  stages: JourneyStage[]
  differentiators: string[]
}

export const JOURNEYS: PersonaJourney[] = [
  {
    personaId: 'cmo',
    goal: 'Achieve and maintain target CMS Star Ratings through data-driven quality improvement.',
    stages: [
      { stage: 'Onboarding', frequency: 'Day 1–5', whatTheyDo: 'Review organizational quality baseline, set targets', featuresUsed: 'CMO Onboarding Scorecard, Knowledge Base', successCriteria: 'All KPIs populated with current data', valueDelivered: 'Time-to-insight: hours instead of weeks' },
      { stage: 'Morning Check', frequency: 'Daily (5 min)', whatTheyDo: 'Scan executive KPI cards: gap closure, interventions, data freshness, compliance', featuresUsed: 'CMO Scorecard → Governance Signals', successCriteria: 'All 4 KPIs trending positively', valueDelivered: 'Early warning on drift before it compounds' },
      { stage: 'Star Rating Review', frequency: 'Weekly', whatTheyDo: 'Review Star Rating projection, simulate gap closure scenarios', featuresUsed: 'Star Ratings, Measure Comparison', successCriteria: 'Projected rating meets bonus threshold', valueDelivered: 'Quantified revenue impact of each half-star' },
      { stage: 'Quality Committee', frequency: 'Monthly', whatTheyDo: 'Generate compliance reports, review measure trends', featuresUsed: 'Reports, QRDA Export', successCriteria: 'Reports generated in <5 min', valueDelivered: 'Eliminates 40+ hours per quarter' },
      { stage: 'CMS Submission', frequency: 'Quarterly', whatTheyDo: 'Export submission-ready data, verify completeness', featuresUsed: 'Reports → QRDA Export, Star Ratings', successCriteria: '100% measure coverage', valueDelivered: 'Confidence in submission accuracy' },
    ],
    differentiators: [
      'Executive KPI cards: gap closure rate (68%), intervention completion (74%), data freshness SLA (99.1%), compliance evidence (92%)',
      'Governance signals: weekly active users, workflow SLA adherence, last audit date, open escalations',
      'Top Actions: AI-recommended next steps for quality improvement',
    ],
  },
  {
    personaId: 'quality-director',
    goal: 'Ensure all HEDIS/CMS quality measures meet compliance targets.',
    stages: [
      { stage: 'Measure Setup', frequency: 'Onboarding', whatTheyDo: 'Configure active measures, set compliance targets', featuresUsed: 'Quality Measures Library (50+ measures, 12 categories)', successCriteria: 'All relevant measures activated', valueDelivered: 'Purpose-built measure library eliminates manual CQL authoring' },
      { stage: 'Batch Evaluation', frequency: 'Daily / Weekly', whatTheyDo: 'Run CQL evaluations across populations', featuresUsed: 'Evaluations (single + bulk mode)', successCriteria: 'Evaluations complete without errors', valueDelivered: 'Automated evaluation replaces days of manual chart review' },
      { stage: 'Performance Tracking', frequency: 'Daily', whatTheyDo: 'Compare measure performance against targets', featuresUsed: 'Measure Comparison, Star Ratings', successCriteria: 'Lagging measures identified', valueDelivered: 'Real-time visibility vs. quarterly surprises' },
      { stage: 'Gap Prioritization', frequency: 'Weekly', whatTheyDo: 'Review care gap distribution, assign priorities', featuresUsed: 'Care Gap Manager, Population Insights', successCriteria: 'Gaps prioritized by impact', valueDelivered: 'AI prioritization maximizes quality improvement per dollar' },
      { stage: 'Compliance Reporting', frequency: 'Quarterly', whatTheyDo: 'Generate HEDIS/CMS submission packages', featuresUsed: 'Reports → QRDA Export', successCriteria: 'Zero manual corrections', valueDelivered: 'Submission prep: 2 weeks → 2 days' },
    ],
    differentiators: [
      '12 measure categories: Preventive, Chronic Disease, Behavioral Health, Medication, Women\'s Health, and more',
      'Side-by-side measure comparison with pass rate, execution time, and demographic breakdowns',
      'Embedded AI assistant for evaluation workflow',
    ],
  },
  {
    personaId: 'care-coordinator',
    goal: 'Close the maximum number of care gaps through ROI-optimized outreach.',
    stages: [
      { stage: 'Gap Triage', frequency: 'Daily AM', whatTheyDo: 'Review assigned gaps by urgency, plan outreach', featuresUsed: 'Care Gap Manager (1,366-line triage UI)', successCriteria: 'Priorities identified, plan set', valueDelivered: 'Intelligent triage replaces spreadsheet sorting' },
      { stage: 'Patient Outreach', frequency: 'Daily', whatTheyDo: 'Execute interventions: calls, emails, appointments, referrals', featuresUsed: 'Quick Action Dialogs (5 types)', successCriteria: '15–20 contacts completed', valueDelivered: 'One-click intervention with auto-documentation' },
      { stage: 'Campaign Management', frequency: 'Weekly', whatTheyDo: 'Create outreach campaigns with ROI projections', featuresUsed: 'Outreach Campaigns (5 channels)', successCriteria: 'Campaign meets close rate target', valueDelivered: 'Letter $12/32% vs. Coordinator Call $45/67%' },
      { stage: 'Risk Review', frequency: 'Weekly', whatTheyDo: 'Review population risk stratification', featuresUsed: 'Risk Stratification', successCriteria: 'Rising-risk patients flagged', valueDelivered: 'Prevent costly events through early intervention' },
      { stage: 'Predictive Analysis', frequency: 'Weekly', whatTheyDo: 'Review AI-predicted care gaps', featuresUsed: 'Population Insights → Predicted Gaps', successCriteria: 'Gaps addressed before manifesting', valueDelivered: 'ML prediction: 30–90 day lookahead' },
    ],
    differentiators: [
      'ROI-driven intervention: success rate, cost, time to close, and ROI multiplier per channel',
      '5 intervention channels with per-unit economics: Letter, Provider Alert, Call, SMS, Portal',
      'Predictive care gaps using weighted factor model (Historical 40%, Adherence 25%, Refills 20%, Peer 15%)',
    ],
  },
  {
    personaId: 'provider',
    goal: 'Address quality gaps during encounters without adding workflow burden.',
    stages: [
      { stage: 'Morning Huddle', frequency: 'Daily (5 min)', whatTheyDo: 'Review schedule with embedded care gap indicators', featuresUsed: 'Provider Dashboard → Today\'s Schedule', successCriteria: 'High-priority gaps noted', valueDelivered: 'Zero-prep quality awareness' },
      { stage: 'Results Review', frequency: 'Daily (10 min)', whatTheyDo: 'Review pending results with severity highlighting', featuresUsed: 'Provider Dashboard → Results Awaiting Review', successCriteria: 'All critical results reviewed', valueDelivered: 'Severity-coded results prevent missed findings' },
      { stage: 'Patient Encounters', frequency: 'Per visit', whatTheyDo: 'View care gaps during visit, close through clinical actions', featuresUsed: 'Patient Detail → Care Gap Closure Dialog', successCriteria: 'Addressable gaps closed during visit', valueDelivered: 'In-visit gap closure: no separate workflow' },
      { stage: 'Quality Check', frequency: 'Weekly (5 min)', whatTheyDo: 'Check personal quality score vs targets', featuresUsed: 'Provider Dashboard → Quality Measures', successCriteria: 'Score stable or improving', valueDelivered: 'Self-service quality tracking' },
      { stage: 'Peer Comparison', frequency: 'Monthly', whatTheyDo: 'Review quality performance relative to peers', featuresUsed: 'Provider Dashboard → Leaderboard', successCriteria: 'At or above peer median', valueDelivered: 'Data-driven quality culture' },
    ],
    differentiators: [
      'Drag-and-drop dashboard: reorder Schedule, Results, Care Gaps, Risk, Quality sections',
      'Keyboard shortcuts for rapid navigation (customizable)',
      'Guided tour for new providers with contextual onboarding',
      'Results review with trend indicators (↑/↓/→/NEW) and one-click actions',
    ],
  },
  {
    personaId: 'rn',
    goal: 'Coordinate care gap closure through outreach, education, and medication management.',
    stages: [
      { stage: 'Shift Start', frequency: 'Daily AM', whatTheyDo: 'Review assigned gaps, pending calls, med reconciliations', featuresUsed: 'RN Dashboard → 4 stat cards', successCriteria: 'Priorities identified from stat cards', valueDelivered: 'Purpose-built nursing workflow' },
      { stage: 'Care Gap Work', frequency: 'Daily', whatTheyDo: 'Address gaps: education, referrals, assessments', featuresUsed: 'RN Dashboard → Care Gaps Tab', successCriteria: 'Assigned gaps addressed', valueDelivered: 'Category-specific actions' },
      { stage: 'Patient Outreach', frequency: 'Daily', whatTheyDo: 'Make scheduled outreach calls, document outcomes', featuresUsed: 'RN Dashboard → Outreach Tab', successCriteria: 'Scheduled outreach completed', valueDelivered: 'Structured tracking (scheduled/completed/missed)' },
      { stage: 'Med Reconciliation', frequency: 'Per patient', whatTheyDo: 'Reconcile medications for assigned patients', featuresUsed: 'Quick Actions → Med Reconciliation', successCriteria: 'Reconciliations complete; discrepancies flagged', valueDelivered: 'Integrated workflow reduces EHR switching' },
      { stage: 'Care Plan Updates', frequency: 'Weekly', whatTheyDo: 'Review and update care plans, coordinate referrals', featuresUsed: 'Quick Actions → Update Care Plan', successCriteria: 'Care plans current; referrals tracked', valueDelivered: 'Centralized management across patients' },
    ],
    differentiators: [
      '4 dedicated stat cards: Care Gaps Assigned, Calls Pending, Med Reconciliations, Education Due',
      '5 quick action buttons: Care Plan, Outreach, Med Reconciliation, Education, Referral',
      'Tabbed workflow: Care Gaps tab and Outreach tab with inline actions',
    ],
  },
  {
    personaId: 'ma',
    goal: 'Ensure efficient patient flow through check-in, vitals, room prep, and gap alerts.',
    stages: [
      { stage: 'Day Start', frequency: 'Daily AM', whatTheyDo: 'Review schedule, check room readiness, pre-visit tasks', featuresUsed: 'MA Dashboard → 4 stat cards', successCriteria: 'Schedule reviewed; rooms prepped', valueDelivered: '4 real-time stat cards eliminate whiteboard tracking' },
      { stage: 'Patient Check-in', frequency: 'Per patient', whatTheyDo: 'Check in patients, verify demographics, note gap alerts', featuresUsed: 'MA Dashboard → Check-in Button', successCriteria: 'Patient checked in; gaps flagged', valueDelivered: 'Gap alerts at check-in ensure provider sees quality opportunities' },
      { stage: 'Vitals Recording', frequency: 'Per patient', whatTheyDo: 'Record vital signs for each patient', featuresUsed: 'MA Dashboard → Vitals Button', successCriteria: 'Vitals recorded within 5 min', valueDelivered: 'In-workflow vitals without context switching' },
      { stage: 'Room Preparation', frequency: 'Per patient', whatTheyDo: 'Prepare exam room, stage supplies', featuresUsed: 'MA Dashboard → Room Prep Button', successCriteria: 'Room ready before provider', valueDelivered: 'Appointment-type-aware prep' },
      { stage: 'Pre-Visit Gaps', frequency: 'Per patient', whatTheyDo: 'Review and stage pre-visit care gap alerts', featuresUsed: 'Pre-Visit Planning (30-day lookahead)', successCriteria: 'All gaps surfaced before encounter', valueDelivered: 'Proactive staging increases closure rates' },
    ],
    differentiators: [
      'Task-driven schedule table: Time, Patient, Task Type, Room, Status — inline action buttons',
      'Pre-visit planning: per-patient expandable cards with gaps, urgency, days overdue',
      'Preparation tracking: visual completion status with summary counts',
    ],
  },
  {
    personaId: 'analyst',
    goal: 'Execute quality evaluations accurately and deliver reports on schedule.',
    stages: [
      { stage: 'Evaluation Setup', frequency: 'Per request', whatTheyDo: 'Configure evaluation parameters with AI assistance', featuresUsed: 'Evaluations (12 categories, AI assistant)', successCriteria: 'Parameters validated correctly', valueDelivered: '12-category library + AI-assisted selection' },
      { stage: 'Batch Execution', frequency: 'Daily / Weekly', whatTheyDo: 'Run CQL evaluations, monitor progress', featuresUsed: 'Bulk Mode, Data Flow Visualization', successCriteria: 'All evaluations complete', valueDelivered: 'Thousands of patients with real-time progress' },
      { stage: 'Results Analysis', frequency: 'Per evaluation', whatTheyDo: 'Review compliance rates, drill into details', featuresUsed: 'Results (sortable, filterable)', successCriteria: 'Results validated; anomalies investigated', valueDelivered: 'Filterable results with compliance metrics' },
      { stage: 'Custom Reporting', frequency: 'Weekly', whatTheyDo: 'Build custom report templates, schedule generation', featuresUsed: 'Custom Report Builder', successCriteria: 'Reports delivered on time', valueDelivered: 'Template-based eliminates repetitive work' },
      { stage: 'Data Export', frequency: 'Quarterly', whatTheyDo: 'Export QRDA-formatted data for CMS/HEDIS', featuresUsed: 'Reports → QRDA Export', successCriteria: 'Export validates against CMS schema', valueDelivered: 'Submission-ready from a single click' },
    ],
    differentiators: [
      'Data flow visualization: real-time pipeline from ingestion through CQL to result storage',
      'Measure favorites and filter persistence across sessions',
      'AI assistant for parameter suggestions and result explanations',
    ],
  },
  {
    personaId: 'it-admin',
    goal: 'Ensure platform reliability, manage access, maintain configurations.',
    stages: [
      { stage: 'System Check', frequency: 'Daily (5 min)', whatTheyDo: 'Monitor service health, review real-time metrics', featuresUsed: 'Admin Portal → System Health, Metrics', successCriteria: 'All services UP; no degradation', valueDelivered: 'Single-pane system health' },
      { stage: 'User Management', frequency: 'As needed', whatTheyDo: 'Create users, assign roles, manage permissions', featuresUsed: 'Admin Portal → Users (13-role RBAC)', successCriteria: 'Users provisioned within SLA', valueDelivered: '13-role RBAC with 31 granular permissions' },
      { stage: 'Tenant Configuration', frequency: 'As needed', whatTheyDo: 'Configure tenant settings, feature flags', featuresUsed: 'Admin Portal → Tenants', successCriteria: 'Tenant isolated; features configured', valueDelivered: 'Multi-tenant isolation at database level' },
      { stage: 'Audit Review', frequency: 'Weekly', whatTheyDo: 'Review audit logs, investigate security events', featuresUsed: 'Audit Logs (search/filter/export)', successCriteria: 'No unauthorized access', valueDelivered: '100% API audit coverage (HIPAA §164.312(b))' },
      { stage: 'Demo / Sandbox', frequency: 'As needed', whatTheyDo: 'Seed demo data for sales and training', featuresUsed: 'Demo Seeding (200 patients, 56 gaps)', successCriteria: 'Environment provisioned in <20 min', valueDelivered: 'Self-service demo provisioning' },
    ],
    differentiators: [
      'Separate Admin Portal with 8 operational views and real-time metrics',
      'Configuration version history with diff and rollback capabilities',
      'Investor portal with separate auth flow for stakeholder access',
    ],
  },
  {
    personaId: 'auditor',
    goal: 'Verify HIPAA compliance, audit PHI access, produce regulatory evidence.',
    stages: [
      { stage: 'Audit Trail Review', frequency: 'Daily / Weekly', whatTheyDo: 'Review PHI access logs, identify anomalies', featuresUsed: 'QA Audit + Clinical Audit Dashboards', successCriteria: 'All access authorized and documented', valueDelivered: '100% HTTP audit coverage — every API call logged' },
      { stage: 'Compliance Verification', frequency: 'Monthly', whatTheyDo: 'Verify HIPAA controls, session timeouts, cache TTLs', featuresUsed: 'Compliance Dashboard, Audit Logs', successCriteria: 'All §164.312 controls verified', valueDelivered: 'Built-in compliance verification' },
      { stage: 'MPI Audit', frequency: 'Monthly', whatTheyDo: 'Review Master Patient Index integrity', featuresUsed: 'MPI Audit Dashboard', successCriteria: 'Accuracy verified; duplicates within threshold', valueDelivered: 'Dedicated MPI audit for data stewardship' },
      { stage: 'Evidence Collection', frequency: 'Quarterly', whatTheyDo: 'Generate compliance evidence bundles', featuresUsed: 'Audit Logs → Export (CSV/PDF)', successCriteria: 'Evidence bundle complete for all controls', valueDelivered: 'Automated evidence replaces manual screenshots' },
    ],
    differentiators: [
      '3 specialized audit dashboards: QA, Clinical, and MPI',
      'Automatic audit interceptor for every backend API call',
      'Session timeout audit: automatic vs. explicit logout with idle duration (HIPAA §164.312(a)(2)(iii))',
    ],
  },
]

// ── Feature Access Matrix ───────────────────────────────────────────────────

export type AccessLevel = 'full' | 'read' | 'none'

export interface FeaturePage {
  name: string
  permission: string
  category: 'clinical' | 'quality' | 'analytics' | 'admin' | 'platform'
  access: Record<PersonaId, AccessLevel>
}

export const FEATURE_PAGES: FeaturePage[] = [
  { name: 'Provider Dashboard', permission: 'CLINICIAN UI', category: 'clinical', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'full', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'none', auditor: 'none' } },
  { name: 'RN Dashboard', permission: 'CLINICIAN UI', category: 'clinical', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'full', ma: 'none', analyst: 'none', 'it-admin': 'none', auditor: 'none' } },
  { name: 'MA Dashboard', permission: 'CLINICIAN UI', category: 'clinical', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'full', analyst: 'none', 'it-admin': 'none', auditor: 'none' } },
  { name: 'Admin Dashboard', permission: 'Default', category: 'admin', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'none', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'full' } },
  { name: 'Patients', permission: 'VIEW_PATIENTS', category: 'clinical', access: { cmo: 'full', 'quality-director': 'read', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'none', 'it-admin': 'full', auditor: 'read' } },
  { name: 'Patient Detail', permission: 'VIEW_PATIENTS', category: 'clinical', access: { cmo: 'full', 'quality-director': 'read', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'none', 'it-admin': 'full', auditor: 'read' } },
  { name: 'Quality Measures', permission: 'VIEW_EVALUATIONS', category: 'quality', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Measure Comparison', permission: 'VIEW_EVALUATIONS', category: 'quality', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Evaluations', permission: 'VIEW_EVALUATIONS', category: 'quality', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Results', permission: 'VIEW_EVALUATIONS', category: 'quality', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Star Ratings', permission: 'VIEW_EVALUATIONS', category: 'quality', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Reports', permission: 'VIEW_REPORTS', category: 'analytics', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'read' } },
  { name: 'Custom Report Builder', permission: 'VIEW_REPORTS', category: 'analytics', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'read' } },
  { name: 'Care Gaps', permission: 'VIEW_CARE_GAPS', category: 'clinical', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'none', analyst: 'read', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Care Recommendations', permission: 'VIEW_CARE_GAPS', category: 'clinical', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'none', analyst: 'read', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Outreach Campaigns', permission: 'VIEW_CARE_GAPS', category: 'clinical', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'none', rn: 'full', ma: 'none', analyst: 'read', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Risk Stratification', permission: 'VIEW_PATIENTS', category: 'analytics', access: { cmo: 'full', 'quality-director': 'read', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'none', 'it-admin': 'full', auditor: 'read' } },
  { name: 'Population Insights', permission: 'VIEW_PATIENTS', category: 'analytics', access: { cmo: 'full', 'quality-director': 'read', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'none', 'it-admin': 'full', auditor: 'read' } },
  { name: 'CMO Onboarding', permission: 'VIEW_REPORTS', category: 'quality', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'read', rn: 'none', ma: 'none', analyst: 'full', 'it-admin': 'full', auditor: 'read' } },
  { name: 'Pre-Visit Planning', permission: 'ADMIN, EVALUATOR', category: 'clinical', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'full', analyst: 'full', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Visualizations (4 views)', permission: 'Auth only', category: 'platform', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'full', 'it-admin': 'full', auditor: 'full' } },
  { name: 'AI Assistant', permission: 'Auth only', category: 'platform', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'full', 'it-admin': 'full', auditor: 'full' } },
  { name: 'Knowledge Base', permission: 'Auth only', category: 'platform', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'full', provider: 'full', rn: 'full', ma: 'full', analyst: 'full', 'it-admin': 'full', auditor: 'full' } },
  { name: 'Measure Builder', permission: 'ADMIN', category: 'admin', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Agent Builder', permission: 'ADMIN', category: 'admin', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'full', auditor: 'none' } },
  { name: 'User Management', permission: 'ADMIN', category: 'admin', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'full', auditor: 'none' } },
  { name: 'Tenant Settings', permission: 'ADMIN', category: 'admin', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'full', auditor: 'none' } },
  { name: 'QA Audit Dashboard', permission: 'AUDITOR, QUALITY_OFFICER', category: 'admin', access: { cmo: 'full', 'quality-director': 'full', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'full', auditor: 'full' } },
  { name: 'Clinical Audit Dashboard', permission: 'CLINICIAN, CLINICAL_ADMIN', category: 'admin', access: { cmo: 'full', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'full', rn: 'full', ma: 'full', analyst: 'none', 'it-admin': 'full', auditor: 'none' } },
  { name: 'MPI Audit Dashboard', permission: 'ADMIN', category: 'admin', access: { cmo: 'none', 'quality-director': 'none', 'care-coordinator': 'none', provider: 'none', rn: 'none', ma: 'none', analyst: 'none', 'it-admin': 'full', auditor: 'none' } },
]

// ── User Stories ─────────────────────────────────────────────────────────────

export interface UserStory {
  id: string
  personaId: PersonaId
  role: string
  phase: 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'cross-role'
  story: string
  acceptanceCriteria: string
  component: string
  status: 'implemented'
}

export const USER_STORIES: UserStory[] = [
  // Provider
  { id: 'PRV-001', personaId: 'provider', role: 'Provider', phase: 'daily', story: "See today's appointment schedule with care gap indicators embedded in each time slot", acceptanceCriteria: 'Schedule displays all today\'s appointments; each row shows gap indicator badges (critical/high/moderate)', component: 'provider-dashboard.component.ts → Today\'s Schedule', status: 'implemented' },
  { id: 'PRV-002', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'Review pending lab/test results with severity-coded highlighting', acceptanceCriteria: 'Results section shows severity color-coded results with trend indicators (↑/↓/→/NEW)', component: 'provider-dashboard.component.ts → Results Awaiting Review', status: 'implemented' },
  { id: 'PRV-003', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'Close care gaps directly from my dashboard during a patient visit', acceptanceCriteria: 'Care gap closure dialog captures evidence, intervention type, and outcome', component: 'care-gap-closure-dialog.component.ts', status: 'implemented' },
  { id: 'PRV-004', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'See high-priority care gaps ranked by clinical urgency', acceptanceCriteria: 'Gaps shown with patient, gap type, context, risk level; actions: Address, View Patient', component: 'provider-dashboard.component.ts → High Priority Care Gaps', status: 'implemented' },
  { id: 'PRV-005', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'View patient panel risk stratification with HCC scores and hospitalizations', acceptanceCriteria: 'Risk scores, HCC, conditions, hospitalizations, trending indicators', component: 'provider-dashboard.component.ts → Risk Stratification', status: 'implemented' },
  { id: 'PRV-006', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'Take actions on results with one click — Review, Sign, View History, Contact, Order, Refer', acceptanceCriteria: 'Action buttons on each result row; confirmation toast; audit logged', component: 'provider-dashboard.component.ts → Results actions', status: 'implemented' },
  { id: 'PRV-007', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'Drag and drop dashboard sections to reorder based on preference', acceptanceCriteria: 'All 5 sections drag-reorderable; order persists across sessions', component: 'provider-dashboard.component.ts → DragDropModule', status: 'implemented' },
  { id: 'PRV-008', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'Keyboard shortcuts for rapid navigation between sections', acceptanceCriteria: 'Shortcuts customizable; dialog accessible; hints on hover', component: 'keyboard-shortcuts.service.ts', status: 'implemented' },
  { id: 'PRV-009', personaId: 'provider', role: 'Provider', phase: 'daily', story: 'Guided tour on first login walking through each dashboard section', acceptanceCriteria: 'Tour overlay highlights sections; user can skip or complete', component: 'guided-tour.service.ts', status: 'implemented' },
  { id: 'PRV-010', personaId: 'provider', role: 'Provider', phase: 'weekly', story: 'See quality performance relative to peers via a leaderboard', acceptanceCriteria: 'Leaderboard shows ranked scores; current user highlighted; peer median visible', component: 'provider-leaderboard-dialog.component.ts', status: 'implemented' },
  { id: 'PRV-011', personaId: 'provider', role: 'Provider', phase: 'weekly', story: 'See quality measure performance with numerator/denominator and target comparison', acceptanceCriteria: 'Performance bar, numerator/denominator, target line; color for above/below', component: 'provider-dashboard.component.ts → Quality Measures', status: 'implemented' },
  { id: 'PRV-012', personaId: 'provider', role: 'Provider', phase: 'weekly', story: 'View detailed patient records including demographics, conditions, medications, and history', acceptanceCriteria: 'Patient detail shows all data; care gap history; medication list; encounter timeline', component: 'patient-detail.component.ts', status: 'implemented' },
  // Registered Nurse
  { id: 'RN-001', personaId: 'rn', role: 'Registered Nurse', phase: 'daily', story: 'See assigned care gaps organized by priority and category', acceptanceCriteria: 'Care Gaps tab: patient, type, priority, category, due date, status; filterable', component: 'rn-dashboard.component.ts → Care Gaps tab', status: 'implemented' },
  { id: 'RN-002', personaId: 'rn', role: 'Registered Nurse', phase: 'daily', story: 'See pending outreach calls with type, reason, and scheduled date', acceptanceCriteria: 'Outreach tab: sorted by date; status tracking; inline completion action', component: 'rn-dashboard.component.ts → Outreach tab', status: 'implemented' },
  { id: 'RN-003', personaId: 'rn', role: 'Registered Nurse', phase: 'daily', story: 'Perform medication reconciliation through a dedicated workflow', acceptanceCriteria: 'Med Reconciliation dialog; medication list; discrepancy flagging', component: 'medication.service.ts', status: 'implemented' },
  { id: 'RN-004', personaId: 'rn', role: 'Registered Nurse', phase: 'daily', story: 'Deliver patient education and document delivery', acceptanceCriteria: 'Education workflow; topic selection; delivery documented; audit record', component: 'nurse-workflow.service.ts', status: 'implemented' },
  { id: 'RN-005', personaId: 'rn', role: 'Registered Nurse', phase: 'daily', story: 'Update patient care plans and coordinate referrals from dashboard', acceptanceCriteria: 'Care Plan and Referral quick actions; tracking to completion', component: 'care-plan.service.ts', status: 'implemented' },
  { id: 'RN-006', personaId: 'rn', role: 'Registered Nurse', phase: 'daily', story: 'See 4 summary stat cards at top of dashboard for instant workload read', acceptanceCriteria: '4 stat cards with real-time counts; clicking filters relevant tab', component: 'rn-dashboard.component.ts → stat cards', status: 'implemented' },
  // Medical Assistant
  { id: 'MA-001', personaId: 'ma', role: 'Medical Assistant', phase: 'daily', story: "See today's schedule with stat cards for Scheduled, Checked In, Vitals Pending, Rooms Ready", acceptanceCriteria: '4 stat cards with live counts; schedule table with time, patient, task, room, status', component: 'ma-dashboard.component.ts', status: 'implemented' },
  { id: 'MA-002', personaId: 'ma', role: 'Medical Assistant', phase: 'daily', story: 'Check in patients with single button click on schedule table', acceptanceCriteria: 'Check-in button per row; status updates; stat cards update; timestamp recorded', component: 'ma-dashboard.component.ts → Check-in', status: 'implemented' },
  { id: 'MA-003', personaId: 'ma', role: 'Medical Assistant', phase: 'daily', story: 'Record patient vitals from the schedule table', acceptanceCriteria: 'Vitals button opens entry; signs recorded; status updates', component: 'ma-dashboard.component.ts → Vitals', status: 'implemented' },
  { id: 'MA-004', personaId: 'ma', role: 'Medical Assistant', phase: 'daily', story: 'Mark room preparation complete for each appointment', acceptanceCriteria: 'Room prep button; Rooms Ready stat increments; visual indicator changes', component: 'ma-dashboard.component.ts → Room Prep', status: 'implemented' },
  { id: 'MA-005', personaId: 'ma', role: 'Medical Assistant', phase: 'daily', story: 'See pre-visit care gap alerts with per-patient expandable cards', acceptanceCriteria: 'Expandable cards with gaps, urgency, days overdue, recommended actions', component: 'pre-visit-planning.component.ts', status: 'implemented' },
  { id: 'MA-006', personaId: 'ma', role: 'Medical Assistant', phase: 'daily', story: 'Preparation summary: total patients, total gaps, prepared count, high urgency', acceptanceCriteria: 'Summary bar with 4 computed counts; updated as patients are marked ready', component: 'pre-visit-planning.component.ts → summary', status: 'implemented' },
  // Care Coordinator
  { id: 'CC-001', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'daily', story: 'Filter care gaps by urgency, type, and days overdue', acceptanceCriteria: 'Filter controls for urgency/type/overdue; patient search; paginated results', component: 'care-gap-manager.component.ts → filter', status: 'implemented' },
  { id: 'CC-002', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'daily', story: 'See summary stats: total gaps, urgency counts, type distribution', acceptanceCriteria: 'Stats dashboard with total, urgency, type breakdown; real-time updates', component: 'CareGapStatsDashboardComponent', status: 'implemented' },
  { id: 'CC-003', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'daily', story: 'Quick-action dialogs for interventions: call, email, appointment, referral, note', acceptanceCriteria: 'Quick action buttons per gap; dialog per type; saves to gap record', component: 'care-gap-manager.component.ts → Quick Actions', status: 'implemented' },
  { id: 'CC-004', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'daily', story: 'Bulk operations: bulk closure, assignment, mass communications', acceptanceCriteria: 'Checkbox selection; select-all; bulk action dialog; progress indicator', component: 'care-gap-manager.component.ts → Bulk Actions', status: 'implemented' },
  { id: 'CC-005', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'daily', story: 'Intervention recommendations with ROI metrics', acceptanceCriteria: 'Per-intervention: success rate, cost, time to close, ROI multiplier', component: 'care-gap-manager.component.ts → Recommendations', status: 'implemented' },
  { id: 'CC-006', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'daily', story: 'Export care gap data as CSV', acceptanceCriteria: 'Export button generates CSV with all visible gaps; download triggers', component: 'care-gap-manager.component.ts → CSV export', status: 'implemented' },
  { id: 'CC-007', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'Create outreach campaigns by selecting measures and channels', acceptanceCriteria: 'Campaign wizard with measure selection; 5 intervention types; cost/success shown', component: 'outreach-campaigns.component.ts', status: 'implemented' },
  { id: 'CC-008', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'See cost-per-unit and success rate for each outreach channel', acceptanceCriteria: '5 channels with unit cost and success rate; projected cost computed', component: 'outreach-campaigns.component.ts → intervention config', status: 'implemented' },
  { id: 'CC-009', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'Track campaign lifecycle: Draft → Scheduled → In Progress → Completed', acceptanceCriteria: 'Status badge; transitions tracked; in-progress shows real-time metrics', component: 'outreach-campaigns.component.ts → lifecycle', status: 'implemented' },
  { id: 'CC-010', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'Export campaign data in CSV, HL7, and PDF formats', acceptanceCriteria: 'Export dropdown with 3 formats; CSV for analysis, HL7 for EHR, PDF for presentations', component: 'outreach-campaigns.component.ts → export', status: 'implemented' },
  { id: 'CC-011', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'See AI-predicted care gaps with risk tiers and prediction factors', acceptanceCriteria: 'Predicted gaps with tier badge; factor breakdown; intervention success rate', component: 'predictive-care-gap.service.ts', status: 'implemented' },
  { id: 'CC-012', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'See population risk pyramids with proportional tier bars', acceptanceCriteria: 'Risk Pyramid: 4 tiers with counts and proportional bars; clickable', component: 'insights.component.ts → Risk Pyramid', status: 'implemented' },
  { id: 'CC-013', personaId: 'care-coordinator', role: 'Care Coordinator', phase: 'weekly', story: 'AI-suggested actions linked to each insight', acceptanceCriteria: 'Insight cards with 1–3 suggested actions; estimated impact per action', component: 'insights.component.ts → suggested actions', status: 'implemented' },
  // CMO
  { id: 'CMO-001', personaId: 'cmo', role: 'CMO', phase: 'daily', story: 'See 4 executive KPI cards with trend arrows for 30-second health check', acceptanceCriteria: '4 KPI cards with values and trend direction; threshold alerts', component: 'cmo-onboarding.component.ts → KPI cards', status: 'implemented' },
  { id: 'CMO-002', personaId: 'cmo', role: 'CMO', phase: 'daily', story: 'Governance signals: active users, SLA adherence, last audit, escalations', acceptanceCriteria: '4 metrics with status indicators; escalation count highlighted if >0', component: 'cmo-onboarding.component.ts → Governance', status: 'implemented' },
  { id: 'CMO-003', personaId: 'cmo', role: 'CMO', phase: 'daily', story: 'Recommended top actions for decisions needing attention', acceptanceCriteria: '3–5 prioritized recommendations with type, description, urgency, links', component: 'cmo-onboarding.component.ts → Top Actions', status: 'implemented' },
  { id: 'CMO-004', personaId: 'cmo', role: 'CMO', phase: 'weekly', story: 'See Star Rating with domain-level breakdown', acceptanceCriteria: 'Overall rating with domain scores; expandable to individual measures', component: 'star-ratings.component.ts', status: 'implemented' },
  { id: 'CMO-005', personaId: 'cmo', role: 'CMO', phase: 'weekly', story: 'Simulate gap closure scenarios with projected Star Rating impact', acceptanceCriteria: 'Simulation mode; rating recalculates in real-time; ROI estimate', component: 'star-ratings.component.ts → simulation', status: 'implemented' },
  { id: 'CMO-006', personaId: 'cmo', role: 'CMO', phase: 'monthly', story: 'Generate quality reports with compliance trend charts', acceptanceCriteria: 'Compliance trend line and measure performance bars; PDF/CSV export', component: 'reports.component.ts', status: 'implemented' },
  { id: 'CMO-007', personaId: 'cmo', role: 'CMO', phase: 'monthly', story: 'Compare measure performance with demographic breakdowns', acceptanceCriteria: 'Side-by-side measures with pass rate, population, age/gender/payer', component: 'measure-comparison.component.ts', status: 'implemented' },
  // Quality Director
  { id: 'QD-001', personaId: 'quality-director', role: 'Quality Director', phase: 'daily', story: 'Browse 50+ HEDIS/CMS quality measures organized by 12 categories', acceptanceCriteria: 'Measure library with all measures; category filter; search', component: 'quality-measures.component.ts', status: 'implemented' },
  { id: 'QD-002', personaId: 'quality-director', role: 'Quality Director', phase: 'daily', story: 'View detailed measure specifications including CQL logic and criteria', acceptanceCriteria: 'Measure detail page with full spec, CQL preview, historical performance', component: 'quality-measure-detail.component.ts', status: 'implemented' },
  { id: 'QD-003', personaId: 'quality-director', role: 'Quality Director', phase: 'daily', story: 'Run single-patient and batch CQL evaluations with progress tracking', acceptanceCriteria: 'Evaluations page: single + bulk mode; progress bar; results available', component: 'evaluations.component.ts', status: 'implemented' },
  { id: 'QD-004', personaId: 'quality-director', role: 'Quality Director', phase: 'weekly', story: 'See per-measure analytics with demographic breakdowns', acceptanceCriteria: 'Measure analytics: pass rate, time, population breakdown, trends', component: 'measure-analytics.service.ts', status: 'implemented' },
  { id: 'QD-005', personaId: 'quality-director', role: 'Quality Director', phase: 'weekly', story: 'Manage care gap assignments: assign, prioritize, track closure', acceptanceCriteria: 'Assignment UI; coordinator selection; priority setting; tracking', component: 'care-gap-manager.component.ts → assignment', status: 'implemented' },
  { id: 'QD-006', personaId: 'quality-director', role: 'Quality Director', phase: 'quarterly', story: 'Export QRDA-formatted data for CMS/HEDIS submission', acceptanceCriteria: 'QRDA export generates CMS-compliant XML; validates against schema', component: 'qrda-export.service.ts', status: 'implemented' },
  { id: 'QD-007', personaId: 'quality-director', role: 'Quality Director', phase: 'quarterly', story: 'Review historical Star Rating snapshots for compliance audit', acceptanceCriteria: 'Trend view with historical snapshots; rollover shows values; exportable', component: 'star-ratings.component.ts → historical', status: 'implemented' },
  // Quality Analyst
  { id: 'QA-001', personaId: 'analyst', role: 'Quality Analyst', phase: 'daily', story: 'Configure evaluation parameters with AI-assisted suggestions', acceptanceCriteria: 'Evaluation form with AI suggestions; validation before execution', component: 'evaluations.component.ts → AI assistant', status: 'implemented' },
  { id: 'QA-002', personaId: 'analyst', role: 'Quality Analyst', phase: 'daily', story: 'See data flow visualization of evaluation pipeline', acceptanceCriteria: 'Pipeline stages with data volume indicators; real-time during execution', component: 'evaluations.component.ts → DataFlowVisualization', status: 'implemented' },
  { id: 'QA-003', personaId: 'analyst', role: 'Quality Analyst', phase: 'daily', story: 'Favorite measures and persist filter preferences across sessions', acceptanceCriteria: 'Star/favorite toggle; favorites first; filter state saved', component: 'evaluations.component.ts → favorites', status: 'implemented' },
  { id: 'QA-004', personaId: 'analyst', role: 'Quality Analyst', phase: 'daily', story: 'Review evaluation results with compliance rate and outcome breakdown', acceptanceCriteria: 'Results table sortable by date, patient, measure; compliance rate; category filter', component: 'results.component.ts', status: 'implemented' },
  { id: 'QA-005', personaId: 'analyst', role: 'Quality Analyst', phase: 'weekly', story: 'Build custom report templates with drag-and-drop sections', acceptanceCriteria: 'Add/remove/reorder sections; template save/load; scheduled generation', component: 'custom-report-builder.component.ts', status: 'implemented' },
  { id: 'QA-006', personaId: 'analyst', role: 'Quality Analyst', phase: 'weekly', story: 'Generate reports on demand or on a schedule', acceptanceCriteria: 'Generate button; scheduling interface; delivery notification', component: 'reports.component.ts', status: 'implemented' },
  // IT Admin
  { id: 'IT-001', personaId: 'it-admin', role: 'IT Administrator', phase: 'daily', story: 'View system health for all microservices with UP/DOWN status', acceptanceCriteria: 'Service grid with status badges; health check polling; alert on change', component: 'Admin Portal → system-health.component.ts', status: 'implemented' },
  { id: 'IT-002', personaId: 'it-admin', role: 'IT Administrator', phase: 'daily', story: 'View real-time metrics: request rates, error rates, latency', acceptanceCriteria: 'Live updating charts; configurable time ranges; threshold alerts', component: 'Admin Portal → real-time-metrics.component.ts', status: 'implemented' },
  { id: 'IT-003', personaId: 'it-admin', role: 'IT Administrator', phase: 'daily', story: 'Manage users: create, update, delete, assign roles, reset passwords', acceptanceCriteria: 'User CRUD with role assignment from 13-role RBAC; status toggle; search', component: 'Admin Portal → users.component.ts', status: 'implemented' },
  { id: 'IT-004', personaId: 'it-admin', role: 'IT Administrator', phase: 'daily', story: 'Manage tenant configurations, feature flags, thresholds', acceptanceCriteria: 'Tenant CRUD; feature flag toggles; isolation verification; config preview', component: 'Admin Portal → tenants.component.ts', status: 'implemented' },
  { id: 'IT-005', personaId: 'it-admin', role: 'IT Administrator', phase: 'weekly', story: 'Review enhanced audit logs with search, filter, and export', acceptanceCriteria: 'Full-text search; filter by user/action/resource/date; CSV export; pagination', component: 'Admin Portal → audit-logs-enhanced.component.ts', status: 'implemented' },
  { id: 'IT-006', personaId: 'it-admin', role: 'IT Administrator', phase: 'weekly', story: 'View configuration version history with diffs', acceptanceCriteria: 'Version list with timestamps; diff view between versions; rollback action', component: 'Admin Portal → config-versions.component.ts', status: 'implemented' },
  { id: 'IT-007', personaId: 'it-admin', role: 'IT Administrator', phase: 'weekly', story: 'Seed demo data for sales demonstrations and training', acceptanceCriteria: 'Seeding UI with progress; produces 200 patients, 56 care gaps, 29K+ observations', component: 'admin-demo-seeding.component.ts', status: 'implemented' },
  { id: 'IT-008', personaId: 'it-admin', role: 'IT Administrator', phase: 'weekly', story: 'Author custom CQL quality measures using Measure Builder', acceptanceCriteria: 'CQL editor; metadata entry; FHIR validation; test execution; publish', component: 'measure-builder.component.ts', status: 'implemented' },
  { id: 'IT-009', personaId: 'it-admin', role: 'IT Administrator', phase: 'weekly', story: 'Configure AI agents using Agent Builder', acceptanceCriteria: 'Configuration editor; parameter tuning; test execution; deployment', component: 'agent-builder.component.ts', status: 'implemented' },
  // Compliance Auditor
  { id: 'AUD-001', personaId: 'auditor', role: 'Compliance Auditor', phase: 'daily', story: 'Review all PHI access events via QA Audit Dashboard', acceptanceCriteria: 'All PHI access events; filterable by user, resource, action, date; 100% coverage', component: 'qa-audit-dashboard.component.ts', status: 'implemented' },
  { id: 'AUD-002', personaId: 'auditor', role: 'Compliance Auditor', phase: 'daily', story: 'Review clinical audit events: patient access, care gap mods, evaluations', acceptanceCriteria: 'Clinical-specific events; action categorization; anomaly detection', component: 'clinical-audit-dashboard.component.ts', status: 'implemented' },
  { id: 'AUD-003', personaId: 'auditor', role: 'Compliance Auditor', phase: 'daily', story: 'Review MPI audit events: matching decisions, merge actions', acceptanceCriteria: 'Identity events; match quality metrics; merge history', component: 'mpi-audit-dashboard.component.ts', status: 'implemented' },
  { id: 'AUD-004', personaId: 'auditor', role: 'Compliance Auditor', phase: 'monthly', story: 'Automatic audit trail coverage for 100% of backend API calls', acceptanceCriteria: 'HTTP interceptor captures all calls; resource type, action, user, tenant, timestamp', component: 'audit.interceptor.ts', status: 'implemented' },
  { id: 'AUD-005', personaId: 'auditor', role: 'Compliance Auditor', phase: 'monthly', story: 'Audit logging for session timeouts: automatic vs explicit logout', acceptanceCriteria: 'Reason, idle duration, warning shown, user ID, timestamp recorded', component: 'app.ts → session timeout audit', status: 'implemented' },
  { id: 'AUD-006', personaId: 'auditor', role: 'Compliance Auditor', phase: 'monthly', story: 'Export audit trails for specified date ranges and resource types', acceptanceCriteria: 'Export by date range, user, resource; CSV and PDF formats', component: 'audit-log.service.ts → export', status: 'implemented' },
  // Cross-Role
  { id: 'XR-001', personaId: 'provider', role: 'All Users', phase: 'cross-role', story: '15-minute idle timeout with 2-minute warning and Stay Logged In option', acceptanceCriteria: 'Activity listeners; 2-min warning dialog; automatic logoff; audit logged', component: 'app.ts → session timeout', status: 'implemented' },
  { id: 'XR-002', personaId: 'provider', role: 'All Users', phase: 'cross-role', story: 'JWT-based authentication through API gateway', acceptanceCriteria: 'Login → Gateway validates JWT → trusted headers → service authorizes', component: 'auth.service.ts', status: 'implemented' },
  { id: 'XR-003', personaId: 'it-admin', role: 'All Users', phase: 'cross-role', story: 'Register a new tenant organization', acceptanceCriteria: 'Registration form; tenant ID generated; database created; admin provisioned', component: 'tenant-registration.component.ts', status: 'implemented' },
  { id: 'XR-004', personaId: 'provider', role: 'All Users', phase: 'cross-role', story: 'Ask AI assistant about HEDIS measures, CQL logic, and quality workflows', acceptanceCriteria: 'AI chat interface; context-aware responses; code examples; measure explanations', component: 'ai-dashboard.component.ts', status: 'implemented' },
  { id: 'XR-005', personaId: 'provider', role: 'All Users', phase: 'cross-role', story: 'Access knowledge base with role-specific articles across 7 categories', acceptanceCriteria: 'Category navigation; role-based visibility; markdown rendering; helpfulness ratings', component: 'knowledge-base.component.ts', status: 'implemented' },
  { id: 'XR-006', personaId: 'analyst', role: 'All Users', phase: 'cross-role', story: 'View quality data through 4 3D visualizations', acceptanceCriteria: '4 WebGL-powered views; interactive navigation; near real-time refresh', component: 'visualization-layout.component.ts', status: 'implemented' },
  { id: 'XR-007', personaId: 'provider', role: 'All Users', phase: 'cross-role', story: 'Application errors handled gracefully without crashes', acceptanceCriteria: 'Global error handler; friendly messages; PHI filtering; app does NOT crash', component: 'Global Error Handler', status: 'implemented' },
  { id: 'XR-008', personaId: 'auditor', role: 'All Users', phase: 'cross-role', story: 'All logs filtered for PHI before output', acceptanceCriteria: 'LoggerService filters all output; ESLint no-console; build fails if console.log', component: 'logger.service.ts', status: 'implemented' },
]

// ── Value Metrics ────────────────────────────────────────────────────────────

export interface ValueMetric {
  personaId: PersonaId
  without: string
  with: string
  improvement: string
  keyDriver: string
}

export const VALUE_METRICS: ValueMetric[] = [
  { personaId: 'cmo', without: 'Quality data available quarterly; surprises common', with: 'Real-time KPI dashboard with daily Star Rating projection', improvement: '90-day faster visibility', keyDriver: 'CMO Scorecard + Star Ratings' },
  { personaId: 'quality-director', without: 'Manual chart review; 2-week submission prep', with: 'Automated CQL evaluation + one-click QRDA export', improvement: '40+ hours saved per quarter', keyDriver: 'Evaluations + Reports' },
  { personaId: 'care-coordinator', without: 'Spreadsheet tracking; no ROI visibility', with: 'AI-prioritized triage + ROI-calculated interventions', improvement: '15–20% more gaps closed', keyDriver: 'Care Gap Manager + Outreach Campaigns' },
  { personaId: 'provider', without: 'Quality gaps invisible during encounters', with: 'Gap indicators in schedule + in-visit closure', improvement: '2–5 additional gaps per week', keyDriver: 'Provider Dashboard' },
  { personaId: 'rn', without: 'Generic task list; separate systems', with: 'Purpose-built nursing dashboard + integrated workflows', improvement: 'Shift start → action in <2 min', keyDriver: 'RN Dashboard (5 quick actions)' },
  { personaId: 'ma', without: 'Whiteboard tracking; no gap awareness', with: 'Real-time schedule + pre-visit gap staging', improvement: '100% pre-visit gap visibility', keyDriver: 'MA Dashboard + Pre-Visit Planning' },
  { personaId: 'analyst', without: 'Manual data extraction; ad-hoc reports', with: '12-category engine + template reporting', improvement: 'Days → hours; hours → minutes', keyDriver: 'Evaluations + Custom Report Builder' },
  { personaId: 'it-admin', without: 'Multi-tool monitoring; manual provisioning', with: 'Single-pane admin with real-time metrics', improvement: 'One platform for all ops', keyDriver: 'Admin Portal (8 views)' },
  { personaId: 'auditor', without: 'Manual trail assembly; screenshots', with: '100% auto API audit + evidence export', improvement: '2 weeks → 2 hours', keyDriver: '3 Audit Dashboards + HTTP Interceptor' },
]

// ── Platform Stats ───────────────────────────────────────────────────────────

export const PLATFORM_STATS = {
  userStories: 80,
  implemented: 80,
  roles: 9,
  permissions: 31,
  pages: 35,
  services: 60,
  microservices: 51,
  databases: 29,
  measures: 50,
  measureCategories: 12,
} as const

// ── Differentiators ──────────────────────────────────────────────────────────

export const DIFFERENTIATORS = [
  { title: '4 Role-Adaptive Dashboards', description: 'MA, RN, Provider, and Admin each see a completely different UI — not just filtered views. HDIM rebuilds the experience per role.' },
  { title: 'AI-Powered Population Insights', description: 'Risk pyramids, predicted care gaps with weighted factor models, and suggested actions. Not just a risk score — a predictive model with transparent factors.' },
  { title: 'ROI-Calculated Outreach', description: 'Every intervention channel shows cost-per-unit, success rate, and ROI multiplier. Optimize your outreach budget in real-time.' },
  { title: '3D Quality Visualizations', description: 'Live Batch Monitor, Quality Constellation, Flow Network, Measure Matrix — 4 WebGL-powered views for executive presentations.' },
  { title: 'Drag-and-Drop Provider Dashboard', description: 'Providers customize their daily view by reordering sections with keyboard shortcuts. Clinician adoption increases with workspace control.' },
  { title: 'Embedded AI Assistant', description: 'Context-aware AI that understands HEDIS measures, CQL logic, and clinical quality workflows. Not a generic chatbot — domain-trained.' },
  { title: '50+ HEDIS/CMS Measures', description: 'Purpose-built CQL measure library across 12 clinical categories. Out-of-the-box coverage versus months of custom CQL development.' },
  { title: 'HIPAA-Native Architecture', description: '100% API audit coverage, 15-min timeout, PHI-filtered logging, no-cache headers. Compliance is architectural, not bolt-on.' },
]
