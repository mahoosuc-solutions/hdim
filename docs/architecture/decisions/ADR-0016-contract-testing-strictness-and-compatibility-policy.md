# ADR-0016: Contract Testing Strictness and Compatibility Policy

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Platform Lead, QA Lead

## Context
Contract testing is required for reliable service/UI integration. Loose compatibility defaults can hide breaking changes.

## Decision
Adopt strict-by-default contract validation with controlled compatibility mode.
- CI uses strict contract enforcement for release and protected branches.
- Compatibility mode is time-bound and explicitly recorded in release evidence.
- Contract test failures block merge/release unless waived.

## Consequences
### Positive
- Strong interface compatibility guarantees.
- Faster detection of integration regressions.

### Negative
- Short-term friction during transition windows.

## References
- `.github/workflows/contract-testing.yml`
- `apps/clinical-portal/src/test/contracts/`
