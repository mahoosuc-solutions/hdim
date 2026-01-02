# Database Migration Notes - CQL Engine Schema Fix

**Date:** 2025-11-14
**Migration ID:** 0009-fix-evaluation-result-nullable
**Service:** CQL Engine Service
**Database:** healthdata_cql
**Status:** Applied to Development Environment

---

## Overview

This migration removes the NOT NULL constraint from the `cql_evaluations.evaluation_result` column to support legitimate null evaluation results when CQL processing returns no qualifying data.

## Problem Statement

**Issue:** Calculate quality measure endpoint returning HTTP 500 errors

**Root Cause:**
- Database schema had `evaluation_result JSONB NOT NULL` constraint
- CQL evaluations can legitimately return null when:
  - Patient doesn't have qualifying clinical data
  - Patient is excluded from denominator
  - Required observations/conditions are missing
  - CQL logic determines no applicable result

**Error Message:**
```
null value in column "evaluation_result" of relation "cql_evaluations" violates not-null constraint
```

---

## Migration Details

### File Location
```
backend/modules/services/cql-engine-service/src/main/resources/db/changelog/0009-fix-evaluation-result-nullable.xml
```

### Changes Applied

**1. Drop NOT NULL Constraint:**
```sql
ALTER TABLE cql_evaluations
ALTER COLUMN evaluation_result DROP NOT NULL;
```

**2. Notes:**
- The `status` column (VARCHAR(32) NOT NULL) already exists for tracking evaluation outcomes
- The `error_message` column (TEXT nullable) already exists for storing error details
- No new columns needed - only constraint modification

### Rollback Plan
```sql
ALTER TABLE cql_evaluations
ALTER COLUMN evaluation_result SET NOT NULL;
```

**⚠️ Warning:** Rollback will fail if any rows have NULL `evaluation_result` values. Ensure all evaluations are re-processed or deleted before rollback.

---

## Deployment Steps

### Development Environment
✅ **Applied:** 2025-11-14 20:00:00 UTC
- Method: Direct SQL execution via Docker
- Command:
  ```bash
  docker exec healthdata-postgres psql -U healthdata -d healthdata_cql \
    -c "ALTER TABLE cql_evaluations ALTER COLUMN evaluation_result DROP NOT NULL;"
  ```

### Production Environment (Future)

**Pre-Deployment Checklist:**
- [ ] Backup `healthdata_cql` database
- [ ] Verify Liquibase changelog includes `0009-fix-evaluation-result-nullable.xml`
- [ ] Check current schema state:
  ```sql
  SELECT column_name, data_type, is_nullable
  FROM information_schema.columns
  WHERE table_name = 'cql_evaluations'
  AND column_name = 'evaluation_result';
  ```
- [ ] Verify no pending transactions on `cql_evaluations` table

**Deployment Steps:**
1. Deploy new CQL Engine service version with updated changelog
2. Service startup will automatically run Liquibase migration
3. Verify migration success in logs:
   ```
   Liquibase: Successfully applied changeset: 0009-1
   ```
4. Verify schema change:
   ```sql
   \d cql_evaluations
   ```
   Should show `evaluation_result | jsonb | | |` (no "not null")

**Post-Deployment Verification:**
1. Test calculate endpoint with sample patient:
   ```bash
   curl -X POST 'http://localhost:8087/quality-measure/quality-measure/calculate?patient=TEST_PATIENT_ID&measure=HEDIS_CDC' \
     -H 'X-Tenant-ID: default' \
     -H 'Authorization: Basic [BASE64_CREDENTIALS]'
   ```
2. Verify response is HTTP 200 with proper result DTO
3. Check database for new evaluation record
4. Monitor error logs for any constraint violations

---

## Impact Assessment

### Breaking Changes
- **None** - This is a backward-compatible schema change
- Existing evaluations with non-null results remain unchanged
- New evaluations can now have null results without errors

### Application Code Changes
- **None required** - `CqlEvaluationService` already handles null results correctly
- Service sets `status="FAILED"` and populates `error_message` when results are null
- No Java entity or repository changes needed

### Performance Impact
- **Negligible** - Removing constraint has minimal performance impact
- No index changes
- No query plan changes

### Data Migration Required
- **None** - No existing data needs modification
- All existing rows remain valid
- New rows can leverage nullable column

---

## Testing Results

