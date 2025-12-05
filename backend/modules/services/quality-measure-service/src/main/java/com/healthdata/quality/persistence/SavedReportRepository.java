package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SavedReportEntity
 * Provides database access methods for saved reports
 */
@Repository
public interface SavedReportRepository extends JpaRepository<SavedReportEntity, UUID> {

    /**
     * Find all reports for a tenant, ordered by creation date (newest first)
     */
    List<SavedReportEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /**
     * Find reports by tenant and report type
     */
    List<SavedReportEntity> findByTenantIdAndReportTypeOrderByCreatedAtDesc(String tenantId, String reportType);

    /**
     * Find reports by tenant and creator
     */
    List<SavedReportEntity> findByTenantIdAndCreatedByOrderByCreatedAtDesc(String tenantId, String createdBy);

    /**
     * Find report by tenant and ID (tenant isolation)
     */
    Optional<SavedReportEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find reports by tenant and patient ID
     */
    List<SavedReportEntity> findByTenantIdAndPatientIdOrderByCreatedAtDesc(String tenantId, UUID patientId);

    /**
     * Find reports by tenant and year
     */
    List<SavedReportEntity> findByTenantIdAndYearOrderByCreatedAtDesc(String tenantId, Integer year);

    /**
     * Find reports by tenant and status
     */
    List<SavedReportEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);

    /**
     * Count reports by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Count reports by tenant and type
     */
    long countByTenantIdAndReportType(String tenantId, String reportType);

    /**
     * Delete reports by tenant and ID (tenant isolation)
     */
    void deleteByTenantIdAndId(String tenantId, UUID id);
}
