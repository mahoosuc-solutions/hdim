---
id: "sales-tools-ai-pricing-forecast-schema"
title: "AI Pricing Forecast Schema"
portalType: "sales"
path: "sales/03-sales-tools/ai-pricing-forecast-schema.md"
category: "sales-tools"
subcategory: "commercial-forecasting"
tags:
  - "pricing"
  - "forecast"
  - "social-impact"
  - "revops"
relatedDocuments:
  - "docs/gtm/AI_PRICING_LOCAL_IMPACT_POLICY.md"
  - "docs/sales/03-sales-tools/pricing-guide.md"
  - "docs/sales/03-sales-tools/pricing-calculator-worksheet.md"
status: "active"
version: "1.0"
lastUpdated: "2026-03-10"
accessLevel: "internal"
---

# AI Pricing Forecast Schema

## Purpose
Standardize launch pricing forecasts so Sales, Finance, and Compliance use the same structure for:
- AI-adjusted implementation pricing
- Buyer-type segmentation
- Local impact allocation forecasting

## Primary Segmentation
Forecasts are grouped by buyer type:
1. Payer
2. Provider / Health System
3. ACO / FQHC
4. Vendor / Partner

Secondary slices:
- Size tier: Growth, Mid-market, Enterprise
- Deployment model: Cloud, Hybrid, On-prem

## Required Fields
Use these fields for every forecast row.

| Field | Type | Example | Notes |
|---|---|---|---|
| `forecast_id` | string | `FY26Q2-PAYER-001` | Unique row identifier |
| `snapshot_date` | date | `2026-03-10` | Forecast run date |
| `buyer_type` | enum | `Payer` | Required segmentation key |
| `size_tier` | enum | `Enterprise` | Drives social allocation % |
| `deployment_model` | enum | `Hybrid` | Cloud/Hybrid/On-prem |
| `raw_story_points` | number | `149` | Pre-ALF story points |
| `alf` | decimal | `0.68` | AI Leverage Factor |
| `effective_billable_points` | number | `101.32` | `raw_story_points x alf` |
| `point_rate_usd` | currency | `1950` | Per effective point |
| `implementation_fee_usd` | currency | `197574` | `effective_billable_points x point_rate_usd` |
| `platform_base_usd` | currency | `210000` | Annual |
| `integration_tier_usd` | currency | `95000` | Annual |
| `intelligence_tier_usd` | currency | `145000` | Annual |
| `annual_subscription_usd` | currency | `450000` | Sum of annual components |
| `social_share_pct` | decimal | `0.03` | 1/2/3% by size tier |
| `social_share_amount_usd` | currency | `19427` | `(implementation + subscription) x social_share_pct` |
| `year1_total_usd` | currency | `667001` | Implementation + Subscription + Social |
| `year2_recurring_usd` | currency | `463500` | Subscription + recurring social share |
| `year3_recurring_usd` | currency | `463500` | Subscription + recurring social share |
| `three_year_tcv_usd` | currency | `1598001` | Year1 + Year2 + Year3 |
| `local_scope` | enum | `MSA_COUNTY` | `MSA_COUNTY` or `STATE_FALLBACK` |
| `local_target_name` | string | `Houston-The Woodlands-Sugar Land MSA` | Required when known |
| `forecast_confidence` | enum | `Moderate` | Conservative/Moderate/Aggressive |
| `owner` | string | `sales-ops` | Forecast record owner |

## Canonical Formulas
1. `effective_billable_points = raw_story_points x alf`
2. `implementation_fee_usd = effective_billable_points x point_rate_usd`
3. `annual_subscription_usd = platform_base_usd + integration_tier_usd + intelligence_tier_usd`
4. `social_share_amount_usd = (implementation_fee_usd + annual_subscription_usd) x social_share_pct`
5. `year1_total_usd = implementation_fee_usd + annual_subscription_usd + social_share_amount_usd`

For recurring years:
- `yearN_social_share = annual_subscription_usd x social_share_pct`
- `yearN_recurring_usd = annual_subscription_usd + yearN_social_share`

## Default Launch Assumptions
- Social share tier defaults:
  - Growth: `1%`
  - Mid-market: `2%`
  - Enterprise: `3%`
- Local routing:
  - Primary: customer MSA/county
  - Fallback: state-level pool

## Validation Rules
- `social_share_pct` must match `size_tier` unless an approved exception exists.
- `local_scope` must be populated for all committed deals.
- `year1_total_usd` must include social share line item.
- Any zero social allocation must include executive exception reference.

## Minimal Export View (CSV)
For board/revops reporting, include:
- `forecast_id`
- `buyer_type`
- `size_tier`
- `implementation_fee_usd`
- `annual_subscription_usd`
- `social_share_amount_usd`
- `year1_total_usd`
- `three_year_tcv_usd`
- `local_scope`
- `forecast_confidence`
