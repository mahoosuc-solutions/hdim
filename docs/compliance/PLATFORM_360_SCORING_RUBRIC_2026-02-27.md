# Platform 360 Scoring Rubric

**Date:** 2026-02-27  
**Purpose:** Quantify readiness for healthcare SaaS release assurance (functional, performance, HIPAA, SOC2, security).

## Scoring Scale (Per Control)

- `0` = Missing (no implementation and no evidence)
- `1` = Implemented, evidence missing or stale
- `2` = Implemented with current evidence, no owner sign-off
- `3` = Implemented with current evidence and owner sign-off

## Domain Weights

| Domain | Weight |
|---|---:|
| A Feature Completeness | 15% |
| B Data Model Integrity | 15% |
| C Performance/Real-Time Validation | 20% |
| D HIPAA Safeguards | 15% |
| E SOC2 Control Evidence | 15% |
| F Security Engineering | 15% |
| G/H Reliability + Multi-Tenant Governance | 5% |

## Grade Bands

| Score | Grade | Decision |
|---:|---|---|
| 90-100 | A | GO |
| 80-89 | B | Conditional GO |
| 70-79 | C | Conditional GO with remediation plan |
| <70 | D/F | NO GO |

## Hard Gates (Override Score)

Any one of these forces `NO GO`:

1. Unresolved critical/blocked HIPAA safeguard control.
2. Unresolved critical/blocked SOC2 CC evidence control.
3. Missing backend CVE evidence artifact for current release candidate (`#497`).
4. Missing named compliance sign-off in SOC2 matrix (`#498`).
5. Missing OWASP ZAP baseline artifact verification for security release evidence (`#500`).

## Computation Method

1. Score every control from the master checklist.
2. Compute each domain average on 0-3 scale.
3. Convert to 100-point weighted total:
   - `DomainPercent = (DomainAverage / 3.0) * 100`
   - `WeightedContribution = DomainPercent * DomainWeight`
4. Sum all weighted contributions.
5. Apply hard-gate override.

## Current Snapshot (2026-02-27)

- Domain-level provisional status from checklist:
  - High-confidence pass: A, B, D(partial), F(partial), G/H(partial)
  - In-progress/blocked: E-04, E-02, F-03
- Provisional decision: **NO GO** due hard-gate failure on backend CVE control (`#497`).

## Required Sign-offs

| Role | Required for GO |
|---|---|
| Security Engineering Lead | Yes |
| Compliance Lead | Yes |
| Platform/Release Lead | Yes |
| QA Lead | Yes |
