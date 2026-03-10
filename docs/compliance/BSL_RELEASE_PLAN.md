# Business Source License (BSL) Release Plan

Last updated: 2026-03-09
Owner: Platform + Compliance + GTM
Status: **EXECUTED** — BSL 1.1 live on master, Licensor: Grateful House Inc.

## Objective

Release HDIM under a Business Source License (BSL) with clear conversion terms, enforceable usage boundaries, and a public transparency trail in the web portal.

## Scope

- In scope:
  - BSL policy and repository-level licensing files.
  - Public transparency pages that explain what is open, restricted, and customer-supplied.
  - Links between legal/compliance documents and externally shared web content.
- Out of scope:
  - Legal advice or jurisdiction-specific counsel outcomes.
  - Changes to customer MSA/BAA templates outside legal review workflows.

## Workstreams and Exit Criteria

## 1) Legal and Policy Baseline

- Tasks:
  - Finalize BSL parameters:
    - Licensor.
    - Additional Use Grant.
    - Change Date.
    - Change License (post-conversion).
  - Add root licensing artifacts:
    - `LICENSE` (BSL text + project-specific parameters).
    - `CHANGELOG` entry for license transition.
    - `NOTICE` (if required by counsel).
  - Confirm compatibility with `docs/compliance/LICENSING-BOUNDARY.md`.
- Exit criteria:
  - Counsel-approved BSL terms are committed.
  - Repository has unambiguous top-level license metadata.

## 2) Packaging and Distribution Controls

- Tasks:
  - Separate “open repository content” from “customer-licensed content” artifacts.
  - Validate that no controlled content is bundled in public artifacts.
  - Add CI check to fail release if controlled content paths are detected in publish bundles.
- Exit criteria:
  - Automated checks pass and block prohibited distribution.
  - Evidence of the check is retained in release artifacts.

## 3) Web Portal Transparency

- Tasks:
  - Publish public licensing transparency page:
    - `/resources/licensing`
  - Link to core proof pages:
    - `/resources/trust-center`
    - `/resources/evidence-room`
    - `/resources/procurement`
    - `/terms`
  - Link to repository-source compliance docs:
    - `docs/compliance/LICENSING-BOUNDARY.md`
    - `docs/compliance/THIRD_PARTY_NOTICES.md`
    - this plan (`docs/compliance/BSL_RELEASE_PLAN.md`)
- Exit criteria:
  - Buyer can navigate from Resources to licensing and compliance proof in two clicks.
  - Public pages state “not legal advice” and identify where custom terms apply.

## 4) Operational Runbooks and Agent Standards

- Tasks:
  - Add runbook checklist for every release:
    - license file presence.
    - third-party notices updated.
    - controlled content scan passed.
    - transparency links validated.
  - Add agent policy requirement:
    - any externally shared page must include licensing/compliance link path.
- Exit criteria:
  - Runbook is used in release reviews.
  - Agent-generated web pages consistently include compliance linkage.

## Web Transparency Link Map

Use this map when sharing the portal publicly.

- Public entrypoint:
  - `/resources`
- Licensing transparency:
  - `/resources/licensing`
- Trust and diligence:
  - `/resources/trust-center`
  - `/resources/evidence-room`
  - `/resources/cio-ciso`
  - `/resources/procurement`
- Legal terms:
  - `/terms`
- Repository compliance references:
  - `https://github.com/mahoosuc-solutions/hdim/blob/master/docs/compliance/LICENSING-BOUNDARY.md`
  - `https://github.com/mahoosuc-solutions/hdim/blob/master/docs/compliance/THIRD_PARTY_NOTICES.md`
  - `https://github.com/mahoosuc-solutions/hdim/blob/master/docs/compliance/BSL_RELEASE_PLAN.md`

## Risks and Controls

- Risk: BSL language conflicts with existing commercial terms.
  - Control: Counsel sign-off gate before merge to release branch.
- Risk: Controlled content accidentally exposed.
  - Control: Automated path/pattern checks in CI and pre-release validation.
- Risk: Buyer confusion about open vs licensed components.
  - Control: Dedicated transparency page and role-specific diligence links.

## Execution Record

| Step | Status | Date |
|------|--------|------|
| Finalize BSL parameters | ✅ Done | 2026-03-09 |
| Add root LICENSE + NOTICE | ✅ Done | 2026-03-09 |
| Transfer Licensor to Grateful House Inc. | ✅ Done | 2026-03-09 |
| Set Change Date to 2030-03-07 (4-year) | ✅ Done | 2026-03-09 |
| Update Terms of Service | ✅ Done | 2026-03-09 |
| Update README licensing section | ✅ Done | 2026-03-09 |
| Publish /resources/licensing page | ✅ Done | 2026-03-09 |
| Tag v3.0.0 release | ✅ Done | 2026-03-09 |
| CI release gate for controlled content | ✅ Done | 2026-03-10 |
| Validate portal transparency links in production | ✅ Done | 2026-03-10 |

## Operational Validation Evidence (2026-03-10)

- CI workflow added:
  - `.github/workflows/bsl-launch-ops-gate.yml`
- Validation script added:
  - `scripts/validation/validate-bsl-launch-ops.sh`
- Validation artifacts:
  - `validation-reports/bsl-launch-ops-summary.md`
  - `validation-reports/bsl-launch-ops-summary.json`
- Launch checklist evidence report:
  - `docs/compliance/BSL_LAUNCH_OPERATIONAL_VALIDATION_2026-03-10.md`
