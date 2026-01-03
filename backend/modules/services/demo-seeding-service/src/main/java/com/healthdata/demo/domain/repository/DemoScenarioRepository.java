package com.healthdata.demo.domain.repository;

import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.model.DemoScenario.ScenarioType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DemoScenario entity.
 */
@Repository
public interface DemoScenarioRepository extends JpaRepository<DemoScenario, UUID> {

    /**
     * Find scenario by unique name.
     */
    Optional<DemoScenario> findByName(String name);

    /**
     * Find all active scenarios.
     */
    List<DemoScenario> findByIsActiveTrueOrderByDisplayNameAsc();

    /**
     * Find scenarios by type.
     */
    List<DemoScenario> findByScenarioTypeAndIsActiveTrue(ScenarioType scenarioType);

    /**
     * Find scenarios by tenant.
     */
    List<DemoScenario> findByTenantIdAndIsActiveTrue(String tenantId);

    /**
     * Check if scenario name exists.
     */
    boolean existsByName(String name);

    /**
     * Find scenario with data snapshot.
     */
    @Query("SELECT s FROM DemoScenario s WHERE s.name = :name AND s.dataSnapshot IS NOT NULL")
    Optional<DemoScenario> findByNameWithSnapshot(@Param("name") String name);
}
