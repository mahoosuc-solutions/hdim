---
id: "sales-tools-pricing-example-enterprise-health-system"
title: "Pricing Calculator Example - Enterprise Health System"
portalType: "sales"
path: "sales/03-sales-tools/pricing-calculator-example-enterprise-health-system.md"
category: "sales-tools"
subcategory: "commercial-model"
tags:
  - "pricing"
  - "story-points"
  - "enterprise"
relatedDocuments:
  - "docs/sales/03-sales-tools/pricing-calculator-worksheet.md"
status: "active"
version: "1.0"
lastUpdated: "2026-03-08"
accessLevel: "internal"
---

# Example: Enterprise Health System

## 1) Deal Snapshot
- Customer: Regional Medical Center (example)
- Segment: `Enterprise`
- Primary buyer: `Mixed (CIO/CISO + Quality + Procurement)`
- Deployment model: `Hybrid`
- Target go-live date: `2026-08-01`

## 2) Story Point Scope (Raw)
| Bucket | Range | Selected Points |
|---|---:|---:|
| B1 Foundation | 8-20 | 18 |
| B2 Integration | 20-80 | 55 |
| B3 Intelligence Overlay | 13-60 | 42 |
| B4 Governance + Security | 8-34 | 21 |
| B5 Enablement + Change | 5-21 | 13 |
| **Total Raw Story Points** |  | **149** |

## 3) AI Leverage Factor (ALF)
- ALF selected: **0.68**
- Rationale:
  - Reusable integration patterns: strong for Epic + FHIR flows.
  - Custom constraints: elevated security governance and enterprise change control.

- Effective Billable Points: `149 x 0.68 = 101.32`

## 4) Pricing Conversion
- Point rate: **$1,950**
- Implementation Fee: `101.32 x 1,950 = $197,574`

Subscription (annual):
- Platform base: **$210,000**
- Integration tier: **$95,000**
- Intelligence tier: **$145,000**
- Annual Subscription: `$450,000`
- Social allocation (enterprise `3%`): `($197,574 + $450,000) x 3% = $19,427`
- Local routing target: `Customer MSA/county; state fallback`

- Year 1 Total: `$667,001`

## 5) Buyer Outcome Commitments
- Time-to-value target: `14 weeks`
- KPI 1: `% reduction in manual quality reporting hours`
- KPI 2: `% increase in in-visit care-gap closure`
- ROI method reference: `docs/investor/PILOT_OUTCOMES_AND_ROI_METHOD_ONE_PAGER_2026-03-08.md`

## 6) Methodology Proof Block
1. Human architect defines risk boundaries and clinical priorities.
2. AI accelerates implementation and test/documentation throughput.
3. Story-point pricing keeps scope and economics explicit.
4. Governance assets preserve compliance and release safety.

## 7) Risk + Confidence Gates
- Evidence room package selected: `security + reliability + procurement`
- Trust center references attached: `yes`
- Manual approval path defined: `yes`
- Go/No-Go conditions documented: `yes`

## 8) Approvals
- Sales owner: `TBD`
- Technical approver: `TBD`
- Security/compliance approver: `TBD`
- Final quote date: `TBD`
