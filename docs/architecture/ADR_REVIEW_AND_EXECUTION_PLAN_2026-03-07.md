# ADR Review And Execution Plan (2026-03-07)

## Objective
Establish a single, governed ADR system for HDIM and define the exact ADRs to update or create for the current AI-driven, event-based healthcare platform and regimented development model.

## Scope Reviewed
- `docs/architecture/decisions/`
- `docs/architecture/adr/`
- `backend/docs/architecture/decisions/`
- Release/process docs tied to implementation gates:
  - `docs/releases/AI_HUMAN_RELEASE_ORCHESTRATION.md`
  - `scripts/release-validation/validate-release-preflight.sh`
  - `scripts/release-validation/validate-upstream-ci-gates.sh`
  - `.github/workflows/contract-testing.yml`
  - `.github/workflows/deploy-docker.yml`

## Current State Findings

### 1) ADR namespace fragmentation
Three ADR locations are active:
1. `docs/architecture/decisions/` (primary platform ADRs)
2. `docs/architecture/adr/` (secondary/duplicate numbering)
3. `backend/docs/architecture/decisions/` (AI-agent/backend ADRs)

Impact: architectural truth is split and difficult to govern.

### 2) Duplicate ADR identifiers with conflicting meanings
Examples in `docs/architecture/decisions/`:
- `0005-postgresql-read-replicas.md` and `ADR-0005-hapi-fhir-selection.md`
- `0006-api-gateway-resilience.md`, `ADR-0006-kafka-event-streaming.md`, `ADR-006-tdd-swarm-methodology.md`

Impact: references like “ADR-0006” are ambiguous.

### 3) Inconsistent ADR formats
- Mixed filename schemes: `000x-*`, `ADR-00x-*`, `ADR-0010-*`.
- Some ADRs include full metadata blocks, others do not.
- `docs/architecture/decisions/README.md` process references `_template.md` which does not exist in that directory.

### 4) Recent delivery not fully represented in ADR corpus
Recent implemented capabilities (release orchestration, preflight gating, contract strictness, polling/cache throttling, upstream CI gate enforcement) are operational but not consistently captured as formal architecture decisions.

## Decision: Canonical ADR Governance Model

### Canonical location
Use `docs/architecture/decisions/` as the single platform ADR source of truth.

### Canonical numbering
Use one format only: `ADR-XXXX-<slug>.md` (4 digits).

### Legacy handling
- Keep legacy files for traceability but mark them `Superseded` and point to canonical ADR IDs.
- No new ADRs in `docs/architecture/adr/` or `backend/docs/architecture/decisions/`.
- Backend AI ADRs are migrated into canonical platform ADRs with cross-links.

## ADR Update Backlog (Existing ADRs)

| Priority | ADR/File | Action | Why |
|---|---|---|---|
| P0 | `docs/architecture/decisions/README.md` | Rebuild index with canonical IDs and status table; fix template reference | Removes ambiguity and broken process instructions |
| P0 | `ADR-007-gateway-trust-authentication.md` | Update with current hard requirements: trusted header signature validation, tenant membership enforcement, privileged endpoint RBAC | Align decision with implemented security hardening |
| P0 | `ADR-009-multi-tenant-isolation.md` | Expand from row-level isolation to full request-to-data authorization model (header validation, membership checks, endpoint guards) | Prevents false confidence from DB-only isolation framing |
| P0 | `ADR-011-shared-module-integration.md` | Add final normative module boundary rules and onboarding guardrails as mandatory controls | Codifies repeated startup failure remediation pattern |
| P1 | `ADR-013-hie-data-pipeline.md` | Add contract validation + DLQ tenant-safe handling constraints | Aligns event pipeline decision with operational hardening |
| P1 | `ADR-010-hipaa-phi-cache-ttl.md` | Add explicit interplay with runtime polling/status caching behavior for PHI-safe caching | Connects caching compliance to ops polling controls |
| P1 | `docs/architecture/decisions/0001..0006` + `ADR-0005..0010` | Mark conflicting legacy IDs as superseded and map to canonical ADR IDs | Eliminates duplicated identifier space |
| P1 | `docs/architecture/adr/*` | Mark superseded or migrate content into canonical ADRs | Removes shadow ADR stream |
| P1 | `backend/docs/architecture/decisions/*` | Migrate AI/backend ADR intent into canonical ADR set and mark backend files as pointers | Unifies platform and AI architecture governance |

## ADR Creation Backlog (New ADRs)

