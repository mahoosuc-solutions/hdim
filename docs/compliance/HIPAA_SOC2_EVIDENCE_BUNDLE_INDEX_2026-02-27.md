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
   - Note: no completed backend report artifact was generated in this environment

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
   - `logs/mcp-reports/operator-go-no-go-20260227-001710.md`

2. **Strict release gate**
   - `logs/mcp-reports/release-gate-20260227-001707.md`

3. **Release evidence pack**
   - `logs/mcp-reports/release-evidence-pack-20260227-001707.md`

4. **Post-remediation MCP regression tests**
   - `test-results/test-mcp-post-cve-remediation-2026-02-27.log`

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

---

## F. Remaining Evidence Needed for Final Audit Package

1. Backend dependency-check report output (`HTML/SARIF/JSON`) with `NVD_API_KEY` configured.
   - Suggested command: `cd backend && NVD_API_KEY=<key> ./gradlew dependencyCheckUpdate dependencyCheckAggregate --no-daemon`
2. Compliance owner sign-off (name/date) on the SOC2 matrix and this bundle index.
3. Optional: attach current strict go/no-go rerun artifact at release cutoff date.
