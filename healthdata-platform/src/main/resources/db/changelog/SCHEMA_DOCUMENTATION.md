# HealthData Platform - Database Schema Documentation

## Complete Schema Overview

### Patient Schema

#### patients Table
Stores core patient demographic and contact information.

```
patients
├── id (UUID, PK)
├── mrn (VARCHAR 50, UK) - Medical Record Number
├── first_name (VARCHAR 100)
├── last_name (VARCHAR 100)
├── middle_name (VARCHAR 100)
├── date_of_birth (DATE)
├── gender (VARCHAR 20, CHECK) - MALE | FEMALE | OTHER | UNKNOWN
├── street (VARCHAR 255)
├── city (VARCHAR 100)
├── state (VARCHAR 50)
├── postal_code (VARCHAR 20)
├── country (VARCHAR 100)
├── phone_number (VARCHAR 50)
├── email (VARCHAR 255)
├── tenant_id (VARCHAR 50, FK) - Multi-tenancy
├── active (BOOLEAN, default true)
├── created_at (TIMESTAMP, default CURRENT_TIMESTAMP)
├── updated_at (TIMESTAMP, auto-updated)
└── version (BIGINT, default 0)
```

**Indexes**:
- `idx_patients_mrn` - Medical Record Number lookup
- `idx_patients_tenant` - Tenant isolation
- `idx_patients_name` - Name-based search
- `idx_patients_active_tenant` - Active patients by tenant
- `idx_patient_search` - Composite search index

**Constraints**:
- `pk_patients` - Primary key on id
- `uk_patients_mrn` - Unique medical record number
- `chk_gender` - Valid gender values

#### patient_identifiers Table
Stores external system identifiers for each patient (e.g., EHR IDs, Insurance IDs).

```
patient_identifiers
├── patient_id (UUID, FK, PK) → patients.id
├── system (VARCHAR 255, PK) - System identifier (e.g., "urn:oid:1.2.840.113619")
├── value (VARCHAR 255, PK) - Identifier value
└── type (VARCHAR 100) - Identifier type (MRN, SSN, etc.)
```

**Indexes**:
- `idx_patient_identifiers_system` - System-based lookup
- Composite primary key on (patient_id, system, value)

---

### FHIR Schema

#### observations Table
Stores FHIR-compliant observation data (lab results, vital signs, measurements).

```
observations
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── code (VARCHAR 50) - LOINC or other code system
├── system (VARCHAR 255) - Code system URL
├── display (VARCHAR 255) - Human-readable code name
├── value_quantity (DECIMAL 10,2) - Numeric result
├── value_unit (VARCHAR 50) - Unit of measurement
├── value_string (TEXT) - Text result
├── status (VARCHAR 50) - final | preliminary | corrected | etc.
├── effective_date (TIMESTAMP) - When observation was taken
├── category (VARCHAR 100) - vital-signs | lab-results | etc.
├── tenant_id (VARCHAR 50)
├── fhir_resource (JSONB) - Complete FHIR Observation resource
└── created_at (TIMESTAMP)
```

**Indexes**:
- `idx_obs_patient_code` - Find observations for patient by code
- `idx_obs_effective` - Sort by effective date
- `idx_obs_tenant` - Tenant isolation
- `idx_obs_fhir_gin` - JSONB GIN index for complex queries
- `idx_fhir_obs_tenant_effective` - Tenant + date queries
- `idx_obs_code_display` - Code/name lookup

#### conditions Table
Stores FHIR-compliant condition data (diagnoses).

```
conditions
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── code (VARCHAR 50) - ICD-10 or SNOMED code
├── system (VARCHAR 255) - Code system
├── display (VARCHAR 255) - Human-readable diagnosis
├── clinical_status (VARCHAR 50) - active | resolved | recurrence | etc.
├── verification_status (VARCHAR 50) - confirmed | unconfirmed | etc.
├── onset_date (DATE) - When condition started
├── abatement_date (DATE) - When condition resolved
├── tenant_id (VARCHAR 50)
├── fhir_resource (JSONB) - Complete FHIR Condition resource
└── created_at (TIMESTAMP)
```

**Indexes**:
- `idx_cond_patient` - Find conditions for patient
- `idx_cond_code` - Code lookup
- `idx_cond_tenant` - Tenant isolation
- `idx_fhir_cond_tenant_status` - Tenant + status queries
- `idx_cond_code_display` - Code/name lookup

#### medication_requests Table
Stores FHIR-compliant medication prescription data.

