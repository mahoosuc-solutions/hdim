package com.healthdata.fhir.bulk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for BulkExportJob entities
 *
 * Provides data access methods for bulk export job management.
 */
@Repository
public interface BulkExportRepository extends JpaRepository<BulkExportJob, UUID> {

    /**
     * Find a job by ID and tenant ID (multi-tenant isolation)
     */
    Optional<BulkExportJob> findByJobIdAndTenantId(UUID jobId, String tenantId);

    /**
     * Find all jobs for a tenant
     */
    List<BulkExportJob> findByTenantIdOrderByRequestedAtDesc(String tenantId);

    /**
     * Find jobs by tenant and status
     */
    List<BulkExportJob> findByTenantIdAndStatus(String tenantId, BulkExportJob.ExportStatus status);

    /**
     * Find jobs by status
     */
    List<BulkExportJob> findByStatus(BulkExportJob.ExportStatus status);

    /**
     * Find jobs older than a specific timestamp (for cleanup)
     */
    List<BulkExportJob> findByCompletedAtBefore(Instant cutoffTime);

    /**
     * Count active jobs (PENDING or IN_PROGRESS)
     */
    @Query("SELECT COUNT(j) FROM BulkExportJob j WHERE j.status IN ('PENDING', 'IN_PROGRESS')")
    long countActiveJobs();

    /**
     * Count active jobs for a specific tenant
     */
    @Query("SELECT COUNT(j) FROM BulkExportJob j WHERE j.tenantId = :tenantId AND j.status IN ('PENDING', 'IN_PROGRESS')")
    long countActiveJobsByTenant(@Param("tenantId") String tenantId);

    /**
     * Find pending jobs ordered by request time (for processing queue)
     */
    List<BulkExportJob> findByStatusOrderByRequestedAtAsc(BulkExportJob.ExportStatus status);
}
