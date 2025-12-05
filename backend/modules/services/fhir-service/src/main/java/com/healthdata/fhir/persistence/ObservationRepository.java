package com.healthdata.fhir.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ObservationRepository extends JpaRepository<ObservationEntity, UUID> {

    /**
     * Find observations for a specific patient
     */
    List<ObservationEntity> findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId);

    /**
     * Find observations for a patient with pagination
     */
    Page<ObservationEntity> findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find observation by tenant and ID
     */
    Optional<ObservationEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find observations by patient and code
     */
    List<ObservationEntity> findByTenantIdAndPatientIdAndCodeOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, String code);

    /**
     * Find observations by patient and category
     */
    List<ObservationEntity> findByTenantIdAndPatientIdAndCategoryOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, String category);

    /**
     * Find observations by patient and code system
     */
    List<ObservationEntity> findByTenantIdAndPatientIdAndCodeSystemOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, String codeSystem);

    /**
     * Find observations by patient within a date range
     */
    @Query("SELECT o FROM ObservationEntity o WHERE o.tenantId = :tenantId " +
           "AND o.patientId = :patientId " +
           "AND o.effectiveDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY o.effectiveDateTime DESC")
    List<ObservationEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find observations by code
     */
    List<ObservationEntity> findByTenantIdAndCodeOrderByEffectiveDateTimeDesc(
            String tenantId, String code);

    /**
     * Find observations by category
     */
    List<ObservationEntity> findByTenantIdAndCategoryOrderByEffectiveDateTimeDesc(
            String tenantId, String category);

    /**
     * Find observations by status
     */
    List<ObservationEntity> findByTenantIdAndStatusOrderByEffectiveDateTimeDesc(
            String tenantId, String status);

    /**
     * Count observations for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count observations by code
     */
    long countByTenantIdAndCode(String tenantId, String code);

    /**
     * Count observations by category
     */
    long countByTenantIdAndCategory(String tenantId, String category);

    /**
     * Search observations by code (case-insensitive contains)
     */
    @Query("SELECT o FROM ObservationEntity o WHERE o.tenantId = :tenantId " +
           "AND LOWER(o.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY o.effectiveDateTime DESC")
    List<ObservationEntity> searchByCode(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find latest observations for a patient by code
     */
    @Query("SELECT o FROM ObservationEntity o WHERE o.tenantId = :tenantId " +
           "AND o.patientId = :patientId AND o.code = :code " +
           "ORDER BY o.effectiveDateTime DESC LIMIT 1")
    Optional<ObservationEntity> findLatestByPatientAndCode(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("code") String code);

    /**
     * Find lab results (category = laboratory)
     */
    @Query("SELECT o FROM ObservationEntity o WHERE o.tenantId = :tenantId " +
           "AND o.patientId = :patientId AND o.category = 'laboratory' " +
           "ORDER BY o.effectiveDateTime DESC")
    List<ObservationEntity> findLabResultsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find vital signs (category = vital-signs)
     */
    @Query("SELECT o FROM ObservationEntity o WHERE o.tenantId = :tenantId " +
           "AND o.patientId = :patientId AND o.category = 'vital-signs' " +
           "ORDER BY o.effectiveDateTime DESC")
    List<ObservationEntity> findVitalSignsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);
}
