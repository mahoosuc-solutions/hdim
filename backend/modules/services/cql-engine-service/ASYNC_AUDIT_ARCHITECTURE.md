# Async Audit Architecture for CQL Engine Service

## Overview

The CQL Engine Service implements a comprehensive asynchronous audit logging system to meet healthcare compliance requirements. This system captures detailed audit trails of all significant operations while ensuring **zero performance impact** on CQL evaluation and data processing.

## Key Requirements Addressed

As stated by stakeholders:
> "we want to audit what is happening in the engine so we can show users how the data is flowing and why. This is making impactful choices about a person's health"

This architecture enables:
- **Complete audit trails** of all CQL evaluations
- **Data flow visualization** showing how data moves through the engine
- **Compliance** with HIPAA and healthcare audit requirements
- **Zero performance impact** through async, fire-and-forget event emission
- **Scalable storage** with long-term retention in TimescaleDB

## Architecture Components

### 1. Audit Event Models (`/event/audit/`)

Five comprehensive event types capture all significant operations:

#### `CqlEvaluationAuditEvent`
**Purpose**: Tracks CQL evaluation operations - the core audit requirement

**Key Fields**:
- `evaluationId`, `cqlLibraryId`, `patientId` - What was evaluated
- `fhirResourcesAccessed`, `fhirResourceCount` - What data was used
- `evaluationStartTime`, `evaluationEndTime`, `durationMs` - Performance metrics
- `numerator`, `denominator`, `exclusion` - Quality measure results
- `dataFlowSteps` - Detailed step-by-step data flow tracking
  - Each step captures: name, type, timestamp, resources accessed, input/output, decision, reasoning

**Example Data Flow**:
```json
{
  "dataFlowSteps": [
    {
      "stepNumber": 1,
      "stepName": "Fetch Patient Demographics",
      "stepType": "DATA_FETCH",
      "resourcesAccessed": ["Patient"],
      "decision": "Patient age: 45",
      "reasoning": "Retrieved patient birth date for age calculation"
    },
    {
      "stepNumber": 2,
      "stepName": "Evaluate Diabetes Criteria",
      "stepType": "LOGIC_DECISION",
      "decision": "Patient IN Numerator",
      "reasoning": "HbA1c < 7% AND last measurement within 12 months"
    }
  ]
}
```

#### `CqlLibraryAuditEvent`
**Purpose**: Tracks CQL library management (create, update, delete)

**Key Fields**:
- `libraryId`, `libraryName`, `libraryVersion`
- `libraryContentLength` - Size of CQL content
- `previousVersion` - For tracking updates

#### `ValueSetAuditEvent`
**Purpose**: Tracks value set management

**Key Fields**:
- `valueSetId`, `valueSetOid`, `valueSetName`
- `codesCount` - Number of codes in the value set

#### `DataAccessAuditEvent`
**Purpose**: HIPAA compliance - tracks all patient data access

**Key Fields**:
- `patientId`, `fhirResourceType`, `fhirResourceIds`
- `purpose` - Why data was accessed
- `evaluationId` - Link to related evaluation

#### `SecurityAuditEvent`
**Purpose**: Security event tracking

**Key Fields**:
- `securityEventType` - Type of security event
- `denialReason` - Why access was denied
- `severity` - Event severity level

### 2. AOP Aspects (`/aspect/`)

Spring AOP aspects automatically capture audit events without modifying business logic.

#### `CqlEvaluationAuditAspect`
```java
@Around("execution(* com.healthdata.cql.service.CqlEvaluationService.executeEvaluation(..))")
```
- Intercepts all CQL evaluation calls
- Captures start/end time, duration
- Emits success or failure events
- **Zero performance impact** - async emission after evaluation completes

#### `CqlLibraryAuditAspect`
```java
@Around("execution(* com.healthdata.cql.service.CqlLibraryService.create*(..))")
@Around("execution(* com.healthdata.cql.service.CqlLibraryService.update*(..))")
@Around("execution(* com.healthdata.cql.service.CqlLibraryService.delete*(..))")
```
- Captures all library management operations
- Tracks who made changes and when

#### `ValueSetAuditAspect`
```java
@Around("execution(* com.healthdata.cql.service.ValueSetService.create*(..))")
@Around("execution(* com.healthdata.cql.service.ValueSetService.update*(..))")
@Around("execution(* com.healthdata.cql.service.ValueSetService.delete*(..))")
```
- Captures all value set management operations
- Tracks code set changes

### 3. Kafka Event Producer (`/event/audit/AuditEventProducer`)

**Topic**: `healthdata.audit.events`

**Partitioning Strategy**: By `tenantId`
- Ensures ordered processing of events per tenant
- Enables tenant-level audit isolation

**Delivery Semantics**: Fire-and-forget with async callbacks
```java
CompletableFuture<SendResult> future = kafkaTemplate.send(topic, tenantId, event);
future.whenComplete((result, ex) -> { /* Log success/failure */ });
```

**Error Handling**: Audit failures never affect business logic
- Exceptions caught and logged
- Business operations always proceed

