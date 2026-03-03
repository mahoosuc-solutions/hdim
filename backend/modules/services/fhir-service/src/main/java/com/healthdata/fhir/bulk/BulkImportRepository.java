package com.healthdata.fhir.bulk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for BulkImportJob entities.
 * Mirrors BulkExportRepository pattern with multi-tenant isolation.
 */
@Repository
public interface BulkImportRepository extends JpaRepository<BulkImportJob, UUID> {

    Optional<BulkImportJob> findByJobIdAndTenantId(UUID jobId, String tenantId);

    List<BulkImportJob> findByTenantIdOrderBySubmittedAtDesc(String tenantId);

    List<BulkImportJob> findByTenantIdAndStatus(String tenantId, BulkImportJob.ImportStatus status);

    @Query("SELECT COUNT(j) FROM BulkImportJob j WHERE j.tenantId = :tenantId "
            + "AND j.status IN ('PENDING', 'IN_PROGRESS')")
    long countActiveJobsByTenant(@Param("tenantId") String tenantId);
}
