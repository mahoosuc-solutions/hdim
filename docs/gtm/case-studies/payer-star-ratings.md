# Case Study: Summit Health Plan

## Achieving 5-Star Medicare Advantage Excellence

---

## Executive Summary

**Organization:** Summit Health Plan
**Size:** 285,000 Medicare Advantage members
**Location:** Mountain West Region (3 states)
**Challenge:** Stagnant Star ratings threatening quality bonus revenue

**Results After 18 Months:**
- **Star Rating** improved from 3.5 to 4.5 Stars
- **$42M** in additional quality bonus payments
- **Quality composite** increased from 71% to 92%
- **Member satisfaction** improved 18 points

---

## The Challenge

### Star Rating Plateau

Summit Health Plan had been stuck at 3.5 Stars for three consecutive years:

**Quality Performance Issues:**
- HEDIS rates stagnant despite increased investment
- Data from provider network fragmented
- No real-time visibility into quality performance
- Annual measurement cycle limiting intervention opportunities

**Provider Network Complexity:**
- 2,400 contracted providers across 3 states
- 18 different EHR systems in network
- No standardized quality data sharing
- Providers unaware of their quality performance

**Financial Pressure:**
- 3.5 Star rating = base bonus only
- 4.0+ Star rating = 5% bonus payment
- 5.0 Star rating = 5% + extra benefits funding
- **At stake: $42M in annual quality bonuses**

**Member Experience Gaps:**
- CAHPS scores lagging competitors
- Care coordination perceived as poor
- Difficulty navigating network
- Chronic care management gaps

---

## The Solution

### HDIM Payer Quality Platform

Summit deployed HDIM to transform their quality management:

**Provider Data Aggregation:**
- FHIR connections to top 50 provider groups (80% of members)
- Claims-clinical data integration
- Real-time quality measure calculation
- Provider-level performance dashboards

**Quality Measure Management:**
- All 45 Star rating measures automated
- Prospective gap identification
- Supplemental data capture workflows
- HEDIS hybrid measure support

**Member Engagement:**
- Care gap outreach prioritization
- High-risk member identification
- Care coordinator workload optimization
- Member health assessment integration

**Provider Performance:**
- Provider quality scorecards
- Incentive tracking and payment
- Best practice sharing
- Targeted intervention support

---

## Implementation Approach

### Phased Deployment

| Phase | Duration | Focus |
|-------|----------|-------|
| Phase 1 | Months 1-4 | Top 10 provider groups (50% of members) |
| Phase 2 | Months 5-8 | Next 40 provider groups (30% of members) |
| Phase 3 | Months 9-12 | Claims-based quality for remaining providers |
| Phase 4 | Months 13-18 | Optimization and advanced analytics |

### Data Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                   Summit HDIM Platform                       │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Provider Data Sources                     │ │
│  │  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────────────┐ │ │
│  │  │ Epic │ │Cerner│ │Athena│ │NextGe│ │ 14 Other EHRs│ │ │
│  │  │  12  │ │  8   │ │  15  │ │  7   │ │    8 groups  │ │ │
│  │  │groups│ │groups│ │groups│ │groups│ │              │ │ │
│  │  └──────┘ └──────┘ └──────┘ └──────┘ └──────────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
│                          ↓                                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                Claims + Clinical Data Lake             │ │
│  │          (285,000 members, 3 years history)            │ │
│  └────────────────────────────────────────────────────────┘ │
│                          ↓                                   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Quality Measure Engine                    │ │
│  │    45 Star Rating Measures | Real-Time Calculation     │ │
│  └────────────────────────────────────────────────────────┘ │
│                          ↓                                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ Provider │ │  Member  │ │   Care   │ │  Executive   │   │
│  │Scorecards│ │ Outreach │ │   Mgmt   │ │  Dashboards  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

---

## Results

### Star Rating Improvement

| Domain | 2023 (Pre-HDIM) | 2024 | 2025 | Improvement |
|--------|-----------------|------|------|-------------|
| Staying Healthy | 3.0 | 4.0 | 4.5 | +1.5 Stars |
| Managing Chronic Conditions | 3.5 | 4.0 | 4.5 | +1.0 Stars |
| Member Experience | 3.0 | 3.5 | 4.5 | +1.5 Stars |
| Customer Service | 4.0 | 4.5 | 5.0 | +1.0 Stars |
| **Overall Star Rating** | **3.5** | **4.0** | **4.5** | **+1.0 Stars** |

### HEDIS Measure Performance

| Measure Category | 2023 | 2025 | Improvement |
|------------------|------|------|-------------|
| Breast Cancer Screening | 68% | 87% | +19 points |
| Colorectal Cancer Screening | 61% | 82% | +21 points |
| Diabetes Care (HbA1c) | 72% | 89% | +17 points |
| Controlling Blood Pressure | 65% | 84% | +19 points |
| Medication Adherence (3 measures avg) | 74% | 91% | +17 points |
| Statin Therapy (SUPD) | 71% | 88% | +17 points |
| **Overall HEDIS Composite** | **71%** | **92%** | **+21 points** |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| Quality Bonus Payment (4.5 Stars) | $42,000,000 |
| Reduced RADV Risk | $3,200,000 |
| Member Retention (reduced disenrollment) | $8,500,000 |
| Administrative Efficiency | $1,800,000 |
| **Total Annual Value** | **$55,500,000** |

