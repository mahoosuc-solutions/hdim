package com.healthdata.fhir.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProcedureRepository extends JpaRepository<ProcedureEntity, UUID> {

    /**
     * Find procedures for a specific patient
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdOrderByPerformedDateDesc(
            String tenantId, UUID patientId);

    /**
     * Find procedures for a patient with pagination
     */
    Page<ProcedureEntity> findByTenantIdAndPatientIdOrderByPerformedDateDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find procedure by tenant and ID
     */
    Optional<ProcedureEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find procedures by patient and procedure code
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdAndProcedureCodeOrderByPerformedDateDesc(
            String tenantId, UUID patientId, String procedureCode);

    /**
     * Find procedures by patient and status
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdAndStatusOrderByPerformedDateDesc(
            String tenantId, UUID patientId, String status);

    /**
     * Find completed procedures for a patient
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId AND p.status = 'completed' " +
           "ORDER BY p.performedDate DESC")
    List<ProcedureEntity> findCompletedProceduresByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find procedures by patient within a date range
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND p.performedDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.performedDate DESC")
    List<ProcedureEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find surgical procedures for a patient
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND LOWER(p.categoryCode) LIKE '%surgical%' " +
           "ORDER BY p.performedDate DESC")
    List<ProcedureEntity> findSurgicalProceduresByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find diagnostic procedures for a patient
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND LOWER(p.categoryCode) LIKE '%diagnostic%' " +
           "ORDER BY p.performedDate DESC")
    List<ProcedureEntity> findDiagnosticProceduresByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find procedures by patient and category
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdAndCategoryCodeOrderByPerformedDateDesc(
            String tenantId, UUID patientId, String categoryCode);

    /**
     * Find procedures by performer
     */
    List<ProcedureEntity> findByTenantIdAndPerformerIdOrderByPerformedDateDesc(
            String tenantId, String performerId);

    /**
     * Find procedures by location
     */
    List<ProcedureEntity> findByTenantIdAndLocationIdOrderByPerformedDateDesc(
            String tenantId, String locationId);

    /**
     * Find procedures by encounter
     */
    List<ProcedureEntity> findByTenantIdAndEncounterIdOrderByPerformedDateDesc(
            String tenantId, UUID encounterId);

    /**
     * Find procedures by reason code
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdAndReasonCodeOrderByPerformedDateDesc(
            String tenantId, UUID patientId, String reasonCode);

    /**
     * Find procedures by body site
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdAndBodySiteCodeOrderByPerformedDateDesc(
            String tenantId, UUID patientId, String bodySiteCode);

    /**
     * Find procedures with complications
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND p.complicationCode IS NOT NULL " +
           "ORDER BY p.performedDate DESC")
    List<ProcedureEntity> findProceduresWithComplications(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find procedures by outcome
     */
    List<ProcedureEntity> findByTenantIdAndPatientIdAndOutcomeCodeOrderByPerformedDateDesc(
            String tenantId, UUID patientId, String outcomeCode);

    /**
     * Count procedures for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count procedures by status for a patient
     */
    long countByTenantIdAndPatientIdAndStatus(
            String tenantId, UUID patientId, String status);

    /**
     * Count procedures by code for a patient
     */
    long countByTenantIdAndPatientIdAndProcedureCode(
            String tenantId, UUID patientId, String procedureCode);

    /**
     * Check if patient has procedure in date range
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND p.performedDate BETWEEN :startDate AND :endDate")
    boolean hasProcedureInDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Check if patient has specific procedure
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND p.procedureCode = :procedureCode " +
           "AND p.status = 'completed'")
    boolean hasCompletedProcedure(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("procedureCode") String procedureCode);

    /**
     * Find most recent procedure for patient
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "ORDER BY p.performedDate DESC LIMIT 1")
    Optional<ProcedureEntity> findMostRecentProcedureByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find procedures by procedure system (CPT, SNOMED CT, ICD-10-PCS)
     */
    List<ProcedureEntity> findByTenantIdAndProcedureSystemOrderByPerformedDateDesc(
            String tenantId, String procedureSystem);

    /**
     * Find recent procedures (within last N days)
     */
    @Query("SELECT p FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND p.performedDate >= :sinceDate " +
           "ORDER BY p.performedDate DESC")
    List<ProcedureEntity> findRecentProcedures(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("sinceDate") LocalDate sinceDate);

    /**
     * Count surgical procedures in date range (for quality measures)
     */
    @Query("SELECT COUNT(p) FROM ProcedureEntity p WHERE p.tenantId = :tenantId " +
           "AND p.patientId = :patientId " +
           "AND LOWER(p.categoryCode) LIKE '%surgical%' " +
           "AND p.performedDate BETWEEN :startDate AND :endDate " +
           "AND p.status = 'completed'")
    long countSurgicalProceduresInDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
