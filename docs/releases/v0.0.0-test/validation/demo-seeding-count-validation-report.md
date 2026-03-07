# Demo Seeding Count Validation Report

- **Timestamp (UTC):** 2026-03-06T16:16:41Z
- **Version:** v0.0.0-test
- **Status:** FAILED
- **Tenants:** `summit-care-2026,valley-health-2026`
- **Expected Patients:** `summit-care-2026=1200,valley-health-2026=1200`
- **Expected Care Gap Ranges:** `not-set`

## Command Output

```text
Tenant                 | Patients     | Care Gaps    | Patient OK   | CareGap OK  
------                 | --------     | --------     | ----------   | ----------  
summit-care-2026       | 200          | 91           | no           | n/a         
valley-health-2026     | 0            | 0            | no           | n/a         

Expected count mismatch detected.
EXPECTED_PATIENTS_PER_TENANT=
EXPECTED_PATIENTS_BY_TENANT=summit-care-2026=1200,valley-health-2026=1200
EXPECTED_CARE_GAPS_PER_TENANT=
EXPECTED_CARE_GAPS_BY_TENANT=
EXPECTED_CARE_GAPS_RANGE=
EXPECTED_CARE_GAPS_RANGE_BY_TENANT=
```
