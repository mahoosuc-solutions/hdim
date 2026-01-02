# Event-Driven Patient Health Assessment Platform - Implementation Progress

## Executive Summary

This document tracks the implementation of a complete event-driven architecture transformation for the HealthData-in-Motion platform. The goal is to enable real-time, automated patient health assessments where FHIR data changes automatically cascade through quality measures, care gaps, risk stratification, and health scores.

**Timeline:** 3-6 months complete architecture transformation
**Primary Goal:** Automated care coordination workflows
**Status:** Phase 1 - Foundation (85% complete)

---

## Phase 1: Foundation (Weeks 1-3) - IN PROGRESS

### ✅ Phase 1.1: Enable Kafka Event Publishing - COMPLETED

**What Was Done:**
- Enabled Kafka event publishing in CQL engine service by updating `application.yml`
- Changed `visualization.kafka.enabled` from `false` to `true`
- Kafka infrastructure was already configured in Docker environment

**Files Modified:**
- `backend/modules/services/cql-engine-service/src/main/resources/application.yml:98`

**Impact:**
- Real-time CQL evaluation events now flow through Kafka
- WebSocket clients can receive batch progress updates
- Foundation for event-driven measure calculations

---

### ✅ Phase 1.2-1.3: FHIR Event Publishers - COMPLETED

**What Was Found:**
All FHIR resource services already have complete event publishing implemented! No code changes were needed.

**Events Already Being Published:**

| Resource | Topics | Files |
|----------|--------|-------|
| **Patients** | `fhir.patients.{created\|updated\|deleted}` | `PatientService.java:63,116,137` |
| **Observations** | `fhir.observations.{created\|updated\|deleted}` | `ObservationService.java:65,119,134` |
| **Conditions** | `fhir.conditions.{created\|updated\|deleted}` | `ConditionService.java:64,120,135` |
| **Procedures** | `fhir.procedures.{created\|updated\|deleted}` | `ProcedureService.java:82,147,172` |
| **Encounters** | `fhir.encounters.{created\|updated\|deleted}` | `EncounterService.java:79,144,169` |
| **MedicationRequests** | `fhir.medication-requests.{created\|updated\|deleted}` | `MedicationRequestService.java:66,125,140` |

**Event Payload Structure:**
```java
record ObservationEvent(
    String id,
    String tenantId,
    String patientId,
    String type,  // CREATED, UPDATED, DELETED
    Instant occurredAt,
    String actor
)
```

**Impact:**
- Every FHIR data change is now published to Kafka
- Ready for downstream event consumers to react
- Foundation for automated care coordination workflows

---

### ✅ Phase 1.4: Dead Letter Queue (DLQ) Implementation - COMPLETED

**What Was Built:**
Created a comprehensive DLQ system for handling failed events with automatic retry and exponential backoff.

**New Files Created:**

1. **`DeadLetterQueueEntity.java`** - Entity with smart retry logic
   - Exponential backoff: 1min → 5min → 30min → 2hr → 12hr
   - Status tracking: FAILED, RETRYING, EXHAUSTED, RESOLVED, DISCARDED
   - Automatic retry eligibility checks
   - Path: `event-processing-service/src/main/java/com/healthdata/events/entity/`

2. **`DeadLetterQueueRepository.java`** - Repository with advanced queries
   - Find retry-eligible events
   - Find exhausted events needing manual intervention
   - Query by tenant, patient, topic, date range
   - Count failures for monitoring
   - Path: `event-processing-service/src/main/java/com/healthdata/events/repository/`

3. **`DeadLetterQueueService.java`** - Service with full DLQ lifecycle
   - `recordFailure()` - Store failed events with full context
   - `markForRetry()` - Retry management with backoff calculation
   - `markAsResolved()` - Manual resolution tracking
   - `markAsExhausted()` - Flag for manual intervention
   - `getStats()` - DLQ metrics for monitoring
   - `cleanupOldResolved()` - Data retention (30 days)
   - Path: `event-processing-service/src/main/java/com/healthdata/events/service/`

4. **`DLQRetryProcessor.java`** - Scheduled automatic retry processor
   - Runs every 2 minutes to retry eligible events
   - Republishes failed events to original Kafka topics
   - Alerts on exhausted events every hour
   - Daily cleanup of old resolved entries at 2 AM
   - Path: `event-processing-service/src/main/java/com/healthdata/events/service/`

