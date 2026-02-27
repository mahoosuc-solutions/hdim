# SOC2, HIPAA, and CVE Hardening Backlog

**Date:** 2026-02-27  
**Status:** Active backlog for audit-grade readiness  
**Inputs:**
- `docs/compliance/SECURITY_COMPLIANCE_VALIDATION_2026-02-27.md`
- `docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_2026-02-27.md`
- `docs/compliance/HIPAA_SOC2_EVIDENCE_BUNDLE_INDEX_2026-02-27.md`
- Latest test stabilization commits (`9c16e483d`, `b31388379`)

## Prioritized Work Items

## P0-1: Complete backend CVE evidence with NVD data

- **Owner:** SWE (Backend Platform)
- **Priority:** P0
- **Objective:** Produce backend dependency vulnerability reports required for SOC2/enterprise diligence.
- **Execution:**
  - `cd backend && NVD_API_KEY=<key> ./gradlew dependencyCheckUpdate dependencyCheckAggregate --no-daemon`
- **Acceptance criteria:**
  - Dependency-Check completes successfully.
  - Artifacts exist and are archived in evidence package:
    - `backend/build/reports/dependency-check-report.html`
    - `backend/build/reports/dependency-check-report.json` (if enabled)
    - `backend/build/reports/dependency-check-report.sarif` (if enabled)
  - `docs/compliance/HIPAA_SOC2_EVIDENCE_BUNDLE_INDEX_2026-02-27.md` updated with final artifact links.
- **Current gap evidence:**
  - `test-results/gradle-dependency-check-2026-02-27-after-wire.log`
  - `test-results/gradle-dependency-check-aggregate-2026-02-27.log`
  - `test-results/gradle-dependency-check-aggregate-2026-02-27-offline.log`

## P0-2: Promote SOC2 control matrix to signed release artifact

- **Owner:** QA/Compliance
- **Priority:** P0
- **Objective:** Convert current control mapping into formal sign-off evidence.
- **Execution:**
  - Update control statuses and link final artifact paths.
  - Add approver/date/version metadata.
- **Acceptance criteria:**
  - `docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_2026-02-27.md` includes sign-off section with named owner(s).
  - Each control row contains direct link to current evidence artifact.
  - Release gate references this matrix as required attachment.
- **Current gap evidence:**
  - Matrix exists, but explicit owner sign-off not yet recorded.

## P1-1: Automate evidence packaging gate in CI

- **Owner:** SWE (DevOps)
- **Priority:** P1
- **Objective:** Prevent releases when mandatory compliance evidence is missing.
- **Execution:**
  - Extend release validation workflow to fail if required artifacts are missing.
  - Required artifacts:
    - latest backend dependency-check report
    - latest HIPAA controls log
    - latest SOC2 matrix
- **Acceptance criteria:**
  - CI job fails on missing artifact set.
  - CI job publishes machine-readable summary of evidence completeness.
- **Current references:**
  - `.github/workflows/security-scan.yml`
  - `.github/workflows/hdim-compliance-validation.yml`

## P1-2: OWASP ZAP baseline artifact for application tier

- **Owner:** QA/Security
- **Priority:** P1
- **Objective:** Add current DAST evidence to complement dependency scans.
- **Execution:**
  - Run existing OWASP ZAP baseline workflow path and archive report.
- **Acceptance criteria:**
  - Latest ZAP report artifact is linked in evidence bundle index.
  - Medium+ findings are triaged with owner and due date.
- **Current references:**
  - `.github/workflows/frontend-ci.yml` (`owasp-zap-scan` job)

## P2-1: Compliance evidence retention and cadence policy

- **Owner:** Security/Compliance
- **Priority:** P2
- **Objective:** Define repeatable cadence and retention for audit evidence.
- **Execution:**
  - Add evidence lifecycle section (frequency, retention period, storage location, approvers).
- **Acceptance criteria:**
  - Policy doc published and linked from SOC2 matrix.
  - Monthly evidence snapshot workflow documented and scheduled.

## Tracking Checklist

- [ ] P0-1 complete: backend CVE report generated with `NVD_API_KEY`
- [ ] P0-2 complete: SOC2 matrix signed by compliance owner
- [ ] P1-1 complete: CI release gate enforces evidence presence
- [ ] P1-2 complete: OWASP ZAP report archived and linked
- [ ] P2-1 complete: evidence cadence/retention policy documented

## Closure Definition

Backlog is considered closed when all P0 items are complete and at least one full release gate run includes all required evidence attachments with compliance owner sign-off.
