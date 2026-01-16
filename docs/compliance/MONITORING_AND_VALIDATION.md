# Compliance System Monitoring and Validation

## Overview

This document describes how to monitor and validate the compliance error tracking system in real-time.

---

## Quick Start

### 1. End-to-End Validation

Run the comprehensive validation script:

```bash
./scripts/validate-compliance-end-to-end.sh
```

**What it checks:**
- ✅ Database connectivity
- ✅ Table existence and structure
- ✅ Backend API health
- ✅ Frontend accessibility
- ✅ Column validation
- ✅ Index validation
- ✅ Insert/Query functionality
- ✅ JSONB support
- ✅ Current database state

### 2. Real-Time Monitoring

Start the database monitor:

```bash
./scripts/monitor-compliance-database.sh
```

**What it shows:**
- Real-time error count
- New errors detected
- Summary statistics
- Recent errors (last 10)
- Errors by service
- Errors by tenant

---

## Monitoring Scripts

### `validate-compliance-end-to-end.sh`

Comprehensive validation of the entire compliance system.

**Usage:**
```bash
./scripts/validate-compliance-end-to-end.sh
```

**Configuration (Environment Variables):**
```bash
export DB_CONTAINER=hdim-demo-postgres
export DB_NAME=gateway_db
export DB_USER=healthdata
export GATEWAY_URL=http://localhost:8080
export FRONTEND_URL=http://localhost:4200
```

**Output:**
- Detailed test results for each component
- Current database state
- Next steps for system activation

### `monitor-compliance-database.sh`

Real-time monitoring of the compliance_errors table.

**Usage:**
```bash
./scripts/monitor-compliance-database.sh
```

**Configuration (Environment Variables):**
```bash
export DB_CONTAINER=hdim-demo-postgres
export DB_NAME=gateway_db
export DB_USER=healthdata
export REFRESH_INTERVAL=2  # seconds
```

**Features:**
- Auto-refresh every 2 seconds (configurable)
- Detects new errors in real-time
- Shows error statistics
- Displays recent errors
- Groups errors by service and tenant
- Press Ctrl+C to stop

---

## Manual Monitoring

### Check Current Error Count

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT COUNT(*) FROM compliance_errors;"
```

### View Recent Errors

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT id, timestamp, tenant_id, service, error_code, severity, LEFT(message, 50) 
     FROM compliance_errors 
     ORDER BY timestamp DESC 
     LIMIT 10;"
```

### Get Error Statistics

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT 
        COUNT(*) as total_errors,
        COUNT(DISTINCT tenant_id) as unique_tenants,
        COUNT(DISTINCT service) as unique_services,
        COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical,
        COUNT(CASE WHEN severity = 'ERROR' THEN 1 END) as errors,
        COUNT(CASE WHEN severity = 'WARNING' THEN 1 END) as warnings,
        MAX(timestamp) as latest_error
     FROM compliance_errors;"
```

### Errors by Service

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT 
        service,
        COUNT(*) as error_count,
        COUNT(CASE WHEN severity = 'CRITICAL' THEN 1 END) as critical,
        MAX(timestamp) as latest
     FROM compliance_errors 
     GROUP BY service 
     ORDER BY error_count DESC;"
```

### Errors by Tenant

```bash
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT 
        tenant_id,
        COUNT(*) as error_count,
        MAX(timestamp) as latest
     FROM compliance_errors 
     GROUP BY tenant_id 
     ORDER BY error_count DESC 
     LIMIT 10;"
```

---

## Validation Checklist

### Database Validation

- [ ] Database connection successful
- [ ] Table `compliance_errors` exists
- [ ] All required columns present
- [ ] All indexes created
- [ ] JSONB column functional
- [ ] Insert operations working
- [ ] Query operations working

### Backend Validation

- [ ] Gateway service running
- [ ] Compliance API endpoint accessible
- [ ] Error sync endpoint working
- [ ] Error query endpoint working
- [ ] Error statistics endpoint working

### Frontend Validation

- [ ] Frontend application running
- [ ] Compliance dashboard accessible
- [ ] Error tracking service active
- [ ] Error sync to backend working
- [ ] Real-time error display working

### End-to-End Validation

