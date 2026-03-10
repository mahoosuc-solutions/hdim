# HIMSS 2026 Print Deck (8.5x11 HQ)

This folder contains laminate-ready conference poster pages.

## Files

1. `01-hdim-overview-poster.html`
2. `02-traditional-vs-ai-timeline-poster.html`
3. `03-care-gap-impact-poster.html`
4. `04-trust-center-poster.html`
5. `05-cio-ciso-path-poster.html`
6. `06-procurement-partner-poster.html`
7. `07-licensing-transparency-poster.html`
8. `08-announcement-ops-poster.html`
9. `09-himss-front-back-onepager.html` (2-page duplex handout with QR code)

Shared style:
- `poster-theme.css`

## Print settings (important)

- Paper size: **US Letter (8.5 x 11 in)**
- Orientation: **Portrait**
- Scale: **100%** (no fit-to-page shrink)
- Margins: **None** (files already have internal safe padding)
- Background graphics: **Enabled**
- Quality: **Best / High**

## Laminating guidance

- Print on 28-32 lb matte stock for readability under conference lighting.
- Use 5 mil laminating pouches for rigidity on easels.
- Keep one unlaminated backup set for emergency reprints.

## Quick local preview

```bash
cd docs/marketing/himss/print-deck-2026-03-10
python3 -m http.server 4182
```

Then open:
- `http://127.0.0.1:4182/01-hdim-overview-poster.html`
- `http://127.0.0.1:4182/09-himss-front-back-onepager.html`

## Duplex print settings for one-pager

For `09-himss-front-back-onepager.html`:
- Two-sided print: **On**
- Flip on: **Long edge**
- Background graphics: **Enabled**
- Scale: **100%**

## Source notes

Quant metrics and messaging are derived from current HIMSS and marketing artifacts in:
- `docs/marketing/traditional-vs-ai-solutioning-comparison.md`
- `docs/marketing/ai-solutioning-metrics.md`
- `docs/marketing/himss/`
