# Intelligence Rollout Runbook (v1.3.0)

## Purpose
Operational runbook for staged rollout of the Intelligence Engine with deployment gates, authorization checks, observability monitoring, and rollback controls.

## Prerequisites
- Deployment workflow with intelligence gates is active: `.github/workflows/deploy-docker.yml`
- Intelligence release-validation scripts are available:
  - `scripts/release-validation/validate-intelligence-gate-config.sh`
  - `scripts/release-validation/validate-intelligence-readiness.sh`
  - `scripts/release-validation/validate-intelligence-authz.sh`
- Secrets and variables are configured per:
  - `scripts/release-validation/INTELLIGENCE_GATE_CONFIG.md`
- Alert rules deployed:
  - `monitoring/rules-staging.yml`
  - `monitoring/rules-production.yml`

---

## Staging Week Plan

### Day 0: Configuration and Dry Run
1. Set staging secrets and vars listed in `INTELLIGENCE_GATE_CONFIG.md`.
2. Keep deep checks off initially:
   - `STAGING_INTELLIGENCE_CHECK_MIGRATIONS=false`
   - `STAGING_INTELLIGENCE_CHECK_KAFKA_TOPICS=false`
3. Trigger deployment workflow manually.
4. Verify artifact upload includes:
   - `intelligence-readiness-validation-report.md`
   - `intelligence-authz-validation-report.md`

Success criteria:
- Gate config preflight passes.
- Staging intelligence gate steps complete successfully.

### Day 1: Canary Tenant Enablement
1. Enable intelligence feature flags for one low-risk staging tenant only.
2. Keep all non-canary tenants disabled.
3. Re-run deployment workflow and validate reports.

Success criteria:
- Readiness/authz scripts pass.
- `403/404` behavior is correct for invalid roles/tenant mismatch.
- Canary tenant ingest/review/status behavior is healthy.

### Day 2: Observability and Alert Validation
1. Confirm metrics exist in Prometheus:
   - `intelligence_validation_findings_consumer_lag_seconds`
   - `intelligence_trust_projection_freshness_seconds`
   - `http_server_requests_seconds_count` (intelligence mutable endpoint status split)
2. Execute controlled tests:
   - generate unauthorized mutable requests (expect `403`)
   - generate cross-tenant path mismatch (expect `404`)
   - temporarily delay consumer processing (controlled test)
3. Verify staging alerts fire and recover.

Success criteria:
- Alerts trigger as expected and clear after remediation.
- No persistent false-positive critical alerts.

### Day 3: Deep Check Enablement
1. Enable staging deep checks:
   - `STAGING_INTELLIGENCE_CHECK_MIGRATIONS=true`
   - `STAGING_INTELLIGENCE_CHECK_KAFKA_TOPICS=true`
2. Ensure CI runner connectivity to staging DB/Kafka.
3. Re-run deployment gate.

Success criteria:
- Migrations/tables/topics validated successfully.
- No connectivity failures in gate steps.

### Day 4-5: Staging Cohort Expansion
1. Expand from 1 canary tenant to a small cohort.
2. Re-run gate each expansion.
3. Observe for 24-48h before production cutover readiness.

Success criteria:
- No sustained lag/staleness alerts.
- No unexpected authz spike behavior.

---

## Production Cutover Plan

### T-2h Pre-Cutover
1. Verify production secrets/vars are configured.
2. Confirm release tag and image manifest are ready.
3. Verify rollback script path is available and tested.

Go/No-Go criteria:
- Staging stable for >=24h.
- No unresolved intelligence incidents.

### T0 Deploy and Gate
1. Trigger production deployment workflow (tag-based).
2. Allow production intelligence gate steps to execute.
3. Verify gate artifacts uploaded.

Success criteria:
- Gate config validation passes.
- Readiness/authz validation passes.

### T+0 to T+2h Production Canary
1. Enable intelligence feature flags for one production tenant.
2. Monitor alerts and key metrics continuously.

Abort criteria:
- Sustained critical staleness or lag alert.
- Unexpected high authz 403/404 spike indicating rollout misconfiguration.

### Progressive Expansion
1. Expand tenant cohorts in fixed increments (example: 1 -> 3 -> 10 -> full).
2. Require stable observation window between increments.

Recommended hold windows:
- 2-4h between early cohorts.
- 24h before full rollout.

### T+24h Stabilization
1. Review all gate reports and alert history.
2. If stable, enable production deep checks:
   - `PRODUCTION_INTELLIGENCE_CHECK_MIGRATIONS=true`
   - `PRODUCTION_INTELLIGENCE_CHECK_KAFKA_TOPICS=true`
3. Publish final operational summary.

---

## Rollback Procedure
1. Disable all intelligence feature flags immediately.
2. Redeploy configuration.
3. Keep intelligence gate scripts active (optionally endpoint-only while triaging infra).
4. Collect and attach:
   - readiness report
   - authz report
   - alert timeline

Rollback success criteria:
- Alerts return to baseline.
- No tenant-level intelligence mutations continue after disable.

---

## Required Artifacts per Deployment
- `docs/releases/<version>/validation/intelligence-readiness-validation-report.md`
- `docs/releases/<version>/validation/intelligence-authz-validation-report.md`
- Deployment job logs for staging and production intelligence gate steps

