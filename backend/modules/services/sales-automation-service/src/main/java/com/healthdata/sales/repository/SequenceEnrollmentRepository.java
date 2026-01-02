package com.healthdata.sales.repository;

import com.healthdata.sales.entity.EnrollmentStatus;
import com.healthdata.sales.entity.SequenceEnrollment;
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
public interface SequenceEnrollmentRepository extends JpaRepository<SequenceEnrollment, UUID> {

    Page<SequenceEnrollment> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<SequenceEnrollment> findByIdAndTenantId(UUID id, UUID tenantId);

    Page<SequenceEnrollment> findByTenantIdAndStatus(UUID tenantId, EnrollmentStatus status, Pageable pageable);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.sequence.id = :sequenceId")
    Page<SequenceEnrollment> findBySequenceId(@Param("sequenceId") UUID sequenceId, Pageable pageable);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.sequence.id = :sequenceId AND e.status = :status")
    Page<SequenceEnrollment> findBySequenceIdAndStatus(
        @Param("sequenceId") UUID sequenceId,
        @Param("status") EnrollmentStatus status,
        Pageable pageable);

    List<SequenceEnrollment> findByLeadId(UUID leadId);

    List<SequenceEnrollment> findByContactId(UUID contactId);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.tenantId = :tenantId AND e.leadId = :leadId AND e.status = 'ACTIVE'")
    List<SequenceEnrollment> findActiveEnrollmentsForLead(
        @Param("tenantId") UUID tenantId,
        @Param("leadId") UUID leadId);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.tenantId = :tenantId AND e.contactId = :contactId AND e.status = 'ACTIVE'")
    List<SequenceEnrollment> findActiveEnrollmentsForContact(
        @Param("tenantId") UUID tenantId,
        @Param("contactId") UUID contactId);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.sequence.id = :sequenceId " +
           "AND (e.leadId = :leadId OR e.contactId = :contactId) AND e.status IN ('ACTIVE', 'PAUSED')")
    Optional<SequenceEnrollment> findExistingEnrollment(
        @Param("sequenceId") UUID sequenceId,
        @Param("leadId") UUID leadId,
        @Param("contactId") UUID contactId);

    // For scheduled email processing
    @Query("SELECT e FROM SequenceEnrollment e WHERE e.status = 'ACTIVE' " +
           "AND e.nextEmailAt IS NOT NULL AND e.nextEmailAt <= :cutoffTime " +
           "ORDER BY e.nextEmailAt ASC")
    List<SequenceEnrollment> findDueForEmail(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.tenantId = :tenantId AND e.status = 'ACTIVE' " +
           "AND e.nextEmailAt IS NOT NULL AND e.nextEmailAt <= :cutoffTime " +
           "ORDER BY e.nextEmailAt ASC")
    List<SequenceEnrollment> findDueForEmailByTenant(
        @Param("tenantId") UUID tenantId,
        @Param("cutoffTime") LocalDateTime cutoffTime);

    // Analytics queries
    @Query("SELECT COUNT(e) FROM SequenceEnrollment e WHERE e.sequence.id = :sequenceId")
    Long countBySequenceId(@Param("sequenceId") UUID sequenceId);

    @Query("SELECT COUNT(e) FROM SequenceEnrollment e WHERE e.sequence.id = :sequenceId AND e.status = :status")
    Long countBySequenceIdAndStatus(
        @Param("sequenceId") UUID sequenceId,
        @Param("status") EnrollmentStatus status);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.unsubscribeToken = :token")
    Optional<SequenceEnrollment> findByUnsubscribeToken(@Param("token") String token);

    @Query("SELECT e FROM SequenceEnrollment e WHERE e.email = :email AND e.status = 'ACTIVE'")
    List<SequenceEnrollment> findActiveEnrollmentsByEmail(@Param("email") String email);
}
