# Race Track + Portal Validation Summary

- Timestamp (UTC): 2026-02-22T15:11:09Z
- Production URL: https://web-gamma-snowy-38.vercel.app
- Preview Policy: Protected preview by default; optional authenticated preview audit in CI via `VERCEL_AUTOMATION_BYPASS_SECRET`.

## Web A/B Validation (Production)

- PASS report: `web-validation-20260222T151109Z.md`
- Performance CSV: `web-performance-20260222T151109Z.csv`
- Vitals CSV: `web-vitals-20260222T151109Z.csv`

## Portal Link Audit (Production)

- PASS report: `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T151052Z.md`
- CSV: `portal-link-audit-web-gamma-snowy-38-vercel-app-20260222T151052Z.csv`
- Result: no internal non-200 links and no sitemap/discoverability gaps.

## Implementation Additions

- Discoverability improvements:
  - Shared-nav “Collateral Assets” links for:
    - `kill-tony-vision-deck.html`
    - `speech-final.html`
    - `validation-report.html`
  - Index cards added for those same pages.
- CI automation added:
  - `.github/workflows/portal-deployment-validation.yml`
  - Runs production portal link audit + web A/B validation on `docs/marketing/web/**` changes.
  - Supports workflow-dispatch preview policy checks and optional authenticated preview link audit.
