# Phase 4 Troubleshooting Guide

## Common Problems

- Missing metrics in dashboards
- High alert volume / noisy alerts
- Slow dashboard queries
- Trace correlation gaps

## First Checks

1. Verify scrape targets and health endpoints.
2. Confirm Alertmanager routing and silencing rules.
3. Check dashboard datasource connectivity.
4. Validate tenant context and access filters.

## Escalation

Use operational runbooks in `docs/runbooks/` for incident-specific workflows.
