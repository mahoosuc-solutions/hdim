package com.healthdata.caregap.persistence;

import com.healthdata.caregap.projection.PopulationHealthProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Population Health Repository
 *
 * Persistence layer for PopulationHealthProjection (aggregated read model)
 * Enables fast queries of population health metrics
 * Multi-tenant isolation via tenantId parameter
 */
@Repository
public interface PopulationHealthRepository extends JpaRepository<PopulationHealthProjection, String> {

    /**
     * Find population health metrics by tenant
     * Multi-tenant isolation query
     */
    @Query("SELECT p FROM PopulationHealthHandlerProjection p WHERE p.tenantId = :tenantId")
    Optional<PopulationHealthProjection> findByTenantId(@Param("tenantId") String tenantId);
}
