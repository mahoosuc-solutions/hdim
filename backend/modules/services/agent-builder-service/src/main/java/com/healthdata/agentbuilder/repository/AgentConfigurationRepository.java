package com.healthdata.agentbuilder.repository;

import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentConfigurationRepository extends JpaRepository<AgentConfiguration, UUID> {

    Page<AgentConfiguration> findByTenantId(String tenantId, Pageable pageable);

    Page<AgentConfiguration> findByTenantIdAndStatus(String tenantId, AgentStatus status, Pageable pageable);

    Optional<AgentConfiguration> findByTenantIdAndId(String tenantId, UUID id);

    Optional<AgentConfiguration> findByTenantIdAndSlug(String tenantId, String slug);

    boolean existsByTenantIdAndSlug(String tenantId, String slug);

    boolean existsByTenantIdAndName(String tenantId, String name);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, AgentStatus status);

    @Query("SELECT a FROM AgentConfiguration a WHERE a.tenantId = :tenantId AND a.status = 'ACTIVE' ORDER BY a.name")
    List<AgentConfiguration> findActiveAgents(@Param("tenantId") String tenantId);

    @Query(value = "SELECT * FROM agent_configurations a WHERE a.tenant_id = :tenantId AND :tag = ANY(a.tags)",
           countQuery = "SELECT COUNT(*) FROM agent_configurations a WHERE a.tenant_id = :tenantId AND :tag = ANY(a.tags)",
           nativeQuery = true)
    Page<AgentConfiguration> findByTenantIdAndTag(@Param("tenantId") String tenantId, @Param("tag") String tag, Pageable pageable);

    @Query("SELECT a FROM AgentConfiguration a WHERE a.tenantId = :tenantId AND " +
           "(LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AgentConfiguration> searchByTenantId(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);
}
