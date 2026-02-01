# Agent-Browser UI Suites

## Prereqs
- `npm install -g agent-browser`
- `agent-browser install`
- Portal running (default: `http://localhost:18080`)
- Demo data seeded: `./scripts/seed-all-demo-data.sh`

## Environment
- `PORTAL_URL` (default: `http://localhost:18080`)
- `SCREENSHOT_DIR` (default: `/tmp/agent-browser-smoke`)
- `AB_SLEEP_SECS` (default: `2`)

## Run All Suites
```
PORTAL_URL=http://localhost:18080 \
SCREENSHOT_DIR=/tmp/agent-browser-smoke \
./scripts/agent-browser/nav-global.sh
```

## Suites
- `nav-global.sh` — core navigation
- `dashboard-admin.sh` — admin dashboard overview
- `reports-compliance.sh` — compliance report flow
- `provider-care-gaps.sh` — provider care gap workflow
- `provider-results.sh` — provider results review
- `provider-measures.sh` — provider measure performance
- `rn-care-gaps.sh` — RN care gap triage
- `rn-followups.sh` — RN follow-up coordination
- `ma-prep.sh` — MA patient prep
- `ma-reminders.sh` — MA reminders
- `quality-trends.sh` — quality trends
- `quality-reports.sh` — quality report/export

## Selector Notes
- Role switching uses the dashboard role toggle with aria-labels like:
  - "View dashboard as Provider"
  - "View dashboard as Registered Nurse"
  - "View dashboard as Medical Assistant"
- If a flow is unstable, add `data-testid` to the target element and update the script.
