# Demo Readiness Gate

This gate defines what must be true before we record screenshots and videos that represent the platform publicly.

## Goals

- Prove the platform works end-to-end from a clean start.
- Showcase validated differentiators with minimal explanation required.
- Ensure the system is stable enough to record without “demo gremlins”.

## Core Differentiators To Communicate

This is the “why we exist” set. Every demo should show at least 3 of these clearly.

1. **Predictive care gaps (30-60 day look-ahead)**
   - Show the dashboard surfacing upcoming risk and the path to action.
2. **AI context over raw lists**
   - Prefer narrative/contextual explanations (where implemented) over “here’s a spreadsheet”.
3. **FHIR-native interoperability, ECDS-ready posture**
   - Show `/fhir/metadata` and a concrete read path (Patient/MeasureReport/etc).
4. **Quality + Care Gap execution loop**
   - Measures evaluated, care gaps surfaced, work prioritized.
5. **Multi-tenant, secure gateway entrypoint**
   - Tenant context enforced; protected APIs behind the gateway.
6. **Auditability and compliance posture**
   - Audit log visibility, exports, and “who did what” traceability.

Note: Real-time financial ROI dashboards are a market-validated differentiator, but only demo them when verified end-to-end in the current build. Otherwise position as preview/roadmap.

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

- 1 flow from **Predictive care gaps**
- 1 flow from **Care gaps execution loop**
- 1 flow from **FHIR-native proof point**
- 1 flow from **Audit/Export** (or Events, if you can show it cleanly)

### D. Performance Baseline (Demo-Level)

- First page interactive in < 5 seconds on demo hardware.
- Core list pages usable without stutter (patients/care-gaps).

## Fail Conditions (Stop Recording)

- Seeding fails or produces inconsistent results between runs.
- Authentication/tenant context is confusing or inconsistent.
- Any hero flow requires “handwaving” due to broken UI/API.
