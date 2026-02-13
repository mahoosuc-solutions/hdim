# Pilot Customer Contract - Observable Service Level Objectives (SLOs)

**Document Type:** Contract Language for Pilot Customer Agreement
**Effective Date:** March 1, 2026
**Service Period:** Months 1-12
**Last Updated:** February 14, 2026

---

## 1. Service Level Objectives Overview

The parties agree that performance of the HDIM platform shall be measured using Observable Service Level Objectives (SLOs). Observable SLOs are unique because all metrics are verifiable in real-time through the Jaeger distributed tracing dashboard, providing complete transparency into actual system performance.

### 1.1 Key Difference from Traditional SLOs

**Traditional SLOs:**
- Vendor claims: "Our system is fast"
- Measurement: Internal metrics only
- Verification: Trust us
- Result: Vendor controls narrative

**Observable SLOs (HDIM Standard):**
- Vendor commits: "Here's your live trace data"
- Measurement: Customer-accessible dashboard
- Verification: Customer can verify in real-time
- Result: Transparent, verifiable commitments

### 1.2 Measurement & Verification

All SLOs listed in Section 2 shall be measured and verified using:

1. **Jaeger Distributed Tracing System**
   - Real-time trace visualization at `http://your-jaeger-instance:16686`
   - Customer has read-only access to all traces
   - Traces capture 100% of requests (development) or 10% of requests (production)

2. **Automated SLO Dashboard**
   - Monthly performance reports (auto-generated)
   - Real-time SLO compliance dashboard
   - Email alerts if SLO is at risk of breach

3. **Third-Party Verification**
   - Customer may request independent SLO verification
   - HDIM shall provide trace exports in standard format
   - Typical verification time: 5 business days

---

## 2. Observable Service Level Objectives

### 2.1 Phase 1: Baseline Establishment (March 1 - March 31, 2026)

During Phase 1, HDIM shall establish baseline performance for the four core operations. These baselines inform the performance guarantees in Phase 2 and beyond.

#### SLO #1.1: Star Rating Calculation

**Definition:** Time from receipt of star rating calculation request to complete rating result returned to client.

**Measurement Points:**
- Request starts: HTTP POST received by quality-measure-service
- Request ends: HTTP response sent with rating payload
- Percentile: P99 (99th percentile of requests)
- Period: Rolling 30-day window

**Phase 1 Baseline (Mar 1-31, 2026):**
- Target P50: 400-600ms
- Target P95: 1000-1500ms
- Target P99: 1500-2000ms (baseline establishment, not yet guaranteed)
- Verification: Jaeger dashboard, `quality-measure-service` → `calculateStarRating` operation

**Data Captured:**
- Request timestamp, patient ID, calculation parameters
- Dependency latencies (patient fetch, care gap detection, HCC lookup)
- Cache hit/miss status
- Errors and timeouts
- Processing time breakdown

---

#### SLO #1.2: Care Gap Detection

**Definition:** Time from receipt of care gap detection request for a patient to complete list of gaps returned.

**Measurement Points:**
- Request starts: HTTP POST received by care-gap-service
- Request ends: HTTP response sent with gaps payload
- Percentile: P99 (99th percentile)
- Period: Rolling 30-day window

**Phase 1 Baseline (Mar 1-31, 2026):**
- Target P50: 800-1200ms
- Target P95: 2500-3500ms
- Target P99: 3500-5000ms (baseline establishment)
- Verification: Jaeger dashboard, `care-gap-service` → `detectCareGaps` operation

**Data Captured:**
- Request timestamp, patient ID, clinical context
- Rule evaluation latencies (30+ clinical rules)
- Dependency latencies (patient service, quality measure service)
- Cache status (HIPAA-compliant 2-minute cache)
- Number of gaps detected
- Errors and exceptions

---

#### SLO #1.3: FHIR Patient Data Fetch

**Definition:** Time from receipt of patient ID to complete FHIR R4-compliant patient record returned.

**Measurement Points:**
- Request starts: HTTP GET received by patient-service
- Request ends: HTTP response sent with FHIR Bundle
- Percentile: P99 (99th percentile)
- Period: Rolling 30-day window

