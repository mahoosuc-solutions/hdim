# ADR-0014: AI-Human Release Orchestration and Approval Tokens

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Release Manager, Platform Lead

## Context
Release automation is AI-assisted, but final accountability is human. A formal phase-gated handoff model is required to prevent unreviewed progression through critical release steps.

## Decision
Adopt mandatory AI-human phase orchestration with approval tokens between phases.
- AI executes validation phases and publishes evidence artifacts.
- Human release manager approves phase transitions.
- No automatic phase progression across approval boundaries.
- Waivers require owner, risk, mitigation, and expiration.

## Consequences
### Positive
- Clear accountability and auditable go/no-go decisions.
- Controlled automation with explicit human oversight.

### Negative
- Slower progression if approvers are unavailable.

## References
- `docs/releases/AI_HUMAN_RELEASE_ORCHESTRATION.md`
- `scripts/release-validation/run-release-validation.sh`
