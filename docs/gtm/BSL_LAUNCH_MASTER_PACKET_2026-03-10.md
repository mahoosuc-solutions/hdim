# BSL Launch Master Packet

Date: 2026-03-10  
Launch host (canonical for morning window): `https://hdim-himss.vercel.app`

## 1) Core Launch Narrative

HDIM is a source-available healthcare quality and interoperability platform delivered with AI-assisted implementation discipline, evidence-linked release governance, and commercial production rights under BSL-aligned terms.

One-line floor opener:
- "We map platform claims to proof in real time and move teams from FHIR data to operational outcomes fast."

## 2) BSL Plain-Language Script

Use this exact script:

1. "HDIM is source-available for evaluation and non-production development."
2. "Production and hosted use require commercial licensing rights."
3. "Licensing boundaries and conversion terms are public in our licensing transparency path."

Primary link:
- `https://hdim-himss.vercel.app/resources/licensing`

## 3) Persona Routing Matrix

| Persona | Primary URL | Secondary Proof URL | Call to Action |
|---|---|---|---|
| CIO/CISO | `https://hdim-himss.vercel.app/resources/cio-ciso` | `/resources/trust-center` | Book architecture/security review |
| Procurement / Partner | `https://hdim-himss.vercel.app/resources/procurement` | `/resources/evidence-room` | Book commercial scoping call |
| Clinical Leadership | `https://hdim-himss.vercel.app/resources/clinical` | `/resources/executive-summary` | Book workflow outcomes review |
| Mixed Executive | `https://hdim-himss.vercel.app/resources/himss-brief` | `/resources/trust-center` | Book cross-functional briefing |
| Investor / Strategic | `https://hdim-himss.vercel.app/resources/himss-brief` | `/resources/licensing` | Book investor confidence briefing |

## 4) Pricing + Social Allocation Script

Use this exact language:

1. "Implementation is priced from scoped story points with explicit ALF assumptions."
2. "Annual subscription is platform + integration + intelligence tiers."
3. "Every deal includes a hard-wired local impact allocation."
4. "Local impact allocation is tiered 1/2/3% and is not removed through standard discounting."

References:
- `docs/sales/03-sales-tools/pricing-guide.md`
- `docs/sales/03-sales-tools/ai-pricing-forecast-schema.md`
- `docs/gtm/AI_PRICING_LOCAL_IMPACT_POLICY.md`

## 5) QR and Print Asset Paths

HIMSS one-pager (front/back with QR):
- `docs/marketing/himss/print-deck-2026-03-10/09-himss-front-back-onepager.html`

Poster set:
- `docs/marketing/himss/print-deck-2026-03-10/01-hdim-overview-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/04-trust-center-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/05-cio-ciso-path-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/06-procurement-partner-poster.html`
- `docs/marketing/himss/print-deck-2026-03-10/07-licensing-transparency-poster.html`

QR reference pages:
- `docs/marketing/web/qr-links.html`
- `docs/marketing/web/qr-portal.html`
- `docs/marketing/web/assets/qr/README.md`

## 6) Launch Control Checklist (Go/No-Go)

- [x] HIMSS launch routes return HTTP 200 on `hdim-himss.vercel.app`
- [x] Evidence request path active
- [x] Book Meeting CTA UTM wiring active
- [x] Licensing transparency route live
- [x] Executive Summary screenshot corrected (clear image)
- [x] BSL operational validation report generated
- [x] Controlled-content scan gate implemented

Operational evidence:
- `docs/compliance/BSL_LAUNCH_OPERATIONAL_VALIDATION_2026-03-10.md`
- `validation-reports/bsl-launch-ops-summary.md`
- `validation-reports/bsl-launch-ops-summary.json`

## 7) Known Constraint (Not Blocking Morning Launch)

- Custom domain `www.healthdatainmotion.com/resources/*` is not the active morning canonical.
- Morning launch canonical remains `https://hdim-himss.vercel.app/resources/*`.

## 8) Conference Execution Sequence (Fast)

1. Qualify persona in first 20 seconds.
2. Route to correct URL from Section 3.
3. Deliver BSL plain-language script from Section 2.
4. Deliver pricing + local impact script from Section 4.
5. Capture next step and schedule.
6. Log persona + urgency + next step in CRM immediately.
