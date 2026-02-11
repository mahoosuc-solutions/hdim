package com.healthdata.investor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing a task in the investor launch plan.
 * Tracks 23 pre-defined tasks across categories like Legal, Financial, Technical, etc.
 */
@Entity
@Table(name = "investor_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvestorTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "pending";  // pending, in_progress, completed, blocked

    @Column(name = "category", nullable = false, length = 50)
    private String category;  // Legal, Financial, Technical, Marketing, Governance, Admin

    @Column(name = "week", nullable = false)
    private Integer week;  // Week 1, 2, 3, or 4

    @Column(name = "deliverable", columnDefinition = "TEXT")
    private String deliverable;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if ("completed".equals(status) && completedAt == null) {
            completedAt = Instant.now();
        }
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isBlocked() {
        return "blocked".equals(status);
    }

    public boolean isInProgress() {
        return "in_progress".equals(status);
    }
}
