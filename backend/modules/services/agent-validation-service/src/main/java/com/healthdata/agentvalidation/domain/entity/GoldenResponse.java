package com.healthdata.agentvalidation.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Stores approved baseline responses for regression testing.
 * Golden responses are used to detect behavioral drift in agent outputs.
 */
@Entity
@Table(name = "golden_responses", indexes = {
    @Index(name = "idx_golden_responses_test_case", columnList = "test_case_id"),
    @Index(name = "idx_golden_responses_archived", columnList = "archived"),
    @Index(name = "idx_golden_responses_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoldenResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    /**
     * The approved baseline response text.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    /**
     * Pre-computed embedding vector for similarity comparison.
     * Stored as JSON array of floats.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "embedding_vector", columnDefinition = "jsonb")
    private float[] embeddingVector;

    /**
     * The evaluation score when this golden response was approved.
     */
    @Column(name = "evaluation_score", precision = 3, scale = 2)
    private BigDecimal evaluationScore;

    /**
     * Metric results from the original evaluation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metric_results", columnDefinition = "jsonb")
    private Map<String, Object> metricResults;

    /**
     * Minimum similarity threshold for regression detection.
     */
    @Column(name = "similarity_threshold", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal similarityThreshold = new BigDecimal("0.85");

    /**
     * Who approved this as a golden response.
     */
    @Column(name = "approved_by")
    private String approvedBy;

    /**
     * When the golden response was approved.
     */
    @Column(name = "approved_at")
    private Instant approvedAt;

    /**
     * Reason for approval or notes.
     */
    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    /**
     * Whether this golden response has been superseded.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    /**
     * When this golden response was archived (if applicable).
     */
    @Column(name = "archived_at")
    private Instant archivedAt;

    /**
     * Who archived this golden response.
     */
    @Column(name = "archived_by")
    private String archivedBy;

    /**
     * Reason for archiving.
     */
    @Column(name = "archive_reason", columnDefinition = "TEXT")
    private String archiveReason;

    /**
     * Reference to the previous golden response (for lineage tracking).
     */
    @Column(name = "previous_golden_id")
    private UUID previousGoldenId;

    /**
     * LLM provider used to generate this response.
     */
    @Column(name = "llm_provider")
    private String llmProvider;

    /**
     * Model version used to generate this response.
     */
    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "version")
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    /**
     * Archive this golden response when superseded.
     */
    public void archive(String archivedByUser, String reason) {
        this.archived = true;
        this.archivedAt = Instant.now();
        this.archivedBy = archivedByUser;
        this.archiveReason = reason;
    }
}
