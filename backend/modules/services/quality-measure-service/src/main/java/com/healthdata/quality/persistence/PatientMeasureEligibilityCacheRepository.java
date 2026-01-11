package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Patient Measure Eligibility Cache Repository
 * Performance cache for expensive eligibility computations.
 */
@Repository
public interface PatientMeasureEligibilityCacheRepository extends JpaRepository<PatientMeasureEligibilityCacheEntity, UUID> {

    // Tenant-isolated queries
    Optional<PatientMeasureEligibilityCacheEntity> findByIdAndTenantId(UUID id, String tenantId);

    // Cache lookup (primary use case)
    @Query("SELECT c FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.measureId = :measureId AND c.invalidated = false")
    Optional<PatientMeasureEligibilityCacheEntity> findValidCache(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId
    );

    // Valid and not expired
    @Query("SELECT c FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.measureId = :measureId " +
           "AND c.invalidated = false AND c.validUntil > :now")
    Optional<PatientMeasureEligibilityCacheEntity> findValidAndNotExpired(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId,
        @Param("now") OffsetDateTime now
    );

    // By patient
    @Query("SELECT c FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.invalidated = false")
    List<PatientMeasureEligibilityCacheEntity> findByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    // By measure
    @Query("SELECT c FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.measureId = :measureId AND c.invalidated = false")
    List<PatientMeasureEligibilityCacheEntity> findByMeasure(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );

    // Eligible patients for a measure
    @Query("SELECT c FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.measureId = :measureId AND c.isEligible = true AND c.invalidated = false")
    List<PatientMeasureEligibilityCacheEntity> findEligiblePatients(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );

    // Expired cache entries
    @Query("SELECT c FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.invalidated = false AND c.validUntil < :now")
    List<PatientMeasureEligibilityCacheEntity> findExpiredEntries(
        @Param("tenantId") String tenantId,
        @Param("now") OffsetDateTime now
    );

    // Cache invalidation
    @Modifying
    @Query("UPDATE PatientMeasureEligibilityCacheEntity c SET c.invalidated = true " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId")
    int invalidateByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    @Modifying
    @Query("UPDATE PatientMeasureEligibilityCacheEntity c SET c.invalidated = true " +
           "WHERE c.tenantId = :tenantId AND c.measureId = :measureId")
    int invalidateByMeasure(
        @Param("tenantId") String tenantId,
        @Param("measureId") UUID measureId
    );

    @Modifying
    @Query("UPDATE PatientMeasureEligibilityCacheEntity c SET c.invalidated = true " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.measureId = :measureId")
    int invalidateByPatientAndMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId
    );

    // Count cache entries
    @Query("SELECT COUNT(c) FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId AND c.invalidated = false")
    long countValidEntries(
        @Param("tenantId") String tenantId
    );

    // Cache hit rate analytics
    @Query("SELECT COUNT(c) FROM PatientMeasureEligibilityCacheEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId AND c.measureId = :measureId")
    long countCacheLookups(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") UUID measureId
    );
}
