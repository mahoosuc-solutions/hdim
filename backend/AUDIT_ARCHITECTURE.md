# Asynchronous Audit Architecture for CQL Engine

## Overview
Healthcare-grade audit logging for high-speed CQL evaluation with zero performance impact.

## Architecture Principles

### 1. **Event-Driven Asynchronous Design**
- Business logic emits audit events to Kafka (non-blocking)
- Separate audit consumer service processes events
- No database writes in critical path
- Sub-millisecond overhead per operation

### 2. **Comprehensive Audit Coverage**

#### What to Audit (CQL Engine Context):
```
┌─────────────────────────────────────────────────────────────┐
│ Audit Event Types                                           │
├─────────────────────────────────────────────────────────────┤
│ 1. MEASURE_EVALUATION_STARTED                               │
│    - Patient ID, Measure ID, Tenant ID                      │
│    - Timestamp, User/System ID                              │
│                                                              │
│ 2. CQL_EXPRESSION_EVALUATED                                 │
│    - Expression name, Input parameters                      │
│    - Result value, Execution time                           │
│    - Data sources accessed                                  │
│                                                              │
│ 3. FHIR_DATA_RETRIEVED                                      │
│    - Resource type, Resource IDs                            │
│    - Query parameters, Result count                         │
│    - PHI accessed flag                                      │
│                                                              │
│ 4. DECISION_MADE                                            │
│    - Decision type (numerator/denominator/exclusion)        │
│    - Contributing factors, Rule applied                     │
│    - Confidence score                                       │
│                                                              │
│ 5. VALUE_SET_LOOKUP                                         │
│    - ValueSet ID, Code searched                             │
│    - Match result, Source                                   │
│                                                              │
│ 6. MEASURE_EVALUATION_COMPLETED                             │
│    - Final result, Duration                                 │
│    - Cache hit/miss, Error flag                             │
└─────────────────────────────────────────────────────────────┘
```

### 3. **Event Schema Design**

```java
// Base audit event
@Data
public class AuditEvent {
    private String eventId;          // UUID
    private String eventType;        // MEASURE_EVALUATION_STARTED, etc.
    private String tenantId;         // Multi-tenant isolation
    private String userId;           // Who initiated
    private String patientId;        // PHI - encrypted in transit
    private Instant timestamp;       // Event time
    private String correlationId;    // Trace entire evaluation
    private Map<String, Object> context;  // Event-specific data
    private Map<String, Object> metadata; // System metadata
}

// Specific event types
@Data
public class CqlExpressionEvaluatedEvent extends AuditEvent {
    private String expressionName;
    private String expressionLibrary;
    private Object inputParameters;
    private Object resultValue;
    private Long executionTimeMs;
    private List<String> dataSourcesAccessed;
    private String cqlVersion;
}

@Data
public class DecisionMadeEvent extends AuditEvent {
    private String decisionType;     // NUMERATOR, DENOMINATOR, EXCLUSION
    private String measureId;
    private Boolean inNumerator;
    private Boolean inDenominator;
    private List<String> contributingFactors;
    private Map<String, Object> ruleEvaluation;
    private String rationale;        // Human-readable explanation
}
```

### 4. **Implementation Approaches**

#### Option A: **Aspect-Oriented Programming (AOP)** ✅ RECOMMENDED
```java
@Aspect
@Component
public class AuditAspect {

    @Autowired
    private AuditEventPublisher publisher;

    // Audit CQL expression evaluation
    @Around("@annotation(Auditable)")
    public Object auditCqlEvaluation(ProceedingJoinPoint joinPoint) throws Throwable {
        String correlationId = MDC.get("correlationId");
        Instant start = Instant.now();

        try {
            Object result = joinPoint.proceed();

            // Emit audit event asynchronously
            publisher.publishAsync(CqlExpressionEvaluatedEvent.builder()
                .correlationId(correlationId)
                .expressionName(getExpressionName(joinPoint))
                .resultValue(result)
                .executionTimeMs(Duration.between(start, Instant.now()).toMillis())
                .build());

            return result;
        } catch (Exception e) {
            publisher.publishAsync(createErrorEvent(joinPoint, e));
            throw e;
        }
    }
}

// Usage
@Service
public class CqlEvaluationService {

    @Auditable(eventType = "CQL_EXPRESSION_EVALUATED")
    public Object evaluateExpression(String expression, Context context) {
        // Business logic only - audit is transparent
        return cqlEngine.evaluate(expression, context);
    }
}
```

