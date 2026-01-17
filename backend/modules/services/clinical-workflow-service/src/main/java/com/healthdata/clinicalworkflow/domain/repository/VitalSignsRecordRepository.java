package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.domain.model.VitalSignsRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VitalSignsRecordEntity
 *
 * Provides data access for vital signs records with multi-tenant isolation.
 * Includes queries for alert detection and trend analysis.
 *
 * HIPAA Compliance:
 * - All PHI access is audited
 * - Cache TTL must be <= 5 minutes
 * - Multi-tenant filtering enforced on all queries
 */
@Repository
@Transactional(readOnly = true)
public interface VitalSignsRecordRepository extends JpaRepository<VitalSignsRecordEntity, UUID> {

    /**
     * Find vital signs by ID and tenant
     *
     * @param id UUID of the vital signs record
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the vital signs entity if found
     */
    Optional<VitalSignsRecordEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find abnormal vitals by tenant and alert status
     * Used for MA dashboard to show patients requiring attention
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param alertStatus Alert status (warning, critical)
     * @return List of vital signs records with abnormal values
     */
    @Query("""
        SELECT v FROM VitalSignsRecordEntity v
        WHERE v.tenantId = :tenantId
        AND v.alertStatus = :alertStatus
        ORDER BY v.recordedAt DESC
    """)
    List<VitalSignsRecordEntity> findAbnormalVitalsByTenant(
        @Param("tenantId") String tenantId,
        @Param("alertStatus") String alertStatus
    );

    /**
     * Find patient vitals history within date range
     * Used for trend analysis and clinical decision support
     *
     * @param patientId UUID of the patient
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param from Start of date range
     * @param to End of date range
     * @return List of vital signs records within the date range
     */
    @Query("""
        SELECT v FROM VitalSignsRecordEntity v
        WHERE v.tenantId = :tenantId
        AND v.patientId = :patientId
        AND v.recordedAt >= :from
        AND v.recordedAt <= :to
        ORDER BY v.recordedAt DESC
    """)
    List<VitalSignsRecordEntity> findPatientVitalsHistory(
        @Param("patientId") UUID patientId,
        @Param("tenantId") String tenantId,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to
    );

    /**
     * Find vitals by alert status and tenant
     * Used to filter vitals requiring clinical review
     *
     * @param alertStatus Alert status (normal, warning, critical)
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of vital signs records with the specified alert status
     */
    @Query("""
        SELECT v FROM VitalSignsRecordEntity v
        WHERE v.tenantId = :tenantId
        AND v.alertStatus = :alertStatus
        ORDER BY v.recordedAt DESC
    """)
    List<VitalSignsRecordEntity> findByAlertStatusAndTenant(
        @Param("alertStatus") String alertStatus,
        @Param("tenantId") String tenantId
    );

    /**
     * Find latest vital signs for a patient
     * Returns the most recent vital signs record
     *
     * @param patientId UUID of the patient
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the latest vital signs if found
     */
    @Query("""
        SELECT v FROM VitalSignsRecordEntity v
        WHERE v.tenantId = :tenantId
        AND v.patientId = :patientId
        ORDER BY v.recordedAt DESC
        LIMIT 1
    """)
    Optional<VitalSignsRecordEntity> findLatestVitalForPatient(
        @Param("patientId") UUID patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all vitals for encounter
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param encounterId FHIR Encounter resource ID
     * @return List of vital signs records for the encounter
     */
    List<VitalSignsRecordEntity> findByTenantIdAndEncounterIdOrderByRecordedAtDesc(
        String tenantId,
        String encounterId
    );

    /**
     * Find vitals recorded by specific staff
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param recordedBy Staff identifier who recorded the vitals
     * @return List of vital signs records by the staff member
     */
    List<VitalSignsRecordEntity> findByTenantIdAndRecordedByOrderByRecordedAtDesc(
        String tenantId,
        String recordedBy
    );

    /**
     * Find all vitals for a patient
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param patientId UUID of the patient
     * @return List of all vital signs records for the patient
     */
    List<VitalSignsRecordEntity> findByTenantIdAndPatientIdOrderByRecordedAtDesc(
        String tenantId,
        UUID patientId
    );

    /**
     * Count critical alerts for tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Count of critical vitals requiring immediate attention
     */
    @Query("""
        SELECT COUNT(v) FROM VitalSignsRecordEntity v
        WHERE v.tenantId = :tenantId
        AND v.alertStatus = 'critical'
    """)
    long countCriticalAlertsByTenant(@Param("tenantId") String tenantId);
}
