---
# Core Identifiers
id: "sales-tools-pricing"
title: "Pricing Guide"
portalType: "sales"
path: "sales/03-sales-tools/pricing-guide.md"

# Organization
category: "sales-tools"
subcategory: "commercial-model"
tags:
  - "pricing"
  - "story-points"
  - "ai-delivery"
  - "integration"
relatedDocuments:
  - "docs/investor/ROI_DEFENSIBILITY_PACK_TEMPLATE.md"
  - "docs/investor/PILOT_OUTCOMES_AND_ROI_METHOD_ONE_PAGER_2026-03-08.md"
  - "docs/investor/yc-application-v2/INNOVATION_STORY_SCRIPTS.md"
  - "docs/sales/03-sales-tools/pricing-calculator-worksheet.md"

# Content Description
summary: "How to quantify HDIM AI-delivered intelligence in story points and convert that into pricing."
estimatedReadTime: 12
difficulty: "intermediate"
lastUpdated: "2026-03-08"

# Access & Governance
targetAudience:
  - "sales-rep"
  - "founder"
  - "revops"
owner: "Sales Operations"
reviewCycle: "quarterly"
nextReviewDate: "2026-06-01"
accessLevel: "internal"

# Status & Versioning
status: "active"
version: "2.0"
lastReviewed: "2026-03-08"

# SEO & Discovery
seoKeywords:
  - "healthcare AI pricing"
  - "story point pricing"
  - "integration pricing"
externalLinks: []
hasVideo: false
videoUrl: null

# Content Metrics
wordCount: 0
createdDate: "2025-12-01"
viewCount: 0
avgRating: null
feedbackCount: 0
---

# Pricing Guide: AI Intelligence + Story Point Model

## Purpose
Use this model to explain:
1. What is included in base HDIM.
2. What additional intelligence is layered for each customer.
3. How AI-assisted delivery changes speed and economics.
4. How to convert scope to a defendable implementation price.

## Core Message (Customer + Investor)
HDIM is not priced like a generic software license plus undefined services.
We price a measurable delivery system:
1. Base platform capability.
2. Integration complexity.
3. Intelligence overlays (automation, governance, analytics, domain tuning).
4. Story-pointed implementation scope with explicit assumptions.

## What "AI Intelligence" Means in HDIM
When we say we poured intelligence into the platform, we mean six concrete layers above base infrastructure:
1. Clinical intelligence: measure logic, care-gap prioritization, workflow signals.
2. Integration intelligence: repeatable Epic/Cerner/Athena/FHIR onboarding patterns.
3. Operational intelligence: release evidence, fail-closed governance, runbooks.
4. Security intelligence: control mapping, audit traceability, policy enforcement.
5. Commercial intelligence: ROI defensibility packs, procurement-ready artifacts.
6. Agentic delivery intelligence: AI-assisted implementation acceleration with human architectural control.

## Story Point Taxonomy
Use Fibonacci-like sizing to keep calibration consistent.

| Size | Points | Typical effort profile |
|---|---:|---|
| XS | 1 | Copy or config updates with no dependency risk |
| S | 2 | Minor feature wiring or single-system config |
| M | 3 | Standard endpoint/workflow with known pattern |
| L | 5 | Multi-file feature + test + documentation |
| XL | 8 | Cross-service change with integration/testing impact |
| XXL | 13 | New capability slice with governance requirements |

## Scope Buckets for Pricing
Estimate each engagement across five buckets.

| Bucket | Description | Typical Point Range |
|---|---|---:|
| B1 Foundation | Environment, access, baseline configuration, readiness | 8-20 |
| B2 Integration | EHR/FHIR interfaces, mapping, validation, data flow hardening | 20-80 |
| B3 Intelligence Overlay | AI workflows, prioritization logic, automation tuning, analytics | 13-60 |
| B4 Governance + Security | Audit evidence, controls, release gates, runbooks | 8-34 |
| B5 Enablement + Change | UAT, training, adoption workflows, operating cadence | 5-21 |

## Example Story Point Breakdown (Enterprise Health System)

| Workstream | Story Points |
|---|---:|
| Base tenant + deployment foundation | 13 |
| Primary EHR integration (FHIR) | 34 |
| Secondary data feed normalization | 21 |
| Clinical measure tuning + AI prioritization | 21 |
| Release evidence and trust-center packetization | 13 |
| Security controls + procurement package support | 13 |
| Training + go-live enablement | 8 |
| **Total** | **123** |

## AI Leverage Factor (ALF)
ALF explains why delivery is faster without hiding risk.

Formula:
- `Effective Billable Points = Raw Story Points x ALF`

Recommended ALF range:
- `0.55` to `0.75`

Interpretation:
1. Lower ALF = more reusable patterns, higher AI acceleration.
2. Higher ALF = more custom constraints, higher human-intensive design.
3. Architecture, security decisions, and clinical accountability remain human-led.

## Pricing Conversion Model
Use a point rate plus platform subscription.

Formula:
1. `Implementation Fee = Effective Billable Points x Point Rate`
2. `Annual Subscription = Platform Base + Integration Tier + Intelligence Tier`

Point-rate guidance (internal):
- Growth segment: `$900-$1,200` per effective point
- Mid-market: `$1,200-$1,600` per effective point
- Enterprise: `$1,600-$2,200` per effective point

Example (123 raw points, ALF 0.65, $1,750/point):
- Effective Billable Points: `79.95`
- Implementation Fee: `~$140K`

## Packaging for Sales
Use three commercial packages to simplify buying:

| Package | Typical Raw Points | Target Buyer | Commercial Framing |
|---|---:|---|---|
| Launch | 35-60 | Small network / pilot | Fast go-live, single integration, essential intelligence |
| Scale | 61-110 | Multi-site org | Multi-feed integration, governance, role-based evidence |
| Enterprise+ | 111-180 | Health system / payer | Multi-system architecture, advanced automation, full trust posture |

## Talk Track: "Why This Is a New Implementation Model"
Use this exact narrative:
1. "We do not estimate by vague hours; we estimate by outcome-defined story points."
2. "We separate base platform value from customer-specific intelligence work."
3. "AI increases delivery velocity, but clinical safety and architecture control remain human-led."
4. "You get faster implementation, clearer economics, and stronger auditability."

## Evidence You Should Pair With This Pricing Story
Always attach these with proposals:
1. Trust Center + Evidence Room flow.
2. Pilot outcomes and ROI method one-pager.
3. Implementation plan with bucketed story points.
4. Risk and governance checklist (go/no-go criteria).

## Internal Operating Standard (Required)
For every scoped deal, store these artifacts:
1. Raw story point estimate by bucket.
2. ALF rationale (why acceleration factor chosen).
3. Effective billable points and rate used.
4. Subscription tier recommendation and assumptions.
5. Expected customer outcomes with validation method.

This is required so pricing remains consistent, auditable, and defensible.

## Worksheet
Use the one-page calculator worksheet for live deal pricing:
- `docs/sales/03-sales-tools/pricing-calculator-worksheet.md`
