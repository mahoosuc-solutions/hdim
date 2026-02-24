# v2.7.1-rc2 Go/No-Go Checklist

Date: 2026-02-24  
Owner: Release coordination

## Current RC2 Build State
- [x] RC2 release branch prepared locally: `release/v2.7.1-rc2`
- [x] RC2 release commit created locally: `e77a34c2b`
- [x] RC2 tag created locally: `v2.7.1-rc2`
- [ ] RC2 branch pushed to origin
- [ ] RC2 tag pushed to origin

## Scope Locked
- [x] Landing page modern screenshot integration completed.
- [x] Blog includes build-sequence originals and architecture timeline.
- [x] Build Story Evidence Index published (`docs/marketing/web/build-story-evidence.html`).
- [x] Discoverability updates applied (`sitemap.xml`, `ai-solutioning-index.html`).

## Validation Status
- [x] `landing-page-v0 npm run validate:ci` passed (local, 2026-02-24).
- [x] `landing-page-v0 npm run build` passed (local, 2026-02-24).
- [ ] Portal link audit against deployed URL verified from a networked runner.

## Important Constraint
- Local sandbox runs currently show DNS errors for external URLs:
  - Example: `<urlopen error [Errno -3] Temporary failure in name resolution>`
- Result: the latest local `validate-portal-links.sh` output is **not** a trustworthy go/no-go signal for deployment health.

## Required Pre-Release Reruns (networked environment)
Run from repo root:

```bash
bash docs/marketing/web/scripts/validate-portal-links.sh \
  BASE_URL="https://<staging-domain>"
```

Then for production candidate:

```bash
bash docs/marketing/web/scripts/validate-portal-links.sh \
  BASE_URL="https://web-gamma-snowy-38.vercel.app"
```

Acceptance criteria:
- Internal Non-200: none (except explicitly approved expected cases)
- Discoverability gaps: none
- External failures: only approved exceptions

## Release Notes Inputs
- Commit anchors: `0bb549d7c`, `5d4ffea7c`, `3e24bec9f`, `4cb1a4bd9`, `bb422d3c7`, `f2b52c9de`
- Evidence index: `docs/marketing/web/build-story-evidence.html`
- RC2 draft: `docs/marketing/web/evidence/RC2_RELEASE_NOTE_DRAFT_2026-02-24.md`

## Final Go/No-Go
- [ ] Staging deploy successful
- [ ] Networked portal-link audit clean
- [ ] RC2 release note finalized with artifact links
- [ ] Rollback deployment ID recorded
- [ ] Tag created and pushed: `v2.7.1-rc2`

## Final Execution Commands (Networked Runner)
```bash
git checkout release/v2.7.1-rc2
git push origin release/v2.7.1-rc2
git push origin v2.7.1-rc2

# Staging link audit
bash docs/marketing/web/scripts/validate-portal-links.sh \
  BASE_URL="https://<staging-domain>"

# Production candidate link audit
bash docs/marketing/web/scripts/validate-portal-links.sh \
  BASE_URL="https://web-gamma-snowy-38.vercel.app"
```
