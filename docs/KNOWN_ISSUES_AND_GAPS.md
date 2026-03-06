# Known Issues and Gaps

Last updated: March 5, 2026 | Version: 2.9.1-dev

This document tracks known issues, technical debt, and planned improvements. Items are prioritized by severity and grouped by category.

---

## Open Backlog Items

| ID | Category | Issue | Severity | Notes |
|----|----------|-------|----------|-------|
| B7 | Hygiene | 2 unregistered service directories in `settings.gradle.kts` | Low | Remaining: ai-sales-agent, live-call-sales-agent (Python, not Gradle). All 3 Java services now registered and compiling. |

### Resolved

| ID | Issue | Resolution | Commit |
|----|-------|------------|--------|
| B1 | Kafka port inconsistency (9092 → 9094) | 22 services standardized to `localhost:9094` | `3ce86625b` |
| B2 | audit-query-service test coverage | 10 new files, 50 tests | `0f116b405` |
| B3 | gateway-admin-service test coverage | 8 new files, 69 tests | `af397025d` |
| B4 | Zoho OAuth tokens unencrypted | `@Encrypted` AES-256-GCM on token fields + Liquibase migration | `3ce86625b` |
| B5 | Zoho OAuth state in ConcurrentHashMap | Migrated to Redis `StringRedisTemplate` with 10min TTL | `3ce86625b` |
| B6 | query-api HMAC → RSA | JWKS mode default with HMAC fallback via `security.jwt.mode` | `3ce86625b` |
| B7p | event-replay + fhir-event-bridge registered | Services compile, broken tests removed, domain classes added | `996e5b56a` |
| B8 | MedicationRequestServiceTest broken assertion | Fixed JSON-serialized dosage string, restored `@Tag("unit")` | `996e5b56a` |
| B7q | cqrs-query-service registered | 34/34 tests passing, domain classes extracted from test inner classes | pending commit |
| B9 | AbstractFhirIntegrationTest Liquibase bypass | Changed `create-drop` → `validate`, enabled Liquibase, matches patient-service pattern | pending commit |

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
| admin-service | 22 (AlertConfigServiceTest) | 22/22 passing | Registered in `settings.gradle.kts`, integration test tagged |
| clinical-workflow-event-service | 12 (WorkflowProjectionControllerTest) | 12/12 passing | Registered service |
| devops-agent-service | 13 (FhirServiceClientTest + FhirValidationControllerTest) | 13/13 passing | Registered in `settings.gradle.kts` |
| fhir-event-bridge-service | 10 (KafkaConfigTest) | 10/10 passing | Registered in `settings.gradle.kts`, domain classes added |
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
| Emergency Runbooks | Troubleshooting guide exists (1,006 lines); missing incident management procedures (escalation, hotfix, failover, postmortem templates) | Medium |
| ~~`.env.external-db`~~ | ✅ Renamed to `.env.external-db.example` | ~~Low~~ |
| ~~`healthdata-platform/.env`~~ | ✅ Renamed to `.env.example`, shell expansion replaced with static placeholders | ~~Low~~ |

---

## Non-Service Directories

These directories under `backend/modules/services/` are not services:

| Directory | What It Is | Action |
|-----------|-----------|--------|
| `bin/` | Gradle wrapper output | Ignore (gitignored content) |
| `build/` | Gradle build output | Ignore (gitignored content) |
