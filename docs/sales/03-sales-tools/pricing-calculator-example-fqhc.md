---
id: "sales-tools-pricing-example-fqhc"
title: "Pricing Calculator Example - FQHC"
portalType: "sales"
path: "sales/03-sales-tools/pricing-calculator-example-fqhc.md"
category: "sales-tools"
subcategory: "commercial-model"
tags:
  - "pricing"
  - "story-points"
  - "fqhc"
relatedDocuments:
  - "docs/sales/03-sales-tools/pricing-calculator-worksheet.md"
status: "active"
version: "1.0"
lastUpdated: "2026-03-08"
accessLevel: "internal"
---

# Example: FQHC

## 1) Deal Snapshot
- Customer: Community Health Partners (example)
- Segment: `Growth`
- Primary buyer: `Executive Director + Quality Lead`
- Deployment model: `Cloud`
- Target go-live date: `2026-05-20`

## 2) Story Point Scope (Raw)
| Bucket | Range | Selected Points |
|---|---:|---:|
| B1 Foundation | 8-20 | 9 |
| B2 Integration | 20-80 | 21 |
| B3 Intelligence Overlay | 13-60 | 13 |
| B4 Governance + Security | 8-34 | 8 |
| B5 Enablement + Change | 5-21 | 5 |
| **Total Raw Story Points** |  | **56** |

## 3) AI Leverage Factor (ALF)
- ALF selected: **0.58**
- Rationale:
  - Reusable patterns: very high with standard workflows.
  - Custom constraints: lower than enterprise, limited integration edge cases.

- Effective Billable Points: `56 x 0.58 = 32.48`

## 4) Pricing Conversion
- Point rate: **$1,050**
- Implementation Fee: `32.48 x 1,050 = $34,104`

Subscription (annual):
- Platform base: **$42,000**
- Integration tier: **$14,000**
- Intelligence tier: **$16,000**
- Annual Subscription: `$72,000`
- Social allocation (growth `1%`): `($34,104 + $72,000) x 1% = $1,061`
- Local routing target: `Customer MSA/county; state fallback`

- Year 1 Total: `$107,165`

## 5) Buyer Outcome Commitments
- Time-to-value target: `6 weeks`
- KPI 1: `% reduction in manual chart review/report prep`
- KPI 2: `% increase in preventive care gap closure`
- ROI method reference: `docs/investor/PILOT_OUTCOMES_AND_ROI_METHOD_ONE_PAGER_2026-03-08.md`

## 6) Methodology Proof Block
1. Small teams can execute like top-tier engineering orgs with guided AI methodology.
2. AI compresses delivery effort without sacrificing clinical intent.
3. Story-point pricing keeps scope visible and affordable.
4. Governance artifacts keep leadership confident in reliability and compliance.

## 7) Risk + Confidence Gates
- Evidence room package selected: `reliability`
- Trust center references attached: `yes`
- Manual approval path defined: `yes`
- Go/No-Go conditions documented: `yes`

## 8) Approvals
- Sales owner: `TBD`
- Technical approver: `TBD`
- Security/compliance approver: `TBD`
- Final quote date: `TBD`