**Phase 1 Baseline (Mar 1-31, 2026):**
- Target P50: 80-120ms (most requests cached)
- Target P95: 200-350ms
- Target P99: 350-500ms (baseline establishment)
- Verification: Jaeger dashboard, `patient-service` → `getPatientFHIR` operation

**Data Captured:**
- Request timestamp, patient ID, requested resources
- Cache hit/miss (Redis 2-minute HIPAA-compliant TTL)
- Database query latencies
- FHIR serialization time
- Bundle size (affects network latency)
- Data completeness (% of requested resources included)

---

#### SLO #1.4: Compliance Report Generation

**Definition:** Time from receipt of report generation request to downloadable compliance report (PDF) ready for delivery.

**Measurement Points:**
- Request starts: HTTP POST received with report parameters (patient cohort, measures, date range)
- Request ends: HTTP response sent with download link
- Percentile: P99 (99th percentile)
- Period: Rolling 30-day window

**Phase 1 Baseline (Mar 1-31, 2026):**
- Target P50: 8-12 seconds (for 500-patient cohort)
- Target P95: 15-20 seconds
- Target P99: 20-30 seconds (baseline establishment)
- Verification: Jaeger dashboard, `quality-measure-service` → `generateComplianceReport` operation
- Cohort Size Note: Time scales linearly with patient count; adjusted targets provided for >500 or <500 patient cohorts

**Data Captured:**
- Request timestamp, cohort size, measure selection
- Patient evaluation latencies (per-patient parallel processing)
- Metric aggregation time
- PDF generation and storage time
- Report delivery time (S3 upload)
- File size and completeness
- Errors during generation

---

### 2.2 Phase 2: Performance Guarantees (April 1, 2026 onwards)

Upon completion of Phase 1 baseline establishment, HDIM commits to the following performance guarantees, effective April 1, 2026.

#### SLO #2.1: Star Rating Calculation

**Guaranteed Performance Commitment:**
- P99 latency shall not exceed **2.0 seconds (2000ms)**
- Monthly SLO compliance target: **99.5%** of requests must meet P99 < 2000ms
- This guarantee applies to all patient cohorts and calculation scenarios

**SLO Breach Definition:**
- Monthly SLO compliance drops below 99.5%
- Example: More than 1 in 200 requests exceeds 2000ms

**Breach Remediation:**
- HDIM shall provide root cause analysis within 5 business days
- HDIM shall propose optimization plan within 5 business days
- If breach persists beyond 7 days: 5% monthly service credit applied

**Exception Cases (not counted as breach):**
- System maintenance windows (max 2 hours/month, scheduled 72 hours in advance)
- Dependency service failures (external FHIR server, HCC provider)
- Customer-caused issues (incorrect API parameters, DDoS-like behavior)

---

#### SLO #2.2: Care Gap Detection

**Guaranteed Performance Commitment:**
- P99 latency shall not exceed **5.0 seconds (5000ms)**
- Monthly SLO compliance target: **99.5%** of requests must meet P99 < 5000ms
- This guarantee applies to standard clinical rule sets and patient populations

**SLO Breach Definition:**
- Monthly SLO compliance drops below 99.5%
- Example: More than 1 in 200 requests exceeds 5000ms

**Breach Remediation:**
- HDIM shall provide root cause analysis within 5 business days
- HDIM shall propose optimization plan within 5 business days
- If breach persists beyond 7 days: 5% monthly service credit applied

**Exception Cases:**
- System maintenance windows
- Rule engine dependency failures
- Large patient record processing (>1GB records)
- Dependency service failures

---

#### SLO #2.3: FHIR Patient Data Fetch

**Guaranteed Performance Commitment:**
- P99 latency shall not exceed **500 milliseconds (500ms)**
- Monthly SLO compliance target: **99.8%** of requests must meet P99 < 500ms
- This is the strictest SLO due to caching effectiveness and expected baseline performance

**SLO Breach Definition:**
- Monthly SLO compliance drops below 99.8%
- Example: More than 1 in 500 requests exceeds 500ms

**Breach Remediation:**
- HDIM shall provide root cause analysis within 4 business days
- HDIM shall propose optimization plan within 4 business days
- If breach persists beyond 7 days: 10% monthly service credit applied (higher credit due to more strict target)

**Exception Cases:**
- System maintenance windows
- Cache cluster failures
- Database performance issues affecting HIPAA-compliant cached data
- Unusually large patient records (>100MB)

