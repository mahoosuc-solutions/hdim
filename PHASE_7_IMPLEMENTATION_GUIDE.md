# Phase 7 Implementation Guide

## Quick Reference for Implementing Test-Passing Code

This guide provides the exact steps to implement services that will pass all TDD tests.

---

## 🗂️ File Structure

### Test Files (Already Created) ✅
```
src/test/java/com/healthdata/quality/
├── scheduler/
│   ├── RiskReassessmentSchedulerTest.java         (7 tests)
│   ├── PopulationUpdateSchedulerTest.java         (9 tests)
│   ├── DataFreshnessMonitorTest.java             (10 tests)
│   └── JobExecutionTrackerTest.java              (11 tests)
├── eventsourcing/
│   ├── EventSourcingServiceTest.java             (10 tests)
│   └── EventReplayServiceTest.java               (10 tests)
└── ml/
    ├── PredictiveAnalyticsServiceTest.java       (12 tests)
    └── FeatureExtractorTest.java                 (12 tests)
```

### Implementation Files (To Create)
```
src/main/java/com/healthdata/quality/
├── scheduler/
│   ├── RiskReassessmentScheduler.java
│   ├── PopulationUpdateScheduler.java
│   ├── DataFreshnessMonitor.java
│   └── JobExecutionTracker.java
├── eventsourcing/
│   ├── EventSourcingService.java
│   └── EventReplayService.java
├── ml/
│   ├── PredictiveAnalyticsService.java
│   ├── FeatureExtractor.java
│   └── ModelRegistry.java
└── persistence/
    ├── HealthEventEntity.java
    ├── HealthEventRepository.java
    ├── EventSnapshotEntity.java
    ├── EventSnapshotRepository.java
    ├── MLPredictionEntity.java
    ├── MLPredictionRepository.java
    ├── JobExecutionEntity.java
    └── JobExecutionRepository.java
```

---

## 📦 Step 1: Create Entity Classes

### HealthEventEntity.java

```java
package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "health_events", indexes = {
    @Index(name = "idx_he_event_number", columnList = "event_number"),
    @Index(name = "idx_he_aggregate", columnList = "tenant_id,aggregate_type,aggregate_id,event_number"),
    @Index(name = "idx_he_event_type", columnList = "tenant_id,event_type,occurred_at"),
    @Index(name = "idx_he_time_travel", columnList = "tenant_id,aggregate_type,aggregate_id,occurred_at"),
    @Index(name = "idx_he_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthEventEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "event_number", nullable = false)
    private Long eventNumber;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion = 1;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "event_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> eventData;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
```

### EventSnapshotEntity.java

```java
package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "event_snapshots", indexes = {
    @Index(name = "idx_es_latest", columnList = "tenant_id,aggregate_type,aggregate_id,event_number"),
    @Index(name = "idx_es_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSnapshotEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_number", nullable = false)
    private Long eventNumber;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "snapshot_data", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> snapshotData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
```

### MLPredictionEntity.java

