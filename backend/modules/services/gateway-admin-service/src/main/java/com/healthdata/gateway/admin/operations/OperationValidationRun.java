package com.healthdata.gateway.admin.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_validation_runs")
public class OperationValidationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "operation_run_id", nullable = false)
    private OperationRun operationRun;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "grade", nullable = false, length = 2)
    private String grade;

    @Column(name = "critical_pass", nullable = false)
    private boolean criticalPass;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "summary_json", columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public OperationRun getOperationRun() {
        return operationRun;
    }

    public void setOperationRun(OperationRun operationRun) {
        this.operationRun = operationRun;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public boolean isCriticalPass() {
        return criticalPass;
    }

    public void setCriticalPass(boolean criticalPass) {
        this.criticalPass = criticalPass;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
