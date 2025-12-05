-- ============================================================================
-- DATABASE INDEXES FOR SPRING DATA JPA REPOSITORIES
-- HealthData Platform
-- ============================================================================
-- These indexes are recommended to optimize the queries used by the
-- Spring Data JPA repositories. They support the most common access patterns.
-- ============================================================================

-- ============================================================================
-- PATIENT TABLE INDEXES
-- ============================================================================

-- MRN lookup with tenant isolation (findByMrnAndTenantId)
CREATE INDEX IF NOT EXISTS idx_patient_tenant_mrn
ON patient.patients(tenant_id, mrn);

-- Active patients by tenant (findAllActivePatientsForTenant)
CREATE INDEX IF NOT EXISTS idx_patient_tenant_active
ON patient.patients(tenant_id, active)
WHERE active = true;

-- Patient search by name (searchPatients)
CREATE INDEX IF NOT EXISTS idx_patient_tenant_firstName
ON patient.patients(tenant_id, first_name);

CREATE INDEX IF NOT EXISTS idx_patient_tenant_lastName
ON patient.patients(tenant_id, last_name);

-- Recent patient activity (findRecentlyActivePatients)
CREATE INDEX IF NOT EXISTS idx_patient_updated_at
ON patient.patients(updated_at DESC)
WHERE active = true;

-- ============================================================================
-- OBSERVATION TABLE INDEXES
-- ============================================================================

-- Observations by patient and code (findByPatientIdAndCode, findLatestByPatientIdAndCode)
CREATE INDEX IF NOT EXISTS idx_observation_patient_code
ON fhir.observations(patient_id, code);

-- Observations by patient and date range (findByPatientIdAndDateRange)
CREATE INDEX IF NOT EXISTS idx_observation_patient_effectiveDate
ON fhir.observations(patient_id, effective_date DESC);

-- Observations by patient and category (findByPatientIdAndCategoryOrderByEffectiveDateDesc)
CREATE INDEX IF NOT EXISTS idx_observation_patient_category
ON fhir.observations(patient_id, category, effective_date DESC);

-- Tenant observations by system (findByTenantIdAndSystem)
CREATE INDEX IF NOT EXISTS idx_observation_tenant_system
ON fhir.observations(tenant_id, system);

-- Status filtering (findByPatientIdAndStatus)
CREATE INDEX IF NOT EXISTS idx_observation_patient_status
ON fhir.observations(patient_id, status);

-- ============================================================================
-- CONDITION TABLE INDEXES
-- ============================================================================

-- Active conditions by patient (findActiveConditionsByPatientId)
CREATE INDEX IF NOT EXISTS idx_condition_patient_status
ON fhir.conditions(patient_id, clinical_status)
WHERE clinical_status = 'active';

-- Conditions by patient and code (findByPatientIdAndCode)
CREATE INDEX IF NOT EXISTS idx_condition_patient_code
ON fhir.conditions(patient_id, code);

-- Conditions by patient and onset date (findByPatientIdAndOnsetDateBetween)
CREATE INDEX IF NOT EXISTS idx_condition_patient_onsetDate
ON fhir.conditions(patient_id, onset_date DESC);

-- Conditions by patient and category (findByPatientIdAndCategory)
CREATE INDEX IF NOT EXISTS idx_condition_patient_category
ON fhir.conditions(patient_id, category);

-- Conditions by patient and severity (findByPatientIdAndSeverity)
CREATE INDEX IF NOT EXISTS idx_condition_patient_severity
ON fhir.conditions(patient_id, severity);

-- Severe active conditions (findSevereActiveConditions)
CREATE INDEX IF NOT EXISTS idx_condition_patient_severity_status
ON fhir.conditions(patient_id, severity, clinical_status)
WHERE severity = 'severe' AND clinical_status = 'active';

-- Resolved conditions by date (findResolvedConditionsBetweenDates)
CREATE INDEX IF NOT EXISTS idx_condition_patient_abatementDate
ON fhir.conditions(patient_id, abatement_date DESC);

-- Tenant conditions by code (findByTenantIdAndCode)
CREATE INDEX IF NOT EXISTS idx_condition_tenant_code
ON fhir.conditions(tenant_id, code);

-- Recent conditions (findRecentByPatientId)
CREATE INDEX IF NOT EXISTS idx_condition_patient_recordedDate
ON fhir.conditions(patient_id, recorded_date DESC);

