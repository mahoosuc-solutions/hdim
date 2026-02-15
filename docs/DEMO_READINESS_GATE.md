# Demo Readiness Gate

This gate defines what must be true before we record screenshots and videos that represent the platform publicly.

## Goals

- Prove the platform works end-to-end from a clean start.
- Showcase differentiators with minimal explanation required.
- Ensure the system is stable enough to record without “demo gremlins”.

## Core Differentiators To Communicate

This is the “why we exist” set. Every demo should show at least 3 of these clearly.

1. **Multi-tenant, secure gateway entrypoint**
   - Tenant headers enforced, protected APIs behind the gateway.
2. **FHIR-native interoperability**
   - FHIR endpoints and metadata; realistic patient artifacts.
3. **Quality + Care Gap execution loop**
   - Measures evaluated, care gaps surfaced, work prioritized.
4. **Event-driven platform behaviors**
   - Events flowing through Kafka, services reacting (where applicable).
5. **Auditability and compliance posture**
   - Audit log visibility, exports, and “who did what” traceability.
6. **Agent-enabled workflows**
   - AI/agent services integrated as first-class modules (where enabled).

## Demo Readiness Checklist (Must Pass)

### A. Boot + Seed

- Demo stack boots from scratch:
  - `docker compose -f docker-compose.demo.yml up -d`
- Seeding completes:
  - `./scripts/seed-all-demo-data.sh`
- System checks pass:
  - `./validate-system.sh`

### B. UX Stability

- Key UI routes render without console errors:
  - Portal landing and role dashboards
  - Patients list/details
  - Care gaps list/details
  - Quality measures / reports (as applicable)
- No broken navigation or empty states that look like failures.

### C. Differentiator Proof Points (Pick Your “Hero Set”)

Minimum for public recording:

- 1 flow from **Quality/Care Gaps**
- 1 flow from **FHIR + Patient context**
- 1 flow from **Audit/Export** or **Events**

### D. Performance Baseline (Demo-Level)

- First page interactive in < 5 seconds on demo hardware.
- Core list pages usable without stutter (patients/care-gaps).

## Fail Conditions (Stop Recording)

- Seeding fails or produces inconsistent results between runs.
- Authentication/tenant context is confusing or inconsistent.
- Any hero flow requires “handwaving” due to broken UI/API.

