# HDIM MCP Edge Layer 2 — Clinical Edge Sidecar Design

> **Status:** COMPLETE

**Date:** 2026-03-04
**Status:** Approved
**Author:** Claude Code + Aaron
**Depends on:** Layer 0+1 (commit 94f7b35cc)
**Approach:** Full clinical surface with composite + domain shortcut tool strategy

## Overview

The clinical-edge sidecar (`mcp-edge-clinical`, port 3300) exposes all 5 HDIM clinical
services (Patient, Care Gap, FHIR, Quality Measure, CQL Engine) to MCP clients. It handles
full PHI with HIPAA-compliant audit logging, multi-tenant isolation, and role-based access.

Three interchangeable tool strategies are provided — selectable at runtime via env var — so
the deployment can be tuned for different use cases without code changes.

## Sidecar Topology (Updated)

```
Claude Desktop / VS Code
    |
    +-- stdio --> hdim-platform-bridge.mjs --> hdim-platform-edge   :3100  (Layer 0+1)
    |
    +-- stdio --> hdim-devops-bridge.mjs   --> hdim-devops-edge     :3200  (Layer 0+1)
    |
    +-- stdio --> hdim-clinical-bridge.mjs --> hdim-clinical-edge   :3300  (Layer 2)
                                                Gateway proxy (18080)
                                                PHI: full
                                                Audit: per-tool invocation
```

### Security Boundaries

| Sidecar | Port | PHI Access | Network Access | Docker Socket |
|---------|------|------------|----------------|---------------|
| platform-edge | 3100 | Aggregate only | Gateway (18080) | No |
| devops-edge | 3200 | None | Docker socket, localhost | Yes (read-only) |
| **clinical-edge** | **3300** | **Full** | **Gateway (18080)** | **No** |

## Tool Strategies

### Strategy A: Composite + Domain Shortcuts (default, 25 tools)

Combines 4 generic FHIR tools with 21 domain-specific shortcuts that map to pre-aggregated
backend endpoints. Best for LLM tool selection quality and complex patient workflows.

**Env:** `CLINICAL_TOOL_STRATEGY=composite`

#### FHIR Composite Tools (4)

| Tool | Required Input | Backend Route | PHI |
|------|---------------|---------------|-----|
| `fhir_read` | `resourceType`, `id`, `tenantId` | `GET /fhir/{resourceType}/{id}` | Yes |
| `fhir_search` | `resourceType`, `tenantId`, `patient?`, `params?` | `GET /fhir/{resourceType}?...` | Yes |
| `fhir_create` | `resourceType`, `resource`, `tenantId` | `POST /fhir/{resourceType}` | Yes |
| `fhir_bundle` | `type`, `entries`, `tenantId` | `POST /fhir/Bundle` | Yes |

**`resourceType` validation:** Must be one of: Patient, Observation, Encounter, Condition,
MedicationRequest, MedicationAdministration, Immunization, AllergyIntolerance, Procedure,
DiagnosticReport, DocumentReference, CarePlan, Goal, Coverage, Appointment, Task, Bundle,
Organization, Practitioner, PractitionerRole.

#### Patient Domain Tools (5)

| Tool | Required Input | Backend Route | PHI |
|------|---------------|---------------|-----|
| `patient_summary` | `patientId`, `tenantId` | `GET /patient/health-record?patient={id}` | Yes |
| `patient_timeline` | `patientId`, `tenantId` | `GET /patient/timeline/by-date?patient={id}` | Yes |
| `patient_risk` | `patientId`, `tenantId` | `GET /patient/risk-assessment?patient={id}` | Yes |
| `patient_list` | `tenantId`, `page?`, `size?` | `GET /api/v1/patients?page=&size=` | Yes |
| `pre_visit_plan` | `providerId`, `patientId`, `tenantId` | `GET /api/v1/providers/{pid}/patients/{id}/pre-visit-summary` | Yes |

#### Care Gap Domain Tools (6)

| Tool | Required Input | Backend Route | PHI |
|------|---------------|---------------|-----|
| `care_gap_list` | `patientId`, `tenantId`, `status?` | `GET /care-gap/open?patient={id}` | Yes |
| `care_gap_identify` | `patientId`, `tenantId`, `library?` | `POST /care-gap/identify` | Yes |
| `care_gap_close` | `gapId`, `tenantId`, `closedBy`, `reason` | `POST /care-gap/close` | Yes |
| `care_gap_stats` | `tenantId` | `GET /care-gap/stats` | No |
| `care_gap_population` | `tenantId` | `GET /care-gap/population-report` | No |
| `care_gap_provider` | `providerId`, `tenantId` | `GET /care-gap/providers/{id}/prioritized` | Yes |

