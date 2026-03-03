# Metro Health Alliance - Mid-size ACO

> Enterprise-scale SMART on FHIR integration with real-time alerts and provider dashboards.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Mid-size ACO |
| **Size** | 45 practices, 180 providers, 42,000 attributed lives |
| **Structure** | Mix of employed and independent practices |
| **Location** | Denver Metro Area, CO |
| **EHR Systems** | Epic (predominant), Cerner (some specialties) |
| **Quality Programs** | ACO REACH (Global Risk), MIPS, Commercial P4P |
| **IT Capabilities** | Strong (dedicated ACO IT team) |
| **HDIM Tier** | Enterprise Plus |
| **Monthly Cost** | $2,499/month |

## Challenge

### Current State

Metro Health Alliance is a mature ACO that has participated in shared savings programs for 5 years. They have 42,000 attributed Medicare lives and take full downside risk under ACO REACH Global. With $380M in total cost of care, even a 1% quality score improvement translates to millions in shared savings.

Their challenge isn't data access—they have a data warehouse and BI team. The problem is speed. Their quality data is always 30-60 days old because batch ETL processes take time. By the time they identify a care gap, the patient has often already had an adverse event or sought care elsewhere.

They also struggle with provider engagement. 180 providers can't all look at the same dashboard. They need personalized, real-time feedback at the point of care.

### Pain Points

- **Latency:** Quality data is 30-60 days stale
- **Scale:** 42,000 patients, 180 providers—can't manage manually
- **Risk:** Global risk means quality failures cost them directly
- **Provider variability:** 20-point spread in quality scores across providers
- **Alert fatigue:** Existing alerts are too many, too late
- **Opportunity cost:** Each preventable admission costs $15,000+
- **Commercial pressure:** Blue Cross and Cigna have their own P4P programs

### Why HDIM

Metro Health Alliance's CMO saw a presentation on HDIM's real-time CQL engine at HIMSS. The ability to evaluate quality in <200ms—compared to their 48-hour batch process—was transformational. They could finally have real-time alerts at the point of care.

They ran a 90-day pilot with 5 practices. The pilot reduced HbA1c >9% by 8 percentage points. The board approved enterprise-wide deployment the following month.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    Metro Health Alliance ACO                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     Epic Enterprise                          │   │
│  │                    (35 Practices)                            │   │
│  │                                                              │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │              Epic SMART on FHIR                        │  │   │
│  │  │  • Real-time hooks on patient open                     │  │   │
│  │  │  • Bulk FHIR export (nightly)                         │  │   │
│  │  │  • Webhooks for encounters                             │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────┬──────────────────────────────┘   │
│                                 │                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                  Cerner Specialties                          │   │
│  │                   (10 Practices)                             │   │
│  │                                                              │   │
│  │  ┌───────────────────────────────────────────────────────┐  │   │
│  │  │              Cerner FHIR R4                             │  │   │
│  │  │  • Daily bulk sync                                      │  │   │
│  │  └───────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────┬──────────────────────────────┘   │
│                                 │                                   │
└─────────────────────────────────┼───────────────────────────────────┘
                                  │
                                  │ FHIR R4 / SMART on FHIR
                                  │ Real-time + Batch
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐  │
│  │  Epic SMART     │   │   Cerner FHIR   │   │  Webhook        │  │
│  │  Connector      │   │   Connector     │   │  Processor      │  │
│  └────────┬────────┘   └────────┬────────┘   └────────┬────────┘  │
│           │                     │                     │            │
│           └─────────────────────┼─────────────────────┘            │
│                                 │                                   │
│                        ┌────────▼────────┐                         │
│                        │  Real-Time CQL  │                         │
│                        │     Engine      │                         │
│                        │   (<200ms)      │                         │
│                        └────────┬────────┘                         │
│                                 │                                   │
│        ┌────────────────────────┼────────────────────────┐         │
│        │                        │                        │          │
│  ┌─────▼─────┐          ┌───────▼───────┐        ┌──────▼──────┐  │
│  │ Real-Time │          │   Provider    │        │   ACO       │  │
│  │  Alerts   │          │  Dashboards   │        │  Analytics  │  │
│  │           │          │  (per-PCP)    │        │             │  │
│  └─────┬─────┘          └───────────────┘        └─────────────┘  │
│        │                                                           │
│        ▼                                                           │
│  ┌───────────────────────────────────────────────────────────────┐│
│  │                    Alert Destinations                         ││
│  │  • Epic In-Basket (provider)                                  ││
│  │  • EHR Best Practice Alert                                    ││
│  │  • Care coordinator worklist                                  ││
│  │  • SMS/Email (patient permission)                             ││
│  └───────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Method: SMART on FHIR + Webhooks + Bulk

