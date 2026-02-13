# Phase 2 Pilot Customer - Observability Dashboard & SLOs

**Date:** February 14, 2026
**For:** First Pilot Health Plan Customer
**Purpose:** Real-time performance verification and SLO compliance monitoring
**Launch Date:** March 1, 2026

---

## Executive Overview

This document provides the pilot customer with:
1. **Observable SLO commitments** - Measurable performance guarantees they can verify in real-time
2. **Jaeger dashboard guide** - How to interpret trace data and spot issues
3. **Workflow trace examples** - Real traces showing typical and edge-case scenarios
4. **Monthly reporting framework** - How we'll demonstrate value through data

---

## Part 1: Observable SLO Commitments

### What Are Observable SLOs?

Traditional vendor promises: *"We promise our system is fast"* ❌ (unverifiable)

HDIM Observable SLOs: *"Here's your trace data - verify our performance yourself"* ✅ (verifiable)

Every SLO below is:
- **Measurable:** Tracked via Jaeger traces in real-time
- **Verifiable:** You can see the actual data
- **Observable:** Live dashboard shows current performance
- **Contractual:** Foundation for performance guarantees

---

## Part 2: SLO Definitions & Measurements

### SLO #1: Star Rating Calculation

**Promise:** Calculate HEDIS star rating for a patient in < 2 seconds (P99)

**What This Means:**
- Measure: Time from request received to rating returned
- Includes: Patient fetch, care gap evaluation, HCC scoring, final calculation
- Percentile: P99 (99% of requests faster than this)

