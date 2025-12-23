package com.healthdata.cql.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for CQL Evaluation Results
 *
 * Stores the results of CQL expression evaluations for patients.
 * Maps to the cql_evaluations table created by Liquibase migration.
 */
@Entity
@Table(name = "cql_evaluations", indexes = {
    @Index(name = "idx_eval_library", columnList = "library_id, evaluation_date"),
    @Index(name = "idx_eval_patient", columnList = "patient_id, evaluation_date"),
    @Index(name = "idx_eval_tenant", columnList = "tenant_id")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CqlEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "library_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_evaluation_library"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CqlLibrary library;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_data", columnDefinition = "JSON")
    private String contextData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evaluation_result", columnDefinition = "JSON")
    private String evaluationResult;

    @Column(name = "status", length = 32)
    private String status; // SUCCESS, FAILED, PENDING

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "evaluation_date", nullable = false)
    private Instant evaluationDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public CqlEvaluation() {
        this.status = "PENDING";
        this.evaluationDate = Instant.now();
    }

    public CqlEvaluation(String tenantId, CqlLibrary library, UUID patientId) {
        this();
        this.tenantId = tenantId;
        this.library = library;
        this.patientId = patientId;
    }

    // JPA Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public CqlLibrary getLibrary() {
        return library;
    }

    public void setLibrary(CqlLibrary library) {
        this.library = library;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public String getContextData() {
        return contextData;
    }

    public void setContextData(String contextData) {
        this.contextData = contextData;
    }

    public String getEvaluationResult() {
        return evaluationResult;
    }

    public void setEvaluationResult(String evaluationResult) {
        this.evaluationResult = evaluationResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Instant getEvaluationDate() {
        return evaluationDate;
    }

    public void setEvaluationDate(Instant evaluationDate) {
        this.evaluationDate = evaluationDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "CqlEvaluation{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", patientId='" + patientId + '\'' +
                ", status='" + status + '\'' +
                ", evaluationDate=" + evaluationDate +
                '}';
    }
}
