# HDIM Metrics Dashboard Specification

> KPI definitions, data source mappings, dashboard layouts, and reporting cadences.

---

## Overview

This document defines the key performance indicators (KPIs) for HDIM, their calculation methods, data sources, and dashboard specifications. These metrics drive decision-making across all business functions.

**Dashboard Tool:** [Metabase/Looker/Tableau]
**Data Warehouse:** [Snowflake/BigQuery/Redshift]
**Refresh Frequency:** Daily (overnight ETL)

---

## Executive Dashboard

### Purpose
Single view of overall business health for leadership team.

### Metrics Summary

| Metric | Current | Target | Trend |
|--------|---------|--------|-------|
| MRR | $X | $X | ↑/↓ |
| ARR | $X | $X | ↑/↓ |
| Net Revenue Retention | X% | 120% | ↑/↓ |
| Customers | # | # | ↑/↓ |
| NPS | # | 50+ | ↑/↓ |

### Dashboard Layout

```
┌────────────────────────────────────────────────────────────────┐
│                    EXECUTIVE DASHBOARD                          │
│  [Date Range Selector]                    [Refresh: Daily]      │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────┐ │
│  │     MRR      │ │     ARR      │ │  Customers   │ │  NPS   │ │
│  │   $125,000   │ │  $1,500,000  │ │     127      │ │   52   │ │
│  │   ↑ 8% MoM   │ │   ↑ 15% YoY  │ │   ↑ 12 MoM   │ │  ↑ 5   │ │
│  └──────────────┘ └──────────────┘ └──────────────┘ └────────┘ │
├────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────┐ ┌─────────────────────────────┐│
│  │       MRR Trend Chart       │ │    Customer Growth Chart    ││
│  │   [12-month line chart]     │ │    [12-month line chart]    ││
│  └─────────────────────────────┘ └─────────────────────────────┘│
├────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────┐ ┌─────────────────────────────┐│
│  │    Revenue by Segment       │ │      Customer Health        ││
│  │   [Pie chart: SMB/MM/ENT]   │ │  [Pie: Healthy/Risk/Churn]  ││
│  └─────────────────────────────┘ └─────────────────────────────┘│
└────────────────────────────────────────────────────────────────┘
```

---

## Revenue Metrics

### MRR (Monthly Recurring Revenue)

**Definition:** Total recurring revenue normalized to a monthly amount.

**Formula:**
```
MRR = Σ (Active Customer Monthly Subscription Fees)
```

**Components:**
| Component | Description |
|-----------|-------------|
| New MRR | MRR from new customers this month |
| Expansion MRR | MRR increase from existing customers |
| Contraction MRR | MRR decrease from existing customers |
| Churned MRR | MRR lost from cancelled customers |
| Net New MRR | New + Expansion - Contraction - Churned |

**Data Source:** Billing system (Stripe)
**Refresh:** Daily
**Owner:** Finance

---

### ARR (Annual Recurring Revenue)

**Definition:** Annualized value of recurring revenue.

**Formula:**
```
ARR = MRR × 12
```

**Data Source:** Calculated from MRR
**Refresh:** Daily
**Owner:** Finance

---

### Net Revenue Retention (NRR)

**Definition:** Percentage of revenue retained from existing customers, including expansion.

**Formula:**
```
NRR = (Starting MRR + Expansion - Contraction - Churn) / Starting MRR × 100
```

**Example:**
- Starting MRR: $100,000
- Expansion: $10,000
- Contraction: $2,000
- Churn: $3,000
- NRR: ($100K + $10K - $2K - $3K) / $100K = 105%

**Target:** 120%+
**Data Source:** Billing system
**Refresh:** Monthly
**Owner:** Finance / CS

---

### Gross Revenue Retention (GRR)

**Definition:** Percentage of revenue retained from existing customers, excluding expansion.

**Formula:**
```
GRR = (Starting MRR - Contraction - Churn) / Starting MRR × 100
```

**Target:** 95%+
**Data Source:** Billing system
**Refresh:** Monthly
**Owner:** Finance / CS

---

### Average Revenue Per User (ARPU)

**Definition:** Average monthly revenue per customer.

**Formula:**
```
ARPU = MRR / Active Customers
```

**Data Source:** Billing system
**Refresh:** Monthly
**Owner:** Finance

---

### Customer Lifetime Value (LTV)

**Definition:** Expected total revenue from a customer over their lifetime.

