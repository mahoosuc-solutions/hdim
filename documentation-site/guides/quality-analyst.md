# Quality Analyst Guide

This guide is designed for **Quality Analysts** using the HDIM Clinical Portal to manage quality measurement, population health analytics, and compliance reporting.

## Overview

As a Quality Analyst, you are responsible for:
- Running population-wide quality evaluations
- Generating compliance reports
- Tracking quality measure performance
- Identifying improvement opportunities
- Supporting payer reporting requirements
- Analyzing care gap trends

---

## Your Dashboard

When you log in, you'll see the **Quality Analyst Dashboard** with these widgets:

### Compliance Summary Widget
Organization-wide metrics:
- Overall compliance rate
- Comparison to target
- Trend vs previous period
- YTD performance

### Measure Performance Widget
Performance by quality measure:
- Measure name
- Current rate
- Target rate
- Gap to target
- Trend indicator

**Quick Actions:**
- Click measure → View detailed breakdown
- Click "Evaluate All" → Run population evaluation

### Evaluation Status Widget
Recent and running evaluations:
- Active batch evaluations
- Progress percentage
- Completed today
- Scheduled evaluations

### Care Gap Trends Widget
Gap analysis:
- Open gaps by type
- Gaps by urgency
- Closure rate trend
- Average time to closure

---

## Population Evaluation

### Run Population Evaluation

1. Navigate to **Evaluations**
2. Click **"Population Evaluation"** tab
3. Configure:
   - Select measure(s) to evaluate
   - Select population (All patients or filtered)
   - Set evaluation date
   - Enable "Detect Care Gaps"
4. Click **"Run Evaluation"**
5. Monitor progress

### Monitor Evaluation Progress

**Real-time monitoring:**
- Progress bar shows completion percentage
- Count of patients evaluated
- Estimated time remaining
- Error count

**Background processing:**
- Navigate to **Visualization** → **Live Monitor**
- View all active evaluations
- Cancel if needed

### Review Evaluation Results

After completion:

1. **Summary View**
   - Total patients evaluated
   - Compliant count and rate
   - Non-compliant count and rate
   - Not eligible count

2. **By-Measure Breakdown**
   - Performance per measure
   - Comparison to previous run
   - Gap analysis

3. **Export Results**
   - CSV for detailed analysis
   - Excel for reporting
   - PDF for presentations

---

## Quality Reporting

### Generate Population Report

1. Navigate to **Reports**
2. Click **"Population Report"**
3. Configure:
   - Reporting period (year/quarter)
   - Measures to include
   - Provider/department filter (optional)
4. Click **"Generate"**
5. Review and export

**Report Contents:**
| Section | Data |
|---------|------|
| Summary | Total patients, evaluations, overall compliance |
| By Measure | Compliance rate per quality measure |
| By Category | Aggregate by measure category |
| Trends | Comparison to previous periods |
| Gaps | Open care gaps summary |
| Benchmarks | Performance vs regional/national benchmarks |

### Generate Trend Report

1. Navigate to **Reports**
2. Click **"Trend Report"**
3. Configure:
   - Date range (recommend 12+ months)
   - Measures to include
   - Comparison periods
4. Click **"Generate"**
5. Review trend visualizations

### Schedule Automated Reports

1. Navigate to **Admin** → **Report Scheduling**
2. Click **"Create Schedule"**
3. Configure:
   - Report type
   - Frequency (weekly, monthly, quarterly)
   - Recipients
   - Format (PDF, Excel)
4. Save and activate

---

## Care Gap Analysis

### Population Gap Analysis

1. Navigate to **Care Gaps**
2. View aggregate metrics:
   - Total open gaps
   - By urgency level
   - By gap type
   - By measure

### Identify Priority Areas

1. Sort by gap count (descending)
2. Identify measures with most gaps
3. Analyze root causes:
   - Data quality issues?
   - Outreach gaps?
   - Provider awareness?

### Track Closure Rates

1. Navigate to **Reports** → **Trend Report**
2. View care gap closure trends:
   - Monthly closure rate
   - Average days to closure
   - Closure by type
   - Reopened gaps

### Create Outreach Campaigns

Based on gap analysis:

1. Identify target population
2. Create patient list
3. Assign to outreach team
4. Track campaign progress
5. Measure outcomes

---

## Measure Management

### View Measure Library

1. Navigate to **Quality Measures** (or Admin → Measures)
2. Browse all measures:
   - HEDIS measures
   - Custom measures
   - Active/inactive status
   - Evaluable status

