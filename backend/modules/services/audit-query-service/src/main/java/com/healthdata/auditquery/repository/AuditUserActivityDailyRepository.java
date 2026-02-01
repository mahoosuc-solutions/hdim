package com.healthdata.auditquery.repository;

import com.healthdata.auditquery.persistence.AuditUserActivityDailyEntity;
import com.healthdata.auditquery.persistence.AuditUserActivityDailyEntity.UserActivityKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for querying daily user activity projections.
 */
@Repository
public interface AuditUserActivityDailyRepository extends JpaRepository<AuditUserActivityDailyEntity, UserActivityKey> {

    /**
     * Find all user activity records for a tenant in a date range.
     *
     * @param tenantId tenant ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of user activity records
     */
    @Query("SELECT a FROM AuditUserActivityDailyEntity a " +
           "WHERE a.id.tenantId = :tenantId " +
           "AND a.id.activityDate >= :startDate " +
           "AND a.id.activityDate <= :endDate " +
           "ORDER BY a.id.activityDate DESC, a.totalEvents DESC")
    List<AuditUserActivityDailyEntity> findByTenantIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find activity records for a specific user in a date range.
     *
     * @param tenantId tenant ID
     * @param userId user ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of user activity records
     */
    @Query("SELECT a FROM AuditUserActivityDailyEntity a " +
           "WHERE a.id.tenantId = :tenantId " +
           "AND a.id.userId = :userId " +
           "AND a.id.activityDate >= :startDate " +
           "AND a.id.activityDate <= :endDate " +
           "ORDER BY a.id.activityDate DESC")
    List<AuditUserActivityDailyEntity> findByTenantIdAndUserIdAndDateRange(
        @Param("tenantId") String tenantId,
        @Param("userId") String userId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find top N most active users in a date range.
     *
     * @param tenantId tenant ID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param limit max results
     * @return list of user activity records
     */
    @Query(value = "SELECT a.tenant_id, a.user_id, a.activity_date, " +
                   "SUM(a.total_events) as total_events, " +
                   "SUM(a.phi_access_count) as phi_access_count, " +
                   "SUM(a.failed_events) as failed_events, " +
                   "SUM(a.unique_resources) as unique_resources " +
                   "FROM audit_user_activity_daily a " +
                   "WHERE a.tenant_id = :tenantId " +
                   "AND a.activity_date >= :startDate " +
                   "AND a.activity_date <= :endDate " +
                   "GROUP BY a.tenant_id, a.user_id, a.activity_date " +
                   "ORDER BY total_events DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findTopActiveUsers(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("limit") int limit
    );
}
