# Case Study: Mountain States Health Information Exchange

## Building a Regional Quality Reporting Infrastructure

---

## Executive Summary

**Organization:** Mountain States Health Information Exchange (MSHIE)
**Size:** 12 health systems, 340 clinics, 2.8 million patients
**Location:** Rocky Mountain Region (4 states)
**Use Case:** Regional quality reporting and population health infrastructure

**Results After 18 Months:**
- **340 clinics** connected to unified quality platform
- **2.8 million patients** with real-time quality tracking
- **$4.2M** in member organization quality bonuses
- **78%** of clinics achieving quality targets (up from 34%)

---

## The Challenge

### Fragmented Regional Infrastructure

Mountain States HIE served as the data backbone for 12 health systems across 4 states, but struggled to provide meaningful quality analytics:

**Technical Complexity:**
- 23 different EHR systems across member organizations
- No standardized quality measure calculation
- Legacy HL7 v2 infrastructure limiting interoperability
- Each health system calculating measures differently

**Governance Issues:**
- No agreed-upon measure definitions
- Inconsistent data quality across organizations
- Concerns about data sharing and competition
- Lack of benchmarking capabilities

**Value Proposition Gap:**
- HIE seen as "plumbing" not strategic asset
- Member organizations questioning ROI
- Competitors (regional ACOs) building own infrastructure
- Sustainability concerns

---

## The Solution

### HDIM as Quality Infrastructure Layer

MSHIE deployed HDIM as a shared services platform for regional quality reporting:

**Architecture:**
- Multi-tenant deployment with organization-level isolation
- FHIR-native integration replacing legacy HL7 feeds
- Centralized quality engine with standardized measures
- Organization-specific dashboards with regional benchmarking

**Phase 1: Infrastructure (Months 1-3)**
- Deployed HDIM enterprise platform
- Established FHIR connections to top 5 health systems
- Configured 52 HEDIS measures with CQL definitions

**Phase 2: Expansion (Months 4-8)**
- Connected remaining 7 health systems
- Onboarded 340 clinics to platform
- Deployed care gap workflows

**Phase 3: Value-Add Services (Months 9-18)**
- Launched regional benchmarking dashboards
- Implemented mental health screening infrastructure
- Added risk stratification services

---

