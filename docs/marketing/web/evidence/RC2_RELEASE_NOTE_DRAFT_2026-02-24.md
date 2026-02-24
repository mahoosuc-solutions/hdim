# RC2 Release Note Draft (2026-02-24)

## Scope
- Landing page narrative and visuals updated to highlight modern clinical UI and architecture-led delivery.
- Blog now includes both:
  - Original live build screenshots (with browser shell) for build authenticity.
  - Commit-anchored architecture timeline for leadership narrative.
- New release artifact hub: `build-story-evidence.html`.

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
- Note: latest local sandbox run reports DNS resolution failures (`[Errno -3] Temporary failure in name resolution`) and must be re-run from a networked runner before final go/no-go.

## Release Notes Highlights (External-Facing)
- Architecture decisions are now directly tied to reproducible evidence.
- Product visuals now show current, production-like clinical workflows.
- Performance claims remain grounded in measured validation artifacts.

## Go/No-Go Checklist for RC2
- [ ] CI green on release branch
- [ ] Staging deploy successful
- [ ] Staging portal-link audit generated and attached
- [ ] Release notes include commit anchors + evidence index link
- [ ] Rollback target deployment ID recorded

## Latest Local Artifacts
- `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T225355Z.md`
- `portal-link-audit-web-gamma-snowy-38-vercel-app-20260224T225957Z.md` (DNS-constrained local run)