#### Quality Measure Domain Tools (6)

| Tool | Required Input | Backend Route | PHI |
|------|---------------|---------------|-----|
| `measure_evaluate` | `patientId`, `tenantId`, `measureId?` | `POST /quality-measure/calculate` | Yes |
| `measure_results` | `patientId`, `tenantId` | `GET /quality-measure/results?patient={id}` | Yes |
| `measure_score` | `patientId`, `tenantId` | `GET /quality-measure/score?patient={id}` | Yes |
| `measure_population` | `tenantId` | `GET /quality-measure/report/population` | No |
| `cds_patient_view` | `patientId`, `tenantId` | `POST /quality-measure/cds-services/patient-view` | Yes |
| `health_score` | `patientId`, `tenantId` | `GET /quality-measure/patients/{id}/health-score` | Yes |

#### CQL Engine Domain Tools (4)

| Tool | Required Input | Backend Route | PHI |
|------|---------------|---------------|-----|
| `cql_evaluate` | `library`, `patientId`, `tenantId` | `POST /cql/evaluate?library=&patient=` | Yes |
| `cql_batch` | `library`, `patientIds[]`, `tenantId` | `POST /cql/api/v1/cql/evaluations/batch` | Yes |
| `cql_libraries` | `tenantId`, `status?` | `GET /cql/api/v1/cql/libraries` | No |
| `cql_result` | `patientId`, `library`, `tenantId` | `GET /cql/api/v1/cql/evaluations/patient/{id}/library/{lib}/latest` | Yes |

### Strategy B: High-Value Resources (15 tools)

Individual tools for the 5 most-used FHIR resources plus care gap and measure tools.
Good for environments where tool count must stay under 20.

**Env:** `CLINICAL_TOOL_STRATEGY=high-value`

| Tool | Backend Route | PHI |
|------|---------------|-----|
| `patient_read` | `GET /fhir/Patient/{id}` | Yes |
| `patient_search` | `GET /fhir/Patient?...` | Yes |
| `observation_read` | `GET /fhir/Observation/{id}` | Yes |
| `observation_search` | `GET /fhir/Observation?patient=` | Yes |
| `condition_read` | `GET /fhir/Condition/{id}` | Yes |
| `condition_search` | `GET /fhir/Condition?patient=` | Yes |
| `medication_read` | `GET /fhir/MedicationRequest/{id}` | Yes |
| `medication_search` | `GET /fhir/MedicationRequest?patient=` | Yes |
| `encounter_read` | `GET /fhir/Encounter/{id}` | Yes |
| `encounter_search` | `GET /fhir/Encounter?patient=` | Yes |
| `care_gap_list` | `GET /care-gap/open?patient=` | Yes |
| `care_gap_close` | `POST /care-gap/close` | Yes |
| `care_gap_stats` | `GET /care-gap/stats` | No |
| `measure_evaluate` | `POST /quality-measure/calculate` | Yes |
| `measure_results` | `GET /quality-measure/results?patient=` | Yes |

### Strategy C: Full Surface (60+ tools)

One tool per FHIR resource type per operation (read/search/create), plus all care gap,
quality measure, and CQL tools. Generated dynamically from a resource registry.

**Env:** `CLINICAL_TOOL_STRATEGY=full-surface`

Uses a `tool-factory.js` that reads from `resource-registry.js` (listing all 20 FHIR
resource types) and generates `{resourceType}_read`, `{resourceType}_search`,
`{resourceType}_create` tool definitions programmatically.

Total: ~60 FHIR tools + ~12 domain tools = ~72 tools.

**Warning:** Tool selection accuracy degrades above ~20 tools. Use only when the MCP
client supports tool namespacing or the user has a specific workflow requiring direct
resource access.

## Role-Based Access Control

### Clinical-Edge Role Matrix (Composite Strategy)

| Role | Accessible Tools | Count |
|------|-----------------|-------|
| `clinical_admin` | All 25 tools | 25 |
| `platform_admin` | All 25 tools | 25 |
| `developer` | All 25 tools | 25 |
| `clinician` | patient_*, care_gap_list/identify/close/provider, fhir_read/search, cds_*, health_score, pre_visit_plan | 21 |
| `care_coordinator` | patient_summary, patient_list, pre_visit_plan, care_gap_* | 9 |
| `quality_officer` | measure_*, cql_*, care_gap_stats, care_gap_population | 10 |
| `executive` | care_gap_stats, care_gap_population, measure_population, health_score | 4 |

Each strategy has its own `role-policies.js` following the same regex-match pattern as
Layer 0+1's `auth.js`.

## PHI Audit Logging

