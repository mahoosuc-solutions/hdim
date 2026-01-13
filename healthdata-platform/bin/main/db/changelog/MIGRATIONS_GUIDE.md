# HealthData Platform - Liquibase Database Migrations Guide

## Overview

This directory contains comprehensive Liquibase database migrations for the HealthData Platform. The migrations are organized into modular changelog files that create a unified PostgreSQL database with logical separation via schemas.

## Architecture

The database uses a **unified schema approach** with 6 logical schemas:

### 1. **patient** - Patient Management
- `patients` - Core patient demographics and identifiers
- `patient_identifiers` - External system identifiers (MRN, EHR ID, etc.)

### 2. **fhir** - FHIR Resources
- `observations` - Lab results and clinical measurements
- `conditions` - Patient diagnoses
- `medication_requests` - Prescribed medications

### 3. **quality** - Quality Measures
- `measures` - Quality measure definitions
- `measure_results` - Individual patient measure evaluations
- `health_scores` - Composite health scores
- `health_score_history` - Historical health score tracking

### 4. **caregap** - Care Gap Management
- `care_gaps` - Identified care gaps for patients
- `interventions` - Actions taken to close care gaps

### 5. **notification** - Notification System
- `templates` - Reusable notification templates
- `history` - Sent notification tracking
- `preferences` - User notification preferences

### 6. **audit** - Compliance and Audit Trail
- `audit_log` - Entity change logging
- `access_log` - User login/logout tracking
- `data_change_log` - Detailed data change tracking

## Migration Files

### Master Changelog
- **db.changelog-master.xml** - Main changelog that orchestrates all migrations in order

### Individual Migrations

#### 001-create-schemas.xml
Creates all database schemas and enables necessary PostgreSQL extensions.
- Creates 6 logical schemas
- Enables UUID extension for ID generation

#### 002-create-patient-tables.xml
Creates patient management tables with tenant isolation and identifiers.
- `patients` table with comprehensive demographic fields
- `patient_identifiers` table for system-specific IDs
- Indexes for efficient patient lookups
- Constraints for data validation

#### 003-create-fhir-tables.xml
Creates FHIR resource tables for clinical data.
- `observations` - Lab results and vital signs
- `conditions` - Diagnoses
- `medication_requests` - Medication orders
- GIN indexes on JSONB FHIR resources
- Foreign key relationships to patients

#### 004-create-quality-tables.xml
Creates quality measure and health score tables.
- `measures` - Measure definitions
- `measure_results` - Patient measure evaluations
- `health_scores` - Composite scores
- `health_score_history` - Score trend tracking
- Indexes for time-series queries

#### 005-create-caregap-tables.xml
Creates care gap and intervention tables.
- `care_gaps` - Identified care gaps
- `interventions` - Closure actions
- Status constraints (OPEN, IN_PROGRESS, CLOSED, DEFERRED)
- Priority-based indexes

#### 006-create-notification-tables.xml
Creates notification system tables.
- `templates` - Reusable notification content
- `history` - Sent notification tracking
- `preferences` - User notification settings
- Channel-specific constraints (EMAIL, SMS, PUSH, IN_APP)

#### 007-create-audit-tables.xml
Creates audit and compliance tables.
- `audit_log` - Entity change tracking
- `access_log` - Authentication tracking
- `data_change_log` - Sensitive data change tracking
- Comprehensive indexes for audit queries

#### 008-create-indexes.xml
Creates additional performance and optimization indexes.
- Tenant isolation indexes
- JSONB GIN indexes
- Date range query indexes
- Search and lookup indexes
- Audit-specific indexes

#### 009-insert-initial-data.xml
Inserts default data and creates automation triggers.
- Default quality measures (5 HEDIS measures)
- Default notification templates (6 templates)
- Trigger functions for:
  - Automatic timestamp updates
  - Default preference creation
  - Audit logging

## Key Features

### Multi-Tenancy
All tables include `tenant_id` column for data isolation:
```sql
-- Example tenant-specific query
SELECT * FROM patient.patients WHERE tenant_id = 'tenant-123' AND active = true
```

### Foreign Key Relationships
All tables maintain referential integrity:
- Patient records are the root aggregate
- FHIR data references patients
- Quality measures reference patients
- Care gaps reference both patients and measures

### Comprehensive Indexing
Multiple index strategies for performance:
- B-tree indexes on common filters
- Composite indexes for multi-column queries
- GIN indexes for JSONB columns
- Partial indexes for filtered queries

### Audit Trail
Complete audit logging for compliance:
- Entity-level changes captured
- User action tracking
- Data access logging
- Detailed field-level change history

### Data Validation
Constraints and triggers ensure data integrity:
- Check constraints for enums (gender, status, channel)
- Unique constraints on identifiers
- Foreign key constraints with CASCADE
- Automatic timestamp management

## Running Migrations