**Formula:**
```
LTV = ARPU × Average Customer Lifetime (months)
```

Or:
```
LTV = ARPU / Monthly Churn Rate
```

**Data Source:** Billing system + churn data
**Refresh:** Quarterly
**Owner:** Finance

---

### Customer Acquisition Cost (CAC)

**Definition:** Average cost to acquire a new customer.

**Formula:**
```
CAC = (Sales + Marketing Spend) / New Customers Acquired
```

**Data Source:** Accounting + CRM
**Refresh:** Monthly
**Owner:** Finance / Marketing

---

### LTV:CAC Ratio

**Definition:** Efficiency of customer acquisition spend.

**Formula:**
```
LTV:CAC = LTV / CAC
```

**Target:** 3:1 or higher
**Data Source:** Calculated
**Refresh:** Quarterly
**Owner:** Finance

---

## Sales Metrics

### Pipeline Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Pipeline Value | Σ Opportunity Values | 3x Quota |
| Pipeline Coverage | Pipeline / Quota | 3x+ |
| Win Rate | Closed Won / (Won + Lost) | 25%+ |
| Average Deal Size | Total Revenue / Deals Closed | By segment |
| Sales Cycle | Avg days from first touch to close | <60 days |

### Conversion Funnel

| Stage | Definition | Target Conversion |
|-------|------------|-------------------|
| Lead | Inbound or outbound contact | - |
| MQL | Marketing qualified lead | 30% of leads |
| SQL | Sales qualified lead | 50% of MQLs |
| Opportunity | Active sales opportunity | 60% of SQLs |
| Demo | Completed demo | 80% of opps |
| Proposal | Proposal sent | 60% of demos |
| Closed Won | Customer signed | 40% of proposals |

### Sales Dashboard Layout

```
┌────────────────────────────────────────────────────────────────┐
│                     SALES DASHBOARD                             │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────┐ │
│  │   Pipeline   │ │   Quota %    │ │   Win Rate   │ │ Deals  │ │
│  │  $1,250,000  │ │     67%      │ │     28%      │ │   45   │ │
│  │   ↑ 15% MoM  │ │   On Track   │ │   ↑ 3% MoM   │ │  Open  │ │
│  └──────────────┘ └──────────────┘ └──────────────┘ └────────┘ │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 Conversion Funnel                         │  │
│  │  Leads → MQL → SQL → Opp → Demo → Proposal → Won          │  │
│  │   500    150    75    45    36      22       9             │  │
│  └──────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────┐ ┌──────────────────────────────┐   │
│  │   Pipeline by Stage    │ │    Pipeline by Segment       │   │
│  │   [Horizontal bar]     │ │    [Pie chart]               │   │
│  └────────────────────────┘ └──────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

---

## Marketing Metrics

### Demand Generation

| Metric | Formula | Target |
|--------|---------|--------|
| Website Visitors | Unique monthly visitors | 10,000+ |
| Lead Volume | New leads per month | 200+ |
| MQLs | Marketing qualified leads | 60+ |
| Cost per Lead | Marketing Spend / Leads | <$50 |
| Cost per MQL | Marketing Spend / MQLs | <$150 |

### Channel Performance

| Channel | Leads | MQLs | Customers | CAC |
|---------|-------|------|-----------|-----|
| Organic Search | # | # | # | $ |
| Paid Search | # | # | # | $ |
| Social | # | # | # | $ |
| Referral | # | # | # | $ |
| Events | # | # | # | $ |
| Outbound | # | # | # | $ |

### Content Performance

| Metric | Formula | Target |
|--------|---------|--------|
| Blog Traffic | Unique visitors to blog | 5,000+/mo |
| Email Open Rate | Opens / Sent | 25%+ |
| Email CTR | Clicks / Opens | 3%+ |
| Webinar Registration | Sign-ups per webinar | 100+ |
| Webinar Attendance | Attendees / Registrants | 40%+ |

---

## Customer Success Metrics

### Health Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Customer Health Score | Composite (usage, outcomes, engagement) | 80+ avg |
| Healthy Customers | % with score 80+ | 70%+ |
| At-Risk Customers | % with score <60 | <15% |
| NPS | Promoters - Detractors | 50+ |
| CSAT | Satisfied / Total Responses | 95%+ |

### Retention Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Logo Churn Rate | Churned / Starting Customers | <5% annually |
| Revenue Churn Rate | Churned MRR / Starting MRR | <3% annually |
| Customer Lifetime | Avg months as customer | 36+ months |

### Engagement Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| DAU | Daily active users | Trend up |
| MAU | Monthly active users | 80%+ of users |
| Feature Adoption | Users using key features | 70%+ |
| Training Completion | Users completing training | 90%+ |

### CS Dashboard Layout

```
┌────────────────────────────────────────────────────────────────┐
│                CUSTOMER SUCCESS DASHBOARD                       │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────┐ │
│  │ Health Avg   │ │     NPS      │ │    NRR       │ │ Churn  │ │
│  │     82       │ │      52      │ │    118%      │ │  2.1%  │ │
│  │   ↑ 3 pts    │ │   ↑ 5 pts    │ │   On Target  │ │  ↓ YoY │ │
│  └──────────────┘ └──────────────┘ └──────────────┘ └────────┘ │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Customer Health Distribution                 │  │
│  │  [Pie: Healthy (70%) / Neutral (18%) / At Risk (12%)]     │  │
│  └──────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────┐ ┌──────────────────────────────┐   │
│  │   Renewal Forecast     │ │     At-Risk Customers        │   │
│  │   [Next 90 days]       │ │     [Table with actions]     │   │
│  └────────────────────────┘ └──────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