### Audit Record Structure

```json
{
  "timestamp": "2026-03-04T15:30:00.000Z",
  "level": "audit",
  "source": "mcp-edge-clinical",
  "tool": "patient_summary",
  "action": "PHI_ACCESS",
  "role": "clinician",
  "tenantId": "acme-health",
  "patientId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "success": true,
  "durationMs": 234,
  "responseSize": 4521
}
```

### Audit Rules

- **PHI tools:** Log with `action: "PHI_ACCESS"`, include `patientId` and `tenantId`
- **Non-PHI tools:** Log with `action: "TOOL_CALL"`, include `tenantId` only
- **Write operations:** Log with `action: "PHI_WRITE"`, include full input params (minus resource body)
- **Failed auth:** Log with `action: "AUTH_DENIED"`, include `role` and `toolName`
- **Response bodies** are never logged (HIPAA compliance)

### PHI Leak Prevention

- Tool error messages must not include patient data, only IDs
- Response body truncation at 20KB (matching platform-edge)
- No caching of PHI responses (`Cache-Control: no-store` on all clinical tools)
- Demo mode fixtures contain only synthetic data

## Directory Structure

```
mcp-edge-clinical/
  package.json
  Dockerfile
  .dockerignore
  .env.example
  index.js                          # Entry point (reads CLINICAL_TOOL_STRATEGY env)
  server.js                         # Express app factory

  lib/
    clinical-client.js              # HTTP client for gateway (extends platform-client pattern)
    phi-audit.js                    # PHI-specific audit logging

    strategies/
      composite/                    # Strategy A (default, 25 tools)
        index.js                    # Exports tool definitions array
        role-policies.js            # RBAC for composite strategy
        tools/                      # 25 tool files
          fhir-read.js
          fhir-search.js
          fhir-create.js
          fhir-bundle.js
          patient-summary.js
          patient-timeline.js
          patient-risk.js
          patient-list.js
          pre-visit-plan.js
          care-gap-list.js
          care-gap-identify.js
          care-gap-close.js
          care-gap-stats.js
          care-gap-population.js
          care-gap-provider.js
          measure-evaluate.js
          measure-results.js
          measure-score.js
          measure-population.js
          cds-patient-view.js
          health-score.js
          cql-evaluate.js
          cql-batch.js
          cql-libraries.js
          cql-result.js
        fixtures/                   # 25 demo mode response files

      high-value/                   # Strategy B (15 tools)
        index.js
        role-policies.js
        tools/                      # 15 tool files
        fixtures/                   # 15 demo mode response files

      full-surface/                 # Strategy C (60+ tools)
        index.js
        role-policies.js
        tool-factory.js             # Generates tool defs from registry
        resource-registry.js        # All 20 FHIR resource types
        fixtures/

  __tests__/
    clinical-client.test.js
    phi-audit.test.js
    server.test.js
    integration.test.js
    security.test.js
    boundary.test.js
    demo-integration.test.js
    rbac-matrix.test.js
    phi-leak.test.js

    strategies/
      composite/
        tools/                      # 25 test files (1 per tool)
        role-policies.test.js
      high-value/
        tools/                      # 15 test files
        role-policies.test.js
      full-surface/
        tool-factory.test.js
        resource-registry.test.js
        role-policies.test.js

scripts/mcp/
  mcp-edge-clinical-bridge.mjs      # stdio bridge for Claude Desktop
  __tests__/
    bridge-helpers.test.mjs          # (existing, shared)
```

## Testing Strategy

### Test Tiers

| Tier | Scope | Files | Independence | Parallel |
|------|-------|-------|-------------|----------|
| T1 | Shared infra (client, audit, server) | 3 | Full | Yes |
| T2 | Composite tool units (25 tools) | 25 | Full | Yes |
| T3 | RBAC matrix (composite) | 1 | Full | Yes |
| T4 | PHI leak tests | 1 | Full | Yes |
| T5 | Security boundary | 1 | Full | Yes |
| T6 | Integration (JSON-RPC round-trip) | 1 | Sequential | No |
| T7 | Demo mode integration | 1 | Full | Yes |
| T8 | Bridge (stdio) | 1 | Full | Yes |
| T9 | High-value strategy tools | 15 | Full | Yes |
| T10 | Full-surface strategy | 3 | Full | Yes |
| **Total** | | **~52** | | **~50** |

### TDD Swarm Execution Waves

**Wave 1 — Infrastructure (sequential, 1 agent):**
- clinical-client.js + test
- phi-audit.js + test
- server.js + test
- Strategy loader (index.js)