```
medication_requests
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── medication_code (VARCHAR 50) - RxNorm or similar
├── medication_display (VARCHAR 255) - Medication name
├── status (VARCHAR 50) - active | completed | cancelled | etc.
├── intent (VARCHAR 50) - order | plan | original-order | etc.
├── authored_on (TIMESTAMP) - When prescribed
├── dosage_instruction (TEXT) - Dosage and frequency
├── tenant_id (VARCHAR 50)
├── fhir_resource (JSONB) - Complete FHIR MedicationRequest
└── created_at (TIMESTAMP)
```

**Indexes**:
- `idx_med_patient` - Find medications for patient
- `idx_med_status` - Filter by status
- `idx_med_tenant` - Tenant isolation
- `idx_fhir_med_tenant_status` - Tenant + status
- `idx_med_code_display` - Medication lookup

---

### Quality Schema

#### measures Table
Stores quality measure definitions (e.g., HEDIS measures).

```
measures
├── id (UUID, PK)
├── measure_id (VARCHAR 50, UK) - Measure identifier
├── name (VARCHAR 255) - Display name
├── description (TEXT) - Detailed description
├── category (VARCHAR 100) - Clinical | Preventive | Process | etc.
├── version (VARCHAR 20) - Version string
├── cql_library (TEXT) - CQL code for measure calculation
└── active (BOOLEAN, default true)
```

**Indexes**:
- `idx_measure_names` - Name-based lookup

#### measure_results Table
Stores patient-level quality measure evaluation results.

```
measure_results
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── measure_id (VARCHAR 50, FK) → measures.measure_id
├── score (DECIMAL 5,2) - Percentage score
├── numerator (INTEGER) - Numerator of measure
├── denominator (INTEGER) - Denominator of measure
├── compliant (BOOLEAN) - Met measure criteria?
├── calculation_date (TIMESTAMP) - When calculated
├── period_start (DATE) - Measurement period start
├── period_end (DATE) - Measurement period end
├── tenant_id (VARCHAR 50)
├── details (JSONB) - Additional calculation details
└── created_at (TIMESTAMP)
```

**Indexes**:
- `idx_results_patient_measure` - Find results for patient/measure
- `idx_results_calculation` - Sort by calculation date
- `idx_results_tenant` - Tenant isolation
- `idx_measure_results_compliance` - Tenant + compliance
- `idx_measure_results_period` - Period-based queries
- `idx_quality_details_gin` - JSONB details search
- `idx_quality_results_tenant_calc` - Tenant + date queries

#### health_scores Table
Stores composite health scores for patients.

```
health_scores
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── overall_score (DECIMAL 5,2) - Composite score (0-100)
├── clinical_score (DECIMAL 5,2) - Clinical measure score
├── preventive_score (DECIMAL 5,2) - Preventive care score
├── medication_score (DECIMAL 5,2) - Medication adherence
├── calculated_at (TIMESTAMP) - When calculated
├── tenant_id (VARCHAR 50)
└── score_components (JSONB) - Breakdown of component scores
```

**Indexes**:
- `idx_scores_patient` - Find scores for patient
- `idx_scores_tenant` - Tenant isolation
- `idx_health_scores_calculated` - Recent scores
- `idx_health_components_gin` - JSONB components search
- `idx_quality_scores_tenant_calc` - Tenant + date

#### health_score_history Table
Tracks historical health scores for trend analysis.

```
health_score_history
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── overall_score (DECIMAL 5,2)
├── clinical_score (DECIMAL 5,2)
├── preventive_score (DECIMAL 5,2)
├── medication_score (DECIMAL 5,2)
├── recorded_at (TIMESTAMP)
└── tenant_id (VARCHAR 50)
```

**Indexes**:
- `idx_score_history_patient` - Patient score history
- `idx_score_history_tenant` - Tenant isolation

---

### Care Gap Schema

#### care_gaps Table
Stores identified care gaps for patients.

```
care_gaps
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── measure_id (VARCHAR 50, FK) → measures.measure_id
├── gap_type (VARCHAR 50) - Type of gap
├── status (VARCHAR 50, CHECK) - OPEN | IN_PROGRESS | CLOSED | DEFERRED
├── priority (VARCHAR 20) - HIGH | MEDIUM | LOW
├── identified_date (TIMESTAMP)
├── due_date (DATE) - Target closure date
├── closed_date (TIMESTAMP)
├── closure_reason (VARCHAR 255)
├── tenant_id (VARCHAR 50)
└── metadata (JSONB) - Additional gap information
```

