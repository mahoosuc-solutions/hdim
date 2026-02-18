# Load Test GitHub Secrets Setup

**Audience:** DevOps / operator configuring CI/CD for nightly load tests
**Required before:** Nightly `load-tests.yml` workflow runs against staging

---

## Overview

The `load-tests.yml` workflow runs daily at 06:00 UTC against a staging environment.
Without these secrets configured, the nightly job will target `http://localhost:*` (which
fails in GitHub Actions) and Slack breach notifications will be silently dropped.

---

## Required Secrets

Navigate to **GitHub → Repository → Settings → Secrets and variables → Actions → New repository secret** and add each of the following:

| Secret Name | Description | Example Value |
|---|---|---|
| `BASE_URL_PATIENT` | Base URL of the patient-service on staging | `https://staging.hdim.io:8084` |
| `BASE_URL_CARE_GAP` | Base URL of the care-gap-service on staging | `https://staging.hdim.io:8086` |
| `BASE_URL_QUALITY` | Base URL of the quality-measure-service on staging | `https://staging.hdim.io:8087` |
| `LOAD_TEST_AUTH_TOKEN` | Valid JWT for staging (used as Bearer token in k6 scripts) | `eyJhbGci...` |
| `LOAD_TEST_TENANT_ID` | Tenant ID seeded on staging (defaults to `test-tenant-perf`) | `staging-tenant-1` |
| `SLACK_WEBHOOK_URL` | Slack incoming webhook for SLO breach notifications | `https://hooks.slack.com/services/T.../B.../xxx` |

> **Note:** `LOAD_TEST_TENANT_ID` and `SLACK_WEBHOOK_URL` already have fallback defaults
> in the workflow. The three `BASE_URL_*` secrets and `LOAD_TEST_AUTH_TOKEN` are
> **required** for nightly runs to succeed.

---

## Step-by-Step

### 1. Get a staging JWT

Generate a JWT scoped to the staging environment using your identity provider or a
service account:

```bash
# Example: exchange credentials for a token via the gateway
curl -s -X POST https://staging.hdim.io:8001/auth/token \
  -H "Content-Type: application/json" \
  -d '{"clientId":"load-test-client","clientSecret":"<secret>","tenantId":"staging-tenant-1"}' \
  | jq -r .access_token
```

Copy the resulting token — it goes into `LOAD_TEST_AUTH_TOKEN`.

### 2. Add secrets via GitHub CLI (faster than UI)

```bash
# From the repository root (requires gh CLI authenticated)
gh secret set BASE_URL_PATIENT      --body "https://staging.hdim.io:8084"
gh secret set BASE_URL_CARE_GAP     --body "https://staging.hdim.io:8086"
gh secret set BASE_URL_QUALITY      --body "https://staging.hdim.io:8087"
gh secret set LOAD_TEST_AUTH_TOKEN  --body "<jwt-from-step-1>"
gh secret set LOAD_TEST_TENANT_ID   --body "staging-tenant-1"
gh secret set SLACK_WEBHOOK_URL     --body "https://hooks.slack.com/services/..."
```

### 3. Verify secrets are visible

```bash
gh secret list
```

All 6 secrets should appear (values are redacted).

---

## Triggering a Manual Test Run

After adding secrets, validate the configuration with a manual smoke run before waiting
for the next scheduled execution:

```bash
gh workflow run load-tests.yml \
  -f test_type=smoke \
  -f scenario=patient \
  -f base_url_override=""
```

Or use the GitHub Actions UI:
1. Go to **Actions → Load Tests — SLO Validation → Run workflow**
2. Select `smoke` type and `patient` scenario
3. Leave base URL override blank (secrets are used automatically)

---

## What Happens Without These Secrets

| Missing Secret | Impact |
|---|---|
| `BASE_URL_*` | Nightly job targets `localhost`, all k6 requests fail immediately |
| `LOAD_TEST_AUTH_TOKEN` | k6 runs with `test-token` (rejected by staging auth), all requests → 401 |
| `LOAD_TEST_TENANT_ID` | k6 uses `test-tenant-perf` (may not exist on staging, data returns empty) |
| `SLACK_WEBHOOK_URL` | SLO breach notifications silently dropped — on-call not alerted |

---

## Related Files

| File | Purpose |
|------|---------|
| `.github/workflows/load-tests.yml` | Workflow definition — reads these secrets |
| `load-tests/run-load-tests.sh` | k6 orchestration script |
| `load-tests/config/auth.js` | k6 auth config (reads `AUTH_TOKEN` env var) |
| `load-tests/scenarios/patient-service.js` | Patient service load scenario |
| `load-tests/scenarios/care-gap-service.js` | Care gap service load scenario |
| `load-tests/scenarios/quality-measure-service.js` | Quality measure load scenario |

---

## Orphaned Legacy Tests (Cleanup Pending)

`backend/performance-tests/k6/` contains 7 k6 scripts from January 2026 that are **not**
wired into `load-tests.yml`. They are superseded by the `load-tests/` directory.

**Action required (post-pilot):** Delete or archive `backend/performance-tests/k6/` to
avoid confusion. These files are not referenced by any CI workflow.

```bash
# After pilot launch, run:
git rm -r backend/performance-tests/k6/
git commit -m "chore: remove orphaned legacy k6 tests (superseded by load-tests/)"
```

---

_Last updated: February 2026_
