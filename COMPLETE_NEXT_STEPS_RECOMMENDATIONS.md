# Complete Next Steps Recommendations

**Date**: January 15, 2026  
**Status**: ✅ **FIXES APPLIED - READY FOR VALIDATION**

---

## Summary of Fixes Applied

### ✅ Fix 1: Patient Service Audit Tables
**File**: `backend/modules/services/patient-service/src/main/resources/db/changelog/db.changelog-master.xml`
**Change**: Added `<include file="audit/db/changelog/db.changelog-master.xml"/>`
**Result**: Will create 7 missing audit tables on next startup

### ✅ Fix 2: Gateway Service Token Column
**File**: `backend/modules/services/gateway-service/src/main/resources/db/changelog/0002-add-refresh-token-column.xml` (NEW)
**Change**: Created migration to add `token` column to `refresh_tokens` table
**Result**: Will add missing column on next startup

---

## Immediate Next Steps (Next 15 Minutes)

### 1. Verify Services Started Successfully (5 min)

**Commands**:
```bash
# Check service status
docker compose ps | grep -E "(gateway|patient|fhir|notification)"

# Check service logs for errors
docker compose logs patient-service --tail=50 | grep -E "(ERROR|Exception|Started)"
docker compose logs gateway-service --tail=50 | grep -E "(ERROR|Exception|Started)"
```

**Expected**:
- All services show "healthy" or "starting" (not "unhealthy")
- No ERROR or Exception messages in recent logs
- Services show "Started" message

---

### 2. Verify Migrations Executed (5 min)

**Commands**:
```bash
# Check patient_db for audit tables
docker exec healthdata-postgres psql -U healthdata -d patient_db -c "\dt" | grep -E "(qa_|ai_|configuration|user_config|data_quality|clinical|mpi)"

# Check gateway_db for token column
docker exec healthdata-postgres psql -U healthdata -d gateway_db -c "\d refresh_tokens" | grep token
```

**Expected**:
- Patient DB: 7+ audit tables (qa_reviews, ai_agent_decision_events, etc.)
- Gateway DB: `refresh_tokens` table has both `token` and `token_hash` columns

---

### 3. Test Service Health Endpoints (5 min)

**Commands**:
```bash
# Test gateway service
curl -s http://localhost:8087/actuator/health | jq .

# Test patient service
curl -s http://localhost:8084/patient/actuator/health | jq .

# Test FHIR service
curl -s http://localhost:8085/fhir/actuator/health | jq .

# Test notification service
curl -s http://localhost:8107/notification/actuator/health | jq .
```

**Expected**: All return `{"status":"UP"}` or similar healthy status

---

## Short Term Next Steps (Next 30 Minutes)

### 4. Re-run Integration Tests (10 min)

**Command**:
```bash
cd backend
./gradlew :modules:shared:infrastructure:audit:test --tests "*IntegrationTest" --no-daemon
```

**Expected**: All 14 tests pass (tables now exist)

**If Tests Fail**:
- Check specific error messages
- Verify tables were created correctly
- Check repository query syntax

---

### 5. Test Critical API Endpoints (10 min)

**FHIR Service**:
```bash
# Test FHIR metadata endpoint
curl -s http://localhost:8085/fhir/metadata | jq .resourceType

# Test patient search (if data exists)
curl -s "http://localhost:8085/fhir/Patient?name=test" | jq .
```

**Patient Service**:
```bash
# Test patient list endpoint
curl -s -H "X-Tenant-ID: test-tenant" http://localhost:8084/api/v1/patients | jq .
```

**Gateway Service**:
```bash
# Test gateway health
curl -s http://localhost:8087/actuator/health | jq .
```

**Notification Service**:
```bash
# Test notification health
curl -s http://localhost:8107/notification/actuator/health | jq .
```

---

### 6. Run Validation Scripts (10 min)

**Command**:
```bash
cd /home/webemo-aaron/projects/hdim-master
DB_PASSWORD=healthdata_password ./scripts/validate-database-schema.sh
```

**Expected**: All tables show as existing

---

## Long Term Next Steps (Next Session)

### 7. Complete Integration Testing
- Run all integration tests
- Fix any remaining issues
- Document test results

### 8. Performance Validation
- Test query performance with indexes
- Verify tenant isolation
- Test concurrent access

### 9. Documentation
- Update deployment documentation
- Document database schema
- Create runbook for operations

---

## Troubleshooting Guide

### If Patient Service Still Fails

**Check**:
1. Verify audit changelog path is correct
2. Check if audit module migrations are in classpath
3. Verify Liquibase can find the changelog file

**Fix**:
- Ensure path is relative to classpath root
- Check `application.yml` Liquibase configuration
- Verify changelog file exists in built JAR

### If Gateway Service Still Fails

**Check**:
1. Verify migration executed (check databasechangelog table)
2. Check if column was added successfully
3. Verify entity matches database schema

**Fix**:
- Manually add column if migration didn't run: `ALTER TABLE refresh_tokens ADD COLUMN token VARCHAR(1000) UNIQUE;`
- Or update entity to match existing schema

---

## Success Indicators

✅ **All Services Healthy**: 4/4 services show healthy status  
✅ **All Tables Created**: Audit tables exist in patient_db  
✅ **Token Column Added**: refresh_tokens has token column  
✅ **Integration Tests Pass**: 14/14 tests passing  
✅ **API Endpoints Work**: All health endpoints return 200 OK

---

**Status**: ✅ **FIXES APPLIED - READY FOR VALIDATION**  
**Next**: Monitor service startup and verify fixes