-- ============================================================================
-- MEDICATION REQUEST TABLE INDEXES
-- ============================================================================

-- Active medications (findActiveByPatientId)
CREATE INDEX IF NOT EXISTS idx_medication_patient_status
ON fhir.medication_requests(patient_id, status)
WHERE status = 'active';

-- Medications expiring soon (findMedicationsExpiringWithinDays)
CREATE INDEX IF NOT EXISTS idx_medication_patient_validPeriodEnd
ON fhir.medication_requests(patient_id, valid_period_end DESC)
WHERE status = 'active';

-- Medications by code (findByPatientIdAndMedicationCode)
CREATE INDEX IF NOT EXISTS idx_medication_patient_code
ON fhir.medication_requests(patient_id, medication_code);

-- Current medications by date (findCurrentMedicationsByPatientId)
CREATE INDEX IF NOT EXISTS idx_medication_patient_authoredOn
ON fhir.medication_requests(patient_id, authored_on DESC)
WHERE status = 'active';

-- Refillable medications (findRefillableByPatientId)
CREATE INDEX IF NOT EXISTS idx_medication_patient_refills
ON fhir.medication_requests(patient_id, refills_remaining DESC)
WHERE status = 'active' AND refills_remaining > 0;

-- Medications by prescriber (findByPrescriberId)
CREATE INDEX IF NOT EXISTS idx_medication_prescriber
ON fhir.medication_requests(prescriber_id);

-- Recent medications (findRecentByPatientId)
CREATE INDEX IF NOT EXISTS idx_medication_patient_authored
ON fhir.medication_requests(patient_id, authored_on DESC);

-- Medications by priority (findByTenantIdAndPriority)
CREATE INDEX IF NOT EXISTS idx_medication_tenant_priority
ON fhir.medication_requests(tenant_id, priority)
WHERE status = 'active';

-- Medications by reason (findActiveMedicationsByTenantAndCondition)
CREATE INDEX IF NOT EXISTS idx_medication_tenant_reason
ON fhir.medication_requests(tenant_id, reason_code)
WHERE status = 'active';

-- Medications by date range (findByPatientIdAndAuthoredOnBetween)
CREATE INDEX IF NOT EXISTS idx_medication_patient_authored_range
ON fhir.medication_requests(patient_id, authored_on);

-- ============================================================================
-- QUALITY MEASURE RESULT TABLE INDEXES
-- ============================================================================

-- Results by patient and measure (findByPatientIdAndMeasureId)
CREATE INDEX IF NOT EXISTS idx_measure_result_patient_measure
ON quality.measure_results(patient_id, measure_id);

-- Results by measure and tenant (findByMeasureIdAndTenant)
CREATE INDEX IF NOT EXISTS idx_measure_result_measure_tenant
ON quality.measure_results(measure_id, tenant_id, calculation_date DESC);

-- Latest results by patient (findLatestResultsByPatient)
CREATE INDEX IF NOT EXISTS idx_measure_result_patient_calculated
ON quality.measure_results(patient_id, calculation_date DESC);

-- Compliant results (findCompliantResultsByPatient, findByTenantIdAndCompliant)
CREATE INDEX IF NOT EXISTS idx_measure_result_patient_compliant
ON quality.measure_results(patient_id, compliant, calculation_date DESC);

CREATE INDEX IF NOT EXISTS idx_measure_result_tenant_compliant
ON quality.measure_results(tenant_id, compliant, calculation_date DESC);

-- Measurement period queries (findByTenantAndMeasurementPeriod)
CREATE INDEX IF NOT EXISTS idx_measure_result_tenant_period
ON quality.measure_results(tenant_id, period_start, period_end);

-- Results within date range (findByPatientIdAndCalculationDateRange)
CREATE INDEX IF NOT EXISTS idx_measure_result_patient_calculated_range
ON quality.measure_results(patient_id, calculation_date);

-- Low performance results (findBelowPerformanceThreshold)
CREATE INDEX IF NOT EXISTS idx_measure_result_tenant_score
ON quality.measure_results(tenant_id, score ASC);

-- Top performers (findTopPerformers)
CREATE INDEX IF NOT EXISTS idx_measure_result_tenant_score_desc
ON quality.measure_results(tenant_id, score DESC);

