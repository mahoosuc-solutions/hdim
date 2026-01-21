# Liquibase Migration Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: "Liquibase lockfile could not be acquired"

**Symptoms**:
```
Error: Liquibase lockfile could not be acquired
Caused by: WAIT_FOR_LOCK timed out after 300 seconds
```

**Causes**:
- Another instance is running migrations
- Database connection timeout
- Lock table is corrupted

**Solutions**:

1. **Check for running instances**:
   ```bash
   ps aux | grep liquibase
   ps aux | grep java
   ```

2. **Check database locks**:
   ```sql
   -- View active connections
   SELECT pid, usename, application_name, state
   FROM pg_stat_activity
   WHERE state != 'idle';

   -- Kill blocking connection
   SELECT pg_terminate_backend(pid)
   FROM pg_stat_activity
   WHERE application_name LIKE '%liquibase%';
   ```

3. **Clear lock manually** (use with caution):
   ```sql
   DELETE FROM databasechangeloglock;
   ```

4. **Increase timeout** in application.yml:
   ```yaml
   spring:
     liquibase:
       liquibase-schema: public
   ```

---

### Issue 2: "Table already exists"

**Symptoms**:
```
Error: relation "patient.patients" already exists
```

**Causes**:
- Migration already ran successfully
- Manual table creation exists
- Duplicate changeset IDs

**Solutions**:

1. **Verify Liquibase tracking**:
   ```sql
   SELECT id, dateexecuted, status FROM databasechangelog
   ORDER BY dateexecuted DESC LIMIT 10;
   ```

2. **If table exists but not tracked**:
   ```sql
   -- Mark changeset as executed
   INSERT INTO databasechangelog
   (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id)
   VALUES
   ('002-001-create-patients-table', 'healthdata', 'db/changelog/002-create-patient-tables.xml',
    NOW(), 1, 'EXECUTED', 'xxx', 'Create core patients table', '', NULL, '4.25.0', NULL, NULL, NULL);
   ```

3. **If you need to clean slate** (development only):
   ```sql
   DROP SCHEMA IF EXISTS patient CASCADE;
   DROP SCHEMA IF EXISTS fhir CASCADE;
   DROP SCHEMA IF EXISTS quality CASCADE;
   DROP SCHEMA IF EXISTS caregap CASCADE;
   DROP SCHEMA IF EXISTS notification CASCADE;
   DROP SCHEMA IF EXISTS audit CASCADE;
   DELETE FROM databasechangelog;
   DELETE FROM databasechangeloglock;
   ```

---

### Issue 3: "Foreign key constraint violation"

**Symptoms**:
```
Error: insert or update on table "care_gaps" violates foreign key constraint
```

**Causes**:
- Referenced table hasn't been created yet
- Migration order is wrong
- Data doesn't match foreign key

**Solutions**:

1. **Verify migration order**:
   ```sql
   SELECT id, dateexecuted FROM databasechangelog
   ORDER BY dateexecuted;
   ```
   Should see: schemas → patient → fhir → quality → caregap → ...

2. **Check foreign key existence**:
   ```sql
   SELECT constraint_name, table_name, column_name
   FROM information_schema.key_column_usage
   WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
   ORDER BY table_name;
   ```

3. **Validate referenced data**:
   ```sql
   -- Check for orphaned records
   SELECT cg.* FROM caregap.care_gaps cg
   LEFT JOIN patient.patients p ON cg.patient_id = p.id
   WHERE p.id IS NULL;

   -- Delete orphaned records
   DELETE FROM caregap.care_gaps
   WHERE patient_id NOT IN (SELECT id FROM patient.patients);
   ```

---

### Issue 4: "Index creation failed"

**Symptoms**:
```
Error: relation "idx_patients_active_tenant" already exists
```

**Causes**:
- Index already exists from previous migration
- Partial index syntax error
- Missing schema specification

**Solutions**:

1. **Check for duplicate indexes**:
   ```sql
   SELECT schemaname, tablename, indexname
   FROM pg_indexes
   WHERE indexname LIKE 'idx_%'
   ORDER BY schemaname, indexname;
   ```

2. **Drop and recreate**:
   ```sql
   DROP INDEX IF EXISTS patient.idx_patients_active_tenant;
   -- Re-run migration
   ```

3. **Check index definition**:
   ```sql
   SELECT * FROM pg_indexes
   WHERE indexname = 'idx_patients_active_tenant';
   ```

---

### Issue 5: "JSONB column not supported"

**Symptoms**:
```
Error: Unsupported column type jsonb
```

**Causes**:
- PostgreSQL version < 9.4
- Dialect not properly configured
- Wrong database type

