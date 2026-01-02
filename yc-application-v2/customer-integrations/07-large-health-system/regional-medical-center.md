# Regional Medical Center - Large Health System

> Private cloud deployment with dedicated infrastructure for an enterprise health system.

## Profile

| Attribute | Value |
|-----------|-------|
| **Organization Type** | Integrated Health System |
| **Size** | 3 hospitals, 120 clinics, 800+ providers, 180,000 patients |
| **Structure** | Academic medical center + community hospitals + employed physicians |
| **Location** | Pacific Northwest (multi-state) |
| **EHR System** | Epic (enterprise-wide) |
| **Quality Programs** | Medicare Stars, ACO REACH, Commercial VBC (12+ contracts), Leapfrog |
| **IT Capabilities** | Advanced (150+ IT staff, dedicated data warehouse team) |
| **HDIM Tier** | Health System (Custom) |
| **Monthly Cost** | ~$15,000/month (negotiated enterprise agreement) |

## Challenge

### Current State

Regional Medical Center (RMC) is a large integrated delivery network that has made significant investments in population health. They have an Epic data warehouse, a BI team of 12, and an analytics platform that cost $2M to implement. They're in the top quartile on most quality measures.

Their challenge is threefold:
1. **Speed:** Their data warehouse has a 48-72 hour latency. In an era of real-time care, that's too slow.
2. **Scale:** They have 12 different value-based care contracts, each with different measure specifications. Customizing reports for each payer is a nightmare.
3. **Data residency:** As an academic medical center with NIH grants, they have strict requirements about where PHI can reside. Third-party cloud solutions are scrutinized heavily.

They've evaluated enterprise population health vendors (Arcadia, Health Catalyst, Innovaccer) and found them to be expensive ($500K+/year), slow to implement (18+ months), and inflexible about deployment options.

### Pain Points

- **Data latency:** 48-72 hours from event to insight
- **Payer complexity:** 12 VBC contracts with different specifications
- **Data residency:** Compliance requirements limit cloud options
- **Cost:** Enterprise vendors quoted $500K-$1.2M/year
- **Implementation time:** Vendors estimate 18-24 months
- **Customization:** Need ability to create custom measures for research
- **SSO:** Must integrate with institutional identity provider

### Why HDIM

RMC's CMIO was intrigued by HDIM's architecture after reading a white paper on real-time CQL evaluation. The private cloud deployment option addressed their data residency concerns. The pricing ($15K/month vs. $500K+/year from competitors) made the CFO happy.

They ran a 6-month pilot in one hospital's primary care network. The results exceeded expectations: real-time gap closure, 12-point improvement in diabetes control, and $400K in avoided readmissions.

---