---

## Product Metrics

### Usage Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| API Calls | Total API requests | Trend |
| Patients Analyzed | Unique patients processed | Growth |
| Measures Calculated | Quality evaluations | Growth |
| Care Gaps Identified | New care gaps found | Growth |
| Care Gaps Closed | Gaps resolved | Growth |
| Dashboard Logins | User logins per day | Growth |

### Performance Metrics

| Metric | Formula | Target | Alert |
|--------|---------|--------|-------|
| API Latency (p50) | 50th percentile response | <100ms | >200ms |
| API Latency (p95) | 95th percentile response | <500ms | >1000ms |
| CQL Evaluation | Measure calculation time | <200ms | >500ms |
| Error Rate | Errors / Requests | <0.1% | >0.5% |
| Uptime | Available / Total time | 99.9% | <99.5% |

### Feature Adoption

| Feature | Definition | Target |
|---------|------------|--------|
| Dashboard | Users viewing dashboard weekly | 80%+ |
| Care Gaps | Users using care gap worklists | 70%+ |
| Reports | Users running reports monthly | 60%+ |
| API | Customers using API | 20%+ |

---

## Support Metrics

### Volume Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Ticket Volume | Tickets per month | Track trend |
| Tickets per Customer | Total / Customers | <2/month |
| Ticket Growth | MoM change | Flat or down |

### Quality Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| First Response Time | Time to first response | <4 hours |
| Resolution Time | Time to close | <24 hours (P1/P2) |
| First Contact Resolution | Resolved on first reply | 70%+ |
| CSAT | Satisfied responses | 95%+ |
| SLA Compliance | Tickets within SLA | 95%+ |

### Support Dashboard Layout

```
┌────────────────────────────────────────────────────────────────┐
│                   SUPPORT DASHBOARD                             │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌────────┐ │
│  │ Open Tickets │ │  Avg Reply   │ │     FCR      │ │  CSAT  │ │
│  │      23      │ │   2.4 hrs    │ │     72%      │ │   96%  │ │
│  │   ↓ 15%      │ │   ↓ 30 min   │ │   ↑ 3%       │ │  Stable│ │
│  └──────────────┘ └──────────────┘ └──────────────┘ └────────┘ │
├────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                 Ticket Volume Trend                       │  │
│  │        [30-day trend line with daily volume]              │  │
│  └──────────────────────────────────────────────────────────┘  │
├────────────────────────────────────────────────────────────────┤
│  ┌────────────────────────┐ ┌──────────────────────────────┐   │
│  │  Top Issue Categories  │ │    SLA Compliance            │   │
│  │  [Horizontal bar]      │ │    [Gauge chart]             │   │
│  └────────────────────────┘ └──────────────────────────────┘   │
└────────────────────────────────────────────────────────────────┘
```

---

## Engineering Metrics

### Delivery Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Deployment Frequency | Deploys per week | Daily |
| Lead Time | Commit to production | <1 day |
| Change Failure Rate | Failed deploys / Total | <5% |
| Mean Time to Recovery | Avg incident resolution | <1 hour |

### Quality Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Test Coverage | Lines covered / Total | 80%+ |
| Bug Escape Rate | Bugs found in prod / Total | <10% |
| Technical Debt | Sprint capacity spent on debt | <20% |

