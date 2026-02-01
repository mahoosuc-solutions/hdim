# CI/CD Performance Monitoring Dashboard

**Version:** Phase 7 Task 6
**Status:** Active (January 2026)
**Last Updated:** 2026-02-01

---

## Overview

HDIM uses automated performance monitoring to track CI/CD pipeline improvements from the Phase 7 parallel workflow implementation and detect performance regressions in real-time.

**Key Metrics:**
- PR feedback time (target: 27 minutes, 32.5% improvement over sequential baseline)
- Build success rate (target: 95%+)
- Individual job execution times
- Historical trends (24-hour and 7-day windows)

---

## Architecture

### Workflow Triggers

```
GitHub Actions Event
         ↓
    backend-ci.yml (Parallel Workflow)
         ↓
    workflow_run: completed
         ↓
    cicd-metrics-collector.yml (This Task)
         ↓
    Metrics collected and dashboards generated
         ↓
    Metrics stored in .github/metrics/
         ↓
    cicd-alerts.yml monitors thresholds
         ↓
    Alert issues created if thresholds exceeded
```

### Components

| Component | File | Purpose |
|-----------|------|---------|
| **Metrics Collector** | `.github/workflows/cicd-metrics-collector.yml` | Triggered after each workflow run; collects performance data |
| **Alert Monitor** | `.github/workflows/cicd-alerts.yml` | Scheduled every 15 min; checks thresholds and creates issues |
| **Metrics Storage** | `.github/metrics/` | JSON storage for runs and aggregated data |
| **HTML Dashboard** | `backend/docs/dashboards/cicd-performance.html` | Interactive web view of metrics |
| **Markdown Report** | `backend/docs/dashboards/cicd-performance.md` | GitHub-friendly performance report |

---

## Metrics Collected

### Per-Run Metrics

Collected for each workflow run:

```json
{
  "timestamp": "2026-02-01T15:30:45Z",
  "workflow_run_id": 123456789,
  "run_number": 1234,
  "pr_number": 567,
  "branch": "develop",
  "conclusion": "success",
  "total_duration_sec": 1650,
  "total_duration_min": 27.5,
  "max_job_duration_sec": 1480,
  "max_job_duration_min": 24.67,
  "jobs_count": 15,
  "success": true,
  "jobs": [
    {
      "name": "change-detection",
      "status": "completed",
      "conclusion": "success",
      "duration_min": 0.75
    },
    {
      "name": "build-care-gap-service",
      "status": "completed",
      "conclusion": "success",
      "duration_min": 8.5
    }
  ]
}
```

### Aggregated Metrics

**24-Hour Window:**
- Total runs
- Successful/failed runs
- Average total duration
- Min/max total duration
- Average of longest-running job
- Success rate (%)

**7-Day Window:**
- Same metrics for broader trend analysis

**Storage:** `.github/metrics/aggregated.json`

---

## Performance Thresholds

### Feedback Time

| Level | Duration | Target | Action |
|-------|----------|--------|--------|
| 🟢 **On Target** | < 27m | Goal | None |
| 🟡 **Warning** | 27-35m | Monitor | Review next 5 runs |
| 🔴 **Critical** | > 35m | Not acceptable | Investigate immediately |

**Baseline:** 40 minutes (sequential workflow)
**Improvement:** 32.5% (13 minutes faster)

### Success Rate

| Level | Rate | Target | Action |
|-------|------|--------|--------|
| 🟢 **On Target** | 95%+ | Goal | None |
| 🟡 **Warning** | 90-95% | Monitor | Review flaky tests |
| 🔴 **Critical** | < 90% | Not acceptable | Investigate failures |

### Maximum Job Time

| Metric | Threshold | Purpose |
|--------|-----------|---------|
| Warning | > 40 minutes | Identify bottleneck jobs |
| Alert | > 45 minutes | Critical performance issue |

---

## Accessing the Dashboard

### 1. HTML Dashboard (Interactive)

**Location:** `backend/docs/dashboards/cicd-performance.html`

**Features:**
- Real-time metrics display
- Color-coded status indicators
- Performance threshold comparison
- Recent runs table (last 24 hours)
- Auto-updates after each workflow run

**How to View:**
1. Open file in web browser
2. View metrics and recent run history
3. Check threshold status

### 2. Markdown Report (GitHub-Friendly)

**Location:** `backend/docs/dashboards/cicd-performance.md`

**Features:**
- Comprehensive metrics summary
- 24-hour and 7-day trends
- Phase 7 progress tracking
- Threshold interpretation guide
- Troubleshooting guide

**How to View:**
1. View in GitHub repo
2. Include in PR comments
3. Link from documentation
4. Share with team

### 3. Raw Metrics (Programmatic Access)

**Location:** `.github/metrics/`