## Solution Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│              REGIONAL MEDICAL CENTER PRIVATE CLOUD                  │
│                    (Customer Azure Tenant)                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌───────────────────────────────────────────────────────────────┐ │
│  │                    Epic Enterprise                             │ │
│  │                                                                │ │
│  │  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐       │ │
│  │  │Hospital │   │Hospital │   │Hospital │   │  120    │       │ │
│  │  │   A     │   │   B     │   │   C     │   │ Clinics │       │ │
│  │  └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘       │ │
│  │       │             │             │             │             │ │
│  │       └─────────────┼─────────────┼─────────────┘             │ │
│  │                     │             │                           │ │
│  │              ┌──────▼─────────────▼──────┐                   │ │
│  │              │     Epic Interconnect      │                   │ │
│  │              │   (FHIR R4 + Bulk Export) │                   │ │
│  │              └─────────────┬─────────────┘                   │ │
│  └────────────────────────────┼──────────────────────────────────┘ │
│                               │                                     │
│                      Internal Network                               │
│                               │                                     │
│  ┌────────────────────────────▼──────────────────────────────────┐ │
│  │                  HDIM Private Cloud                            │ │
│  │              (Customer Azure Subscription)                     │ │
│  │                                                                │ │
│  │  ┌────────────────────────────────────────────────────────┐  │ │
│  │  │                 Kubernetes Cluster                      │  │ │
│  │  │                                                         │  │ │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │  │ │
│  │  │  │ FHIR        │  │ CQL Engine  │  │ API Gateway │    │  │ │
│  │  │  │ Connector   │  │ (3 replicas)│  │ + Auth      │    │  │ │
│  │  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘    │  │ │
│  │  │         │                │                │            │  │ │
│  │  │  ┌──────▼────────────────▼────────────────▼──────┐    │  │ │
│  │  │  │              PostgreSQL (HA)                   │    │  │ │
│  │  │  │           + Azure Blob Storage                 │    │  │ │
│  │  │  └────────────────────────────────────────────────┘    │  │ │
│  │  └─────────────────────────────────────────────────────────┘  │ │
│  │                                                                │ │
│  │  ┌────────────────────────────────────────────────────────┐  │ │
│  │  │                 Security Controls                       │  │ │
│  │  │  • Customer-managed encryption keys                     │  │ │
│  │  │  • SAML SSO (Azure AD)                                  │  │ │
│  │  │  • Private endpoints (no public internet)               │  │ │
│  │  │  • Customer audit log retention                         │  │ │
│  │  │  • HIPAA BAA with HDIM (software) + Azure (infra)       │  │ │
│  │  └────────────────────────────────────────────────────────┘  │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────────┐│
│  │                    Integration Layer                           ││
│  │  • Real-time alerts → Epic In-Basket                          ││
│  │  • Discharge alerts → Care Management System                   ││
│  │  • Quality reports → Tableau (existing)                        ││
│  │  • Custom measures → Research Data Warehouse                   ││
│  └────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────┘
```

---

## Integration Configuration

### Deployment: Private Cloud (Customer Azure)

| Component | Specification |
|-----------|---------------|
| **Cloud Provider** | Microsoft Azure (Customer tenant) |
| **Region** | West US 2 (customer preference) |
| **Compute** | Azure Kubernetes Service (AKS) |
| **Database** | Azure PostgreSQL Flexible Server (HA) |
| **Storage** | Azure Blob Storage (encrypted) |
| **Networking** | Private endpoints, no public IPs |
| **Identity** | Azure AD SAML SSO |
| **Encryption** | Customer-managed keys (Azure Key Vault) |

### Epic Integration

| Method | Use Case | Frequency |
|--------|----------|-----------|
| FHIR R4 API | Real-time patient data | On-demand |
| Bulk FHIR Export | Population sync | Nightly |
| SMART on FHIR | Point-of-care alerts | Real-time |
| Webhooks (ADT) | Admission/Discharge | Real-time |
| Interconnect | Enterprise data access | As needed |

### Data Volume

| Resource | Daily New/Updated | Total |
|----------|-------------------|-------|
| Patient | 2,000 | 180,000 |
| Condition | 8,000 | 2.1M |
| MedicationRequest | 12,000 | 3.8M |
| Observation | 45,000 | 15M |
| Encounter | 3,500 | 4.2M |
| Immunization | 1,500 | 850K |

---

## Multi-Payer Configuration

### Value-Based Contracts Supported

| Payer | Contract Type | Lives | Measures | Custom Rules |
|-------|---------------|-------|----------|--------------|
| Medicare FFS | ACO REACH | 35,000 | CMS ACO Set | Standard |
| Medicare Advantage | Stars | 28,000 | Stars v2025 | Star ratings |
| Aetna | VBC Collaborative | 22,000 | Custom | 3 custom |
| Blue Cross | Total Cost | 18,000 | Custom | 5 custom |
| Cigna | ACO Arrangement | 15,000 | Custom | 2 custom |
| United Healthcare | P4P | 12,000 | Standard | Standard |
| Humana | MA Stars | 8,000 | Stars | Star ratings |
| Premera | Quality Bonus | 10,000 | Custom | 4 custom |
| Regence | VBC 2.0 | 8,000 | Custom | 2 custom |
| Medicaid MCO 1 | Quality Withhold | 12,000 | State specs | 3 custom |
| Medicaid MCO 2 | Quality Withhold | 8,000 | State specs | 2 custom |
| Self-Insured | Employer Coalition | 4,000 | Custom | 6 custom |

### Payer-Specific Dashboard View

```
┌─────────────────────────────────────────────────────────────────────┐
│  PAYER PERFORMANCE SUMMARY                                          │
│  Regional Medical Center | Q4 2024                                  │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  CONTRACT        │ LIVES  │ QUALITY │ COST    │ SAVINGS  │ STATUS │
│  ────────────────┼────────┼─────────┼─────────┼──────────┼─────── │
│  Medicare REACH  │ 35,000 │ 88%     │ -2.3%   │ $4.2M    │ ✓      │
│  Medicare Stars  │ 28,000 │ 4.2★    │ N/A     │ $1.8M    │ ✓      │
│  Aetna VBC       │ 22,000 │ 82%     │ -1.8%   │ $980K    │ ✓      │
│  Blue Cross TC   │ 18,000 │ 78%     │ +0.5%   │ -$120K   │ ⚠      │
│  Cigna ACO       │ 15,000 │ 85%     │ -3.1%   │ $720K    │ ✓      │
│  UHC P4P         │ 12,000 │ 80%     │ N/A     │ $340K    │ ✓      │
│  Humana Stars    │ 8,000  │ 4.0★    │ N/A     │ $280K    │ ✓      │
│  ... (5 more)    │ ...    │ ...     │ ...     │ ...      │ ...    │
│  ────────────────┼────────┼─────────┼─────────┼──────────┼─────── │
│  TOTAL           │180,000 │ 83% avg │ -1.4%   │ $9.8M    │        │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Custom Measure Development

