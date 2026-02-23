---
date: 2026-02-21
topic: race-track-fhir-pipeline-architecture-views
---

# Race Track FHIR Pipeline Architecture Views

## What We're Building
A three-tab architecture narrative that shows the same patient journey at three abstraction levels so each audience can understand how the pipeline works without losing alignment:
- Non-technical tab: fully illustrative race-track story.
- Mixed tab: hybrid architecture with metaphor + platform internals.
- Technical tab: full system internals with Kafka/Redis/service detail.

The core visual metaphor is patients driving race cars around a track while their data travels with them, representing event-driven processing. The diagram explicitly distinguishes customer-side systems from internal platform components, including customer FHIR server, customer CDR/EHR, internal FHIR server(s), Kafka-based services, and Redis data/context layers.

## Why This Approach
We considered three options: a single style for all users, separate unrelated diagrams per audience, and one canonical journey with progressive detail. We chose one canonical journey with progressive detail because it keeps executive messaging simple while giving technical buyers confidence in implementation realism.

This supports compare-and-contrast directly in one experience, preserves narrative consistency across sales/product/engineering conversations, and avoids contradiction between collateral variants.

## Key Decisions
- Decision: Use three tabs in one experience.
  Rationale: Users can switch perspectives without changing storyline.
- Decision: Keep one canonical patient journey across all tabs.
  Rationale: Supports clean abstraction, not conflicting flows.
- Decision: Increase technical depth per tab.
  Rationale: Non-technical users stay oriented; technical users get internals.
- Decision: Explicitly call out customer FHIR + customer CDR/EHR vs internal FHIR.
  Rationale: Demonstrates interoperable boundaries and plug-and-play integration.
- Decision: Include named credibility anchors (Healthix, HealthInfoNet, Verato).
  Rationale: Ties architecture choices to proven real-world HIE and identity experience.
- Decision: Deliver both interactive and static outputs.
  Rationale: Enables demos plus deck/docs reuse.

## Canonical Journey (All Tabs)
1. Patient Event Created
2. Data Ingested
3. Kafka Stream Routed
4. Service Processing
5. Redis Context Lookup
6. Care Rule Evaluation
7. Action Generated
8. Consumer Delivery
9. Outcome + Audit

## Canonical Lanes (All Tabs)
- Track lane: patient progression through workflow.
- Data lane: event/payload movement.
- Platform lane: services + Kafka + Redis.
- Outcome lane: tasks, alerts, care-gap updates.

## System Boundaries To Show Explicitly
- Customer Domain:
  - Customer FHIR Server (FHIR R4 APIs)
  - Customer CDR/EHR
- Interoperability Boundary:
  - Standards adapter (FHIR REST, bulk export/import, subscriptions)
- Internal Platform Domain:
  - Internal FHIR Server(s) (normalized longitudinal records)
  - Kafka event backbone
  - Processing services
  - Redis caches and idempotency store
- Write-back Domain:
  - FHIR-native actions to customer systems (Task, CarePlan, Communication)

## Tab-by-Tab Content Spec

### Tab 1: Non-Technical (Fully Illustrative)
Title: `Patient Race Track: From Data to Better Outcomes`

Visual language:
- Race track with pit stops as simple stages.
- Patient cars carry a “health data” token.
- Minimal technical labels.

Required labels:
- Start: `Customer Data Sources`
- Stop 1: `Customer FHIR + CDR`
- Stop 2: `Smart Processing Engine`
- Stop 3: `Care Decision Checkpoint`
- Finish: `Action Delivered + Outcome Improved`

Connector copy:
- `FHIR standards in`
- `Events move in real time`
- `Clinical context applied`
- `Standard actions back to care teams`

Narrative callout:
- `Plug-and-play workflows using healthcare standards, not custom interfaces.`

### Tab 2: Mixed Audience (Hybrid)
Title: `Standards + Streaming Pipeline`

Visual language:
- Architecture blocks with race track overlays.
- Moderate internals visible.

Required nodes:
- `Customer FHIR Server`
- `Customer CDR/EHR`
- `Interoperability Gateway (FHIR R4)`
- `Internal FHIR Server`
- `Kafka Topics`
- `Services: Patient, Care Gap, Quality Measure, Event`
- `Redis: Snapshot, Rule Context, Idempotency`
- `Action APIs (Task/CarePlan/Communication)`

Required topic labels:
- `patient-events`
- `care-gap-events`
- `quality-events`
- `action-events`

Connector copy:
- `Normalize and enrich`
- `Publish/subscribe processing`
- `Low-latency context lookup`
- `FHIR-native write-back`

Narrative callout:
- `Designed from high-scale HIE experience and modernized identity patterns.`

### Tab 3: Technical (Mostly Technical)
Title: `Event-Driven FHIR Processing Internals`

Visual language:
- Precise architecture graph.
- Detailed dataflow and control flow.

Required internals:
- Producers and consumers for each topic.
- Consumer groups per service.
- Retry and dead-letter handling:
  - `retry`
  - `dlq`
- Redis roles:
  - `Patient Snapshot Cache`
  - `Rule Context Cache`
  - `Idempotency Keys`
- Observability overlays:
  - processing lag
  - consumer health
  - trace propagation
  - audit events
- Security controls:
  - tenant context propagation
  - PHI-safe logging boundaries
  - API auth boundary at interoperability layer

Connector copy:
- `FHIR ingestion -> canonical event`
- `Topic fan-out by capability`
- `State-aware evaluation with cache + source-of-truth`
- `Deterministic action publishing with idempotency`

## Story and Credibility Layer
Global callout text to include in all tabs:

`Built from real-world HIE operating patterns (Healthix, HealthInfoNet) and modern identity strategy (Verato) to remove custom-interface bottlenecks and activate FHIR-native, event-driven workflows.`

## Presenter Script (Short)
- Non-technical: `Patients move through one continuous workflow. Their data moves with them, and standards keep onboarding fast.`
- Mixed: `Customer FHIR/CDR data is normalized internally, processed through Kafka + services, and returned as FHIR-native actions.`
- Technical: `This is producer-consumer processing with explicit cache roles, idempotency, retry/DLQ, and tenant-safe observability.`

## Deliverables
- Interactive:
  - Tabbed architecture page with 3 views and synchronized step highlighting.
- Static exports:
  - 3 PNG/SVG slides (one per tab)
  - 1 comparison slide showing all three side-by-side
  - 1 legend sheet for terms and icons

## Open Questions
- Final production topic names vs generic labels.
- Exact internal FHIR server naming used in customer-facing materials.
- Whether to show Verato as a separate identity resolution service block or as a cross-cutting capability annotation.

## Next Steps
1. Create wireframe-level layout for all three tabs with locked labels.
2. Produce v1 interactive artifact and export static PNG/SVG set.
3. Run terminology pass for audience-safe wording in sales and product narratives.
4. Move to implementation planning using `/workflows:plan` if you want this built directly in the repo.