### ADR-0014: AI-Human Release Orchestration And Approval Tokens
- Decision scope: mandatory human checkpoints, AI execution boundaries, waiver protocol, and go/no-go authority.
- Primary refs: `docs/releases/AI_HUMAN_RELEASE_ORCHESTRATION.md`, release validation scripts.

### ADR-0015: Preflight Stability Gate As Mandatory Release Precondition
- Decision scope: required stack readiness checks, health criteria, failure semantics, evidence artifact requirements.
- Primary refs: `scripts/release-validation/validate-release-preflight.sh`.

### ADR-0016: Contract Testing Strictness And Compatibility Policy
- Decision scope: strict-by-default contract enforcement, compatibility mode constraints, CI trigger scope.
- Primary refs: `.github/workflows/contract-testing.yml`, shell-app contract test implementation.

### ADR-0017: Runtime Polling Budget And Status Cache Coalescing
- Decision scope: frontend polling clamp, backend status cache TTL/coalescing, anti-thrash requirements.
- Primary refs: `docs/releases/AI_HUMAN_RELEASE_ORCHESTRATION.md`, `tools/ops-server/server.js`, `scripts/README.md`.

### ADR-0018: Upstream CI Security/Performance Freshness Gates For Release Policy
- Decision scope: branch freshness windows, required upstream workflow conclusions, fail-closed behavior.
- Primary refs: `scripts/release-validation/validate-upstream-ci-gates.sh`, `.github/workflows/deploy-docker.yml`.

### ADR-0019: Release Evidence Provenance And Artifact Retention
- Decision scope: mandatory evidence artifacts per phase, retention windows, required metadata in reports.
- Primary refs: `docs/releases/<version>/validation/*`, release workflow artifacts.

### ADR-0020: Event Pipeline Failure Handling And Tenant-Safe DLQ Controls
- Decision scope: tenant-scoped DLQ access, mutation role requirements, retry/exhaust governance.
- Primary refs: event-processing security tests and controller/service behavior.

### ADR-0021: AI Agent Platform Governance (PHI, Tooling, Versioning)
- Decision scope: unify AI-agent architectural decisions currently in backend-only ADR stream (provider strategy, memory safety, version rollback) under platform ADR governance.
- Primary refs: `backend/docs/architecture/decisions/0001-0004`.

## Regimented Development Requirements (ADR Lifecycle)

1. ADR required before implementation for any architecture-impacting change.
2. ADR must include:
   - Decision drivers
   - considered alternatives
   - explicit operational constraints
   - validation/evidence plan
   - rollback/supersession criteria
3. PR policy:
   - Architecture-impacting PRs must link ADR ID
   - CI blocks merge if `adr-required` label is present and no ADR linked
4. Release policy:
   - No release candidate without all P0 ADRs accepted and cross-linked to runbooks
5. Supersession policy:
   - Changed decisions require new ADR; old ADR marked `Superseded by ADR-XXXX`

## Execution Plan (Two Sprint Waves)

### Wave 1 (P0 closure)
1. Canonicalize ADR index and naming policy.
2. Update ADR-007, ADR-009, ADR-011 to match current implementation.
3. Create ADR-0014, ADR-0015, ADR-0016.
4. Add cross-links from release docs to these ADRs.

### Wave 2 (P1 closure)
1. Create ADR-0017 through ADR-0021.
2. Migrate backend/docs ADR intent into canonical ADRs.
3. Mark legacy ADR files as superseded with pointer headers.
4. Add ADR compliance checks to architecture/release checklist docs.

## Ownership Model
- Chief Architect / Platform Lead: ADR acceptance authority.
- Security Lead: co-approver for ADR-007/009/0018/0020/0021.
- Release Manager: co-approver for ADR-0014/0015/0019.
- Service Owners: implement and link evidence to ADR verification sections.

## Acceptance Criteria For This ADR Program
- Single canonical ADR index in `docs/architecture/decisions/README.md`.
- Zero unresolved duplicate ADR identifiers.
- All new release/security/process controls represented by accepted ADRs.
- Architecture-impacting PR template requires ADR link.
- Release candidate checklist references ADR IDs for each enforced gate.

## Execution Status (2026-03-07)

This plan has been executed in documentation as follows:
- Wave 1 completed: canonical index updated, ADR-007/009/011 updated, ADR-0014/0015/0016 created.
- Wave 2 completed: ADR-0017/0018/0019/0020/0021 created; legacy ADR streams marked superseded with canonical pointers.