| Component | Method | Frequency | Purpose |
|-----------|--------|-----------|---------|
| Epic Real-time | SMART on FHIR hooks | On patient open | Point-of-care alerts |
| Epic Bulk | Bulk FHIR Export | Nightly | Population analytics |
| Epic Webhooks | Encounter notifications | Real-time | Admission/discharge |
| Cerner | FHIR R4 API | Daily | Specialty data |

### Real-Time Alert Configuration

| Alert Type | Trigger | Response Time | Destination |
|------------|---------|---------------|-------------|
| HbA1c >9% | Lab result posted | <5 seconds | Epic In-Basket + MA worklist |
| BP >160/100 | Vital recorded | <5 seconds | EHR Best Practice Alert |
| Admission | ADT webhook | <1 minute | Care coordinator notification |
| Discharge | ADT webhook | <1 minute | Transition of care worklist |
| Gap in care | Patient chart opened | <200ms | EHR banner alert |
| High-risk patient | Patient chart opened | <200ms | Risk score display |

### FHIR Scope Configuration

```
launch
openid
fhirUser
patient/*.read
offline_access
```

---

## Real-Time Alert Examples

### Point-of-Care Gap Alert

When a provider opens a patient chart in Epic:

```
┌─────────────────────────────────────────────────────────────────────┐
│  ⚠️  QUALITY CARE GAPS FOR MARIA SANTOS (DOB: 03/15/1958)         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  OVERDUE ACTIONS:                                                  │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ ● Colorectal Cancer Screening - Last: Never                 │   │
│  │   Action: Order FIT test or colonoscopy referral            │   │
│  │                                                              │   │
│  │ ● Diabetic Eye Exam - Last: 2022-11-15 (23 months ago)      │   │
│  │   Action: Refer to ophthalmology                            │   │
│  │                                                              │   │
│  │ ● Flu Vaccine - Last season: Not received                   │   │
│  │   Action: Administer today                                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  PATIENT RISK: High (Score: 78)                                    │
│  └── Contributing factors: A1c 8.9%, CKD Stage 3, CHF             │
│                                                                     │
│  [Close] [Order All Gaps] [View Full Dashboard]                    │
└─────────────────────────────────────────────────────────────────────┘
```

### Admission Alert (to Care Coordinator)

```
Subject: 🏥 ACO Patient Admitted - Robert Williams

Patient: Robert Williams (MRN: 1234567)
Attributed PCP: Dr. Jennifer Chen
Admission: Denver General Hospital
Date/Time: 10/15/2024 14:32
Admitting Dx: CHF Exacerbation (I50.9)

Risk Factors:
• Previous admission: 45 days ago (same dx)
• A1c: 9.2% (high)
• Medication adherence: Low (52%)
• Social isolation: Lives alone

Recommended Actions:
1. Contact inpatient case manager
2. Schedule PCP follow-up for 48-72 hrs post-discharge
3. Arrange home health evaluation
4. Review medication reconciliation

[View Patient] [Acknowledge] [Assign to Team Member]
```

---

## Implementation Steps

### Phase 1: Epic Enterprise Integration (Week 1-3)

- [x] Epic App Orchard submission/approval
- [x] SMART on FHIR configuration for 35 practices
- [x] Webhook endpoint setup
- [x] Bulk FHIR export configuration
- [x] Real-time hook testing
- [x] Security review and go-live

### Phase 2: Cerner Integration (Week 3-4)

- [x] Cerner CODE program registration
- [x] FHIR API configuration for 10 practices
- [x] Daily sync setup
- [x] Cross-EHR patient matching

### Phase 3: Alert Configuration (Week 4-5)

- [x] Define alert rules with clinical leadership
- [x] Configure Epic In-Basket delivery
- [x] Set up care coordinator worklists
- [x] Test alert delivery end-to-end
- [x] Tune alert frequency (reduce fatigue)

### Phase 4: Training & Rollout (Week 5-6)

- [x] Train 180 providers on point-of-care alerts
- [x] Train care coordinators on worklists
- [x] Train ACO leadership on analytics
- [x] Phased rollout (10 practices/week)

---

## Measures Enabled

### ACO REACH Global Risk Measures

