# MCP Implementation Reference Specification

Version: `1.0`  
Status: `Draft (normative for current implementation + planned extensions)`  
Last Updated: `2026-02-16`

## 1. Purpose

- Define a centralized MCP control plane for HDIM service lifecycle, configuration, validation, and release governance.
- Standardize tool contracts, safety guardrails, evidence artifacts, and operator workflows.

## 2. Scope

- In scope:
  - Docker Compose orchestration and status
  - Service health/readiness validation
  - Tenant isolation policy checks
  - Release-gate and evidence generation
  - Centralized service config and restart orchestration
- Out of scope:
  - Secret manager ownership
  - Kubernetes-native controllers
  - Direct tenant business-data mutation

## 3. Runtime & Identity

- MCP server name: `hdim-docker`
- Transport: `stdio`
- Default compose file: `docker-compose.demo.yml`
- Default gateway base URL: `http://localhost:18080`

## 4. Design Principles

- Safe-by-default mutation (`dryRun` where applicable)
- Deterministic validation (timeouts, warmup, stability passes)
- Evidence-first operations (JSON + Markdown artifacts)
- Policy-enforced operations (`strict` vs `permissive`)
- Reproducible local/WSL/CI behavior

## 5. Capability Domains

- Discovery: topology, inventory, runtime status
- Configuration: contracts, drift detection, controlled apply
- Operations: start/stop/restart orchestration
- Validation: gateway, readiness, tenant isolation
- Governance: release gate, evidence, artifact diff

## 6. Current Tool Surface (Normative)

### 6.1 Infrastructure & Operations

- `hdim_docker_info`
- `hdim_docker_ps`
- `hdim_docker_up`
- `hdim_docker_down`
- `hdim_docker_logs`

### 6.2 Validation

- `hdim_gateway_validate`
- `hdim_demo_seed`
- `hdim_system_validate`
- `hdim_seed_diagnostics`
- `hdim_live_readiness`

### 6.3 Governance & Central Management

- `hdim_topology_report`
- `hdim_config_audit`
- `hdim_tenant_isolation_check`
- `hdim_release_gate`
- `hdim_service_catalog`
- `hdim_service_config_contracts`
- `hdim_policy_registry`
- `hdim_service_restart_plan`
- `hdim_service_operate`

### 6.4 Release Evidence

- `hdim_release_artifact_diff`
- `hdim_release_evidence_pack`

## 7. Common Request Conventions

- Optional input conventions:
  - `composeFile`
  - `requestTimeoutSecs`
  - `policyMode` (`strict|permissive`)
- Mutation conventions:
  - `dryRun` for preview-first operations
- Warmup conventions:
  - `warmupTimeoutSecs`
  - `pollIntervalMs`
  - `stablePasses`

## 8. Response Contract

- MCP response payload is JSON in `content[0].text`.
- Expected fields by tool class:
  - Command tools: `ok`, `exitCode`, `timedOut`, `command`, `stdout`, `stderr`
  - Validation tools: `checks[]`, `allHealthy`, `ready`, `history[]`
  - Governance tools: `pass`, `summary`, `warnings[]`, `violations[]`
- Recoverable runtime failure should remain structured (`ok: false` and/or `error`).

## 9. Warmup & Readiness Semantics

- `hdim_gateway_validate` checks:
  - `/actuator/health`
  - `/fhir/metadata`
- Warmup behavior:
  - Retry checks until stable healthy passes or timeout deadline
  - Capture per-attempt history in `warmup.history[]`
  - `allHealthy=true` only when latest result set is healthy

## 10. Policy Model

- `strict`: policy violations fail release gate
- `permissive`: policy violations may become warnings
- Policy inputs:
  - tool args
  - `scripts/mcp/release-gate-policy.json` profiles
- Tenant isolation supports no-header allowlist and differentiation checks.

## 11. Service Registry Model

- Canonical service metadata:
  - `name`, `category`, `owner`, `dependencies[]`, optional `healthEndpoint`
- Configuration contract:
  - required keys, optional keys, defaults, changeability policy
- Operational contract:
  - restart ordering and readiness criteria

## 12. Release Gate Decision Contract

- Required output:
  - `pass`
  - `summary.runningServices`
  - `summary.missingRequiredConfig`
  - `summary.tenantHeaderEnforcedCount`
  - `summary.tenantPolicyMode`
  - `summary.tenantPolicyPass`
  - `summary.readiness`

## 13. Evidence Artifact Contract

- Core artifacts:
  - `release-gate-<timestamp>.json`
  - `release-gate-<timestamp>.md`
  - `release-evidence-pack-<timestamp>.json`
  - `release-evidence-pack-<timestamp>.md`
- Bundle artifact:
  - `deployment-evidence-<timestamp>/DEPLOYMENT_EVIDENCE_SUMMARY.md`