```java
package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "ml_predictions", indexes = {
    @Index(name = "idx_mlp_patient", columnList = "tenant_id,patient_id,predicted_at"),
    @Index(name = "idx_mlp_type", columnList = "tenant_id,prediction_type,predicted_at"),
    @Index(name = "idx_mlp_model_performance", columnList = "tenant_id,model_name,model_version,actual_outcome"),
    @Index(name = "idx_mlp_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLPredictionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 100)
    private String patientId;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "prediction_type", nullable = false, length = 50)
    private PredictionType predictionType;

    @Column(name = "prediction_value", nullable = false)
    private Double predictionValue;

    @Column(name = "confidence_score", nullable = false)
    private Double confidenceScore;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "features_used", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> featuresUsed;

    @Column(name = "predicted_at", nullable = false)
    private Instant predictedAt;

    @Column(name = "outcome_date")
    private Instant outcomeDate;

    @Column(name = "actual_outcome")
    private Boolean actualOutcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum PredictionType {
        READMISSION_RISK,
        ED_VISIT_RISK,
        DISEASE_PROGRESSION,
        MEDICATION_ADHERENCE,
        CARE_GAP_CLOSURE
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (predictedAt == null) {
            predictedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### JobExecutionEntity.java

```java
package com.healthdata.quality.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "job_executions", indexes = {
    @Index(name = "idx_je_job_history", columnList = "tenant_id,job_name,started_at"),
    @Index(name = "idx_je_running", columnList = "tenant_id,job_name,status"),
    @Index(name = "idx_je_performance", columnList = "job_name,status,duration_ms"),
    @Index(name = "idx_je_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "result_message", columnDefinition = "TEXT")
    private String resultMessage;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Type(io.hypersistence.utils.hibernate.type.json.JsonBinaryType.class)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum Status {
        RUNNING,
        SUCCESS,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (startedAt == null) {
            startedAt = now;
        }
        if (status == null) {
            status = Status.RUNNING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        if (completedAt != null && durationMs == null && startedAt != null) {
            durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }
}
```

---

## 📦 Step 2: Create Repository Interfaces

### HealthEventRepository.java

```java
package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthEventRepository extends JpaRepository<HealthEventEntity, UUID> {

    @Query("SELECT COALESCE(MAX(e.eventNumber), 0) + 1 FROM HealthEventEntity e " +
           "WHERE e.tenantId = :tenantId AND e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId")
    Long getNextEventNumber(
        @Param("tenantId") String tenantId,
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId
    );

    @Query("SELECT COALESCE(MAX(e.eventNumber), 0) FROM HealthEventEntity e " +
           "WHERE e.tenantId = :tenantId AND e.aggregateType = :aggregateType AND e.aggregateId = :aggregateId")
    Long getLatestEventNumber(
        @Param("tenantId") String tenantId,
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId
    );

    List<HealthEventEntity> findByTenantIdAndAggregateTypeAndAggregateIdOrderByEventNumberAsc(
        String tenantId,
        String aggregateType,
        String aggregateId
    );

    @Query("SELECT e FROM HealthEventEntity e " +
           "WHERE e.tenantId = :tenantId AND e.aggregateType = :aggregateType " +
           "AND e.aggregateId = :aggregateId AND e.occurredAt <= :timestamp " +
           "ORDER BY e.eventNumber ASC")
    List<HealthEventEntity> findByAggregateAndTimestamp(
        @Param("tenantId") String tenantId,
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId,
        @Param("timestamp") Instant timestamp
    );

    @Query("SELECT e FROM HealthEventEntity e " +
           "WHERE e.tenantId = :tenantId AND e.aggregateType = :aggregateType " +
           "AND e.aggregateId = :aggregateId AND e.eventNumber > :eventNumber " +
           "ORDER BY e.eventNumber ASC")
    List<HealthEventEntity> findByAggregateAfterEventNumber(
        @Param("tenantId") String tenantId,
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId,
        @Param("eventNumber") Long eventNumber
    );

    List<HealthEventEntity> findByTenantIdAndAggregateTypeAndAggregateIdAndEventTypeOrderByEventNumberAsc(
        String tenantId,
        String aggregateType,
        String aggregateId,
        String eventType
    );
}
```

### EventSnapshotRepository.java

```java
package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventSnapshotRepository extends JpaRepository<EventSnapshotEntity, UUID> {

    @Query("SELECT s FROM EventSnapshotEntity s " +
           "WHERE s.tenantId = :tenantId AND s.aggregateType = :aggregateType " +
           "AND s.aggregateId = :aggregateId " +
           "ORDER BY s.eventNumber DESC LIMIT 1")
    Optional<EventSnapshotEntity> findLatestSnapshot(
        @Param("tenantId") String tenantId,
        @Param("aggregateType") String aggregateType,
        @Param("aggregateId") String aggregateId
    );
}
```

### MLPredictionRepository.java

```java
package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MLPredictionRepository extends JpaRepository<MLPredictionEntity, UUID> {

    @Query("SELECT COUNT(p) FROM MLPredictionEntity p " +
           "WHERE p.tenantId = :tenantId AND p.modelName = :modelName " +
           "AND p.modelVersion = :modelVersion AND p.actualOutcome IS NOT NULL " +
           "AND ((p.predictionValue >= :threshold AND p.actualOutcome = true) " +
           "OR (p.predictionValue < :threshold AND p.actualOutcome = false))")
    Long countCorrectPredictions(
        @Param("tenantId") String tenantId,
        @Param("modelName") String modelName,
        @Param("modelVersion") String modelVersion,
        @Param("threshold") Double threshold
    );

    @Query("SELECT COUNT(p) FROM MLPredictionEntity p " +
           "WHERE p.tenantId = :tenantId AND p.modelName = :modelName " +
           "AND p.modelVersion = :modelVersion AND p.actualOutcome IS NOT NULL")
    Long countTotalPredictionsWithOutcomes(
        @Param("tenantId") String tenantId,
        @Param("modelName") String modelName,
        @Param("modelVersion") String modelVersion
    );
}
```

### JobExecutionRepository.java

```java
package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobExecutionRepository extends JpaRepository<JobExecutionEntity, UUID> {

    @Query("SELECT j FROM JobExecutionEntity j " +
           "WHERE j.tenantId = :tenantId AND j.jobName = :jobName " +
           "AND j.status = 'SUCCESS' " +
           "ORDER BY j.completedAt DESC LIMIT 1")
    Optional<JobExecutionEntity> findLastSuccessfulExecution(
        @Param("tenantId") String tenantId,
        @Param("jobName") String jobName
    );

    @Query("SELECT CASE WHEN COUNT(j) > 0 THEN true ELSE false END " +
           "FROM JobExecutionEntity j " +
           "WHERE j.tenantId = :tenantId AND j.jobName = :jobName " +
           "AND j.status = 'RUNNING'")
    boolean hasRunningJob(
        @Param("tenantId") String tenantId,
        @Param("jobName") String jobName
    );

    @Query("SELECT j FROM JobExecutionEntity j " +
           "WHERE j.tenantId = :tenantId AND j.jobName = :jobName " +
           "ORDER BY j.startedAt DESC LIMIT :limit")
    List<JobExecutionEntity> findRecentExecutions(
        @Param("tenantId") String tenantId,
        @Param("jobName") String jobName,
        @Param("limit") int limit
    );

    @Query("SELECT COUNT(j) FROM JobExecutionEntity j " +
           "WHERE j.tenantId = :tenantId AND j.jobName = :jobName " +
           "AND j.status = 'SUCCESS' AND j.completedAt >= :since")
    Long countSuccessfulExecutions(
        @Param("tenantId") String tenantId,
        @Param("jobName") String jobName,
        @Param("since") Instant since
    );

    @Query("SELECT COUNT(j) FROM JobExecutionEntity j " +
           "WHERE j.tenantId = :tenantId AND j.jobName = :jobName " +
           "AND j.completedAt >= :since")
    Long countTotalExecutions(
        @Param("tenantId") String tenantId,
        @Param("jobName") String jobName,
        @Param("since") Instant since
    );

    @Modifying
    @Query("DELETE FROM JobExecutionEntity j " +
           "WHERE j.tenantId = :tenantId AND j.completedAt < :cutoffDate")
    void deleteOldExecutions(
        @Param("tenantId") String tenantId,
        @Param("cutoffDate") Instant cutoffDate
    );
}
```

---

## 🎯 Running Tests

After implementing entities and repositories:

```bash
cd /home/webemo-aaron/projects/healthdata-in-motion/backend

# Run all Phase 7 tests
./gradlew :modules:services:quality-measure-service:test \
  --tests "*Scheduler*" \
  --tests "*EventSourcing*" \
  --tests "*PredictiveAnalytics*" \
  --tests "*FeatureExtractor*"

# Run specific test class
./gradlew :modules:services:quality-measure-service:test \
  --tests "RiskReassessmentSchedulerTest"
```

---

## 📝 Next Implementation Steps

1. **Implement EventSourcingService** - Make the 10 tests pass
2. **Implement EventReplayService** - Make the 10 tests pass
3. **Implement FeatureExtractor** - Make the 12 tests pass
4. **Implement PredictiveAnalyticsService** - Make the 12 tests pass
5. **Implement JobExecutionTracker** - Make the 11 tests pass
6. **Implement all Schedulers** - Make the 26 tests pass

Total: **81 tests to pass** ✅

---

## 🔍 Test Verification Checklist

- [ ] All 81 tests pass
- [ ] No compilation errors
- [ ] Database migrations run successfully
- [ ] Multi-tenant isolation verified
- [ ] Error handling tested
- [ ] Performance benchmarks met

---

**Ready to implement? Start with entities and repositories, then move to services!**
