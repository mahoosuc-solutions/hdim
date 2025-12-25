package com.healthdata.sales.repository;

import com.healthdata.sales.entity.Lead;
import com.healthdata.sales.entity.LeadSource;
import com.healthdata.sales.entity.LeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID> {

    Page<Lead> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Lead> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Lead> findByEmailAndTenantId(String email, UUID tenantId);

    Optional<Lead> findByZohoLeadId(String zohoLeadId);

    Page<Lead> findByTenantIdAndStatus(UUID tenantId, LeadStatus status, Pageable pageable);

    Page<Lead> findByTenantIdAndSource(UUID tenantId, LeadSource source, Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.assignedToUserId = :userId")
    Page<Lead> findByTenantIdAndAssignedTo(@Param("tenantId") UUID tenantId,
                                            @Param("userId") UUID userId,
                                            Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.score >= :minScore")
    Page<Lead> findByTenantIdAndMinScore(@Param("tenantId") UUID tenantId,
                                          @Param("minScore") Integer minScore,
                                          Pageable pageable);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId " +
           "AND l.status = :status " +
           "AND l.createdAt >= :since")
    List<Lead> findRecentLeadsByStatus(@Param("tenantId") UUID tenantId,
                                        @Param("status") LeadStatus status,
                                        @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.tenantId = :tenantId AND l.status = :status")
    Long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId,
                                   @Param("status") LeadStatus status);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.zohoLeadId IS NOT NULL")
    List<Lead> findSyncedLeads(@Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM Lead l WHERE l.tenantId = :tenantId AND l.zohoLeadId IS NULL")
    List<Lead> findUnsyncedLeads(@Param("tenantId") UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
