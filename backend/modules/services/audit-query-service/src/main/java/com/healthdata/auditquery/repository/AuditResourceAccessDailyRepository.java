package com.healthdata.auditquery.repository;

import com.healthdata.auditquery.persistence.AuditResourceAccessDailyEntity;
import com.healthdata.auditquery.persistence.AuditResourceAccessDailyEntity.ResourceAccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for querying daily resource access projections.
 */
@Repository
public interface AuditResourceAccessDailyRepository extends JpaRepository<AuditResourceAccessDailyEntity, ResourceAccessKey> {

    /**
     * Find all resource access records for a tenant in a date range.
     *
     * @param tenantId tenant ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of resource access records
     */
    @Query("SELECT a FROM AuditResourceAccessDailyEntity a " +
           "WHERE a.id.tenantId = :tenantId " +
           "AND a.id.accessDate >= :startDate " +
           "AND a.id.accessDate <= :endDate " +
           "ORDER BY a.id.accessDate DESC, a.accessCount DESC")
    List<AuditResourceAccessDailyEntity> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find access records for a specific resource in a date range.
     *
     * @param tenantId tenant ID
     * @param resourceType resource type
     * @param resourceId resource ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of resource access records
     */
    @Query("SELECT a FROM AuditResourceAccessDailyEntity a " +
           "WHERE a.id.tenantId = :tenantId " +
           "AND a.id.resourceType = :resourceType " +
           "AND a.id.resourceId = :resourceId " +
           "AND a.id.accessDate >= :startDate " +
           "AND a.id.accessDate <= :endDate " +
           "ORDER BY a.id.accessDate DESC")
    List<AuditResourceAccessDailyEntity> findByTenantIdAndResourceAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("resourceType") String resourceType,
        @Param("resourceId") String resourceId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find top N most accessed resources in a date range.
     *
     * @param tenantId tenant ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param limit max results
     * @return list of resource access records
     */
    @Query(value = "SELECT a.tenant_id, a.resource_type, a.resource_id, a.access_date, " +
                   "SUM(a.access_count) as access_count, " +
                   "COUNT(DISTINCT a.unique_users) as unique_users " +
                   "FROM audit_resource_access_daily a " +
                   "WHERE a.tenant_id = :tenantId " +
                   "AND a.access_date >= :startDate " +
                   "AND a.access_date <= :endDate " +
                   "GROUP BY a.tenant_id, a.resource_type, a.resource_id, a.access_date " +
                   "ORDER BY access_count DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findTopAccessedResources(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("limit") int limit
    );
}
