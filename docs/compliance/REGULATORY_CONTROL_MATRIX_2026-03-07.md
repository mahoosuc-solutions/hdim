# Regulatory Control Matrix (2026-03-07)

**Scope:** HDIM platform release readiness (`v0.0.0-test` baseline)  
**Policy:** Missing evidence defaults to `NO-GO`.

| Control ID | Domain | Control Requirement | Evidence Artifact | Owner | Cadence | Status |
|---|---|---|---|---|---|---|
| RC-SEC-001 | Security | Auth bypass prevention and trusted-header validation regression coverage | `test-results/security-auth-tenant-rerun-2026-03-07.log` | Security Lead | Per RC | PASS |
| RC-TEN-001 | Tenant Isolation | Tenant-scoped API access and mutation enforcement | `docs/releases/v0.0.0-test/validation/security-auth-tenant-rerun-2026-03-06.md` | Security Lead | Per RC | PASS |
| RC-RBAC-001 | Authorization | Privileged operation RBAC enforced on mutation endpoints | `test-results/security-auth-tenant-rerun-2026-03-07.log` | Security Lead | Per RC | PASS |
| RC-REL-001 | Release Orchestration | Preflight gate executed and recorded prior to release phases | `test-results/release-preflight-2026-03-07.log` | Release Manager | Per RC | PASS |
| RC-CNT-001 | Contract Integrity | Consumer contract tests pass in current branch | `test-results/contract-tests-2026-03-07.log` | QA Lead | Per RC | PASS |
| RC-MCP-001 | Runtime Orchestration | MCP orchestration/go-no-go runtime tests pass | `test-results/mcp-orchestration-tests-2026-03-07.log` | Platform Lead | Per RC | PASS |
| RC-CI-001 | CI Gate Freshness | Upstream security/performance CI gate validation succeeds with fresh successful runs | `test-results/upstream-ci-gates-2026-03-07.log` | Release Manager | Per RC | PASS |
| RC-EVD-001 | Evidence Provenance | RC proof artifact exists with command-level evidence | `docs/releases/v0.0.0-test/validation/rc-implementation-proof-2026-03-07.md` | Compliance Lead | Per RC | PASS |
| RC-DR-001 | Business Continuity | Recovery drill report with timed RTO/RPO for current release lane | `docs/compliance/DR_TEST_RESULTS_2026-03-07.md` | SRE Lead | Quarterly / Pre-prod | PASS |
| RC-ACC-001 | Access Governance | Access recertification and stale access review evidence | `docs/compliance/ACCESS_REVIEW_2026-03-07.md` | Security Lead | Monthly | PASS |
| RC-TPR-001 | Third-Party Risk | Vendor inventory includes BAA/DPA/SOC mappings and exceptions | `docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md` | Compliance Lead | Quarterly | PASS |

## Decision Rule
- Any `FAIL` in `Critical` release controls (`RC-SEC-*`, `RC-TEN-*`, `RC-RBAC-*`, `RC-REL-*`, `RC-CNT-*`, `RC-CI-*`) => `NO-GO`.
- Missing artifacts are treated as failures until closed.
