# Customer Success Operations Guide

**Owner:** VP Customer Success
**Timeline:** February 20-28, 2026 (Preparation phase before pilot launch)
**Objective:** Prepare for pilot customer onboarding and support
**Status:** Ready to execute

---

## Overview

This guide provides step-by-step procedures for Customer Success team operations during Phase 2 pilot launch (Mar 1-31). Includes onboarding procedures, support playbooks, and customer communication templates.

---

## Week 1 (Mar 1-7): Customer Onboarding

### Pre-Launch Setup (By Feb 28)

**Day 1 Checklist (Before First Customer Call):**
```
[ ] Jaeger dashboard access credentials prepared (1-2 sets)
[ ] "Welcome to HDIM" email template finalized
[ ] Customer dashboard walkthrough deck reviewed
[ ] Jaeger user guide ("PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md") printed/shared
[ ] SLO contract template ready ("PHASE2_PILOT_CONTRACT_SLO_LANGUAGE.md")
[ ] Slack channel created (if using Slack for customer comms)
[ ] Weekly meeting scheduled (Thursday 2 PM)
[ ] Daily SLO email automation configured
[ ] Engineering on-call contact list available
[ ] Customer success metrics dashboard prepared
```

### Day 1: New Customer Gets Access

**Immediately After First VP Sales LOI:**

```
1. Generate Jaeger Dashboard Credentials
   - Create read-only Jaeger user account
   - Generate secure password (shared via secure channel)
   - Document: Username, password, Jaeger UI URL

2. Send Welcome Email
   ----
   Subject: Welcome to HDIM - Your Observable Dashboard Access

   Hi [Customer Name],

   Welcome to HDIM! Enclosed are your Jaeger dashboard credentials.

   Dashboard URL: https://traces.hdim.com
   Username: [generated_user]
   Password: [secure_password]

   Your dashboard shows real-time performance data for all HDIM
   operations. This is your window into exactly how the system performs.

   Next Steps:
   1. Log in and explore the dashboard (take 10 minutes)
   2. We'll do a 1-hour walkthrough call [SCHEDULED DATE]
   3. You'll see real traces, understand SLO metrics, ask questions

   Attached: Jaeger Dashboard User Guide (explains everything)

   Questions? Reply to this email or call [CS Contact].

   Cheers,
   [Your Name]
   Customer Success, HDIM
   ----

3. Attach: PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md
```

**Action Items:**
- [ ] Credentials created and tested
- [ ] Welcome email sent same day
- [ ] Jaeger dashboard guide provided
- [ ] Meeting link sent for Week 1 walkthrough

### Day 2-7: Weekly Dashboard Walkthrough Call (Schedule for Day 3-5)

**1-Hour Walkthrough Call (Scheduled for Thu or Fri)**

**Agenda (60 minutes):**
```
1. Opening & Welcome (5 min)
   "Thanks for trying HDIM. Today we're going to walk through your
    dashboard so you understand how to read the performance data and
    verify our SLO commitments."

2. Dashboard Login & Navigation (10 min)
   - Login together via screen share
   - Show services list (payer-workflows, patient, care-gap, quality-measure)
   - Show operations list (star-rating, detect-gaps, fetch-patient, etc.)
   - Point out: "This is YOUR data. You can filter, export, analyze."

3. Latency Metrics Explanation (10 min)
   - Show latency histogram for one operation
   - Explain P50 / P95 / P99
   - Point to SLO targets: "Star rating P99 < 2 seconds"
   - Show actual performance: "Actual P99: 1,847ms - we're exceeding target"

4. Trace Interpretation (20 min)
   - Select a healthy trace (under 2s for star rating)
   - Expand waterfall view
   - Identify components: "Database query: 800ms, RPC call: 200ms, etc."
   - Point out: "This is where the time went. Bottleneck is the database."

   - Select a slow trace (but still meeting SLO)
   - Show: "Database took longer this time: 1.5s total"
   - Explain: "Still under 2s SLO, but slower than average. This happens."

   - Select an error trace (if available)
   - Show error condition
   - Explain: "This request failed. You can see exactly why from the trace."

5. SLO Verification (10 min)
   - Show SLO definition (we committed to P99 < 2s for star ratings)
   - Show actual P99 from dashboard
   - Point out: "Your dashboard proves we're meeting this. Monthly report
     will confirm."

6. Questions & Next Steps (5 min)
   - Answer any questions about dashboard
   - Explain: "You'll get daily SLO email every morning"
   - Schedule: "Weekly check-in call each Thursday"
   - Escalation: "Need help? Email me, I'll respond within 4 hours"
```

