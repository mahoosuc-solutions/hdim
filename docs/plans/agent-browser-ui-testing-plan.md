# Agent-Browser UI Testing Plan (User Types + User Stories)

## Goal
Build a fully scripted, repeatable agent-browser test suite that exercises core UI workflows by user type and user story. The suite must be deterministic (seeded data), accessible (role-based navigation), and CI-ready with artifacts (screenshots, logs).

## Scope
- Clinical portal UI flows by user role
- Role-specific dashboards and core workflows
- Navigation, filtering, and action confirmation
- Context-aware navigation (care gaps, evaluations, results)

## Non-Goals (Phase 1)
- Deep visual regression across all screens
- Full parity with Playwright E2E
- Performance benchmarking

## Prerequisites
- Portal available at `http://localhost:18080` or configured via `PORTAL_URL`
- Demo data seeded via `scripts/seed-all-demo-data.sh`
- Optional: ensure auth is bypassed or automated via headers/cookies

## Selector Strategy
1. Prefer ARIA roles + accessible names.
2. Add `data-testid` for any element that is dynamic or ambiguous.
3. Avoid brittle selectors (DOM structure, CSS classes).

## Script Structure
```
scripts/agent-browser/
  README.md
  _common.sh                # helpers: open, wait, snapshot, assert
  nav-global.sh
  dashboard-admin.sh
  provider-care-gaps.sh
  provider-results.sh
  provider-measures.sh
  rn-care-gaps.sh
  rn-followups.sh
  ma-prep.sh
  ma-reminders.sh
  quality-trends.sh
  quality-reports.sh
```

## User Types and User Stories

### Admin
- Story: View system overview
  - Assertions: stat cards, trend chart, care gap summary visible
- Story: Navigate to core sections
  - Assertions: Patients, Care Gaps, Evaluations, Reports reachable
- Story: View compliance breakdown
  - Assertions: compliance report list and open report detail

### Provider (MD/DO/PA/NP)
- Story: Review high-priority care gaps
  - Assertions: urgent care gaps list, open detail, schedule/pre-visit action
- Story: Review pending results
  - Assertions: results list, open detail, patient context panel
- Story: Check measure performance
  - Assertions: measure table/cards, chart present

### Registered Nurse (RN)
- Story: Triage care gaps by urgency
  - Assertions: urgency filters, list updates, open patient
- Story: Coordinate follow-ups
  - Assertions: reminder/schedule actions, confirmation dialogs

### Medical Assistant (MA)
- Story: Prepare patient panel for visits
  - Assertions: patient list, open patient detail, care gap tab
- Story: Record reminders/next steps
  - Assertions: reminder UI, success message

### Quality/Analyst
- Story: Review performance trends
  - Assertions: trend chart, date range change updates data
- Story: Export or review reports
  - Assertions: report list, open report, export button visible

## Data Requirements
- Seeded patients, evaluations, measures, care gaps
- Consistent timestamps for trend assertions
- Known users/roles or deterministic role-switch UI

## CI Integration
- Job steps:
  1) Start demo stack
  2) Seed demo data
  3) Run smoke + per-role suites
  4) Upload screenshots and logs as artifacts

## Rollout Plan
1) Implement smoke + admin + provider care gaps suites
2) Add RN/MA suites
3) Add quality/analyst suites
4) Add CI wiring and optional visual regression

## Pass Criteria
- Each script completes without errors
- Core assertions pass for each story
- Screenshots captured per suite

## Ownership
- Frontend: selectors/testids
- QA: test scripts and assertions
- DevOps: CI wiring and artifacts