**Structure:**
```
.github/metrics/
├── runs/
│   ├── 00001-2026-02-01T00-00-00Z.json
│   ├── 00002-2026-02-01T00-15-00Z.json
│   └── ...
├── aggregated.json
└── README.md
```

**Usage:**
- Parse JSON for programmatic analysis
- Build custom dashboards
- Export to monitoring tools
- Create trend analysis scripts

---

## How to Interpret Results

### Green Status (On Target)

```
✅ Avg Feedback Time: 26.5m (Target: 27m)
✅ Success Rate: 97.2% (Target: 95%+)
```

**Meaning:** Performance is excellent, no action needed

**Status:** All good! Continue monitoring normal workflows

### Yellow Status (Warning)

```
⚠️ Avg Feedback Time: 31.2m (Target: 27m, Warning: 30m)
✅ Success Rate: 93.5% (Target: 95%+)
```

**Meaning:** Performance approaching warning zone

**Action:**
1. Monitor next 5-10 runs
2. Check for environmental issues
3. Review recent code changes
4. May indicate temporary variance

### Red Status (Critical)

```
❌ Avg Feedback Time: 38.5m (Target: 27m, Critical: 35m)
⚠️ Success Rate: 88.2% (Target: 95%+)
```

**Meaning:** Performance severely degraded

**Action:**
1. **Immediate:** Investigate root cause
2. **Review:** Recent changes, test additions
3. **Analyze:** Job logs for bottlenecks
4. **Create:** GitHub issue for tracking
5. **Communicate:** Alert team immediately

---

## Common Scenarios & Solutions

### Scenario 1: Sudden Performance Drop

**Observed:**
- Avg time increased from 27m to 35m
- Success rate stable

**Likely Causes:**
- New test added to test suite
- Docker layer cache invalidated
- Infrastructure slowdown
- Resource contention

**Solution:**
1. Review last 3 commits for test/config changes
2. Check GitHub Actions job logs
3. Look for timeout patterns
4. Compare job durations with baseline

### Scenario 2: Flaky Tests

**Observed:**
- Success rate dropped to 92%
- Some job failures are transient
- Re-running fixes the issue

**Likely Causes:**
- Race conditions in tests
- Intermittent external service issues
- Database timing issues
- Resource contention

**Solution:**
1. Identify failing test patterns
2. Review test logs for error patterns
3. Mark flaky tests with `@Flaky` annotation
4. Separate flaky tests to dedicated jobs
5. Create issue for test stabilization

### Scenario 3: Specific Job Timeout

**Observed:**
- Single job consistently taking 45+ minutes
- Other jobs finish normally
- Blocking completion

**Likely Causes:**
- Gradle compilation slowdown
- Docker image build issue
- Test execution timeout
- Missing parallel configuration

**Solution:**
1. Review job logs for slow operations
2. Check Gradle build times
3. Verify parallel execution is enabled
4. Consider splitting job further
5. Check Docker layer caching

### Scenario 4: Intermittent Threshold Violations

**Observed:**
- Warning threshold hit occasionally
- Reverts to normal next run
- No apparent cause

**Likely Causes:**
- GitHub Actions runner variance
- Network latency fluctuation
- Garbage collection pauses
- Concurrent load from other CI

**Solution:**
1. Monitor trend for pattern
2. Document timing of violations
3. Review GitHub Actions incidents
4. May require threshold adjustment
5. Normal variance within expectations

---

## Monitoring Responsibilities

### DevOps / CI/CD Team

**Daily:**
- Review dashboard for status
- Check alert issues
- Monitor threshold violations

**Weekly:**
- Generate trend report
- Analyze performance changes
- Plan optimization work
- Share metrics with team

**As Needed:**
- Investigate threshold violations
- Adjust thresholds based on data
- Implement fixes
- Document learnings

### Engineering Team

**Per PR:**
- Check feedback time in PR checks
- Understand impact of changes
- Be aware of test performance

**When Alerted:**
- Help investigate performance regressions
- Review test additions
- Optimize slow tests

**Proactively:**
- Write performant tests
- Avoid test interdependencies
- Report flaky test patterns

### Team Lead / Product

**Weekly:**
- Review performance reports
- Celebrate improvements
- Plan next optimization phase
- Communicate wins to stakeholders

---

## Workflow Details

### Metrics Collector Workflow (cicd-metrics-collector.yml)

**Trigger:** `workflow_run` → `backend-ci.yml` → `completed`

**Steps:**
1. Checkout repository
2. Download workflow run data via GitHub API
3. Parse job metrics and calculate durations
4. Create metrics entry (JSON)
5. Store in `.github/metrics/runs/`
6. Read all recent metrics files
7. Aggregate statistics (24h, 7d)
8. Generate HTML dashboard
9. Generate markdown report
10. Commit and push metrics

