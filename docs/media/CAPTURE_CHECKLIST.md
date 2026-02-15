# Screenshot + Video Capture Checklist

This checklist is meant to be *repeatable*. Capture media only after `docs/DEMO_READINESS_GATE.md` passes.

## Standard Capture Settings

- Browser: Chromium
- Desktop viewport: 1440x900
- Mobile viewport: 390x844 (optional)
- Use seeded demo data (no manual DB edits)
- Record in a clean session (fresh browser profile)

## Artifacts Location

- Screenshots: `docs/releases/<version>/media/screenshots/`
- Videos: `docs/releases/<version>/media/videos/`
- Supporting logs (if needed): `docs/releases/<version>/logs/`

## Local Gates Before Capture

```bash
./scripts/ci/local-ci.sh demo
```

## Capture Pass 1: Platform Overview (2-3 minutes)

1. Landing / entrypoint
2. Role selection (if present)
3. Core navigation (Patients, Care Gaps, Measures/Reports)

## Capture Pass 2: Differentiator Flows (5-10 minutes total)

Pick the minimum set that tells the story:

### A. Patient Context -> Action

1. Patients list (search/filter if available)
2. Patient details
3. Jump to care gaps for that patient

### B. Care Gaps -> Resolution Loop

1. Care gaps list (show prioritization)
2. Care gap details
3. Any resolution action / notes / status change (if present)

### C. Quality / Reporting

1. Quality measures overview
2. Run evaluation / view results (if present)
3. Export/report screen

### D. Audit + Export (Compliance Proof)

1. Audit log viewer
2. Event detail modal
3. Export CSV/JSON

## Automation: Agent Browser (Screenshots)

If the portal is running at the gateway and seeded:

```bash
PORTAL_URL=http://localhost:18080 \
SCREENSHOT_DIR=docs/releases/<version>/media/screenshots/agent-browser \
./scripts/agent-browser/nav-global.sh
```

Run the rest of the suites as needed:
- `./scripts/agent-browser/provider-care-gaps.sh`
- `./scripts/agent-browser/quality-reports.sh`

## Automation: Video (Recommended Approach)

Use Playwright video recording for deterministic clips:
- Create a small Playwright “storyboard” per flow
- Enable `video: 'on'` and fixed viewport in the config
- Export MP4s into `docs/releases/<version>/media/videos/`

Once we pick the exact “hero set”, we can implement the storyboard suite.

