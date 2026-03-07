# ADR-0017: Runtime Polling Budget and Status Cache Coalescing

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Platform Lead, SRE Lead

## Context
Unbounded status polling can overload operational services and create noisy runtime behavior. Polling must be constrained and coalesced.

## Decision
Enforce a polling budget and backend cache-coalescing policy.
- Frontend operational polling interval is clamped (`2000-60000ms`, default `10000ms`).
- Ops status responses are cached/coalesced server-side (default TTL `5000ms`, minimum `500ms`).
- Force-refresh paths must be explicit and auditable.

## Consequences
### Positive
- Reduced control-plane churn and improved stability.
- Predictable operator UX under load.

### Negative
- Slightly stale status windows under cache TTL.

## References
- `tools/ops-server/server.js`
- `scripts/README.md`
- `docs/releases/AI_HUMAN_RELEASE_ORCHESTRATION.md`
