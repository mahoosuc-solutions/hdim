# Phase 1 Pilot Scope of Work (SOW)

Prepared: 2026-02-23
Pilot window: 90 calendar days from kickoff
Target population: 200-500 hospital discharges

## 1) Scope

In scope:

1. Post-discharge patient engagement workflow.
2. AI layman summary generation from curated clinical context.
3. Patient Q&A with guardrails and escalation routing.
4. Secure clinician notification path (in-platform and secure email option).
5. KPI dashboard and weekly pilot reporting.
6. Security baseline and audit evidence package.
7. Deployment model selection and environment-specific hardening plan.

Out of scope for Phase 1:

1. Full bi-directional Epic MyChart write-back in production.
2. Broad payer-wide rollout beyond pilot cohort.
3. New claims or UM core-system replacement.

## 2) Deliverables

1. Pilot deployment and tenant configuration.
2. Patient engagement API and clinician escalation APIs.
3. AI service controls for tenant-aware, low-retention processing.
4. Pilot metrics pack:
   - Engagement rate
   - Median first response time
   - Comprehension confirmation rate
   - Escalation rate and time to resolution
5. Pilot closeout readout with scale recommendation.

## 3) Success Criteria

Minimum success criteria for pilot completion:

1. 200+ discharges processed through the workflow.
2. 99.5%+ platform availability during pilot hours.
3. At least 20% relative lift in patient engagement versus baseline cohort.
4. At least 15% relative improvement in comprehension confirmation score.
5. No critical security incident or reportable PHI breach.

## 4) Timeline

1. Weeks 1-2: Discovery hardening, data mapping, security controls finalization.
2. Weeks 3-5: Build and integration sprint.
3. Weeks 6-7: UAT, workflow tuning, staff readiness.
4. Weeks 8-13: Live pilot execution and weekly optimization.

## 5) Governance

1. Weekly clinical + operations steering call.
2. Weekly KPI and risk review.
3. Formal go/no-go gates at end of Weeks 2, 7, and 13.

## 6) Deployment Model Selection (Required)

One model must be selected during Week 1:

1. HDIM-hosted.
2. Customer-hosted cloud.
3. On-prem/private data center (optional air-gapped variant).

The selected model drives:

1. Final timeline and mobilization sequence.
2. Shared responsibility matrix for security and operations.
3. Final commercial estimate and contract language.