**Screen Share Talking Points:**
```
"This is your window into HDIM's performance. Not vendor-controlled metrics.
Not internal dashboards you can't see. This is YOUR data.

You can:
- See every trace for 30 days
- Verify our SLO commitments yourself
- Export data for independent analysis
- Audit our performance anytime

This is what transparency in healthcare software looks like."
```

**Recording & Follow-up:**
- [ ] Record session (with customer permission)
- [ ] Send recording to customer (for reference)
- [ ] Send summary email with key takeaways
- [ ] Offer: "Questions anytime, no dumb questions"

### Daily SLO Email

**Template (Sent 8 AM every day during March):**
```
From: success@hdim.com
Subject: Your HDIM Daily SLO Report - [Date]

Hi [Customer Name],

Yesterday's Performance Summary:

Star Rating Calculation:
  P99 Latency: 1,823ms (Target: <2000ms) ✅ PASS

Care Gap Detection:
  P99 Latency: 4,237ms (Target: <5000ms) ✅ PASS

FHIR Patient Fetch:
  P99 Latency: 387ms (Target: <500ms) ✅ PASS

Compliance Report:
  P99 Latency: 18.4s (Target: <30s) ✅ PASS

All SLOs Met: YES ✅

View detailed traces: https://traces.hdim.com

Questions? Reply to this email.

- HDIM Customer Success
```

---

## Month 1 (Mar 1-31): Baseline Establishment

### Weekly Check-In Calls (Every Thursday)

**30-Minute Call Agenda:**

```
1. Performance Review (10 min)
   "How did performance look this week from your perspective?"
   - Review this week's SLO metrics
   - Compare to previous week
   - Identify any anomalies or slowness

2. Dashboard Insights (10 min)
   - Did they notice anything interesting in traces?
   - Any questions about how to read the data?
   - Can we help with optimization?

3. Operational Discussion (5 min)
   - Are there any issues or concerns?
   - Any features they want to see in traces?
   - How's adoption going internally?

4. Next Week Preview (5 min)
   - What's coming: "Next week you'll see March SLO summary"
   - Upcoming dates: "Apr 1 starts guarantee phase"
   - Set expectations: "Phase 2 means automatic credits if we miss"
```

**Notes Template (One per call):**
```
Date: March [X], 2026
Customer: [Name]
Attendees: [Customer names], [Your name]

Performance This Week:
- Star Rating P99: [X]ms - [Status]
- Care Gaps P99: [X]ms - [Status]
- Patient Fetch P99: [X]ms - [Status]
- Compliance Report P99: [X]s - [Status]

Key Discussion Points:
1. [Insight or question]
2. [Action item, if any]

Follow-up Actions:
- [ ] [Action 1]
- [ ] [Action 2]

Next Call: [Date/Time]
```

### End of Month: Baseline Report

**Due: March 31, 2026**

**Report Contents:**

