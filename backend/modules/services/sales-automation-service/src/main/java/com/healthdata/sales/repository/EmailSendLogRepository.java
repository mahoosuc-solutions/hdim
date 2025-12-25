package com.healthdata.sales.repository;

import com.healthdata.sales.entity.EmailSendLog;
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
public interface EmailSendLogRepository extends JpaRepository<EmailSendLog, UUID> {

    Page<EmailSendLog> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<EmailSendLog> findByIdAndTenantId(UUID id, UUID tenantId);

    List<EmailSendLog> findByEnrollmentId(UUID enrollmentId);

    Page<EmailSendLog> findBySequenceId(UUID sequenceId, Pageable pageable);

    List<EmailSendLog> findByLeadId(UUID leadId);

    List<EmailSendLog> findByContactId(UUID contactId);

    Optional<EmailSendLog> findByTrackingId(String trackingId);

    @Query("SELECT l FROM EmailSendLog l WHERE l.tenantId = :tenantId " +
           "AND l.sentAt >= :since ORDER BY l.sentAt DESC")
    Page<EmailSendLog> findRecentSends(
        @Param("tenantId") UUID tenantId,
        @Param("since") LocalDateTime since,
        Pageable pageable);

    @Query("SELECT l FROM EmailSendLog l WHERE l.tenantId = :tenantId " +
           "AND l.status = 'PENDING' ORDER BY l.createdAt ASC")
    List<EmailSendLog> findPendingEmails(@Param("tenantId") UUID tenantId);

    // Analytics
    @Query("SELECT COUNT(l) FROM EmailSendLog l WHERE l.sequenceId = :sequenceId")
    Long countBySequenceId(@Param("sequenceId") UUID sequenceId);

    @Query("SELECT COUNT(l) FROM EmailSendLog l WHERE l.sequenceId = :sequenceId " +
           "AND l.status = 'OPENED'")
    Long countOpensBySequenceId(@Param("sequenceId") UUID sequenceId);

    @Query("SELECT COUNT(l) FROM EmailSendLog l WHERE l.sequenceId = :sequenceId " +
           "AND l.status = 'CLICKED'")
    Long countClicksBySequenceId(@Param("sequenceId") UUID sequenceId);

    @Query("SELECT COUNT(l) FROM EmailSendLog l WHERE l.sequenceId = :sequenceId " +
           "AND l.status = 'BOUNCED'")
    Long countBouncesBySequenceId(@Param("sequenceId") UUID sequenceId);

    @Query("SELECT COUNT(l) FROM EmailSendLog l WHERE l.tenantId = :tenantId " +
           "AND l.sentAt >= :since")
    Long countSentSince(
        @Param("tenantId") UUID tenantId,
        @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(l) FROM EmailSendLog l WHERE l.tenantId = :tenantId " +
           "AND l.sentAt >= :since AND l.openedAt IS NOT NULL")
    Long countOpenedSince(
        @Param("tenantId") UUID tenantId,
        @Param("since") LocalDateTime since);
}
