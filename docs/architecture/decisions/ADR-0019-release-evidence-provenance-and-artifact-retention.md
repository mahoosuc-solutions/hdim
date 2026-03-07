# ADR-0019: Release Evidence Provenance and Artifact Retention

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Release Manager, Compliance Lead

## Context
Investor and compliance-grade releases require reproducible proof, not narrative claims.

## Decision
Require structured release evidence artifacts with provenance metadata.
- Each gate produces a timestamped artifact under `docs/releases/<version>/validation/`.
- Artifacts include command, environment, result, and go/no-go implication.
- Retention and discoverability are mandatory for release records.

## Consequences
### Positive
- High-trust release audits and faster incident forensics.

### Negative
- More documentation overhead during release cycles.

## References
- `docs/releases/*/validation/`
- `docs/releases/AI_HUMAN_RELEASE_ORCHESTRATION.md`
