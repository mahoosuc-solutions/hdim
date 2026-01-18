package com.healthdata.patientevent.persistence;

import com.healthdata.patientevent.projection.PatientActiveProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Patient Projection Repository
 *
 * Persistence layer for PatientActiveProjection (read model)
 * Enables fast queries of patient denormalized state
 * Multi-tenant isolation via tenantId parameter
 */
@Repository
public interface PatientProjectionRepository extends JpaRepository<PatientActiveProjection, String> {

    /**
     * Find patient projection by ID and tenant
     * Multi-tenant isolation query
     */
    @Query("SELECT p FROM PatientActiveProjection p WHERE p.id = :patientId AND p.tenantId = :tenantId")
    Optional<PatientActiveProjection> findByIdAndTenant(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );

    /**
     * Find all active patients for tenant
     */
    @Query("SELECT p FROM PatientActiveProjection p WHERE p.tenantId = :tenantId AND p.status = 'ACTIVE' ORDER BY p.lastUpdated DESC")
    List<PatientActiveProjection> findActivePatientsByTenant(@Param("tenantId") String tenantId);

    /**
     * Count active patients by tenant
     */
    @Query("SELECT COUNT(p) FROM PatientActiveProjection p WHERE p.tenantId = :tenantId AND p.status = 'ACTIVE'")
    long countActivePatientsByTenant(@Param("tenantId") String tenantId);
}
