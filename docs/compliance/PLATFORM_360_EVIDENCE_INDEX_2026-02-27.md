# Platform 360 Evidence Index

**Date:** 2026-02-27  
**Purpose:** Single index of objective artifacts for 360 assurance controls.

## Functional + Test Stability Evidence

| Control Area | Artifact |
|---|---|
| Backend stability summary | `docs/TECHNICAL_INVESTOR_STABILITY_REPORT_2026-02-27.md` |
| Release traceability | `docs/releases/v2.7.2-rc1/RELEASE_NOTES_v2.7.2-rc1.md` |
| Aggregated tests | `5852 tests / 0 failures / 0 errors / 1176 suites` from JUnit XML under `backend/modules/services/**/build/test-results/test/*.xml` |

## Data Model + Migration Evidence

| Control Area | Artifact |
|---|---|
| Entity migration validation pipeline | `.github/workflows/entity-migration-validation.yml` |
| Service-level migration tests | `backend/modules/services/**/integration/EntityMigrationValidationTest.java` |
| Data access security matrix | `test-results/validate-data-access-security-2026-02-27-after-fix.log` |

## Performance + Real-Time Evidence

| Control Area | Artifact |
|---|---|
| SLO definitions | `docs/SLO_DEFINITIONS.md` |
| k6 load profiles | `backend/performance-tests/k6/` |
| Load test workflow gate | `.github/workflows/load-tests.yml` |
| E2E performance suite | `.github/workflows/e2e-tests.yml` |

## HIPAA Evidence

| Control Area | Artifact |
|---|---|
| HIPAA control run | `test-results/hipaa-controls-2026-02-27.log` |
| HIPAA cache compliance doctrine | `backend/HIPAA-CACHE-COMPLIANCE.md` |
| HIPAA compliance workflow | `.github/workflows/hdim-compliance-validation.yml` |
| Annual DR retention evidence | `.github/workflows/annual-dr-test.yml` |

## SOC2 Evidence

| Control Area | Artifact |
|---|---|
| SOC2 control matrix | `docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_2026-02-27.md` |
| Evidence bundle index | `docs/compliance/HIPAA_SOC2_EVIDENCE_BUNDLE_INDEX_2026-02-27.md` |
| Retention and cadence policy | `docs/compliance/COMPLIANCE_EVIDENCE_RETENTION_AND_CADENCE_POLICY_2026-02-27.md` |
| Compliance gate workflow | `.github/workflows/compliance-evidence-gate.yml` |

## Security Evidence

| Control Area | Artifact |
|---|---|
| Dependency scanning configuration | `.github/workflows/security-scan.yml` |
| Latest backend dependency-check execution | `test-results/dependency-check-aggregate-exec-2026-02-27.log` |
| Container scanning configuration | `.github/workflows/security-scan.yml` (Trivy sections) |
| DAST configuration | `.github/workflows/owasp-zap-baseline.yml` |
| Security hardening checks | `.github/workflows/security-hardening-validation.yml` |

## Open Evidence Gaps

| Gap | Tracking |
|---|---|
| Backend strict CVE remediation: latest scan failed with `303` vulnerabilities, `max_cvss=9.8` | `#497` |
| SOC2 named approver sign-off | `#498` |
| First successful OWASP baseline artifact capture | `#500` |

## Active Workflow Run Links (Current)

- Compliance Evidence Gate run: `https://github.com/webemo-aaron/hdim/actions/runs/22473840003` (queued at last check)
- OWASP ZAP Baseline run: `https://github.com/webemo-aaron/hdim/actions/runs/22474191527` (queued at last check)
