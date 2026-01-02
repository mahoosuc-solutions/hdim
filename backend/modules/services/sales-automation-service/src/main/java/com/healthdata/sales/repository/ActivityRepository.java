package com.healthdata.sales.repository;

import com.healthdata.sales.entity.Activity;
import com.healthdata.sales.entity.ActivityType;
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
public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    Page<Activity> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<Activity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Activity> findByZohoActivityId(String zohoActivityId);

    Page<Activity> findByTenantIdAndLeadId(UUID tenantId, UUID leadId, Pageable pageable);

    Page<Activity> findByTenantIdAndContactId(UUID tenantId, UUID contactId, Pageable pageable);

    Page<Activity> findByTenantIdAndOpportunityId(UUID tenantId, UUID opportunityId, Pageable pageable);

    Page<Activity> findByTenantIdAndAccountId(UUID tenantId, UUID accountId, Pageable pageable);

    Page<Activity> findByTenantIdAndActivityType(UUID tenantId, ActivityType type, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.assignedToUserId = :userId")
    Page<Activity> findByTenantIdAndAssignedTo(@Param("tenantId") UUID tenantId,
                                                @Param("userId") UUID userId,
                                                Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId " +
           "AND a.completed = false AND a.scheduledAt <= :before")
    Page<Activity> findOverdueActivities(@Param("tenantId") UUID tenantId,
                                          @Param("before") LocalDateTime before,
                                          Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId " +
           "AND a.completed = false " +
           "AND a.scheduledAt BETWEEN :startDate AND :endDate")
    Page<Activity> findUpcomingActivities(@Param("tenantId") UUID tenantId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId " +
           "AND a.assignedToUserId = :userId " +
           "AND a.completed = false")
    Page<Activity> findPendingActivitiesForUser(@Param("tenantId") UUID tenantId,
                                                 @Param("userId") UUID userId,
                                                 Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId " +
           "AND a.completed = true " +
           "AND a.completedAt BETWEEN :startDate AND :endDate")
    List<Activity> findCompletedActivitiesInRange(@Param("tenantId") UUID tenantId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId " +
           "AND a.completed = false")
    Long countPendingActivities(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(a) FROM Activity a WHERE a.tenantId = :tenantId " +
           "AND a.activityType = :type " +
           "AND a.completedAt >= :since")
    Long countCompletedActivitiesByType(@Param("tenantId") UUID tenantId,
                                         @Param("type") ActivityType type,
                                         @Param("since") LocalDateTime since);

    @Query("SELECT a FROM Activity a WHERE a.tenantId = :tenantId AND a.zohoActivityId IS NULL")
    List<Activity> findUnsyncedActivities(@Param("tenantId") UUID tenantId);

    List<Activity> findByOpportunityId(UUID opportunityId);

    List<Activity> findByLeadId(UUID leadId);

    List<Activity> findByAccountId(UUID accountId);
}