### Capacity Metrics

| Metric | Formula | Target |
|--------|---------|--------|
| Sprint Velocity | Story points completed | Stable |
| Sprint Completion | Completed / Committed | 90%+ |
| Backlog Health | Groomed stories / Committed | 2+ sprints |

---

## Reporting Cadences

### Daily Reports

| Report | Audience | Metrics |
|--------|----------|---------|
| Executive Summary | Leadership | MRR, tickets, uptime |
| Sales Activity | Sales | Pipeline changes, demos |
| Support Queue | Support | Open tickets, SLA status |
| System Health | Engineering | Uptime, errors, performance |

### Weekly Reports

| Report | Audience | Metrics |
|--------|----------|---------|
| Executive Weekly | Leadership | All KPIs, trends |
| Sales Pipeline | Sales | Pipeline, forecast, activity |
| CS Health | CS | Health scores, at-risk |
| Marketing Performance | Marketing | Leads, conversions |
| Sprint Review | Engineering | Velocity, delivery |

### Monthly Reports

| Report | Audience | Metrics |
|--------|----------|---------|
| Board Report | Board/Investors | ARR, growth, runway |
| Monthly Business Review | All Hands | Company KPIs |
| Finance Report | Leadership | Revenue, expenses, cash |
| Customer Insights | Product | Usage, feedback, requests |

### Quarterly Reports

| Report | Audience | Metrics |
|--------|----------|---------|
| Quarterly Business Review | Board | Full business review |
| OKR Review | Leadership | Objective progress |
| Customer Analysis | CS/Product | Cohort analysis, LTV |

---

## Data Sources & ETL

### Source Systems

| System | Data | Refresh |
|--------|------|---------|
| Stripe | Billing, subscriptions | Real-time |
| HubSpot/Salesforce | CRM, leads, pipeline | Hourly |
| HDIM Platform | Usage, features | Real-time |
| Zendesk/Intercom | Support tickets | Real-time |
| Google Analytics | Website traffic | Daily |
| GitHub | Engineering metrics | Daily |

### Data Warehouse Tables

| Table | Source | Key Fields |
|-------|--------|------------|
| `dim_customers` | CRM | customer_id, segment, tier |
| `fact_mrr` | Stripe | customer_id, amount, date |
| `fact_usage` | Platform | customer_id, feature, count |
| `fact_tickets` | Support | ticket_id, customer_id, status |
| `fact_leads` | CRM | lead_id, source, stage |

### ETL Schedule

| Job | Frequency | Window |
|-----|-----------|--------|
| Billing sync | Hourly | :00 past each hour |
| CRM sync | Hourly | :15 past each hour |
| Usage aggregation | Daily | 2:00 AM ET |
| Full warehouse rebuild | Weekly | Sunday 3:00 AM ET |

---

## Alert Configuration

### Business Alerts

| Alert | Condition | Notify |
|-------|-----------|--------|
| MRR Drop | >5% MoM decline | Finance, CEO |
| Churn Spike | >2% monthly churn | CS, CEO |
| Pipeline Low | <2x quota coverage | Sales, CEO |
| NPS Drop | <40 or >10pt decline | CS, CEO |

### Operational Alerts

| Alert | Condition | Notify |
|-------|-----------|--------|
| Uptime | <99.5% (15 min) | Engineering |
| Error Rate | >0.5% (5 min) | Engineering |
| Latency | p95 >1000ms (5 min) | Engineering |
| Support Queue | >50 open tickets | Support |

---

## Appendix: Metric Definitions Glossary

| Term | Definition |
|------|------------|
| **MRR** | Monthly Recurring Revenue |
| **ARR** | Annual Recurring Revenue (MRR × 12) |
| **NRR** | Net Revenue Retention |
| **GRR** | Gross Revenue Retention |
| **ARPU** | Average Revenue Per User |
| **LTV** | Customer Lifetime Value |
| **CAC** | Customer Acquisition Cost |
| **NPS** | Net Promoter Score |
| **CSAT** | Customer Satisfaction Score |
| **FCR** | First Contact Resolution |
| **DAU** | Daily Active Users |
| **MAU** | Monthly Active Users |
| **MQL** | Marketing Qualified Lead |
| **SQL** | Sales Qualified Lead |
| **ACV** | Annual Contract Value |
| **TCV** | Total Contract Value |

---

*Last Updated: December 2025*
*Owner: Business Operations*
*Review Cadence: Quarterly*