### Measure Performance Deep Dive

1. Click measure to view details
2. Review:
   - Current performance
   - Historical trend
   - Denominator/numerator breakdown
   - Exclusion analysis
   - Provider variation

### Favorite Measures

1. Click star icon on frequently used measures
2. Access quickly from favorites
3. Dashboard shows favorited measures

---

## Data Analysis

### Export Data for Analysis

1. Navigate to **Reports** or **Patients**
2. Apply filters as needed
3. Click **"Export"**
4. Select format:
   - CSV for Excel/data tools
   - JSON for programmatic use
5. Download and analyze

### Key Metrics to Track

**Compliance Metrics:**
- Overall compliance rate
- Compliance by measure
- Compliance by provider
- Compliance trend

**Gap Metrics:**
- Open gap count
- Gap age (days overdue)
- Closure rate
- Time to closure

**Operational Metrics:**
- Evaluation volume
- Outreach success rate
- Documentation completeness

### Benchmark Comparison

1. Navigate to **Reports** → **Comparison Report**
2. Compare performance to:
   - National benchmarks
   - Regional averages
   - Internal targets
   - Historical performance

---

## Payer Reporting

### Prepare Submission Data

1. Run population evaluation for reporting period
2. Generate Population Report
3. Export in required format
4. Validate against specifications
5. Document methodology

### Common Reporting Periods

| Payer Type | Reporting Period |
|------------|------------------|
| CMS MIPS | Calendar year |
| HEDIS | Calendar year, hybrid |
| Commercial | Varies by contract |
| State Medicaid | State-specific |

### Quality Review Checklist

Before submission:
- [ ] All required measures evaluated
- [ ] Data refresh completed
- [ ] Exclusions properly applied
- [ ] Sample validation completed
- [ ] Documentation prepared
- [ ] Approval obtained

---

## Visualization and Monitoring

### Live Monitor

1. Navigate to **Visualization** → **Live Monitor**
2. View real-time:
   - Active evaluations
   - System throughput
   - Error rates
   - Queue status

### CQL Execution Visualization

1. During evaluation, view execution details
2. Understand processing:
   - Data retrieval
   - Logic execution
   - Result calculation

### Performance Dashboards

1. Navigate to **Visualization**
2. View dashboards:
   - Compliance trends
   - Gap analysis
   - Provider comparison
   - Measure performance

---

## Best Practices

### Evaluation Scheduling

1. **Regular cadence** - Run monthly at minimum
2. **Off-peak timing** - Schedule during low-usage periods
3. **Pre-submission** - Evaluate before reporting deadlines
4. **Post-import** - Evaluate after data refreshes

### Data Quality

1. **Validate source data** - Check for completeness
2. **Monitor exclusions** - Review exclusion rates
3. **Track anomalies** - Investigate sudden changes
4. **Document methodology** - Maintain audit trail

### Continuous Improvement

1. **Identify patterns** - Look for systemic gaps
2. **Root cause analysis** - Understand why gaps exist
3. **Intervention tracking** - Measure outreach effectiveness
4. **Feedback loop** - Share insights with care teams

---

## Tips for Success

### Maximize Impact

1. **Focus on high-gap measures** - Prioritize improvement
2. **Engage stakeholders** - Share insights regularly
3. **Actionable reporting** - Provide recommendations
4. **Track interventions** - Measure what works

### Common Issues

| Issue | Solution |
|-------|----------|
| Low compliance rates | Analyze data quality, review exclusions |
| Evaluation errors | Check data completeness, contact support |
| Report discrepancies | Verify evaluation dates, methodology |
| Slow processing | Run during off-peak, reduce batch size |

---

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+K` | Global search |
| `Ctrl+E` | New evaluation |
| `Ctrl+R` | New report |
| `Esc` | Close dialog |

---

## Related Workflows

- [Quality Evaluation](/workflows/quality-evaluation)
- [Report Generation](/workflows/report-generation)
- [Care Gap Closure](/workflows/care-gap-closure)

## Related User Stories

- [US-QE-012: Run Batch Evaluation](/user-stories/quality-evaluations#us-qe-012)
- [US-RP-002: Generate Population Report](/user-stories/reports#us-rp-002)
- [US-RP-003: Generate Trend Report](/user-stories/reports#us-rp-003)
- [US-CG-001: View All Care Gaps](/user-stories/care-gaps#us-cg-001)
