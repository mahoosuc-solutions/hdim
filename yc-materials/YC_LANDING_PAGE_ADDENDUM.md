# HDIM Landing Page Addendum (Current-State Messaging)

## Core Value
- Real-time quality measurement that fits into existing provider workflows.
- Point-of-care signals instead of overnight batch reporting.
- Faster measure iteration through template-driven CQL.

## Integration and Trust
- Designed to overlay existing systems using a Kong API Gateway front door.
- Standards-first: FHIR R4, CQL, and IHE-aligned patterns.
- Multi-tenant isolation and audit logging for healthcare environments.

## Intelligence Delivered
- Care-gap detection and quality score APIs.
- Patient-level reports and population reporting exports.
- Risk scoring endpoints and event-driven alerting hooks.
- Consent, audit logging, and usage tracking to support compliance and oversight.

## Proof Points (Current State)
- 26 backend microservices implemented (validated in `backend/modules/services`, excluding build artifacts).
- Operational HEDIS measures in demo: CDC, CBP, COL, BCS.
- Sub-second demo evaluations on sample data.
- Docker-based demo environment running locally.

## Founder Credibility
- Built by a founder with direct provider integration experience at HealthInfoNet, Healthix, and Verato.
- Designed to plug in, not replace, existing stacks.

## What to Avoid Claiming (Until Verified)
- "All 52 HEDIS measures live in production."
- "Guaranteed <200ms at scale."
- "Full production readiness without final validation."
