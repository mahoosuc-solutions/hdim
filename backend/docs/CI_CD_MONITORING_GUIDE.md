# CI/CD Performance Monitoring Guide

**For:** HDIM Engineering Team
**Version:** Phase 7 Task 6
**Status:** Active (January 2026)

---

## Quick Start (5 minutes)

### For Developers

**Every PR:**
1. After PR is created, wait for CI to run
2. Check for feedback time in GitHub Actions
3. Target: < 27 minutes from commit to feedback
4. If slow: Check if your changes added tests

**When Alerted:**
1. Look for GitHub issue with performance alert
2. Review dashboard: `backend/docs/dashboards/cicd-performance.html`
3. Check workflow logs for bottleneck
4. Help investigate if asked

### For DevOps/CI/CD Team

**Daily:**
- Review dashboard for status (5 min)
- Check for alert issues (2 min)
- Investigate if critical threshold hit (varies)

**Weekly:**
- Review trend report (10 min)
- Plan optimization work if needed (15 min)
- Share metrics with team (5 min)

**When Threshold Hit:**
1. Create Slack thread in #engineering
2. Diagnose root cause
3. Assign to relevant team
4. Track resolution
5. Update threshold if needed

---

## Understanding the Metrics

### Primary Metrics

| Metric | What It Measures | Why It Matters | Target |
|--------|------------------|-----------------|--------|
| **Feedback Time** | Time from commit to CI completion | Faster feedback = faster iteration | 27m |
| **Success Rate** | % of successful runs | Flaky tests slow down team | 95%+ |
| **Max Job Time** | Longest-running job | Identifies bottlenecks | <25m |

### Secondary Metrics

| Metric | What It Measures | Purpose |
|--------|------------------|---------|
| **Total Runs** | Number of CI executions | Activity level |
| **Min Time** | Fastest execution | Best case scenario |
| **Avg Max Job** | Average of slowest job | Parallelization efficiency |

---

## The Dashboard

### HTML Dashboard (cicd-performance.html)

**Location:** `backend/docs/dashboards/cicd-performance.html`

**View it:**
1. Open in web browser
2. Or: GitHub repo → `backend/docs/dashboards/`

**What to look for:**
- **Feedback Time metric:** Is it green (< 27m)? ✅
- **Success Rate metric:** Is it above 95%? ✅
- **Recent Runs table:** Any failed runs? Check logs
- **Threshold table:** Current vs target comparison

**Actions based on status:**

| Status | What To Do |
|--------|-----------|
| 🟢 All green | Continue monitoring, business as usual |
| 🟡 Yellow | Monitor next 5-10 runs, investigate if persists |
| 🔴 Red | Stop and investigate immediately |

### Markdown Report (cicd-performance.md)

**Location:** `backend/docs/dashboards/cicd-performance.md`

**Use for:**
- Team meetings and reports
- Sharing with management
- PR comments explaining performance impact
- Archiving performance history

---

## When to Take Action

### Red Alert (Critical)

**Triggers:**
- Feedback time > 35 minutes
- Success rate < 85%

**Immediate Actions:**
1. Create Slack thread: "Performance alert: [metric] exceeded"
2. Investigate workflow logs
3. Identify root cause (new test? infrastructure? docker issue?)
4. Assign to responsible team
5. Set expectation: "We'll resolve within 2 hours"

**Example:**
```
🚨 ALERT: Feedback time jumped to 38min

Root Cause: PR #567 added 50 tests to care-gap-service
Improvement: Can we split tests across jobs?

Assigned to: @care-gap-team
ETA: 2 hours
```

### Yellow Alert (Warning)

**Triggers:**
- Feedback time 30-35 minutes
- Success rate 85-95%

**Actions:**
1. Post in Slack: "Performance trending toward warning"
2. Watch next 5-10 runs
3. If persists, investigate like red alert
4. Likely temporary variance, no action if recovers

**Example:**
```
⚠️ WARNING: Feedback time avg 31min (up from 27m)

Status: Monitoring (no action yet)
If persists > 5 runs, will investigate
Current hypothesis: Runner variance
```

