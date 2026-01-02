package com.healthdata.documentation.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document Feedback Entity
 *
 * Stores user feedback and ratings for product documentation.
 */
@Entity
@Table(name = "document_feedback",
       indexes = {
           @Index(name = "idx_feedback_document", columnList = "document_id"),
           @Index(name = "idx_feedback_tenant", columnList = "tenant_id"),
           @Index(name = "idx_feedback_user", columnList = "user_id"),
           @Index(name = "idx_feedback_rating", columnList = "rating")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFeedbackEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId; // Reference to DocumentMetadataEntity

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "rating")
    private Integer rating; // 1-5 stars

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "helpful")
    private Boolean helpful; // Was this document helpful?

    @Column(name = "feedback_type", length = 50)
    @Builder.Default
    private String feedbackType = "GENERAL"; // GENERAL, BUG, SUGGESTION, QUESTION

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, REVIEWED, RESOLVED

    @Column(name = "admin_response", columnDefinition = "TEXT")
    private String adminResponse;

    @Column(name = "responded_by", length = 100)
    private String respondedBy;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }

    public boolean isPositive() {
        return rating != null && rating >= 4;
    }

    public boolean isNegative() {
        return rating != null && rating <= 2;
    }
}
