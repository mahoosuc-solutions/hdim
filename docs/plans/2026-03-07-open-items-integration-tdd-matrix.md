# Open Items Integration TDD Matrix (2026-03-07)

## Scope
Remaining open items from `docs/compliance/GAP_REGISTER_2026-03-07.md`:
- GAP-002 (DR Recovery Validation)
- GAP-003 (Access Recertification)
- GAP-004 (Third-Party Risk Register)

## GAP-002: DR Recovery Validation

### High-Level Requirements
1. Execute a recovery drill against current release lane.
2. Capture measured RTO/RPO with timestamps.
3. Validate post-recovery platform health and security posture.
4. Record approvals from SRE and Release Manager.

### TDD Artifacts
- Plan: `docs/plans/2026-03-07-gap-002-dr-validation-tdd-plan.md`
- Evidence doc: `docs/compliance/DR_TEST_RESULTS_2026-03-07.md`
- Validator: `scripts/release-validation/validate-dr-evidence.sh`
- Log output: `test-results/validate-dr-evidence-2026-03-07.log`

### Platform Integration Points
- `scripts/release-validation/validate-release-preflight.sh`
- Release evidence path: `docs/releases/v0.0.0-test/validation/`
- Compliance index: `docs/compliance/EVIDENCE_INDEX_2026-03-07.md`

### Success Criteria
- Validator returns PASS.
- DR evidence has non-TBD RTO/RPO and threshold decision.
- Post-recovery checks show pass.
- `GAP-002` updated to Closed with evidence link.

## GAP-003: Access Recertification

### High-Level Requirements
1. Produce current privileged account and role inventory.
2. Identify least-privilege deviations and stale/orphaned access.
3. Record remediation actions with owners and due dates.
4. Capture security and domain sign-off.

### TDD Artifacts
- Plan: `docs/plans/2026-03-07-gap-003-access-review-tdd-plan.md`
- Evidence doc: `docs/compliance/ACCESS_REVIEW_2026-03-07.md`
- Validator: `scripts/release-validation/validate-access-review-evidence.sh`
- Log output: `test-results/validate-access-review-evidence-2026-03-07.log`

### Platform Integration Points
- AuthN/AuthZ governance controls in compliance matrix.
- Gap and evidence tracking documents.

### Success Criteria
- Validator returns PASS.
- No unresolved critical access deviations without approved exception.
- Required sign-offs recorded.
- `GAP-003` updated to Closed with evidence link.

## GAP-004: Third-Party Risk Register

### High-Level Requirements
1. Publish current vendor inventory supporting platform operations.
2. Map BAA/DPA/SOC2 (or explicit N/A rationale) for each vendor.
3. Assign risk rating, owner, and next review date.
4. Record exceptions and approvals.

### TDD Artifacts
- Plan: `docs/plans/2026-03-07-gap-004-third-party-risk-tdd-plan.md`
- Evidence doc: `docs/compliance/THIRD_PARTY_RISK_REGISTER_2026-03-07.md`
- Validator: `scripts/release-validation/validate-third-party-risk-evidence.sh`
- Log output: `test-results/validate-third-party-risk-evidence-2026-03-07.log`

### Platform Integration Points
- `docs/compliance/THIRD_PARTY_NOTICES.md`
- `docs/compliance/PROVIDER-COMPLIANCE-CHECKLIST.md`
- Compliance control matrix and evidence index

### Success Criteria
- Validator returns PASS.
- No unowned high-risk third-party findings.
- Compliance/security sign-offs recorded.
- `GAP-004` updated to Closed with evidence link.

## Execution Order (Recommended)
1. GAP-002 DR evidence completion and validation.
2. GAP-003 access review completion and validation.
3. GAP-004 third-party risk completion and validation.
4. Re-run:
   - `scripts/release-validation/validate-upstream-ci-gates.sh`
   - `scripts/release-validation/validate-regulatory-readiness.sh v0.0.0-test`

## Exit Criteria For Full Integration
- GAP-002, GAP-003, GAP-004 all set to Closed.
- All three evidence validators pass.
- Regulatory readiness remains GO after closure updates.
