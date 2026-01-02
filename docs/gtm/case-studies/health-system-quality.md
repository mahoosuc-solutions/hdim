# Case Study: Riverside Health System

## Achieving Quality Excellence Across a Multi-Hospital Network

---

## Executive Summary

**Organization:** Riverside Health System
**Size:** 4 hospitals, 85 ambulatory clinics, 2,200 physicians, 420,000 patients
**Location:** Southeast United States
**Challenge:** Quality measure fragmentation across acquired facilities

**Results After 12 Months:**
- **Unified quality platform** across all 89 facilities
- **23% improvement** in overall quality scores
- **$6.8M** in quality-based revenue improvement
- **85% reduction** in quality reporting manual effort

---

## The Challenge

### Post-Acquisition Integration Complexity

Riverside Health System had grown through acquisition, inheriting a patchwork of EHR systems and quality processes:

**Technology Fragmentation:**
- 4 different EHR platforms (Epic, Cerner, Meditech, CPSI)
- Separate quality databases at each legacy organization
- No unified patient matching across systems
- Inconsistent measure calculation methodologies

**Operational Inefficiency:**
- 14 FTE dedicated to quality reporting across system
- 6-week lag in quality performance visibility
- Manual reconciliation consuming 200+ hours/month
- Duplicate efforts across facilities

**Regulatory Pressure:**
- CMS Hospital Quality Reporting requirements
- Value-Based Purchasing penalties at risk
- Joint Commission accreditation concerns
- Payer quality contract obligations

**Leadership Frustration:**
- No system-wide quality dashboard
- Unable to benchmark facilities against each other
- Quality improvement initiatives siloed
- Board asking for unified quality metrics

---

## The Solution

### Enterprise Quality Platform

Riverside deployed HDIM as their enterprise quality management platform:

**Unified Data Layer:**
- Single FHIR-based repository for all patient data
- Master patient index with cross-facility matching
- Real-time data feeds from all 4 EHR systems
- Standardized quality measure calculation

**Quality Automation:**
- 52 HEDIS measures + hospital quality measures
- CMS Hospital Compare metrics
- Value-Based Purchasing measures
- Payer-specific quality requirements

**Facility Benchmarking:**
- System-wide quality dashboards
- Facility-to-facility comparisons
- Provider-level performance tracking
- Real-time quality alerts

---

## Implementation Approach

### Phased Rollout

| Phase | Duration | Scope |
|-------|----------|-------|
| Phase 1 | Months 1-3 | Flagship hospital (Epic) |
| Phase 2 | Months 4-6 | Two community hospitals (Cerner, Meditech) |
| Phase 3 | Months 7-9 | Critical access hospital (CPSI) + ambulatory |
| Phase 4 | Months 10-12 | Optimization and advanced analytics |

### Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Riverside HDIM Platform                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Riverside  │  │  Community  │  │  Community  │         │
│  │   Medical   │  │  Hospital   │  │  Hospital   │         │
│  │   Center    │  │    East     │  │    West     │         │
│  │   (Epic)    │  │  (Cerner)   │  │ (Meditech)  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                                                             │
│  ┌─────────────┐  ┌─────────────────────────────────────┐  │
│  │   Valley    │  │         85 Ambulatory Clinics       │  │
│  │   CAH       │  │    (Mixed: Epic, Athena, NextGen)   │  │
│  │   (CPSI)    │  │                                     │  │
│  └─────────────┘  └─────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          Unified Quality & Analytics Layer          │   │
│  │   HEDIS | Hospital Quality | VBP | Payer Contracts  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Results

### Quality Performance Improvement

| Measure Domain | Baseline | Year 1 | Improvement |
|----------------|----------|--------|-------------|
| Hospital Readmissions | 18.2% | 14.1% | -4.1 points |
| Patient Safety | 72% | 91% | +19 points |
| Clinical Process | 78% | 94% | +16 points |
| Patient Experience | 68% | 79% | +11 points |
| **Overall Quality Composite** | **71%** | **87%** | **+23%** |

### Hospital Compare Star Ratings

