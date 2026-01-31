package com.healthdata.caregap.persistence;

import com.healthdata.caregap.projection.CareGapProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Care Gap Projection Repository
 *
 * Persistence layer for CareGapProjection (read model)
 * Enables fast queries of care gap data
 * Multi-tenant isolation via tenantId parameter
 */
@Repository
public interface CareGapProjectionRepository extends JpaRepository<CareGapProjection, String> {

    /**
     * Find care gap by patient ID and tenant
     * Multi-tenant isolation query
     */
    @Query("SELECT c FROM CareGapHandlerProjection c WHERE c.patientId = :patientId AND c.tenantId = :tenantId")
    Optional<CareGapProjection> findByPatientIdAndTenant(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all open gaps for a tenant
     */
    @Query("SELECT c FROM CareGapHandlerProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN'")
    List<CareGapProjection> findOpenGapsByTenant(@Param("tenantId") String tenantId);

    /**
     * Find all gaps by gap code and tenant
     */
    @Query("SELECT c FROM CareGapHandlerProjection c WHERE c.gapCode = :gapCode AND c.tenantId = :tenantId ORDER BY c.lastUpdated DESC")
    List<CareGapProjection> findByGapCodeAndTenant(
        @Param("gapCode") String gapCode,
        @Param("tenantId") String tenantId
    );

    /**
     * Count open gaps by severity for tenant
     */
    @Query("SELECT COUNT(c) FROM CareGapHandlerProjection c WHERE c.tenantId = :tenantId AND c.status = 'OPEN' AND c.severity = :severity")
    long countOpenGapsBySeverity(
        @Param("tenantId") String tenantId,
        @Param("severity") String severity
    );
}