#### Option B: **Event Publisher Pattern**
```java
@Service
public class CqlEvaluationService {

    @Autowired
    private AuditEventPublisher auditPublisher;

    public MeasureResult evaluateMeasure(String patientId, String measureId) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        // Start event
        auditPublisher.publish(MeasureEvaluationStartedEvent.builder()
            .correlationId(correlationId)
            .patientId(patientId)
            .measureId(measureId)
            .build());

        try {
            // Business logic
            MeasureResult result = performEvaluation(patientId, measureId);

            // Decision event
            auditPublisher.publish(DecisionMadeEvent.builder()
                .correlationId(correlationId)
                .decisionType("NUMERATOR")
                .inNumerator(result.isInNumerator())
                .contributingFactors(result.getFactors())
                .build());

            return result;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
```

### 5. **Kafka Integration**

```java
@Component
public class KafkaAuditEventPublisher implements AuditEventPublisher {

    @Autowired
    private KafkaTemplate<String, AuditEvent> kafkaTemplate;

    @Value("${audit.kafka.topic}")
    private String auditTopic;

    @Async("auditExecutor")
    public CompletableFuture<SendResult> publishAsync(AuditEvent event) {
        // Enrich with system metadata
        event.setHostname(getHostname());
        event.setServiceName("cql-engine-service");
        event.setServiceVersion(getVersion());

        // Partition by tenant for isolation
        String key = event.getTenantId() + ":" + event.getCorrelationId();

        return kafkaTemplate.send(auditTopic, key, event);
    }
}

// Configuration
@Configuration
@EnableAsync
public class AuditConfig {

    @Bean("auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(10000);  // Large queue to handle bursts
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### 6. **Audit Consumer Service**

```java
@Service
public class AuditConsumerService {

    @Autowired
    private AuditRepository auditRepository;

    @KafkaListener(
        topics = "${audit.kafka.topic}",
        groupId = "audit-consumer-group",
        concurrency = "4"  // Parallel processing
    )
    public void consumeAuditEvent(ConsumerRecord<String, AuditEvent> record) {
        AuditEvent event = record.value();

        // Batch insert for performance
        auditRepository.save(event);

        // Optional: Send to other systems
        if (event.containsPHI()) {
            phiAccessLogger.log(event);
        }

        if (event.isDecisionEvent()) {
            clinicalDecisionSupport.record(event);
        }
    }

    // Batch processing for high throughput
    @Scheduled(fixedDelay = 5000)
    public void flushBatch() {
        auditRepository.flush();
    }
}
```

### 7. **Database Schema (Time-Series Optimized)**

```sql
-- Use TimescaleDB or similar for time-series data
CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    correlation_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    patient_id VARCHAR(100),  -- Encrypted
    timestamp TIMESTAMPTZ NOT NULL,
    event_data JSONB NOT NULL,  -- Flexible schema
    service_name VARCHAR(50),
    service_version VARCHAR(20)
);

-- Hypertable for automatic partitioning
SELECT create_hypertable('audit_events', 'timestamp');