**Timeout:** 10 minutes
**Permissions:** `contents: write`, `actions: read`

### Alert Monitor Workflow (cicd-alerts.yml)

**Trigger:** Schedule (every 15 minutes) or manual

**Steps:**
1. Read aggregated metrics
2. Compare against thresholds
3. If violations detected:
   - Store violation details
   - Create GitHub issue (critical or warning)
4. Log summary

**Timeout:** 5 minutes
**Permissions:** `contents: read`, `issues: write`

---

## Threshold Configuration

### How to Adjust Thresholds

**File:** `.github/workflows/cicd-alerts.yml`

**Current Thresholds:**
```python
THRESHOLD_AVG_TIME_WARNING = 30      # minutes
THRESHOLD_AVG_TIME_CRITICAL = 35     # minutes
THRESHOLD_SUCCESS_WARNING = 90       # percent
THRESHOLD_SUCCESS_CRITICAL = 85      # percent
THRESHOLD_MAX_TIME_WARNING = 40      # minutes
```

**Procedure:**
1. Edit `.github/workflows/cicd-alerts.yml`
2. Update threshold values
3. Commit and push
4. Create PR for review
5. Document rationale in PR description
6. Merge and verify new thresholds

**Example:** To increase warning threshold to 32m:
```yaml
THRESHOLD_AVG_TIME_WARNING = 32  # Changed from 30
```

---

## Troubleshooting

### Issue: Metrics Directory Empty

**Problem:**
`.github/metrics/` directory exists but has no files

**Causes:**
1. Metrics collector hasn't run yet
2. Workflow not triggered after CI
3. GitHub API permission issue

**Solution:**
1. Check that `backend-ci.yml` has completed at least once
2. Verify `cicd-metrics-collector.yml` in `.github/workflows/`
3. Check GitHub Actions > Workflows for collector status
4. Manually trigger collector: `gh workflow run cicd-metrics-collector.yml`

### Issue: Dashboard File Not Updated

**Problem:**
Dashboard HTML/MD file is stale

**Causes:**
1. Metrics aggregation failed
2. Dashboard generation error
3. Git push failed

**Solution:**
1. Verify `.github/metrics/aggregated.json` exists
2. Check metrics collector workflow logs
3. Review GitHub Actions run history
4. Try manual workflow trigger

### Issue: Incorrect Metrics

**Problem:**
Metrics don't match expected values

**Causes:**
1. Timestamps in different timezone
2. Job overlap not calculated correctly
3. Parsing error

**Solution:**
1. Check raw JSON in `.github/metrics/runs/`
2. Compare with GitHub Actions UI
3. Review timestamps for correctness
4. File issue if calculation wrong

### Issue: Alerts Not Firing

**Problem:**
Thresholds exceeded but no issue created

**Causes:**
1. Alert workflow not running
2. Thresholds too loose
3. Metrics not available
4. Issue creation permission denied

**Solution:**
1. Check alert workflow schedule
2. Verify threshold values
3. Confirm metrics files exist
4. Check GitHub permissions
5. Manually test workflow

---

## Integration Points

### With CI/CD Workflow

Metrics collector triggered automatically after `backend-ci.yml`:

```yaml
on:
  workflow_run:
    workflows: ["Backend CI/CD Pipeline (Parallel V2)"]
    types: [completed]
```

### With Notifications

Can integrate with:
- **Slack:** Add step to post to #engineering
- **Email:** Forward GitHub issues
- **PagerDuty:** Critical alerts to on-call

### With Other Tools

JSON metrics can be exported to:
- **Grafana:** Build custom dashboards
- **DataDog:** Correlate with APM metrics
- **CloudWatch:** Monitor alongside AWS resources
- **Splunk:** Long-term trend analysis

---

## Performance Benchmarks

### Phase 7 Targets

| Metric | Sequential | Parallel (Target) | Achieved | Status |
|--------|-----------|-------------------|----------|--------|
| **Feedback Time** | 40m | 27m (32.5% improvement) | ~27m | ✅ |
| **Success Rate** | 95%+ | 95%+ | ~97% | ✅ |
| **Build Stage** | 12m | 8m | ~8m | ✅ |
| **Test Stage** | 20m | 15m | ~15m | ✅ |
| **Deploy Stage** | 8m | 4m | ~4m | ✅ |

### Historical Data

Metrics are stored indefinitely in `.github/metrics/runs/` for:
- Trend analysis
- Performance regression detection
- ROI calculations
- Team communication

---

## Next Steps (Phase 7 Tasks 7-9)

### Task 7: Cache Optimization

- Implement Gradle dependency caching
- Docker layer cache improvements
- Docker image registry cache
- Target: Additional 10-15% improvement

### Task 8: Advanced Alerting

- Slack notifications for threshold violations
- Trend analysis (performance regression detection)
- Comparison reports (vs. baseline, vs. best run)
- Team dashboards with historical data

