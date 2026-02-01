# CI/CD Performance Metrics

**Phase 7 Task 6: Performance Monitoring Dashboard**

This directory stores performance metrics for the HDIM CI/CD pipeline, collected automatically after each workflow run.

---

## Directory Structure

```
.github/metrics/
├── runs/
│   ├── 00001-2026-02-01T00-00-00Z.json
│   ├── 00002-2026-02-01T00-15-00Z.json
│   └── ...
├── aggregated.json
├── README.md (this file)
└── .gitkeep
```

---

## Files

### `runs/` Directory

Individual workflow run metrics collected by `cicd-metrics-collector.yml`

**Naming:** `NNNNN-YYYY-MM-DDTHH-MM-SSZ.json`
- `NNNNN` = 5-digit zero-padded run number
- Rest = ISO timestamp of workflow completion

**Example:** `00001234-2026-02-01T15-30-45Z.json`
- Run #1234 completed on Feb 1, 2026 at 15:30:45 UTC

**Content:**
```json
{
  "timestamp": "ISO 8601 datetime",
  "workflow_run_id": "GitHub run ID",
  "run_number": 1234,
  "pr_number": 567,
  "branch": "develop",
  "conclusion": "success|failure|cancelled",
  "total_duration_min": 27.5,
  "max_job_duration_min": 24.67,
  "jobs_count": 15,
  "success": true,
  "jobs": [
    {
      "name": "job-name",
      "conclusion": "success",
      "duration_min": 0.75
    }
  ]
}
```

**Retention:** Forever (no automatic cleanup)
**Count:** ~1-2 per hour = 24-48 per day = 700-1,400 per month

### `aggregated.json`

Summary statistics for 24-hour and 7-day windows, regenerated after each workflow run.

**Updated by:** `cicd-metrics-collector.yml`
**Frequency:** After each workflow run (typically every 30-60 minutes)

**Content:**
```json
{
  "generated_at": "ISO 8601 datetime",
  "summary_24h": {
    "total_runs": 48,
    "successful_runs": 47,
    "failed_runs": 1,
    "avg_total_duration_min": 27.2,
    "min_total_duration_min": 24.8,
    "max_total_duration_min": 31.5,
    "success_rate": 97.9
  },
  "summary_7d": {
    "total_runs": 336,
    "successful_runs": 326,
    "failed_runs": 10,
    "avg_total_duration_min": 27.5,
    "success_rate": 97.0
  },
  "last_24h_runs": [ /* Array of most recent 20 run objects */ ],
  "last_7d_runs": [ /* Array of most recent 50 run objects */ ]
}
```

---

## Accessing Metrics

### View Current Status

```bash
# See latest 24-hour statistics
cat .github/metrics/aggregated.json | jq '.summary_24h'

# See latest 7-day statistics
cat .github/metrics/aggregated.json | jq '.summary_7d'

# See most recent run
cat .github/metrics/aggregated.json | jq '.last_24h_runs[0]'
```

### Analyze Trends

```bash
# List all runs from last 7 days
cat .github/metrics/aggregated.json | jq '.last_7d_runs[] | {run_number, branch, total_duration_min, success}'

# Find slowest run
cat .github/metrics/aggregated.json | jq '.last_24h_runs | max_by(.total_duration_min) | {run_number, total_duration_min}'

# Find slowest job across all recent runs
cat .github/metrics/aggregated.json | jq '.last_24h_runs[] | .jobs | max_by(.duration_min)'
```

### Export Metrics

```bash
# Export 24-hour metrics as CSV (requires jq and csvkit)
cat .github/metrics/aggregated.json | jq -r '.last_24h_runs[] | [.run_number, .branch, .total_duration_min, .success] | @csv' > metrics.csv

# Or export as JSON for external tools
cat .github/metrics/aggregated.json > export.json
```

---

## Dashboards

Metrics are visualized in two places:

### 1. HTML Dashboard

**Location:** `backend/docs/dashboards/cicd-performance.html`

Auto-generated after each workflow run with:
- Current performance metrics
- Recent runs table
- Threshold comparisons
- Status indicators

**View:** Open in web browser

### 2. Markdown Report

**Location:** `backend/docs/dashboards/cicd-performance.md`

Auto-generated with:
- Summary statistics
- 24h and 7d trends
- Threshold interpretation
- Troubleshooting guide

**View:** In GitHub or markdown viewer

---

