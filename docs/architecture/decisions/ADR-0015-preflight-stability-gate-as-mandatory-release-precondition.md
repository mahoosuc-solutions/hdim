# ADR-0015: Preflight Stability Gate as Mandatory Release Precondition

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Release Manager, SRE Lead

## Context
Release validation without verified baseline stack health creates false negatives/positives and wastes incident budget.

## Decision
Make preflight stability validation mandatory before release phase execution.
- Required services must be `Up` and `healthy`.
- Preflight artifacts must be generated before Phase 1 approval.
- Preflight failure is automatic `NO-GO` until remediated.

## Consequences
### Positive
- Consistent validation starting state.
- Early detection of environmental failures.

### Negative
- Additional startup dependency on infra health.

## References
- `scripts/release-validation/validate-release-preflight.sh`
- `docs/releases/*/validation/preflight-stability-report.md`
