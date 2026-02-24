# RC2 Release Note Draft (2026-02-24)

## Scope
- Landing page narrative and visuals updated to highlight modern clinical UI and architecture-led delivery.
- Blog now includes both:
  - Original live build screenshots (with browser shell) for build authenticity.
  - Commit-anchored architecture timeline for leadership narrative.
- New release artifact hub: `build-story-evidence.html`.
- RC2 packaging commit prepared locally: `e77a34c2b` on `release/v2.7.1-rc2`.
- RC2 tag prepared locally: `v2.7.1-rc2`.

## Key Commits to Reference
- `0bb549d7c`: GTM landing production baseline and validation closure.
- `5d4ffea7c`: `/performance` content and traditional-vs-HDIM proof framing.
- `3e24bec9f`: Race Track FHIR A/B validation and evidence publication.
- `4cb1a4bd9`: Automated portal deployment validation workflow.
- `bb422d3c7`: Care transitions pilot integration across public surfaces.
- `f2b52c9de`: Vercel/runtime hardening for resilient lead handling.

## Evidence Links
- Build Story Evidence Index: `build-story-evidence.html`
- Blog Post: `blog-post-ai-solutioning.html`
- AI Solutioning Index: `ai-solutioning-index.html`
- Validation Report: `validation-report.html`
- Latest portal link audit:
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T225355Z.md`
  - `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T225355Z.csv`

## Validation Summary
- `landing-page-v0 npm run validate:ci` passed on 2026-02-24.
- `landing-page-v0 npm run build` passed on 2026-02-24.
- `docs/marketing/web/scripts/validate-portal-links.sh` executed and produced fresh audit artifacts.
- Networked runner validation complete:
  - Production deploy completed and aliased to `https://web-gamma-snowy-38.vercel.app`.
  - Production audit (`20260224T234533Z`) reports:
    - Internal Non-200: none
    - External Non-200/3xx: none
    - Discoverability gaps: none

## Release Notes Highlights (External-Facing)
- Architecture decisions are now directly tied to reproducible evidence.
- Product visuals now show current, production-like clinical workflows.
- Performance claims remain grounded in measured validation artifacts.

## Go/No-Go Checklist for RC2
- [x] CI green on release branch
- [x] Staging deploy successful
- [x] Staging portal-link audit generated and attached
- [x] Release notes include commit anchors + evidence index link
- [x] Rollback target deployment ID recorded
- [x] Branch and tag pushed from a networked environment

## Latest Local Artifacts
- `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T225355Z.md`
- `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T225957Z.md` (DNS-constrained local run)
- `portal-link-audit-web-au6r5ywn9-mahooosuc-solutions-vercel-app-20260224T234251Z.md` (preview environment; auth-protected responses)
- `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T234533Z.md` (production clean run)

## Remaining Release Commands
```bash
# Completed:
# git checkout release/v2.7.1-rc2
# git push origin release/v2.7.1-rc2
# git push origin v2.7.1-rc2 --force
```