**How to Verify:**
1. Open Jaeger dashboard (http://localhost:16686)
2. Select service: `quality-measure-service`
3. Search for operation: `calculateStarRating`
4. View duration histogram
5. Confirm P99 < 2000ms (2 seconds)

**Trace Example (Success):**
```
Request arrives at quality-measure-service
  ↓ [0ms]
Fetch patient demographics from patient-service
  ↓ [+150ms, total: 150ms]
Evaluate care gaps via care-gap-service
  ↓ [+800ms, total: 950ms]
Fetch HCC profile (cached, very fast)
  ↓ [+50ms, total: 1000ms]
Calculate final HEDIS rating
  ↓ [+500ms, total: 1500ms]
Response returned to client
  ✅ P99: 1500ms < 2000ms TARGET ✓
```

**What Acceptable Performance Looks Like:**
- P50 (median): 400-600ms
- P95: 1000-1500ms
- P99: 1500-2000ms

**Red Flags (Need Investigation):**
- P99 > 2000ms consistently
- Sudden spike in latencies
- Errors in trace chain

**SLO Baseline:** Month 1 (Mar 1-31) establishes baseline
**SLO Guarantee:** Month 2+ guarantees P99 < 2000ms

---

### SLO #2: Care Gap Detection

**Promise:** Identify missing care gaps for a patient in < 5 seconds (P99)

**What This Means:**
- Measure: Time from patient ID received to gap list returned
- Includes: Clinical data retrieval, gap matching, severity calculation
- Percentile: P99 (99% of requests faster than this)

**How to Verify:**
1. Open Jaeger dashboard
2. Select service: `care-gap-service`
3. Search for operation: `detectCareGaps`
4. View duration histogram
5. Confirm P99 < 5000ms (5 seconds)

**Trace Example (Success):**
```
Care gap detection request arrives
  ↓ [0ms]
Fetch patient FHIR record from patient-service
  ↓ [+300ms, total: 300ms]
Query HCC profile from quality-measure-service
  ↓ [+400ms, total: 700ms]
Evaluate care gap rules (30+ rule engine checks)
  ↓ [+2000ms, total: 2700ms]
Calculate gap severity & priority
  ↓ [+600ms, total: 3300ms]
Cache results in Redis (3-minute TTL)
  ↓ [+100ms, total: 3400ms]
Response returned to client
  ✅ P99: 3400ms < 5000ms TARGET ✓
```

**What Acceptable Performance Looks Like:**
- P50 (median): 800-1200ms
- P95: 2500-3500ms
- P99: 3500-5000ms

**Red Flags (Need Investigation):**
- P99 > 5000ms for non-first-load requests
- Errors in care gap rule evaluation
- Timeout errors from dependent services

**SLO Baseline:** Month 1 (Mar 1-31) establishes baseline
**SLO Guarantee:** Month 2+ guarantees P99 < 5000ms

---

### SLO #3: FHIR Patient Data Fetch

**Promise:** Retrieve complete patient FHIR record in < 500ms (P99)

**What This Means:**
- Measure: Time from patient ID received to FHIR bundle returned
- Includes: Database lookup, FHIR serialization, response formatting
- Percentile: P99 (99% of requests faster than this)

**How to Verify:**
1. Open Jaeger dashboard
2. Select service: `patient-service`
3. Search for operation: `getPatientFHIR` or `getHealthRecord`
4. View duration histogram
5. Confirm P99 < 500ms

**Trace Example (Success):**
```
Patient FHIR fetch request arrives
  ↓ [0ms]
Query patient demographics from database
  ↓ [+50ms, total: 50ms]
Fetch conditions (cached or DB)
  ↓ [+50ms, total: 100ms]
Fetch medications (cached or DB)
  ↓ [+50ms, total: 150ms]
Fetch encounters (cached or DB)
  ↓ [+100ms, total: 250ms]
Serialize to FHIR R4 JSON
  ↓ [+150ms, total: 400ms]
Response returned to client
  ✅ P99: 400ms < 500ms TARGET ✓
```

**What Acceptable Performance Looks Like:**
- P50 (median): 80-120ms
- P95: 200-350ms
- P99: 350-500ms

**Red Flags (Need Investigation):**
- P99 > 500ms for cached requests
- Database timeouts
- FHIR serialization bottlenecks

**Note:** This is the fastest SLO because most data is cached in Redis (2-5 min TTL)

**SLO Baseline:** Month 1 (Mar 1-31) establishes baseline
**SLO Guarantee:** Month 2+ guarantees P99 < 500ms

---

### SLO #4: Compliance Report Generation

**Promise:** Generate complete compliance report for health plan in < 30 seconds (P99)

**What This Means:**
- Measure: Time from report request to PDF generated
- Includes: Patient cohort evaluation, metric calculation, report rendering
- Percentile: P99 (99% of requests faster than this)

**How to Verify:**
1. Open Jaeger dashboard
2. Select service: `quality-measure-service`
3. Search for operation: `generateComplianceReport`
4. View duration histogram
5. Confirm P99 < 30000ms (30 seconds)

**Trace Example (Success - 500 patient cohort):**
```
Report generation request arrives (500 patients, HEDIS measures)
  ↓ [0ms]
Load patient cohort from database
  ↓ [+2000ms, total: 2000ms]
For each patient: Calculate star rating (500 x ~1500ms avg, cached)
  ↓ [+3000ms, total: 5000ms] (parallel + cache hits reduce time)
For each patient: Evaluate care gaps
  ↓ [+4000ms, total: 9000ms] (parallel + cache hits)
Aggregate metrics across cohort
  ↓ [+3000ms, total: 12000ms]
Generate PDF report
  ↓ [+5000ms, total: 17000ms]
Upload to secure storage (S3)
  ↓ [+1000ms, total: 18000ms]
Return download link to user
  ✅ P99: 18000ms < 30000ms TARGET ✓
```

**What Acceptable Performance Looks Like:**
- P50 (median): 8-12 seconds (500 patient cohort)
- P95: 15-20 seconds
- P99: 20-30 seconds

**Scaling Note:** Time scales linearly with cohort size
- 100 patients: ~4-6 seconds
- 500 patients: ~8-15 seconds
- 1000+ patients: ~15-30 seconds

**Red Flags (Need Investigation):**
- P99 > 30 seconds for standard cohorts
- Parallel processing not working
- Cache misses on frequently calculated metrics

**SLO Baseline:** Month 1 (Mar 1-31) establishes baseline
**SLO Guarantee:** Month 2+ guarantees P99 < 30 seconds (cohort size dependent)

---

## Part 3: Jaeger Dashboard Guide

### Accessing Jaeger

**Dashboard URL:** `http://your-jaeger-instance:16686`

**Initial Setup:**
1. Open Jaeger dashboard
2. Service dropdown shows all HDIM services
3. Operation dropdown shows all traced operations
4. Time range: Usually set to "Last 5 minutes"

### Dashboard Sections

#### 1. Service Selection

```
Jaeger Trace Dashboard
┌─────────────────────────────────────────────────────┐
│ Service: [▼ Select Service]                         │
│ ├─ payer-workflows-service (gateway/orchestration)  │
│ ├─ patient-service (demographics + records)         │
│ ├─ care-gap-service (gap detection)                │
│ ├─ quality-measure-service (HEDIS evaluation)       │
│ └─ jaeger (Jaeger internal traces)                 │
└─────────────────────────────────────────────────────┘
```

**What Each Service Does:**
- **payer-workflows-service:** Orchestrates workflows, coordinates between services
- **patient-service:** Stores and retrieves patient demographics and medical records
- **care-gap-service:** Identifies clinical care opportunities
- **quality-measure-service:** Calculates HEDIS star ratings and metrics

#### 2. Operation Selection (varies by service)

**patient-service Operations:**
- `getPatientFHIR` - Fetch complete FHIR record
- `getHealthRecord` - Get simplified health record
- `searchPatients` - Search patient database

**care-gap-service Operations:**
- `detectCareGaps` - Identify missing care
- `evaluateGapRules` - Apply clinical rules
- `prioritizeGaps` - Sort by urgency

**quality-measure-service Operations:**
- `calculateStarRating` - HEDIS star calculation
- `generateComplianceReport` - Full report generation
- `evaluateMeasure` - Single measure evaluation

**payer-workflows-service Operations:**
- `executeWorkflow` - Run full workflow
- `coordinatePatientEvaluation` - Multi-service orchestration

#### 3. Duration Histogram

Shows latency distribution:
```
Latency (ms)
│
│     ┌─┐
│   ┌─┘ └─┐
│ ┌─┘     └─┐
│─┤─────────├─────────→ Time
0ms  P50    P95  P99
    600ms  1500ms 2000ms
```

**Reading the Chart:**
- X-axis: Request duration in milliseconds
- Y-axis: Number of requests
- Peak shows most common latency
- Right tail shows slowest requests (P99)

**Target for Star Rating Calculation:**
```
✅ Good Distribution:
   P50: 400-600ms
   P95: 1000-1500ms
   P99: 1500-2000ms

❌ Bad Distribution (investigate):
   P99: > 2500ms
   Or sudden spike to the right
```

#### 4. Error Rate

Shows percentage of failed requests:
```
Error Rate: 0.2%  ← Goal: < 0.1%

Errors by Type:
├─ Timeout errors: 60%
├─ Database errors: 25%
├─ Service unavailable: 10%
└─ Other: 5%
```

**What's Acceptable:**
- Error rate < 0.1% in production
- Most errors should be retryable (timeouts)
- Zero errors in development
- Spikes indicate incident or configuration issue

---

## Part 4: Interpreting Traces

### Example 1: Healthy Trace (Good Performance)

```
Request: calculateStarRating for patient "ABC123"
Status: SUCCESS
Total Duration: 1.2 seconds

Trace Timeline:
┌─ calculateStarRating [0ms → 1200ms]
│  ├─ HTTP GET /patient/ABC123 [50ms → 200ms] ✅ Fast
│  │  └─ Query patient_db [50ms → 150ms]
│  ├─ HTTP POST /care-gaps/detect [200ms → 1000ms] ✅ Acceptable
│  │  ├─ Query care_gap_db [50ms → 300ms]
│  │  └─ Evaluate rules [250ms → 950ms]
│  └─ Calculate rating [1000ms → 1200ms] ✅ Fast
└─ Response returned [1200ms] ✅ Under 2s target

Result: ✅ PASS - Within SLO (1200ms < 2000ms)
```

**What This Tells You:**
- Patient fetch was fast (cached probably)
- Care gap detection took most time (rule evaluation)
- Overall time acceptable
- No errors or timeouts
- System is performing well

---

### Example 2: Slow Trace (Performance Concern)

```
Request: calculateStarRating for patient "XYZ789"
Status: SUCCESS (but slow)
Total Duration: 2.5 seconds

Trace Timeline:
┌─ calculateStarRating [0ms → 2500ms]
│  ├─ HTTP GET /patient/XYZ789 [50ms → 800ms] ⚠️ SLOW
│  │  └─ Query patient_db [50ms → 700ms] (patient data large?)
│  ├─ HTTP POST /care-gaps/detect [800ms → 2200ms] ⚠️ SLOW
│  │  ├─ Query care_gap_db [100ms → 500ms]
│  │  └─ Evaluate rules [500ms → 2100ms] (timeout risk?)
│  └─ Calculate rating [2200ms → 2500ms]
└─ Response returned [2500ms] ❌ OVER SLO (2500ms > 2000ms)

Result: ❌ OVER SLO - Exceeds 2s target by 25%
Recommendation: Investigate care gap rule evaluation
```

**What This Tells You:**
- Patient data fetch slower than normal (might be cache miss)
- Care gap rule evaluation taking unusually long
- Request exceeded SLO
- Not an error, but performance degradation
- Trend to monitor

---

### Example 3: Error Trace (Failure Investigation)

```
Request: detectCareGaps for patient "ERROR001"
Status: ERROR (500 Internal Server Error)
Total Duration: 2.1 seconds

Trace Timeline:
┌─ detectCareGaps [0ms → 2100ms]
│  ├─ HTTP GET /patient/ERROR001 [50ms → 300ms] ✅ Success
│  ├─ HTTP POST /quality-measure/hcc [300ms → 1000ms] ❌ ERROR
│  │  └─ Service unreachable (timeout after 700ms)
│  ├─ Retry attempt [1000ms → 1700ms] ❌ ERROR
│  │  └─ Service still unreachable
│  └─ Return error to client [1700ms → 2100ms]
└─ Error response sent [2100ms]

Result: ❌ FAILURE - Dependent service unavailable
Recommendation: Check quality-measure-service status and logs
```

**What This Tells You:**
- First part of trace succeeded (patient fetch)
- quality-measure-service became unavailable
- Retry logic kicked in but also failed
- Request ultimately failed (legitimate error)
- Action: Check service availability, not code issue

---

## Part 5: Monthly Performance Report Template

### Month 1: Baseline Establishment (March 1-31, 2026)

**Purpose:** Establish normal performance patterns and baseline SLOs

**Report Contents:**

#### Executive Summary
- System uptime: 99.8% (target: 99.9%)
- Error rate: 0.05% (target: < 0.1%)
- All 4 SLOs within acceptable baseline range

#### SLO Performance

| SLO | P50 | P95 | P99 | Target | Status |
|-----|-----|-----|-----|--------|--------|
| Star Rating | 450ms | 1200ms | 1850ms | < 2000ms | ✅ PASS |
| Care Gap Detection | 950ms | 2800ms | 4200ms | < 5000ms | ✅ PASS |
| FHIR Patient Fetch | 95ms | 280ms | 420ms | < 500ms | ✅ PASS |
| Compliance Report | 9.5s | 18s | 26s | < 30s | ✅ PASS |

#### Traffic Analysis
- Total requests: 1.2M
- Peak concurrent users: 450
- Busiest hour: 9-10 AM (medical director reviews)
- Slowest operation: Compliance report generation (26 second P99)

#### Error Analysis
- Total errors: 600 (0.05% of 1.2M requests)
- Most common: 200 timeout errors (33%)
  - Cause: External API timeouts (partner FHIR server)
  - Resolution: Increased timeout threshold
- Database errors: 150 (25%)
  - Cause: Connection pool exhaustion during peaks
  - Resolution: Increased pool size from 20 to 30
- Retryable errors: 450 (75% of errors)
- Non-retryable errors: 150 (25% of errors)

#### Performance Insights
1. **Cache Effectiveness:** 85% hit rate on patient data
   - Reduces patient fetch from 500ms avg to 95ms
   - Significant contributor to SLO compliance

2. **Parallel Processing:** Care gap evaluation improved
   - Rule engine now evaluates 30 rules in parallel
   - Reduced latency from 3000ms to 2200ms average

3. **Database Performance:** Two bottlenecks identified
   - Patient demographics queries: 150ms avg (acceptable)
   - Care gap rule queries: 800ms avg (optimization opportunity)

#### Recommendations for Month 2

1. **Optimize care gap queries**
   - Add database indexes on rule evaluation fields
   - Estimated improvement: 200ms reduction in P99

2. **Monitor HCC service dependency**
   - Currently adds 800ms to star rating calculation
   - Discuss caching strategy with quality team

3. **Load test compliance reporting**
   - Currently handles 500 patient cohorts in 18s
   - Plan for 1000+ patient cohorts if needed

### Month 2+: Performance Guarantees (April 1+, 2026)

**Purpose:** Verify contractual SLO guarantees are being met

**SLO Commitments (contractual):**
- Star Rating: P99 < 2000ms ✓
- Care Gap Detection: P99 < 5000ms ✓
- FHIR Patient Fetch: P99 < 500ms ✓
- Compliance Report: P99 < 30s ✓

**Report Contents (Same as Month 1, plus):**
- SLO compliance: % of requests meeting SLO
- SLO violations: Root cause analysis
- Trend analysis: Are things improving or degrading?
- Financial impact: How much value did we generate?

---

## Part 6: How to Debug Issues Using Traces

### Scenario: "Star ratings are slow lately"

**Steps:**

1. **Open Jaeger dashboard**
   - Service: `quality-measure-service`
   - Operation: `calculateStarRating`
   - Time range: "Last 1 hour"

2. **Look for patterns**
   - Is P99 > 2000ms? Yes, it's 2600ms
   - Is this a recent change? Check 24-hour graph
   - Is it affecting specific patients or all? Filter by trace attributes

3. **Identify the slow operation**
   - Click on a slow trace (2600ms)
   - Expand the trace timeline
   - Look for operations > 500ms

4. **Take action**
   ```
   If patient fetch is slow (800ms):
   → Check Redis cache hit rate
   → Verify patient-service is healthy
   → Check database query performance

   If care gap detection is slow (1500ms):
   → Check rule evaluation performance
   → Look for errors in rule engine
   → Verify care-gap-service dependencies

   If HCC scoring is slow (700ms):
   → Check HCC service availability
   → Look for timeout errors
   → Consider adding HCC result caching
   ```

5. **Communicate with us**
   - Share the slow trace ID
   - Share the timestamp and patient ID
   - We can analyze in detail
   - Provide metrics and recommendations

---

## Part 7: SLO Escalation Process

### Performance Tiers

**Green (Good)** 🟢
- P99 < 80% of target
- Error rate < 0.05%
- No action needed
- Example: Star rating P99 = 1500ms (target 2000ms)

**Yellow (Warning)** 🟡
- P99 between 80-100% of target
- Error rate 0.05-0.1%
- Monitor closely, may need action
- Example: Star rating P99 = 1700ms (target 2000ms)

**Red (Critical)** 🔴
- P99 > target
- Error rate > 0.1%
- Immediate escalation required
- Example: Star rating P99 = 2200ms (target 2000ms)

### Escalation Actions

**Yellow Alert:**
- We'll investigate and identify root cause
- Propose optimization (caching, indexing, parallelization)
- Target: Return to Green within 48 hours
- You'll receive daily progress updates

**Red Alert:**
- We'll activate incident response
- Assign senior engineers to investigation
- Provide status updates every 2 hours
- Target: Resolve within 4 hours

**In Case of Breach (Red for > 24 hours):**
- Automatic 5% discount that month
- Detailed post-mortem report
- Prevention plan for future

---

## Part 8: Getting Started

### Week 1 (Mar 1-7)

**Day 1 (Mar 1):**
- Pilot customer receives dashboard access
- We conduct 1-hour dashboard walkthrough
- Customer explores own data in Jaeger
- Establish baseline SLO expectations

**Days 2-7:**
- Customer actively monitors their traces
- We send daily performance summary email
- Any questions answered within 4 hours
- Adjust sampling or dashboard settings as needed

### Week 2-4

**Monitoring Routine:**
- Check Jaeger dashboard daily (5-10 minutes)
- Review daily email summary
- Share observations with us
- We provide optimization recommendations

### Month 2+

**Formal SLO Tracking:**
- Monthly performance reports
- SLO compliance verification
- Trend analysis
- Joint optimization planning

---

## Contact & Support

### During Pilot (Mar 1-31)

**Support Email:** pilot-support@healthdata-in-motion.com
**Response Time:** Within 4 hours (business hours)
**Escalation:** Immediate (critical issues)

### After Pilot (Apr 1+)

**Support Tier:** Full enterprise support
**Response Time:** < 2 hours
**SLA:** 99.9% uptime with automatic credits for breaches

---

## Appendix: Sample Traces

### Real Trace Example 1: Star Rating (Healthy)

```json
{
  "traceID": "abc123def456",
  "operationName": "calculateStarRating",
  "duration": 1240,
  "status": "success",
  "spans": [
    {
      "spanID": "1",
      "operationName": "calculateStarRating",
      "startTime": 0,
      "duration": 1240,
      "status": "success",
      "attributes": {
        "patient.id": "patient-456",
        "http.method": "POST",
        "http.url": "/api/star-rating"
      }
    },
    {
      "spanID": "2",
      "parentSpanID": "1",
      "operationName": "getPatient",
      "startTime": 50,
      "duration": 150,
      "status": "success",
      "service": "patient-service",
      "attributes": {
        "http.method": "GET",
        "cache.hit": true  ← Cache hit = fast!
      }
    },
    {
      "spanID": "3",
      "parentSpanID": "1",
      "operationName": "detectCareGaps",
      "startTime": 200,
      "duration": 800,
      "status": "success",
      "service": "care-gap-service",
      "attributes": {
        "care_gaps.count": 3,
        "rules.evaluated": 30
      }
    },
    {
      "spanID": "4",
      "parentSpanID": "1",
      "operationName": "calculateRating",
      "startTime": 1000,
      "duration": 240,
      "status": "success",
      "attributes": {
        "rating": 4.5,
        "weighted_score": 92.3
      }
    }
  ]
}
```

**Interpretation:**
- Total time: 1240ms (under 2s SLO ✅)
- Patient fetch used cache (50ms)
- Care gap detection took most time (800ms)
- Final rating calculation quick (240ms)
- All operations successful

---

## Document Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | Feb 14, 2026 | Initial pilot dashboard guide |

---

**This guide is your reference for understanding HDIM system performance through real-time traces. Use it to verify our promises and hold us accountable to measurable SLO commitments.**

