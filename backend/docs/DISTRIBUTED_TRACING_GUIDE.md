# Distributed Tracing Architecture Guide

**HDIM Platform - OpenTelemetry Implementation**

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Trace Propagation Mechanisms](#trace-propagation-mechanisms)
4. [Configuration](#configuration)
5. [Sampling Strategies](#sampling-strategies)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

---

## Overview

HDIM implements distributed tracing using **OpenTelemetry** to provide end-to-end request visibility across 34 microservices. Tracing enables:

- **Request correlation** - Link all service calls from a single user request
- **Performance analysis** - Identify bottlenecks and slow operations
- **Error tracking** - Trace error propagation across service boundaries
- **Dependency mapping** - Visualize service interaction patterns

### Key Technologies

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Instrumentation** | OpenTelemetry Java Agent | Auto-instrumentation of HTTP, database, messaging |
| **Propagation** | W3C Trace Context + B3 | Standard trace context headers |
| **Export** | OTLP HTTP | Send traces to Jaeger collector |
| **Backend** | Jaeger | Trace storage, search, and visualization |

---

## Architecture

### System Diagram

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ HTTP Request (no trace context)
       ▼
┌─────────────────────────────────────────────────────────────┐
│  Gateway Service (trace-id: abc123, span-id: def456)        │
│  - Creates root span if no incoming trace context           │
│  - Injects trace context into all outbound requests         │
└──┬──────────────────────────────────────────────────────┬───┘
   │                                                       │
   │ HTTP (Feign)                                         │ Kafka Producer
   │ traceparent: 00-abc123-ghi789-01                     │ (trace headers in message)
   ▼                                                       ▼
┌──────────────────────┐                          ┌──────────────────────┐
│ Quality Measure      │                          │ Care Gap Identified  │
│ Service              │                          │ Event                │
│ (span-id: ghi789)    │                          └──────────────────────┘
└──┬───────────────┬───┘                                      │
   │               │                                          │ Kafka Consumer
   │               │                                          │ (extracts trace context)
   │ HTTP          │ Kafka                                    ▼
   │ (RestTemplate)│                              ┌──────────────────────┐
   ▼               ▼                              │ Care Gap Service     │
┌─────────┐   ┌─────────┐                        │ (span-id: stu901)    │
│ CQL     │   │ Measure │                        └──────────────────────┘
│ Engine  │   │ Event   │
│ (jkl012)│   │         │
└─────────┘   └─────────┘

All spans linked by trace-id: abc123
```

### Trace Hierarchy

```
Trace (trace-id: abc123)
├── Span: gateway-service (span-id: def456) [ROOT]
│   ├── Span: quality-measure-service (span-id: ghi789) [CHILD]
│   │   ├── Span: cql-engine-service (span-id: jkl012) [CHILD]
│   │   │   └── Span: DB Query (SELECT * FROM measures) [CHILD]
│   │   └── Span: Kafka Produce (measure.evaluated) [CHILD]
│   ├── Span: fhir-service (span-id: mno345) [CHILD]
│   │   └── Span: patient-service (span-id: pqr678) [CHILD]
│   └── Span: Kafka Produce (caregap.identified) [CHILD]
└── Span: care-gap-service (span-id: stu901) [LINKED via Kafka]
```

---

## Trace Propagation Mechanisms

### 1. HTTP Trace Propagation (Automatic)

#### Feign Clients

**Location:** `modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/feign/FeignTraceInterceptor.java`

**How It Works:**
- Auto-registered as Spring `@Component` when Feign + OpenTelemetry are on classpath
- Implements `feign.RequestInterceptor`
- Injects trace context headers on **all** Feign client calls

**Headers Injected:**
- `traceparent` - W3C Trace Context (e.g., `00-abc123-def456-01`)
- `tracestate` - Vendor-specific trace state
- `b3` - B3 single header (Zipkin compatibility)
- `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled` - B3 multi-header

**Example:**
```java
@FeignClient(name = "patient-service", url = "${feign.client.config.patient-service.url}")
public interface PatientClient {

    @GetMapping("/api/v1/patients/{patientId}")
    PatientResponse getPatient(@PathVariable String patientId);
    // Trace context automatically injected - no code changes needed!
}
```

**Configuration:**
```java
@Component
@ConditionalOnClass(name = {"feign.RequestInterceptor", "io.opentelemetry.api.OpenTelemetry"})
public class FeignTraceInterceptor implements RequestInterceptor {

    private final OpenTelemetry openTelemetry;

    @Override
    public void apply(RequestTemplate template) {
        Context currentContext = Context.current();
        openTelemetry.getPropagators()
                .getTextMapPropagator()
                .inject(currentContext, template, SETTER);
    }
}
```

#### RestTemplate

**Location:** `modules/shared/infrastructure/tracing/src/main/java/com/healthdata/tracing/RestTemplateTraceInterceptor.java`

**How It Works:**
- Auto-applied to **all** `RestTemplate` beans via `RestTemplateCustomizer`
- Implements `ClientHttpRequestInterceptor`
- Injects W3C Trace Context headers on outgoing HTTP requests

**Auto-Configuration:**
```java
@Bean
public RestTemplateCustomizer restTemplateTracingCustomizer(OpenTelemetry openTelemetry) {
    RestTemplateTraceInterceptor interceptor = new RestTemplateTraceInterceptor(openTelemetry);
    return restTemplate -> restTemplate.getInterceptors().add(interceptor);
}
```

**Example:**
```java
@Service
public class FhirService {

    private final RestTemplate restTemplate;

    public Bundle getFhirBundle(String patientId) {
        String url = "http://localhost:8085/fhir/Patient/" + patientId;
        return restTemplate.getForObject(url, Bundle.class);
        // Trace context automatically injected!
    }
}
```

### 2. Kafka Trace Propagation (Configured)

#### Producer Side

**Location:** `modules/shared/infrastructure/tracing/src/main/java/com/healthdata/tracing/KafkaProducerTraceInterceptor.java`

**How It Works:**
- Implements `ProducerInterceptor<String, String>`
- Injects trace context into Kafka message headers before sending
- Configured in `application.yml` per service

**Configuration:**
```yaml
spring:
  kafka:
    producer:
      properties:
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
```

**Implementation:**
```java
@Override
public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
    Context currentContext = Context.current();

    // Inject trace context into Kafka headers
    openTelemetry.getPropagators()
            .getTextMapPropagator()
            .inject(currentContext, record.headers(), SETTER);

    return record;
}
```

#### Consumer Side

**Location:** `modules/shared/infrastructure/tracing/src/main/java/com/healthdata/tracing/KafkaConsumerTraceInterceptor.java`

**How It Works:**
- Implements `ConsumerInterceptor<String, String>`
- Extracts trace context from Kafka message headers
- Links consumer span to producer trace

**Configuration:**
```yaml
spring:
  kafka:
    consumer:
      properties:
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

**Implementation:**
```java
@Override
public ConsumerRecords<String, String> onConsume(ConsumerRecords<String, String> records) {
    for (ConsumerRecord<String, String> record : records) {
        // Extract trace context from Kafka headers
        Context extractedContext = openTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.headers(), GETTER);

        // Set as current context for this message processing
        try (Scope scope = extractedContext.makeCurrent()) {
            // Processing happens with correct trace context
        }
    }
    return records;
}
```

### 3. Coverage Summary

| Transport | Propagation Method | Status | Services |
|-----------|-------------------|--------|----------|
| HTTP (Feign) | FeignTraceInterceptor | ✅ Auto | All (when Feign used) |
| HTTP (RestTemplate) | RestTemplateTraceInterceptor | ✅ Auto | All (when RestTemplate used) |
| Kafka (Producer) | KafkaProducerTraceInterceptor | ✅ Configured | 19 services |
| Kafka (Consumer) | KafkaConsumerTraceInterceptor | ✅ Configured | 19 services |

---

## Configuration

### Shared Tracing Module

**Location:** `modules/shared/infrastructure/tracing/`

All services that depend on this module automatically get:
- OpenTelemetry SDK
- OTLP HTTP exporter
- W3C Trace Context + B3 propagators
- Batch span processor
- Service resource attributes

**Auto-Configuration:**
```java
@Configuration
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingAutoConfiguration {

    @Bean
    public OpenTelemetry openTelemetry(
            @Value("${spring.application.name}") String serviceName,
            @Value("${tracing.url:http://localhost:4318/v1/traces}") String otlpEndpoint) {

        // Configure OTLP HTTP exporter
        OtlpHttpSpanExporter spanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .build();

        // Configure batch span processor
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
                .setMaxQueueSize(2048)
                .setScheduleDelay(Duration.ofMillis(5000))
                .setMaxExportBatchSize(512)
                .build();

        // Create OpenTelemetry instance
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder()
                        .addSpanProcessor(spanProcessor)
                        .setResource(Resource.create(Attributes.builder()
                                .put("service.name", serviceName)
                                .put("service.namespace", "hdim")
                                .build()))
                        .build())
                .setPropagators(ContextPropagators.create(
                        TextMapPropagator.composite(
                                W3CTraceContextPropagator.getInstance(),
                                B3Propagator.injectingMultiHeaders())))
                .build();
    }
}
```

### Service Configuration

Each service includes the tracing module:

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":modules:shared:infrastructure:tracing"))
}
```

### OTLP Exporter Endpoint

**Default:** `http://localhost:4318/v1/traces`

