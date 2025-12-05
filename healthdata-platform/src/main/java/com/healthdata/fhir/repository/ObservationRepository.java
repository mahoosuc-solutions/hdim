package com.healthdata.fhir.repository;

import com.healthdata.fhir.domain.Observation;
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
 * Repository for FHIR Observation entities
 * Provides comprehensive access to clinical observations including vital signs, lab results, etc.
 * Supports tenant isolation, pagination, and complex queries
 */
@Repository
public interface ObservationRepository extends JpaRepository<Observation, String> {

    /**
     * Find observations by patient ID
     *
     * @param patientId Patient identifier
     * @return List of observations for the patient
     */
    List<Observation> findByPatientId(String patientId);

    /**
     * Find observations by patient ID and code
     * Used for finding specific types of observations (e.g., blood pressure, glucose)
     *
     * @param patientId Patient identifier
     * @param code LOINC code
     * @return List of matching observations
     */
    List<Observation> findByPatientIdAndCode(String patientId, String code);

    /**
     * Find observations by patient ID within a date range
     * Useful for trending analysis and historical queries
     *
     * @param patientId Patient identifier
     * @param startDate Start of date range
     * @param endDate End of date range
     * @return List of observations within the date range
     */
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patientId = :patientId
        AND o.effectiveDate BETWEEN :startDate AND :endDate
        ORDER BY o.effectiveDate DESC
        """)
    List<Observation> findByPatientIdAndDateRange(
        @Param("patientId") String patientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Find the latest observation for a patient by code
     * Useful for getting the most recent value of a specific measurement
     *
     * @param patientId Patient identifier
     * @param code LOINC code
     * @return Optional containing the latest observation if exists
     */
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patientId = :patientId
        AND o.code = :code
        ORDER BY o.effectiveDate DESC
        LIMIT 1
        """)
    Optional<Observation> findLatestByPatientIdAndCode(
        @Param("patientId") String patientId,
        @Param("code") String code
    );

    /**
     * Find observations by patient ID and category with pagination
     * Categories include vital-signs, laboratory, imaging, etc.
     *
     * @param patientId Patient identifier
     * @param category Observation category
     * @param pageable Pagination information
     * @return Paginated observations ordered by effective date descending
     */
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patientId = :patientId
        AND o.category = :category
        ORDER BY o.effectiveDate DESC
        """)
    Page<Observation> findByPatientIdAndCategoryOrderByEffectiveDateDesc(
        @Param("patientId") String patientId,
        @Param("category") String category,
        Pageable pageable
    );

    /**
     * Find distinct observation codes for a patient
     *
     * @param patientId Patient identifier
     * @return List of unique LOINC codes for observations
     */
    @Query("SELECT DISTINCT o.code FROM Observation o WHERE o.patientId = :patientId")
    List<String> findDistinctCodesByPatientId(@Param("patientId") String patientId);

    /**
     * Find observations by tenant ID
     *
     * @param tenantId Tenant identifier
     * @return List of observations for the tenant
     */
    List<Observation> findByTenantId(String tenantId);

    /**
     * Find observations by patient ID and status
     * Status values include: final, preliminary, corrected, amended, cancelled, etc.
     *
     * @param patientId Patient identifier
     * @param status Observation status
     * @return List of observations with the specified status
     */
    List<Observation> findByPatientIdAndStatus(String patientId, String status);

    /**
     * Find abnormal observations for a patient
     * Used for identifying abnormal lab results or vital signs
     *
     * @param patientId Patient identifier
     * @param status Status of the observation
     * @param category Category of observation
     * @return List of observations with abnormal interpretation
     */
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patientId = :patientId
        AND o.status = :status
        AND o.category = :category
        ORDER BY o.effectiveDate DESC
        """)
    List<Observation> findAbnormalObservations(
        @Param("patientId") String patientId,
        @Param("status") String status,
        @Param("category") String category
    );

    /**
     * Find recent observations for a patient ordered by date
     *
     * @param patientId Patient identifier
     * @param pageable Pagination information
     * @return Paginated observations ordered by most recent first
     */
    @Query("""
        SELECT o FROM Observation o
        WHERE o.patientId = :patientId
        ORDER BY o.effectiveDate DESC
        """)
    Page<Observation> findRecentByPatientId(
        @Param("patientId") String patientId,
        Pageable pageable
    );

    /**
     * Find observations by code system for a tenant
     *
     * @param tenantId Tenant identifier
     * @param system Code system (e.g., http://loinc.org)
     * @return List of observations using the specified code system
     */
    @Query("""
        SELECT o FROM Observation o
        WHERE o.tenantId = :tenantId
        AND o.system = :system
        """)
    List<Observation> findByTenantIdAndSystem(
        @Param("tenantId") String tenantId,
        @Param("system") String system
    );

    /**
     * Count observations by patient and category
     *
     * @param patientId Patient identifier
     * @param category Observation category
     * @return Count of observations in the category
     */
    @Query("""
        SELECT COUNT(o) FROM Observation o
        WHERE o.patientId = :patientId
        AND o.category = :category
        """)
    long countByPatientIdAndCategory(
        @Param("patientId") String patientId,
        @Param("category") String category
    );
}