### Configuration
Add to `application.yml`:
```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
    drop-first: false  # Set to true only for development
  jpa:
    hibernate:
      ddl-auto: validate  # Use 'validate' with Liquibase
  datasource:
    url: jdbc:postgresql://localhost:5432/healthdata
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
```

### Running via Spring Boot
Migrations run automatically on application startup:
```bash
./gradlew bootRun
```

### Running via CLI
```bash
liquibase --changelog-file=src/main/resources/db/changelog/db.changelog-master.xml \
          --url=jdbc:postgresql://localhost:5432/healthdata \
          --username=postgres \
          --password=password \
          update
```

## Rollback Strategies

Each changeset includes rollback instructions. Rollback specific changesets:
```bash
liquibase --changelog-file=db.changelog-master.xml \
          rollbackCount=5
```

Rollback to specific date:
```bash
liquibase --changelog-file=db.changelog-master.xml \
          rollbackToDate=2024-01-01
```

## Best Practices

### When Creating New Migrations

1. **Naming Convention**: `NNN-description.xml` (e.g., `010-add-user-tables.xml`)

2. **Structure**:
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog" ...>
       <changeSet id="010-001-create-users-table" author="your-name">
           <comment>Clear description of what this does</comment>
           <!-- Changes here -->
       </changeSet>
   </databaseChangeLog>
   ```

3. **Include Rollback**: Every changeset should have a rollback section

4. **Schema Specification**: Always specify schema in tableName:
   ```xml
   <createTable tableName="users" schemaName="auth">
   ```

5. **Foreign Keys**: Use explicit constraint names:
   ```xml
   <addForeignKeyConstraint
       constraintName="fk_care_gaps_patient"
       baseTableSchemaName="caregap"
       baseTableName="care_gaps"
       ...
   />
   ```

6. **Indexes**: Create meaningful index names for searchability:
   ```xml
   <createIndex tableName="patients" indexName="idx_patients_tenant">
       <column name="tenant_id"/>
   </createIndex>
   ```

### Testing Migrations

1. **Test Locally**:
   ```bash
   docker run --name healthdata-db -e POSTGRES_PASSWORD=password -d postgres:16
   ./gradlew bootRun
   ```

2. **Verify Schema**:
   ```sql
   -- List all tables by schema
   SELECT schemaname, tablename FROM pg_tables
   WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
   ORDER BY schemaname, tablename;
   ```

3. **Check Indexes**:
   ```sql
   -- List all indexes
   SELECT schemaname, tablename, indexname FROM pg_indexes
   WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
   ORDER BY schemaname, tablename;
   ```

## Migration Dependencies

The migrations must run in this order:

1. `001-create-schemas.xml` - Creates schemas and extensions
2. `002-create-patient-tables.xml` - Base patient data
3. `003-create-fhir-tables.xml` - FHIR resources (references patients)
4. `004-create-quality-tables.xml` - Quality measures (references patients)
5. `005-create-caregap-tables.xml` - Care gaps (references patients and measures)
6. `006-create-notification-tables.xml` - Notifications (references patients)
7. `007-create-audit-tables.xml` - Audit tables (independent)
8. `008-create-indexes.xml` - Performance indexes
9. `009-insert-initial-data.xml` - Default data and triggers

## Performance Considerations

### Index Strategy
- **Tenant Isolation**: All queries filter by `tenant_id` (indexed)
- **Time-Series**: Care gaps and scores use DESC indexes for recent data
- **JSONB**: GIN indexes on FHIR resources for complex queries
- **Composite**: Multi-column indexes match common query patterns

### Query Examples

```sql
-- Get patient's current care gaps
SELECT * FROM caregap.care_gaps
WHERE patient_id = ? AND tenant_id = ? AND status = 'OPEN'
ORDER BY due_date ASC;

-- Get recent health score
SELECT * FROM quality.health_scores
WHERE patient_id = ? AND tenant_id = ?
ORDER BY calculated_at DESC
LIMIT 1;

-- Find observations by code and date
SELECT * FROM fhir.observations
WHERE patient_id = ? AND code = ? AND effective_date > ?
AND tenant_id = ?
ORDER BY effective_date DESC;
```

## Troubleshooting

### Locks During Migration
```sql
-- View active connections
SELECT * FROM pg_stat_activity;

-- Terminate blocking connections
SELECT pg_terminate_backend(pid) FROM pg_stat_activity
WHERE usename = 'postgres' AND state = 'active';
```

### Schema Issues
```sql
-- Verify schema existence
\dn

-- Show tables in schema
SELECT * FROM information_schema.tables
WHERE table_schema = 'patient';
```

### Index Performance
```sql
-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

## Support and Documentation

- Liquibase Docs: https://docs.liquibase.com
- PostgreSQL Docs: https://www.postgresql.org/docs
- HealthData Platform: Check README.md in project root
