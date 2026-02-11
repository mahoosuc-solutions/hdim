package com.healthdata.payer.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2 (Traction) Execution Task Tracking
 *
 * Represents individual tasks that comprise the Phase 2 GTM execution plan.
 * Tracks ownership, status, deadlines, and cross-functional dependencies.
 *
 * HIPAA Note: This entity tracks internal execution tasks only, not patient data.
 * No PHI is stored in this entity.
 */
@Entity
@Table(name = "phase2_execution_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Phase2ExecutionTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Task Identification
    @Column(name = "task_name", nullable = false)
    private String taskName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "task_category", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskCategory category; // PRODUCT, SALES, MARKETING, ENGINEERING

    // Timeline
    @Column(name = "target_due_date", nullable = false)
    private Instant targetDueDate;

    @Column(name = "completed_date")
    private Instant completedDate;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    // Status & Progress
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status; // PENDING, IN_PROGRESS, BLOCKED, COMPLETED, CANCELLED

    @Column(name = "priority", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskPriority priority; // CRITICAL, HIGH, MEDIUM, LOW

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    // Ownership & Assignment
    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "owner_role")
    private String ownerRole; // CEO, VP_SALES, VP_PRODUCT, ENGINEERING_LEAD, etc.

    // Dependencies & Impact
    @Column(name = "blocks_tasks")
    private String blocksTaskIds; // Comma-separated list of task IDs this task blocks

    @Column(name = "blocked_by_tasks")
    private String blockedByTaskIds; // Comma-separated list of task IDs blocking this task

    // Success Metrics
    @Column(name = "success_metrics", length = 1000)
    private String successMetrics; // JSON or formatted text describing how success is measured

    @Column(name = "actual_outcomes", length = 2000)
    private String actualOutcomes; // Results achieved upon completion

    // Audit Fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "notes", length = 2000)
    private String notes;

    // ==================== Financial ROI Tracking Fields ====================
    @Column(name = "hedis_measure", length = 10)
    private String hediseMeasure; // HEDIS measure code (e.g., HBA1C, BCS, CCS)

    @Column(name = "baseline_performance_percentage", precision = 5, scale = 2)
    private BigDecimal baselinePerformancePercentage; // Baseline % before intervention

    @Column(name = "current_performance_percentage", precision = 5, scale = 2)
    private BigDecimal currentPerformancePercentage; // Current % after intervention

    @Column(name = "quality_bonus_at_risk", precision = 12, scale = 2)
    private BigDecimal qualityBonusAtRisk; // Total potential quality bonus revenue at risk

    @Column(name = "quality_bonus_captured", precision = 12, scale = 2)
    private BigDecimal qualityBonusCaptured; // Actual quality bonus revenue captured

    @Column(name = "intervention_type", length = 50)
    private String interventionType; // Type of intervention used (e.g., "Digital Outreach", "Provider Training")

    @Column(name = "gaps_closed")
    private Integer gapsClosed; // Number of care gaps closed

    @Column(name = "cost_per_gap", precision = 10, scale = 2)
    private BigDecimal costPerGap; // Cost incurred per gap closure

    @Column(name = "roi_percentage", precision = 8, scale = 2)
    private BigDecimal roiPercentage; // Return on investment percentage

    @Column(name = "customer_quote", columnDefinition = "TEXT")
    private String customerQuote; // Customer testimonial quote demonstrating value

    @Column(name = "case_study_published")
    private Boolean caseStudyPublished; // Flag indicating if case study has been published

    // Phase 2 Context
    @Column(name = "phase2_week")
    private Integer phase2Week; // 1-4 indicating which week of March 2026

    @Column(name = "sprint_cycle")
    private String sprintCycle; // e.g., "Week 1-2: Positioning", "Week 3-4: Pilot Acquisition"

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = TaskStatus.PENDING;
        }
        if (progressPercentage == null) {
            progressPercentage = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if (status == TaskStatus.COMPLETED && completedDate == null) {
            completedDate = Instant.now();
        }
    }

    public enum TaskCategory {
        PRODUCT("Product & Engineering"),
        SALES("Sales & Business Development"),
        MARKETING("Marketing & Thought Leadership"),
        LEADERSHIP("Executive & Strategy");

        public final String displayName;

        TaskCategory(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum TaskStatus {
        PENDING("Not Started"),
        IN_PROGRESS("In Progress"),
        BLOCKED("Blocked"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");

        public final String displayName;

        TaskStatus(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum TaskPriority {
        CRITICAL("Critical Path - Must Complete"),
        HIGH("High Priority"),
        MEDIUM("Medium Priority"),
        LOW("Nice to Have");

        public final String displayName;

        TaskPriority(String displayName) {
            this.displayName = displayName;
        }
    }
}
