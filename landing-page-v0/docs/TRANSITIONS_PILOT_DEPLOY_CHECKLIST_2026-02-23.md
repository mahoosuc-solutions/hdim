# Care Transitions Pilot Deploy Checklist

Date: 2026-02-23  
Scope: Publish `/solutions/transitions-of-care` and linked IA updates

## Preflight (Local)

- [x] Build passes (`npm run build`)
- [x] Lint passes (`npm run lint`)
- [x] External link check passes (`npm run validate:links`)
- [x] Route generated in build output:
  - [x] `/solutions/transitions-of-care`
  - [x] `/sales`
  - [x] `/research`
  - [x] `/schedule`
- [x] Sitemap contains new route (`/solutions/transitions-of-care`)
- [x] No public pricing shown on transitions page

## Files Included in This Release

- `app/solutions/transitions-of-care/page.tsx`
- `app/components/LandingPageClient.tsx`
- `components/PortalNav.tsx`
- `app/page.tsx`
- `app/research/page.tsx`
- `app/sales/page.tsx`
- `app/sitemap.ts`

## Vercel Promotion Steps

1. Push branch and open/merge PR for `landing-page-v0` changes.
2. Confirm Vercel preview deployment is green.
3. Smoke test preview URLs:
   1. `/solutions/transitions-of-care`
   2. `/sales`
   3. `/research`
   4. `/schedule`
4. Promote to production in Vercel.

## Post-Deploy Verification (Production)

- [ ] `https://www.healthdatainmotion.com/solutions/transitions-of-care` loads successfully
- [ ] Desktop nav includes `Care Transitions`
- [ ] Mobile nav includes `Care Transitions`
- [ ] Home page links to transitions page from solution/CTA areas
- [ ] Sales page includes pilot blueprint section and CTA
- [ ] Research page includes pilot blueprint section and CTA
- [ ] Sitemap includes `https://www.healthdatainmotion.com/solutions/transitions-of-care`
- [ ] Canonical tag on transitions page points to production URL
- [ ] No pricing figures on transitions page

## Rollback Plan

1. Revert the seven files listed in "Files Included in This Release".
2. Redeploy previous known-good commit from Vercel.
3. Re-validate `/`, `/sales`, `/research`, and `/schedule`.

## Notes

- This release intentionally keeps commercial terms non-public.
- Deployment model messaging explicitly covers vendor-hosted, customer-hosted cloud, and on-prem/private data center options.