-- Indexes
CREATE INDEX idx_audit_tenant_time ON audit_events (tenant_id, timestamp DESC);
CREATE INDEX idx_audit_correlation ON audit_events (correlation_id);
CREATE INDEX idx_audit_patient ON audit_events (patient_id, timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_events (event_type, timestamp DESC);

-- GIN index for JSONB queries
CREATE INDEX idx_audit_event_data ON audit_events USING GIN (event_data);

-- Retention policy (automatically drop old partitions)
SELECT add_retention_policy('audit_events', INTERVAL '7 years');  -- HIPAA requirement
```

### 8. **Performance Characteristics**

```
┌────────────────────────────────────────────────────────────┐
│ Performance Metrics                                        │
├────────────────────────────────────────────────────────────┤
│ Event Emission Overhead:     < 1ms (async)                │
│ Kafka Throughput:            100K+ events/sec              │
│ Consumer Throughput:         50K+ events/sec/consumer      │
│ End-to-End Latency:          < 100ms (event to DB)        │
│ Storage per Event:           ~2-5 KB (compressed)          │
│ Query Response:              < 50ms (recent data)          │
│                                                            │
│ Impact on CQL Evaluation:    < 0.5% overhead              │
└────────────────────────────────────────────────────────────┘
```

### 9. **Compliance & Security**

#### HIPAA Compliance
```java
@Component
public class PHIProtectionInterceptor {

    @Autowired
    private EncryptionService encryption;

    public void beforePublish(AuditEvent event) {
        // Encrypt PHI before sending to Kafka
        if (event.getPatientId() != null) {
            event.setPatientId(encryption.encrypt(event.getPatientId()));
        }

        // Redact sensitive fields in event_data
        if (event.getContext() != null) {
            redactPHI(event.getContext());
        }
    }
}
```

#### Audit Trail Integrity
```java
// Add cryptographic hash chain
@Component
public class AuditIntegrityService {

    private String previousHash = "genesis";

    public void addIntegrityHash(AuditEvent event) {
        String eventHash = computeHash(event, previousHash);
        event.setIntegrityHash(eventHash);
        event.setPreviousHash(previousHash);
        previousHash = eventHash;
    }

    private String computeHash(AuditEvent event, String previousHash) {
        String data = event.toString() + previousHash;
        return DigestUtils.sha256Hex(data);
    }
}
```

### 10. **Query & Reporting API**

```java
@RestController
@RequestMapping("/api/audit")
public class AuditQueryController {

    @Autowired
    private AuditQueryService auditService;

    // Get complete audit trail for a patient measure evaluation
    @GetMapping("/measure-evaluation/{correlationId}")
    public AuditTrail getEvaluationTrail(@PathVariable String correlationId) {
        return auditService.getTrailByCorrelation(correlationId);
    }

    // Explain why a decision was made
    @GetMapping("/decision/{patientId}/{measureId}/explain")
    public DecisionExplanation explainDecision(
            @PathVariable String patientId,
            @PathVariable String measureId) {

        List<AuditEvent> events = auditService.findDecisionEvents(patientId, measureId);
        return decisionExplainer.buildExplanation(events);
    }

    // PHI access audit (HIPAA requirement)
    @GetMapping("/phi-access/{patientId}")
    public List<PHIAccessEvent> getPatientAccessLog(
            @PathVariable String patientId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        return auditService.findPHIAccess(patientId, startDate, endDate);
    }
}
```

### 11. **Visualization & Monitoring**

```yaml
# Grafana Dashboard Panels
panels:
  - title: "CQL Evaluations per Second"
    query: "rate(audit_events{event_type='MEASURE_EVALUATION_COMPLETED'}[1m])"

  - title: "Average Evaluation Time"
    query: "avg(audit_event_duration_ms{event_type='MEASURE_EVALUATION_COMPLETED'})"

  - title: "Decision Distribution"
    query: "count by (decision_type) (audit_events{event_type='DECISION_MADE'})"

  - title: "PHI Access Heatmap"
    query: "sum by (user_id, hour) (audit_events{contains_phi='true'})"
```

## Implementation Phases

### Phase 1: Core Infrastructure (Week 1)
- [ ] Event models and schemas
- [ ] Kafka producer configuration
- [ ] Async publishing infrastructure
- [ ] Basic consumer service

### Phase 2: CQL Engine Integration (Week 1-2)
- [ ] AOP aspects for expression evaluation
- [ ] Decision event emission
- [ ] FHIR data access tracking
- [ ] Correlation ID propagation

### Phase 3: Storage & Retrieval (Week 2)
- [ ] TimescaleDB setup
- [ ] Repository implementation
- [ ] Query API
- [ ] Retention policies

### Phase 4: Security & Compliance (Week 3)
- [ ] PHI encryption
- [ ] Integrity hashing
- [ ] Access control
- [ ] HIPAA audit reports

### Phase 5: Visualization & Monitoring (Week 3-4)
- [ ] Grafana dashboards
- [ ] Real-time alerting
- [ ] Decision explanation UI
- [ ] Clinical audit reports

## Configuration

```yaml
# application.yml
audit:
  enabled: true
  kafka:
    topic: audit-events
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
  async:
    core-pool-size: 4
    max-pool-size: 16
    queue-capacity: 10000
  encryption:
    enabled: true
    key-id: ${AUDIT_ENCRYPTION_KEY_ID}
  retention:
    days: 2555  # 7 years for HIPAA
```

## Benefits

✅ **Zero Performance Impact**: Async processing doesn't slow evaluations
✅ **Complete Transparency**: Every decision is explainable
✅ **HIPAA Compliant**: PHI encryption, 7-year retention, access logs
✅ **Scalable**: Handles millions of events per day
✅ **Queryable**: Fast access to audit trails
✅ **Tamper-Evident**: Cryptographic integrity chains
✅ **Multi-Tenant**: Isolated audit trails per tenant