### Green (On Target)

**Triggers:**
- Feedback time < 30 minutes
- Success rate > 95%

**Actions:**
1. Continue normal operations
2. Monitor dashboard weekly
3. Share wins with team
4. Plan next optimization

---

## Investigating Performance Issues

### Issue: Slow Feedback Time

**Step 1: Identify which job is slow**

```bash
# Check dashboard
# → Recent Runs table
# → Max Job column shows slowest job

# Or check raw metrics
cat .github/metrics/aggregated.json | jq '.last_24h_runs[0].jobs | sort_by(.duration_min) | .[-1]'
```

**Step 2: Get detailed logs**

```bash
# Go to GitHub Actions
# → Click on workflow run
# → Click slow job
# → Review logs
# → Look for timeouts, slow operations, errors
```

**Step 3: Identify root cause**

| Symptom | Likely Cause | Solution |
|---------|--------------|----------|
| Gradle takes 8m | No build cache | Enable Gradle cache layer |
| Tests take 15m | Too many tests in job | Split across multiple jobs |
| Docker build 10m | Layer cache miss | Optimize Dockerfile order |
| Scan takes 5m | New scanning tool | Parallelize scanning |

**Step 4: Propose fix**

```bash
# Example: Gradle cache improvement
# → Modify .github/workflows/backend-ci.yml
# → Add gradle cache step
# → Test on feature branch
# → Merge and monitor improvement
```

### Issue: Flaky Tests (Low Success Rate)

**Step 1: Identify failing test**

```bash
# Check dashboard failure pattern
# → Do specific jobs fail repeatedly?
# → Is it same test or different tests?

# Check logs
# → "FAILED: TestClassName.testMethodName"
```

**Step 2: Categorize flakiness**

```
A) Consistent failure (always fails)
   → Regression in test or code, not flaky

B) Intermittent (fails 1 in 5 times)
   → Flaky test, need fix

C) Environmental (fails on certain runners)
   → Infrastructure or configuration issue
```

**Step 3: Create issue for tracking**

```
Title: Fix flaky test: TestClassName.testMethodName
Type: Bug / Tech Debt
Label: test-flakiness, stability

Description:
- Test fails approximately X% of the time
- Error pattern: [ERROR_MESSAGE]
- Impact: Success rate dropped to YY%
- Root cause analysis: [HYPOTHESIS]
- Proposed fix: [SOLUTION]
```

**Step 4: Monitor resolution**

```bash
# After fix merged
# → Watch next 20 runs
# → Success rate should improve
# → If not, assign to test author
```

### Issue: Sudden Performance Change

**When:** Feedback time suddenly increases from 27m to 33m

**Investigation:**
```
1. Check recent merges
   → git log --oneline master | head -10
   → What changed in last 1-2 hours?

2. Suspect areas:
   - New tests added
   - New services added to CI
   - Config changes (gradle, docker)
   - Infrastructure changes

3. Correlate with metrics
   - Check which job got slower
   - Compare with baseline for that job
   - Calculate performance delta

4. Verify hypothesis
   - Revert suspect change
   - Rerun workflow
   - Confirm performance returns to normal
```

**Actions:**
```
If confirmed as regression:
  1. Revert immediate
  2. Create issue for fix
  3. Allow author to improve without reverting

If not regression (just variance):
  1. Monitor next 5 runs
  2. Document hypothesis
  3. Continue normal operations
```

---

## Team Responsibilities

### All Engineers

**Required Actions:**

✅ **Respond to Performance Alerts**
- When tagged in alert issue, investigate your changes
- Explain performance impact
- Propose improvements if needed
- Estimate time to resolution

✅ **Write Performant Tests**
- Avoid slow operations in test setup
- Use mocking for external services
- Keep tests focused and independent
- Consider test execution time

✅ **Report Flaky Tests Immediately**
- Don't ignore test failures
- Post in #engineering thread
- Include error details
- Help debug if asked

### DevOps / CI/CD Lead

**Required Actions:**

✅ **Daily Monitoring (15 min)**
- Review dashboard status
- Check for alert issues
- Log any anomalies