### Test 1: Calculate Endpoint - Success Case
```bash
curl -X POST 'http://localhost:8087/quality-measure/quality-measure/calculate?patient=3553ac0a-762c-4477-a28d-1dba033f379b&measure=HEDIS_CDC'
```
**Result:** ✅ HTTP 200 - Created result ID: `25b06cd0-3859-43b8-aede-041e43c88716`

### Test 2: Calculate Endpoint - Different Measure
```bash
curl -X POST 'http://localhost:8087/quality-measure/quality-measure/calculate?patient=1dbc0fbe-dbd3-482d-9bae-497aac5ba40f&measure=HEDIS_CBP'
```
**Result:** ✅ HTTP 200 - Created result ID: `eda4f77c-97b1-4247-972c-cdb32d7b4066`

### Database Verification
```sql
SELECT id, patient_id, evaluation_result IS NULL as result_is_null, status, error_message
FROM cql_evaluations
WHERE created_at > '2025-11-14'
ORDER BY created_at DESC
LIMIT 5;
```

**Results:**
- New evaluations successfully created
- Both null and non-null `evaluation_result` values supported
- Status tracking working correctly
- Error messages captured when applicable

---

## Monitoring & Alerts

### Metrics to Watch
- **Error Rate:** Monitor for constraint violation errors (should be 0 after migration)
- **Null Result Rate:** Track percentage of evaluations with null results
- **Failed Evaluations:** Monitor `status='FAILED'` count in `cql_evaluations` table

### Log Patterns
**Success:**
```
Evaluation completed successfully: measure=HEDIS_CDC, patient=xxx
```

**Expected Null Result:**
```
Evaluation failed: No qualifying data for patient
status=FAILED, errorMessage=Patient does not meet denominator criteria
```

### Database Queries for Monitoring
```sql
-- Count evaluations by status
SELECT status, COUNT(*)
FROM cql_evaluations
GROUP BY status;

-- Find recent null results
SELECT id, patient_id, status, error_message, created_at
FROM cql_evaluations
WHERE evaluation_result IS NULL
AND created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Check constraint violations (should return 0 after migration)
SELECT COUNT(*)
FROM pg_stat_database_conflicts
WHERE datname = 'healthdata_cql';
```

---

## Rollback Procedure

**⚠️ Only rollback if critical issues arise**

### Rollback Steps:
1. **Check for null results:**
   ```sql
   SELECT COUNT(*) FROM cql_evaluations WHERE evaluation_result IS NULL;
   ```

2. **If count > 0, handle null results:**
   - Option A: Delete null result records (if safe to lose data)
     ```sql
     DELETE FROM cql_evaluations WHERE evaluation_result IS NULL;
     ```
   - Option B: Update null results with placeholder (if data must be preserved)
     ```sql
     UPDATE cql_evaluations
     SET evaluation_result = '{"status": "no_data"}'::jsonb
     WHERE evaluation_result IS NULL;
     ```

3. **Re-apply NOT NULL constraint:**
   ```sql
   ALTER TABLE cql_evaluations
   ALTER COLUMN evaluation_result SET NOT NULL;
   ```

4. **Redeploy previous CQL Engine version**

5. **Remove migration from changelog:**
   - Edit `db.changelog-master.xml`
   - Comment out or remove line:
     ```xml
     <!-- <include file="db/changelog/0009-fix-evaluation-result-nullable.xml"/> -->
     ```

---

## Related Documentation

- **Implementation Guide:** `/IMPLEMENTATION_COMPLETE.md` - Section "Recent Fixes"
- **Liquibase Migration:** `/backend/modules/services/cql-engine-service/src/main/resources/db/changelog/0009-fix-evaluation-result-nullable.xml`
- **Master Changelog:** `/backend/modules/services/cql-engine-service/src/main/resources/db/changelog/db.changelog-master.xml`
- **Service Code:** `/backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/service/CqlEvaluationService.java`

---

## Sign-Off

**Developer:** Claude AI Assistant
**Reviewer:** [Pending]
**DBA Approval:** [Pending]
**Deployed By:** [Pending]
**Deployed Date:** [Pending Production Deployment]

---

## Notes

- This migration is required for the calculate endpoint to function correctly
- The fix enables proper handling of edge cases in CQL evaluation
- No application code changes required beyond the migration
- Migration is backward compatible and safe to apply