**Override via environment variable:**
```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4318/v1/traces
```

**Override in application.yml:**
```yaml
tracing:
  url: http://jaeger:4318/v1/traces
```

### Batch Span Processor Tuning

**Current Settings:**
```yaml
tracing:
  batch:
    max-queue-size: 2048                  # Span queue size
    schedule-delay-ms: 5000               # Export every 5 seconds
    max-export-batch-size: 512            # Spans per batch
```

**High-Traffic Tuning:**
```yaml
tracing:
  batch:
    max-queue-size: 4096                  # Larger queue for bursts
    schedule-delay-ms: 2000               # More frequent export
    max-export-batch-size: 1024           # Larger batches
```

---

## Sampling Strategies

### Environment-Specific Sampling

Sampling controls what percentage of traces are exported to Jaeger.

#### Development (100% Sampling)

**Goal:** Capture all traces for debugging

```yaml
# application.yml
---
spring:
  config:
    activate:
      on-profile: dev

management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling
```

**Use Case:**
- Local development
- Troubleshooting specific issues
- Integration testing

#### Staging (50% Sampling)

**Goal:** Balance between visibility and performance

```yaml
---
spring:
  config:
    activate:
      on-profile: staging

management:
  tracing:
    sampling:
      probability: 0.5  # 50% sampling
```