**Indexes**:
- `idx_gaps_patient_status` - Find open gaps for patient
- `idx_gaps_measure` - Find gaps by measure
- `idx_gaps_tenant` - Tenant isolation
- `idx_gaps_due_date` - Filtered: due date for OPEN gaps
- `idx_caregap_tenant_priority` - Tenant + priority + status
- `idx_care_gaps_priority_due` - Priority + due date for open gaps
- `idx_care_gap_metadata_gin` - JSONB metadata search

#### interventions Table
Tracks actions taken to close care gaps.

```
interventions
├── id (UUID, PK)
├── care_gap_id (UUID, FK) → care_gaps.id
├── intervention_type (VARCHAR 100) - Type of action
├── description (TEXT)
├── performed_by (VARCHAR 255) - User or system
├── performed_date (TIMESTAMP)
├── outcome (VARCHAR 100) - Result of intervention
└── notes (TEXT)
```

**Indexes**:
- `idx_interventions_care_gap` - Find interventions for gap
- `idx_interventions_performed_date` - Sort by date

---

### Notification Schema

#### templates Table
Stores reusable notification templates.

```
templates
├── id (UUID, PK)
├── template_id (VARCHAR 100, UK) - Template identifier
├── name (VARCHAR 255)
├── channel (VARCHAR 50, CHECK) - EMAIL | SMS | PUSH | IN_APP
├── subject (VARCHAR 255) - Email subject
├── body (TEXT) - Template body with {{variables}}
├── variables (JSONB) - Available variables
└── active (BOOLEAN, default true)
```

**Indexes**:
- `idx_template_channel` - Find templates by channel

#### history Table
Tracks all sent notifications for audit and analytics.

```
history
├── id (UUID, PK)
├── notification_id (VARCHAR 100) - Unique notification ID
├── patient_id (UUID, FK) → patients.id (SET NULL on delete)
├── tenant_id (VARCHAR 50)
├── template_id (VARCHAR 100, FK) → templates.template_id
├── channel (VARCHAR 50)
├── recipient (VARCHAR 255) - Email/phone/user ID
├── subject (VARCHAR 255)
├── content (TEXT) - Rendered content
├── status (VARCHAR 50) - PENDING | SENT | DELIVERED | FAILED
├── sent_at (TIMESTAMP)
├── delivered_at (TIMESTAMP)
├── failed_at (TIMESTAMP)
├── error_message (TEXT)
└── metadata (JSONB)
```

**Indexes**:
- `idx_notif_patient` - Find notifications for patient
- `idx_notif_status` - Filter by status
- `idx_notif_sent` - Sort by sent date
- `idx_notif_tenant` - Tenant isolation
- `idx_notifications_recipient` - Recipient lookup
- `idx_notification_sent_status` - Sent + status filtering
- `idx_notif_metadata_gin` - JSONB metadata search

#### preferences Table
Stores user notification preferences.

```
preferences
├── id (UUID, PK)
├── patient_id (UUID, FK) → patients.id
├── tenant_id (VARCHAR 50)
├── email_enabled (BOOLEAN, default true)
├── sms_enabled (BOOLEAN, default true)
├── push_enabled (BOOLEAN, default true)
├── in_app_enabled (BOOLEAN, default true)
├── quiet_hours_start (TIME)
├── quiet_hours_end (TIME)
├── timezone (VARCHAR 50)
└── updated_at (TIMESTAMP)
```

**Indexes**:
- `idx_preferences_patient` - Patient preferences
- `idx_preferences_tenant` - Tenant isolation

---

### Audit Schema

#### audit_log Table
Tracks entity-level changes for compliance.

```
audit_log
├── id (UUID, PK)
├── entity_type (VARCHAR 100) - Entity class name
├── entity_id (VARCHAR 100) - Entity ID
├── action (VARCHAR 50) - CREATE | UPDATE | DELETE
├── user_id (VARCHAR 100) - User making change
├── user_name (VARCHAR 255)
├── tenant_id (VARCHAR 50)
├── timestamp (TIMESTAMP)
├── ip_address (VARCHAR 45)
├── user_agent (TEXT)
├── old_values (JSONB) - Previous values
├── new_values (JSONB) - New values
└── metadata (JSONB)
```

