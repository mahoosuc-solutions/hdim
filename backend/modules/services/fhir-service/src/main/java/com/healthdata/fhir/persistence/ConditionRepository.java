package com.healthdata.fhir.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConditionRepository extends JpaRepository<ConditionEntity, UUID> {

    /**
     * Find conditions for a specific patient
     */
    List<ConditionEntity> findByTenantIdAndPatientIdOrderByRecordedDateDesc(
            String tenantId, UUID patientId);

    /**
     * Find conditions for a patient with pagination
     */
    Page<ConditionEntity> findByTenantIdAndPatientIdOrderByRecordedDateDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find condition by tenant and ID
     */
    Optional<ConditionEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find conditions by patient and code
     */
    List<ConditionEntity> findByTenantIdAndPatientIdAndCodeOrderByRecordedDateDesc(
            String tenantId, UUID patientId, String code);

    /**
     * Find conditions by patient and category
     */
    List<ConditionEntity> findByTenantIdAndPatientIdAndCategoryOrderByRecordedDateDesc(
            String tenantId, UUID patientId, String category);

    /**
     * Find conditions by patient and clinical status
     */
    List<ConditionEntity> findByTenantIdAndPatientIdAndClinicalStatusOrderByRecordedDateDesc(
            String tenantId, UUID patientId, String clinicalStatus);

    /**
     * Find active conditions for a patient (clinical_status = active)
     */
    @Query("SELECT c FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.clinicalStatus = 'active' " +
           "ORDER BY c.recordedDate DESC")
    List<ConditionEntity> findActiveConditionsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find conditions by patient within a date range
     */
    @Query("SELECT c FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.recordedDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.recordedDate DESC")
    List<ConditionEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find conditions by code
     */
    List<ConditionEntity> findByTenantIdAndCodeOrderByRecordedDateDesc(
            String tenantId, String code);

    /**
     * Find conditions by category
     */
    List<ConditionEntity> findByTenantIdAndCategoryOrderByRecordedDateDesc(
            String tenantId, String category);

    /**
     * Find conditions by clinical status
     */
    List<ConditionEntity> findByTenantIdAndClinicalStatusOrderByRecordedDateDesc(
            String tenantId, String clinicalStatus);

    /**
     * Count conditions for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count active conditions for a patient
     */
    @Query("SELECT COUNT(c) FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.clinicalStatus = 'active'")
    long countActiveConditionsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count conditions by code
     */
    long countByTenantIdAndCode(String tenantId, String code);

    /**
     * Search conditions by code or display text (case-insensitive contains)
     */
    @Query("SELECT c FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND (LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(c.codeDisplay) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.recordedDate DESC")
    List<ConditionEntity> searchByCodeOrDisplay(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find chronic conditions (conditions with onset but no abatement)
     */
    @Query("SELECT c FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.onsetDate IS NOT NULL AND c.abatementDate IS NULL " +
           "AND c.clinicalStatus = 'active' " +
           "ORDER BY c.onsetDate DESC")
    List<ConditionEntity> findChronicConditionsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find diagnosis conditions (category = encounter-diagnosis)
     */
    @Query("SELECT c FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.category = 'encounter-diagnosis' " +
           "ORDER BY c.recordedDate DESC")
    List<ConditionEntity> findDiagnosesByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find problem list items (category = problem-list-item)
     */
    @Query("SELECT c FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.category = 'problem-list-item' " +
           "ORDER BY c.recordedDate DESC")
    List<ConditionEntity> findProblemListByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Check if patient has a specific condition (by code)
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ConditionEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.code = :code " +
           "AND c.clinicalStatus = 'active'")
    boolean hasActiveCondition(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("code") String code);
}
