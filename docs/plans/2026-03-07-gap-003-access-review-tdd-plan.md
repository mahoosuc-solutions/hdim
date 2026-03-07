# GAP-003 TDD Plan: Access Recertification

## Gap
- **ID:** GAP-003
- **Domain:** Access Governance
- **Current State:** Missing current-cycle access recertification evidence.
- **Target Artifact:** `docs/compliance/ACCESS_REVIEW_2026-03-07.md`

## High-Level Requirements
1. Enumerate all privileged roles and active principals.
2. Validate least-privilege alignment for each privileged account.
3. Identify stale/inactive/orphaned accounts and define remediation actions.
4. Produce approver sign-off per domain owner.

## TDD Strategy
### Test Suite (red -> green)
1. `access-inventory-present`
- Assert report contains user/role inventory table with owner and last activity.
2. `least-privilege-deviations-tracked`
- Assert each deviation has ticket, owner, due date, and risk level.
3. `stale-account-handling`
- Assert stale accounts are either disabled or have approved exception.
4. `approval-signoff`
- Assert security lead and domain owner sign-offs are recorded.

### Proposed Automation
- Add script: `scripts/release-validation/validate-access-review-evidence.sh`
- Script checks required sections and closure metadata.

## Implementation Steps
1. Extract role/account inventory from auth source of truth.
2. Run review workshop with service/domain owners.
3. Record deviations and remediation plan.
4. Publish evidence report.
5. Run evidence validator and attach log.

## Success Criteria
- Access review artifact published with complete inventory and decisions.
- All critical deviations closed or exception-approved.
- Evidence validator passes.
- `GAP-003` updated to `Closed`.
