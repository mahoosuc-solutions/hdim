export const METRICS = {
  services: 59,
  testClasses: 1171,
  testMethods: '8,000+',
  apiEndpoints: 62,
  hedisMeasures: '80+',
  evaluationSpeed: '<2 seconds',
  deploymentTime: '90 days',
  databases: 29,
  annualSavings: '$722K',
  roi: '1,720%',
  paybackDays: 22,
} as const

export const CAPABILITIES = [
  { id: 'quality-engine', title: 'Quality Measure Engine', description: '80+ HEDIS measures with real-time CQL evaluation', detail: 'Direct CQL execution on FHIR R4 resources. Under 2-second evaluation per patient. Population-level batch processing with 10 concurrent threads.', isNew: false },
  { id: 'care-gaps', title: 'Care Gap Detection', description: 'Automated identification and closure workflows', detail: 'Real-time detection as clinical events occur. Closure workflows for clinical teams. Patient engagement integration.', isNew: false },
  { id: 'revenue-cycle', title: 'Revenue Cycle', description: 'Claims, remittance, price transparency', detail: 'Wave-1: claims processing, remittance reconciliation (ERA/835), price transparency APIs, ADT event handling.', isNew: true, badge: 'Wave-1' },
  { id: 'fhir', title: 'FHIR R4 Interoperability', description: 'Native integration with Epic, Cerner, Athena', detail: '100% FHIR attribute preservation. No ETL translation loss. 62 documented API endpoints.', isNew: false },
  { id: 'measure-builder', title: 'Custom Measure Builder', description: 'Create and deploy custom quality measures via UI', detail: 'Health plans can build proprietary measures without engineering. 7 configurable metadata fields.', isNew: true, badge: 'New' },
  { id: 'cmo-onboarding', title: 'CMO Onboarding', description: 'Dashboard workflows and acceptance playbooks', detail: 'Structured evaluation-to-adoption path for health plan CMOs. Validation hooks at each milestone.', isNew: true, badge: 'New' },
  { id: 'clinical-portal', title: 'Clinical Portal', description: 'Clinical workspace with operations dashboards', detail: 'HIPAA-compliant frontend. PHI filtering, session timeout, 100% API audit coverage.', isNew: false },
  { id: 'operations', title: 'Operations Orchestration', description: '16-class gateway framework for enterprise operations', detail: 'Standardized security, rate limiting, multi-tenancy across 4 API gateways.', isNew: true, badge: 'New' },
  { id: 'security', title: 'Security & Compliance', description: 'HIPAA engineered, CVE remediated, ZAP scanned', detail: 'CVE remediation with burn-down tracking. ZAP scanning on every PR. 360 assurance with evidence sign-off.', isNew: false },
] as const

export type Capability = typeof CAPABILITIES[number]

export interface Role {
  title: string
  pain: string
  solution: string
}

export interface SegmentROI {
  headline: string
  savings: string
  payback: string
  metric: string
}

export interface Segment {
  label: string
  slug: string
  tagline: string
  description: string
  roles: Role[]
  capabilities: string[]
  roi: SegmentROI
}