### Research Integration

RMC's research team uses HDIM to evaluate custom quality measures for NIH-funded studies:

```cql
// Custom measure: Time to antibiotic for community-acquired pneumonia
library CustomPneumoniaTimeline version '1.0.0'

using FHIR version '4.0.1'

context Patient

define "CAP Admission":
  [Encounter: "Inpatient"] E
    where E.reasonCode in "Community Acquired Pneumonia Codes"
      and E.period.start during "Measurement Period"

define "First Antibiotic":
  First([MedicationAdministration: "Pneumonia Antibiotics"] M
    where M.effective during "CAP Admission".period
    sort by effective.start)

define "Time to Antibiotic":
  hours between "CAP Admission".period.start and "First Antibiotic".effective.start

define "Met Target (< 4 hours)":
  "Time to Antibiotic" < 4
```

### Custom Measure Workflow

1. Research team defines measure logic
2. HDIM team validates CQL syntax
3. Deploy to research environment
4. Run against study cohort
5. Export results to research data warehouse
6. Iterate on measure definition

---

## Implementation Timeline

### Phase 1: Infrastructure (Month 1)

- [x] Azure subscription provisioning
- [x] Network architecture design
- [x] AKS cluster deployment
- [x] PostgreSQL + Blob storage setup
- [x] Security controls implementation
- [x] Azure AD SSO configuration

### Phase 2: Data Integration (Month 2)

- [x] Epic Interconnect configuration
- [x] FHIR API authorization
- [x] Bulk export setup
- [x] Webhook configuration
- [x] Initial data load (180K patients)

### Phase 3: Configuration (Month 3)

- [x] 12 payer contract configurations
- [x] Custom measure development (15 measures)
- [x] Alert rule configuration
- [x] Dashboard customization
- [x] Tableau integration

### Phase 4: Training & Rollout (Month 4)

- [x] Train 50 quality staff
- [x] Train 800+ providers (via Epic integration)
- [x] Phased clinic rollout
- [x] Hospital rollout
- [x] Production go-live

---

## Expected Outcomes

### Quality Improvement

| Measure | Baseline | 12-Month | Improvement |
|---------|----------|----------|-------------|
| Diabetes HbA1c >9% | 22% | 14% | -8 pts |
| Blood Pressure Control | 72% | 82% | +10 pts |
| Breast Cancer Screening | 78% | 86% | +8 pts |
| Colorectal Screening | 68% | 80% | +12 pts |
| Medication Adherence | 75% | 85% | +10 pts |
| 30-Day Readmission | 12.8% | 10.5% | -2.3 pts |
| Medicare Stars Rating | 3.5★ | 4.25★ | +0.75★ |