```markdown
# HDIM Performance Baseline Report - March 2026
[Customer Name]

## Executive Summary
March 2026 established baseline performance for [Customer Name].
All SLOs were met throughout the month.

## Performance Metrics

### Star Rating Calculation
- P50 (50% of requests): 1,200ms average
- P95 (95% of requests): 1,800ms average
- P99 (99% of requests): 2,100ms PEAK
- Target (Phase 2): P99 < 2,000ms
- Status: Within range, slightly above target

### Care Gap Detection
- P50: 2,400ms average
- P95: 4,200ms average
- P99: 5,100ms PEAK
- Target (Phase 2): P99 < 5,000ms
- Status: Within range, very slightly above target

### FHIR Patient Fetch
- P50: 150ms average
- P95: 350ms average
- P99: 480ms PEAK
- Target (Phase 2): P99 < 500ms
- Status: Well within range

### Compliance Report
- P50: 8.5s average
- P95: 15.2s average
- P99: 22.8s PEAK
- Target (Phase 2): P99 < 30s
- Status: Well within range

## Observations

### Bottlenecks
The most time-consuming operation is the database query in care gap
detection (accounts for ~70% of P99 time). This is expected for complex
queries but could be optimized in the future.

### Consistency
All operations show consistent performance across the month. No degradation
or spikes observed. System is stable.

### Load Patterns
Peak usage times: 9 AM - 11 AM and 2 PM - 4 PM EST
Off-peak usage: Evenings and weekends

## Phase 2 Transition (April 1)

Starting April 1, HDIM guarantees the following SLOs:
- Star Rating: P99 < 2,000ms (99.5% monthly compliance)
- Care Gaps: P99 < 5,000ms (99.5% monthly compliance)
- Patient Fetch: P99 < 500ms (99.8% monthly compliance)
- Compliance Report: P99 < 30s (99.0% monthly compliance)

If we miss these targets, you automatically receive 5-10% service credit.

## Recommendations

1. **Database Optimization** (Optional, not urgent)
   Care gap detection query could be optimized with additional indexes.
   Would improve P99 from 5,100ms to estimated 4,200ms.

2. **Continue Monitoring**
   Keep checking dashboard weekly. Watch for any degradation or anomalies.

3. **Plan for Growth**
   Current performance assumed 500 users/month. Let us know if you scale.

## Questions?

Reply to this email or schedule a call. Happy to discuss optimization
opportunities or answer questions about the data.

---
[Your Name]
Customer Success, HDIM
```

---

## Month 2+ (Apr 1+): Performance Guarantees & Support

### Automatic Monthly SLO Reporting

**Process (Automated):**

1. **SLO Compliance Calculation** (Automated, Apr 1)
   - Query Prometheus for P99 latencies for each operation
   - Calculate monthly compliance percentage
   - Determine if any SLOs breached

2. **Service Credit Calculation** (Automated)
   ```
   IF breach_count > 0:
     credit_amount = (1 - (99.5% / actual_compliance%)) * 10%
     APPLY_TO_NEXT_INVOICE(credit_amount)
   ```

3. **Monthly Report Generation** (Automated, last day of month)
   ```markdown
   # April 2026 SLO Compliance Report
   [Customer Name]

   ## Compliance Summary
   ✅ Star Rating: 99.7% compliant (Target: 99.5%)
   ✅ Care Gaps: 99.2% compliant (Target: 99.5%)
   ✅ Patient Fetch: 99.9% compliant (Target: 99.8%)
   ✅ Compliance Report: 98.8% compliant (Target: 99.0%)

   ## Breaches
   - Compliance Report: Missed on April 15 (P99 = 32s, target 30s)

   ## Service Credit
   Credit Amount: $1,234 (5% monthly discount)
   Reason: Compliance Report P99 exceeded target 1 day
   Applied to: May invoice

   ## Trend Analysis
   [Charts showing performance trends throughout month]
   ```

### SLO Breach Response Procedure

**If SLO is breached:**

```
1. Automatic Alert (Prometheus)
   - Alert fires when breach detected
   - Page on-call engineer immediately
   - Log incident

2. Customer Notification (Automated, same day)
   Email to customer:
   ----
   Subject: HDIM SLO Alert - Care Gap Detection Performance

   [Time] UTC: Care gap detection P99 latency reached 5,234ms
   (exceeded 5,000ms target)

   Our engineering team has been notified and is investigating.

   Details: https://traces.hdim.com/?service=care-gap&hours=1

   You'll receive service credit on your next invoice for this breach.

   Updates will be posted to [incident channel].
   ----

3. Engineering Investigation (Within 2 hours)
   - Analyze traces from breach window
   - Identify root cause
   - Implement fix or mitigation

4. Customer Update (Same day)
   - Root cause explained
   - Fix deployed and verified
   - Expected impact explained

5. Post-Mortem (Within 24 hours)
   - Document what happened
   - Why it happened
   - How it will be prevented
   - Send to customer for transparency
```

### Escalation Procedures

**Tier 1: Customer Success (First Contact)**
```
Responsibilities:
- Answer dashboard questions
- Explain SLO metrics
- Schedule follow-up calls
- Gather performance feedback

Response SLA: 4 hours
Resolution SLA: 24 hours (for questions)
```