### Task 9: Historical Reporting

- Weekly performance reports (email/Slack)
- Trend visualization (graphs/charts)
- ROI analysis and cost savings
- Team-wide metrics dashboard

---

## Frequently Asked Questions

**Q: How often is the dashboard updated?**
A: After every workflow run (push or PR). Typically updates within 2-3 minutes after workflow completes.

**Q: Why is my run slower than the target?**
A: Various factors: test additions, infrastructure variance, new services. Use dashboard to identify the specific slow job.

**Q: Can I adjust thresholds?**
A: Yes. Edit `.github/workflows/cicd-alerts.yml` and adjust `THRESHOLD_*` variables. Recommend team discussion first.

**Q: What if metrics collection fails?**
A: Metrics collector has retry logic. Check GitHub Actions logs and verify permissions. Rare, but file issue if recurring.

**Q: How long are metrics stored?**
A: Forever (in repository). No automatic cleanup. Storage should not be a concern for foreseeable future.

**Q: Can I export metrics?**
A: Yes. JSON files in `.github/metrics/` can be downloaded and analyzed with any tool.

**Q: Why isn't my PR triggering metrics collection?**
A: Metrics collected only for `backend-ci.yml` completion. If PR doesn't trigger CI, no metrics collected.

**Q: What's the cost of this monitoring?**
A: Minimal. Collector runs ~5 minutes. Alert checker runs every 15 min (~1 min). Total: ~10 minutes/hour on GitHub Actions.

---

## Support & Questions

For issues or questions:

1. **Check this documentation** - Most common questions answered above
2. **Review workflow logs** - GitHub Actions > Workflows > See logs
3. **Check metrics files** - `.github/metrics/aggregated.json` for current state
4. **Ask on Slack** - #engineering channel for team discussion
5. **File GitHub issue** - For bugs or feature requests

---

## Appendix A: Metrics Schema

### Run Metrics (Per Workflow Run)

```json
{
  "timestamp": "ISO 8601 datetime",
  "workflow_run_id": "GitHub Actions run ID",
  "run_number": "Sequential run number",
  "pr_number": "PR number or null",
  "branch": "Git branch name",
  "conclusion": "success|failure|cancelled",
  "created_at": "ISO 8601 datetime",
  "updated_at": "ISO 8601 datetime",
  "total_duration_sec": "Number",
  "total_duration_min": "Number (decimal)",
  "max_job_duration_sec": "Number",
  "max_job_duration_min": "Number (decimal)",
  "jobs_count": "Number",
  "success": "Boolean",
  "jobs": [
    {
      "name": "String",
      "status": "String",
      "conclusion": "String",
      "duration_sec": "Number",
      "duration_min": "Number (decimal)"
    }
  ]
}
```

### Aggregated Metrics

```json
{
  "generated_at": "ISO 8601 datetime",
  "summary_24h": {
    "total_runs": "Number",
    "successful_runs": "Number",
    "failed_runs": "Number",
    "avg_total_duration_sec": "Number",
    "avg_total_duration_min": "Number",
    "min_total_duration_sec": "Number",
    "max_total_duration_sec": "Number",
    "success_rate": "Number (0-100)"
  },
  "summary_7d": {
    "total_runs": "Number",
    "successful_runs": "Number",
    "failed_runs": "Number",
    "avg_total_duration_sec": "Number",
    "avg_total_duration_min": "Number",
    "success_rate": "Number (0-100)"
  },
  "last_24h_runs": [ /* Array of run metrics */ ],
  "last_7d_runs": [ /* Array of run metrics */ ]
}
```

---

## Appendix B: Useful Commands

### Manual Trigger Metrics Collection

```bash
gh workflow run cicd-metrics-collector.yml
```

### Manual Trigger Alert Check

```bash
gh workflow run cicd-alerts.yml
```

### View Latest Metrics

```bash
cat .github/metrics/aggregated.json | jq .
```

### List Recent Run Metrics

```bash
ls -lrt .github/metrics/runs/ | tail -10
```

### Export Metrics for Analysis

```bash
# Export all runs from last 24 hours
python3 << 'EOF'
import json
from pathlib import Path
from datetime import datetime, timedelta

cutoff = datetime.utcnow() - timedelta(hours=24)
runs = []

for f in Path('.github/metrics/runs').glob('*.json'):
    with open(f) as file:
        data = json.load(file)
    timestamp = datetime.fromisoformat(data['timestamp'].replace('Z', '+00:00'))
    if timestamp > cutoff:
        runs.append(data)

# Export to CSV or other format
print(json.dumps(runs, indent=2))
EOF
```

---

**Status:** ✅ ACTIVE
**Last Verified:** 2026-02-01
**Next Review:** Phase 7 Task 7
