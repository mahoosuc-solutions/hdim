# Compliance Evidence Retention and Cadence Policy

**Effective Date:** 2026-02-27  
**Scope:** SOC2 Security criteria and HIPAA operational control evidence artifacts  
**Tracking Issue:** #501

## Objective

Define a repeatable cadence, retention period, storage location, and approver model for compliance evidence used in release gates and audit responses.

## Evidence Cadence

| Evidence Type | Generation Cadence | Trigger |
|---|---|---|
| Compliance evidence gate summary (`md/json`) | Per release workflow run | `Compliance Evidence Gate` workflow |
| HIPAA controls validation log | Per release workflow run + monthly baseline | `validate-phase4-hipaa-controls.sh` |
| SOC2 control-evidence matrix | On evidence refresh + monthly review | Security/Compliance review |
| Backend dependency-check report | Per security release gate | Gradle dependency-check tasks |
| OWASP ZAP baseline report | Weekly + release candidate | Frontend security workflow |

## Scheduled Snapshot

A monthly snapshot workflow is scheduled on the first day of each month at 06:00 UTC:
- Workflow: `.github/workflows/compliance-evidence-snapshot.yml`
- Purpose: capture baseline evidence artifacts even without a release event.

## Retention Policy

| Artifact Class | Retention | Storage |
|---|---|---|
| CI compliance summary artifacts | 400 days | GitHub Actions artifacts |
| Release gate evidence bundle pointers | 7 years | Repository (`docs/compliance`, `logs/mcp-reports`) |
| HIPAA validation logs linked to releases | 7 years | Repository `test-results/` + release attachments |
| SOC2 mapping and sign-off documents | 7 years | Repository `docs/compliance/` |

## Ownership and Approval

| Role | Responsibility |
|---|---|
| Security/Compliance Owner | Reviews matrix accuracy, signs SOC2 mapping, approves retention posture |
| Backend Platform Owner | Ensures dependency-check evidence generation and artifact integrity |
| QA Owner | Verifies HIPAA controls and DAST evidence completeness |

## Monthly Review Checklist

- Confirm latest SOC2 matrix references current artifact paths.
- Confirm latest HIPAA controls log exists and is linked.
- Confirm backend CVE artifact is present or issue is documented with mitigation.
- Confirm OWASP ZAP report status and open finding triage.
- Record approval result in SOC2 matrix approval metadata.

## Exception Handling

If a required artifact cannot be generated (for example missing `NVD_API_KEY`), document the exception in:
- `docs/compliance/HIPAA_SOC2_EVIDENCE_BUNDLE_INDEX_2026-02-27.md`
- Open/active GitHub issue with owner and target resolution date.
