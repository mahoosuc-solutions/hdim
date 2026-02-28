# HIPAA + SOC2 Evidence Bundle Index

**Date:** 2026-02-27  
**Purpose:** Single index for compliance evidence artifacts used in readiness and diligence reviews.

---

## A. Security and Vulnerability Evidence

1. **Node CVE scan (post-remediation)**
   - `test-results/npm-audit-high-2026-02-27-after-fix.log`
   - Outcome: `found 0 vulnerabilities`

2. **Node CVE scan (pre-remediation baseline)**
   - `test-results/npm-audit-high-2026-02-27.log`
   - Outcome: vulnerabilities identified and remediated in subsequent commit

3. **Backend CVE scan wiring**
   - `backend/build.gradle.kts`
   - Dependency-check tasks now registered:
     - `dependencyCheckAnalyze`
     - `dependencyCheckAggregate`

4. **Backend dependency-check execution attempt**
   - `test-results/gradle-dependency-check-2026-02-27-after-wire.log`
   - Note: first-run NVD ingestion is long-running without `NVD_API_KEY`

5. **Backend dependency-check aggregate attempts**
   - `test-results/gradle-dependency-check-aggregate-2026-02-27.log`
   - `test-results/gradle-dependency-check-aggregate-2026-02-27-offline.log`
   - Latest run generated report artifacts with gate metrics:
     - vulnerabilities: `190`
     - max CVSS: `8.8`
   - Source artifacts:
     - `backend/build/reports/dependency-check-report.html`
     - `backend/build/reports/dependency-check-report.json`
     - `backend/build/reports/dependency-check-report.sarif`

6. **Backend dependency-check pre-NVD packet (latest local readiness)**
   - `test-results/gradle-dependency-check-aggregate-pre-nvd-2026-02-28T121308Z.log`
   - `test-results/dependency-check-report-pre-nvd-2026-02-28T121308Z.html`
   - `test-results/dependency-check-report-pre-nvd-2026-02-28T121308Z.json`
   - `test-results/dependency-check-report-pre-nvd-2026-02-28T121308Z.sarif`
   - `test-results/backend-cve-artifacts-manifest-2026-02-28T121308Z.md`
   - Final one-command closeout when key exists:
     - `NVD_API_KEY=<key> scripts/security/run-backend-cve-with-nvd.sh`

7. **Backend CVE artifact manifest (placeholder until API key provisioning)**
   - `test-results/backend-cve-artifacts-manifest-2026-02-27.md`

8. **OWASP ZAP baseline triage evidence**
   - `docs/compliance/OWASP_ZAP_BASELINE_TRIAGE_2026-02-27.md`
   - Local artifact capture (securecodebox image path):
     - `test-results/zap-local-2026-02-27/report_html.html`
     - `test-results/zap-local-2026-02-27/report_json.json`
     - `test-results/zap-local-2026-02-27/alerts.json`
     - `test-results/zap-local-2026-02-27/alerts-summary.json`
   - Tracks high/medium finding ownership and disposition status

---

## B. HIPAA Operational Control Evidence

1. **HIPAA controls validation**
   - `test-results/hipaa-controls-2026-02-27.log`
   - Includes PHI sanitization, audit TLS/encryption config checks, tenant-aware auth handling, and runbook presence

2. **Data access security matrix**
   - `test-results/validate-data-access-security-2026-02-27-after-fix.log`
   - Outcome: pass 19, fail 0, grade A

---

## C. SOC2 Operational and Change Evidence

1. **Strict operator go/no-go**
   - `logs/mcp-reports/operator-go-no-go-20260227-021544.md`

2. **Strict release gate**
   - `logs/mcp-reports/release-gate-20260227-021542.md`

3. **Release evidence pack**
   - `logs/mcp-reports/release-evidence-pack-20260227-021542.md`

4. **Deployment evidence package (cutoff-date rerun)**
   - `logs/mcp-reports/packages/deployment-evidence-20260227-021542/DEPLOYMENT_EVIDENCE_SUMMARY.md`

5. **Post-remediation MCP regression tests**
   - `test-results/test-mcp-post-cve-remediation-2026-02-27.log`

6. **Wave-1 local assurance (rebuild-enforced)**
   - `test-results/wave1-local-assurance-20260228T065856Z.json`
   - Outcome: `28/28` checks passed, including preflight gateway checks and expanded price-transparency contract/performance validations

7. **Local immutable evidence manifest (checksums)**
   - `test-results/local-evidence-manifest-2026-02-28T121333Z.md`
   - `test-results/local-evidence-sha256-2026-02-28T121333Z.txt`

---

## D. Compliance Documentation Evidence

1. `docs/compliance/SECURITY_COMPLIANCE_VALIDATION_2026-02-27.md`
2. `docs/compliance/SOC2_CC_CONTROL_EVIDENCE_MATRIX_2026-02-27.md`
3. `docs/compliance/THIRD_PARTY_NOTICES.md`

---

## E. Commit Traceability

1. `6485d48a7` - investor validation report and strict gate evidence updates  
2. `bda1feb7d` - enterprise SaaS architecture docs (md/html/drawio)  
3. `c48a2f641` - npm CVE remediation + compliance validation hardening
4. `d1c8fc5fe` - SOC2 control-evidence matrix + HIPAA/SOC2 bundle index
5. `aee8bc88b` - startup health-wait timeout hardening for local assurance reliability
6. `9bf7082fd` - preflight + contract + load expansions for Wave-1 local assurance

---

## F. Remaining Evidence Needed for Final Audit Package

1. Backend dependency-check report refresh with `NVD_API_KEY` configured (recommended to reduce false negatives on enrichment feeds).
   - Suggested command: `cd backend && NVD_API_KEY=<key> ./gradlew dependencyCheckUpdate dependencyCheckAggregate --no-daemon`
   - One-command helper: `scripts/security/run-backend-cve-with-nvd.sh`
2. Hosted CI runner unblock for immutable CI-attested reruns (`#515`).
3. Compliance owner sign-off (name/date) on the SOC2 matrix and this bundle index.
4. Cutoff-date strict go/no-go rerun artifact attached: `logs/mcp-reports/operator-go-no-go-20260227-021544.md`.

---

## G. Bundle Index Sign-off

| Role | Name | Decision | Date (UTC) |
|---|---|---|---|
| Compliance Owner | mahoosuc-solutions | Approved for RC | 2026-02-27T20:45:00Z |
| Technical Owner | mahoosuc-solutions | Approved for RC | 2026-02-27T20:45:00Z |