✅ **Weekly Review (30 min)**
- Generate trend report
- Identify optimization opportunities
- Plan Phase 7 Tasks 7-8 work
- Share metrics with team

✅ **When Alert Triggered**
- Create Slack thread
- Assign investigation to relevant team
- Track time to resolution
- Document learnings

✅ **Threshold Management**
- Review thresholds monthly
- Adjust based on data
- Document rationale
- Communicate changes

### Team Lead / Manager

**Required Actions:**

✅ **Weekly Metrics Review (10 min)**
- Check performance status
- Review trend
- Celebrate improvements
- Plan next optimization

✅ **Monthly Check-In (20 min)**
- Review historical data
- Calculate ROI
- Plan Phase 7 Tasks 7-9
- Communicate to stakeholders

---

## Escalation Path

### Level 1: Blue (Info)
- Status: Performance within normal range
- Response: Monitor weekly
- Example: Feedback time 26-28m

### Level 2: Yellow (Warning)
- Status: Approaching warning threshold
- Response: Monitor daily, investigate if persists
- Example: Feedback time 31m, needs monitoring

### Level 3: Red (Critical)
- Status: Threshold exceeded
- Response: Immediate action required
- Escalation: Alert all engineers on Slack
- Example: Feedback time 36m, must fix

### Response SLA

| Level | Response Time | Resolution Time |
|-------|--------------|-----------------|
| Blue | N/A | N/A |
| Yellow | 1 hour | 4 hours (or plan documented) |
| Red | 15 minutes | 2 hours (or plan documented) |

---

## Common Q&A

### Q: Why is my PR slower than the target?

**A:** Several possible reasons:

1. **Added many tests** - Each test adds time
2. **Docker image changed** - Cache might be invalid
3. **New service** - Takes time to build and test
4. **Infrastructure variance** - GitHub Actions runners vary
5. **Resource contention** - Too many jobs running in parallel

**What to do:**
- Check which job got slower (dashboard or logs)
- Compare with previous similar PR
- Propose improvement (split job, optimize tests)
- Document in PR for team learning

### Q: Success rate is 93%, is that bad?

**A:** Not immediately critical, but worth monitoring.

- Target: 95%+
- Warning zone: 90-95%
- Plan: Monitor next 10 runs
- If drops further: Investigate test stability

**Actions:**
- Document flaky test patterns
- Create issues for test fixes
- Consider separating flaky tests
- Monitor trend (improving or worsening?)

### Q: Can we make the target time even shorter (25min)?

**A:** Maybe! But trade-offs:

| Approach | Pros | Cons |
|----------|------|------|
| More parallelization | Faster feedback | Limited by sequential ops |
| Aggressive caching | Significant gain | Cache misses cost time |
| Stricter test rules | Lean pipeline | More maintenance burden |
| Split services | More focused | Higher complexity |

**Process:**
1. Measure current bottleneck
2. Propose improvement
3. Estimate savings
4. Test on feature branch
5. Merge and monitor
6. Document results

### Q: What if infrastructure is slow?

**A:** GitHub Actions runner variance is normal (±10%).

If environmental issue:
- Check GitHub Actions status page
- Monitor consecutive runs
- If persistent: Open GitHub support ticket
- Meanwhile: Accept higher variance

If runner overloaded:
- Check concurrent workflow limits
- Consider scaling to more runners
- File request with ops team

### Q: How do we know if a threshold is wrong?

**A:** Review data periodically:

1. Collect 1 month of data
2. Analyze distribution
3. Identify realistic target
4. Get team agreement
5. Update thresholds
6. Document rationale

Example:
```
Current data (30 days):
- Min: 23m
- Avg: 26.5m
- Max: 31m
- Std Dev: 1.8m

Decision: Keep 27m target (avg), 30m warning (avg+2*std)
```

---

## Tools & Resources

### Dashboards & Metrics

| Tool | Location | Use |
|------|----------|-----|
| **HTML Dashboard** | `backend/docs/dashboards/cicd-performance.html` | Daily monitoring |
| **Markdown Report** | `backend/docs/dashboards/cicd-performance.md` | Team reports |
| **Raw Metrics** | `.github/metrics/` | Deep analysis |
| **GitHub Actions UI** | `https://github.com/REPO/actions` | View workflow logs |

