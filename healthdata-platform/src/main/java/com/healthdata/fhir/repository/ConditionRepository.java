package com.healthdata.fhir.repository;

import com.healthdata.fhir.domain.Condition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for FHIR Condition entities
 * Provides comprehensive access to patient conditions, diagnoses, and problems
 * Supports complex queries, pagination, and tenant isolation
 */
@Repository
public interface ConditionRepository extends JpaRepository<Condition, String> {

    /**
     * Find conditions by patient ID
     *
     * @param patientId Patient identifier
     * @return List of all conditions for the patient
     */
    List<Condition> findByPatientId(String patientId);

    /**
     * Find conditions by patient ID and clinical status
     * Status values include: active, recurrence, relapse, inactive, remission, resolved
     *
     * @param patientId Patient identifier
     * @param clinicalStatus Clinical status value
     * @return List of conditions with the specified status
     */
    List<Condition> findByPatientIdAndClinicalStatus(String patientId, String clinicalStatus);

    /**
     * Find active conditions for a patient
     * Only returns conditions with clinicalStatus = 'active'
     *
     * @param patientId Patient identifier
     * @return List of active conditions
     */
    @Query("""
        SELECT c FROM Condition c
        WHERE c.patientId = :patientId
        AND c.clinicalStatus = 'active'
        ORDER BY c.onsetDate DESC
        """)
    List<Condition> findActiveConditionsByPatientId(@Param("patientId") String patientId);

    /**
     * Find conditions by patient ID and code
     * Used for finding specific condition types (e.g., diabetes, hypertension)
     *
     * @param patientId Patient identifier
     * @param code ICD-10 or other medical code
     * @return List of conditions with the specified code
     */
    List<Condition> findByPatientIdAndCode(String patientId, String code);

    /**
     * Find conditions by patient ID and onset date range
     *
     * @param patientId Patient identifier
     * @param startDate Start of onset date range
     * @param endDate End of onset date range
     * @return List of conditions with onset dates in the range
     */
    @Query("""
        SELECT c FROM Condition c
        WHERE c.patientId = :patientId
        AND c.onsetDate BETWEEN :startDate AND :endDate
        ORDER BY c.onsetDate DESC
        """)
    List<Condition> findByPatientIdAndOnsetDateBetween(
        @Param("patientId") String patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find conditions by patient ID and category
     * Categories include: problem-list-item, encounter-diagnosis
     *
     * @param patientId Patient identifier
     * @param category Condition category
     * @return List of conditions with the specified category
     */
    List<Condition> findByPatientIdAndCategory(String patientId, String category);

    /**
     * Find conditions by patient ID and severity
     * Severity values include: mild, moderate, severe
     *
     * @param patientId Patient identifier
     * @param severity Condition severity
     * @return List of conditions with the specified severity
     */
    List<Condition> findByPatientIdAndSeverity(String patientId, String severity);

    /**
     * Find distinct condition codes for a patient
     *
     * @param patientId Patient identifier
     * @return List of unique condition codes
     */
    @Query("SELECT DISTINCT c.code FROM Condition c WHERE c.patientId = :patientId")
    List<String> findDistinctCodesByPatientId(@Param("patientId") String patientId);

    /**
     * Find conditions by tenant ID
     *
     * @param tenantId Tenant identifier
     * @return List of conditions for the tenant
     */
    List<Condition> findByTenantId(String tenantId);

    /**
     * Count active conditions for a patient
     *
     * @param patientId Patient identifier
     * @return Count of active conditions
     */
    @Query("""
        SELECT COUNT(c) FROM Condition c
        WHERE c.patientId = :patientId
        AND c.clinicalStatus = 'active'
        """)
    Long countActiveConditionsByPatientId(@Param("patientId") String patientId);

    /**
     * Count active conditions by tenant
     * Provides metrics for population health analysis
     *
     * @param tenantId Tenant identifier
     * @return Total count of active conditions
     */
    @Query("""
        SELECT COUNT(c) FROM Condition c
        WHERE c.tenantId = :tenantId
        AND c.clinicalStatus = 'active'
        """)
    long countActiveConditionsByTenant(@Param("tenantId") String tenantId);

    /**
     * Find severe conditions for a patient
     * Used for risk assessment and clinical alerts
     *
     * @param patientId Patient identifier
     * @return List of severe conditions
     */
    @Query("""
        SELECT c FROM Condition c
        WHERE c.patientId = :patientId
        AND c.severity = 'severe'
        AND c.clinicalStatus = 'active'
        ORDER BY c.recordedDate DESC
        """)
    List<Condition> findSevereActiveConditions(@Param("patientId") String patientId);

    /**
     * Find recent conditions for a patient with pagination
     *
     * @param patientId Patient identifier
     * @param pageable Pagination information
     * @return Paginated list of recent conditions
     */
    @Query("""
        SELECT c FROM Condition c
        WHERE c.patientId = :patientId
        ORDER BY c.recordedDate DESC
        """)
    Page<Condition> findRecentByPatientId(
        @Param("patientId") String patientId,
        Pageable pageable
    );

    /**
     * Find conditions by code for a tenant
     *
     * @param tenantId Tenant identifier
     * @param code Condition code
     * @return List of conditions with the specified code
     */
    @Query("""
        SELECT c FROM Condition c
        WHERE c.tenantId = :tenantId
        AND c.code = :code
        """)
    List<Condition> findByTenantIdAndCode(
        @Param("tenantId") String tenantId,
        @Param("code") String code
    );

    /**
     * Count conditions by category for a patient
     *
     * @param patientId Patient identifier
     * @param category Condition category
     * @return Count of conditions in the category
     */
    @Query("""
        SELECT COUNT(c) FROM Condition c
        WHERE c.patientId = :patientId
        AND c.category = :category
        """)
    long countByPatientIdAndCategory(
        @Param("patientId") String patientId,
        @Param("category") String category
    );

    /**
     * Find conditions resolved between dates
     *
     * @param patientId Patient identifier
     * @param startDate Start of resolution date range
     * @param endDate End of resolution date range
     * @return List of resolved conditions
     */
    @Query("""
        SELECT c FROM Condition c
        WHERE c.patientId = :patientId
        AND c.abatementDate BETWEEN :startDate AND :endDate
        ORDER BY c.abatementDate DESC
        """)
    List<Condition> findResolvedConditionsBetweenDates(
        @Param("patientId") String patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}