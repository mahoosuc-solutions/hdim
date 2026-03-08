# HIMSS Announcement Day-Of Runbook

Last updated: 2026-03-08  
Owners: GTM Lead, Product Lead, Technical Founder

## Objective

Drive one conversion action from all conference channels:
- `Book Meeting` (customer pipeline first, strategic partner and investor follow-up in the same flow).

## Canonical link set

Use these links in all outreach:

- Campaign hub: `https://healthdatainmotion.com/resources/himss-brief`
- Trust and proof: `https://healthdatainmotion.com/resources/trust-center`
- Gated diligence: `https://healthdatainmotion.com/resources/evidence-room`
- CIO/CISO path: `https://healthdatainmotion.com/resources/cio-ciso`
- Procurement path: `https://healthdatainmotion.com/resources/procurement`
- Licensing transparency: `https://healthdatainmotion.com/resources/licensing`

## Channel execution blocks

## 1) LinkedIn groups

- Post format:
  - Problem signal (quality and operational friction)
  - One proof line (trust center or evidence room)
  - One CTA line: "Book Meeting"
- Link target:
  - Start with HIMSS brief link.
- Tracking expectation:
  - Meeting CTA carries `utm_campaign=himss_2026`.

## 2) Floor guerrilla conversations

- 30-second talk track:
  - "We map claims to evidence in real time and keep buyer diligence in one path."
  - "If this is relevant, book a 15-minute review now."
- QR routing:
  - Default QR points to HIMSS brief.
  - Technical QR points to CIO/CISO path.
  - Commercial QR points to procurement path.

## 3) Partner conversations

- Positioning:
  - Co-sell and integration alignment.
- Link path:
  - HIMSS brief partner section -> Procurement path -> Book Meeting.

## 4) Investor conversations

- Positioning:
  - Execution discipline, release evidence, and licensing transparency.
- Link path:
  - HIMSS brief investor section -> Trust center + Licensing page -> Book Meeting.

## Go/No-Go checklist (T-12 hours)

- [ ] All canonical routes return 200.
- [ ] Book Meeting CTA visible and clickable on:
  - `/resources`
  - `/resources/himss-brief`
  - `/resources/trust-center`
  - `/resources/evidence-room`
  - `/resources/cio-ciso`
  - `/resources/procurement`
- [ ] Evidence request form submits and logs tracking events.
- [ ] No stale date markers in HIMSS-facing content (use March 2026 baseline).
- [ ] Licensing transparency page is reachable from resources and terms.

## Daily operating rhythm

- Morning (pre-floor):
  - Validate links and CTA behavior.
  - Pin today’s LinkedIn group post copy.
- Midday:
  - Check booked meetings and evidence requests.
  - Adjust talk track based on objections heard.
- End of day:
  - Publish summary metrics:
    - booked meetings
    - partner-intent clicks
    - investor-material views
    - evidence requests

## Escalation

- Broken link or no-CTA issue: Product Lead + Web owner immediate fix.
- Tracking outage: Analytics owner to provide manual backup count using meeting calendar and evidence request IDs.
