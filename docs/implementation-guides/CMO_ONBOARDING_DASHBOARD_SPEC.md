# CMO Onboarding Dashboard Spec

**Product Area:** Clinical Portal  
**Route Proposal:** `/executive/cmo-onboarding`  
**Primary Users:** CMO, VP Quality, Population Health Director  
**Secondary Users:** CTO, Operations Leadership  
**Status:** Draft v1 (implementation-ready)

---

## 1. Objective

Provide a one-page executive dashboard that answers three questions during onboarding:

1. Are we clinically improving?
2. Are teams operationally adopting workflows?
3. Is data/compliance trust sufficient for scale?

---

## 2. UX Layout (Single Page)

## Header
- Title: `CMO Onboarding Command Center`
- Subtitle: `Clinical outcomes, operational adoption, and trust signals for days 0-90`
- Controls:
  - Date range (`Last 7/30/90 days`)
  - Cohort selector (`All`, `High Risk`, `Pediatrics`, `Custom`)
  - Site filter (`All sites` + multi-select)

## Row 1: Executive KPI Cards (8)
- Care Gap Closure Rate
- High-Risk Intervention Completion
- Top 3 Measure Compliance Composite
- Workflow Cycle Time
- Backlog Over SLA
- Data Freshness SLA
- Data Completeness
- Compliance Evidence Completion

Each card includes:
- Current value
- Delta vs baseline
- Trend sparkline
- Status color (`G|Y|R`)

## Row 2: Outcome And Adoption Panels
- Left: `Clinical Outcomes Trend`
  - 90-day trend chart for 3 core clinical KPIs
- Right: `Adoption Funnel`
  - Assigned -> In Progress -> Completed workflow funnel
  - Drop-off highlights by team/role

## Row 3: Operational Risk And Actions
- Left: `Top Risks And Escalations`
  - Open high-severity risks
  - Affected workflows, owner, due date
- Right: `CMO Action Queue`
  - Ranked recommendations (max 5)
  - Each action: expected impact, effort estimate, owner

## Footer: Trust And Evidence
- `Data Trust` mini-panel:
  - Last successful refresh timestamp
  - Data quality test pass rate
- `Compliance` mini-panel:
  - Audit log integrity status
  - Control evidence completeness

---

## 3. Data Model (View Contract)

```ts
interface CmoOnboardingDashboardView {
  period: { start: string; end: string };
  filters: { cohorts: string[]; sites: string[] };
  kpis: Array<{
    id: string;
    label: string;
    currentValue: number | string;
    baselineValue: number | string;
    delta: number;
    trendDirection: 'up' | 'down' | 'stable';
    status: 'green' | 'yellow' | 'red';
    unit?: string;
  }>;
  outcomesTrend: Array<{
    date: string;
    careGapClosureRate: number;
    highRiskInterventionCompletion: number;
    complianceComposite: number;
  }>;
  adoptionFunnel: {
    assigned: number;
    inProgress: number;
    completed: number;
    byRole: Array<{ role: string; assigned: number; completed: number }>;
  };
  risks: Array<{
    id: string;
    title: string;
    severity: 'low' | 'medium' | 'high' | 'critical';
    owner: string;
    dueDate: string;
    affectedWorkflow: string;
  }>;
  recommendedActions: Array<{
    id: string;
    title: string;
    expectedImpact: string;
    effort: 'low' | 'medium' | 'high';
    owner: string;
  }>;
  trustSignals: {
    lastDataRefreshUtc: string;
    dataQualityPassRate: number;
    auditIntegrityStatus: 'pass' | 'warn' | 'fail';
    evidenceCompletionRate: number;
  };
}
```

---

## 4. API Surface (Proposed)

- `GET /api/executive/cmo-onboarding/summary?start=&end=&sites=&cohorts=`
  - Returns `CmoOnboardingDashboardView`
- `GET /api/executive/cmo-onboarding/actions`
  - Returns ranked action queue
- `GET /api/executive/cmo-onboarding/export?format=pdf|csv`
  - Exports executive view snapshot

---

## 5. Acceptance Criteria

1. Dashboard loads in < 2.5s p95 for default filters.
2. KPI values reconcile to backend source-of-truth within defined tolerance.
3. Role-based access enforced (CMO/VP Quality/authorized executives only).
4. Export produces auditable, timestamped snapshot.
5. All displayed metrics include definition tooltip and baseline comparison.

---

## 6. Testing Strategy

## Unit Tests
- KPI status color logic and baseline delta calculations.
- Date and filter transformation logic.

## Integration Tests
- API contract tests for summary and actions endpoints.
- Data parity tests against reporting aggregates.

## E2E Tests
- Executive user opens page, applies filters, validates visible changes.
- Export flow success and audit event verification.
- Unauthorized user denied access.

## Non-Functional
- Performance baseline tests at peak dataset sizes.
- Accessibility checks (keyboard navigation, aria labels, color contrast).

---

## 7. Security And Compliance Notes

- Enforce least-privilege access via existing RBAC model.
- Do not expose row-level PHI in executive cards by default.
- Record dashboard access and export actions in audit logs.
- Include data provenance metadata for all KPI values.

---

## 8. Implementation Tasks

1. Add route and navigation entry for CMO onboarding dashboard.
2. Create page component and presentational widgets.
3. Implement executive data service + API integration layer.
4. Add KPI definition tooltips and export action.
5. Add unit/integration/e2e tests and accessibility checks.

