---
status: ready
priority: p1
issue_id: "001"
tags: [seeding, fhir, validation]
dependencies: []
---

# Verify Seeding Record Counts Across FHIR + Care Gap

## Problem Statement
We need an authoritative verification that demo seeding populated the expected record counts across FHIR and care-gap services, to ensure end-to-end data completeness.

## Findings
- Demo seeding service was previously unhealthy while FHIR was down.
- FHIR is now healthy; seeding service restarted and healthy.

## Proposed Solutions
- Option A: Add a CLI verification script (preferred for repeatable checks).
- Option B: Add a UI status panel that queries FHIR and care-gap counts.
- Option C: Add a post-seed validation task in ops-service.

## Recommended Action
Implement a repeatable verification checklist using existing endpoints and add a small helper script to query FHIR and care-gap counts per tenant. Document the exact commands and expected ranges.

## Acceptance Criteria
- [ ] Verified FHIR counts match expected patient count per tenant
- [ ] Verified care-gap counts match expected per tenant
- [ ] Documented verification commands or scripts

## Work Log

### 2026-02-11 - Created

**By:** Codex

**Actions:**
- Logged the validation gap

**Learnings:**
- TBD

### 2026-02-11 - Added Verification Script

**By:** Codex

**Actions:**
- Added `scripts/verify-seeding-counts.sh` and documented usage in `scripts/README.md`.
- Captured current counts:
  - `summit-care-2026`: Patients `3100`, Care Gaps `1437`
  - `valley-health-2026`: Patients `1300`, Care Gaps `998`
  - `acme-health`: Patients `5508`, Care Gaps `706`
- Ran verification with expected 10% target (120 patients/tenant) and found mismatches for all tenants.

**Learnings:**
- FHIR `_summary=count` still returns entries; script reads `total` from JSON.
- Care gap API base path is `/care-gap/api/v1/care-gaps`.