**Tier 2: Engineering (Performance Issues)**
```
Trigger: Performance degradation, SLO breach, error spikes

Responsibilities:
- Analyze traces for root cause
- Implement fixes
- Communicate timeline
- Prevent future occurrences

Response SLA: 1 hour
Resolution SLA: 4 hours (for critical issues)
```

**Tier 3: VP Engineering (SLA Breaches)**
```
Trigger: Multiple SLO breaches in month, service credits >10%

Responsibilities:
- Review trend analysis
- Commit to improvement plan
- Executive communication
- Root cause prevention

Response SLA: Same day
Meeting Required: Yes
```

---

## Training Checklist (For Feb 20-25)

**CS Team Must Know:**
```
[ ] How to access and navigate Jaeger dashboard
[ ] How to read latency metrics (P50, P95, P99)
[ ] How to interpret trace waterfall (identify bottlenecks)
[ ] What each SLO means (Star, Gaps, Fetch, Reports)
[ ] How to explain SLO breaches to customers
[ ] When to escalate to engineering
[ ] Customer communication templates
[ ] Onboarding procedures
[ ] Troubleshooting procedures
```

---

## Customer Communication Templates

### Template 1: Welcome Email
```
Subject: Welcome to HDIM - Your Observable Dashboard Access

Hi [Customer Name],

Welcome! [Company] has chosen HDIM for its observable quality
measurement platform.

Your Jaeger Dashboard Access:
URL: https://traces.hdim.com
Username: [generated]
Password: [secure]

Your dashboard is live right now with real performance data.

Next Steps:
1. Log in and explore (10 minutes)
2. Read attached guide (15 minutes)
3. We'll do a walkthrough call [DATE] at [TIME]

This dashboard shows you exactly what we promised: real-time,
verifiable performance proof.

Questions? Reply here or call [number].

Welcome aboard,
[Your Name]
```

### Template 2: SLO Breach Notification
```
Subject: HDIM Performance Alert - Immediate Action Taken

At [TIME] on [DATE], HDIM's Care Gap Detection service exceeded
its SLO target:

Actual P99: 5,234ms
Target P99: 5,000ms
Breach Duration: 3 minutes
Status: Resolved

What We Did:
1. Identified root cause (database lock during backup)
2. Optimized query
3. Deployed fix (verification complete)

Service Credit:
Amount: $XXX (X% credit)
Applied to: [Invoice date]

Root Cause Analysis:
[Link to detailed post-mortem]

This transparency is what observable SLOs mean. You see the problem,
we explain what happened, and you're credited automatically.

Questions? Let's discuss.

[Your Name]
```

### Template 3: Monthly Performance Report
```
Subject: Your HDIM Monthly Performance Report - [Month]

Hi [Customer Name],

Here's your April 2026 performance summary:

SLO Compliance:
✅ Star Rating: 99.7% (Target: 99.5%)
✅ Care Gaps: 99.2% (Target: 99.5%)
✅ Patient Fetch: 99.9% (Target: 99.8%)
✅ Compliance Report: 98.8% (Target: 99.0%)

Service Credits This Month:
Amount: $[X]
Reason: [Reason for credits]
Applied to: [Next invoice]

Trends:
[Link to dashboard showing trends]

Next Steps:
We'll discuss optimization opportunities in your weekly call this week.

Questions? Let's talk.

[Your Name]
```

---

## Success Metrics (March Pilot)

**Track:**
- [ ] Customer onboarding time (target: <2 hours)
- [ ] Dashboard walkthrough satisfaction (target: >4/5)
- [ ] Weekly call attendance (target: 100%)
- [ ] SLO compliance (target: >99%)
- [ ] Customer satisfaction score (target: >4.5/5)
- [ ] Time to resolution (target: <4 hours)
- [ ] Escalation frequency (target: <1 per month)

---

## Next Steps

1. **Feb 20-25:** CS team completes training
2. **Feb 28:** All procedures tested with staging customer
3. **Mar 1:** Live customer onboarding begins
4. **Mar 7:** First customer walk-through complete
5. **Mar 31:** Baseline report generated

---

**Generated:** February 14, 2026
**Timeline:** Feb 20-28 (Preparation), Mar 1-31 (Pilot execution)
**Status:** Ready to execute
