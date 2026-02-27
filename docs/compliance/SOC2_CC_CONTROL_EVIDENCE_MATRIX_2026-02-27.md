# SOC2 CC Control-Evidence Matrix

**Date:** 2026-02-27  
**Scope:** HDIM SaaS technical control evidence mapped to SOC2 Trust Services Criteria (Security)
**Version:** 1.1
**Tracking Issue:** #498

---

## Approval Metadata

| Field | Value |
|---|---|
| Matrix Owner | Security/Compliance (mahoosuc-solutions) |
| Technical Owner | Backend Platform (mahoosuc-solutions) |
| Status | Approved for RC with tracked residual blockers |
| Last Updated (UTC) | 2026-02-27T20:45:00Z |
| Approval SLA | Within 2 business days of evidence refresh |

**Sign-off block (complete before closing #498):**
- Compliance approver: `mahoosuc-solutions`
- Technical approver: `mahoosuc-solutions`
- Final approval date (UTC): `2026-02-27T20:45:00Z`

---

## CC6.1 Logical and Physical Access Controls
- **Control intent:** Access to systems/data is restricted to authorized users.
- **Implementation evidence:**
  - Tenant + auth access matrix validation passed (wrong tenant denied, missing tenant/auth denied).
    - `test-results/validate-data-access-security-2026-02-27-after-fix.log`
  - Strict release gate indicates tenant policy pass.
    - `logs/mcp-reports/release-gate-20260227-001707.md`
- **Status:** Implemented and evidenced.

## CC6.2 Authentication and Authorization
- **Control intent:** User identity is validated and permissions enforced.
- **Implementation evidence:**
  - Auth token issuance and protected endpoint behavior validated.
    - `test-results/validate-data-access-security-2026-02-27-after-fix.log`
  - HIPAA control script validates trusted header auth filter and tenant-aware handling.
    - `test-results/hipaa-controls-2026-02-27.log`
- **Status:** Implemented and evidenced.

## CC6.6 Data Transmission and Encryption
- **Control intent:** Data in transit is protected and secure protocols are enforced.
- **Implementation evidence:**
  - HIPAA control script checks audit TLS/encryption configuration presence.
    - `test-results/hipaa-controls-2026-02-27.log`
- **Status:** Implemented and evidenced (configuration-level proof).

## CC7.1 Monitoring and Detection
- **Control intent:** Security events and health signals are monitored.
- **Implementation evidence:**
  - Strict operator go/no-go with pretest/release-gate/evidence-pack/controlled-restart.
    - `logs/mcp-reports/operator-go-no-go-20260227-001710.md`
  - System validation artifact for operational status.
    - `test-results/validate-system-2026-02-26.log`
- **Status:** Implemented and evidenced.

## CC7.2 Vulnerability Management
- **Control intent:** Vulnerabilities are identified, assessed, and remediated.
- **Implementation evidence:**
  - Node dependency audit after remediation reports no high/critical findings.
    - `test-results/npm-audit-high-2026-02-27-after-fix.log`
  - Backend OWASP dependency-check now wired in Gradle.
    - `backend/build.gradle.kts`
  - Backend dependency-check tasks available (`dependencyCheckAnalyze`, `dependencyCheckAggregate`).
- **Status:** Partially complete (backend scan execution/report still pending without NVD API key).

## CC8.1 Change Management
- **Control intent:** Changes are tested and approved prior to release.
- **Implementation evidence:**
  - Version-controlled commits for security remediations and validation hardening:
    - `c48a2f641`
    - `bda1feb7d`
    - `6485d48a7`
  - MCP regression tests passing post-remediation.
    - `test-results/test-mcp-post-cve-remediation-2026-02-27.log`
- **Status:** Implemented and evidenced.

## CC9.2 Risk Mitigation Through Controls
- **Control intent:** Risks are reduced through layered controls and release gates.
- **Implementation evidence:**
  - Strict release gate pass with readiness and tenant policy checks.
    - `logs/mcp-reports/release-gate-20260227-001707.md`
  - Security and HIPAA runbooks validated present by control script.
    - `test-results/hipaa-controls-2026-02-27.log`
- **Status:** Implemented and evidenced.

---

## Residual Gaps Before Audit Assertion

1. Complete backend dependency-check output artifact generation (`dependencyCheckAggregate`) in an environment configured with `NVD_API_KEY`.
2. Attach backend CVE report artifact to release evidence package.
3. Obtain compliance owner sign-off for this matrix and evidence index.

## Evidence Lifecycle Reference

- Evidence cadence and retention policy:
  - `docs/compliance/COMPLIANCE_EVIDENCE_RETENTION_AND_CADENCE_POLICY_2026-02-27.md`
