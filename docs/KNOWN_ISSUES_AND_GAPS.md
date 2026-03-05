# Known Issues and Gaps

Last updated: March 5, 2026 | Version: 2.9.0

This document tracks known issues, technical debt, and planned improvements. Items are prioritized by severity and grouped by category.

---

## Open Backlog Items

| ID | Category | Issue | Severity | Notes |
|----|----------|-------|----------|-------|
| B1 | Infrastructure | Kafka port inconsistency (9092 internal vs 9094 host binding) | Medium | Docker Compose and service configs disagree; causes local dev friction |
| B4 | Security | Zoho OAuth tokens stored unencrypted | High | Tokens at rest not encrypted; needs Vault or encrypted column |
| B5 | Security | Zoho OAuth state not stored in Redis | High | Vulnerable to CSRF replay; state should be Redis-backed with TTL |
| B6 | Security | query-api-service uses HMAC instead of RSA | High | HMAC shared secret less secure than asymmetric RSA for inter-service auth |
| B7 | Hygiene | 7 unregistered service directories in `settings.gradle.kts` | Medium | 3 have new test files waiting (admin, devops-agent, fhir-event-bridge); tests compile per-service but not via root Gradle |
| B8 | Tests | MedicationRequestServiceTest broken assertion | Low | Pre-existing; test asserts wrong value |
| B9 | Tests | AbstractFhirIntegrationTest bypasses Liquibase | Medium | Uses Hibernate DDL instead of Liquibase migrations for test schema |

### Resolved

| ID | Issue | Resolution | Commit |
|----|-------|------------|--------|
| B2 | audit-query-service test coverage | 10 new files, 50 tests | `0f116b405` |
| B3 | gateway-admin-service test coverage | 8 new files, 69 tests | `af397025d` |

---

## Low Test Coverage Services — RESOLVED

All 9 previously-flagged services now have adequate test coverage:

### Tier A: Already Well-Tested (no action needed)

These services were flagged by file count but already had 31-39 tests each in single comprehensive test files:

| Service | Existing Tests | Status |
|---------|---------------|--------|
| care-gap-event-handler-service | 39 tests | Adequate |
| clinical-workflow-event-handler-service | 31 tests | Adequate |
| patient-event-handler-service | 35 tests | Adequate |
| quality-measure-event-handler-service | 33 tests | Adequate |

### Tier B: New Tests Added (v2.9.0)

| Service | New Tests | Verified | Notes |
|---------|----------|----------|-------|
| admin-service | 22 (AlertConfigServiceTest) | Pending B7 | Compiles per-service; needs `settings.gradle.kts` registration |
| clinical-workflow-event-service | 12 (WorkflowProjectionControllerTest) | 12/12 passing | Registered service |
| devops-agent-service | 13 (FhirServiceClientTest + FhirValidationControllerTest) | Pending B7 | Compiles per-service; needs `settings.gradle.kts` registration |
| fhir-event-bridge-service | 10 (KafkaConfigTest) | Pending B7 | Compiles per-service; needs `settings.gradle.kts` registration |
| gateway-fhir-service | 9 (GatewayFhirRoutingEdgeCaseTest) | 9/9 passing | Registered service |

Design doc: `docs/plans/2026-03-05-test-coverage-improvement-design.md`

---

## Pre-Existing Test Environment Issues

These are environmental issues that affect test execution on certain machines:

| Service | Issue | Workaround |
|---------|-------|-----------|
| cms-connector-service | Testcontainers needs Docker daemon | Skip locally; runs in CI |
| consent-service | Kafka `localhost:9094` timeout | Skip locally; runs in CI with Docker Kafka |
| clinical-workflow-service | TestContainers DB pool exhaustion | Increase `maxPoolSize` or skip locally |
| audit-query-service | 2 EntityMigrationValidationTest failures | Pre-existing; not blocking |
| testFast mode | WSL2 XML write issue with `$` in nested class filenames | Use `testAll` instead |

---

## MCP Edge Gaps

| Gap | Status | Target |
|-----|--------|--------|
| Layer 2 (clinical workflow automation) | Design complete, not implemented | Q2 2026 |
| Layer 3 (executive dashboards) | Outlined in design doc | Q2-Q3 2026 |
| Layer 4 (hardening, mTLS, Vault) | Planned | Q3 2026 |

Design doc: `docs/plans/2026-03-04-hdim-mcp-edge-design.md`

---

## Documentation Gaps

| Area | Gap | Priority |
|------|-----|----------|
| API Documentation | Phase 1B: ~20 undocumented endpoints beyond initial 62 | Medium |
| Emergency Runbooks | No emergency troubleshooting guide for production incidents | Medium |
| `.env.external-db` | Tracked template with placeholder passwords; rename to `.example` | Low |
| `healthdata-platform/.env` | Template with shell expansion; should be `.env.example` | Low |

---

## Non-Service Directories

These directories under `backend/modules/services/` are not services:

| Directory | What It Is | Action |
|-----------|-----------|--------|
| `bin/` | Gradle wrapper output | Ignore (gitignored content) |
| `build/` | Gradle build output | Ignore (gitignored content) |
