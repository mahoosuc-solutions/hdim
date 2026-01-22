# Comprehensive Next Steps - Service Resolution Complete

**Date**: January 15, 2026  
**Status**: ✅ **FIXES APPLIED - AWAITING VALIDATION**

---

## Executive Summary

Successfully identified and fixed 2 critical service startup issues:
1. ✅ Patient Service: Added audit module changelog inclusion
2. ✅ Gateway Service: Created migration for missing token column

Services are rebuilding and will restart with fixes. Migrations will execute automatically.

---

## Issues Fixed

### ✅ Fix 1: Patient Service - Audit Tables

**Problem**: Missing audit tables (`ai_agent_decision_events`, `qa_reviews`, etc.)

**Solution Applied**:
- Updated `db.changelog-master.xml` to include audit module changelog
- File: `backend/modules/services/patient-service/src/main/resources/db/changelog/db.changelog-master.xml`
- Added: `<include file="audit/db/changelog/db.changelog-master.xml"/>`

**Status**: ✅ Fix applied, service rebuilding

---

### ✅ Fix 2: Gateway Service - Token Column

**Problem**: Missing `token` column in `refresh_tokens` table

**Solution Applied**:
- Created migration: `0002-add-refresh-token-column.xml`
- Added `token VARCHAR(1000) UNIQUE` column
- Added index for token column
- Updated master changelog

**Status**: ✅ Fix applied, service rebuilding

---

## Current Status

### Services
- **Patient Service**: 🔄 Rebuilding with audit changelog
- **Gateway Service**: 🔄 Rebuilding with token column migration
- **FHIR Service**: 🔄 Starting normally
- **Notification Service**: 🔄 Starting normally

### Databases
- **patient_db**: ✅ 7 tables (patient tables created)
- **gateway_db**: ✅ 3 changesets executed
- **fhir_db**: ✅ 30 tables
- **notification_db**: ✅ 5 tables

---

## Next Steps (In Order)

### Step 1: Verify Services Started (5 min)

**Wait 2-3 minutes for services to fully start, then check**:

```bash
# Check service status
docker compose ps | grep -E "(gateway|patient|fhir|notification)"

# Expected: All should show "healthy" or at least "Up" without errors
```

**If services show errors**:
- Check logs: `docker compose logs <service-name> --tail=50`
- Look for migration errors or schema validation issues

---

### Step 2: Verify Migrations Executed (5 min)

**Check Patient Service**:
```bash
# Verify audit tables created
docker exec healthdata-postgres psql -U healthdata -d patient_db -c "\dt" | grep -E "(qa_|ai_|configuration|user_config|data_quality|clinical|mpi)"

# Check changelog execution
docker exec healthdata-postgres psql -U healthdata -d patient_db -c "SELECT id, filename FROM databasechangelog WHERE filename LIKE '%audit%' ORDER BY dateexecuted DESC;"
```

**Expected**: 
- 7+ audit tables should exist
- Changelog should show audit migrations executed

**Check Gateway Service**:
```bash
# Verify token column exists
docker exec healthdata-postgres psql -U healthdata -d gateway_db -c "\d refresh_tokens" | grep -E "token[^_]"

# Check changelog
docker exec healthdata-postgres psql -U healthdata -d gateway_db -c "SELECT id, filename FROM databasechangelog WHERE id LIKE '%refresh%' ORDER BY dateexecuted DESC;"
```

**Expected**:
- `refresh_tokens` table should have both `token` and `token_hash` columns
- Changelog should show `0002-add-refresh-token-column` executed

---

### Step 3: Test Service Health (5 min)

```bash
# Test all service health endpoints
curl -s http://localhost:8087/actuator/health | jq .status
curl -s http://localhost:8084/patient/actuator/health | jq .status
curl -s http://localhost:8085/fhir/actuator/health | jq .status
curl -s http://localhost:8107/notification/actuator/health | jq .status
```

**Expected**: All return `"UP"` or `{"status":"UP"}`

---

### Step 4: Re-run Integration Tests (10 min)

```bash
cd backend
./gradlew :modules:shared:infrastructure:audit:test --tests "*IntegrationTest" --no-daemon
```

**Expected**: 
- All 14 tests should pass
- Tables now exist for test execution

**If Tests Still Fail**:
- Check specific error messages
- Verify table structure matches entities
- Check repository query syntax

---

### Step 5: Test API Endpoints (10 min)

**FHIR Service**:
```bash
curl -s http://localhost:8085/fhir/metadata | jq .resourceType
# Expected: "CapabilityStatement"
```

**Patient Service**:
```bash
curl -s -H "X-Tenant-ID: test-tenant" http://localhost:8084/api/v1/patients?page=0&size=10
# Expected: JSON response with patient list (may be empty if no data)
```

**Gateway Service**:
```bash
curl -s http://localhost:8087/actuator/health
# Expected: {"status":"UP"}
```

**Notification Service**:
```bash
curl -s http://localhost:8107/notification/actuator/health
# Expected: {"status":"UP"}
```

---

### Step 6: Run Validation Scripts (5 min)

```bash
cd /home/webemo-aaron/projects/hdim-master
DB_PASSWORD=healthdata_password ./scripts/validate-database-schema.sh
```

**Expected**: All tables show as existing

---

## Troubleshooting

### If Patient Service Still Fails

**Possible Issues**:
1. Audit changelog path not resolving
2. Migration files not in classpath
3. Changelog syntax error

**Solutions**:
1. **Check if files are in JAR**: 
   ```bash
   docker exec healthdata-patient-service ls -la /app/BOOT-INF/classes/audit/db/changelog/
   ```

2. **Alternative**: Copy audit migration files directly to patient service changelog directory:
   ```bash
   cp backend/modules/shared/infrastructure/audit/src/main/resources/audit/db/changelog/0002-*.xml \
      backend/modules/services/patient-service/src/main/resources/db/changelog/
   # Repeat for 0003-0008
   ```
   Then update master changelog to include individual files

3. **Manual Migration**: If automatic fails, execute migrations manually using Liquibase CLI

---

### If Gateway Service Still Fails

**Possible Issues**:
1. Migration file not found
2. Migration didn't execute
3. Column already exists (if manually added)

**Solutions**:
1. **Check if migration file exists in JAR**
2. **Verify changelog includes new migration**
3. **Manually add column if needed**:
   ```sql
   ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token VARCHAR(1000) UNIQUE;
   CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
   ```

---

## Success Indicators

✅ **All Services Healthy**: 4/4 services show healthy status  
✅ **All Tables Created**: 
   - Patient DB: 7+ audit tables exist
   - Gateway DB: `token` column exists in `refresh_tokens`  
✅ **Migrations Executed**: Changelog shows new changesets executed  
✅ **Integration Tests Pass**: 14/14 tests passing  
✅ **API Endpoints Work**: All health endpoints return 200 OK

---

## Estimated Timeline

- **Service Startup**: 2-5 minutes
- **Migration Execution**: 1-2 minutes
- **Health Verification**: 2-3 minutes
- **Integration Tests**: 2-3 minutes
- **API Testing**: 5-10 minutes

**Total**: 15-25 minutes to fully operational

---

**Status**: ✅ **FIXES APPLIED - MONITORING STARTUP**  
**Next**: Verify services start successfully and migrations execute