## Performance Targets

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| Avg Feedback Time | 27m | 30m | 35m |
| Success Rate | 95%+ | 90% | 85% |
| Max Job Time | - | 40m | 45m |

---

## Workflow Automation

### Metrics Collector

**File:** `.github/workflows/cicd-metrics-collector.yml`

Triggered automatically when `backend-ci.yml` completes:

1. Collects workflow run data via GitHub API
2. Parses job metrics and calculates durations
3. Stores run metrics in `runs/` directory
4. Aggregates 24h and 7d statistics
5. Generates HTML and Markdown dashboards
6. Commits and pushes metrics to repository

### Alert Monitor

**File:** `.github/workflows/cicd-alerts.yml`

Runs every 15 minutes:

1. Reads `aggregated.json`
2. Compares metrics against thresholds
3. Creates GitHub issues if thresholds exceeded
4. Logs summary to GitHub Actions output

---

## Troubleshooting

### Metrics Not Collecting

**Problem:** `runs/` directory is empty

**Causes:**
1. No workflow runs completed yet
2. Metrics collector workflow not running
3. GitHub API permissions issue

**Solution:**
1. Ensure `backend-ci.yml` has run at least once
2. Verify `cicd-metrics-collector.yml` exists in `.github/workflows/`
3. Check GitHub Actions logs for errors
4. Manually trigger: `gh workflow run cicd-metrics-collector.yml`

### Aggregated.json Not Updating

**Problem:** File is stale (old timestamp)

**Causes:**
1. Metrics collector failed silently
2. No workflow runs since last update
3. Aggregation logic error

**Solution:**
1. Check metrics collector workflow logs
2. Verify `runs/` directory has new files
3. Manually run metrics collector
4. File issue if bug suspected

### Incorrect Metrics

**Problem:** Numbers don't match GitHub Actions UI

**Causes:**
1. Timezone mismatch
2. Duration calculation error
3. Parsing issue

**Solution:**
1. Compare with GitHub Actions UI
2. Check raw JSON timestamps
3. Verify calculation logic
4. File issue if reproducible

---

## Manual Operations

### View Workflow Runs

```bash
# List last 10 runs
gh run list --limit 10

# Get specific run details
gh run view <RUN_ID>

# View run logs
gh run view <RUN_ID> --log

# Download run artifacts
gh run download <RUN_ID>
```

### Manually Trigger Collectors

```bash
# Collect metrics
gh workflow run cicd-metrics-collector.yml

# Check thresholds
gh workflow run cicd-alerts.yml

# View all workflows
gh workflow list
```

---

## Data Management

### Storage Considerations

- **Growth rate:** ~1,440 files/month (1 per hour)
- **File size:** ~2-5 KB per run
- **Total:** ~3-7 MB/month
- **Retention:** Indefinite (git LFS optional for very old data)

### Backup & Recovery

Metrics are version controlled in git:
- All historical metrics preserved in repository
- Backed up with regular repository backups
- Recoverable from git history if deleted

### Privacy & Compliance

- ✅ No PHI in metrics (only timing and status)
- ✅ No sensitive data (just job names and durations)
- ✅ HIPAA compliant (no patient information)
- ✅ Safe to commit to repository

---

## Integration Possibilities

Metrics JSON can be integrated with:

- **Grafana:** Build custom dashboards
- **DataDog:** Correlate with application metrics
- **Splunk:** Long-term trend analysis
- **CloudWatch:** Monitor alongside AWS resources
- **Slack:** Post summaries to #engineering
- **Email:** Send weekly reports

---

## Documentation References

| Document | Purpose |
|----------|---------|
| **[CI_CD_PERFORMANCE_DASHBOARD.md](../CI_CD_PERFORMANCE_DASHBOARD.md)** | Technical dashboard guide |
| **[CI_CD_MONITORING_GUIDE.md](../CI_CD_MONITORING_GUIDE.md)** | Team usage and escalation guide |
| **[CLAUDE.md](../../CLAUDE.md)** | Phase 7 overview |
| **Dashboard HTML** | `backend/docs/dashboards/cicd-performance.html` |
| **Dashboard Markdown** | `backend/docs/dashboards/cicd-performance.md` |

---

## Support

For questions or issues:

1. Check this README
2. Review GitHub Actions workflow logs
3. Check metrics files for raw data
4. Post in #engineering Slack channel
5. File GitHub issue if bug suspected

---

**Created:** Phase 7 Task 6 (January 2026)
**Status:** ✅ ACTIVE
**Last Updated:** 2026-02-01
