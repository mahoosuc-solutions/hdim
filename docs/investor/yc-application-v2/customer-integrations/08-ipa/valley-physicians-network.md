# Valley Physicians Network - Independent Physician Association

> n8n hub architecture for an IPA with 85 independent practices and 15 different EHRs.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Independent Physician Association (IPA) |
| **Size** | 85 independent practices, 220 providers |
| **Patient Count** | 125,000 managed care lives |
| **Structure** | Contracted independent practices, capitated arrangements |
| **Location** | San Fernando Valley, CA |
| **EHR Systems** | 15 different EHRs (see breakdown) |
| **Quality Programs** | California IHA P4P, Commercial HMO, Medi-Cal Managed Care |
| **IT Capabilities** | Moderate (IPA has 3 IT staff) |
| **HDIM Tier** | Enterprise Plus |
| **Monthly Cost** | $2,499/month |

## Challenge

### Current State

Valley Physicians Network (VPN) is a large IPA that contracts with 85 independent practices in the San Fernando Valley. Unlike an ACO where practices might share an EHR, each practice in VPN chose their own system. The result: 15 different EHRs, from major platforms like Epic and athenahealth to smaller systems like Practice Fusion and CareCloud.

VPN receives capitated payments from 6 health plans. Their revenue depends on managing total cost of care and hitting quality targets. But without visibility into what's happening at each practice, they're flying blind. They rely on quarterly claims data that's 90+ days old.