export const SEGMENTS: Record<string, Segment> = {
  healthPlans: {
    label: 'Health Plans',
    slug: 'health-plans',
    tagline: 'Close care gaps before they cost you',
    description: 'Medicare Advantage, Commercial, Medicaid managed care',
    roles: [
      { title: 'CMO / Medical Director', pain: 'HEDIS scores stagnating while quality bonuses slip away. No visibility into real-time measure performance.', solution: 'Real-time dashboards showing measure performance across your population. Custom measure creation for proprietary quality programs.' },
      { title: 'Quality Director', pain: 'Manual HEDIS calculations consuming 40-60% of your team\'s time. Specs change, you start over.', solution: '80+ HEDIS measures auto-evaluated in <2 seconds. Care gap detection triggers same-day interventions.' },
      { title: 'CFO / Finance', pain: '$500K-2M revenue loss per 1% HEDIS compliance gap. Revenue cycle and quality on separate platforms.', solution: '$722K annual savings. Integrated revenue cycle (claims, remittance, price transparency) on the same platform.' },
      { title: 'IT / CTO', pain: 'EHR integrations taking 18-24 months. Security compliance is a manual process.', solution: '90-day deployment via FHIR R4. CVE-remediated, ZAP-scanned, HIPAA engineered -- not bolted on.' },
    ],
    capabilities: ['quality-engine', 'care-gaps', 'measure-builder', 'cmo-onboarding', 'revenue-cycle'],
    roi: { headline: '$500K-2M per 1% HEDIS gap', savings: '$722K annual savings', payback: '22-day payback', metric: '1,720% ROI' },
  },
  healthSystems: {
    label: 'Health Systems',
    slug: 'health-systems',
    tagline: 'Quality measurement that deploys in weeks, not years',
    description: 'Hospitals, IDNs, academic medical centers',
    roles: [
      { title: 'CMO / Medical Director', pain: 'Quality data arrives months after the intervention window closes. No way to act in real-time.', solution: 'Event-driven quality evaluation surfaces care gaps during patient visits -- not months later.' },
      { title: 'Quality Director', pain: 'Reporting burden growing while staff shrinks. Multiple disconnected quality tools.', solution: 'Single platform for quality measurement, care gaps, and reporting. 62 documented APIs for data integration.' },
      { title: 'CTO / IT Director', pain: 'Vendor implementations taking 18-24 months. Security audits consume engineering time.', solution: '90-day deployment. FHIR R4 native -- works with Epic, Cerner, Athena. Security evidence provided, not promised.' },
      { title: 'CFO', pain: 'Quality improvement programs with unclear ROI. Implementation costs in the millions.', solution: '$150-300K/year platform cost vs. $1-5M for Epic. 22-day payback period.' },
    ],
    capabilities: ['quality-engine', 'care-gaps', 'fhir', 'operations', 'security'],
    roi: { headline: '90 days vs. 18-24 months', savings: '10x faster deployment', payback: '$150-300K/yr vs. $1-5M', metric: '2.4x faster implementation' },
  },
  acos: {
    label: 'ACOs & Provider Groups',
    slug: 'acos',
    tagline: 'Population health visibility that drives shared savings',
    description: 'Accountable Care Organizations, physician groups, IPAs',
    roles: [
      { title: 'Quality Lead', pain: 'Care gap closure rates below shared savings targets. Only see gaps in retrospective reports.', solution: 'Real-time care gap detection. Same-day intervention triggers instead of quarterly reports.' },
      { title: 'IT Director', pain: 'Data scattered across multiple EHR systems. Integration projects never finish.', solution: 'FHIR R4 native -- connects to any EHR. Single data pipeline, 62 API endpoints.' },
      { title: 'Finance / Operations', pain: 'Shared savings at risk. Quality bonuses missed due to late gap identification.', solution: 'Automated quality measurement and real-time gap detection maximize shared savings capture.' },
      { title: 'Medical Director', pain: 'Providers don\'t have care gap information at the point of care.', solution: 'Real-time alerts surface care gaps during patient encounters.' },
    ],
    capabilities: ['quality-engine', 'care-gaps', 'fhir', 'clinical-portal', 'security'],
    roi: { headline: 'Maximize shared savings', savings: 'Real-time gap closure', payback: 'Same-day interventions', metric: 'Population-wide visibility' },
  },
} as const

export interface RoleScreenshot {
  src: string
  alt: string
  title: string
  description: string
}

export interface SegmentScreenshots {
  role: string
  screenshots: RoleScreenshot[]
}

