# PHI Audit Integration — Design Document

> **Status:** COMPLETE

**Date:** 2026-03-05
**Status:** Approved
**Author:** Claude Code + Aaron
**Prerequisite:** mcp-edge v0.1.0 (released)
**Target:** v0.2.0

## Overview

Wire the existing `phi-audit.js` module into the MCP router so every clinical tool call emits structured PHI access events (`PHI_ACCESS`, `PHI_WRITE`, `TOOL_CALL`, `AUTH_DENIED`). Platform and devops sidecars are unaffected.

## Architecture Decision

**Approach A: Tool-level PHI annotations + optional router hook.**

Each tool definition carries an `audit` field declaring its PHI classification. The MCP router accepts an optional `phiAuditLogger` — when present, it fires audit events at 4 hook points. When absent (platform/devops), the entire audit path short-circuits via optional chaining (`?.`).

### Why This Approach

- Co-locates PHI classification with the tool that owns it
- New tools must explicitly declare their PHI posture
- Zero runtime cost for non-clinical sidecars
- No subclassing, no middleware duplication, no parallel registries
- CISO can audit PHI surface via `grep -r "phi: true"` across tool files

## Tool Audit Annotation Schema

```js
tool.audit = {
  phi: boolean,         // true = accesses/writes PHI
  write: boolean,       // true = mutates data (PHI_WRITE vs PHI_ACCESS)
  patientIdArg: string  // arg name holding patient ID, or undefined
}
```

Absence of `audit` field = non-PHI `TOOL_CALL` (e.g., `edge_health`).

### patientId Extraction Rules

- `args[tool.audit.patientIdArg]` when `patientIdArg` is set
- `undefined` when `phi: true` but no extractable patient (e.g., `fhir_read` uses resource `id`, not patient ID)
- Optional search params (e.g., `fhir_search.patient`) emit the value when present, `undefined` when absent

## PHI Classification

### Composite Strategy (25 tools)

| Tool | phi | write | patientIdArg |
|------|-----|-------|-------------|
| fhir_read | true | false | - |
| fhir_search | true | false | patient |
| fhir_create | true | true | - |
| fhir_bundle | true | true | - |
| patient_summary | true | false | patientId |
| patient_timeline | true | false | patientId |
| patient_risk | true | false | patientId |
| patient_list | true | false | - |
| pre_visit_plan | true | false | patientId |
| care_gap_list | true | false | patientId |
| care_gap_identify | true | false | patientId |
| care_gap_close | true | true | - |
| care_gap_stats | false | false | - |
| care_gap_population | false | false | - |
| care_gap_provider | true | false | - |
| measure_evaluate | true | false | patientId |
| measure_results | true | false | patientId |
| measure_score | true | false | patientId |
| measure_population | false | false | - |
| cds_patient_view | true | false | patientId |
| health_score | true | false | patientId |
| cql_evaluate | true | false | patientId |
| cql_batch | true | false | patientId |
| cql_libraries | false | false | - |
| cql_result | true | false | patientId |

### Full-Surface Strategy (52 factory + 16 reused)

Factory tools derive `phi` from a new `phi` field on the resource registry:

| Resource Types | phi |
|---------------|-----|
| Organization, Practitioner, PractitionerRole | false |
| All others (Patient, Observation, Condition, etc.) | true |

Factory audit annotations:
- `*_read`: `{ phi: resource.phi, write: false, patientIdArg: undefined }`
- `*_search`: `{ phi: resource.phi, write: false, patientIdArg: 'patient' }`
- `*_create`: `{ phi: resource.phi, write: true, patientIdArg: undefined }`

### High-Value Strategy (15 unique + 3 reused)

Same rules as composite/full-surface. All FHIR read/search tools are PHI.

## Router Integration

### Signature Change

```js
function createMcpRouter({
  tools, serverName, serverVersion, enforceRoleAuth,
  fixturesDir, logger, rolePolicies,
  phiAuditLogger  // NEW — optional { logToolAccess, logAuthDenied }
})
```

### Private Helper

```js
function extractPhiContext(tool, args) {
  const a = tool.audit;
  if (!a) return { phi: false, write: false, patientId: undefined };
  const patientId = a.patientIdArg ? (args?.[a.patientIdArg] ?? undefined) : undefined;
  return { phi: !!a.phi, write: !!a.write, patientId };
}
```

### 4 Hook Points in handleToolsCall

1. **Auth denial** — after existing `logger.warn`:
   ```js
   phiAuditLogger?.logAuthDenied({ tool: name, role });
   ```

2. **Demo mode success** — after existing `logger.info`:
   ```js
   phiAuditLogger?.logToolAccess({ tool, role, tenantId, patientId, success: true, durationMs, phi, write });
   ```

