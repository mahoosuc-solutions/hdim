package com.healthdata.sales.repository;

import com.healthdata.sales.entity.LinkedInCampaign;
import com.healthdata.sales.entity.LinkedInCampaign.CampaignStatus;
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
public interface LinkedInCampaignRepository extends JpaRepository<LinkedInCampaign, UUID> {

    // Basic queries
    Page<LinkedInCampaign> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<LinkedInCampaign> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<LinkedInCampaign> findByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    // Find by status
    Page<LinkedInCampaign> findByTenantIdAndStatus(UUID tenantId, CampaignStatus status, Pageable pageable);

    List<LinkedInCampaign> findByTenantIdAndStatus(UUID tenantId, CampaignStatus status);

    // Count campaigns
    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, CampaignStatus status);

    // Search by name
    @Query("SELECT c FROM LinkedInCampaign c WHERE c.tenantId = :tenantId " +
           "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<LinkedInCampaign> searchByName(@Param("tenantId") UUID tenantId,
                                        @Param("search") String search,
                                        Pageable pageable);

    // Analytics aggregation
    @Query("SELECT SUM(c.totalSent), SUM(c.totalAccepted), SUM(c.totalReplied) " +
           "FROM LinkedInCampaign c WHERE c.tenantId = :tenantId")
    Object[] getAggregatedMetrics(@Param("tenantId") UUID tenantId);
}