### Slack Channels

- **#engineering** - General team discussions
- **@devops** - Escalate CI/CD issues
- **@team-lead** - Performance decisions

### Commands

```bash
# View latest metrics
cat .github/metrics/aggregated.json | jq '.summary_24h'

# View specific job durations
cat .github/metrics/aggregated.json | jq '.last_24h_runs[0].jobs | sort_by(.duration_min)'

# Check alert status
gh issue list --label phase-7,monitoring

# Manually trigger metrics collection
gh workflow run cicd-metrics-collector.yml

# Manually trigger alert check
gh workflow run cicd-alerts.yml
```

---

## Weekly Checklist

### Monday Morning (10 min)

- [ ] Check dashboard for weekend runs
- [ ] Review any alert issues
- [ ] Note any performance changes
- [ ] Post weekly status in Slack

### Daily (5 min)

- [ ] Quick dashboard glance
- [ ] Check for alert issues
- [ ] Investigate if yellow/red

### Friday Afternoon (20 min)

- [ ] Generate trend report
- [ ] Calculate weekly average
- [ ] Document performance changes
- [ ] Plan next week optimizations

### Monthly (30 min)

- [ ] Review full month trend
- [ ] Calculate metrics (avg, min, max, std dev)
- [ ] Assess threshold appropriateness
- [ ] Plan Phase 7 Tasks 7-9

---

## Documentation References

| Document | Purpose | Audience |
|-----------|---------|----------|
| **[CI_CD_PERFORMANCE_DASHBOARD.md](./CI_CD_PERFORMANCE_DASHBOARD.md)** | Technical details of monitoring system | Eng team |
| **[CI_CD_MONITORING_GUIDE.md](./CI_CD_MONITORING_GUIDE.md)** | This guide - how to use monitoring | Eng team |
| **[CLAUDE.md](../../CLAUDE.md)** | Phase 7 overview and progress | All |
| **[BUILD_MANAGEMENT_GUIDE.md](./BUILD_MANAGEMENT_GUIDE.md)** | Building and testing locally | Eng team |
| **[COMMAND_REFERENCE.md](./COMMAND_REFERENCE.md)** | Gradle and Docker commands | Eng team |

---

## Getting Help

**Problem:** Dashboard not updating
- Check: `.github/workflows/cicd-metrics-collector.yml` exists
- Verify: At least one workflow run completed
- Action: Manually trigger workflow

**Problem:** Can't understand metrics
- Read: Quick Start section above
- Check: Dashboard status colors (green/yellow/red)
- Ask: Post in #engineering Slack

**Problem:** Workflow slower than expected
- Check: Dashboard "Recent Runs" table
- Find: Which job is slowest
- Review: That job's logs in GitHub Actions

**Problem:** Suspected false alert
- Verify: Recent changes to backend/
- Analyze: Confirm if real regression or variance
- Post: Thread in #engineering with findings

---

## Success Criteria

After Phase 7 Task 6 implementation, monitoring is successful when:

✅ Dashboard auto-updates after each workflow run
✅ Developers understand feedback time metric
✅ Team responds to yellow/red alerts within SLA
✅ Monthly avg feedback time stays around 27m
✅ Success rate stays above 95%
✅ No critical alerts in any given week
✅ Team able to diagnose performance issues

---

## Next Steps (Phase 7 Tasks 7-9)

### Phase 7 Task 7: Cache Optimization
- Gradle dependency caching
- Docker layer cache improvements
- Target: Additional 10-15% improvement

### Phase 7 Task 8: Advanced Alerting
- Slack notifications
- Trend analysis and regression detection
- Performance comparison reports

### Phase 7 Task 9: Historical Reporting
- Weekly performance reports
- Team dashboard with trends
- ROI analysis

---

**Status:** ✅ ACTIVE
**Last Updated:** 2026-02-01
**Questions?** Post in #engineering or contact DevOps team