- Optional operation artifacts:
  - `controlled-restart-<timestamp>.json/.md`
  - smoke transcripts and summaries

## 14. Security & Safety Requirements

- Do not emit secrets in artifacts/log excerpts
- Require explicit mutate intent (`dryRun: false` for live mutations)
- Bound all external calls with timeouts
- Preserve operation traceability and auditability

## 15. Observability Requirements

- Record command previews for mutate tools
- Record retry/warmup attempt history for health checks
- Include explicit warning/violation reason strings
- Truncate large outputs to bounded sizes

## 16. Testing Requirements

- Unit tests must cover:
  - Tool registration and schema behavior
  - Warmup/retry behavior
  - Policy decision behavior
  - Artifact generation and parsing
- Integration smoke should cover:
  - initialize + tools/list
  - read-only call
  - guarded mutation dry-run
  - controlled live restart flow (environment permitting)
- CI policy:
  - run on code changes only (no nightly requirement)

## 17. CLI Runner Contracts

- `npm run mcp:release-gate`
- `npm run mcp:evidence-pack`
- `npm run mcp:evidence-package`
- `npm run mcp:controlled-restart`
- `npm run mcp:pretest`

Each runner must emit structured JSON output and stable artifact paths.

## 18. Exit Codes

- `0`: pass/success
- `2`: executed but failed policy/readiness/gate criteria
- `1`: runtime/config/tooling failure

## 19. Planned Extensions (v1.1)

- `hdim_service_config_get`
- `hdim_service_config_set` (policy-gated)
- `hdim_service_config_diff`
- `hdim_service_config_apply` (transactional + rollback handle)
- Dependency-aware rolling orchestration with convergence gates
- Central change-journal artifact integrated into deployment bundle

## 20. Tool Schema Appendix (Reference)

This appendix provides normalized request/response keys for high-use tools.
All payloads are returned as JSON text in `content[0].text`.

### 20.1 `hdim_docker_ps`

- Inputs:
  - `composeFile?: string`
- Response keys:
  - `dockerBin`, `composeFile`, `command`
  - `ok`, `exitCode`, `timedOut`
  - `stdout`, `stderr`

### 20.2 `hdim_service_operate`

- Inputs:
  - `composeFile?: string`
  - `action: "start" | "stop" | "restart"`
  - `services: string[]`
  - `dryRun?: boolean` (default `true`)
- Response keys:
  - `composeFile`, `action`, `services`
  - `dryRun`, `commandPreview`
  - `result` (present when `dryRun=false`, command result envelope)

### 20.3 `hdim_gateway_validate`

- Inputs:
  - `baseUrl?: string`
  - `requestTimeoutSecs?: number`
  - `warmupTimeoutSecs?: number`
  - `pollIntervalMs?: number`
  - `stablePasses?: number`
- Response keys:
  - `baseUrl`, `allHealthy`, `checks[]`
  - `warmup.requestTimeoutSecs`
  - `warmup.warmupTimeoutSecs`
  - `warmup.pollIntervalMs`
  - `warmup.stablePassesRequired`
  - `warmup.attempts`
  - `warmup.achievedStablePasses`
  - `warmup.consecutiveHealthyPasses`
  - `warmup.history[]`

### 20.4 `hdim_release_gate`

- Inputs:
  - `composeFile?: string`
  - `gatewayUrl?: string`
  - `demoSeedingUrl?: string`
  - `tenantA?: string`, `tenantB?: string`
  - `policyMode?: "strict" | "permissive"`
  - `noHeaderAllowlist?: string[]`
  - `requestTimeoutSecs?: number`
  - `includeLogs?: boolean`
  - `runSystemValidate?: boolean`
  - `skipFrontend?: boolean`
  - `skipFhirQuery?: boolean`
- Response keys:
  - `pass`
  - `summary.runningServices`
  - `summary.missingRequiredConfig`
  - `summary.tenantHeaderEnforcedCount`
  - `summary.tenantPolicyMode`
  - `summary.tenantPolicyPass`
  - `summary.readiness`
  - `tenantIsolation.warnings[]`
  - `tenantIsolation.violations[]`

### 20.5 `hdim_release_evidence_pack`

- Inputs:
  - `reportDir?: string`
  - `outputDir?: string`
  - `includeDiff?: boolean`
- Response keys:
  - `reportDir`, `outputDir`
  - `latestReportPath`
  - `evidencePackJson`, `evidencePackMarkdown`
  - `includeDiff`, `diff`

### 20.6 `hdim_tenant_isolation_check`

- Inputs:
  - `gatewayUrl?: string`
  - `tenantA?: string`, `tenantB?: string`
  - `policyMode?: "strict" | "permissive"`
  - `noHeaderAllowlist?: string[]`
  - `requestTimeoutSecs?: number`
  - `endpoints?: string[]`
  - `requireTenantDifferentiation?: boolean`