-- Denominator status (findByPatientAndMeasureInDenominator)
CREATE INDEX IF NOT EXISTS idx_measure_result_patient_measure_denominator
ON quality.measure_results(patient_id, measure_id)
WHERE denominator IS NOT NULL AND denominator > 0;

-- ============================================================================
-- CARE GAP TABLE INDEXES
-- ============================================================================

-- Open gaps by patient (findOpenGapsByPatient)
CREATE INDEX IF NOT EXISTS idx_care_gap_patient_status
ON caregap.care_gaps(patient_id, status)
WHERE status = 'OPEN';

-- Gaps by type and priority (findGapsByTypeAndPriority)
CREATE INDEX IF NOT EXISTS idx_care_gap_type_priority
ON caregap.care_gaps(gap_type, priority, status)
WHERE status = 'OPEN';

-- Overdue gaps (findOverdueGaps)
CREATE INDEX IF NOT EXISTS idx_care_gap_tenant_due
ON caregap.care_gaps(tenant_id, due_date ASC)
WHERE status = 'OPEN';

-- Gaps by status and tenant (countGapsByStatusAndTenant)
CREATE INDEX IF NOT EXISTS idx_care_gap_tenant_status
ON caregap.care_gaps(tenant_id, status);

-- High-risk gaps (findHighRiskGaps)
CREATE INDEX IF NOT EXISTS idx_care_gap_risk_score
ON caregap.care_gaps(risk_score DESC)
WHERE status = 'OPEN';

-- Gaps due soon (findGapsDueSoon)
CREATE INDEX IF NOT EXISTS idx_care_gap_tenant_due_soon
ON caregap.care_gaps(tenant_id, due_date ASC);

-- High-impact gaps (findHighImpactGaps)
CREATE INDEX IF NOT EXISTS idx_care_gap_financial_impact
ON caregap.care_gaps(tenant_id, financial_impact DESC)
WHERE status = 'OPEN';

-- Recently closed gaps (findRecentlyClosedGaps)
CREATE INDEX IF NOT EXISTS idx_care_gap_patient_closed
ON caregap.care_gaps(patient_id, closed_date DESC)
WHERE status = 'CLOSED';

-- Gap by provider (findByProviderId)
CREATE INDEX IF NOT EXISTS idx_care_gap_provider
ON caregap.care_gaps(provider_id);

-- Gap by care team (findByCareTeamId)
CREATE INDEX IF NOT EXISTS idx_care_gap_care_team
ON caregap.care_gaps(care_team_id);

-- Gap by measure (findByMeasureId)
CREATE INDEX IF NOT EXISTS idx_care_gap_measure
ON caregap.care_gaps(measure_id);

-- Due date range (findByPatientIdAndDueDateBetween)
CREATE INDEX IF NOT EXISTS idx_care_gap_patient_due_range
ON caregap.care_gaps(patient_id, due_date);

-- ============================================================================
-- AUDIT LOG TABLE INDEXES
-- ============================================================================

-- Logs by user (findByUserId)
CREATE INDEX IF NOT EXISTS idx_audit_log_user_created
ON shared.audit_logs(user_id, created_at DESC);

-- Logs by entity type (findByEntityType)
CREATE INDEX IF NOT EXISTS idx_audit_log_entity_type
ON shared.audit_logs(entity_type, created_at DESC);

-- Logs by date range (findByDateRange)
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at
ON shared.audit_logs(created_at DESC);

-- Logs for specific entity (findChangesByEntity)
CREATE INDEX IF NOT EXISTS idx_audit_log_entity_id
ON shared.audit_logs(entity_type, entity_id, created_at DESC);

-- Logs by action type (findByActionType)
CREATE INDEX IF NOT EXISTS idx_audit_log_action_type
ON shared.audit_logs(action_type, created_at DESC);

-- Sensitive actions (findSensitiveActions)
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_action_type
ON shared.audit_logs(tenant_id, action_type)
WHERE action_type IN ('DELETE', 'EXPORT', 'MODIFY_SECURITY');

-- Failed access attempts (findFailedAccessAttempts)
CREATE INDEX IF NOT EXISTS idx_audit_log_action_status
ON shared.audit_logs(action_type, status, created_at DESC)
WHERE action_type = 'LOGIN' AND status = 'FAILED';