**Use Case:**
- Pre-production testing
- Load testing
- Performance analysis

#### Production (10% Sampling)

**Goal:** Reduce overhead, maintain representative sample

```yaml
---
spring:
  config:
    activate:
      on-profile: prod

management:
  tracing:
    sampling:
      probability: 0.1  # 10% sampling
```

**Use Case:**
- Production monitoring
- Cost optimization
- Representative performance data

### Activating Profiles

**Via environment variable:**
```bash
export SPRING_PROFILES_ACTIVE=prod
```

**Via Docker Compose:**
```yaml
services:
  quality-measure-service:
    environment:
      - SPRING_PROFILES_ACTIVE=prod
```

**Via command line:**
```bash
java -jar quality-measure-service.jar --spring.profiles.active=prod
```

### Adaptive Sampling (Future Enhancement)

Consider implementing error-based sampling:
```yaml
management:
  tracing:
    sampling:
      # Sample 100% of errors, 10% of successful requests
      error-probability: 1.0
      success-probability: 0.1
```

---

## Troubleshooting

### Trace Not Appearing in Jaeger

**Symptom:** Request completes but no trace shows up in Jaeger

**Possible Causes:**

1. **Sampling Disabled**
   ```yaml
   # Check sampling probability
   management:
     tracing:
       sampling:
         probability: 1.0  # Ensure not 0.0
   ```

2. **OTLP Exporter Unreachable**
   ```bash
   # Check Jaeger is running
   docker ps | grep jaeger

   # Check network connectivity
   curl http://localhost:4318/v1/traces
   ```

3. **Trace Context Not Propagated**
   ```java
   // Verify interceptor is registered
   // Check logs for: "Configuring RestTemplate trace propagation interceptor"
   ```

4. **Span Not Exported (Batch Delay)**
   ```yaml
   # Reduce batch delay for faster export
   tracing:
     batch:
       schedule-delay-ms: 1000  # Export every 1 second
   ```

### Incomplete Traces (Missing Spans)

**Symptom:** Trace shows up but some service calls are missing

**Possible Causes:**

1. **Service Not Configured for Tracing**
   ```kotlin
   // Verify dependency in build.gradle.kts
   implementation(project(":modules:shared:infrastructure:tracing"))
   ```

2. **Kafka Interceptor Not Configured**
   ```yaml
   # Check application.yml
   spring:
     kafka:
       producer:
         properties:
           interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
   ```

3. **HTTP Client Not Using Interceptor**
   ```java
   // Use @Autowired RestTemplate (has interceptor)
   // NOT: new RestTemplate() (no interceptor)
   ```

### High Trace Volume Impact

**Symptom:** Performance degradation with 100% sampling

**Solutions:**

1. **Reduce Sampling Rate**
   ```yaml
   management:
     tracing:
       sampling:
         probability: 0.1  # 10% sampling
   ```

2. **Increase Batch Size**
   ```yaml
   tracing:
     batch:
       max-export-batch-size: 1024  # Larger batches
   ```