| Measure | Weight | Baseline | Target | Financial Impact |
|---------|--------|----------|--------|------------------|
| All-Cause Readmission | 4x | 14.8% | <12.5% | $1.2M |
| Diabetes HbA1c >9% | 3x | 24% | <15% | $800K |
| Controlling High BP | 3x | 68% | >78% | $600K |
| Depression Screening | 2x | 55% | >75% | $300K |
| Colorectal Cancer Screening | 2x | 62% | >75% | $400K |
| Statin Therapy ASCVD | 2x | 78% | >88% | $250K |
| Falls Risk Screening | 1x | 72% | >85% | $150K |
| Medication Reconciliation | 1x | 82% | >95% | $100K |

### Provider-Level Performance

| Quintile | Providers | Avg Quality Score | Intervention |
|----------|-----------|-------------------|--------------|
| Top 20% | 36 | 92% | Recognition, share best practices |
| 60-80% | 36 | 82% | Standard support |
| 40-60% | 36 | 72% | Targeted coaching |
| 20-40% | 36 | 62% | Intensive support |
| Bottom 20% | 36 | 52% | Performance improvement plan |

---

## Expected Outcomes

### Readmission Impact

| Metric | Baseline | With HDIM | Impact |
|--------|----------|-----------|--------|
| 30-day readmission rate | 14.8% | 11.5% | -3.3 pts |
| Annual readmissions | 620 | 483 | -137 readmissions |
| Cost per readmission | $15,000 | $15,000 | — |
| **Annual savings** | — | — | **$2.05M** |

### Quality Score Impact

| Scenario | Quality Score | REACH Multiplier | Shared Savings |
|----------|---------------|------------------|----------------|
| Baseline (2023) | 72% | 0.72 | $2.88M |
| With HDIM (2024) | 88% | 0.88 | $3.52M |
| **Improvement** | +16 pts | +0.16 | **+$640K** |

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| Quality reporting | 120 hrs/month | 20 hrs/month | 100 hrs/month |
| Care gap identification | 80 hrs/month | Automated | 80 hrs/month |
| Provider scorecards | 40 hrs/month | 5 hrs/month | 35 hrs/month |
| High-risk patient ID | 60 hrs/month | Automated | 60 hrs/month |
| **Total** | **300 hrs/month** | **25 hrs/month** | **275 hrs/month** |

### Financial Summary

| Category | Annual Value |
|----------|--------------|
| **Readmission reduction** | $2,050,000 |
| **Additional shared savings** | $640,000 |
| **Commercial P4P bonuses** | $380,000 |
| **Staff efficiency** | $165,000 |
| **Total Annual Value** | **$3,235,000** |

### ROI Calculation

```
Annual Value:        $3,235,000
Annual HDIM Cost:    $29,988 ($2,499 × 12)
Net Annual Benefit:  $3,205,012
ROI:                 108x
Payback Period:      3 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Enterprise Plus tier | $2,499/month |
| SMART on FHIR real-time | Included |
| Webhook processing | Included |
| Provider dashboards (180) | Included |
| Custom alert rules | Included |
| **Total Monthly** | **$2,499/month** |
| **Annual Cost** | **$29,988/year** |

*Cost per attributed life: $0.06/patient/month*

---

## Success Metrics

| Metric | Target | Actual (Month 6) |
|--------|--------|------------------|
| Real-time alert latency | <200ms | 145ms avg |
| Alert-to-action rate | >60% | 72% |
| Care gaps closed/month | >2,000 | 2,847 |
| Provider dashboard adoption | >80% | 88% |
| Readmission rate reduction | -3 pts | -2.8 pts |
| Quality score improvement | +15 pts | +12 pts (on track) |

---

## CMO Testimonial

> "We've been doing population health for years, but we were always looking in the rearview mirror. By the time we knew a patient had an HbA1c of 10%, they'd already been living with it for 3 months. HDIM changed that. Now, when a provider opens a chart, they see the care gaps immediately. When a patient gets admitted, our care coordinator knows within 60 seconds. We closed 2,847 care gaps last month—that's almost 100 per day. Our readmission rate dropped from 14.8% to 12%. That's $2 million in avoided costs. The best part? Our providers don't hate it. The alerts are timely and actionable, not noise."
>
> — Dr. Michael Torres, CMO, Metro Health Alliance

---

## Related Resources

- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [ACO Measure Sets](../_shared/MEASURE_SETS.md#aco-measures)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