**Solutions**:

1. **Verify PostgreSQL version**:
   ```sql
   SELECT version();
   -- Should be PostgreSQL 9.4+
   ```

2. **Update dialect in application.yml**:
   ```yaml
   spring:
     jpa:
       database-platform: org.hibernate.dialect.PostgreSQL15Dialect
   ```

3. **Check actual column type**:
   ```sql
   SELECT column_name, udt_name
   FROM information_schema.columns
   WHERE table_schema = 'fhir' AND table_name = 'observations';
   -- Should show 'jsonb'
   ```

---

### Issue 6: "GIN index creation on non-JSONB column"

**Symptoms**:
```
Error: access method "gin" is invalid for column types used
```

**Causes**:
- Column is not JSONB type
- Trying to create GIN on TEXT or VARCHAR
- Migration ran partially

**Solutions**:

1. **Verify column types**:
   ```sql
   SELECT column_name, data_type
   FROM information_schema.columns
   WHERE table_schema = 'fhir' AND table_name = 'observations'
   AND column_name IN ('fhir_resource', 'metadata');
   ```

2. **Check GIN indexes**:
   ```sql
   SELECT indexname, indexdef
   FROM pg_indexes
   WHERE indexname LIKE '%_gin%'
   ORDER BY schemaname, indexname;
   ```

3. **Recreate with correct type**:
   ```sql
   -- First, convert to JSONB if needed
   ALTER TABLE fhir.observations
   ALTER COLUMN fhir_resource TYPE jsonb USING fhir_resource::jsonb;

   -- Then create index
   CREATE INDEX idx_obs_fhir_gin ON fhir.observations USING gin(fhir_resource);
   ```

---

### Issue 7: "Duplicate key value violates unique constraint"

**Symptoms**:
```
Error: duplicate key value violates unique constraint "uk_measures_measure_id"
```

**Causes**:
- Trying to insert default data that already exists
- Multiple migrations inserting same data
- Partial data insertion

**Solutions**:

1. **Check existing data**:
   ```sql
   SELECT measure_id, name FROM quality.measures;
   ```

2. **Clear duplicate data** (if safe):
   ```sql
   DELETE FROM quality.measure_results
   WHERE measure_id = 'HbA1c-Control' AND created_at < '2024-01-01';

   DELETE FROM quality.measures
   WHERE measure_id = 'HbA1c-Control';
   ```

3. **Modify migration** to use INSERT ... ON CONFLICT:
   ```sql
   INSERT INTO quality.measures (measure_id, name, description, category)
   VALUES ('HbA1c-Control', 'Diabetes: HbA1c Control', '...', 'Clinical')
   ON CONFLICT (measure_id) DO NOTHING;
   ```

---

### Issue 8: "Timeout connecting to database"

**Symptoms**:
```
Error: Failed to obtain JDBC Connection: Connection timeout
```

**Causes**:
- Database not running
- Connection string incorrect
- Database credentials wrong
- Network/firewall issue

**Solutions**:

1. **Verify database is running**:
   ```bash
   # PostgreSQL
   pg_isready -h localhost -p 5432

   # Or with Docker
   docker ps | grep postgres
   ```

2. **Test connection**:
   ```bash
   psql -h localhost -U postgres -d healthdata
   ```

3. **Check application.yml**:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/healthdata
       username: postgres
       password: password
   ```

4. **Increase connection timeout**:
   ```yaml
   spring:
     datasource:
       hikari:
         connection-timeout: 60000  # 60 seconds
   ```

---

### Issue 9: "Trigger function compilation error"

**Symptoms**:
```
Error: function patient.update_patient_timestamp() does not exist
```

**Causes**:
- PL/pgSQL extension not enabled
- Syntax error in function
- Schema not created before function

**Solutions**:

1. **Enable PL/pgSQL**:
   ```sql
   CREATE LANGUAGE IF NOT EXISTS plpgsql;
   ```

2. **Check function existence**:
   ```sql
   SELECT proname, pronargs, prosrc
   FROM pg_proc
   WHERE proname LIKE 'update_%'
   AND pg_catalog.pg_get_userbyid(proowner) = 'postgres';
   ```

3. **Recreate function** with correct syntax:
   ```sql
   CREATE OR REPLACE FUNCTION patient.update_patient_timestamp()
   RETURNS TRIGGER AS $$
   BEGIN
       NEW.updated_at = CURRENT_TIMESTAMP;
       RETURN NEW;
   END;
   $$ LANGUAGE plpgsql;
   ```

---

### Issue 10: "Schema permission denied"

**Symptoms**:
```
Error: permission denied for schema patient
```

**Causes**:
- User doesn't have schema permissions
- Role not granted proper privileges
- Using wrong user/role

**Solutions**:

1. **Check current user**:
   ```sql
   SELECT current_user;
   ```

2. **Grant schema permissions**:
   ```sql
   GRANT ALL PRIVILEGES ON SCHEMA patient TO postgres;
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA patient TO postgres;
   GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA patient TO postgres;
   ```

3. **Check role membership**:
   ```sql
   SELECT * FROM pg_roles WHERE rolname = 'postgres';
   ```

---

## Verification Checklist

After migrations complete successfully, verify:

```sql
-- 1. All schemas exist
\dn
-- Expected: patient, fhir, quality, caregap, notification, audit

