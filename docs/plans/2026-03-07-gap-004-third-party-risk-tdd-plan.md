# GAP-004 TDD Plan: Third-Party Risk Register

## Gap
- **ID:** GAP-004
- **Domain:** Third-Party Risk
- **Current State:** Missing current-cycle vendor risk register with BAA/DPA/SOC mapping.
- **Target Artifact:** `docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md`

## High-Level Requirements
1. Maintain complete inventory of external vendors/services handling platform data.
2. Track contractual/compliance status per vendor (BAA, DPA, SOC2/ISO, data residency).
3. Document risk rating, compensating controls, and renewal/review dates.
4. Identify blockers and exceptions requiring leadership approval.

## TDD Strategy
### Test Suite (red -> green)
1. `vendor-inventory-complete`
- Assert all production-relevant vendors are listed with owner and service category.
2. `compliance-artifacts-mapped`
- Assert BAA/DPA/SOC fields are present with evidence links or explicit N/A rationale.
3. `risk-treatment-defined`
- Assert each medium/high vendor risk has mitigation or exception path.
4. `review-cadence-recorded`
- Assert next review date and accountable owner exist per vendor.

### Proposed Automation
- Add script: `scripts/release-validation/validate-third-party-risk-evidence.sh`
- Script checks schema completeness and unresolved high-risk exceptions.
 - Current red-state proof:
   - `test-results/validate-third-party-risk-evidence-2026-03-07.log` (failing as expected before evidence completion)

## Implementation Steps
1. Build vendor inventory from infra, code, and procurement inputs.
2. Attach compliance evidence links (or file references).
3. Assign risk ratings and treatment actions.
4. Publish report and run validator.

## Success Criteria
- Third-party risk register published with complete required fields.
- No unowned high-risk findings.
- Evidence validator passes.
- `GAP-004` updated to `Closed`.
