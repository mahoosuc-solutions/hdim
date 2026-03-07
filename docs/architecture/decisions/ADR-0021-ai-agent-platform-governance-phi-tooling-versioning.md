# ADR-0021: AI Agent Platform Governance (PHI, Tooling, Versioning)

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Platform Lead, Security Lead, Compliance Lead

## Context
AI capabilities are part of the production platform and require explicit architecture governance across provider strategy, PHI safety, and version rollback controls.

## Decision
Unify AI architecture governance into canonical platform ADR policy.
- AI platform decisions are governed in `docs/architecture/decisions/`.
- PHI-safe memory handling and data minimization are mandatory.
- Tooling access is policy-governed and auditable.
- Agent versioning must support deterministic rollback.

## Consequences
### Positive
- AI architecture decisions become first-class, auditable platform controls.
- Consistent governance across core services and AI runtime.

### Negative
- Requires migration of backend-local ADR streams into canonical references.

## References
- `backend/docs/architecture/decisions/0001-multi-provider-llm-architecture.md`
- `backend/docs/architecture/decisions/0002-no-code-agent-builder.md`
- `backend/docs/architecture/decisions/0003-phi-safe-memory-management.md`
- `backend/docs/architecture/decisions/0004-agent-versioning-strategy.md`