-- 2. All tables created
SELECT schemaname, tablename
FROM pg_tables
WHERE schemaname IN ('patient', 'fhir', 'quality', 'caregap', 'notification', 'audit')
ORDER BY schemaname, tablename;
-- Expected: 20+ tables

-- 3. All indexes created
SELECT schemaname, COUNT(*)
FROM pg_indexes
WHERE schemaname IN ('patient', 'fhir', 'quality', 'caregap', 'notification', 'audit')
GROUP BY schemaname;
-- Expected: 70+ total indexes

-- 4. Foreign keys working
SELECT constraint_name, table_name, column_name
FROM information_schema.key_column_usage
WHERE table_schema IN ('patient', 'fhir', 'quality', 'caregap', 'notification', 'audit')
AND constraint_name LIKE 'fk_%'
ORDER BY table_name;

-- 5. Default data inserted
SELECT COUNT(*) FROM quality.measures;  -- Should be 5
SELECT COUNT(*) FROM notification.templates;  -- Should be 7

-- 6. Triggers created
SELECT trigger_name, event_manipulation, event_object_table
FROM information_schema.triggers
WHERE trigger_schema IN ('patient', 'notification', 'audit')
ORDER BY trigger_name;
```

---

## Performance Diagnostics

### Check Slow Queries

```sql
-- Enable query logging
ALTER DATABASE healthdata SET log_min_duration_statement = 1000;

-- Check slow query log
SELECT query, calls, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### Analyze Query Plans

```sql
EXPLAIN ANALYZE
SELECT * FROM patient.patients
WHERE tenant_id = 'tenant-123' AND active = true;

-- Should use idx_patients_active_tenant
```

### Check Index Usage

```sql
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;
-- Shows unused indexes
```

---

## Rollback Guide

### Rollback Single Changeset

```bash
liquibase --changelog-file=db.changelog-master.xml \
          rollbackOneChangeSet
```

### Rollback to Specific Date

```bash
liquibase --changelog-file=db.changelog-master.xml \
          rollbackToDate=2024-01-01
```

### Rollback Last N Changes

```bash
liquibase --changelog-file=db.changelog-master.xml \
          rollbackCount=5
```

---

## Emergency Procedures

### Complete Database Reset (Development Only)

```sql
-- Drop all schemas
DROP SCHEMA IF EXISTS patient CASCADE;
DROP SCHEMA IF EXISTS fhir CASCADE;
DROP SCHEMA IF EXISTS quality CASCADE;
DROP SCHEMA IF EXISTS caregap CASCADE;
DROP SCHEMA IF EXISTS notification CASCADE;
DROP SCHEMA IF EXISTS audit CASCADE;

-- Clear Liquibase tables
TRUNCATE TABLE databasechangelog;
TRUNCATE TABLE databasechangeloglock;

-- Drop functions/triggers if needed
DROP FUNCTION IF EXISTS patient.update_patient_timestamp();
DROP FUNCTION IF EXISTS notification.create_default_preferences();
DROP FUNCTION IF EXISTS audit.audit_data_changes();
```

Then restart the application - migrations will run from scratch.

### Backup Before Migration

```bash
# PostgreSQL backup
pg_dump -h localhost -U postgres -d healthdata > healthdata_backup.sql

# Later restore if needed
psql -h localhost -U postgres -d healthdata < healthdata_backup.sql
```

---

## Getting Help

1. **Check Liquibase logs**:
   ```bash
   tail -f logs/application.log | grep -i liquibase
   ```

2. **Enable debug logging**:
   ```yaml
   logging:
     level:
       liquibase: debug
   ```

3. **Verify database directly**:
   ```bash
   psql -h localhost -U postgres -c "SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;"
   ```

4. **Review change history**:
   ```bash
   git log --oneline src/main/resources/db/changelog/
   ```

---

**Last Updated**: December 2024
**Version**: 1.0
