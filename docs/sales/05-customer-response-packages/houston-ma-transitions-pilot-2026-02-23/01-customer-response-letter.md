# Customer Response Letter

Subject: Phase 1 Pilot Proposal - Post-Discharge Patient Engagement for Houston MA Plan

Dr. [Last Name],

Thank you for the thoughtful discussion and for sharing your prototype concept. We align with the core direction:

1. Zero durable PHI storage on the AI layer and minimal persistence overall.
2. Secure, context-aware AI summaries in plain language for patients.
3. Future-ready architecture for Epic MyChart bi-directional workflows.
4. A measurable, 90-day pilot tied to transitions-of-care outcomes.

We propose a Phase 1 pilot designed specifically for a Houston Medicare Advantage plan (approximately 30,000 members) focused on 200-500 discharges over 90 days.

## Expected Pilot Outcomes

1. Increased 7-day post-discharge patient engagement.
2. Improved patient comprehension confirmation on discharge instructions.
3. Faster routing and closure of clinically relevant patient questions.
4. Clear evidence package for pilot-to-scale conversion.

## Deployment Model Options

We can execute the pilot under any of these operating models:

1. HDIM-hosted (recommended for fastest start and lowest internal lift).
2. Customer-hosted cloud (customer account ownership with HDIM-managed platform).
3. On-prem/private data center, including an air-gapped operating variant.

Detailed tradeoffs are included in `07-deployment-models-hosted-vs-onprem.md`.

## What We Will Deliver in Phase 1

1. A secure patient engagement workflow for post-discharge questions and education.
2. AI-generated layman summaries of discharge context and care instructions.
3. Escalation paths to care teams for high-risk or unresolved questions.
4. KPI tracking for engagement, comprehension, and operational response times.
5. Pilot governance, security controls, and weekly outcomes reporting.

## Pilot Objectives

1. Increase post-discharge engagement within 7 days.
2. Improve patient comprehension scores on key discharge topics.
3. Reduce unresolved patient questions at 48-72 hours post-discharge.
4. Produce a conversion-ready operational and economic case for scale.

## Architecture Position

Phase 1 uses a zero-retention AI pattern:

- AI receives scoped context at request time.
- PHI is not durably stored in the AI service.
- Source-of-truth clinical data remains in payer/provider systems.
- Platform persistence is limited to operational metadata and audit evidence.

## Commercial Summary

- Pilot duration: 90 days
- Estimated implementation and pilot operations fee: $265,000 to $345,000
- Estimated infrastructure and model usage pass-through: $12,000 to $38,000 total for pilot period
- Total estimated pilot budget: $277,000 to $383,000

Detailed assumptions and line-item estimates are included in `05-estimated-costs-and-commercials.md`.

If this aligns, we propose two immediate next steps:

1. Schedule a 90-minute Week-1 architecture workshop with clinical, IT, and security stakeholders.
2. Confirm deployment model selection by end of Week 1 to finalize timeline and commercial terms.

Sincerely,

Aaron [Last Name]
Founder, HDIM