## Multi-Tenant Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    MSHIE HDIM Platform                  │
├─────────────────────────────────────────────────────────┤
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │ Health   │ │ Health   │ │ Health   │ │ Health   │   │
│  │ System A │ │ System B │ │ System C │ │ System D │   │
│  │ (Epic)   │ │ (Cerner) │ │ (Meditech│ │ (Athena) │   │
│  │ 45 sites │ │ 62 sites │ │ 28 sites │ │ 89 sites │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
│                        ...                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │           Regional Benchmarking Layer            │  │
│  │    (De-identified, aggregated comparisons)       │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Implementation Timeline

| Month | Milestone |
|-------|-----------|
| 1 | Platform deployment, governance framework |
| 2 | Health System A (Epic) integration |
| 3 | Health Systems B, C (Cerner, Meditech) integration |
| 4-5 | Remaining health systems connected |
| 6-8 | All 340 clinics onboarded |
| 9-12 | Value-add services launched |
| 13-18 | Full optimization and expansion |

---

## Results

### Connectivity Achievement

| Metric | Before HDIM | After HDIM |
|--------|-------------|------------|
| Connected Organizations | 12 (HL7 only) | 12 (FHIR + HL7) |
| Connected Clinics | 0 (quality) | 340 |
| Patients with Quality Data | 0 | 2.8 million |
| EHR Systems Integrated | 8 | 23 |

### Quality Performance

| Metric | Baseline | Year 1 | Improvement |
|--------|----------|--------|-------------|
| Clinics Meeting Quality Targets | 34% | 78% | +44 points |
| Average Quality Score | 62% | 84% | +22 points |
| Mental Health Measure Compliance | 28% | 71% | +43 points |
| Care Gap Closure Rate | 23% | 58% | +35 points |

### Financial Impact (Across All Members)

| Metric | Value |
|--------|-------|
| Additional Quality Bonuses Earned | $4,200,000 |
| Shared Savings (ACO participants) | $8,700,000 |
| Administrative Cost Reduction | $1,200,000/year |
| Avoided Duplicate Infrastructure | $3,500,000 |
| **Total Regional Value** | **$17,600,000** |

---

## Member Organization Perspectives

### Large Health System (Epic)
"We were building our own population health infrastructure. MSHIE's HDIM platform gave us enterprise-grade capabilities at a fraction of the cost - and we can benchmark against the region."
— *CIO, Regional Medical Center*

### Community Health Center
"As a small FQHC, we could never afford this technology on our own. The shared services model gives us the same quality tools as the big health systems."
— *CEO, Valley Community Health*

### Rural Critical Access Hospital
"We have one IT person. MSHIE handles all the quality infrastructure - we just use the dashboards. It's transformed our reporting."
— *Administrator, Mountain View CAH*

---

## Governance Model

### Data Sharing Framework

| Level | Access | Use Case |
|-------|--------|----------|
| Organization | Full PHI access | Quality improvement, care coordination |
| Regional | De-identified aggregates | Benchmarking, trend analysis |
| Public Health | Aggregate reporting | Population health surveillance |

### Quality Measure Governance

- **Technical Committee:** Validates measure definitions (CQL)
- **Clinical Committee:** Approves measure selection
- **Executive Committee:** Oversees strategic direction
- **HDIM Role:** Platform management, technical support

---

## Technology Highlights

### FHIR-Native Integration
- Replaced 80% of legacy HL7 feeds with FHIR R4
- Real-time data vs. batch processing
- Standardized data model across all organizations

### Multi-Tenant Security
- Organization-level data isolation
- Role-based access control
- HIPAA-compliant audit logging
- SOC 2 Type II certified infrastructure

### Scalability
- 2.8 million patient records
- 340 concurrent clinic connections
- Sub-500ms query response times
- 99.9% platform uptime

---

## ROI Analysis

### MSHIE Investment
| Item | Annual Cost |
|------|-------------|
| HDIM Enterprise Platform | $150,000/year |
| Implementation (amortized) | $25,000/year |
| Support & Training | $15,000/year |
| **Total Annual Cost** | **$190,000** |

### Cost Per Member Organization
- 12 health systems sharing costs
- **$15,833/year per organization**
- Equivalent to 0.5 FTE quality analyst

### Regional Return
| Value Category | Annual Value |
|----------------|--------------|
| Quality Bonuses | $4,200,000 |
| Shared Savings | $8,700,000 |
| Admin Savings | $1,200,000 |
| **Total Annual Return** | **$14,100,000** |

### ROI: **7,421%**

---

## Lessons Learned

1. **Governance First** - Establish data sharing agreements before technology
2. **Start with Willing Partners** - Pilot with engaged organizations
3. **Demonstrate Quick Wins** - Regional benchmarking drove adoption
4. **Invest in Training** - Each organization needs local champions
5. **Build for Scale** - Multi-tenant architecture enabled rapid expansion

---

## Sustainability Model

MSHIE transitioned HDIM from grant-funded pilot to sustainable shared service:

| Revenue Source | Contribution |
|----------------|--------------|
| Member Dues (per patient) | 60% |
| Quality Bonus Sharing | 25% |
| Grant Funding | 10% |
| Value-Add Services | 5% |

---

## About Mountain States HIE

Mountain States Health Information Exchange is a nonprofit organization facilitating health information exchange across 4 states in the Rocky Mountain region. Serving 12 health systems and 340 clinics, MSHIE is committed to improving regional health outcomes through data sharing and quality improvement infrastructure.

---

## Next Steps

Interested in regional quality infrastructure? Contact HDIM for:
- **HIE Assessment:** Evaluate your readiness for quality services
- **Architecture Review:** Design multi-tenant deployment
- **Pilot Program:** Start with core member organizations

**Contact:** hie-partnerships@healthdata-in-motion.com
