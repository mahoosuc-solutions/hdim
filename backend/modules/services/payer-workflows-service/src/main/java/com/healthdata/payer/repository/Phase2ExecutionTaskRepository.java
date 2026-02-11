package com.healthdata.payer.repository;

import com.healthdata.payer.domain.Phase2ExecutionTask;
import com.healthdata.payer.domain.Phase2ExecutionTask.TaskCategory;
import com.healthdata.payer.domain.Phase2ExecutionTask.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Phase2ExecutionTaskRepository extends JpaRepository<Phase2ExecutionTask, String> {

    // ===== Basic Queries =====

    Optional<Phase2ExecutionTask> findByIdAndTenantId(String id, String tenantId);

    List<Phase2ExecutionTask> findByTenantIdOrderByPhase2WeekAscTargetDueDate(String tenantId);

    // ===== Filter by Category =====

    Page<Phase2ExecutionTask> findByTenantIdAndCategoryOrderByTargetDueDate(
            String tenantId,
            TaskCategory category,
            Pageable pageable);

    // ===== Filter by Status =====

    Page<Phase2ExecutionTask> findByTenantIdAndStatusOrderByTargetDueDate(
            String tenantId,
            TaskStatus status,
            Pageable pageable);

    // ===== Filter by Week =====

    List<Phase2ExecutionTask> findByTenantIdAndPhase2WeekOrderByPriorityDescTargetDueDate(
            String tenantId,
            Integer week);

    // ===== Open Tasks (not completed or cancelled) =====

    @Query("""
        SELECT t FROM Phase2ExecutionTask t
        WHERE t.tenantId = :tenantId
        AND t.status NOT IN ('COMPLETED', 'CANCELLED')
        ORDER BY t.priority DESC, t.targetDueDate ASC
        """)
    List<Phase2ExecutionTask> findOpenTasks(@Param("tenantId") String tenantId);

    // ===== Dependency Queries =====

    @Query("""
        SELECT t FROM Phase2ExecutionTask t
        WHERE t.tenantId = :tenantId
        AND t.blockedByTasks LIKE CONCAT('%', :blockingTaskId, '%')
        ORDER BY t.targetDueDate ASC
        """)
    List<Phase2ExecutionTask> findBlockedByTask(
            @Param("blockingTaskId") String blockingTaskId,
            @Param("tenantId") String tenantId);

    @Query("""
        SELECT t FROM Phase2ExecutionTask t
        WHERE t.tenantId = :tenantId
        AND t.blocksTasks LIKE CONCAT('%', :blockedTaskId, '%')
        ORDER BY t.targetDueDate ASC
        """)
    List<Phase2ExecutionTask> findBlockingTasks(
            @Param("blockedTaskId") String blockedTaskId,
            @Param("tenantId") String tenantId);

    // ===== Upcoming Tasks (due in next N days) =====

    @Query("""
        SELECT t FROM Phase2ExecutionTask t
        WHERE t.tenantId = :tenantId
        AND t.status != 'COMPLETED'
        AND t.targetDueDate <= :dueDate
        ORDER BY t.targetDueDate ASC
        """)
    List<Phase2ExecutionTask> findUpcomingTasks(
            @Param("tenantId") String tenantId,
            @Param("dueDate") java.time.Instant dueDate);
}