### Financial Impact

| Category | Annual Value |
|----------|--------------|
| **ACO REACH shared savings (additional)** | $2,100,000 |
| **Medicare Stars bonus (improvement)** | $3,200,000 |
| **Commercial VBC incentives** | $2,800,000 |
| **Readmission reduction** | $1,800,000 |
| **Avoided vendor costs** | $400,000 (vs. $500K competitors) |
| **Staff efficiency** | $320,000 |
| **Total Annual Value** | **$10,620,000** |

### ROI Calculation

```
Annual Value:        $10,620,000
Annual HDIM Cost:    $180,000 ($15K × 12)
Net Annual Benefit:  $10,440,000
ROI:                 59x
Payback Period:      6 days
```

---

## Pricing Summary

| Component | Cost |
|-----------|------|
| HDIM software license (Health System tier) | $8,000/month |
| Private cloud deployment support | $3,000/month |
| Custom measure development (15) | $2,000/month |
| Premium support (dedicated CSM) | $2,000/month |
| **Total Monthly** | **$15,000/month** |
| **Annual Cost** | **$180,000/year** |

*Cost per patient: $0.08/patient/month*
*Compared to: Arcadia ($0.50/patient), Innovaccer ($0.35/patient)*

---

## Security & Compliance

### Compliance Certifications

| Requirement | Status |
|-------------|--------|
| HIPAA BAA | Signed (HDIM + Azure) |
| SOC 2 Type II | In progress (HDIM), Azure certified |
| HITRUST | Azure certified |
| State Privacy Laws | Compliant |
| Data Residency | All data in customer Azure |
| NIH Data Security | Compliant (private cloud) |

### Security Controls

| Control | Implementation |
|---------|----------------|
| Encryption at rest | AES-256 (customer-managed keys) |
| Encryption in transit | TLS 1.3 |
| Network isolation | Private endpoints, no public IPs |
| Identity | Azure AD SAML SSO |
| Access control | RBAC with Epic MyChart linkage |
| Audit logging | Azure Monitor + HDIM audit logs |
| Vulnerability scanning | Azure Defender + HDIM scans |
| Penetration testing | Annual (customer-led) |

---

## Success Metrics

| Metric | Target | Actual (Month 6) |
|--------|--------|------------------|
| System uptime | 99.95% | 99.98% |
| Query latency | <200ms | 142ms avg |
| Data freshness | <4 hours | 2.3 hours avg |
| Payer reports delivered | 100% on-time | 100% |
| Custom measures deployed | 15 | 18 |
| Provider satisfaction | >4.0/5.0 | 4.3/5.0 |
| ROI achievement | >50x | 59x |

---

## CMIO Testimonial

> "We looked at every major population health vendor. They wanted $500K-$1.2M per year and 18-24 months to implement. HDIM gave us a private cloud deployment in 4 months for $180K/year. But the real differentiator is speed. Our old system had 48-hour latency. HDIM gives us real-time. When a patient's A1c comes back at 10%, the provider sees an alert in their Epic In-Basket within seconds—not two days later. We're now managing 12 different VBC contracts from a single platform. Our Medicare Stars rating went from 3.5 to 4.25. That's worth $3.2M in bonus payments alone."
>
> — Dr. Katherine Park, CMIO, Regional Medical Center

---

## Related Resources

- [FHIR Payloads](../_shared/FHIR_PAYLOADS.md)
- [ACO Measure Sets](../_shared/MEASURE_SETS.md#aco-measures)
- [Implementation Checklist](../_templates/IMPLEMENTATION_CHECKLIST.md)
- [Security Architecture](../../SECURITY_ARCHITECTURE.md)
- [Deployment Options](../../DEPLOYMENT_OPTIONS.md)

---

*Last Updated: December 2025*
*Version: 1.0*