---

#### SLO #2.4: Compliance Report Generation

**Guaranteed Performance Commitment:**
- P99 latency shall not exceed **30 seconds** for patient cohorts up to 500 patients
- P99 latency shall not exceed **60 seconds** for patient cohorts 501-1000 patients
- P99 latency shall not exceed **120 seconds** for patient cohorts >1000 patients
- Monthly SLO compliance target: **99.0%** of requests must meet respective P99 target
- Note: Report generation is the most computationally intensive operation

**SLO Breach Definition:**
- Monthly SLO compliance drops below 99.0%
- Example: More than 1 in 100 requests exceeds P99 target for their cohort size

**Breach Remediation:**
- HDIM shall provide root cause analysis within 5 business days
- HDIM shall propose optimization plan within 5 business days
- If breach persists beyond 7 days: 5% monthly service credit applied

**Exception Cases:**
- System maintenance windows
- Unusually large cohort processing (>2000 patients)
- Complex custom measure evaluation
- Database performance under load
- Third-party service dependencies

---

## 3. Measurement & Verification Process

### 3.1 Real-Time Dashboard Access

Customer shall have read-only access to:
- Jaeger distributed tracing dashboard at `http://your-jaeger-instance:16686`
- Real-time SLO compliance dashboard (provided by HDIM)
- Service health and status page

**Access Details:**
- Browser: Any modern browser (Chrome, Firefox, Safari, Edge)
- Authentication: Single Sign-On (SAML/OAuth)
- Data retention: 30 days of trace data on-system, 1-year archive available
- Refresh rate: Real-time (traces appear within 5-10 seconds of request)

### 3.2 Monthly SLO Reports

HDIM shall provide automated SLO reports by the 5th of each month containing:

1. **Performance Summary**
   - Overall uptime percentage
   - Overall error rate
   - Average and peak load metrics

2. **SLO Compliance Table**
   ```
   | Metric | P50 | P95 | P99 | Target | Status |
   | Star Rating | 500ms | 1200ms | 1950ms | <2000ms | ✅ PASS |
   | Care Gap | 950ms | 2800ms | 4800ms | <5000ms | ✅ PASS |
   | Patient Fetch | 95ms | 280ms | 480ms | <500ms | ✅ PASS |
   | Compliance Report | 9.2s | 18.5s | 29.8s | <30s | ✅ PASS |
   ```

3. **Error Analysis**
   - Total errors and error rate
   - Error breakdown by type (timeout, database, service unavailable)
   - Root causes and resolutions

4. **Performance Insights**
   - Top slow operations
   - Cache hit rates
   - Database performance metrics
   - Dependency service performance

5. **Recommendations**
   - Optimization opportunities identified
   - Preventive actions recommended
   - Planned improvements for next month

### 3.3 Independent Verification Rights

Customer may request independent SLO verification:

1. **Trace Data Export**
   - HDIM shall export traces in standard OpenTelemetry format
   - Export scope: 30-day rolling window or custom date range
   - Timeline: Provided within 5 business days
   - Cost: Included in base service (first 2 exports/month)

2. **Third-Party Audit**
   - Customer may hire external auditor
   - HDIM shall cooperate fully with audit process
   - Auditor shall sign NDA before data access
   - Timeline: 2-week audit period
   - Cost: Customer responsible, HDIM provides data at no additional charge

3. **Variance Resolution**
   - If independent verification shows variance from HDIM reports:
   - Both parties investigate root cause (usually measurement method differences)
   - Mutually agreed adjustment made if necessary
   - Variance report attached to next monthly SLO report

---

## 4. Service Credits (SLO Breach Remedies)

### 4.1 Service Credit Schedule

If HDIM fails to meet guaranteed SLO (Section 2.2), customer shall receive monthly service credits automatically applied to next invoice:

**Star Rating SLO (P99 < 2000ms):**
- Monthly compliance 99.0-99.4%: 5% monthly service credit
- Monthly compliance <99.0%: 10% monthly service credit

**Care Gap Detection SLO (P99 < 5000ms):**
- Monthly compliance 99.0-99.4%: 5% monthly service credit
- Monthly compliance <99.0%: 10% monthly service credit

