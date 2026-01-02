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
 * Repository for FHIR Coverage resources.
 * Provides tenant-scoped queries for insurance coverage data.
 */
@Repository
public interface CoverageRepository extends JpaRepository<CoverageEntity, UUID> {

    /**
     * Find coverage by tenant and ID
     */
    Optional<CoverageEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);

    /**
     * Find all coverages for a patient
     */
    List<CoverageEntity> findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByPeriodStartDesc(
            String tenantId, UUID patientId);

    /**
     * Find coverages by patient and status
     */
    List<CoverageEntity> findByTenantIdAndPatientIdAndStatusAndDeletedAtIsNull(
            String tenantId, UUID patientId, String status);

    /**
     * Find active coverages for a patient
     */
    @Query("SELECT c FROM CoverageEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.deletedAt IS NULL " +
           "AND (c.periodStart IS NULL OR c.periodStart <= :asOf) " +
           "AND (c.periodEnd IS NULL OR c.periodEnd >= :asOf) " +
           "ORDER BY c.coverageOrder ASC NULLS LAST")
    List<CoverageEntity> findActiveCoveragesForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("asOf") Instant asOf);

    /**
     * Find coverage by subscriber ID
     */
    List<CoverageEntity> findByTenantIdAndSubscriberIdAndDeletedAtIsNull(
            String tenantId, String subscriberId);

    /**
     * Find coverages by payor
     */
    List<CoverageEntity> findByTenantIdAndPayorReferenceAndDeletedAtIsNull(
            String tenantId, String payorReference);

    /**
     * Find coverages by type
     */
    List<CoverageEntity> findByTenantIdAndTypeCodeAndDeletedAtIsNull(
            String tenantId, String typeCode);

    /**
     * Search coverages with multiple criteria
     */
    @Query("SELECT c FROM CoverageEntity c WHERE c.tenantId = :tenantId " +
           "AND c.deletedAt IS NULL " +
           "AND (:patientId IS NULL OR c.patientId = :patientId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:typeCode IS NULL OR c.typeCode = :typeCode) " +
           "AND (:subscriberId IS NULL OR c.subscriberId = :subscriberId) " +
           "AND (:payorReference IS NULL OR c.payorReference = :payorReference) " +
           "ORDER BY c.lastModifiedAt DESC")
    Page<CoverageEntity> searchCoverages(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("status") String status,
            @Param("typeCode") String typeCode,
            @Param("subscriberId") String subscriberId,
            @Param("payorReference") String payorReference,
            Pageable pageable);

    /**
     * Find coverages expiring within a date range (for notifications)
     */
    @Query("SELECT c FROM CoverageEntity c WHERE c.tenantId = :tenantId " +
           "AND c.status = 'active' " +
           "AND c.deletedAt IS NULL " +
           "AND c.periodEnd IS NOT NULL " +
           "AND c.periodEnd BETWEEN :startDate AND :endDate " +
           "ORDER BY c.periodEnd ASC")
    List<CoverageEntity> findExpiringCoverages(
            @Param("tenantId") String tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Count coverages by status for a tenant
     */
    @Query("SELECT c.status, COUNT(c) FROM CoverageEntity c " +
           "WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL " +
           "GROUP BY c.status")
    List<Object[]> countByStatus(@Param("tenantId") String tenantId);

    /**
     * Count coverages by payor for analytics
     */
    @Query("SELECT c.payorReference, c.payorDisplay, COUNT(c) FROM CoverageEntity c " +
           "WHERE c.tenantId = :tenantId AND c.deletedAt IS NULL AND c.status = 'active' " +
           "GROUP BY c.payorReference, c.payorDisplay " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> countByPayor(@Param("tenantId") String tenantId);

    /**
     * Find primary coverage for a patient (order = 1)
     */
    @Query("SELECT c FROM CoverageEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.deletedAt IS NULL " +
           "AND (c.coverageOrder = 1 OR c.coverageOrder IS NULL) " +
           "AND (c.periodStart IS NULL OR c.periodStart <= :asOf) " +
           "AND (c.periodEnd IS NULL OR c.periodEnd >= :asOf) " +
           "ORDER BY c.coverageOrder ASC NULLS LAST")
    Optional<CoverageEntity> findPrimaryCoverage(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("asOf") Instant asOf);

    /**
     * Check if patient has any active coverage
     */
    @Query("SELECT COUNT(c) > 0 FROM CoverageEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.deletedAt IS NULL " +
           "AND (c.periodStart IS NULL OR c.periodStart <= :asOf) " +
           "AND (c.periodEnd IS NULL OR c.periodEnd >= :asOf)")
    boolean hasActiveCoverage(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("asOf") Instant asOf);

    /**
     * Find all coverages for tenant (paginated)
     */
    Page<CoverageEntity> findByTenantIdAndDeletedAtIsNullOrderByLastModifiedAtDesc(
            String tenantId, Pageable pageable);
}
