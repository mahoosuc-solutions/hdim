package com.healthdata.quality.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Quality Measure Result Repository
 */
@Repository
public interface QualityMeasureResultRepository extends JpaRepository<QualityMeasureResultEntity, UUID> {

    Optional<QualityMeasureResultEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<QualityMeasureResultEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    List<QualityMeasureResultEntity> findByTenantId(String tenantId);

    @Query("SELECT q FROM QualityMeasureResultEntity q WHERE q.tenantId = :tenantId AND q.patientId = :patientId AND q.measureId = :measureId ORDER BY q.calculationDate DESC")
    List<QualityMeasureResultEntity> findByPatientAndMeasure(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("measureId") String measureId
    );

    @Query("SELECT q FROM QualityMeasureResultEntity q WHERE q.tenantId = :tenantId AND q.measureYear = :year ORDER BY q.patientId")
    List<QualityMeasureResultEntity> findByMeasureYear(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year
    );

    @Query("SELECT COUNT(q) FROM QualityMeasureResultEntity q WHERE q.tenantId = :tenantId AND q.patientId = :patientId AND q.numeratorCompliant = true")
    long countCompliantMeasures(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    @Query("SELECT q FROM QualityMeasureResultEntity q WHERE q.tenantId = :tenantId ORDER BY q.calculationDate DESC")
    List<QualityMeasureResultEntity> findByTenantIdWithPagination(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );

    /**
     * Find all results by IDs for a specific tenant (for bulk operations)
     */
    @Query("SELECT q FROM QualityMeasureResultEntity q WHERE q.id IN :ids AND q.tenantId = :tenantId")
    List<QualityMeasureResultEntity> findAllByIdInAndTenantId(
        @Param("ids") List<UUID> ids,
        @Param("tenantId") String tenantId
    );
}
