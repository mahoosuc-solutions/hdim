# Care Transitions Pilot: Release Note + PR Description

Date: 2026-02-23  
Area: Public portal (`landing-page-v0`)  
Primary URL: `/solutions/transitions-of-care`

## Release Note (Copy/Paste)

### Summary
Published a new public, indexed solution page for the Care Transitions Pilot and integrated it across the portal navigation and core conversion pages.

### What is new

- New public route: `/solutions/transitions-of-care`
- New public content:
  - Pilot objective and Phase 1 scope
  - Security baseline (minimum-necessary PHI handling, request-scoped AI context, auditability)
  - Deployment options (vendor-hosted, customer-hosted cloud, on-prem/private data center, air-gapped)
  - KPI framework and 90-day delivery timeline
- IA integrations:
  - Global nav and mobile nav links
  - Home page CTA and footer links
  - Research page pilot blueprint callout
  - Sales page pilot offer section
- SEO/indexing:
  - Canonical metadata on new page
  - Sitemap inclusion for `/solutions/transitions-of-care`

### Guardrails enforced

- No public pricing figures shown
- No customer-identifying content included
- Clear Phase 1 vs later-phase boundaries

### Validation completed

- `npm run build` (pass)
- `npm run lint` (pass)
- `npm run validate:links` (pass)
- Build artifact verification:
  - New route generated
  - Canonical tag present
  - Sitemap entry present

---

## PR Description (Copy/Paste)

### Title
Add public Care Transitions solution page and integrate across portal IA

### Why
We needed a public, indexable destination that communicates the Care Transitions pilot model clearly for provider and payer buyers, while preserving compliance and commercial guardrails (no public pricing, no internal-only details).

### What changed

#### New page

- Added `app/solutions/transitions-of-care/page.tsx`
  - Metadata + canonical for SEO
  - Pilot objective, scope boundaries, security baseline
  - Deployment model matrix including on-prem/private data center
  - KPI model and 90-day timeline
  - Workshop CTA to `/schedule`

#### Navigation and IA wiring

- Updated `app/components/LandingPageClient.tsx`
  - Desktop and mobile nav link for Care Transitions
- Updated `components/PortalNav.tsx`
  - Added `Transitions Pilot` nav item
- Updated `app/page.tsx`
  - Added homepage CTA path to new solution page
  - Added footer solution link
- Updated `app/research/page.tsx`
  - Added pilot blueprint callout + CTA
- Updated `app/sales/page.tsx`
  - Added focused Care Transitions pilot offer section + CTA

#### SEO

- Updated `app/sitemap.ts`
  - Added `/solutions/transitions-of-care`

#### Operations docs

- Added `docs/TRANSITIONS_PILOT_DEPLOY_CHECKLIST_2026-02-23.md`

### Validation

- [x] `npm run build`
- [x] `npm run lint`
- [x] `npm run validate:links`
- [x] Route present in build output: `/solutions/transitions-of-care`
- [x] Canonical rendered for new route
- [x] Sitemap includes new route

### Risk and rollback

- Risk: low (content/IA/SEO additions only)
- Rollback: revert changed files listed in deploy checklist and redeploy previous known-good build
