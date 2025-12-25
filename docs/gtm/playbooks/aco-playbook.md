# ACO Success Playbook

## Your Complete Guide to Value-Based Care Excellence

---

## Executive Summary

This playbook provides a comprehensive strategy for Accountable Care Organizations (ACOs) to maximize shared savings through quality improvement, cost management, and operational excellence using the HDIM platform.

**Target Outcomes:**
- 20-40% improvement in quality scores
- $2-5M+ in annual shared savings
- 90-day implementation timeline
- 3,000-7,000% ROI in year one

---

## Part 1: Understanding the ACO Landscape

### ACO Models Overview

| Model | Risk Level | Quality Weight | Key Metrics |
|-------|-----------|----------------|-------------|
| MSSP Basic | One-sided (no downside) | 100% of savings at 50%+ quality | Quality score, TCOC |
| MSSP Enhanced | Two-sided (up to 40% loss) | Variable by track | Quality score, TCOC, risk adjustment |
| ACO REACH | Two-sided (100% risk) | Quality impacts savings rate | Quality, TCOC, risk adjustment |
| Commercial ACOs | Varies by contract | Varies | Contract-specific |

### Quality Scoring Mechanics

**MSSP Quality Score Calculation:**

Your quality score determines how much of your shared savings you keep:

| Quality Score Percentile | Savings Sharing Rate |
|-------------------------|---------------------|
| < 30th percentile | 0% (no shared savings) |
| 30th - 40th percentile | 25% |
| 40th - 50th percentile | 50% |
| 50th - 60th percentile | 70% |
| 60th - 70th percentile | 80% |
| 70th - 80th percentile | 90% |
| > 80th percentile | 100% |

**Example Impact:**
- ACO generates $3M in gross savings
- Quality score at 45th percentile = $1.5M shared savings
- Quality score at 85th percentile = $3M shared savings
- **Difference: $1.5M from quality improvement alone**

### 2025 ACO Quality Measures

**Domain 1: Patient/Caregiver Experience (25%)**
- CAHPS for ACOs survey measures
- Getting timely care
- Provider communication
- Care coordination
- Access to specialists

**Domain 2: Care Coordination/Patient Safety (25%)**
- All-cause unplanned admissions for patients with multiple chronic conditions
- Depression screening and follow-up
- Screening for falls risk

**Domain 3: Preventive Health (25%)**
- Breast cancer screening
- Colorectal cancer screening
- Influenza immunization
- Tobacco use screening and cessation

**Domain 4: At-Risk Population (25%)**
- Controlling high blood pressure
- Diabetes: HbA1c poor control
- Depression remission at 12 months
- Statin therapy for cardiovascular disease

---

## Part 2: The HDIM ACO Strategy

### Strategic Framework