- Response keys:
  - `policyMode`
  - `checks[]` (no-header and per-tenant comparisons)
- `warnings[]`
- `violations[]`
- `pass`

## 21. Example Payload Appendix

The following examples are canonical reference shapes (fields may include additional metadata in runtime output).
Machine-readable companions:

- `docs/runbooks/MCP_IMPLEMENTATION_REFERENCE_SPEC.schema.json`
- `docs/runbooks/MCP_IMPLEMENTATION_REFERENCE_SPEC.examples.json`

### 21.1 `hdim_docker_ps` example

Request:

```json
{
  "name": "hdim_docker_ps",
  "arguments": {
    "composeFile": "docker-compose.demo.yml"
  }
}
```

Response payload excerpt:

```json
{
  "dockerBin": "docker",
  "composeFile": "docker-compose.demo.yml",
  "command": "docker compose -f docker-compose.demo.yml ps",
  "ok": true,
  "exitCode": 0
}
```

### 21.2 `hdim_service_operate` dry-run example

Request:

```json
{
  "name": "hdim_service_operate",
  "arguments": {
    "composeFile": "docker-compose.demo.yml",
    "action": "restart",
    "services": [
      "gateway-edge"
    ],
    "dryRun": true
  }
}
```

Response payload excerpt:

```json
{
  "action": "restart",
  "services": [
    "gateway-edge"
  ],
  "dryRun": true,
  "commandPreview": "docker compose -f docker-compose.demo.yml restart gateway-edge"
}
```

### 21.3 `hdim_gateway_validate` warmup example

Request:

```json
{
  "name": "hdim_gateway_validate",
  "arguments": {
    "baseUrl": "http://localhost:18080",
    "requestTimeoutSecs": 5,
    "warmupTimeoutSecs": 120,
    "pollIntervalMs": 1000,
    "stablePasses": 1
  }
}
```

Response payload excerpt:

```json
{
  "allHealthy": true,
  "checks": [
    {
      "endpoint": "/actuator/health",
      "status": 200,
      "ok": true
    },
    {
      "endpoint": "/fhir/metadata",
      "status": 200,
      "ok": true
    }
  ],
  "warmup": {
    "attempts": 1,
    "achievedStablePasses": true,
    "stablePassesRequired": 1
  }
}
```

### 21.4 `hdim_release_gate` example

Request:

```json
{
  "name": "hdim_release_gate",
  "arguments": {
    "composeFile": "docker-compose.demo.yml",
    "gatewayUrl": "http://localhost:18080",
    "policyMode": "strict",
    "requestTimeoutSecs": 8
  }
}
```

Response payload excerpt:

```json
{
  "pass": false,
  "summary": {
    "runningServices": 19,
    "tenantPolicyMode": "strict",
    "tenantPolicyPass": false,
    "readiness": true
  }
}
```

### 21.5 `hdim_release_evidence_pack` example

Request:

```json
{
  "name": "hdim_release_evidence_pack",
  "arguments": {
    "reportDir": "logs/mcp-reports",
    "outputDir": "logs/mcp-reports",
    "includeDiff": true
  }
}
```

Response payload excerpt:

```json
{
  "latestReportPath": "logs/mcp-reports/release-gate-20260216-193559.json",
  "evidencePackJson": "logs/mcp-reports/release-evidence-pack-20260216-193559.json",
  "evidencePackMarkdown": "logs/mcp-reports/release-evidence-pack-20260216-193559.md",
  "includeDiff": true
}
```
## 22. Failure-Mode Appendix

### 22.1 Gateway warmup timeout (readiness false)

- Trigger: `hdim_gateway_validate` exhausts warmup attempts before stable healthy state.
- Expected signal:
  - `allHealthy: false`
  - `warmup.achievedStablePasses: false`
  - `checks[].status` may be `null` with `error: "fetch failed"` during startup windows.
- Operator action:
  - verify `docker compose ps` health convergence
  - rerun validation with larger `warmupTimeoutSecs`

### 22.2 Strict tenant policy violation

- Trigger: `hdim_release_gate` in `strict` mode with tenant header policy not enforced.
- Expected signal:
  - `pass: false`
  - `summary.tenantPolicyMode: "strict"`
  - `summary.tenantPolicyPass: false`
  - `tenantIsolation.violations[]` populated
- Operator action:
  - review no-header allowlist scope
  - enforce gateway header checks before promoting

### 22.3 Docker socket permission denied

- Trigger: MCP tool execution without Docker daemon access.
- Expected signal:
  - command envelope `ok: false`
  - `stderr` includes docker socket permission error
  - downstream readiness/gate checks fail deterministically
- Operator action:
  - run via approved elevated execution path
  - confirm Docker Desktop/WSL integration and daemon availability
