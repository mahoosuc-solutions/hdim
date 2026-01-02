package com.healthdata.sales.repository;

import com.healthdata.sales.entity.LinkedInOutreach;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachStatus;
import com.healthdata.sales.entity.LinkedInOutreach.OutreachType;
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
public interface LinkedInOutreachRepository extends JpaRepository<LinkedInOutreach, UUID> {

    // Basic queries
    Page<LinkedInOutreach> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<LinkedInOutreach> findByIdAndTenantId(UUID id, UUID tenantId);

    List<LinkedInOutreach> findByTenantIdAndLeadId(UUID tenantId, UUID leadId);

    List<LinkedInOutreach> findByTenantIdAndContactId(UUID tenantId, UUID contactId);

    // Find pending outreach ready to send
    @Query("SELECT o FROM LinkedInOutreach o WHERE o.status = 'PENDING' " +
           "AND o.scheduledAt <= :now ORDER BY o.scheduledAt ASC")
    List<LinkedInOutreach> findDueForSending(@Param("now") LocalDateTime now);

    // Find by campaign
    Page<LinkedInOutreach> findByTenantIdAndCampaignName(UUID tenantId, String campaignName, Pageable pageable);

    // Find by status
    Page<LinkedInOutreach> findByTenantIdAndStatus(UUID tenantId, OutreachStatus status, Pageable pageable);

    // Find by type
    Page<LinkedInOutreach> findByTenantIdAndOutreachType(UUID tenantId, OutreachType type, Pageable pageable);

    // Check if already contacted on LinkedIn
    @Query("SELECT COUNT(o) > 0 FROM LinkedInOutreach o WHERE o.tenantId = :tenantId " +
           "AND o.linkedinProfileUrl = :profileUrl AND o.outreachType = :type " +
           "AND o.status NOT IN ('FAILED', 'CANCELLED')")
    boolean existsByProfileAndType(@Param("tenantId") UUID tenantId,
                                   @Param("profileUrl") String profileUrl,
                                   @Param("type") OutreachType type);

    // Count by status for dashboard
    @Query("SELECT o.status, COUNT(o) FROM LinkedInOutreach o " +
           "WHERE o.tenantId = :tenantId GROUP BY o.status")
    List<Object[]> countByStatus(@Param("tenantId") UUID tenantId);

    // Count daily outreach for rate limiting
    @Query("SELECT COUNT(o) FROM LinkedInOutreach o WHERE o.tenantId = :tenantId " +
           "AND o.outreachType = :type AND o.sentAt >= :startOfDay AND o.sentAt < :endOfDay")
    long countDailyOutreach(@Param("tenantId") UUID tenantId,
                            @Param("type") OutreachType type,
                            @Param("startOfDay") LocalDateTime startOfDay,
                            @Param("endOfDay") LocalDateTime endOfDay);

    // Analytics
    @Query("SELECT COUNT(o) FROM LinkedInOutreach o WHERE o.tenantId = :tenantId " +
           "AND o.outreachType = :type AND o.sentAt >= :since")
    long countSentSince(@Param("tenantId") UUID tenantId,
                        @Param("type") OutreachType type,
                        @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM LinkedInOutreach o WHERE o.tenantId = :tenantId " +
           "AND o.connectionAccepted = true AND o.acceptedAt >= :since")
    long countAcceptedSince(@Param("tenantId") UUID tenantId,
                            @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM LinkedInOutreach o WHERE o.tenantId = :tenantId " +
           "AND o.replied = true AND o.repliedAt >= :since")
    long countRepliedSince(@Param("tenantId") UUID tenantId,
                           @Param("since") LocalDateTime since);

    // Find by sequence
    List<LinkedInOutreach> findByTenantIdAndSequenceIdOrderBySequenceStepAsc(UUID tenantId, UUID sequenceId);
}