**Patient Fetch SLO (P99 < 500ms):**
- Monthly compliance 99.5-99.7%: 5% monthly service credit
- Monthly compliance <99.5%: 10% monthly service credit

**Compliance Report SLO (P99 < 30/60/120s):**
- Monthly compliance 98.5-98.9%: 5% monthly service credit
- Monthly compliance <98.5%: 10% monthly service credit

### 4.2 Credit Limits

- Maximum monthly service credits: 30% of monthly service fee
- Credits do not provide refunds; applied to future invoices only
- Credits expire if not used within 6 months
- Credits are exclusive remedy for SLO breaches (no additional damages)

### 4.3 Automatic Application

Service credits shall be:
- Automatically calculated and applied by HDIM
- Shown as line item on next month's invoice
- Acknowledged in monthly SLO report
- No customer request required

---

## 5. Exclusions & Exceptions

The following events are NOT counted as SLO breaches and do NOT trigger service credits:

### 5.1 Scheduled Maintenance

- Window: Scheduled between 11:00 PM - 2:00 AM ET, Sundays and Wednesdays only
- Duration: Maximum 2 hours per month
- Notice: 72 hours advance notice via email and dashboard banner
- Frequency: Maximum 2 maintenance windows per month

### 5.2 Unscheduled Maintenance (Emergencies)

- Scope: Security incidents, data corruption, critical vulnerabilities
- Notice: As much notice as practicable (typically 4 hours)
- Duration: Limited to time necessary to resolve emergency
- Limit: Maximum 4 hours per year unscheduled maintenance

### 5.3 Dependency Service Failures

Failures of external services outside HDIM's control are not counted as SLO breaches:
- Patient health information provider FHIR servers
- HCC calculation services (if external)
- Payment processing systems
- Customer's network or infrastructure

**Note:** HDIM maintains redundancy and failover for most dependencies; only unexpected, unrecoverable failures qualify for exception.

### 5.4 Customer-Caused Issues

The following customer actions do not trigger SLO credits:
- Incorrect API usage or malformed requests
- DDoS-like behavior or abusive access patterns
- Requests exceeding documented API limits
- Data format issues or incompatible data
- Unauthorized modifications to system configuration

### 5.5 Acts of God

Unforeseeable events outside both parties' control:
- Natural disasters
- Utility failures
- Government-mandated shutdowns
- Pandemic-related disruptions

---

## 6. SLO Improvement Roadmap

While Phase 2 SLO targets (Section 2.2) represent achievable, industry-leading commitments, HDIM is committed to continuous improvement:

### 6.1 Planned Optimizations (Q2 2026)

- **Care gap rule engine parallelization:** Target 30% latency reduction
- **Patient data caching expansion:** Target 95% cache hit rate (vs current 85%)
- **Report generation parallelization:** Target 40% latency reduction for large cohorts

### 6.2 Planned Optimizations (Q3 2026)

- **Database query optimization:** Expected 25% latency reduction
- **HCC service integration:** Embed scoring locally if external service used
- **Real-time cache warming:** Proactive cache updates based on access patterns

### 6.3 Aggressive Stretch Goals (Q4 2026)

If Phase 2 SLOs are consistently exceeded:
- Star Rating P99: Reduce from 2.0s to 1.0s target
- Care Gap P99: Reduce from 5.0s to 3.0s target
- Patient Fetch P99: Reduce from 500ms to 300ms target
- Compliance Report: Reduce target by 50% based on cohort size

---

## 7. Performance Monitoring & Alerting

### 7.1 Real-Time Alerts

HDIM shall configure automated alerts for:

1. **SLO At-Risk Alert**
   - Triggers when P99 hits 90% of SLO threshold
   - Example: Star rating P99 > 1800ms (90% of 2000ms target)
   - Action: Escalates to on-call engineering team
   - Customer notification: Email alert

2. **SLO Breach Alert**
   - Triggers when SLO is breached (P99 > target)
   - Action: Immediate incident investigation
   - Customer notification: Immediate email + SMS (if provided)

3. **Error Rate Alert**
   - Triggers when error rate > 0.5%
   - Action: Incident investigation
   - Customer notification: Email alert

### 7.2 Customer Notification