3. **Increase Export Interval**
   ```yaml
   tracing:
     batch:
       schedule-delay-ms: 10000  # Export every 10 seconds
   ```

### Trace Context Lost Across Async Operations

**Symptom:** Spans created in async methods not linked to parent trace

**Solution:** Propagate context manually
```java
@Async
public CompletableFuture<Result> asyncOperation() {
    Context currentContext = Context.current();

    return CompletableFuture.supplyAsync(() -> {
        try (Scope scope = currentContext.makeCurrent()) {
            // Execute with correct trace context
            return performOperation();
        }
    });
}
```

---

## Best Practices

### 1. Use Auto-Configuration

**DO:**
```java
@Autowired
private RestTemplate restTemplate;  // Has trace interceptor

@Autowired
private PatientClient patientClient;  // Has trace interceptor
```

**DON'T:**
```java
RestTemplate restTemplate = new RestTemplate();  // No trace interceptor!
```

### 2. Add Custom Spans for Business Operations

```java
@Service
public class QualityMeasureService {

    private final Tracer tracer;

    public EvaluationResult evaluateMeasure(String measureId, String patientId) {
        Span span = tracer.spanBuilder("evaluate_measure")
                .setAttribute("measure.id", measureId)
                .setAttribute("patient.id", patientId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Business logic here
            EvaluationResult result = performEvaluation(measureId, patientId);

            span.setAttribute("result.score", result.getScore());
            span.setStatus(StatusCode.OK);
            return result;

        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;

        } finally {
            span.end();
        }
    }
}
```

### 3. Use Meaningful Span Names

**DO:**
```java
Span span = tracer.spanBuilder("evaluate_hedis_cbp")
        .setAttribute("measure.type", "CBP")
        .startSpan();
```

**DON'T:**
```java
Span span = tracer.spanBuilder("process")  // Too generic!
        .startSpan();
```

### 4. Add Context Attributes

```java
span.setAttribute("tenant.id", tenantId);
span.setAttribute("user.id", userId);
span.setAttribute("measure.numerator", numeratorCount);
span.setAttribute("measure.denominator", denominatorCount);
```

### 5. Record Exceptions

```java
try {
    // Operation
} catch (Exception e) {
    span.recordException(e);
    span.setStatus(StatusCode.ERROR, e.getMessage());
    throw e;
}
```

### 6. Use Async-Safe Context Propagation

```java
@Async
public CompletableFuture<Result> asyncTask() {
    Context context = Context.current();  // Capture before async

    return CompletableFuture.supplyAsync(() -> {
        try (Scope scope = context.makeCurrent()) {  // Restore in async
            return executeTask();
        }
    });
}
```

### 7. Monitor Trace Volume

**Set up alerts for high cardinality:**
```yaml
# Example alert rule (Prometheus)
- alert: HighTraceVolume
  expr: rate(traces_total[5m]) > 1000
  annotations:
    summary: "High trace volume detected"
```

### 8. Use Sampling Wisely

**Traffic-based sampling:**
- Development: 100% (full visibility)
- Staging: 50% (representative sample)
- Production: 10% (cost-effective monitoring)

**Error-based sampling (future):**
- Errors: 100% (always capture failures)
- Success: 10% (representative sample)

---

## Integration with Jaeger

### Docker Compose Setup

```yaml
services:
  jaeger:
    image: jaegertracing/all-in-one:1.50
    ports:
      - "16686:16686"  # Jaeger UI
      - "4318:4318"    # OTLP HTTP receiver
    environment:
      - COLLECTOR_OTLP_ENABLED=true
      - SPAN_STORAGE_TYPE=badger
      - BADGER_EPHEMERAL=false
      - BADGER_DIRECTORY_VALUE=/badger/data
      - BADGER_DIRECTORY_KEY=/badger/key
    volumes:
      - jaeger-badger:/badger
```

### Accessing Jaeger UI

**URL:** `http://localhost:16686`

**Features:**
- Search traces by service, operation, tags
- View trace timeline and span details
- Analyze service dependencies
- Compare trace performance

### Query Examples

**Find slow requests:**
```
service=quality-measure-service
duration>5s
```

**Find errors:**
```
service=fhir-service
error=true
```

**Find specific patient operations:**
```
patient.id=PATIENT123
```

---

## Further Reading

- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/instrumentation/java/)
- [W3C Trace Context Specification](https://www.w3.org/TR/trace-context/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)

---

**Last Updated:** January 11, 2026
**Status:** Production-Ready
**Coverage:** 34/34 services (100%)