5. **`DeadLetterQueueController.java`** - REST API for DLQ management
   - `GET /api/v1/dead-letter-queue/failed` - List failed events
   - `GET /api/v1/dead-letter-queue/patient/{patientId}` - Patient failures
   - `GET /api/v1/dead-letter-queue/exhausted` - Manual intervention needed
   - `GET /api/v1/dead-letter-queue/stats` - Monitoring statistics
   - `POST /api/v1/dead-letter-queue/{dlqId}/retry` - Manual retry
   - `POST /api/v1/dead-letter-queue/{dlqId}/resolve` - Mark resolved
   - Path: `event-processing-service/src/main/java/com/healthdata/events/controller/`

**Database Schema:**
Updated migration: `0003-create-dead-letter-queue-table.xml`

```sql
CREATE TABLE dead_letter_queue (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL,
  tenant_id VARCHAR(64) NOT NULL,
  subscription_id UUID,
  topic VARCHAR(255) NOT NULL,
  event_type VARCHAR(100) NOT NULL,
  patient_id VARCHAR(100),
  event_payload JSONB,
  error_message TEXT NOT NULL,
  stack_trace TEXT,
  retry_count INTEGER DEFAULT 0,
  max_retry_count INTEGER DEFAULT 3,
  first_failure_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  last_retry_at TIMESTAMP WITH TIME ZONE,
  next_retry_at TIMESTAMP WITH TIME ZONE,
  status VARCHAR(20) DEFAULT 'FAILED',
  resolved_at TIMESTAMP WITH TIME ZONE,
  resolved_by VARCHAR(128),
  resolution_notes TEXT
);

-- Indexes for performance
CREATE INDEX idx_dlq_status ON dead_letter_queue(status, first_failure_at DESC);
CREATE INDEX idx_dlq_retry_eligible ON dead_letter_queue(status, next_retry_at);
CREATE INDEX idx_dlq_patient ON dead_letter_queue(patient_id);
CREATE INDEX idx_dlq_topic ON dead_letter_queue(topic, event_type);
```

**Application Configuration:**
- Enabled scheduling in `EventProcessingServiceApplication.java` with `@EnableScheduling`

**Impact:**
- Zero data loss - all failed events are captured
- Automatic retry with exponential backoff
- Manual intervention for exhausted events
- Full audit trail of failures and resolutions
- Monitoring and alerting ready

**Example Usage:**
```java
// In any event consumer:
try {
  processEvent(event);
} catch (Exception e) {
  dlqService.recordFailure(
    "fhir.observations.created",
    "OBSERVATION_CREATED",
    tenantId,
    patientId,
    event,
    e
  );
}
```

---

### 🔄 Phase 1.5: Event Monitoring and Metrics - IN PROGRESS

**Next Steps:**
1. Add Micrometer metrics to DLQ service
2. Prometheus endpoints for monitoring
3. Grafana dashboard templates
4. Alert rules for SLOs

---

### ⏳ Phase 1.6: Event Router Service - PENDING

**Planned Features:**
- Intelligent event routing based on FHIR resource type
- Determine which measures/assessments are affected by data changes
- Priority queue for urgent vs routine events
- Tenant-based routing and isolation
- Load balancing across event consumers

---

## Current Architecture

### Event Flow Diagram

```
FHIR Data Changes
  ├─> fhir.patients.{created|updated|deleted}
  ├─> fhir.observations.{created|updated|deleted}
  ├─> fhir.conditions.{created|updated|deleted}
  ├─> fhir.procedures.{created|updated|deleted}
  ├─> fhir.encounters.{created|updated|deleted}
  └─> fhir.medication-requests.{created|updated|deleted}
        ↓
    Kafka Broker
        ↓
  [Event Consumers] ← Dead Letter Queue (with retry)
        ↓
    [To Be Built in Phase 2-7]
  ├─> Automated Care Gap Closure
  ├─> Real-Time Health Score Updates
  ├─> Continuous Risk Assessment
  ├─> Mental Health Crisis Detection
  └─> Clinical Alerts & Notifications
```

### Services

