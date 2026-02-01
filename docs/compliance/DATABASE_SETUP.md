# Compliance Database Setup

## Overview

The compliance error tracking system uses PostgreSQL to store error data. The database is automatically initialized via Liquibase migrations when the `gateway-clinical-service` starts.

## Database Configuration

**Database**: `gateway_db`  
**Host**: `localhost` (default)  
**Port**: `5435` (default)  
**User**: `healthdata` (default)  
**Table**: `compliance_errors`

## Automatic Setup (Recommended)

The database table is created automatically when the service starts:

1. **Start the service**:
   ```bash
   cd backend
   ./gradlew :modules:services:gateway-clinical-service:bootRun
   ```

2. **Liquibase will**:
   - Check if `compliance_errors` table exists
   - Create the table if it doesn't exist
   - Create all required indexes
   - Track migration history

## Manual Setup

If you need to create the database manually:

### Option 1: Using SQL Script

```bash
psql -h localhost -p 5435 -U healthdata -f scripts/create-compliance-database.sql
```

### Option 2: Using psql Directly

```sql
-- Connect to PostgreSQL
psql -h localhost -p 5435 -U healthdata -d postgres

-- Create database
CREATE DATABASE gateway_db;

-- Connect to gateway_db
\c gateway_db

-- Run the migration SQL (from create-compliance-database.sql)
-- Or let Liquibase handle it on service startup
```

## Validation

### Validate Database Setup

Run the validation script:

```bash
./scripts/validate-compliance-database.sh
```

This script checks:
- ✅ Database connection
- ✅ Table existence
- ✅ Table structure (all required columns)
- ✅ Indexes
- ✅ JSONB support
- ✅ Insert/Query functionality

### Manual Validation

```sql
-- Connect to database
psql -h localhost -p 5435 -U healthdata -d gateway_db

-- Check if table exists
\dt compliance_errors

-- View table structure
\d compliance_errors

-- Check indexes
\di compliance_errors*

-- Test insert
INSERT INTO compliance_errors (
    id, timestamp, tenant_id, service, operation, 
    error_code, severity, message, created_at
) VALUES (
    gen_random_uuid(), 
    NOW(), 
    'test-tenant', 
    'Test Service', 
    'Test Operation', 
    'ERR-9001', 
    'INFO', 
    'Test message', 
    NOW()
);

-- Query test
SELECT COUNT(*) FROM compliance_errors WHERE tenant_id = 'test-tenant';

-- Cleanup
DELETE FROM compliance_errors WHERE tenant_id = 'test-tenant';
```

## Table Structure

### compliance_errors Table

| Column | Type | Nullable | Description |
|--------|------|----------|-------------|
| id | UUID | NO | Primary key |
| timestamp | TIMESTAMP | NO | Error timestamp |
| tenant_id | VARCHAR(255) | NO | Tenant identifier |
| user_id | VARCHAR(255) | YES | User identifier |
| service | VARCHAR(100) | NO | Service name |
| endpoint | VARCHAR(500) | YES | API endpoint |
| operation | VARCHAR(500) | NO | Operation description |
| error_code | VARCHAR(50) | NO | Error code |
| severity | VARCHAR(20) | NO | Error severity (INFO, WARNING, ERROR, CRITICAL) |
| message | TEXT | NO | Error message |
| stack | TEXT | YES | Stack trace |
| additional_data | JSONB | YES | Additional context data |
| created_at | TIMESTAMP | NO | Record creation time |

### Indexes

1. **idx_compliance_tenant_timestamp**: Composite index on `(tenant_id, timestamp DESC)`
   - Optimizes tenant-scoped queries ordered by time

2. **idx_compliance_severity**: Index on `severity`
   - Optimizes severity-based filtering

3. **idx_compliance_service**: Index on `service`
   - Optimizes service-based filtering

4. **idx_compliance_timestamp**: Index on `timestamp DESC`
   - Optimizes time-based queries

5. **idx_compliance_additional_data**: GIN index on `additional_data` (JSONB)
   - Optimizes JSON queries on additional data

## Environment Variables

Configure database connection via environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5435/gateway_db
export SPRING_DATASOURCE_USERNAME=healthdata
export SPRING_DATASOURCE_PASSWORD=your_password
```

Or set in `application.yml`:

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5435/gateway_db}
    username: ${SPRING_DATASOURCE_USERNAME:healthdata}
    password: ${SPRING_DATASOURCE_PASSWORD:}
```

## Troubleshooting

### Table Not Created

**Issue**: Table doesn't exist after service startup

**Solutions**:
1. Check Liquibase logs for errors
2. Verify `spring.liquibase.enabled=true` in `application.yml`
3. Check database connection credentials
4. Run manual setup script: `scripts/create-compliance-database.sql`

### Connection Refused

**Issue**: Cannot connect to database

**Solutions**:
1. Verify PostgreSQL is running: `docker ps | grep postgres`
2. Check port: Default is `5435` (not standard `5432`)
3. Verify credentials in environment variables
4. Test connection: `psql -h localhost -p 5435 -U healthdata -d gateway_db`

### Migration Errors

**Issue**: Liquibase migration fails

**Solutions**:
1. Check Liquibase changelog lock: `SELECT * FROM databasechangeloglock;`
2. Clear lock if needed: `DELETE FROM databasechangeloglock;`
3. Check migration history: `SELECT * FROM databasechangelog ORDER BY dateexecuted DESC;`
4. Review error logs for specific issues

## Production Considerations

1. **Backup Strategy**: Include `compliance_errors` table in regular backups
2. **Retention Policy**: Configure cleanup job to remove old errors
3. **Performance**: Monitor index usage and query performance
4. **Security**: Ensure proper access controls and encryption
5. **Monitoring**: Set up alerts for table growth and query performance

## Next Steps

1. ✅ Database configuration complete
2. ✅ Migration files ready
3. ✅ Validation scripts created
4. ⏳ Start service to run migrations
5. ⏳ Validate database setup

---

**Status**: ✅ Database setup ready - migrations will run on service startup
