package com.healthdata.payer.service;

import com.healthdata.payer.domain.Phase2ExecutionTask;
import com.healthdata.payer.domain.Phase2ExecutionTask.*;
import com.healthdata.payer.repository.Phase2ExecutionTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Phase 2 (Traction) Execution Service
 *
 * Manages task scheduling, status tracking, and cross-functional visibility
 * for the March 2026 go-to-market execution plan.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class Phase2ExecutionService {
    private final Phase2ExecutionTaskRepository taskRepository;

    // ===== Create Operations =====

    /**
     * Create a new Phase 2 execution task
     */
    @Transactional
    public Phase2ExecutionTask createTask(
            String tenantId,
            String taskName,
            String description,
            TaskCategory category,
            Instant targetDueDate,
            TaskPriority priority,
            String ownerName,
            String ownerRole) {

        Phase2ExecutionTask task = Phase2ExecutionTask.builder()
                .tenantId(tenantId)
                .taskName(taskName)
                .description(description)
                .category(category)
                .targetDueDate(targetDueDate)
                .priority(priority)
                .ownerName(ownerName)
                .ownerRole(ownerRole)
                .status(TaskStatus.PENDING)
                .progressPercentage(0)
                .build();

        return taskRepository.save(task);
    }

    // ===== Read Operations =====

    /**
     * Get all Phase 2 tasks for a tenant, grouped by week
     */
    public List<Phase2ExecutionTask> getTasksByTenantAndWeek(String tenantId, Integer week) {
        return taskRepository.findByTenantIdAndPhase2WeekOrderByPriorityDescTargetDueDate(
                tenantId, week);
    }

    /**
     * Get all Phase 2 tasks by category
     */
    public Page<Phase2ExecutionTask> getTasksByCategory(
            String tenantId,
            TaskCategory category,
            Pageable pageable) {
        return taskRepository.findByTenantIdAndCategoryOrderByTargetDueDate(
                tenantId, category, pageable);
    }

    /**
     * Get all Phase 2 tasks by status
     */
    public Page<Phase2ExecutionTask> getTasksByStatus(
            String tenantId,
            TaskStatus status,
            Pageable pageable) {
        return taskRepository.findByTenantIdAndStatusOrderByTargetDueDate(
                tenantId, status, pageable);
    }

    /**
     * Get all open Phase 2 tasks (not completed or cancelled)
     */
    public List<Phase2ExecutionTask> getOpenTasks(String tenantId) {
        return taskRepository.findOpenTasks(tenantId);
    }

    /**
     * Get Phase 2 dashboard summary (week-by-week progress)
     */
    public Phase2DashboardSummary getDashboardSummary(String tenantId) {
        List<Phase2ExecutionTask> allTasks = taskRepository.findByTenantIdOrderByPhase2WeekAscTargetDueDate(tenantId);

        int totalTasks = allTasks.size();
        long completedTasks = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();
        long inProgressTasks = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS)
                .count();
        long blockedTasks = allTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.BLOCKED)
                .count();

        int completionPercentage = totalTasks > 0 ? (int) ((completedTasks * 100) / totalTasks) : 0;

        return Phase2DashboardSummary.builder()
                .totalTasks(totalTasks)
                .completedTasks((int) completedTasks)
                .inProgressTasks((int) inProgressTasks)
                .blockedTasks((int) blockedTasks)
                .completionPercentage(completionPercentage)
                .tasksByCategory(groupByCategory(allTasks))
                .tasksByWeek(groupByWeek(allTasks))
                .build();
    }

    private java.util.Map<String, Integer> groupByCategory(List<Phase2ExecutionTask> tasks) {
        var grouped = new java.util.HashMap<String, Integer>();
        for (TaskCategory cat : TaskCategory.values()) {
            long count = tasks.stream().filter(t -> t.getCategory() == cat).count();
            grouped.put(cat.name(), (int) count);
        }
        return grouped;
    }

    private java.util.Map<Integer, Integer> groupByWeek(List<Phase2ExecutionTask> tasks) {
        var grouped = new java.util.HashMap<Integer, Integer>();
        for (Phase2ExecutionTask task : tasks) {
            if (task.getPhase2Week() != null) {
                grouped.put(task.getPhase2Week(),
                        grouped.getOrDefault(task.getPhase2Week(), 0) + 1);
            }
        }
        return grouped;
    }

    // ===== Update Operations =====

    /**
     * Update task status and progress
     */
    @Transactional
    public Phase2ExecutionTask updateTaskStatus(
            String taskId,
            String tenantId,
            TaskStatus newStatus,
            Integer progressPercentage) {

        Phase2ExecutionTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(newStatus);
        if (progressPercentage != null) {
            task.setProgressPercentage(progressPercentage);
        }

        return taskRepository.save(task);
    }

    /**
     * Mark task as complete with outcomes
     */
    @Transactional
    public Phase2ExecutionTask completeTask(
            String taskId,
            String tenantId,
            String actualOutcomes) {

        Phase2ExecutionTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.COMPLETED);
        task.setProgressPercentage(100);
        task.setActualOutcomes(actualOutcomes);
        task.setCompletedDate(Instant.now());

        return taskRepository.save(task);
    }

    /**
     * Block a task with unblock date
     */
    @Transactional
    public Phase2ExecutionTask blockTask(
            String taskId,
            String tenantId,
            String blockReason,
            Instant unblockedDate) {

        Phase2ExecutionTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        task.setStatus(TaskStatus.BLOCKED);
        task.setBlockedUntil(unblockedDate);
        task.setNotes((task.getNotes() != null ? task.getNotes() + "\n" : "") + "BLOCKED: " + blockReason);

        return taskRepository.save(task);
    }

    /**
     * Unblock a task
     */
    @Transactional
    public Phase2ExecutionTask unblockTask(String taskId, String tenantId) {
        Phase2ExecutionTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (task.getStatus() == TaskStatus.BLOCKED) {
            task.setStatus(TaskStatus.IN_PROGRESS);
            task.setBlockedUntil(null);
        }

        return taskRepository.save(task);
    }

    /**
     * Add a note to a task
     */
    @Transactional
    public Phase2ExecutionTask addNote(String taskId, String tenantId, String note) {
        Phase2ExecutionTask task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        String timestamp = "[" + Instant.now() + "] ";
        task.setNotes((task.getNotes() != null ? task.getNotes() + "\n" : "") + timestamp + note);

        return taskRepository.save(task);
    }

    // ===== Dependency Management =====

    /**
     * Get tasks blocked by a specific task
     */
    public List<Phase2ExecutionTask> getBlockedByTask(String blockingTaskId, String tenantId) {
        return taskRepository.findBlockedByTask(blockingTaskId, tenantId);
    }

    /**
     * Get tasks blocking a specific task
     */
    public List<Phase2ExecutionTask> getBlockingTasks(String blockedTaskId, String tenantId) {
        return taskRepository.findBlockingTasks(blockedTaskId, tenantId);
    }

    @lombok.Data
    @lombok.Builder
    public static class Phase2DashboardSummary {
        private int totalTasks;
        private int completedTasks;
        private int inProgressTasks;
        private int blockedTasks;
        private int completionPercentage;
        private java.util.Map<String, Integer> tasksByCategory;
        private java.util.Map<Integer, Integer> tasksByWeek;
    }
}