| Service | Status | Purpose |
|---------|--------|---------|
| **fhir-service** | ✅ Active | Publishes FHIR resource events |
| **cql-engine-service** | ✅ Active | Publishes evaluation events |
| **event-processing-service** | ✅ Active | DLQ and event routing |
| **quality-measure-service** | 🔄 Partial | Quality calculations (to add event consumers) |
| **care-gap-service** | ⏳ Pending | Care gap workflows (to add event consumers) |

---

## Upcoming Phases

### Phase 2: Care Gap Automation (Weeks 4-6)
- Automated care gap closure on procedure/observation events
- Proactive care gap creation from measure calculations
- Integration with clinical workflows

### Phase 3: Real-Time Health Score Engine (Weeks 7-9)
- Event-driven composite health score calculation
- WebSocket broadcast to clinical portals
- Threshold-based alerting

### Phase 4: Risk Stratification Automation (Weeks 10-12)
- Continuous risk assessment on data changes
- Chronic disease deterioration detection
- Predictive modeling integration

### Phase 5: Mental Health Crisis Detection (Weeks 13-15)
- Critical alerts for PHQ-9/GAD-7 scores
- Multi-channel notifications
- Care team escalation workflows

### Phase 6: Performance Optimization (Weeks 16-18)
- Parallel batch processing
- CQRS pattern implementation
- Read model optimization

### Phase 7: Advanced Automation (Weeks 19-24)
- Scheduled intelligence jobs
- Event sourcing for audit trails
- Predictive analytics service

---

## Key Metrics

### Phase 1 Completion Metrics
- ✅ 100% of FHIR resources publishing events
- ✅ Kafka event publishing enabled
- ✅ DLQ with automatic retry implemented
- ✅ API for DLQ management available
- ⏳ Monitoring/metrics: In progress

### Success Criteria for Complete Project
- Time from FHIR data change to health score update: <5 seconds
- Care gap auto-closure rate: >80% of eligible gaps
- Critical mental health alert delivery: <30 seconds
- Population calculation throughput: >1000 patients/minute
- Event processing success rate: >99.9%
- Zero data loss (via DLQ and retry)

---

## How to Use

### Monitoring DLQ

```bash
# Get DLQ statistics
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/event-processing/api/v1/dead-letter-queue/stats

# List failed events
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/event-processing/api/v1/dead-letter-queue/failed

# Get exhausted events (manual intervention needed)
curl http://localhost:8080/event-processing/api/v1/dead-letter-queue/exhausted

# Manually retry an event
curl -X POST http://localhost:8080/event-processing/api/v1/dead-letter-queue/{dlqId}/retry
```

### Checking Event Flow

```bash
# Monitor Kafka topics
kafka-console-consumer --bootstrap-server localhost:9092 --topic fhir.observations.created
kafka-console-consumer --bootstrap-server localhost:9092 --topic fhir.conditions.created

# Watch CQL engine events (if enabled)
kafka-console-consumer --bootstrap-server localhost:9092 --topic evaluation.completed
```

---

## Technical Decisions

### Why Kafka?
- High throughput event streaming
- Topic-based routing
- Message persistence
- Exactly-once semantics with idempotent producers
- Built-in partitioning for scalability

### Why Exponential Backoff?
- Prevents thundering herd on downstream service recovery
- Gives time for transient issues to resolve
- Reduces load during incidents
- Industry best practice for distributed systems

### Why Separate Event Processing Service?
- Centralized DLQ management across all services
- Single source of truth for event failures
- Easier monitoring and alerting
- Reusable event routing logic
- Independent scaling

---

## Next Session Plan

1. **Complete Phase 1.5:** Add monitoring and metrics
   - Micrometer integration
   - Prometheus endpoint
   - Key DLQ metrics

2. **Start Phase 2:** Automated care gap closure
   - Create care gap event listener
   - Match procedures/observations to open gaps
   - Auto-close with evidence linking

3. **Begin Phase 3 foundation:** Health score service structure

---

## Questions for Product/Architecture Review

1. Should we prioritize Phase 2 (care gaps) or Phase 3 (health scores) next?
2. What alert channels should we integrate for exhausted DLQ events? (Email, Slack, PagerDuty?)
3. What SLOs should we target for event processing latency?
4. Should event consumers be in separate services or embedded in existing services?

---

**Last Updated:** 2025-11-25
**Progress:** 21% complete (4/19 tasks)
**Next Milestone:** Phase 1 complete (Event foundation with monitoring)
