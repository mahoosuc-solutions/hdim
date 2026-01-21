# Compliance Validation with Test Harness

## Overview

This guide explains how to validate compliance tracking across the entire platform while inserting data from the test harness.

## Quick Start

### 1. Start Services

```bash
# Terminal 1: Start backend
cd backend
./gradlew :modules:services:gateway-clinical-service:bootRun

# Terminal 2: Start frontend
cd /home/webemo-aaron/projects/hdim-master
npx nx serve clinical-portal
```

### 2. Run Validation Script

```bash
./scripts/run-compliance-validation.sh
```

This script will:
- ✅ Check service health
- ✅ Enable compliance mode
- ✅ Test compliance endpoint
- ✅ Load test data from test harness
- ✅ Display error statistics

### 3. Monitor Compliance Dashboard

Navigate to: `http://localhost:4200/compliance`

You should see:
- Real-time error tracking
- Error statistics
- Active alerts (if thresholds exceeded)
- Compliance score

---

## Detailed Validation Process

### Step 1: Enable Compliance Mode

Compliance mode disables all fallbacks, ensuring errors are properly tracked.

**Automatic (via script)**:
```bash
./scripts/run-compliance-validation.sh
```

**Manual**:
1. Edit `apps/clinical-portal/src/environments/environment.ts`
2. Set `compliance.disableFallbacks = true`
3. Restart frontend

### Step 2: Load Test Data

The test harness loads FHIR data that will trigger various API calls:

```bash
cd test-harness
./load-fhir-data.sh
```

This will:
- Load FHIR Patient resources
- Trigger care gap detection
- Generate quality measure evaluations
- Create various API calls that may fail

### Step 3: Monitor Errors

**Frontend Dashboard**:
- Navigate to `/compliance`
- Watch errors appear in real-time
- Check alert thresholds

**Backend Logs**:
```bash
# Watch backend logs for sync confirmations
tail -f backend/logs/gateway-clinical-service.log | grep -i compliance
```

**Database**:
```bash
psql -h localhost -p 5435 -U healthdata -d gateway_db

SELECT 
    severity,
    service,
    COUNT(*) as count
FROM compliance_errors
WHERE tenant_id = 'test-tenant'
GROUP BY severity, service
ORDER BY count DESC;
```

### Step 4: Validate Error Sync

Check that errors are syncing to backend:

```bash
# Get error count
curl -X GET "http://localhost:8080/api/v1/compliance/errors?tenantId=test-tenant&size=1" \
  -H "X-Tenant-ID: test-tenant"

# Get error statistics
curl -X GET "http://localhost:8080/api/v1/compliance/errors/stats?tenantId=test-tenant" \
  -H "X-Tenant-ID: test-tenant"
```

---

## Expected Behavior

### During Test Harness Execution

1. **FHIR Data Loading**:
   - May generate errors if services are unavailable
   - Errors tracked automatically via HTTP interceptor

2. **Care Gap Detection**:
   - Calls to care gap service
   - Errors if service unavailable

3. **Quality Measure Evaluation**:
   - CQL engine calls
   - FHIR resource lookups
   - Potential errors if data missing

### Error Categories

Errors will be categorized by:
- **Service**: FHIR Service, Care Gap Service, Quality Measure Service, etc.
- **Severity**: INFO, WARNING, ERROR, CRITICAL
- **Error Code**: ERR-5001 (HTTP 500), ERR-4041 (Not Found), etc.

### Alert Thresholds

Alerts trigger when:
- **Critical Errors**: ≥ 10 (prod) or ≥ 5 (dev)
- **Error Rate**: ≥ 100/hour (prod) or ≥ 50/hour (dev)

---

## Validation Checklist

- [ ] Backend service running
- [ ] Frontend service running
- [ ] Compliance mode enabled
- [ ] Test data loaded successfully
- [ ] Errors appearing in compliance dashboard
- [ ] Errors syncing to backend
- [ ] Error statistics accessible
- [ ] Alerts triggering at thresholds
- [ ] Database storing errors correctly

---

## Troubleshooting

### No Errors Appearing

1. **Check Compliance Mode**:
   ```bash
   # Verify environment.ts has disableFallbacks: true
   grep "disableFallbacks" apps/clinical-portal/src/environments/environment.ts
   ```

2. **Check Error Tracking Enabled**:
   ```bash
   grep "enableErrorTracking" apps/clinical-portal/src/environments/environment.ts
   ```

3. **Check Browser Console**:
   - Open DevTools
   - Check for JavaScript errors
   - Verify API calls are being made

### Errors Not Syncing to Backend

1. **Check Backend Sync Config**:
   ```bash
   grep "syncToBackend" apps/clinical-portal/src/environments/environment.ts
   ```

2. **Check Backend Logs**:
   ```bash
   tail -f backend/logs/gateway-clinical-service.log | grep -i "compliance\|error"
   ```

3. **Test Endpoint Manually**:
   ```bash
   curl -X POST "http://localhost:8080/api/v1/compliance/errors" \
     -H "Content-Type: application/json" \
     -H "X-Tenant-ID: test-tenant" \
     -d '{"errors":[...],"syncedAt":"..."}'
   ```

### Database Not Updating

1. **Check Migration**:
   ```bash
   # Verify table exists
   psql -h localhost -p 5435 -U healthdata -d gateway_db -c "\d compliance_errors"
   ```

2. **Check Liquibase**:
   ```bash
   # Check if migration ran
   tail -f backend/logs/gateway-clinical-service.log | grep -i liquibase
   ```

---

## Test Scenarios

### Scenario 1: Service Unavailable

1. Stop a backend service (e.g., FHIR service)
2. Trigger frontend operations that use that service
3. Verify errors are tracked
4. Check compliance dashboard shows errors

### Scenario 2: Invalid Data

1. Load test data with invalid formats
2. Verify errors are caught
3. Check error messages are descriptive

### Scenario 3: High Error Rate

1. Generate many errors quickly
2. Verify error rate threshold alert triggers
3. Check alert appears in dashboard

### Scenario 4: Critical Errors

1. Generate critical errors (e.g., 500 errors)
2. Verify critical error threshold alert triggers
3. Check alert severity is correct

---

## Success Criteria

✅ **Compliance Mode Active**:
- All fallbacks disabled
- Errors thrown instead of masked

✅ **Error Tracking Working**:
- Errors captured automatically
- Errors stored in localStorage
- Errors synced to backend

✅ **Backend Storage Working**:
- Errors stored in database
- Deduplication working
- Queries return correct data

✅ **Alerts Working**:
- Thresholds trigger alerts
- Alerts displayed in dashboard
- Snackbar notifications shown

✅ **Test Harness Integration**:
- Data loading generates errors
- Errors tracked during load
- No data loss during errors

---

## Next Steps

After validation:

1. **Review Error Patterns**:
   - Identify common error sources
   - Fix underlying issues
   - Improve error handling

2. **Optimize Thresholds**:
   - Adjust based on actual error rates
   - Set appropriate alert levels
   - Configure retention policies

3. **Production Deployment**:
   - Run database migration
   - Configure environment variables
   - Set up monitoring alerts

---

**Status**: Ready for validation
