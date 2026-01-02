package com.healthdata.agentbuilder.repository;

import com.healthdata.agentbuilder.domain.entity.AgentVersion;
import com.healthdata.agentbuilder.domain.entity.AgentVersion.VersionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentVersionRepository extends JpaRepository<AgentVersion, UUID> {

    Page<AgentVersion> findByAgentConfigurationId(UUID agentConfigurationId, Pageable pageable);

    /**
     * Get version history with eager-loaded agent configuration to avoid N+1 queries.
     * Uses JOIN FETCH for optimal performance.
     */
    @Query("SELECT v FROM AgentVersion v JOIN FETCH v.agentConfiguration " +
           "WHERE v.agentConfiguration.id = :agentId ORDER BY v.createdAt DESC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<AgentVersion> findByAgentConfigurationIdOrderByCreatedAtDesc(@Param("agentId") UUID agentConfigurationId);

    /**
     * Paginated version history with JOIN FETCH - use for large histories.
     */
    @Query(value = "SELECT v FROM AgentVersion v JOIN FETCH v.agentConfiguration " +
                   "WHERE v.agentConfiguration.id = :agentId",
           countQuery = "SELECT COUNT(v) FROM AgentVersion v WHERE v.agentConfiguration.id = :agentId")
    Page<AgentVersion> findByAgentConfigurationIdWithConfig(@Param("agentId") UUID agentConfigurationId, Pageable pageable);

    Optional<AgentVersion> findByAgentConfigurationIdAndVersionNumber(UUID agentConfigurationId, String versionNumber);

    Optional<AgentVersion> findByAgentConfigurationIdAndStatus(UUID agentConfigurationId, VersionStatus status);

    @Query("SELECT v FROM AgentVersion v WHERE v.agentConfiguration.id = :agentId ORDER BY v.createdAt DESC LIMIT 1")
    Optional<AgentVersion> findLatestVersion(@Param("agentId") UUID agentConfigurationId);

    @Query("SELECT v FROM AgentVersion v WHERE v.agentConfiguration.id = :agentId AND v.status = 'PUBLISHED' ORDER BY v.publishedAt DESC LIMIT 1")
    Optional<AgentVersion> findLatestPublishedVersion(@Param("agentId") UUID agentConfigurationId);

    /**
     * Lightweight projection query - returns only metadata, not full JSONB snapshot.
     * Use for list views where full configuration is not needed.
     */
    @Query("SELECT v.id, v.versionNumber, v.status, v.changeSummary, v.createdBy, v.createdAt " +
           "FROM AgentVersion v WHERE v.agentConfiguration.id = :agentId ORDER BY v.createdAt DESC")
    List<Object[]> findVersionMetadataByAgentId(@Param("agentId") UUID agentConfigurationId);

    long countByAgentConfigurationId(UUID agentConfigurationId);

    boolean existsByAgentConfigurationIdAndVersionNumber(UUID agentConfigurationId, String versionNumber);

    /**
     * Batch delete old versions for cleanup - uses efficient bulk operation.
     */
    @Query("DELETE FROM AgentVersion v WHERE v.agentConfiguration.id = :agentId " +
           "AND v.status = 'SUPERSEDED' AND v.createdAt < :cutoffDate")
    int deleteOldVersions(@Param("agentId") UUID agentConfigurationId, @Param("cutoffDate") java.time.Instant cutoffDate);
}
