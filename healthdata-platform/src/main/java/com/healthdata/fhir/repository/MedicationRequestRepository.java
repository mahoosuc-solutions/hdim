package com.healthdata.fhir.repository;

import com.healthdata.fhir.domain.MedicationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for FHIR MedicationRequest entities
 * Provides comprehensive access to medication prescriptions and orders
 * Supports complex queries, pagination, and tenant isolation
 * Includes medication adherence tracking and refill management
 */
@Repository
public interface MedicationRequestRepository extends JpaRepository<MedicationRequest, String> {

    /**
     * Find medication requests by patient ID
     *
     * @param patientId Patient identifier
     * @return List of medication requests for the patient
     */
    List<MedicationRequest> findByPatientId(String patientId);

    /**
     * Find medication requests by patient ID and status
     * Status values include: active, on-hold, cancelled, completed, entered-in-error, stopped, draft
     *
     * @param patientId Patient identifier
     * @param status Medication request status
     * @return List of medication requests with the specified status
     */
    List<MedicationRequest> findByPatientIdAndStatus(String patientId, String status);

    /**
     * Find active medication requests for a patient
     * Only returns medication requests with status = 'active'
     *
     * @param patientId Patient identifier
     * @return List of active medication requests
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.status = 'active'
        ORDER BY mr.authoredOn DESC
        """)
    List<MedicationRequest> findActiveByPatientId(@Param("patientId") String patientId);

    /**
     * Find medication requests by patient ID and medication code
     * Used for finding prescriptions of specific medications
     *
     * @param patientId Patient identifier
     * @param medicationCode Medication code (RxNorm or other system)
     * @return List of medication requests for the specified medication
     */
    List<MedicationRequest> findByPatientIdAndMedicationCode(String patientId, String medicationCode);

    /**
     * Find current active medication requests for a patient
     * Only returns medications that are active and within their valid period
     *
     * @param patientId Patient identifier
     * @param currentDate Current date for comparison
     * @return List of current active medications
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.validPeriodEnd >= :currentDate
        AND mr.status = 'active'
        ORDER BY mr.authoredOn DESC
        """)
    List<MedicationRequest> findCurrentMedicationsByPatientId(
        @Param("patientId") String patientId,
        @Param("currentDate") LocalDateTime currentDate
    );

    /**
     * Find refillable medication requests for a patient
     * Only returns medications that have refills remaining
     *
     * @param patientId Patient identifier
     * @return List of refillable medication requests
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.refillsRemaining > 0
        AND mr.status = 'active'
        """)
    List<MedicationRequest> findRefillableByPatientId(@Param("patientId") String patientId);

    /**
     * Find medication requests by prescriber ID
     *
     * @param prescriberId Prescriber identifier
     * @return List of medication requests prescribed by the provider
     */
    List<MedicationRequest> findByPrescriberId(String prescriberId);

    /**
     * Find distinct medication codes for a patient
     *
     * @param patientId Patient identifier
     * @return List of unique medication codes
     */
    @Query("SELECT DISTINCT mr.medicationCode FROM MedicationRequest mr WHERE mr.patientId = :patientId")
    List<String> findDistinctMedicationCodesByPatientId(@Param("patientId") String patientId);

    /**
     * Find medication requests by tenant ID
     *
     * @param tenantId Tenant identifier
     * @return List of medication requests for the tenant
     */
    List<MedicationRequest> findByTenantId(String tenantId);

    /**
     * Count active medications for a patient
     *
     * @param patientId Patient identifier
     * @return Count of active medication requests
     */
    @Query("""
        SELECT COUNT(mr) FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.status = 'active'
        """)
    Long countActiveMedicationsByPatientId(@Param("patientId") String patientId);

    /**
     * Find medication requests by patient ID and authored date range
     *
     * @param patientId Patient identifier
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of medication requests authored in the date range
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.authoredOn BETWEEN :startDate AND :endDate
        ORDER BY mr.authoredOn DESC
        """)
    List<MedicationRequest> findByPatientIdAndAuthoredOnBetween(
        @Param("patientId") String patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find active medications for a patient by condition (reason code)
     *
     * @param tenantId Tenant identifier
     * @param conditionCode Condition code for the medication reason
     * @return List of active medications prescribed for the condition
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.tenantId = :tenantId
        AND mr.reasonCode = :conditionCode
        AND mr.status = 'active'
        ORDER BY mr.authoredOn DESC
        """)
    List<MedicationRequest> findActiveMedicationsByTenantAndCondition(
        @Param("tenantId") String tenantId,
        @Param("conditionCode") String conditionCode
    );

    /**
     * Find medication requests that need refills soon
     * Useful for medication adherence and refill management
     *
     * @param patientId Patient identifier
     * @param daysUntilExpiry Days until valid period ends
     * @return List of medications expiring soon
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.status = 'active'
        AND mr.validPeriodEnd <= CURRENT_TIMESTAMP + INTERVAL :daysUntilExpiry day
        ORDER BY mr.validPeriodEnd ASC
        """)
    List<MedicationRequest> findMedicationsExpiringWithinDays(
        @Param("patientId") String patientId,
        @Param("daysUntilExpiry") int daysUntilExpiry
    );

    /**
     * Find recent medications for a patient with pagination
     *
     * @param patientId Patient identifier
     * @param pageable Pagination information
     * @return Paginated list of recent medications
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        ORDER BY mr.authoredOn DESC
        """)
    Page<MedicationRequest> findRecentByPatientId(
        @Param("patientId") String patientId,
        Pageable pageable
    );

    /**
     * Count medications by status for a patient
     *
     * @param patientId Patient identifier
     * @param status Medication status
     * @return Count of medications with the specified status
     */
    @Query("""
        SELECT COUNT(mr) FROM MedicationRequest mr
        WHERE mr.patientId = :patientId
        AND mr.status = :status
        """)
    long countByPatientIdAndStatus(
        @Param("patientId") String patientId,
        @Param("status") String status
    );

    /**
     * Find medications by priority
     *
     * @param tenantId Tenant identifier
     * @param priority Medication priority (routine, urgent, asap, stat)
     * @return List of medications with the specified priority
     */
    @Query("""
        SELECT mr FROM MedicationRequest mr
        WHERE mr.tenantId = :tenantId
        AND mr.priority = :priority
        AND mr.status = 'active'
        ORDER BY mr.authoredOn DESC
        """)
    List<MedicationRequest> findByTenantIdAndPriority(
        @Param("tenantId") String tenantId,
        @Param("priority") String priority
    );

    /**
     * Update medication status in bulk
     * Used for status transitions and workflow management
     *
     * @param patientId Patient identifier
     * @param oldStatus Current status
     * @param newStatus New status to apply
     * @param updatedBy User making the change
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE MedicationRequest mr
        SET mr.status = :newStatus, mr.updatedAt = CURRENT_TIMESTAMP
        WHERE mr.patientId = :patientId
        AND mr.status = :oldStatus
        """)
    void updateMedicationStatus(
        @Param("patientId") String patientId,
        @Param("oldStatus") String oldStatus,
        @Param("newStatus") String newStatus
    );
}