**Wave 2 — Composite Tool Definitions (parallel, 5 agents):**
- Agent A: FHIR composite tools (4 tools + 4 tests + 4 fixtures)
- Agent B: Patient domain tools (5 tools + 5 tests + 5 fixtures)
- Agent C: Care Gap domain tools (6 tools + 6 tests + 6 fixtures)
- Agent D: Quality Measure domain tools (6 tools + 6 tests + 6 fixtures)
- Agent E: CQL Engine domain tools (4 tools + 4 tests + 4 fixtures)

**Wave 3 — Cross-Cutting Validation (parallel, 4 agents):**
- Agent F: RBAC matrix tests (all role × tool combinations)
- Agent G: PHI leak tests + security boundary tests
- Agent H: Demo mode fixtures + integration tests
- Agent I: Bridge + stdio tests + .mcp.json config

**Wave 4 — Integration (sequential, 1 agent):**
- Full JSON-RPC round-trip tests
- Docker compose config + healthcheck
- Validation report generation

**Wave 5 — Alternative Strategies (parallel, 2 agents):**
- Agent J: High-value strategy (15 tools + tests + fixtures + role policies)
- Agent K: Full-surface strategy (factory + registry + tests + role policies)

### Coverage Targets

| Metric | Target | Rationale |
|--------|--------|-----------|
| Statement coverage | ≥99% | Match Layer 0+1 (99.35%) |
| Branch coverage | ≥93% | Match Layer 0+1 (93.13%) |
| Function coverage | 100% | Match Layer 0+1 |
| PHI leak coverage | 100% of PHI tools | HIPAA requirement |
| RBAC matrix | 100% role × tool | Security requirement |
| Demo fixtures | 100% of tools | Demo mode completeness |

### Test Pattern

Each tool test follows the Layer 0+1 pattern:

```javascript
const { createDefinition } = require('../../../lib/strategies/composite/tools/fhir-read');

describe('fhir_read', () => {
  let tool, mockClient;

  beforeEach(() => {
    mockClient = { get: jest.fn(), post: jest.fn() };
    tool = createDefinition(mockClient);
  });

  describe('definition', () => {
    it('has correct name and description', () => { ... });
    it('requires resourceType, id, and tenantId', () => { ... });
    it('validates resourceType against allowed list', () => { ... });
  });

  describe('handler', () => {
    it('calls gateway with correct path and headers', () => { ... });
    it('forwards X-Tenant-ID header', () => { ... });
    it('returns FHIR JSON in MCP content format', () => { ... });
    it('returns error for 404', () => { ... });
    it('returns error for 403', () => { ... });
  });

  describe('PHI safety', () => {
    it('does not include patient data in error messages', () => { ... });
    it('logs audit record with patientId', () => { ... });
  });
});
```

## Docker Compose (addition to docker-compose.mcp-edge.yml)

```yaml
  mcp-edge-clinical:
    build: ./mcp-edge-clinical
    ports: ["3300:3300"]
    env_file: ./mcp-edge-clinical/.env
    environment:
      CLINICAL_TOOL_STRATEGY: composite
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "node", "-e", "fetch('http://127.0.0.1:3300/health').then(r => r.ok ? process.exit(0) : process.exit(1))"]
      interval: 30s
      timeout: 5s
      retries: 3
```

## MCP Client Configuration (addition to .mcp.json)

```json
{
  "hdim-clinical-edge": {
    "type": "stdio",
    "command": "node",
    "args": ["scripts/mcp/mcp-edge-clinical-bridge.mjs"],
    "env": {
      "MCP_EDGE_URL": "http://localhost:3300/mcp",
      "MCP_EDGE_OPERATOR_ROLE": "clinical_admin"
    }
  }
}
```

## Auth Model (Layer 2)

Same as Layer 0+1: API key via `MCP_EDGE_API_KEY` env var, forwarded as Bearer token.
Role via `X-Operator-Role` header. Gateway resolves tenant from token.

Layer 4 (future) will upgrade to signed JWT with per-tool PHI audit.

## Multi-Tenant Isolation

Every tool that calls the gateway includes `X-Tenant-ID` in the request header.
The `tenantId` parameter is **required** on all tools (no default).
The gateway enforces tenant isolation at the service layer.

## Deliverables Checklist

- [ ] `mcp-edge-clinical/` package with all 3 strategies
- [ ] `mcp-edge-common/` updates (if any shared lib changes needed)
- [ ] `scripts/mcp/mcp-edge-clinical-bridge.mjs`
- [ ] `docker-compose.mcp-edge.yml` updated
- [ ] `.mcp.json` updated
- [ ] Validation report (matching Layer 0+1 format)
- [ ] All tests passing, coverage targets met
- [ ] Demo mode verified for all 3 strategies
