package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PatientCheckInEntity
 *
 * Provides data access methods for patient check-in tracking with multi-tenant isolation.
 * All queries filter by tenant_id to ensure multi-tenant data isolation.
 *
 * HIPAA Compliance:
 * - All PHI access is audited
 * - Cache TTL must be <= 5 minutes
 * - Multi-tenant filtering enforced on all queries
 */
@Repository
@Transactional(readOnly = true)
public interface PatientCheckInRepository extends JpaRepository<PatientCheckInEntity, UUID> {

    /**
     * Find check-in by ID and tenant
     *
     * @param id UUID of the check-in record
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the check-in entity if found
     */
    Optional<PatientCheckInEntity> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Find check-in record by patient ID and tenant ID
     * Returns the most recent check-in for the patient
     *
     * @param patientId UUID of the patient
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the most recent check-in entity if found
     */
    @Query("""
        SELECT c FROM PatientCheckInEntity c
        WHERE c.tenantId = :tenantId
        AND c.patientId = :patientId
        ORDER BY c.checkInTime DESC
        LIMIT 1
    """)
    Optional<PatientCheckInEntity> findByPatientIdAndTenantId(
        @Param("patientId") UUID patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all check-ins for today by tenant
     * Uses PostgreSQL DATE function to filter by calendar date
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param date The target date for check-ins
     * @return List of check-in entities for the specified date
     */
    @Query("""
        SELECT c FROM PatientCheckInEntity c
        WHERE c.tenantId = :tenantId
        AND CAST(c.checkInTime AS LocalDate) = :date
        ORDER BY c.checkInTime DESC
    """)
    List<PatientCheckInEntity> findTodayCheckInsByTenant(
        @Param("tenantId") String tenantId,
        @Param("date") LocalDate date
    );

    /**
     * Find check-in record by appointment ID and tenant ID
     *
     * @param appointmentId FHIR Appointment resource ID
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return Optional containing the check-in entity if found
     */
    @Query("""
        SELECT c FROM PatientCheckInEntity c
        WHERE c.tenantId = :tenantId
        AND c.appointmentId = :appointmentId
    """)
    Optional<PatientCheckInEntity> findByAppointmentId(
        @Param("appointmentId") String appointmentId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find recent check-ins for a tenant with limit
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param limit Maximum number of records to return
     * @return List of recent check-in entities, ordered by check-in time descending
     */
    @Query("""
        SELECT c FROM PatientCheckInEntity c
        WHERE c.tenantId = :tenantId
        ORDER BY c.checkInTime DESC
        LIMIT :limit
    """)
    List<PatientCheckInEntity> findRecentCheckIns(
        @Param("tenantId") String tenantId,
        @Param("limit") int limit
    );

    /**
     * Find all check-ins for a patient in a tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param patientId UUID of the patient
     * @return List of check-in entities ordered by check-in time descending
     */
    List<PatientCheckInEntity> findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
        String tenantId,
        UUID patientId
    );

    /**
     * Count check-ins for a tenant on a specific date
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param date The target date
     * @return Count of check-ins
     */
    @Query("""
        SELECT COUNT(c) FROM PatientCheckInEntity c
        WHERE c.tenantId = :tenantId
        AND CAST(c.checkInTime AS LocalDate) = :date
    """)
    long countCheckInsByTenantAndDate(
        @Param("tenantId") String tenantId,
        @Param("date") LocalDate date
    );

    /**
     * Count check-ins for patient
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param patientId UUID of the patient
     * @return Count of check-ins for the patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Find check-ins by status and tenant
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @param status Check-in status (e.g., checked-in, waiting, in-room)
     * @return List of check-in entities with the specified status
     */
    List<PatientCheckInEntity> findByTenantIdAndStatusOrderByCheckInTimeDesc(
        String tenantId,
        String status
    );

    /**
     * Find check-ins pending insurance verification
     *
     * @param tenantId Tenant identifier for multi-tenant isolation
     * @return List of check-ins where insurance is not verified
     */
    @Query("""
        SELECT c FROM PatientCheckInEntity c
        WHERE c.tenantId = :tenantId
        AND c.insuranceVerified = false
        ORDER BY c.checkInTime ASC
    """)
    List<PatientCheckInEntity> findPendingInsuranceVerification(
        @Param("tenantId") String tenantId
    );
}
