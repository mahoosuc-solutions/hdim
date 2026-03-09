# HIMSS Announcement Launch Brief

Launch date: Tuesday, March 10, 2026  
Prepared on: Monday, March 9, 2026

## 1) Live URLs to use tomorrow

Primary launch surface (Next.js resources):
- `https://landing-page-ecru-five-65.vercel.app/resources/himss-brief`
- `https://landing-page-ecru-five-65.vercel.app/resources/trust-center`
- `https://landing-page-ecru-five-65.vercel.app/resources/evidence-room`
- `https://landing-page-ecru-five-65.vercel.app/resources/cio-ciso`
- `https://landing-page-ecru-five-65.vercel.app/resources/procurement`
- `https://landing-page-ecru-five-65.vercel.app/resources/licensing`

Supporting static portal:
- `https://web-liart-seven-73.vercel.app/github-pages-share.html`
- `https://web-liart-seven-73.vercel.app/licensing-transparency.html`

## 2) Channel plan (LinkedIn + floor motion)

## LinkedIn groups
- Post #1 (morning): problem + proof + `Book Meeting` CTA using HIMSS brief URL.
- Post #2 (midday): trust/evidence angle with Trust Center URL.
- Post #3 (afternoon): partner/investor confidence angle with Licensing URL.

## Guerrilla conversations on floor
- Open with 20-30 second line:
  - "We map healthcare platform claims to proof in real time and keep buyer diligence in one path."
- Route by persona:
  - Technical buyer -> CIO/CISO URL
  - Procurement/partner -> Procurement URL
  - Executive mixed audience -> HIMSS brief URL

## 3) CTA and tracking policy

- Single conversion action: `Book Meeting`.
- UTM standard (already wired):
  - `utm_source`, `utm_medium`, `utm_campaign=himss_2026`, `utm_content`, `utm_term`
- Events expected:
  - `cta_book_meeting_click`
  - `partner_interest_click`
  - `investor_material_view`
  - `evidence_request_submit`

## 4) Morning launch checklist (March 10, 2026)

- [ ] Confirm every URL above returns HTTP 200.
- [ ] Confirm `Book Meeting` CTA opens scheduling page with UTM parameters.
- [ ] Confirm at least one test click appears in analytics events.
- [ ] Confirm evidence request form submit still works.
- [ ] Publish LinkedIn group post set with campaign links.
- [ ] Print or display quick QR set for HIMSS brief / CIO-CISO / Procurement.

## 5) Risk and fallback

Current status:
- Vercel alias URLs above are live and return HTTP 200.
- `www.healthdatainmotion.com/resources/himss-brief` currently returns 404.

Fallback decision:
- Use the deployed alias URLs for tomorrow’s announcement unless custom-domain routing is fixed before launch window.
