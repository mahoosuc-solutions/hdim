# BSL Launch Operational Validation

Date: 2026-03-10 13:28 UTC  
Owner: Platform + Compliance + GTM  
Status: PASS

## Scope

This run validates the remaining BSL launch operational controls before morning launch execution:
- Launch URLs and transparency routes return HTTP 200.
- Controlled-content scan passes on public artifact paths.
- Landing page build/type checks pass.
- HIMSS collateral routes are aligned to the live launch host.

## Executed Checks

## 1) BSL Launch Ops Gate Script

Command:
- `scripts/validation/validate-bsl-launch-ops.sh`

Artifact outputs:
- `validation-reports/bsl-launch-ops-summary.md`
- `validation-reports/bsl-launch-ops-summary.json`

Result:
- PASS

Validated controls:
- `docs/compliance/BSL_RELEASE_PLAN.md` exists
- `docs/compliance/LICENSING-BOUNDARY.md` exists
- `docs/compliance/THIRD_PARTY_NOTICES.md` exists
- `landing-page/src/app/resources/licensing/LicensingTransparencyHub.tsx` exists
- `https://hdim-himss.vercel.app/resources/himss-brief` returns 200
- `https://hdim-himss.vercel.app/resources/trust-center` returns 200
- `https://hdim-himss.vercel.app/resources/evidence-room` returns 200
- `https://hdim-himss.vercel.app/resources/procurement` returns 200
- `https://hdim-himss.vercel.app/resources/licensing` returns 200
- `https://hdim-himss.vercel.app/terms` returns 200
- Controlled-content filename scan passes for:
  - `landing-page/public`
  - `docs/marketing/web`
  - `docs/marketing/himss/print-deck-2026-03-10`

## 2) Landing Page Build Validation

Commands:
- `npm -C landing-page run type-check`
- `npm -C landing-page run build`

Result:
- PASS

## 3) Event Tracking Wiring Validation (Build Artifact)

Validation method:
- Searched built JS chunks for required event names.

Required events found:
- `cta_book_meeting_click`
- `evidence_request_submit`
- `partner_interest_click`
- `investor_material_view`

## 4) HIMSS Content Route Alignment

Launch-critical collateral updated from legacy alias to:
- `https://hdim-himss.vercel.app/resources/*`

Updated artifacts include:
- `docs/runbooks/HIMSS_ANNOUNCEMENT_DAY_OF_RUNBOOK.md`
- `docs/marketing/himss/HIMSS_ANNOUNCEMENT_LAUNCH_BRIEF_2026-03-10.md`
- `docs/marketing/himss/HIMSS_ANNOUNCEMENT_SIGNOFF_2026-03-08.md`
- `docs/gtm/HIMSS_ONE_PAGER_FRONT_BACK_COPY_2026-03-10.md`
- `docs/marketing/himss/print-deck-2026-03-10/01-hdim-overview-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/04-trust-center-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/05-cio-ciso-path-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/06-procurement-partner-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/07-licensing-transparency-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/09-himss-front-back-onepager.html`

## 5) Remaining External Dependency

Custom domain status at validation time:
- `https://www.healthdatainmotion.com/resources/*` still returned 404.

Operational decision:
- Use `https://hdim-himss.vercel.app/resources/*` as the canonical launch host for this morning window.

## Go/No-Go

- Go for BSL launch execution this morning using `hdim-himss.vercel.app` routes.