3. **Handler success** — after existing `logger.info`:
   ```js
   phiAuditLogger?.logToolAccess({ tool, role, tenantId, patientId, success: true, durationMs, phi, write });
   ```

4. **Handler error** — after existing `logger.error`:
   ```js
   phiAuditLogger?.logToolAccess({ tool, role, tenantId, patientId, success: false, durationMs, phi, write });
   ```

### Data Flow

```
POST /mcp → handleToolsCall
  → tool lookup (miss → error, no audit)
  → extractOperatorRole
  → authorizeToolCall (denied → Hook 1: AUTH_DENIED)
  → validateToolParams (invalid → error, no audit)
  → demo fixture? (yes → Hook 2: TOOL_CALL/PHI_ACCESS)
  → tool.handler()
    → success → Hook 3: TOOL_CALL/PHI_ACCESS/PHI_WRITE
    → error → Hook 4: TOOL_CALL/PHI_ACCESS/PHI_WRITE (success: false)
```

## Clinical Server Wiring

```js
const { createPhiAuditLogger } = require('./lib/phi-audit');
const phiAuditLogger = createPhiAuditLogger({ serviceName: 'hdim-clinical-edge' });

app.use(createMcpRouter({
  // ...existing options...
  phiAuditLogger
}));
```

## phi-audit.js Enhancement

Add `phi` boolean to log output (currently used internally but not persisted):

```js
function logToolAccess({ tool, role, tenantId, patientId, success, durationMs, phi, write }) {
  const action = !phi ? 'TOOL_CALL' : write ? 'PHI_WRITE' : 'PHI_ACCESS';
  const record = { action, source: name, tool, role, tenantId, success, durationMs, phi };
  if (phi && patientId) record.patientId = patientId;
  pino.info(record);
}
```

## HIPAA Compliance Status

### Covered by This Design

| Field | Source | HIPAA/NIST |
|-------|--------|-----------|
| timestamp | Pino auto (ISO-8601) | NIST AU-8 |
| action | PHI_ACCESS/PHI_WRITE/TOOL_CALL/AUTH_DENIED | NIST AU-3 |
| tool | Tool name | HIPAA "what" |
| role | x-operator-role header | HIPAA 164.312(a)(2)(i) |
| tenantId | Tool arguments | SOC2 CC6.1 |
| patientId | Tool arguments | HIPAA 164.528 |
| success | Boolean | NIST AU-3 "outcome" |
| durationMs | Timer | SOC2 CC7.2 |
| phi | Boolean flag | HIPAA 164.528 filtering |
| source | Service name | NIST AU-3 "where" |

### Deferred to Layer 4 (requires gateway JWT)

| Field | Why Deferred |
|-------|-------------|
| userId | Requires signed JWT with subject claim |
| sourceIp | Requires X-Forwarded-For from gateway |
| requestId | Requires correlation ID middleware |
| resourceType | Requires tool-level FHIR type extraction |
| purposeOfUse | Requires OAuth scope mapping |

## Files Changed

| File | Change |
|------|--------|
| `mcp-edge-common/lib/mcp-router.js` | Add phiAuditLogger option, extractPhiContext, 4 hooks |
| `mcp-edge-clinical/server.js` | Instantiate + pass phiAuditLogger |
| `mcp-edge-clinical/lib/phi-audit.js` | Persist `phi` field to log output |
| `mcp-edge-clinical/lib/strategies/composite/tools/*.js` (25) | Add audit annotation |
| `mcp-edge-clinical/lib/strategies/high-value/tools/*.js` (~11) | Add audit annotation |
| `mcp-edge-clinical/lib/strategies/full-surface/resource-registry.js` | Add phi boolean |
| `mcp-edge-clinical/lib/strategies/full-surface/tool-factory.js` | Thread phi into audit |

## Testing Strategy

TDD with tests written before implementation:

1. **Router hook tests** — mock phiAuditLogger, verify 4 hooks fire correctly
2. **extractPhiContext unit tests** — missing audit, phi/write/patientIdArg combinations
3. **Annotation contract test** — every clinical tool has a valid `audit` field
4. **PHI audit integration test** — full round-trip: patient_summary → PHI_ACCESS, care_gap_close → PHI_WRITE, care_gap_stats → TOOL_CALL, forbidden → AUTH_DENIED
5. **phi-audit.js updated tests** — verify `phi` field appears in log output
6. **Regression** — all existing test suites pass unchanged
7. **Proof tests** — PHI audit completeness proof (every PHI tool emits audit), write operation proof (every write tool emits PHI_WRITE)