export const SEGMENT_SCREENSHOTS: Record<string, SegmentScreenshots[]> = {
  healthPlans: [
    {
      role: 'CMO Dashboard',
      screenshots: [
        { src: '/images/screenshots/cmo/cmo-01-dashboard.jpg', alt: 'CMO Dashboard overview', title: 'Executive Dashboard', description: 'Real-time quality KPIs and measure performance' },
        { src: '/images/screenshots/cmo/cmo-02-quality-measures.jpg', alt: 'Quality measures list', title: 'Quality Measures', description: 'HEDIS measure library with compliance tracking' },
      ],
    },
    {
      role: 'Quality Analyst',
      screenshots: [
        { src: '/images/screenshots/quality-analyst/quality-analyst-04-run-evaluation.jpg', alt: 'Run evaluation', title: 'Measure Evaluation', description: 'One-click HEDIS measure evaluation' },
        { src: '/images/screenshots/quality-analyst/quality-analyst-06-results-pass-fail.jpg', alt: 'Results pass/fail', title: 'Evaluation Results', description: 'Pass/fail compliance scoring with drilldowns' },
      ],
    },
    {
      role: 'Data Analyst',
      screenshots: [
        { src: '/images/screenshots/data-analyst/data-analyst-02-risk-stratification.jpg', alt: 'Risk stratification', title: 'Risk Stratification', description: 'Population-level risk scoring and cohort analysis' },
        { src: '/images/screenshots/data-analyst/data-analyst-04-quality-constellation.jpg', alt: 'Quality constellation', title: 'Quality Constellation', description: 'Multi-dimensional measure visualization' },
      ],
    },
  ],
  healthSystems: [
    {
      role: 'Provider',
      screenshots: [
        { src: '/images/screenshots/provider/provider-01-pre-visit.jpg', alt: 'Pre-visit summary', title: 'Pre-Visit Summary', description: 'Care gaps and recommendations before patient arrives' },
        { src: '/images/screenshots/provider/provider-06-care-gaps.jpg', alt: 'Provider care gaps', title: 'Point-of-Care Gaps', description: 'Real-time care gap alerts during patient visit' },
      ],
    },
    {
      role: 'Care Manager',
      screenshots: [
        { src: '/images/screenshots/care-manager/care-manager-02-care-gaps-table.jpg', alt: 'Care gaps table', title: 'Care Gap Worklist', description: 'Priority-based worklist for care gap closure' },
        { src: '/images/screenshots/care-manager/care-manager-06-intervention-dialog.jpg', alt: 'Intervention dialog', title: 'Intervention Dialog', description: 'Document interventions and close gaps' },
      ],
    },
    {
      role: 'Admin',
      screenshots: [
        { src: '/images/screenshots/admin/admin-05-audit-logs.jpg', alt: 'Audit logs', title: 'Audit Logs', description: 'HIPAA-compliant audit trail for all PHI access' },
        { src: '/images/screenshots/admin/admin-03-user-management.jpg', alt: 'User management', title: 'User Management', description: 'Role-based access control and user provisioning' },
      ],
    },
  ],
  acos: [
    {
      role: 'Provider',
      screenshots: [
        { src: '/images/screenshots/provider/provider-01-pre-visit.jpg', alt: 'Pre-visit summary', title: 'Pre-Visit Summary', description: 'Care gaps surfaced before patient arrives' },
        { src: '/images/screenshots/provider/provider-07-care-recommendations.jpg', alt: 'Care recommendations', title: 'Care Recommendations', description: 'Evidence-based care recommendations at the point of care' },
      ],
    },
    {
      role: 'Care Manager',
      screenshots: [
        { src: '/images/screenshots/care-manager/care-manager-01-dashboard-overview.jpg', alt: 'Care manager dashboard', title: 'Care Manager Dashboard', description: 'Population overview with gap closure tracking' },
        { src: '/images/screenshots/care-manager/care-manager-05-gap-detail.jpg', alt: 'Gap detail', title: 'Gap Detail', description: 'Full clinical context for care gap closure' },
      ],
    },
    {
      role: 'Quality Analyst',
      screenshots: [
        { src: '/images/screenshots/quality-analyst/quality-analyst-01-measures-list.jpg', alt: 'Measures list', title: 'Measure Library', description: 'Full HEDIS measure library with evaluation status' },
        { src: '/images/screenshots/quality-analyst/quality-analyst-08-qrda-export.jpg', alt: 'QRDA export', title: 'QRDA Export', description: 'CMS-ready quality reporting export' },
      ],
    },
  ],
}

export const WIZARD_OPTIONS = {
  segments: [
    { id: 'health-plan', label: 'Health Plan', slug: 'health-plans', description: 'Medicare Advantage, Commercial, Medicaid' },
    { id: 'health-system', label: 'Health System', slug: 'health-systems', description: 'Hospital, IDN, academic medical center' },
    { id: 'aco', label: 'ACO / Provider Group', slug: 'acos', description: 'Accountable care, physician group, IPA' },
  ],
  roles: {
    'health-plan': [
      { id: 'cmo', label: 'CMO / Medical Director' },
      { id: 'quality-dir', label: 'Quality Director' },
      { id: 'cfo', label: 'CFO / Finance' },
      { id: 'cto', label: 'IT / CTO' },
    ],
    'health-system': [
      { id: 'cmo', label: 'CMO / Medical Director' },
      { id: 'quality-dir', label: 'Quality Director' },
      { id: 'cto', label: 'CTO / IT Director' },
      { id: 'cfo', label: 'CFO' },
    ],
    'aco': [
      { id: 'quality-lead', label: 'Quality Lead' },
      { id: 'it-dir', label: 'IT Director' },
      { id: 'finance', label: 'Finance / Operations' },
      { id: 'med-dir', label: 'Medical Director' },
    ],
  },
} as const
