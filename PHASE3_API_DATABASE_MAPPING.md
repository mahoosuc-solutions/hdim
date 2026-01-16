# Phase 3: API-to-Database Mapping

**Date**: January 15, 2026  
**Status**: ✅ **COMPLETE**

---

## Mapping Format

**API Endpoint** → **Controller Method** → **Service Method** → **Repository Method** → **Database Table** → **Columns Used** → **Indexes Used**

---

## FHIR Service

### Patient Endpoints

| API Endpoint | Controller | Service | Repository | Table | Columns | Indexes |
|--------------|------------|---------|------------|-------|---------|---------|
| `POST /fhir/Patient` | `FhirPatientController.createPatient()` | `FhirPatientService.create()` | `PatientRepository.save()` | `patients` | All columns | Primary key |
| `GET /fhir/Patient/{id}` | `FhirPatientController.getPatient()` | `FhirPatientService.findById()` | `PatientRepository.findById()` | `patients` | All columns | Primary key |
| `GET /fhir/Patient?name={name}` | `FhirPatientController.searchPatients()` | `FhirPatientService.search()` | `PatientRepository.findByTenantIdAndLastNameContaining()` | `patients` | `tenant_id`, `last_name` | `idx_patients_tenant_lastname` |
| `PUT /fhir/Patient/{id}` | `FhirPatientController.updatePatient()` | `FhirPatientService.update()` | `PatientRepository.save()` | `patients` | All columns | Primary key, version |

**Indexes Used**:
- `patients_pkey` - Primary key lookups
- `idx_patients_tenant_lastname` - Name searches
- `idx_patients_tenant_birthdate` - Date-based queries
- `idx_patients_resource_json_gin` - JSONB queries

---

## Patient Service

### Patient Aggregation Endpoints

| API Endpoint | Controller | Service | Repository | Table | Columns | Indexes |
|--------------|------------|---------|------------|-------|---------|---------|
| `GET /api/v1/patients` | `PatientApiController.listPatients()` | - | `PatientDemographicsRepository.findByTenantIdAndActiveTrue()` | `patient_demographics` | `tenant_id`, `active` | Tenant + active index |
| `GET /patient/health-record` | `PatientController.getComprehensiveHealthRecord()` | `PatientAggregationService.getComprehensiveHealthRecord()` | Calls FHIR service | `patients` (via FHIR) | All columns | Various |
| `GET /patient/timeline` | `PatientController.getTimeline()` | `PatientTimelineService.getTimeline()` | Calls FHIR service | Multiple FHIR tables | Various | Various |

**Note**: Patient service aggregates data from FHIR service - doesn't directly query patient_demographics table in most cases

---

## Notification Service

### Notification Endpoints

| API Endpoint | Controller | Service | Repository | Table | Columns | Indexes |
|--------------|------------|---------|------------|-------|---------|---------|
| `POST /api/v1/notifications` | `NotificationController.sendNotification()` | `NotificationService.sendNotification()` | `NotificationRepository.save()` | `notifications` | All columns | Primary key |
| `GET /api/v1/notifications/{id}` | `NotificationController.getNotification()` | `NotificationService.getNotification()` | `NotificationRepository.findById()` | `notifications` | All columns | Primary key |
| `GET /api/v1/notifications` | `NotificationController.getNotifications()` | `NotificationService.getNotifications()` | `NotificationRepository.findByTenantId()` | `notifications` | `tenant_id` | `idx_notifications_tenant_id` |

**Indexes Used**:
- Primary key - ID lookups
- `idx_notifications_tenant_id` - Tenant filtering
- `idx_notifications_recipient_id` - Recipient filtering
- `idx_notifications_status` - Status filtering
- `idx_notifications_created_at` - Time-based queries

---

## Query Performance Validation

### Audit Module Queries

| Repository Method | Query Pattern | Required Indexes | Status |
|-------------------|---------------|------------------|--------|
| `AuditEventRepository.findByTenantIdAndTimestampBetween()` | `tenant_id`, `timestamp` | `idx_audit_tenant_timestamp` | ✅ EXISTS |
| `QAReviewRepository.findFlagged()` | `tenant_id`, `review_status`, `flag_type` | `idx_qa_tenant_status` | ⚠️ Table missing |
| `AIAgentDecisionEventRepository.findByTenantIdAndTimestampBetween()` | `tenant_id`, `timestamp` | `idx_ai_decision_tenant_timestamp` | ⚠️ Table missing |

### FHIR Service Queries

| Repository Method | Query Pattern | Required Indexes | Status |
|-------------------|---------------|------------------|--------|
| `PatientRepository.findByTenantIdAndLastNameContaining()` | `tenant_id`, `last_name` | `idx_patients_tenant_lastname` | ✅ EXISTS |
| `PatientRepository.findByTenantIdAndBirthDateBetween()` | `tenant_id`, `birth_date` | `idx_patients_tenant_birthdate` | ✅ EXISTS |

### Patient Service Queries

| Repository Method | Query Pattern | Required Indexes | Status |
|-------------------|---------------|------------------|--------|
| `PatientDemographicsRepository.findByTenantIdAndActiveTrue()` | `tenant_id`, `active` | Composite index needed | ⚠️ Table missing |

### Notification Service Queries

| Repository Method | Query Pattern | Required Indexes | Status |
|-------------------|---------------|------------------|--------|
| `NotificationRepository.findByTenantId()` | `tenant_id` | `idx_notifications_tenant_id` | ✅ EXISTS |
| `NotificationRepository.findByRecipientId()` | `recipient_id` | `idx_notifications_recipient_id` | ✅ EXISTS |

---

## Summary

### ✅ Validated
- FHIR service: PatientEntity matches database
- Notification service: Tables exist and match entities
- Indexes exist for most query patterns

### ⚠️ Issues Found
- Patient service: Database empty (migrations need to run)
- Audit module: 7 tables missing (migrations created)
- Some indexes missing for missing tables

### Next Steps
1. Run migrations to create missing tables
2. Verify indexes are created
3. Re-test integration tests

---

**Phase 3 Status**: ✅ **COMPLETE - Mapping documented**