**Indexes**:
- `idx_audit_entity` - Find changes to entity
- `idx_audit_user` - Find user's changes
- `idx_audit_timestamp` - Sort by timestamp
- `idx_audit_tenant` - Tenant isolation
- `idx_audit_action` - Filter by action type
- `idx_audit_entity_action` - Entity + action
- `idx_audit_user_tenant` - User + tenant queries
- `idx_audit_timestamp_action` - Time + action
- `idx_audit_old_values_gin` - Search old values
- `idx_audit_new_values_gin` - Search new values

#### access_log Table
Tracks user login/logout and authentication.

```
access_log
├── id (UUID, PK)
├── user_id (VARCHAR 100)
├── tenant_id (VARCHAR 50)
├── login_time (TIMESTAMP)
├── logout_time (TIMESTAMP)
├── ip_address (VARCHAR 45)
├── user_agent (TEXT)
├── session_id (VARCHAR 255)
├── status (VARCHAR 50) - SUCCESS | FAILED | TIMEOUT
└── failure_reason (VARCHAR 255)
```

**Indexes**:
- `idx_access_user` - User access history
- `idx_access_tenant` - Tenant logins
- `idx_access_login_time` - Sort by login
- `idx_access_status` - Filter by result
- `idx_access_user_tenant_time` - User + tenant + time

#### data_change_log Table
Detailed field-level change tracking for sensitive data.

```
data_change_log
├── id (UUID, PK)
├── table_name (VARCHAR 100) - Table modified
├── record_id (VARCHAR 100) - Record ID
├── column_name (VARCHAR 100) - Column changed
├── old_value (TEXT)
├── new_value (TEXT)
├── changed_by (VARCHAR 100)
├── change_timestamp (TIMESTAMP)
├── tenant_id (VARCHAR 50)
└── reason (VARCHAR 255)
```

**Indexes**:
- `idx_data_change_table_record` - Find field changes
- `idx_data_change_timestamp` - Sort by timestamp
- `idx_data_change_tenant` - Tenant isolation
- `idx_data_change_user` - Changes by user

---

## Entity Relationships

### Hierarchy
```
patients (root aggregate)
    ├── patient_identifiers (1:many)
    ├── observations (1:many)
    ├── conditions (1:many)
    ├── medication_requests (1:many)
    ├── measure_results (1:many)
    ├── health_scores (1:many)
    ├── health_score_history (1:many)
    ├── care_gaps (1:many)
    │   └── interventions (1:many)
    ├── notification history (1:many)
    └── notification preferences (1:1)

measures (independent)
    ├── care_gaps (1:many)
    └── measure_results (1:many)

notification.templates (independent)
    └── notification.history (1:many)
```

## Data Flow Example

```
Patient Created
  ↓
Default Notification Preferences Auto-Created (trigger)
  ↓
FHIR Data Ingested (observations, conditions, medications)
  ↓
Quality Measures Evaluated → measure_results created
  ↓
Health Score Calculated → health_scores created
  ↓
Care Gaps Identified → care_gaps created
  ↓
Notifications Sent → notification.history created
  ↓
All Changes Logged → audit_log entries created
```

## Multi-Tenancy Design

All tables include `tenant_id` column for complete isolation:

```sql
-- Example: Ensure data isolation
SELECT * FROM patient.patients
WHERE tenant_id = 'tenant-123' AND active = true;

-- All queries should filter by tenant_id
SELECT * FROM quality.measure_results
WHERE tenant_id = 'tenant-123' AND patient_id = ?;
```

## Performance Considerations

### Query Examples with Index Usage

```sql
-- Fast: Uses idx_patients_active_tenant
SELECT * FROM patient.patients
WHERE tenant_id = ? AND active = true;

-- Fast: Uses idx_obs_patient_code
SELECT * FROM fhir.observations
WHERE patient_id = ? AND code = 'LOINC-CODE';

-- Fast: Uses idx_gaps_patient_status
SELECT * FROM caregap.care_gaps
WHERE patient_id = ? AND status = 'OPEN'
ORDER BY due_date ASC;

-- Fast: Uses idx_scores_patient and DESC index
SELECT * FROM quality.health_scores
WHERE patient_id = ?
ORDER BY calculated_at DESC LIMIT 1;

-- Fast: Uses JSONB GIN index
SELECT * FROM fhir.observations
WHERE fhir_resource @> '{"status":"final"}'
AND tenant_id = ?;
```

---

**Schema Version**: 1.0
**Last Updated**: December 2024
**Database**: PostgreSQL 15+
**Status**: Production Ready