Their IT team of 3 can't possibly maintain integrations with 15 EHRs. They've tried manual reporting (Excel hell), health plan portals (each plan has different data), and a legacy disease management vendor (couldn't handle the EHR diversity).

### Pain Points

- **EHR fragmentation:** 15 different EHRs, no common data model
- **Practice autonomy:** Practices won't change EHRs for the IPA
- **Claims lag:** 90+ days to see utilization data
- **Quality reporting:** Each health plan has different requirements
- **Practice variation:** Quality scores range from 35% to 92%
- **No care coordination:** Can't identify high-risk patients across network
- **Limited IT:** 3 staff for 85 practice relationships
- **Member engagement:** Don't know who their members are until claims arrive

### Why HDIM

VPN's Medical Director heard about HDIM's n8n integration approach at a California IPA conference. The idea of a "universal adapter" that could connect to any EHR—via FHIR, CSV, direct database, or file drops—was exactly what they needed. They didn't have to convince 85 practices to change anything.

A pilot with 10 practices proved the concept worked across 6 different EHRs. Full deployment followed.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                   VALLEY PHYSICIANS NETWORK                         │
│                   85 Practices, 15 EHR Types                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  TIER 1: FHIR-CAPABLE (45 practices)                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │athena(18)│ │ Epic (8) │ │Cerner(6) │ │eCW (13)  │              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
│       │            │            │            │                      │
│       └────────────┴────────────┴────────────┘                      │
│                         │ FHIR R4                                   │
│                         │                                           │
│  TIER 2: CSV EXPORT (28 practices)                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐              │
│  │DrChrono  │ │Practice  │ │ Kareo    │ │ Elation  │              │
│  │   (8)    │ │Fusion(7) │ │   (6)    │ │   (7)    │              │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘              │
│       │            │            │            │                      │
│       └────────────┴────────────┴────────────┘                      │
│                         │ CSV/Excel                                 │
│                         │                                           │
│  TIER 3: LEGACY/CUSTOM (12 practices)                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                           │
│  │CareCloud │ │GE Centri │ │  Paper + │                           │
│  │   (5)    │ │ city (4) │ │Billing(3)│                           │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘                           │
│       │            │            │                                   │
│       └────────────┴────────────┘                                   │
│                         │ Custom                                    │
└─────────────────────────┼───────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    HDIM n8n INTEGRATION HUB                         │
│                    (HDIM-Managed Cloud)                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                   n8n Workflow Engine                        │   │
│  │                                                              │   │
│  │  TIER 1 WORKFLOWS (FHIR)                                    │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │   │
│  │  │ athena   │ │  Epic    │ │  Cerner  │ │   eCW    │       │   │
│  │  │ FHIR     │ │  FHIR    │ │  FHIR    │ │  FHIR    │       │   │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘       │   │
│  │       │            │            │            │              │   │
│  │  TIER 2 WORKFLOWS (CSV)                                     │   │
│  │  ┌──────────────────────────────────────────────────────┐  │   │
│  │  │  Universal CSV Parser + FHIR Transform               │  │   │
│  │  │  • DrChrono template → FHIR                          │  │   │
│  │  │  • Practice Fusion template → FHIR                   │  │   │
│  │  │  • Kareo template → FHIR                             │  │   │
│  │  │  • Elation template → FHIR                           │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  │                                                              │   │
│  │  TIER 3 WORKFLOWS (Custom)                                  │   │
│  │  ┌──────────────────────────────────────────────────────┐  │   │
│  │  │  • CareCloud: API → FHIR                             │  │   │
│  │  │  • GE Centricity: Database export → FHIR             │  │   │
│  │  │  • Paper practices: Claims-only workflow             │  │   │
│  │  └──────────────────────────────────────────────────────┘  │   │
│  │                                                              │   │
│  │       └────────────┴────────────┴────────────┘              │   │
│  │                         │                                    │   │
│  └─────────────────────────┼────────────────────────────────────┘   │
│                            │                                        │
└────────────────────────────┼────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         HDIM Cloud                                  │
├─────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │               Patient Matching & Aggregation                  │  │
│  │  • Match across EHRs by MRN, name, DOB, health plan ID       │  │
│  │  • Deduplicate clinical records                               │  │
│  │  • Attribution from health plan rosters                       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                              │                                      │
│                     ┌────────▼────────┐                            │
│                     │   CQL Engine    │                            │
│                     │  (IHA + Custom) │                            │
│                     └────────┬────────┘                            │
│                              │                                      │
│        ┌─────────────────────┼─────────────────────┐               │
│        │                     │                     │                │
│  ┌─────▼─────┐        ┌──────▼──────┐       ┌─────▼─────┐         │
│  │   IPA     │        │  Practice   │       │  Member   │         │
│  │ Dashboard │        │ Scorecards  │       │ Registry  │         │
│  └───────────┘        └─────────────┘       └───────────┘         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### EHR Breakdown

| EHR | Practices | Patients | Integration Method | Status |
|-----|-----------|----------|-------------------|--------|
| athenahealth | 18 | 32,000 | FHIR R4 | Live |
| eClinicalWorks | 13 | 22,000 | FHIR R4 | Live |
| Epic | 8 | 18,000 | FHIR R4 | Live |
| DrChrono | 8 | 12,000 | CSV Upload | Live |
| Practice Fusion | 7 | 9,000 | CSV Upload | Live |
| Elation | 7 | 8,000 | CSV Upload | Live |
| Cerner | 6 | 10,000 | FHIR R4 | Live |
| Kareo | 6 | 6,000 | CSV Upload | Live |
| CareCloud | 5 | 4,000 | API + n8n | Live |
| GE Centricity | 4 | 3,000 | DB Export + n8n | Live |
| Paper/Billing Only | 3 | 1,000 | Claims only | Live |
| **TOTAL** | **85** | **125,000** | | |

### n8n Workflow Configuration

| Workflow | Practices | Frequency | Records/Day |
|----------|-----------|-----------|-------------|
| athena FHIR Sync | 18 | Daily 2 AM | ~8,000 |
| eCW FHIR Sync | 13 | Daily 2:30 AM | ~5,500 |
| Epic FHIR Sync | 8 | Daily 3 AM | ~4,500 |
| Cerner FHIR Sync | 6 | Daily 3:30 AM | ~2,500 |
| CSV Import (28 practices) | 28 | Weekly (Mon) | ~15,000 |
| CareCloud API Sync | 5 | Daily 4 AM | ~1,000 |
| GE Centricity DB Export | 4 | Weekly (Tue) | ~750 |
| Claims Processing | 85 | Daily | ~3,000 |

---

## Health Plan Integration

### Capitated Contracts

| Health Plan | Members | Cap Type | Quality Bonus Pool |
|-------------|---------|----------|-------------------|
| Health Net | 42,000 | Full Cap | $2.8M |
| Blue Shield CA | 28,000 | Shared Risk | $1.5M |
| Anthem | 22,000 | Full Cap | $1.2M |
| Cigna | 15,000 | Shared Savings | $800K |
| Aetna | 12,000 | Shared Risk | $600K |
| Medi-Cal MCO | 6,000 | Per-Visit | $200K |
| **TOTAL** | **125,000** | | **$7.1M** |

### Quality Measure Requirements

| Plan | Measure Set | Measures | Reporting |
|------|-------------|----------|-----------|
| Health Net | IHA P4P | 7 | Quarterly |
| Blue Shield | Custom | 12 | Monthly |
| Anthem | IHA P4P | 7 | Quarterly |
| Cigna | HEDIS subset | 10 | Quarterly |
| Aetna | Custom | 8 | Quarterly |
| Medi-Cal | State specs | 15 | Annual |

---

## Practice Performance Management

### Practice Scorecard Example

```
┌─────────────────────────────────────────────────────────────────────┐
│  PRACTICE PERFORMANCE SCORECARD                                     │
│  Valley Family Medicine (Practice #47)                              │
│  athenahealth | 2 providers | 1,800 assigned members                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  QUALITY MEASURES                          SCORE    RANK    TREND  │
│  ────────────────────────────────────────────────────────────────  │
│  Diabetes: HbA1c Control (<9%)             78%      22/85   ↑ +8   │
│  Hypertension: BP Control (<140/90)        72%      35/85   ↑ +5   │
│  Breast Cancer Screening                   82%      18/85   ↔  0   │
│  Colorectal Cancer Screening               68%      42/85   ↑ +12  │
│  Cervical Cancer Screening                 75%      28/85   ↑ +3   │
│  Child Immunization Status                 62%      55/85   ↓ -2   │
│  Well-Child Visits                         58%      62/85   ↑ +4   │
│  ────────────────────────────────────────────────────────────────  │
│  COMPOSITE SCORE                           71%      32/85   ↑ +5   │
│                                                                     │
│  QUALITY INCENTIVE PROJECTION                                       │
│  ├── Current trajectory:     $18,200 (of $28,000 possible)         │
│  ├── If all gaps closed:     $26,500                                │
│  └── Gap to target:          89 care gaps remaining                 │
│                                                                     │
│  TOP OPPORTUNITIES                                                  │
│  1. Colorectal screening (32 patients need FIT/colonoscopy)        │
│  2. Childhood immunizations (18 children behind schedule)           │
│  3. Well-child visits (22 children need visit)                      │
│                                                                     │
│  [View Patient List] [Export Gaps] [Schedule Outreach]             │
└─────────────────────────────────────────────────────────────────────┘
```

### Network-Wide Performance Distribution

```
                    Quality Score Distribution (85 Practices)

 Practices
    │
 20 ┤                                    ████████
    │                          ████████  ████████
 15 ┤                ████████  ████████  ████████
    │      ████████  ████████  ████████  ████████  ████████
 10 ┤      ████████  ████████  ████████  ████████  ████████
    │      ████████  ████████  ████████  ████████  ████████
  5 ┤████  ████████  ████████  ████████  ████████  ████████  ████
    │████  ████████  ████████  ████████  ████████  ████████  ████
  0 ┼────────────────────────────────────────────────────────────────
       <50%   50-60%   60-70%   70-80%   80-90%   >90%

    Mean: 72%  |  Median: 74%  |  Std Dev: 14%  |  Range: 35%-92%
```

---

## Implementation Timeline

### Phase 1: FHIR-Capable Practices (Month 1-2)

- [x] athenahealth (18 practices): OAuth authorization
- [x] eClinicalWorks (13 practices): FHIR configuration
- [x] Epic (8 practices): App Orchard authorization
- [x] Cerner (6 practices): CODE program setup
- [x] Initial sync: 82,000 patients

### Phase 2: CSV Practices (Month 2-3)

- [x] Develop EHR-specific CSV templates
- [x] Train practice staff on export procedures
- [x] Build n8n parsing workflows
- [x] Onboard 28 practices
- [x] Validate data quality

### Phase 3: Custom Integrations (Month 3-4)

- [x] CareCloud API integration
- [x] GE Centricity database connector
- [x] Paper practice claims workflow
- [x] Data validation and matching
- [x] Full network coverage achieved

### Phase 4: Health Plan Integration (Month 4)

- [x] Import member rosters from 6 plans
- [x] Configure plan-specific measure sets
- [x] Build quality bonus calculators
- [x] Train IPA staff on reporting

---

## Expected Outcomes

### Quality Score Improvement

| Metric | Baseline | 12-Month | Improvement |
|--------|----------|----------|-------------|
| Network average score | 62% | 78% | +16 pts |
| Score standard deviation | 18% | 10% | -8 pts |
| Practices <60% | 25 | 5 | -20 practices |
| Practices >80% | 12 | 35 | +23 practices |

### Quality Incentive Capture

| Health Plan | Baseline Capture | With HDIM | Improvement |
|-------------|------------------|-----------|-------------|
| Health Net | 62% ($1.74M) | 85% ($2.38M) | +$640K |
| Blue Shield | 58% ($870K) | 82% ($1.23M) | +$360K |
| Anthem | 65% ($780K) | 88% ($1.06M) | +$280K |
| Cigna | 60% ($480K) | 80% ($640K) | +$160K |
| Aetna | 55% ($330K) | 78% ($468K) | +$138K |
| Medi-Cal | 50% ($100K) | 72% ($144K) | +$44K |
| **TOTAL** | **$4.30M** | **$5.92M** | **+$1.62M** |

### Time Savings

| Task | Before HDIM | After HDIM | Savings |
|------|-------------|------------|---------|
| Practice data collection | 160 hrs/month | 10 hrs/month | 150 hrs/month |
| Quality report generation | 80 hrs/quarter | 8 hrs/quarter | 72 hrs/quarter |
| Plan roster reconciliation | 40 hrs/month | 4 hrs/month | 36 hrs/month |
| Practice outreach | 60 hrs/month | 20 hrs/month | 40 hrs/month |
| **Total** | **296 hrs/month** | **54 hrs/month** | **242 hrs/month** |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **Additional quality incentives** | $1,620,000 |
| **Avoided utilization (care gaps)** | $480,000 |
| **Staff time savings** | $145,200 (242 hrs × $50 × 12) |
| **Avoided FTE hire** | $75,000 |
| **Total Annual Value** | **$2,320,200** |

### ROI Calculation

```
Annual Value:        $2,320,200
Annual HDIM Cost:    $29,988 ($2,499 × 12)
n8n Workflow Setup: $15,000 (one-time, Year 1)
Year 1 Total Cost:  $44,988
Net Year 1 Benefit: $2,275,212
ROI:                51.6x
Payback Period:     7 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| Enterprise Plus tier | $2,499/month |
| n8n workflow development (15 EHRs) | $15,000 one-time |
| Ongoing workflow maintenance | Included |
| 85 practice dashboards | Included |
| 6 health plan configurations | Included |
| **Year 1 Total** | **$44,988** |
| **Year 2+ Annual** | **$29,988** |

*Cost per managed life: $0.02/member/month*

---

## Success Metrics

| Metric | Target | Actual (Month 6) |
|--------|--------|------------------|
| Practice data coverage | >95% | 97% (82/85 with clinical data) |
| Data freshness (FHIR) | <48 hours | 24 hours avg |
| Data freshness (CSV) | <14 days | 7 days avg |
| Quality incentive capture | >80% | 78% (on track) |
| Practice score variation | <12% SD | 11% SD |
| Practice adoption | 100% | 100% |

---

## Medical Director Testimonial

> "Managing 85 independent practices with 15 different EHRs was a nightmare. We tried everything—spreadsheets, health plan portals, even a consultant who charged us $200K to tell us we needed a data warehouse. HDIM did what we thought was impossible: they connected all 85 practices, including the 3 that still use paper charts, into a single quality dashboard. Now I can see which practices are struggling and where. Last quarter, we identified 10 practices that were below 60%. We did targeted outreach, helped them close gaps, and 8 of them are now above 70%. Our Health Net quality bonus went from 62% capture to 85%. That's an extra $640K just from one plan. The n8n approach meant we didn't have to ask any practice to change their EHR."
>
> — Dr. Richard Yamamoto, Medical Director, Valley Physicians Network

---

## Related Resources

- [n8n Workflows](../_shared/N8N_WORKFLOWS.md)
- [CSV Templates](../_shared/CSV_TEMPLATES.md)
- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [Measure Sets](../_shared/MEASURE_SETS.md)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)

---

*Last Updated: December 2025*
*Version: 1.0*