For all alerts:
- Email notification within 15 minutes of trigger
- Status page update within 30 minutes
- Incident update every 2 hours until resolved
- Final incident report within 24 hours

---

## 8. Performance Escalation Procedures

### 8.1 Performance Issue Classification

| Level | Condition | Response Time | Resolution Target |
|-------|-----------|---------------|--------------------|
| **Green** | P99 < 80% of target | None | Ongoing optimization |
| **Yellow** | P99 80-99% of target | 4 hours | 48 hours |
| **Red** | P99 > target | 1 hour | 4 hours |
| **Critical** | Red for >24 hours | 30 min | 2 hours |

### 8.2 Escalation Actions by Level

**Green:**
- Routine monitoring continues
- Monthly optimization review
- No customer notification

**Yellow:**
- Engineer assigned within 4 hours
- Daily status updates
- Customer email notification
- Plan to return to Green within 48 hours

**Red:**
- Senior engineer assigned within 1 hour
- Hourly status updates
- Page through executive if needed
- Full incident response activated

**Critical:**
- VP Engineering engaged
- 30-minute status updates
- Direct phone communication
- Root cause analysis under way

---

## 9. Dispute Resolution

### 9.1 SLO Dispute Process

If customer disputes SLO measurement or calculation:

1. **Initial Discussion** (within 3 business days)
   - Customer submits dispute with specific trace ID, timestamp, and concern
   - HDIM provides measurement methodology and data
   - Discussion to clarify measurement approach

2. **Data Review** (within 5 business days)
   - Both parties review raw trace data in Jaeger
   - Verification of calculation methodology
   - Identification of any measurement discrepancies

3. **Escalation** (if unresolved)
   - Joint meeting with engineering leads
   - Review of statistical methodology
   - Propose mutually agreeable resolution

4. **Independent Arbitration** (if still unresolved)
   - Both parties agree on independent auditor
   - Auditor reviews all data and methodology
   - Auditor's determination is binding
   - Costs split 50/50 between parties

### 9.2 Timeline SLA

SLO dispute resolution shall be completed within 15 business days of dispute submission.

---

## 10. Definitions

**P99 (99th Percentile):** The latency that 99% of requests complete within. For example, P99 = 2000ms means 99% of requests complete in 2 seconds or less; 1% may take longer.

**SLO (Service Level Objective):** A measurable commitment regarding system performance. HDIM commits to meet these SLOs; failure to do so triggers service credits.

**SLA (Service Level Agreement):** The legal agreement that includes SLOs and defines breach remedies. This document is part of the SLA.

**Trace:** A record of a single request through the system, including all operations performed, latencies, errors, and outcomes. All traces are stored in Jaeger.

**Jaeger:** Open source distributed tracing system that records and visualizes requests through the system.

**Observable:** Means verifiable through real-time data visible to the customer, not vendor-controlled internal metrics.

**Monthly SLO Compliance:** Percentage of requests in a month that meet the SLO target. Example: 99.5% compliance = 99.5% of requests meet target, 0.5% exceed target.

**P50, P95, P99:** Percentile latency metrics showing how fast 50%, 95%, and 99% of requests complete.

---

## 11. Changes & Updates

HDIM may update SLO targets or measurement methodologies with 30 days' notice to Customer. Customer must accept updates in writing or may terminate engagement without penalty.

Updates may be required due to:
- Infrastructure improvements enabling better performance
- Scaling to larger patient populations
- New feature additions requiring different measurement
- Industry standard changes

---

## 12. Effective Date & Termination

**Effective Date:** March 1, 2026 (Phase 1 - Baseline Establishment)
**Phase 2 Start Date:** April 1, 2026 (Performance Guarantees begin)
**Initial Term:** 12 months
**Termination:** Either party may terminate with 30 days' notice
**Upon Termination:** All credits, measurement obligations, and commitments expire

---

## Signature Block

**FOR HEALTHDATA-IN-MOTION:**

Name: ___________________________
Title: ___________________________
Date: ___________________________


**FOR [PILOT CUSTOMER]:**

Name: ___________________________
Title: ___________________________
Date: ___________________________

---

**Appendix A: Sample Monthly SLO Report** - Attached separately
**Appendix B: Jaeger Dashboard Access Guide** - PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md
**Appendix C: Trace Interpretation Examples** - PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md (Part 4)

