---
id: "sales-tools-pricing-example-mid-size-aco"
title: "Pricing Calculator Example - Mid-size ACO"
portalType: "sales"
path: "sales/03-sales-tools/pricing-calculator-example-mid-size-aco.md"
category: "sales-tools"
subcategory: "commercial-model"
tags:
  - "pricing"
  - "story-points"
  - "aco"
relatedDocuments:
  - "docs/sales/03-sales-tools/pricing-calculator-worksheet.md"
status: "active"
version: "1.0"
lastUpdated: "2026-03-08"
accessLevel: "internal"
---

# Example: Mid-size ACO

## 1) Deal Snapshot
- Customer: Metro Health Alliance (example)
- Segment: `Mid-market`
- Primary buyer: `Clinical + Operations`
- Deployment model: `Cloud`
- Target go-live date: `2026-06-15`

## 2) Story Point Scope (Raw)
| Bucket | Range | Selected Points |
|---|---:|---:|
| B1 Foundation | 8-20 | 12 |
| B2 Integration | 20-80 | 32 |
| B3 Intelligence Overlay | 13-60 | 24 |
| B4 Governance + Security | 8-34 | 13 |
| B5 Enablement + Change | 5-21 | 8 |
| **Total Raw Story Points** |  | **89** |

## 3) AI Leverage Factor (ALF)
- ALF selected: **0.62**
- Rationale:
  - Reusable patterns: high for standard FHIR and known measure set.
  - Custom constraints: moderate governance and reporting requirements.

- Effective Billable Points: `89 x 0.62 = 55.18`

## 4) Pricing Conversion
- Point rate: **$1,350**
- Implementation Fee: `55.18 x 1,350 = $74,493`

Subscription (annual):
- Platform base: **$96,000**
- Integration tier: **$36,000**
- Intelligence tier: **$48,000**
- Annual Subscription: `$180,000`

- Year 1 Total: `$254,493`

## 5) Buyer Outcome Commitments
- Time-to-value target: `10 weeks`
- KPI 1: `% increase in attributed panel quality measure compliance`
- KPI 2: `% reduction in care-management list preparation time`
- ROI method reference: `docs/investor/PILOT_OUTCOMES_AND_ROI_METHOD_ONE_PAGER_2026-03-08.md`

## 6) Methodology Proof Block
1. Domain leads define measure priorities and workflow outcomes.
2. AI-assisted build compresses delivery timeline.
3. Story points convert scope into transparent price.
4. Evidence package supports board and payer reporting confidence.

## 7) Risk + Confidence Gates
- Evidence room package selected: `reliability + procurement`
- Trust center references attached: `yes`
- Manual approval path defined: `yes`
- Go/No-Go conditions documented: `yes`

## 8) Approvals
- Sales owner: `TBD`
- Technical approver: `TBD`
- Security/compliance approver: `TBD`
- Final quote date: `TBD`
