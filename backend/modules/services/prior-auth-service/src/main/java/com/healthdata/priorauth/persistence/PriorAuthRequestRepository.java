package com.healthdata.priorauth.persistence;

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

/**
 * Repository for Prior Authorization requests.
 */
@Repository
public interface PriorAuthRequestRepository extends JpaRepository<PriorAuthRequestEntity, UUID> {

    Optional<PriorAuthRequestEntity> findByPaRequestId(String paRequestId);

    Page<PriorAuthRequestEntity> findByTenantIdAndPatientId(
        String tenantId, UUID patientId, Pageable pageable);

    Page<PriorAuthRequestEntity> findByTenantIdAndStatus(
        String tenantId, PriorAuthRequestEntity.Status status, Pageable pageable);

    Page<PriorAuthRequestEntity> findByTenantIdAndPayerId(
        String tenantId, String payerId, Pageable pageable);

    @Query("SELECT p FROM PriorAuthRequestEntity p WHERE p.tenantId = :tenantId " +
           "AND p.status IN :statuses ORDER BY p.createdAt DESC")
    Page<PriorAuthRequestEntity> findByTenantIdAndStatusIn(
        @Param("tenantId") String tenantId,
        @Param("statuses") List<PriorAuthRequestEntity.Status> statuses,
        Pageable pageable);

    @Query("SELECT p FROM PriorAuthRequestEntity p WHERE p.tenantId = :tenantId " +
           "AND p.slaDeadline < :deadline AND p.status IN :pendingStatuses")
    List<PriorAuthRequestEntity> findApproachingSlaDeadline(
        @Param("tenantId") String tenantId,
        @Param("deadline") LocalDateTime deadline,
        @Param("pendingStatuses") List<PriorAuthRequestEntity.Status> pendingStatuses);

    @Query("SELECT p FROM PriorAuthRequestEntity p WHERE p.status = :status " +
           "AND p.retryCount < :maxRetries ORDER BY p.createdAt ASC")
    List<PriorAuthRequestEntity> findPendingForRetry(
        @Param("status") PriorAuthRequestEntity.Status status,
        @Param("maxRetries") Integer maxRetries);

    @Query("SELECT COUNT(p) FROM PriorAuthRequestEntity p WHERE p.tenantId = :tenantId " +
           "AND p.status = :status AND p.createdAt >= :since")
    long countByTenantIdAndStatusSince(
        @Param("tenantId") String tenantId,
        @Param("status") PriorAuthRequestEntity.Status status,
        @Param("since") LocalDateTime since);

    @Query("SELECT p.status, COUNT(p) FROM PriorAuthRequestEntity p " +
           "WHERE p.tenantId = :tenantId GROUP BY p.status")
    List<Object[]> getStatusCounts(@Param("tenantId") String tenantId);

    @Query("SELECT p FROM PriorAuthRequestEntity p WHERE p.tenantId = :tenantId " +
           "AND p.providerId = :providerId AND p.status IN :statuses " +
           "ORDER BY p.createdAt DESC")
    Page<PriorAuthRequestEntity> findByProviderAndStatuses(
        @Param("tenantId") String tenantId,
        @Param("providerId") String providerId,
        @Param("statuses") List<PriorAuthRequestEntity.Status> statuses,
        Pageable pageable);
}
