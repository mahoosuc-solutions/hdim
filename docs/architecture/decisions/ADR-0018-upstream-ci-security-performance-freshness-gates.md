# ADR-0018: Upstream CI Security/Performance Freshness Gates for Release Policy

**Status**: Accepted
**Date**: 2026-03-07
**Deciders**: Release Manager, Security Lead

## Context
Release decisions must reflect recent upstream security and performance outcomes, not stale historical passes.

## Decision
Make upstream CI gate freshness a hard release policy requirement.
- Required upstream workflows: security scan and performance gate.
- Latest completed runs on target branch must be successful and within max age policy.
- Missing credentials/metadata for gate checks fail closed.

## Consequences
### Positive
- Release posture is anchored to current risk signal.
- Eliminates stale-pass blind spots.

### Negative
- Additional dependency on CI metadata and API access.

## References
- `scripts/release-validation/validate-upstream-ci-gates.sh`
- `.github/workflows/deploy-docker.yml`
