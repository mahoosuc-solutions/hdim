package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for FHIR DiagnosticReport resources.
 * Provides tenant-scoped queries for diagnostic reports.
 */
@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReportEntity, UUID> {

    /**
     * Find diagnostic report by tenant and ID
     */
    Optional<DiagnosticReportEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);

    /**
     * Find all diagnostic reports for a patient
     */
    List<DiagnosticReportEntity> findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(
            String tenantId, UUID patientId);

    /**
     * Find diagnostic reports by patient and status
     */
    List<DiagnosticReportEntity> findByTenantIdAndPatientIdAndStatusAndDeletedAtIsNull(
            String tenantId, UUID patientId, String status);

    /**
     * Find final diagnostic reports for a patient
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.status = 'final' " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.issuedDatetime DESC")
    List<DiagnosticReportEntity> findFinalReportsForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find diagnostic reports by encounter
     */
    List<DiagnosticReportEntity> findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByIssuedDatetimeDesc(
            String tenantId, UUID encounterId);

    /**
     * Find diagnostic reports by code (report type)
     */
    List<DiagnosticReportEntity> findByTenantIdAndPatientIdAndCodeAndDeletedAtIsNull(
            String tenantId, UUID patientId, String code);

    /**
     * Find diagnostic reports by category
     */
    List<DiagnosticReportEntity> findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(
            String tenantId, UUID patientId, String categoryCode);

    /**
     * Find lab reports for a patient
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND UPPER(d.categoryCode) = 'LAB' " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.issuedDatetime DESC")
    List<DiagnosticReportEntity> findLabReportsForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find imaging reports for a patient
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND UPPER(d.categoryCode) IN ('RAD', 'IMAGING') " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.issuedDatetime DESC")
    List<DiagnosticReportEntity> findImagingReportsForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Search diagnostic reports with multiple criteria
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.deletedAt IS NULL " +
           "AND (:patientId IS NULL OR d.patientId = :patientId) " +
           "AND (:encounterId IS NULL OR d.encounterId = :encounterId) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "AND (:code IS NULL OR d.code = :code) " +
           "AND (:categoryCode IS NULL OR d.categoryCode = :categoryCode) " +
           "ORDER BY d.issuedDatetime DESC")
    Page<DiagnosticReportEntity> searchReports(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("encounterId") UUID encounterId,
            @Param("status") String status,
            @Param("code") String code,
            @Param("categoryCode") String categoryCode,
            Pageable pageable);

    /**
     * Find reports by effective date range
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.effectiveDatetime BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.effectiveDatetime DESC")
    List<DiagnosticReportEntity> findByEffectiveDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find reports by issued date range
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.issuedDatetime BETWEEN :startDate AND :endDate " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.issuedDatetime DESC")
    List<DiagnosticReportEntity> findByIssuedDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find reports based on a service request
     */
    List<DiagnosticReportEntity> findByTenantIdAndBasedOnReferenceAndDeletedAtIsNull(
            String tenantId, String basedOnReference);

    /**
     * Count reports by category for a patient
     */
    @Query("SELECT d.categoryCode, d.categoryDisplay, COUNT(d) FROM DiagnosticReportEntity d " +
           "WHERE d.tenantId = :tenantId AND d.patientId = :patientId AND d.deletedAt IS NULL " +
           "GROUP BY d.categoryCode, d.categoryDisplay " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> countByCategory(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count reports by status for a patient
     */
    @Query("SELECT d.status, COUNT(d) FROM DiagnosticReportEntity d " +
           "WHERE d.tenantId = :tenantId AND d.patientId = :patientId AND d.deletedAt IS NULL " +
           "GROUP BY d.status")
    List<Object[]> countByStatus(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find latest report of a specific type for a patient
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.code = :code " +
           "AND d.status = 'final' " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.issuedDatetime DESC")
    List<DiagnosticReportEntity> findLatestByCode(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("code") String code,
            Pageable pageable);

    /**
     * Find reports with specific conclusion codes
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.conclusionCodes LIKE %:conclusionCode% " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.issuedDatetime DESC")
    List<DiagnosticReportEntity> findByConclusion(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("conclusionCode") String conclusionCode);

    /**
     * Find pending/preliminary reports (awaiting final results)
     */
    @Query("SELECT d FROM DiagnosticReportEntity d WHERE d.tenantId = :tenantId " +
           "AND d.patientId = :patientId " +
           "AND d.status IN ('registered', 'partial', 'preliminary') " +
           "AND d.deletedAt IS NULL " +
           "ORDER BY d.effectiveDatetime DESC")
    List<DiagnosticReportEntity> findPendingReports(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find all diagnostic reports for tenant (paginated)
     */
    Page<DiagnosticReportEntity> findByTenantIdAndDeletedAtIsNullOrderByLastModifiedAtDesc(
            String tenantId, Pageable pageable);
}
