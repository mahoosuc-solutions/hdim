package com.healthdata.demo.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks a demo session (recording session or live demo).
 *
 * Sessions help track:
 * - Which scenario is currently loaded
 * - When data was last reset
 * - Performance metrics during the session
 * - Session state for recovery
 */
@Entity
@Table(name = "demo_sessions")
public class DemoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    private DemoScenario scenario;

    @Column(name = "session_name", length = 255)
    private String sessionName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SessionStatus status = SessionStatus.INITIALIZING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_reset_at")
    private Instant lastResetAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "session_state", columnDefinition = "JSONB")
    private String sessionState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "performance_metrics", columnDefinition = "JSONB")
    private String performanceMetrics;

    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
    }

    // Constructors
    public DemoSession() {}

    public DemoSession(DemoScenario scenario, String sessionName) {
        this.scenario = scenario;
        this.sessionName = sessionName;
        this.status = SessionStatus.INITIALIZING;
    }

    // Business methods
    public void markReady() {
        this.status = SessionStatus.READY;
    }

    public void markRecording() {
        this.status = SessionStatus.RECORDING;
    }

    public void reset() {
        this.lastResetAt = Instant.now();
        this.status = SessionStatus.READY;
    }

    public void end() {
        this.endedAt = Instant.now();
        this.status = SessionStatus.ENDED;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public DemoScenario getScenario() { return scenario; }
    public void setScenario(DemoScenario scenario) { this.scenario = scenario; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getLastResetAt() { return lastResetAt; }
    public void setLastResetAt(Instant lastResetAt) { this.lastResetAt = lastResetAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public String getSessionState() { return sessionState; }
    public void setSessionState(String sessionState) { this.sessionState = sessionState; }

    public String getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(String performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }

    public UUID getSnapshotId() { return snapshotId; }
    public void setSnapshotId(UUID snapshotId) { this.snapshotId = snapshotId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    /**
     * Session lifecycle states.
     */
    public enum SessionStatus {
        INITIALIZING,  // Scenario being loaded
        READY,         // Ready for recording/demo
        RECORDING,     // Currently being recorded
        PAUSED,        // Recording paused
        ENDED          // Session completed
    }
}
