package com.healthdata.fhir.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for FHIR Goal resources.
 * Provides tenant-scoped queries for patient health goals.
 */
@Repository
public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    /**
     * Find goal by tenant and ID
     */
    Optional<GoalEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);

    /**
     * Find all goals for a patient
     */
    List<GoalEntity> findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByTargetDateAsc(
            String tenantId, UUID patientId);

    /**
     * Find goals by patient and lifecycle status
     */
    List<GoalEntity> findByTenantIdAndPatientIdAndLifecycleStatusAndDeletedAtIsNull(
            String tenantId, UUID patientId, String lifecycleStatus);

    /**
     * Find active goals for a patient
     */
    @Query("SELECT g FROM GoalEntity g WHERE g.tenantId = :tenantId " +
           "AND g.patientId = :patientId " +
           "AND g.lifecycleStatus IN ('active', 'accepted') " +
           "AND g.deletedAt IS NULL " +
           "ORDER BY g.priorityCode ASC NULLS LAST, g.targetDate ASC NULLS LAST")
    List<GoalEntity> findActiveGoalsForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find goals by achievement status
     */
    List<GoalEntity> findByTenantIdAndPatientIdAndAchievementStatusAndDeletedAtIsNull(
            String tenantId, UUID patientId, String achievementStatus);

    /**
     * Find goals by category
     */
    List<GoalEntity> findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(
            String tenantId, UUID patientId, String categoryCode);

    /**
     * Find goals addressing a specific condition
     */
    List<GoalEntity> findByTenantIdAndAddressesConditionIdAndDeletedAtIsNull(
            String tenantId, UUID conditionId);

    /**
     * Find overdue goals (past target date, not achieved)
     */
    @Query("SELECT g FROM GoalEntity g WHERE g.tenantId = :tenantId " +
           "AND g.patientId = :patientId " +
           "AND g.targetDate < :today " +
           "AND g.achievementStatus NOT IN ('achieved', 'sustaining') " +
           "AND g.lifecycleStatus IN ('active', 'accepted') " +
           "AND g.deletedAt IS NULL " +
           "ORDER BY g.targetDate ASC")
    List<GoalEntity> findOverdueGoals(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("today") LocalDate today);

    /**
     * Find goals due within a date range
     */
    @Query("SELECT g FROM GoalEntity g WHERE g.tenantId = :tenantId " +
           "AND g.targetDate BETWEEN :startDate AND :endDate " +
           "AND g.lifecycleStatus IN ('active', 'accepted') " +
           "AND g.deletedAt IS NULL " +
           "ORDER BY g.targetDate ASC")
    List<GoalEntity> findGoalsDueInRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Search goals with multiple criteria
     */
    @Query("SELECT g FROM GoalEntity g WHERE g.tenantId = :tenantId " +
           "AND g.deletedAt IS NULL " +
           "AND (:patientId IS NULL OR g.patientId = :patientId) " +
           "AND (:lifecycleStatus IS NULL OR g.lifecycleStatus = :lifecycleStatus) " +
           "AND (:achievementStatus IS NULL OR g.achievementStatus = :achievementStatus) " +
           "AND (:categoryCode IS NULL OR g.categoryCode = :categoryCode) " +
           "AND (:priority IS NULL OR g.priorityCode = :priority) " +
           "ORDER BY g.lastModifiedAt DESC")
    Page<GoalEntity> searchGoals(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("lifecycleStatus") String lifecycleStatus,
            @Param("achievementStatus") String achievementStatus,
            @Param("categoryCode") String categoryCode,
            @Param("priority") String priority,
            Pageable pageable);

    /**
     * Count goals by lifecycle status for a patient
     */
    @Query("SELECT g.lifecycleStatus, COUNT(g) FROM GoalEntity g " +
           "WHERE g.tenantId = :tenantId AND g.patientId = :patientId AND g.deletedAt IS NULL " +
           "GROUP BY g.lifecycleStatus")
    List<Object[]> countByLifecycleStatus(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count goals by achievement status for a patient
     */
    @Query("SELECT g.achievementStatus, COUNT(g) FROM GoalEntity g " +
           "WHERE g.tenantId = :tenantId AND g.patientId = :patientId AND g.deletedAt IS NULL " +
           "GROUP BY g.achievementStatus")
    List<Object[]> countByAchievementStatus(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count goals by category for a patient
     */
    @Query("SELECT g.categoryCode, g.categoryDisplay, COUNT(g) FROM GoalEntity g " +
           "WHERE g.tenantId = :tenantId AND g.patientId = :patientId AND g.deletedAt IS NULL " +
           "GROUP BY g.categoryCode, g.categoryDisplay")
    List<Object[]> countByCategory(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find high priority active goals
     */
    @Query("SELECT g FROM GoalEntity g WHERE g.tenantId = :tenantId " +
           "AND g.patientId = :patientId " +
           "AND g.priorityCode = 'high-priority' " +
           "AND g.lifecycleStatus IN ('active', 'accepted') " +
           "AND g.deletedAt IS NULL " +
           "ORDER BY g.targetDate ASC NULLS LAST")
    List<GoalEntity> findHighPriorityGoals(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find all goals for tenant (paginated)
     */
    Page<GoalEntity> findByTenantIdAndDeletedAtIsNullOrderByLastModifiedAtDesc(
            String tenantId, Pageable pageable);
}