- [ ] Frontend error → Backend API → Database flow working
- [ ] Error data persisted correctly
- [ ] Error queries return correct data
- [ ] Error statistics accurate
- [ ] Error alerts triggered correctly

---

## Monitoring Dashboard

### Frontend Dashboard

Access the compliance dashboard:

```
http://localhost:4200/compliance
```

**Features:**
- Real-time error count
- Error summary by severity
- Recent errors list
- Error statistics
- Service breakdown
- Active alerts

### Database Monitoring

Use the monitoring script for real-time database monitoring:

```bash
./scripts/monitor-compliance-database.sh
```

**Displays:**
- Current error count
- New errors detected
- Summary statistics
- Recent errors
- Errors by service
- Errors by tenant

---

## Troubleshooting

### No Errors Appearing

**Check:**
1. Frontend compliance mode enabled
2. Error tracking service active
3. Backend API accessible
4. Database connection working
5. Error sync interval configured

**Commands:**
```bash
# Check frontend
curl http://localhost:4200/compliance

# Check backend
curl http://localhost:8080/api/v1/compliance/errors/stats

# Check database
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT COUNT(*) FROM compliance_errors;"
```

### Errors Not Syncing to Backend

**Check:**
1. Backend service running
2. API endpoint accessible
3. Error sync enabled in config
4. Network connectivity
5. Error logs for sync failures

**Commands:**
```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Test sync endpoint
curl -X POST http://localhost:8080/api/v1/compliance/errors \
    -H "Content-Type: application/json" \
    -d '{"errors": [], "syncedAt": "'$(date -Iseconds)'"}'
```

### Database Connection Issues

**Check:**
1. PostgreSQL container running
2. Database exists
3. User permissions
4. Connection string correct

**Commands:**
```bash
# Check container
docker ps | grep postgres

# Test connection
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c "SELECT 1;"

# Check database exists
docker exec hdim-demo-postgres psql -U healthdata -l | grep gateway_db
```

---

## Performance Monitoring

### Query Performance

Monitor slow queries:

```sql
-- Enable query logging (if needed)
SET log_min_duration_statement = 1000;  -- Log queries > 1 second

-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE tablename = 'compliance_errors';
```

### Table Size

Monitor table growth:

```sql
SELECT 
    pg_size_pretty(pg_total_relation_size('compliance_errors')) as total_size,
    pg_size_pretty(pg_relation_size('compliance_errors')) as table_size,
    pg_size_pretty(pg_indexes_size('compliance_errors')) as indexes_size,
    COUNT(*) as row_count
FROM compliance_errors;
```

### Index Efficiency

Check index usage:

```sql
SELECT 
    indexname,
    idx_scan as scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE tablename = 'compliance_errors'
ORDER BY idx_scan DESC;
```

---

## Alerting

### Error Threshold Alerts

The system automatically alerts when:
- Critical error threshold exceeded
- Error rate threshold exceeded

**Check alerts:**
```bash
# View active alerts in frontend
# Navigate to: http://localhost:4200/compliance

# Or query database for critical errors
docker exec hdim-demo-postgres psql -U healthdata -d gateway_db -c \
    "SELECT COUNT(*) FROM compliance_errors 
     WHERE severity = 'CRITICAL' 
     AND timestamp > NOW() - INTERVAL '1 hour';"
```

### Database Alerts

Monitor for:
- Table size growth
- Index bloat
- Query performance degradation
- Connection pool exhaustion

---

## Best Practices

1. **Regular Monitoring**: Run validation script daily
2. **Real-Time Monitoring**: Use monitoring script during testing
3. **Error Review**: Review errors weekly for patterns
4. **Performance Tuning**: Monitor query performance monthly
5. **Cleanup**: Configure retention policy for old errors
6. **Backup**: Include compliance_errors in regular backups

---

## Next Steps

1. ✅ Run end-to-end validation
2. ✅ Start real-time monitoring
3. ✅ Trigger test errors from frontend
4. ✅ Verify errors appear in database
5. ✅ Check compliance dashboard
6. ✅ Review error statistics
7. ✅ Configure alerts if needed

---

**Status**: ✅ Monitoring and validation tools ready