```
┌─────────────────────────────────────────────────────────────┐
│                    ACO SUCCESS FRAMEWORK                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│   │   IDENTIFY   │ →  │   ENGAGE     │ →  │   IMPROVE    │ │
│   │  High-Risk   │    │  Care Teams  │    │  Outcomes    │ │
│   │  Patients    │    │  & Patients  │    │  & Quality   │ │
│   └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│   │ Risk Strat   │    │ Care Gaps    │    │ Quality      │ │
│   │ Attribution  │    │ Outreach     │    │ Dashboards   │ │
│   │ Cost Predict │    │ Workflows    │    │ Benchmarks   │ │
│   └──────────────┘    └──────────────┘    └──────────────┘ │
│                                                              │
│                    ┌──────────────────┐                     │
│                    │     MEASURE      │                     │
│                    │   ROI & Impact   │                     │
│                    └──────────────────┘                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Key Platform Capabilities

| Capability | ACO Application | Impact |
|------------|-----------------|--------|
| Multi-EHR Integration | Unified view across all practices | Complete patient picture |
| Risk Stratification | Identify high-cost patients before crisis | Proactive care management |
| Quality Dashboards | Real-time quality score tracking | Data-driven improvement |
| Care Gap Management | Patient-level gap identification | Targeted interventions |
| Provider Scorecards | Individual performance visibility | Accountability and improvement |
| CQL Measure Engine | Accurate, CMS-aligned calculations | Reliable quality reporting |

---

## Part 3: Implementation Roadmap

### Phase 1: Foundation (Days 1-30)

**Week 1-2: Technical Setup**
- [ ] Deploy HDIM platform (cloud provisioning)
- [ ] Configure FHIR connections to all EHR systems
- [ ] Establish data refresh schedules
- [ ] Set up user accounts and roles

**Week 3-4: Data Integration**
- [ ] Load attribution file from CMS/payer
- [ ] Import historical claims data (if available)
- [ ] Validate patient matching across sources
- [ ] Establish data quality baseline

**Deliverables:**
- All EHRs connected and flowing data
- Attribution list loaded and reconciled
- Baseline quality scores calculated
- Data quality report

### Phase 2: Quality Activation (Days 31-60)

**Week 5-6: Quality Configuration**
- [ ] Configure 2025 ACO quality measures
- [ ] Set quality score targets by measure
- [ ] Create quality dashboards (exec, provider, care team)
- [ ] Identify priority measures for improvement

**Week 7-8: Care Gap Workflows**
- [ ] Generate care gap lists by measure
- [ ] Configure outreach workflows
- [ ] Assign care coordinators to high-priority patients
- [ ] Launch initial outreach campaigns

**Deliverables:**
- Quality dashboards live
- Care gap lists distributed to practices
- Outreach workflows active
- Provider scorecards accessible

### Phase 3: Operations (Days 61-90)

**Week 9-10: Care Management Integration**
- [ ] Deploy risk stratification models
- [ ] Identify Tier 1 (complex care) patients
- [ ] Establish care management protocols
- [ ] Integrate with existing care management tools

**Week 11-12: Go-Live and Training**
- [ ] Complete user training (all roles)
- [ ] Establish weekly quality review cadence
- [ ] Document workflows and playbooks
- [ ] Transition to operational mode

**Deliverables:**
- Full platform operational
- All users trained
- Weekly quality reviews scheduled
- Success metrics tracking

---

## Part 4: Quality Improvement Tactics

### Measure-Specific Strategies

#### Controlling High Blood Pressure (CBP)

**Current State Assessment:**
- What's your baseline rate?
- How many patients with uncontrolled BP?
- Where are documentation gaps?

**Quick Wins (Week 1-4):**
- [ ] Audit recent visits for undocumented BP readings
- [ ] Ensure BP documented in structured fields
- [ ] Review patients with stale readings

**Medium-Term (Month 2-3):**
- [ ] Launch home BP monitoring program
- [ ] Implement pharmacist BP management
- [ ] Deploy automated patient reminders

**Targets:**
| Timeframe | Target Rate |
|-----------|-------------|
| Baseline | _____% |
| 90 days | +5-8 points |
| 6 months | +10-15 points |
| 12 months | 75%+ |

#### Colorectal Cancer Screening (COL)

**Current State Assessment:**
- What's your baseline rate?
- How many patients due for screening?
- FIT vs. colonoscopy completion rates?

**Quick Wins (Week 1-4):**
- [ ] Identify patients with external colonoscopies not documented
- [ ] Aggregate HIE data for missing procedures
- [ ] Generate outreach list for patients 45-50 (new age range)

**Medium-Term (Month 2-3):**
- [ ] Launch mailed FIT kit program
- [ ] Implement colonoscopy navigation support
- [ ] Create direct-access colonoscopy scheduling

**Targets:**
| Timeframe | Target Rate |
|-----------|-------------|
| Baseline | _____% |
| 90 days | +8-12 points |
| 6 months | +15-20 points |
| 12 months | 70%+ |

#### Depression Screening and Follow-Up (DSF)

**Current State Assessment:**
- What's your screening rate?
- Are positive screens getting follow-up?
- Is follow-up documented correctly?

**Quick Wins (Week 1-4):**
- [ ] Deploy tablet-based PHQ-9 in waiting rooms
- [ ] Configure automatic scoring and EHR documentation
- [ ] Create alert for positive screens

**Medium-Term (Month 2-3):**
- [ ] Establish same-day behavioral health consultation
- [ ] Train PCPs on medication initiation
- [ ] Implement care coordinator follow-up workflow

**Targets:**
| Timeframe | Target Rate |
|-----------|-------------|
| Baseline | _____% |
| 90 days | +15-25 points |
| 6 months | +30-40 points |
| 12 months | 80%+ |

---

## Part 5: Risk Stratification & Care Management

### Risk Tier Definitions

| Tier | Definition | % of Population | Care Model |
|------|------------|-----------------|------------|
| Tier 1 | Complex/High-Risk | 1-2% | Intensive care management |
| Tier 2 | Rising Risk | 3-8% | Enhanced coordination |
| Tier 3 | Moderate Risk | 10-20% | PCP-led with support |
| Tier 4 | Low Risk | 70-85% | Wellness and prevention |

### Tier 1: Complex Care Management

**Patient Criteria:**
- 3+ chronic conditions
- 2+ ED visits or 1+ hospitalization in past 12 months
- Polypharmacy (10+ medications)
- High predicted cost (top 2%)
- Recent discharge with high readmission risk

**Care Model:**
- Dedicated care manager (1:50-1:80 ratio)
- Weekly or bi-weekly contact
- Home visits as needed
- Medication reconciliation
- Social determinant interventions

**Expected Impact:**
- 40-60% reduction in hospitalizations
- 30-50% reduction in ED visits
- 25-35% reduction in total cost of care

### Tier 2: Rising Risk

**Patient Criteria:**
- 2-3 chronic conditions
- 1-2 ED visits in past 12 months
- Moderate complexity medications
- Rising cost trajectory
- Care gaps in chronic disease management

**Care Model:**
- Care coordinator (1:150-1:200 ratio)
- Monthly outreach
- Focused care gap closure
- Transition care after hospitalizations
- Specialist coordination

**Expected Impact:**
- 20-30% reduction in hospitalizations
- Improved medication adherence
- Care gap closure acceleration

---

## Part 6: Provider Engagement

### The Provider Problem

Providers often don't know:
- Their quality scores
- Which patients have care gaps
- How they compare to peers
- What actions to take

### The HDIM Solution

**Provider Scorecards:**
```
┌─────────────────────────────────────────────────────────────┐
│           DR. SARAH CHEN - QUALITY SCORECARD                │
├─────────────────────────────────────────────────────────────┤
│ Overall Score: 87% (Top Quartile)     Panel Size: 1,247    │
├─────────────────────────────────────────────────────────────┤
│ MEASURE                    YOUR RATE    PEER AVG    TARGET  │
│ ─────────────────────────────────────────────────────────── │
│ Breast Cancer Screening      89%         82%         85%  ✓│
│ Colorectal Screening         76%         71%         75%  ✓│
│ Controlling BP               91%         78%         80%  ✓│
│ Diabetes HbA1c Control       83%         75%         80%  ✓│
│ Depression Screening         72%         68%         80%  !│
├─────────────────────────────────────────────────────────────┤
│ CARE GAPS TO CLOSE: 47 patients                             │
│ [View Patient List]                                         │
└─────────────────────────────────────────────────────────────┘
```

**Pre-Visit Preparation:**
- Care gaps displayed in EHR before each visit
- Prioritized action list for the appointment
- Historical trends visible

**Actionable Gap Lists:**
- Sortable by measure, priority, last visit
- Contact information included
- Task assignment to care team
- Completion tracking

### Provider Incentive Alignment

| Approach | Description | Effectiveness |
|----------|-------------|---------------|
| Transparency | Share scorecards monthly | Medium |
| Peer comparison | Show individual vs. peers | High |
| Recognition | Highlight top performers | Medium |
| Financial incentives | Bonus for quality achievement | Very High |
| Care team support | Provide resources to close gaps | High |

---

## Part 7: Operational Cadence

### Weekly Rhythm

**Monday: Quality Review**
- Review prior week's gap closures
- Identify new care gaps
- Prioritize outreach for the week
- Review hospital admissions

**Wednesday: Care Management Sync**
- High-risk patient updates
- Transition care reviews
- Care manager case reviews
- Escalation decisions

**Friday: Performance Check**
- Quality score trending
- Provider performance updates
- Utilization patterns
- Weekend discharge planning

### Monthly Rhythm

| Week | Focus |
|------|-------|
| Week 1 | Quality score deep dive, measure-by-measure analysis |
| Week 2 | Provider performance review, outlier identification |
| Week 3 | Care management effectiveness, tier transitions |
| Week 4 | Cost and utilization analysis, financial projections |

### Quarterly Rhythm

- Full performance review with executive team
- Quality strategy adjustment based on data
- Provider feedback sessions
- Care management capacity planning
- Financial forecast update

---

## Part 8: Measuring Success

### Key Performance Indicators

**Quality KPIs:**
| Metric | Baseline | 90-Day | 6-Month | 12-Month |
|--------|----------|--------|---------|----------|
| Overall Quality Score | _____% | +5-8 pts | +12-18 pts | +20-30 pts |
| Care Gap Closure Rate | _____% | 40%+ | 60%+ | 75%+ |
| Screening Rates (avg) | _____% | +10 pts | +18 pts | +25 pts |
| Depression Screening | _____% | +20 pts | +35 pts | +45 pts |

**Utilization KPIs:**
| Metric | Baseline | 6-Month | 12-Month |
|--------|----------|---------|----------|
| All-Cause Admissions | _____ per 1000 | -10% | -20% |
| ED Visits | _____ per 1000 | -8% | -15% |
| 30-Day Readmissions | _____% | -15% | -25% |
| Avoidable Admissions | _____ per 1000 | -20% | -35% |

**Financial KPIs:**
| Metric | Year 1 | Year 2 | Year 3 |
|--------|--------|--------|--------|
| Gross Shared Savings | $_______ | $_______ | $_______ |
| Quality Multiplier | ____% | ____% | ____% |
| Net Shared Savings | $_______ | $_______ | $_______ |
| HDIM Investment | $_______ | $_______ | $_______ |
| ROI | _______% | _______% | _______% |

### Success Story: Beacon ACO

| Metric | Before HDIM | After HDIM (Year 1) | Change |
|--------|-------------|---------------------|--------|
| Quality Score | 62% | 87% | +25 pts |
| Quality Percentile | 35th | 82nd | +47 percentiles |
| Shared Savings | $1.2M | $3.6M | +$2.4M |
| Savings Rate | 50% | 100% | +50% |
| Care Gap Closure | 45% | 78% | +33 pts |
| Depression Screening | 31% | 84% | +53 pts |

**Investment:** $48,000 (Year 1)
**Return:** $2.4M (additional savings)
**ROI:** 7,129%

---

## Part 9: Common Pitfalls

### Pitfall 1: Data Without Action

**Problem:** Beautiful dashboards, no improvement.

**Solution:**
- Every metric needs an owner
- Every gap needs an assigned action
- Every week needs progress review

### Pitfall 2: Ignoring Rising Risk

**Problem:** All resources on current high-cost patients.

**Solution:**
- Dedicate capacity to Tier 2
- Prevention is cheaper than crisis management
- Track tier transitions

### Pitfall 3: Provider Disengagement

**Problem:** Providers don't use quality tools.

**Solution:**
- Embed in existing workflow (EHR integration)
- Make data visible pre-visit
- Share peer comparisons monthly
- Celebrate successes

### Pitfall 4: Annual Mindset

**Problem:** "We'll focus on quality before year-end."

**Solution:**
- Quality is continuous, not seasonal
- Weekly cadence, not annual push
- Year-round improvement culture

---

## Part 10: Resources

### Case Studies
- [Beacon ACO: 90-Day Implementation](/case-studies/aco-success-story)
- [Mountain States HIE: Regional Quality](/case-studies/hie-implementation)

### Blog Posts
- [5 HEDIS Measures Every ACO Must Master](/blog/hedis-measures-guide)
- [Risk Stratification Guide](/blog/risk-stratification-guide)
- [FHIR-Native Architecture](/blog/fhir-native-architecture)

### Tools
- [ROI Calculator](https://roi.healthdata-in-motion.com)
- Demo Environment Guide

---

## Next Steps

1. **Assess your current state** - Calculate baseline quality scores and identify gaps
2. **Schedule a demo** - See HDIM in action with your organization type
3. **Plan your implementation** - Work with HDIM team on 90-day roadmap
4. **Launch and iterate** - Go live fast, improve continuously

---

*Ready to transform your ACO's quality performance? [Schedule a demo](#) to see how HDIM can help you capture your full shared savings potential.*