### 4. Configuration

#### Kafka Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: StringSerializer
      value-serializer: JsonSerializer
      acks: all
      retries: 3
```

#### Audit Configuration
```yaml
audit:
  kafka:
    topic: "healthdata.audit.events"
    enabled: ${AUDIT_ENABLED:true}
  data-flow-tracking:
    enabled: ${AUDIT_DATA_FLOW_ENABLED:true}
    max-steps: 50
```

## Data Flow Tracking

The most critical feature for healthcare compliance is **data flow tracking** - showing users "how data is flowing and why."

### Implementation Plan

1. **Context Propagation**: Use ThreadLocal or MDC to track current evaluation context
2. **Step Recording**: Each major operation records a DataFlowStep:
   - FHIR data fetches
   - CQL expression evaluations
   - Logic decisions (numerator/denominator/exclusion)
3. **Decision Reasoning**: Capture WHY decisions were made:
   - "Patient age 45 > 40 (threshold)"
   - "HbA1c 6.5% < 7% (target)"
4. **Resource Tracking**: Record all FHIR resources accessed

### Example Integration Point

```java
// In MeasureTemplateEngine or CQL evaluation logic
@Component
public class DataFlowTracker {
    private ThreadLocal<List<DataFlowStep>> currentFlow = new ThreadLocal<>();

    public void recordStep(String name, String type, String decision, String reasoning) {
        currentFlow.get().add(DataFlowStep.builder()
            .stepNumber(currentFlow.get().size() + 1)
            .stepName(name)
            .stepType(type)
            .timestamp(Instant.now())
            .decision(decision)
            .reasoning(reasoning)
            .build());
    }
}
```

## Future Enhancements (Phase 3)

### Audit Consumer Service

A dedicated microservice to consume audit events and store them in TimescaleDB:

```
healthdata.audit.events (Kafka)
           ↓
    Audit Consumer Service
           ↓
    TimescaleDB (time-series DB)
           ↓
    Audit Query API
    Audit Visualization UI
```

**Features**:
- Long-term retention (7+ years for healthcare compliance)
- Fast time-series queries
- Audit trail reporting
- Data flow visualization
- Compliance reports (HIPAA access logs)

### TimescaleDB Schema

```sql
CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    audit_type VARCHAR(50) NOT NULL,
    performed_by VARCHAR(255),
    action VARCHAR(100),
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    result VARCHAR(20),
    details JSONB,
    -- CQL Evaluation specific
    evaluation_id UUID,
    patient_id VARCHAR(255),
    duration_ms BIGINT,
    data_flow_steps JSONB,
    -- Indexes
    INDEX idx_tenant_timestamp (tenant_id, timestamp DESC),
    INDEX idx_patient (patient_id),
    INDEX idx_evaluation (evaluation_id)
);

-- Convert to hypertable for time-series optimization
SELECT create_hypertable('audit_events', 'timestamp');
```

## Compliance Benefits

### HIPAA Compliance
- ✅ Access logs for all patient data
- ✅ Who accessed what and when
- ✅ Purpose of access tracking
- ✅ Audit trail retention (7 years)

### Clinical Decision Support
- ✅ Complete traceability of quality measure calculations
- ✅ Data provenance - what data was used
- ✅ Decision reasoning - why results came out the way they did
- ✅ Reproducibility - ability to replay evaluations

### Operational Benefits
- ✅ Performance monitoring (evaluation duration)
- ✅ Error tracking and debugging
- ✅ Usage analytics
- ✅ Capacity planning

## Performance Characteristics

### Zero-Impact Design
- Audit events emitted **after** business logic completes
- Kafka publishing is async (non-blocking)
- Failures logged but don't affect operations
- No database writes during evaluation

### Scalability
- Kafka handles millions of events/second
- Partitioning by tenant enables horizontal scaling
- TimescaleDB compression for long-term storage
- Consumer service can scale independently

## Monitoring

### Metrics to Track
- Audit events published/second
- Kafka publish latency
- Audit event size distribution
- Failed audit emissions
- Consumer lag

### Alerts
- Kafka connection failures
- High publish latency (> 100ms)
- Consumer lag > 1000 messages
- Audit emission failure rate > 1%

## Security Considerations

### Data Sanitization
- PHI data should be sanitized in audit events
- Store references (IDs) not full patient data
- FHIR resource content not included in audit

### Access Control
- Audit data access restricted to compliance officers
- Tenant isolation enforced in queries
- Audit trail of audit access (meta-auditing)

## Summary

This async audit architecture provides:

1. **Complete Compliance**: HIPAA audit trails, data provenance
2. **Zero Performance Impact**: Async, fire-and-forget design
3. **Data Flow Visibility**: Users can see "how data is flowing and why"
4. **Scalability**: Kafka + TimescaleDB handle enterprise scale
5. **Maintainability**: AOP aspects - no business logic changes needed

The architecture is production-ready and can be extended with the Audit Consumer Service (Phase 3) for long-term storage and querying.