### Operational Metrics

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Gap Closure Rate | 34% | 71% | +37 points |
| Provider Quality Data Lag | 6 months | Real-time | N/A |
| Care Coordinator Productivity | 120 members/FTE | 280 members/FTE | 133% improvement |
| Supplemental Data Capture | 12% | 68% | +56 points |

---

## Provider Performance Program

### Quality Scorecard Components

| Component | Weight | Metrics |
|-----------|--------|---------|
| HEDIS Preventive | 30% | Screenings, immunizations |
| HEDIS Chronic Care | 35% | Diabetes, cardiovascular, mental health |
| Medication Adherence | 20% | PDC measures |
| Patient Experience | 15% | CAHPS-related |

### Provider Incentive Results

| Provider Tier | Providers | Avg. Incentive | Total Paid |
|---------------|-----------|----------------|------------|
| 5-Star Performers | 340 | $45,000 | $15,300,000 |
| 4-Star Performers | 890 | $22,000 | $19,580,000 |
| 3-Star Performers | 720 | $8,000 | $5,760,000 |
| Below 3-Star | 450 | $0 | $0 |
| **Total P4Q Investment** | **2,400** | - | **$40,640,000** |

### Provider Engagement

- 92% of providers accessing quality dashboards monthly
- 78% of gap closures attributed to provider action
- 34 "Quality Champion" practices identified and featured
- Best practice playbooks developed from top performers

---

## Member Engagement Strategy

### Prioritized Outreach

HDIM's risk stratification enabled targeted member engagement:

| Segment | Members | Strategy | Gap Closure Rate |
|---------|---------|----------|------------------|
| High-Risk, Multiple Gaps | 28,500 | Care coordinator assignment | 74% |
| Moderate-Risk | 71,250 | Targeted outreach campaigns | 68% |
| Low-Risk, Single Gap | 114,000 | Automated reminders | 71% |
| Compliant | 71,250 | Retention focus | N/A |

### Channel Optimization

| Channel | Volume | Gap Closures | Cost per Closure |
|---------|--------|--------------|------------------|
| Care Coordinator Calls | 42,000 | 18,200 | $28 |
| IVR Campaigns | 285,000 | 31,400 | $4 |
| Text Messaging | 156,000 | 22,100 | $2 |
| Mail Campaigns | 98,000 | 8,700 | $12 |
| Provider EMR Alerts | N/A | 48,600 | $0 |

---

## Stakeholder Testimonials

### CEO Perspective
"Moving from 3.5 to 4.5 Stars was worth $42 million annually. HDIM paid for itself in the first month."
— *David Mitchell, CEO*

### CMO Perspective
"For the first time, we have real-time visibility into quality across our entire network. We went from reactive to proactive quality management."
— *Dr. Jennifer Adams, Chief Medical Officer*

### Provider Relations
"Our providers finally have the data they need to close gaps at the point of care. They're partners now, not just contractors."
— *Maria Gonzalez, VP Provider Relations*

### Network Provider
"The quality scorecard shows me exactly where my patients have gaps. I close them during visits instead of getting angry letters about my performance."
— *Dr. Robert Kim, Family Medicine*

---

## Technology Capabilities

| Capability | Application |
|------------|-------------|
| FHIR Integration | Real-time data from 50 provider groups |
| Claims Processing | Historical and concurrent claims analysis |
| Quality Engine | 45 Star rating measures, prospective calculation |
| Risk Stratification | Member prioritization for outreach |
| Provider Dashboards | Real-time scorecards and gap lists |
| Supplemental Data | Lab, pharmacy, HRA capture workflows |
| Reporting | CMS Star rating simulation and submission |

---

## ROI Analysis

### Investment
| Item | Annual Cost |
|------|-------------|
| HDIM Enterprise Platform | $280,000/year |
| Provider Integration Services | $150,000 (one-time) |
| Implementation & Training | $120,000 (one-time) |
| **Year 1 Total** | **$550,000** |

### Return
| Item | Value |
|------|-------|
| Quality Bonus Improvement | $42,000,000 |
| Operational Savings | $1,800,000 |
| Member Retention Value | $8,500,000 |
| **Total Year 1 Return** | **$52,300,000** |

### Metrics
- **ROI:** 9,409%
- **Payback Period:** 4 days
- **Quality Bonus per Dollar Invested:** $76

---

## Lessons Learned

1. **Provider Data is Gold** - Clinical data from EHRs drives Star improvement faster than claims
2. **Real-Time Matters** - Monthly data enables monthly intervention
3. **Align Provider Incentives** - P4Q programs drive behavior change
4. **Prioritize Outreach** - Not all members need the same intervention intensity
5. **Measure Continuously** - Prospective gap identification beats retrospective reporting

---

## About Summit Health Plan

Summit Health Plan is a regional Medicare Advantage plan serving 285,000 members across three Mountain West states. As a mission-driven organization, Summit is committed to improving the health of its members while maintaining affordability and access.

---

## Next Steps

Ready to improve your Star rating? Contact HDIM for:
- **Star Rating Assessment:** Gap analysis of current performance
- **Provider Network Analysis:** Data availability and integration readiness
- **ROI Modeling:** Calculate your quality bonus opportunity

**Contact:** payer-solutions@healthdata-in-motion.com