| Facility | Before | After | Change |
|----------|--------|-------|--------|
| Riverside Medical Center | 3 Stars | 4 Stars | +1 Star |
| Community Hospital East | 2 Stars | 3 Stars | +1 Star |
| Community Hospital West | 3 Stars | 4 Stars | +1 Star |
| Valley CAH | 2 Stars | 3 Stars | +1 Star |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| VBP Bonus Earned (vs. penalty avoided) | $3,200,000 |
| Payer Quality Contract Bonuses | $2,100,000 |
| Readmission Penalty Reduction | $890,000 |
| Administrative Efficiency Savings | $610,000 |
| **Total Annual Financial Impact** | **$6,800,000** |

### Operational Efficiency

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Quality Reporting FTE | 14 | 2.5 | 82% reduction |
| Report Generation Time | 6 weeks | Real-time | N/A |
| Manual Reconciliation Hours | 200/month | 30/month | 85% reduction |
| Quality Data Accuracy | 78% | 97% | +19 points |

---

## Facility-Level Outcomes

### Riverside Medical Center (Flagship)
- Achieved Leapfrog "A" safety grade
- Top quartile in CMS Hospital Compare
- 22% reduction in hospital-acquired infections

### Community Hospitals
- Both facilities moved from penalty to bonus in VBP
- Combined $1.4M improvement in quality payments
- Standardized best practices from flagship

### Valley Critical Access Hospital
- First-time quality reporting automation
- Met CMS quality requirements with minimal staff
- Benchmark data enabled targeted improvements

### Ambulatory Clinics
- HEDIS scores improved from 68% to 89%
- Care gap closure rates doubled
- Provider dashboards drove engagement

---

## Stakeholder Testimonials

### CMO Perspective
"For the first time, I can see quality performance across our entire system on a single dashboard. We went from arguing about whose data was right to actually improving care."
— *Dr. Patricia Morrison, Chief Medical Officer*

### CFO Perspective
"We were at risk of $2M in VBP penalties. HDIM helped us flip that to a $3.2M bonus. The ROI was immediate and substantial."
— *Robert Chen, Chief Financial Officer*

### Quality Director Perspective
"I used to manage 14 people doing manual reporting. Now I have 2.5 FTE focused on quality improvement instead of data collection. We're doing the work that actually matters."
— *Jennifer Walsh, VP Quality*

### Frontline Physician
"The provider dashboards show me exactly where my patients have care gaps. I can close them during visits instead of chasing patients later."
— *Dr. Michael Torres, Primary Care*

---

## Technology Capabilities Used

| Capability | Application |
|------------|-------------|
| FHIR Integration | Unified data from 4 EHR platforms |
| CQL Engine | Standardized measure calculation |
| Risk Stratification | High-risk patient identification |
| Real-time Dashboards | System/facility/provider views |
| Care Gap Alerts | Automated workflow triggers |
| Benchmarking | Facility and provider comparisons |
| Audit Logging | Compliance and accreditation support |

---

## ROI Analysis

### Investment
| Item | Cost |
|------|------|
| HDIM Enterprise Platform | $120,000/year |
| Implementation | $50,000 (one-time) |
| Training | $15,000 (one-time) |
| **Year 1 Total** | **$185,000** |

### Return
| Item | Value |
|------|-------|
| Quality-Based Revenue | $6,190,000 |
| Administrative Savings | $610,000 |
| **Total Year 1 Return** | **$6,800,000** |

### Metrics
- **ROI:** 3,576%
- **Payback Period:** 10 days
- **5-Year NPV:** $32,400,000

---

## Lessons Learned

1. **Executive Sponsorship Critical** - CEO and CMO alignment drove adoption
2. **Start with Flagship** - Prove value before expanding
3. **Standardize Measures First** - Governance before technology
4. **Provider Engagement Key** - Dashboards for physicians drove improvement
5. **Celebrate Quick Wins** - Early results built momentum

---

## Future Plans

Riverside is expanding HDIM usage to include:
- Predictive analytics for readmission risk
- Mental health screening integration
- Prior authorization automation
- Population health management for employed physician practices

---

## About Riverside Health System

Riverside Health System is a regional healthcare network serving communities across the Southeast. With 4 hospitals, 85 ambulatory clinics, and 2,200 physicians, Riverside is committed to delivering high-quality, patient-centered care while achieving operational excellence.

---

## Next Steps

Ready to unify quality across your health system? Contact HDIM for:
- **Executive Briefing:** ROI analysis for your organization
- **Technical Assessment:** EHR integration planning
- **Pilot Program:** Start with one facility

**Contact:** health-systems@healthdata-in-motion.com