-- Patient access (findPatientAccessLogs)
CREATE INDEX IF NOT EXISTS idx_audit_log_patient_access
ON shared.audit_logs(entity_type, entity_id, created_at DESC)
WHERE entity_type = 'Patient';

-- Deletion audit (findDeletionAudit)
CREATE INDEX IF NOT EXISTS idx_audit_log_deletion
ON shared.audit_logs(action_type, entity_type, created_at DESC)
WHERE action_type = 'DELETE';

-- Rapid access attempts (findRapidAccessAttempts)
CREATE INDEX IF NOT EXISTS idx_audit_log_user_created_recent
ON shared.audit_logs(user_id, created_at DESC);

-- Count by tenant (countByTenantId)
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant
ON shared.audit_logs(tenant_id);

-- Expired logs (findExpiredLogs)
CREATE INDEX IF NOT EXISTS idx_audit_log_created_asc
ON shared.audit_logs(created_at ASC);

-- Recent sensitive actions (findRecentSensitiveActions)
CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_action_recent
ON shared.audit_logs(tenant_id, action_type, created_at DESC);

-- ============================================================================
-- COMPOSITE/MULTI-COLUMN INDEXES
-- ============================================================================

-- Patient full-text search optimization
CREATE INDEX IF NOT EXISTS idx_patient_search
ON patient.patients(tenant_id, active, last_name, first_name);

-- Observation trend analysis
CREATE INDEX IF NOT EXISTS idx_observation_trend
ON fhir.observations(patient_id, code, effective_date DESC);

-- Condition status reporting
CREATE INDEX IF NOT EXISTS idx_condition_status_report
ON fhir.conditions(tenant_id, clinical_status, code);

-- Medication adherence tracking
CREATE INDEX IF NOT EXISTS idx_medication_adherence
ON fhir.medication_requests(patient_id, status, valid_period_end);

-- Measure compliance reporting
CREATE INDEX IF NOT EXISTS idx_measure_compliance
ON quality.measure_results(tenant_id, measure_id, compliant, calculation_date DESC);

-- Care gap prioritization
CREATE INDEX IF NOT EXISTS idx_care_gap_prioritization
ON caregap.care_gaps(tenant_id, priority, status, due_date);

-- ============================================================================
-- STATISTICS AND ANALYSIS
-- ============================================================================

-- Analyze all indexes
ANALYZE patient.patients;
ANALYZE fhir.observations;
ANALYZE fhir.conditions;
ANALYZE fhir.medication_requests;
ANALYZE quality.measure_results;
ANALYZE caregap.care_gaps;
ANALYZE shared.audit_logs;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- List all created indexes
/*
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE schemaname IN ('patient', 'fhir', 'quality', 'caregap', 'shared')
ORDER BY schemaname, tablename, indexname;

-- Check index usage statistics
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname IN ('patient', 'fhir', 'quality', 'caregap', 'shared')
ORDER BY schemaname, tablename;

-- Identify unused indexes
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND schemaname IN ('patient', 'fhir', 'quality', 'caregap', 'shared')
ORDER BY schemaname, tablename;
*/

-- ============================================================================
-- INDEX MAINTENANCE
-- ============================================================================

-- Rebuild fragmented indexes (if needed)
-- REINDEX INDEX CONCURRENTLY idx_patient_tenant_mrn;

-- Vacuum to reclaim space from deleted rows
-- VACUUM ANALYZE patient.patients;
-- VACUUM ANALYZE fhir.observations;
-- VACUUM ANALYZE fhir.conditions;
-- VACUUM ANALYZE fhir.medication_requests;
-- VACUUM ANALYZE quality.measure_results;
-- VACUUM ANALYZE caregap.care_gaps;
-- VACUUM ANALYZE shared.audit_logs;

-- ============================================================================
-- NOTES
-- ============================================================================
/*
1. All indexes use the default B-tree algorithm (appropriate for most queries)
2. PARTIAL INDEXES (WHERE clauses) reduce size for filtered queries
3. COMPOSITE INDEXES support multi-column queries efficiently
4. Foreign key columns could benefit from additional indexes if joins are common
5. Regular ANALYZE commands update planner statistics
6. Consider index bloat and REINDEX if performance degrades
7. Monitor slow query logs to identify missing indexes
8. Drop unused indexes to reduce insert/update overhead
*/
