## Summary
Implements the remaining post-merge next steps:

1. Adds CI automation for portal deployment validation.
2. Encodes preview validation policy (protected-by-default, optional authenticated audit).
3. Makes CMS collateral pages discoverable from shared nav and index.
4. Publishes latest production evidence references.

## Changes
- Added workflow: `.github/workflows/portal-deployment-validation.yml`
  - Runs `validate-portal-links.sh` and `validate-web-ab-performance.sh` on `docs/marketing/web/**` changes.
  - Supports workflow-dispatch preview checks with optional `VERCEL_AUTOMATION_BYPASS_SECRET`.
- Updated `docs/marketing/web/scripts/validate-portal-links.sh`
  - Supports optional auth headers for protected preview audits.
- Updated navigation/discoverability:
  - `docs/marketing/web/shared-nav.html` (Collateral Assets links)
  - `docs/marketing/web/ai-solutioning-index.html` (cards for collateral pages)
- Updated evidence index:
  - `docs/marketing/web/race-track-fhir-evidence.html`
  - `docs/marketing/web/evidence/validation-summary-20260222T151109Z.md`

## Validation
- Production portal link audit: PASS
  - `docs/marketing/web/evidence/portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T151243Z.md`
- Production web A/B validation (stable pass sample): PASS
  - `docs/marketing/web/evidence/web-validation-20260222T151109Z.md`

## Notes
- Preview deployments remain protected by default; unauthenticated scans return 401 unless bypass secret is configured for workflow dispatch.
