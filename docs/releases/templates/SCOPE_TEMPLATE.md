# Release Scope: {VERSION}

**Date:** {DATE}  
**Previous:** {PREVIOUS_VERSION}

## Why This Release Exists

Describe the outcome this version ships (1-3 bullets).

## Included In Scope

- PRs/commits included:
  - (link PRs here)
- User-facing features:
  - ...
- Operational changes:
  - ...

## Explicitly Out Of Scope

- ...

## Demo Readiness Acceptance Criteria

- Demo stack boots from scratch (`docker-compose.demo.yml`).
- Seeding is deterministic (`./scripts/seed-all-demo-data.sh`).
- System validation passes (`./validate-system.sh`).
- Top differentiator flows are screenshot/video capture ready (see `docs/media/CAPTURE_CHECKLIST.md`).

## Release Validation Evidence

List the reports you generated under `docs/releases/{VERSION}/validation/`.

## Rollback Plan

- If a deployment fails:
  - (what to rollback, how to rollback)

