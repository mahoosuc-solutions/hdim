# Sprint 3 "Observe" — Observability + IHE Profiles Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add full OpenTelemetry tracing, Prometheus metrics, structured ECS logging, and ATNA ARR forwarding to all 3 adapter services, plus scaffold the ihe-gateway-service with IHE PIXv3/PDQv3 REST clients and XDS.b document exchange stubs.

**Architecture:** Each adapter gains custom OTel spans (wrapping external REST calls, Kafka produce/consume, and PHI de-identification), a Prometheus metrics endpoint via the existing shared `metrics` module, and structured JSON logging with traceId/spanId/correlationId. A new `ihe-gateway-service` (port 8125) is created as a Spring Boot service implementing IHE document exchange via FHIR MHD (REST-based fallback for SOAP XDS.b). The healthix-adapter gains a PIXv3 REST client for patient cross-referencing and an XDS.b document source capability.

**Tech Stack:** Java 21, Spring Boot 3.x, OpenTelemetry SDK 1.38.0, Micrometer 1.13.6, Prometheus, Logback ECS encoder, HAPI FHIR 7.6.0, Resilience4j, JUnit 5, Mockito, AssertJ

**References:**
- Design doc: `docs/plans/2026-03-06-v3.0.0-rc1-shield-design.md` (Sprint 3, section 3)
- Sprint 1 commit: `d7454292a` (security hardening)
- Sprint 2 commit: `71f18dbf2` (test coverage)
- Shared tracing module: `modules/shared/infrastructure/tracing/` (OTel SDK, RestTemplate/Kafka interceptors)
- Shared metrics module: `modules/shared/infrastructure/metrics/` (Micrometer, Prometheus, HealthcareMetrics)
- Version catalog: `backend/gradle/libs.versions.toml`

**Conventions:**
- Package pattern: `com.healthdata.{servicenameflat}` (e.g., `corehiveadapter`, `ihegateway`)
- Base paths: `backend/modules/services/{service-name}/src/`
- Test tags: `@Tag("unit")` for unit tests, `@Tag("integration")` for integration tests
- Unit tests: `./gradlew :modules:services:{service}:test`
- Existing shared modules provide `TracingAutoConfiguration` (auto-wires OTel Tracer bean) and `MetricsAutoConfiguration` (auto-wires MeterRegistry + HealthcareMetrics)

---

## Task 1: Add tracing and metrics dependencies to all 3 adapters

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/build.gradle.kts`
- Modify: `backend/modules/services/healthix-adapter-service/build.gradle.kts`
- Modify: `backend/modules/services/hedis-adapter-service/build.gradle.kts`

**Step 1: Add shared module dependencies to each adapter's build.gradle.kts**

Add these two lines to the `dependencies` block of each adapter:

```kotlin
implementation(project(":modules:shared:infrastructure:tracing"))
implementation(project(":modules:shared:infrastructure:metrics"))
```

For **corehive-adapter-service** `build.gradle.kts`, add after the existing `implementation(project(":modules:shared:infrastructure:feature-flags"))` line:

```kotlin
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:metrics"))
```

For **healthix-adapter-service** `build.gradle.kts`, add after `implementation(project(":modules:shared:infrastructure:feature-flags"))`:

```kotlin
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:metrics"))
```

For **hedis-adapter-service** `build.gradle.kts`, add after `implementation(project(":modules:shared:infrastructure:feature-flags"))`:

```kotlin
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:metrics"))
```

**Step 2: Verify compilation**

Run:
```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:compileJava :modules:services:healthix-adapter-service:compileJava :modules:services:hedis-adapter-service:compileJava --no-daemon
```
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/build.gradle.kts \
        backend/modules/services/healthix-adapter-service/build.gradle.kts \
        backend/modules/services/hedis-adapter-service/build.gradle.kts
git commit -m "feat(adapters): add shared tracing and metrics module dependencies"
```

---

## Task 2: Add OpenTelemetry custom spans to corehive-adapter-service

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/observability/AdapterSpanHelper.java`
- Modify: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/client/CorehiveApiClient.java`
- Modify: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/service/CorehiveAdapterService.java`
- Modify: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/event/CareGapEventListener.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/observability/AdapterSpanHelperTest.java`

**Step 1: Create AdapterSpanHelper — a thin wrapper for creating adapter-specific spans**

```java
package com.healthdata.corehiveadapter.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class AdapterSpanHelper {

    private final Tracer tracer;

    /**
     * Execute a supplier within a named span, recording success/failure.
     */
    public <T> T traced(String spanName, Supplier<T> operation, String... attributes) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        for (int i = 0; i < attributes.length - 1; i += 2) {
            span.setAttribute(attributes[i], attributes[i + 1]);
        }
        try (var scope = span.makeCurrent()) {
            T result = operation.get();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Execute a runnable within a named span.
     */
    public void tracedRun(String spanName, Runnable operation, String... attributes) {
        traced(spanName, () -> { operation.run(); return null; }, attributes);
    }
}
```

**Step 2: Write the test for AdapterSpanHelper**

```java
package com.healthdata.corehiveadapter.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AdapterSpanHelperTest {

    @Mock private Tracer tracer;
    @Mock private SpanBuilder spanBuilder;
    @Mock private Span span;
    @Mock private Scope scope;

    private AdapterSpanHelper helper;

    @BeforeEach
    void setUp() {
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(scope);
        when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        helper = new AdapterSpanHelper(tracer);
    }

    @Test
    void traced_successfulOperation_setsOkStatus() {
        String result = helper.traced("test-span", () -> "hello",
                "adapter", "corehive");

        assertThat(result).isEqualTo("hello");
        verify(tracer).spanBuilder("test-span");
        verify(span).setAttribute("adapter", "corehive");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
        verify(scope).close();
    }

    @Test
    void traced_failedOperation_setsErrorStatusAndRecordsException() {
        RuntimeException ex = new RuntimeException("boom");

        assertThatThrownBy(() ->
            helper.traced("fail-span", () -> { throw ex; })
        ).isSameAs(ex);

        verify(span).setStatus(StatusCode.ERROR, "boom");
        verify(span).recordException(ex);
        verify(span).end();
    }

    @Test
    void tracedRun_successfulRunnable_setsOkStatus() {
        Runnable op = mock(Runnable.class);

        helper.tracedRun("run-span", op, "phi.level", "NONE");

        verify(op).run();
        verify(span).setAttribute("phi.level", "NONE");
        verify(span).setStatus(StatusCode.OK);
        verify(span).end();
    }
}
```

**Step 3: Run tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:test --tests "*AdapterSpanHelperTest" --no-daemon
```
Expected: 3 tests PASSED

**Step 4: Add spans to CorehiveApiClient**

Modify `CorehiveApiClient.java` to inject `AdapterSpanHelper` and wrap each REST call:

The constructor becomes:
```java
    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final AdapterSpanHelper spanHelper;

    public CorehiveApiClient(
            @Qualifier("corehiveRestTemplate") RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("corehive-api");
        this.spanHelper = spanHelper;
    }
```

The `scoreCareGaps` method becomes:
```java
    public CareGapScoringResponse scoreCareGaps(CareGapScoringRequest request) {
        return spanHelper.traced("corehive.score_care_gaps",
                () -> {
                    Supplier<CareGapScoringResponse> supplier = CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            () -> restTemplate.postForObject(
                                    "/api/healthcare/score",
                                    request,
                                    CareGapScoringResponse.class));
                    return supplier.get();
                },
                "adapter", "corehive",
                "operation", "score_care_gaps");
    }
```

The `calculateRoi` method becomes:
```java
    public VbcRoiResponse calculateRoi(VbcRoiRequest request) {
        return spanHelper.traced("corehive.calculate_roi",
                () -> {
                    Supplier<VbcRoiResponse> supplier = CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            () -> restTemplate.postForObject(
                                    "/api/healthcare/roi",
                                    request,
                                    VbcRoiResponse.class));
                    return supplier.get();
                },
                "adapter", "corehive",
                "operation", "calculate_roi");
    }
```

Add import:
```java
import com.healthdata.corehiveadapter.observability.AdapterSpanHelper;
```

**Step 5: Add span to CorehiveAdapterService.validateNoPhiInRequest**

In `CorehiveAdapterService.java`, inject `AdapterSpanHelper`:

Add field and update constructor (Lombok `@RequiredArgsConstructor` handles this — just add the field):
```java
    private final AdapterSpanHelper spanHelper;
```

Wrap `scoreCareGaps` method body:
```java
    public CareGapScoringResponse scoreCareGaps(CareGapScoringRequest request, String tenantId) {
        return spanHelper.traced("corehive.adapter.score_care_gaps",
                () -> {
                    log.info("Scoring care gaps for tenant={}, gapCount={}",
                            tenantId, request.getCareGaps().size());

                    validateNoPhiInRequest(request);

                    CareGapScoringResponse response = apiClient.scoreCareGaps(request);

                    ExternalEventEnvelope<CareGapScoringResponse> envelope = ExternalEventEnvelope.of(
                            "external.corehive.decisions.scored",
                            "corehive-adapter-service",
                            tenantId,
                            response,
                            ExternalEventMetadata.builder()
                                    .sourceSystem(SourceSystem.COREHIVE)
                                    .phiLevel(PhiLevel.NONE)
                                    .build());

                    kafkaTemplate.send(TOPIC_DECISIONS, tenantId, envelope);
                    log.info("Published scoring result to {}", TOPIC_DECISIONS);

                    return response;
                },
                "adapter", "corehive",
                "tenant.id", tenantId,
                "phi.level", "NONE");
    }
```

Wrap `calculateRoi` similarly:
```java
    public VbcRoiResponse calculateRoi(VbcRoiRequest request, String tenantId) {
        return spanHelper.traced("corehive.adapter.calculate_roi",
                () -> {
                    log.info("Calculating VBC ROI for tenant={}, contract={}",
                            tenantId, request.getContractId());

                    VbcRoiResponse response = apiClient.calculateRoi(request);

                    ExternalEventEnvelope<VbcRoiResponse> envelope = ExternalEventEnvelope.of(
                            "external.corehive.roi.calculated",
                            "corehive-adapter-service",
                            tenantId,
                            response,
                            ExternalEventMetadata.builder()
                                    .sourceSystem(SourceSystem.COREHIVE)
                                    .phiLevel(PhiLevel.NONE)
                                    .build());

                    kafkaTemplate.send(TOPIC_ROI, tenantId, envelope);

                    return response;
                },
                "adapter", "corehive",
                "tenant.id", tenantId);
    }
```

Add import:
```java
import com.healthdata.corehiveadapter.observability.AdapterSpanHelper;
```

**Step 6: Add span to CareGapEventListener**

Read `CareGapEventListener.java` to understand its current structure, then inject `AdapterSpanHelper` and wrap the `@KafkaListener` method body with:
```java
spanHelper.tracedRun("corehive.kafka.care_gap_received", () -> { /* existing body */ },
        "adapter", "corehive", "kafka.topic", "external.hdim.caregaps");
```

**Step 7: Run all corehive tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:test --no-daemon --rerun
```
Expected: All tests PASSED (existing tests + new AdapterSpanHelperTest)

**Step 8: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/
git commit -m "feat(corehive-adapter): add OpenTelemetry custom spans for REST calls, Kafka, and PHI validation"
```

---

## Task 3: Add OpenTelemetry custom spans to healthix-adapter-service

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/observability/AdapterSpanHelper.java`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/fhir/FhirSubscriptionClient.java`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/ccda/CcdaIngestionService.java`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/mpi/VeratoMpiProxy.java`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/hl7/Hl7AdtConsumer.java`
- Test: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/observability/AdapterSpanHelperTest.java`

**Step 1: Copy AdapterSpanHelper**

Create an identical `AdapterSpanHelper.java` in healthix-adapter's `observability` package (same code as Task 2 Step 1, different package: `com.healthdata.healthixadapter.observability`).

**Step 2: Copy AdapterSpanHelperTest**

Create identical test in healthix-adapter's `observability` package (same code as Task 2 Step 2, different package: `com.healthdata.healthixadapter.observability`).

**Step 3: Add spans to FhirSubscriptionClient**

Inject `AdapterSpanHelper`, wrap `handleNotification` with:
```java
spanHelper.tracedRun("healthix.fhir.notification_received", () -> { /* existing */ },
        "adapter", "healthix", "resource.type", resourceType, "phi.level", "FULL");
```

Wrap `registerSubscription` with:
```java
spanHelper.traced("healthix.fhir.register_subscription", () -> { /* existing */ },
        "adapter", "healthix", "operation", "register_subscription");
```

**Step 4: Add spans to CcdaIngestionService**

Inject `AdapterSpanHelper`, wrap `fetchAndIngestDocument` with:
```java
spanHelper.tracedRun("healthix.ccda.fetch_and_ingest", () -> { /* existing */ },
        "adapter", "healthix", "operation", "ccda_ingestion", "phi.level", "FULL");
```

Wrap `handleCcdaWebhook` with:
```java
spanHelper.tracedRun("healthix.ccda.webhook_received", () -> { /* existing */ },
        "adapter", "healthix", "operation", "ccda_webhook");
```

**Step 5: Add spans to VeratoMpiProxy**

Inject `AdapterSpanHelper`, wrap `queryMpiMatch` with:
```java
spanHelper.traced("healthix.mpi.query_match", () -> { /* existing */ },
        "adapter", "healthix", "operation", "mpi_query", "phi.level", "FULL");
```

**Step 6: Add span to Hl7AdtConsumer**

Inject `AdapterSpanHelper`, wrap the `@KafkaListener` method body with:
```java
spanHelper.tracedRun("healthix.hl7.adt_received", () -> { /* existing */ },
        "adapter", "healthix", "kafka.topic", "external.healthix.hl7", "phi.level", "FULL");
```

**Step 7: Run all healthix tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:healthix-adapter-service:test --no-daemon --rerun
```
Expected: All tests PASSED

**Step 8: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/
git commit -m "feat(healthix-adapter): add OpenTelemetry custom spans for FHIR, C-CDA, MPI, and HL7 operations"
```

---

## Task 4: Add OpenTelemetry custom spans to hedis-adapter-service

**Files:**
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/observability/AdapterSpanHelper.java`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/measures/MeasureRegistrySyncService.java`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/measures/CqlDelegationService.java`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/crm/CrmSyncService.java`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/websocket/KafkaToWebSocketBridge.java`
- Test: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/observability/AdapterSpanHelperTest.java`

**Step 1: Copy AdapterSpanHelper**

Create identical `AdapterSpanHelper.java` in hedis-adapter's `observability` package (`com.healthdata.hedisadapter.observability`).

**Step 2: Copy AdapterSpanHelperTest**

Create identical test (`com.healthdata.hedisadapter.observability`).

**Step 3: Add spans to MeasureRegistrySyncService**

Inject `AdapterSpanHelper`, wrap `syncMeasures` with:
```java
spanHelper.tracedRun("hedis.measures.sync_registry", () -> { /* existing */ },
        "adapter", "hedis", "operation", "measure_sync", "phi.level", "LIMITED");
```

**Step 4: Add spans to CqlDelegationService**

Wrap `delegateCalculation` with:
```java
spanHelper.traced("hedis.cql.delegate_calculation", () -> { /* existing */ },
        "adapter", "hedis", "operation", "cql_delegation", "measure.type", request.getMeasureType());
```

**Step 5: Add spans to CrmSyncService**

Wrap `pushDealUpdate` with:
```java
spanHelper.tracedRun("hedis.crm.push_deal", () -> { /* existing */ },
        "adapter", "hedis", "operation", "crm_push");
```

Wrap `handleWebhook` with:
```java
spanHelper.tracedRun("hedis.crm.webhook_received", () -> { /* existing */ },
        "adapter", "hedis", "operation", "crm_webhook");
```

**Step 6: Add span to KafkaToWebSocketBridge**

Wrap the Kafka listener method with:
```java
spanHelper.tracedRun("hedis.websocket.kafka_bridge", () -> { /* existing */ },
        "adapter", "hedis", "kafka.topic", topic);
```

**Step 7: Run all hedis tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:hedis-adapter-service:test --no-daemon --rerun
```
Expected: All tests PASSED

**Step 8: Commit**

```bash
git add backend/modules/services/hedis-adapter-service/src/
git commit -m "feat(hedis-adapter): add OpenTelemetry custom spans for measures, CQL, CRM, and WebSocket operations"
```

---

## Task 5: Add adapter-specific Prometheus metrics to all 3 adapters

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/observability/AdapterMetrics.java`
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/observability/AdapterMetrics.java`
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/observability/AdapterMetrics.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/observability/AdapterMetricsTest.java`
- Test: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/observability/AdapterMetricsTest.java`
- Test: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/observability/AdapterMetricsTest.java`

**Step 1: Create AdapterMetrics for corehive-adapter**

```java
package com.healthdata.corehiveadapter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AdapterMetrics {

    private final Counter scoringRequests;
    private final Counter scoringErrors;
    private final Counter roiRequests;
    private final Counter phiBlockedRequests;
    private final Timer scoringLatency;
    private final Timer roiLatency;

    public AdapterMetrics(MeterRegistry registry) {
        this.scoringRequests = Counter.builder("hdim.adapter.corehive.scoring.requests.total")
                .description("Total care gap scoring requests to CoreHive AI")
                .tag("adapter", "corehive")
                .register(registry);
        this.scoringErrors = Counter.builder("hdim.adapter.corehive.scoring.errors.total")
                .description("Failed scoring requests")
                .tag("adapter", "corehive")
                .register(registry);
        this.roiRequests = Counter.builder("hdim.adapter.corehive.roi.requests.total")
                .description("Total VBC ROI calculation requests")
                .tag("adapter", "corehive")
                .register(registry);
        this.phiBlockedRequests = Counter.builder("hdim.adapter.corehive.phi.blocked.total")
                .description("Requests blocked due to PHI detection")
                .tag("adapter", "corehive")
                .register(registry);
        this.scoringLatency = Timer.builder("hdim.adapter.corehive.scoring.latency")
                .description("Scoring request latency")
                .tag("adapter", "corehive")
                .register(registry);
        this.roiLatency = Timer.builder("hdim.adapter.corehive.roi.latency")
                .description("ROI calculation latency")
                .tag("adapter", "corehive")
                .register(registry);
    }

    public void recordScoringRequest() { scoringRequests.increment(); }
    public void recordScoringError() { scoringErrors.increment(); }
    public void recordRoiRequest() { roiRequests.increment(); }
    public void recordPhiBlocked() { phiBlockedRequests.increment(); }
    public void recordScoringLatency(Duration duration) { scoringLatency.record(duration); }
    public void recordRoiLatency(Duration duration) { roiLatency.record(duration); }
}
```

**Step 2: Create AdapterMetrics for healthix-adapter**

```java
package com.healthdata.healthixadapter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AdapterMetrics {

    private final Counter fhirNotifications;
    private final Counter ccdaDocuments;
    private final Counter mpiQueries;
    private final Counter hl7Messages;
    private final Counter hl7Errors;
    private final Timer fhirLatency;
    private final Timer mpiLatency;
    private final Timer ccdaLatency;

    public AdapterMetrics(MeterRegistry registry) {
        this.fhirNotifications = Counter.builder("hdim.adapter.healthix.fhir.notifications.total")
                .description("FHIR subscription notifications received")
                .tag("adapter", "healthix")
                .register(registry);
        this.ccdaDocuments = Counter.builder("hdim.adapter.healthix.ccda.documents.total")
                .description("C-CDA documents ingested")
                .tag("adapter", "healthix")
                .register(registry);
        this.mpiQueries = Counter.builder("hdim.adapter.healthix.mpi.queries.total")
                .description("MPI cross-reference queries")
                .tag("adapter", "healthix")
                .register(registry);
        this.hl7Messages = Counter.builder("hdim.adapter.healthix.hl7.messages.total")
                .description("HL7 v2.5 ADT messages consumed")
                .tag("adapter", "healthix")
                .register(registry);
        this.hl7Errors = Counter.builder("hdim.adapter.healthix.hl7.errors.total")
                .description("HL7 processing errors")
                .tag("adapter", "healthix")
                .register(registry);
        this.fhirLatency = Timer.builder("hdim.adapter.healthix.fhir.latency")
                .description("FHIR operation latency")
                .tag("adapter", "healthix")
                .register(registry);
        this.mpiLatency = Timer.builder("hdim.adapter.healthix.mpi.latency")
                .description("MPI query latency")
                .tag("adapter", "healthix")
                .register(registry);
        this.ccdaLatency = Timer.builder("hdim.adapter.healthix.ccda.latency")
                .description("C-CDA ingestion latency")
                .tag("adapter", "healthix")
                .register(registry);
    }

    public void recordFhirNotification() { fhirNotifications.increment(); }
    public void recordCcdaDocument() { ccdaDocuments.increment(); }
    public void recordMpiQuery() { mpiQueries.increment(); }
    public void recordHl7Message() { hl7Messages.increment(); }
    public void recordHl7Error() { hl7Errors.increment(); }
    public void recordFhirLatency(Duration duration) { fhirLatency.record(duration); }
    public void recordMpiLatency(Duration duration) { mpiLatency.record(duration); }
    public void recordCcdaLatency(Duration duration) { ccdaLatency.record(duration); }
}
```

**Step 3: Create AdapterMetrics for hedis-adapter**

```java
package com.healthdata.hedisadapter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AdapterMetrics {

    private final Counter measuresSynced;
    private final Counter cqlDelegations;
    private final Counter crmWebhooks;
    private final Counter websocketMessages;
    private final AtomicLong activeWebsocketConnections;
    private final Timer measureSyncLatency;
    private final Timer cqlLatency;

    public AdapterMetrics(MeterRegistry registry) {
        this.measuresSynced = Counter.builder("hdim.adapter.hedis.measures.synced.total")
                .description("Measure definitions synced from hedis")
                .tag("adapter", "hedis")
                .register(registry);
        this.cqlDelegations = Counter.builder("hdim.adapter.hedis.cql.delegations.total")
                .description("CQL calculation delegations")
                .tag("adapter", "hedis")
                .register(registry);
        this.crmWebhooks = Counter.builder("hdim.adapter.hedis.crm.webhooks.total")
                .description("CRM webhooks received")
                .tag("adapter", "hedis")
                .register(registry);
        this.websocketMessages = Counter.builder("hdim.adapter.hedis.websocket.messages.total")
                .description("WebSocket messages sent to dashboard")
                .tag("adapter", "hedis")
                .register(registry);
        this.activeWebsocketConnections = new AtomicLong(0);
        Gauge.builder("hdim.adapter.hedis.websocket.connections.active", activeWebsocketConnections, AtomicLong::get)
                .description("Active WebSocket connections")
                .tag("adapter", "hedis")
                .register(registry);
        this.measureSyncLatency = Timer.builder("hdim.adapter.hedis.measures.sync.latency")
                .description("Measure sync latency")
                .tag("adapter", "hedis")
                .register(registry);
        this.cqlLatency = Timer.builder("hdim.adapter.hedis.cql.latency")
                .description("CQL delegation latency")
                .tag("adapter", "hedis")
                .register(registry);
    }

    public void recordMeasuresSynced() { measuresSynced.increment(); }
    public void recordCqlDelegation() { cqlDelegations.increment(); }
    public void recordCrmWebhook() { crmWebhooks.increment(); }
    public void recordWebsocketMessage() { websocketMessages.increment(); }
    public void setActiveWebsocketConnections(long count) { activeWebsocketConnections.set(count); }
    public void recordMeasureSyncLatency(Duration duration) { measureSyncLatency.record(duration); }
    public void recordCqlLatency(Duration duration) { cqlLatency.record(duration); }
}
```

**Step 4: Write tests for all 3 AdapterMetrics**

Each test verifies counters increment and timers record. Example for corehive:

```java
package com.healthdata.corehiveadapter.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AdapterMetricsTest {

    private MeterRegistry registry;
    private AdapterMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AdapterMetrics(registry);
    }

    @Test
    void recordScoringRequest_incrementsCounter() {
        metrics.recordScoringRequest();
        metrics.recordScoringRequest();

        assertThat(registry.counter("hdim.adapter.corehive.scoring.requests.total",
                "adapter", "corehive").count()).isEqualTo(2.0);
    }

    @Test
    void recordPhiBlocked_incrementsCounter() {
        metrics.recordPhiBlocked();

        assertThat(registry.counter("hdim.adapter.corehive.phi.blocked.total",
                "adapter", "corehive").count()).isEqualTo(1.0);
    }

    @Test
    void recordScoringLatency_recordsDuration() {
        metrics.recordScoringLatency(Duration.ofMillis(150));

        assertThat(registry.timer("hdim.adapter.corehive.scoring.latency",
                "adapter", "corehive").count()).isEqualTo(1);
    }
}
```

Create similar tests for healthix (`fhirNotifications`, `mpiQueries`, `hl7Messages`) and hedis (`measuresSynced`, `cqlDelegations`, `activeWebsocketConnections` gauge).

For hedis, include a gauge test:
```java
    @Test
    void setActiveWebsocketConnections_updatesGauge() {
        metrics.setActiveWebsocketConnections(42);

        assertThat(registry.find("hdim.adapter.hedis.websocket.connections.active")
                .gauge().value()).isEqualTo(42.0);
    }
```

**Step 5: Run all adapter tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:test :modules:services:healthix-adapter-service:test :modules:services:hedis-adapter-service:test --no-daemon --rerun
```
Expected: All tests PASSED

**Step 6: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/ \
        backend/modules/services/healthix-adapter-service/src/ \
        backend/modules/services/hedis-adapter-service/src/
git commit -m "feat(adapters): add Prometheus metrics — counters, timers, and gauges for all adapter operations"
```

---

## Task 6: Add structured ECS logging to all 3 adapters

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/src/main/resources/application.yml`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/resources/application.yml`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/resources/application.yml`
- Create: `backend/modules/services/corehive-adapter-service/src/main/resources/logback-spring.xml`
- Create: `backend/modules/services/healthix-adapter-service/src/main/resources/logback-spring.xml`
- Create: `backend/modules/services/hedis-adapter-service/src/main/resources/logback-spring.xml`

**Step 1: Create logback-spring.xml for corehive-adapter**

This configures JSON-structured logging with traceId, spanId, tenantId, and correlationId in ECS format:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="corehive-adapter-service"/>

    <!-- Console appender for development (human-readable) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [traceId=%X{traceId}] [spanId=%X{spanId}] [tenantId=%X{tenantId}] [correlationId=%X{correlationId}] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- JSON appender for production (ECS-compatible structured logging) -->
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="ch.qos.logback.contrib.json.classic.JsonLayout">
                <jsonFormatter class="ch.qos.logback.contrib.jackson.JacksonJsonFormatter">
                    <prettyPrint>false</prettyPrint>
                </jsonFormatter>
                <appendLineSeparator>true</appendLineSeparator>
                <includeContextName>false</includeContextName>
            </layout>
        </encoder>
    </appender>

    <!-- Development profile: human-readable -->
    <springProfile name="default,dev,test">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="com.healthdata.corehiveadapter" level="DEBUG"/>
    </springProfile>

    <!-- Docker/production profile: JSON structured -->
    <springProfile name="docker,prod,staging">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
        <logger name="com.healthdata.corehiveadapter" level="DEBUG"/>
    </springProfile>
</configuration>
```

**Note:** Full ECS JSON layout requires `logback-json-classic` and `logback-jackson` dependencies. However, the simpler approach for now is to use the pattern-based layout with MDC fields (traceId, spanId, etc.) which are automatically populated by the shared tracing module. The JSON appender definition is included for future activation but the default and docker profiles both use the CONSOLE appender with structured MDC fields.

**Step 2: Copy logback-spring.xml for healthix-adapter**

Same file but with `APP_NAME=healthix-adapter-service` and logger `com.healthdata.healthixadapter`.

**Step 3: Copy logback-spring.xml for hedis-adapter**

Same file but with `APP_NAME=hedis-adapter-service` and logger `com.healthdata.hedisadapter`.

**Step 4: Add tracing config to application.yml for all adapters**

Add to each adapter's `application.yml` (after the `logging:` section):

```yaml
tracing:
  enabled: true
  url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://jaeger:4318/v1/traces}

management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}
```

Note: The `management:` section already exists in each adapter's application.yml. Merge the `tracing:` subsection into the existing `management:` block. Add the top-level `tracing:` block as a new section.

**Step 5: Verify compilation**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:compileJava :modules:services:healthix-adapter-service:compileJava :modules:services:hedis-adapter-service:compileJava --no-daemon
```
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/resources/ \
        backend/modules/services/healthix-adapter-service/src/main/resources/ \
        backend/modules/services/hedis-adapter-service/src/main/resources/
git commit -m "feat(adapters): add structured ECS logging with traceId/spanId/correlationId and tracing config"
```

---

## Task 7: Add ATNA Audit Record Repository forwarding

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/audit/AtnaAuditService.java`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/audit/AtnaAuditService.java`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/audit/AtnaAuditService.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/audit/AtnaAuditArrForwardingTest.java`

**Step 1: Add Kafka-based ATNA ARR forwarding to AtnaAuditService**

Currently, `AtnaAuditService` only logs to stdout. Add Kafka forwarding to `ihe.audit.events` topic.

Modify each adapter's `AtnaAuditService` to accept an optional `KafkaTemplate` and forward audit events:

For corehive-adapter's `AtnaAuditService.java`, change the class to:

```java
package com.healthdata.corehiveadapter.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;

@Slf4j
public class AtnaAuditService {

    private static final String ATNA_AUDIT_TOPIC = "ihe.audit.events";

    private final String serviceName;
    private final String phiLevel;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AtnaAuditService(String serviceName) {
        this(serviceName, "NONE", null);
    }

    public AtnaAuditService(String serviceName, String phiLevel) {
        this(serviceName, phiLevel, null);
    }

    public AtnaAuditService(String serviceName, String phiLevel, KafkaTemplate<String, Object> kafkaTemplate) {
        this.serviceName = serviceName;
        this.phiLevel = phiLevel;
        this.kafkaTemplate = kafkaTemplate;
    }

    public AtnaAuditEvent buildAuditEvent(String tenantId, String eventType, String resourceType,
                                           String resourceId, String patientId,
                                           String correlationId, String status, String errorMessage) {
        return AtnaAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .sourceSystem(serviceName)
                .tenantId(tenantId)
                .eventType(eventType)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .patientId(patientId)
                .phiLevel(phiLevel)
                .correlationId(correlationId)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    public void logAudit(AtnaAuditEvent event) {
        if ("FAILURE".equals(event.getStatus())) {
            log.error("ATNA_AUDIT: {} | tenant={} | resource={}:{} | correlationId={} | status=FAILURE | error={}",
                    event.getEventType(), event.getTenantId(), event.getResourceType(),
                    event.getResourceId(), event.getCorrelationId(), event.getErrorMessage());
        } else {
            log.info("ATNA_AUDIT: {} | tenant={} | resource={}:{} | correlationId={} | status=SUCCESS",
                    event.getEventType(), event.getTenantId(), event.getResourceType(),
                    event.getResourceId(), event.getCorrelationId());
        }

        forwardToArr(event);
    }

    private void forwardToArr(AtnaAuditEvent event) {
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(ATNA_AUDIT_TOPIC, event.getTenantId(), event);
                log.debug("ATNA audit event forwarded to ARR topic: {}", ATNA_AUDIT_TOPIC);
            } catch (Exception e) {
                log.warn("Failed to forward ATNA audit event to ARR: {}", e.getMessage());
            }
        }
    }
}
```

**Step 2: Update AuditConfig in each adapter to pass KafkaTemplate**

Read each adapter's `AuditConfig.java` to see how `AtnaAuditService` is constructed. Update the `@Bean` method to inject `KafkaTemplate` and pass it to the constructor. For example:

```java
@Bean
public AtnaAuditService atnaAuditService(KafkaTemplate<String, Object> kafkaTemplate) {
    return new AtnaAuditService("corehive-adapter-service", "NONE", kafkaTemplate);
}
```

Apply the same pattern for healthix-adapter (phiLevel="FULL") and hedis-adapter (phiLevel="LIMITED").

**Step 3: Write test for ATNA ARR forwarding**

```java
package com.healthdata.corehiveadapter.audit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class AtnaAuditArrForwardingTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void logAudit_withKafkaTemplate_forwardsToArrTopic() {
        AtnaAuditService service = new AtnaAuditService("corehive-adapter-service", "NONE", kafkaTemplate);
        AtnaAuditEvent event = service.buildAuditEvent("tenant-1", "CARE_GAP_SCORED",
                "CareGap", "gap-123", null, "corr-1", "SUCCESS", null);

        service.logAudit(event);

        verify(kafkaTemplate).send(eq("ihe.audit.events"), eq("tenant-1"), any(AtnaAuditEvent.class));
    }

    @Test
    void logAudit_withoutKafkaTemplate_onlyLogs() {
        AtnaAuditService service = new AtnaAuditService("corehive-adapter-service", "NONE");
        AtnaAuditEvent event = service.buildAuditEvent("tenant-1", "CARE_GAP_SCORED",
                "CareGap", "gap-123", null, "corr-1", "SUCCESS", null);

        service.logAudit(event);

        verifyNoInteractions(kafkaTemplate);
    }
}
```

**Step 4: Run tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:test --tests "*AtnaAuditArrForwardingTest" --no-daemon
```
Expected: 2 tests PASSED

**Step 5: Apply same changes to healthix-adapter and hedis-adapter AtnaAuditService**

Copy the pattern (add `KafkaTemplate` parameter, `forwardToArr` method) to both. The code is identical except the package name.

**Step 6: Run all adapter tests**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:test :modules:services:healthix-adapter-service:test :modules:services:hedis-adapter-service:test --no-daemon --rerun
```
Expected: All tests PASSED

**Step 7: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/ \
        backend/modules/services/healthix-adapter-service/src/ \
        backend/modules/services/hedis-adapter-service/src/
git commit -m "feat(adapters): add ATNA Audit Record Repository forwarding via Kafka ihe.audit.events topic"
```

---

## Task 8: Scaffold ihe-gateway-service (port 8125)

**Files:**
- Create: `backend/modules/services/ihe-gateway-service/build.gradle.kts`
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/IheGatewayApplication.java`
- Create: `backend/modules/services/ihe-gateway-service/src/main/resources/application.yml`
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/config/IheGatewayProperties.java`
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/config/IheSecurityConfig.java`
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/health/IheGatewayHealthController.java`
- Create: `backend/modules/services/ihe-gateway-service/src/main/resources/db/changelog/db.changelog-master.xml`
- Create: `backend/modules/services/ihe-gateway-service/Dockerfile`
- Modify: `backend/settings.gradle.kts`
- Modify: `docker/postgres/init-multi-db.sh`
- Test: `backend/modules/services/ihe-gateway-service/src/test/java/com/healthdata/ihegateway/health/IheGatewayHealthControllerTest.java`

**Step 1: Create build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    java
}

dependencies {
    implementation(project(":modules:shared:domain:common"))
    implementation(project(":modules:shared:domain:fhir-models"))
    implementation(project(":modules:shared:infrastructure:persistence"))
    implementation(project(":modules:shared:infrastructure:database-config"))
    implementation(project(":modules:shared:infrastructure:audit"))
    implementation(project(":modules:shared:infrastructure:security"))
    implementation(project(":modules:shared:infrastructure:feature-flags"))
    implementation(project(":modules:shared:infrastructure:tracing"))
    implementation(project(":modules:shared:infrastructure:metrics"))
    implementation(libs.bundles.spring.boot.web)
    implementation(libs.bundles.spring.boot.data)
    implementation(libs.bundles.kafka)
    implementation(libs.bundles.resilience4j.common)
    implementation(libs.hapi.fhir.base)
    implementation(libs.hapi.fhir.structures.r4)
    implementation(libs.bundles.hapi.fhir.client)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.commons.lang3)
    implementation(libs.spring.boot.starter.security)
    testImplementation(project(":platform:test-fixtures"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.h2)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.kafka.test)
}

tasks.withType<Test> {
    val taskNames = gradle.startParameter.taskNames
    val isFullRun = taskNames.any { it.contains("testAll") || it.contains("testParallel") || it.contains("testIntegration") }
    useJUnitPlatform {
        if (!isFullRun) {
            excludeTags("integration", "e2e", "heavyweight", "slow", "contract")
        }
    }
    if (!isFullRun) {
        systemProperty("spring.profiles.active", "test")
    }
}
```

**Step 2: Create IheGatewayApplication.java**

```java
package com.healthdata.ihegateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.healthdata.ihegateway", "com.healthdata.common"})
public class IheGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(IheGatewayApplication.class, args);
    }
}
```

**Step 3: Create application.yml**

```yaml
server:
  port: 8125
  servlet:
    context-path: /ihe-gateway

spring:
  application:
    name: ihe-gateway-service

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9094}
    consumer:
      group-id: ihe-gateway
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.healthdata.*,java.util,java.lang"
        spring.json.value.default.type: "java.util.LinkedHashMap"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5435}/${DB_NAME:ihe_gateway_db}
    username: ${DB_USERNAME:healthdata}
    password: ${DB_PASSWORD:}
    hikari:
      maximum-pool-size: 15
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml

  flyway:
    enabled: false

external:
  ihe:
    enabled: ${IHE_GATEWAY_ENABLED:false}
    healthix-fhir-url: ${HEALTHIX_FHIR_URL:http://localhost:8080}
    healthix-mpi-url: ${HEALTHIX_MPI_URL:http://localhost:8000}
    healthix-document-url: ${HEALTHIX_DOCUMENT_URL:http://localhost:3010}
    timeout-ms: ${IHE_TIMEOUT_MS:10000}
    connect-timeout-ms: ${IHE_CONNECT_TIMEOUT_MS:5000}
    mtls:
      enabled: ${IHE_MTLS_ENABLED:false}
      keystore-path: ${IHE_MTLS_KEYSTORE_PATH:}
      keystore-password: ${IHE_MTLS_KEYSTORE_PASSWORD:}
      truststore-path: ${IHE_MTLS_TRUSTSTORE_PATH:}
      truststore-password: ${IHE_MTLS_TRUSTSTORE_PASSWORD:}

resilience4j:
  circuitbreaker:
    instances:
      ihe-document-query:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 20
        minimum-number-of-calls: 10
      ihe-document-retrieve:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 20
        minimum-number-of-calls: 10
      ihe-pix-query:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 15
        minimum-number-of-calls: 5
  retry:
    instances:
      ihe-api:
        max-attempts: 3
        wait-duration: 2s

tracing:
  enabled: true
  url: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://jaeger:4318/v1/traces}

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY:1.0}

logging:
  level:
    root: INFO
    com.healthdata.ihegateway: DEBUG
```

**Step 4: Create IheGatewayProperties.java**

```java
package com.healthdata.ihegateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "external.ihe")
public class IheGatewayProperties {
    private boolean enabled;
    private String healthixFhirUrl;
    private String healthixMpiUrl;
    private String healthixDocumentUrl;
    private int timeoutMs;
    private int connectTimeoutMs;
    private Mtls mtls = new Mtls();

    @Data
    public static class Mtls {
        private boolean enabled;
        private String keystorePath;
        private String keystorePassword;
        private String truststorePath;
        private String truststorePassword;
    }
}
```

**Step 5: Create IheSecurityConfig.java**

```java
package com.healthdata.ihegateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class IheSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/ihe-gateway/health/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

**Step 6: Create IheGatewayHealthController.java**

```java
package com.healthdata.ihegateway.health;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class IheGatewayHealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ihe-gateway-service",
                "timestamp", Instant.now().toString()
        ));
    }
}
```

**Step 7: Create empty Liquibase changelog**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.24.xsd">

    <!-- IHE Gateway service database migrations will be added here -->

</databaseChangeLog>
```

**Step 8: Create Dockerfile**

```dockerfile
# Stage 1: Build
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle :modules:services:ihe-gateway-service:bootJar -x test --no-daemon

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1001 -S healthdata && adduser -u 1001 -S healthdata -G healthdata
WORKDIR /app
COPY --from=builder /app/modules/services/ihe-gateway-service/build/libs/*.jar app.jar
RUN chown -R healthdata:healthdata /app
USER healthdata
EXPOSE 8125
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Step 9: Add to settings.gradle.kts**

Add `"modules:services:ihe-gateway-service"` to the External Integration Adapters section:

```kotlin
    // External Integration Adapters
    "modules:services:corehive-adapter-service",
    "modules:services:healthix-adapter-service",
    "modules:services:hedis-adapter-service",
    "modules:services:ihe-gateway-service"
```

**Step 10: Add ihe_gateway_db to init-multi-db.sh**

Read the file to find the section with external adapter databases, then add:
```sql
    CREATE DATABASE ihe_gateway_db;
```

**Step 11: Write health controller test**

```java
package com.healthdata.ihegateway.health;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class IheGatewayHealthControllerTest {

    private final IheGatewayHealthController controller = new IheGatewayHealthController();

    @Test
    void health_returnsUpStatus() {
        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "UP");
        assertThat(response.getBody()).containsEntry("service", "ihe-gateway-service");
        assertThat(response.getBody()).containsKey("timestamp");
    }
}
```

**Step 12: Verify compilation**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:ihe-gateway-service:compileJava --no-daemon
```
Expected: BUILD SUCCESSFUL

**Step 13: Run test**

```bash
./gradlew :modules:services:ihe-gateway-service:test --tests "*IheGatewayHealthControllerTest" --no-daemon
```
Expected: 1 test PASSED

**Step 14: Commit**

```bash
git add backend/modules/services/ihe-gateway-service/ \
        backend/settings.gradle.kts \
        docker/postgres/init-multi-db.sh
git commit -m "feat(ihe-gateway): scaffold ihe-gateway-service with Spring Boot, security, health check, and Liquibase"
```

---

## Task 9: Add IHE PIXv3 REST client to healthix-adapter

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/ihe/PixV3Client.java`
- Test: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/ihe/PixV3ClientTest.java`

**Step 1: Create PixV3Client**

This implements ITI-45 (PIX Query) using FHIR's `$match` operation as a REST-based alternative to SOAP. It queries the Healthix Verato MPI for patient cross-references.

```java
package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@Slf4j
public class PixV3Client {

    private static final String PIX_CROSSREF_TOPIC = "ihe.patient.crossref";

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;

    public PixV3Client(
            @Qualifier("healthixMpiRestTemplate") RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("ihe-pix-query");
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
    }

    /**
     * ITI-45: PIX Query — resolve patient identifiers across domains.
     * Uses FHIR $match as REST alternative to HL7v3 SOAP.
     */
    public PixCrossReferenceResult queryCrossReferences(String patientId, String assigningAuthority) {
        return spanHelper.traced("healthix.ihe.pix_query",
                () -> {
                    log.info("PIX query for patient={} authority={}", patientId, assigningAuthority);

                    Map<String, String> request = Map.of(
                            "patientId", patientId,
                            "assigningAuthority", assigningAuthority
                    );

                    Supplier<PixCrossReferenceResult> supplier = CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            () -> restTemplate.postForObject(
                                    "/api/v1/mpi/pix-query",
                                    request,
                                    PixCrossReferenceResult.class));

                    PixCrossReferenceResult result = supplier.get();

                    if (result != null && result.getIdentifiers() != null && !result.getIdentifiers().isEmpty()) {
                        kafkaTemplate.send(PIX_CROSSREF_TOPIC, patientId, result);
                        log.info("PIX cross-references published: {} identifiers", result.getIdentifiers().size());
                    }

                    return result;
                },
                "adapter", "healthix",
                "ihe.transaction", "ITI-45",
                "phi.level", "FULL");
    }

    @Data
    public static class PixCrossReferenceResult {
        private String sourcePatientId;
        private String sourceAuthority;
        private List<CrossReferenceIdentifier> identifiers;

        public PixCrossReferenceResult() {
            this.identifiers = Collections.emptyList();
        }
    }

    @Data
    public static class CrossReferenceIdentifier {
        private String patientId;
        private String assigningAuthority;
        private String identifierType;
    }
}
```

**Step 2: Write test**

```java
package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PixV3ClientTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private PixV3Client client;

    @BeforeEach
    void setUp() {
        // Use real circuit breaker (defaults, always closed)
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        // Use real span helper with no-op tracer
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.trace.TracerProvider.noop().get("test");
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(noopTracer);
        client = new PixV3Client(restTemplate, registry, kafkaTemplate, spanHelper);
    }

    @Test
    void queryCrossReferences_withMatches_publishesToKafka() {
        PixV3Client.PixCrossReferenceResult result = new PixV3Client.PixCrossReferenceResult();
        result.setSourcePatientId("P-001");
        result.setSourceAuthority("HDIM");
        PixV3Client.CrossReferenceIdentifier id = new PixV3Client.CrossReferenceIdentifier();
        id.setPatientId("MRN-12345");
        id.setAssigningAuthority("OHSU");
        id.setIdentifierType("MRN");
        result.setIdentifiers(List.of(id));

        when(restTemplate.postForObject(eq("/api/v1/mpi/pix-query"), any(), eq(PixV3Client.PixCrossReferenceResult.class)))
                .thenReturn(result);

        PixV3Client.PixCrossReferenceResult actual = client.queryCrossReferences("P-001", "HDIM");

        assertThat(actual.getIdentifiers()).hasSize(1);
        assertThat(actual.getIdentifiers().get(0).getAssigningAuthority()).isEqualTo("OHSU");
        verify(kafkaTemplate).send(eq("ihe.patient.crossref"), eq("P-001"), any());
    }

    @Test
    void queryCrossReferences_noMatches_doesNotPublishToKafka() {
        PixV3Client.PixCrossReferenceResult result = new PixV3Client.PixCrossReferenceResult();
        result.setSourcePatientId("P-999");
        result.setIdentifiers(List.of());

        when(restTemplate.postForObject(eq("/api/v1/mpi/pix-query"), any(), eq(PixV3Client.PixCrossReferenceResult.class)))
                .thenReturn(result);

        PixV3Client.PixCrossReferenceResult actual = client.queryCrossReferences("P-999", "HDIM");

        assertThat(actual.getIdentifiers()).isEmpty();
        verifyNoInteractions(kafkaTemplate);
    }
}
```

**Step 3: Run test**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:healthix-adapter-service:test --tests "*PixV3ClientTest" --no-daemon
```
Expected: 2 tests PASSED

**Step 4: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/
git commit -m "feat(healthix-adapter): add IHE PIXv3 REST client for patient cross-reference queries (ITI-45)"
```

---

## Task 10: Add XDS.b document query stub to ihe-gateway-service

**Files:**
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/actors/DocumentConsumer.java`
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/controller/IheDocumentController.java`
- Create: `backend/modules/services/ihe-gateway-service/src/main/java/com/healthdata/ihegateway/observability/AdapterSpanHelper.java`
- Test: `backend/modules/services/ihe-gateway-service/src/test/java/com/healthdata/ihegateway/actors/DocumentConsumerTest.java`

**Step 1: Create AdapterSpanHelper in ihe-gateway**

Copy the same `AdapterSpanHelper.java` pattern (package: `com.healthdata.ihegateway.observability`).

**Step 2: Create DocumentConsumer**

This implements ITI-18 (Registry Stored Query) and ITI-43 (Retrieve Document Set) using FHIR MHD (REST-based alternative to SOAP XDS.b). Queries Healthix's FHIR DocumentReference endpoint.

```java
package com.healthdata.ihegateway.actors;

import com.healthdata.ihegateway.config.IheGatewayProperties;
import com.healthdata.ihegateway.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
@Slf4j
public class DocumentConsumer {

    private static final String DOCUMENTS_RECEIVED_TOPIC = "ihe.documents.received";

    private final RestTemplate restTemplate;
    private final CircuitBreaker queryCircuitBreaker;
    private final CircuitBreaker retrieveCircuitBreaker;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;
    private final IheGatewayProperties properties;

    public DocumentConsumer(
            RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper,
            IheGatewayProperties properties) {
        this.restTemplate = restTemplate;
        this.queryCircuitBreaker = registry.circuitBreaker("ihe-document-query");
        this.retrieveCircuitBreaker = registry.circuitBreaker("ihe-document-retrieve");
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
        this.properties = properties;
    }

    /**
     * ITI-18: Registry Stored Query — find documents by patient ID.
     * Uses FHIR MHD (DocumentReference search) as REST alternative to SOAP.
     */
    @SuppressWarnings("unchecked")
    public DocumentQueryResult queryDocuments(String patientId, String documentType) {
        return spanHelper.traced("ihe.xds.registry_stored_query",
                () -> {
                    log.info("ITI-18 query: patient={} type={}", patientId, documentType);

                    String url = properties.getHealthixDocumentUrl()
                            + "/fhir/DocumentReference?patient=" + patientId
                            + "&type=" + documentType;

                    Supplier<Map> supplier = CircuitBreaker.decorateSupplier(
                            queryCircuitBreaker,
                            () -> restTemplate.getForObject(url, Map.class));

                    Map responseMap = supplier.get();

                    DocumentQueryResult result = new DocumentQueryResult();
                    result.setPatientId(patientId);
                    result.setDocumentType(documentType);
                    if (responseMap != null && responseMap.containsKey("entry")) {
                        List<Map<String, Object>> entries = (List<Map<String, Object>>) responseMap.get("entry");
                        result.setTotalResults(entries.size());
                    } else {
                        result.setTotalResults(0);
                    }

                    kafkaTemplate.send(DOCUMENTS_RECEIVED_TOPIC, patientId, result);
                    return result;
                },
                "ihe.transaction", "ITI-18",
                "patient.id", patientId,
                "document.type", documentType);
    }

    /**
     * ITI-43: Retrieve Document Set — fetch document content by URL.
     */
    public byte[] retrieveDocument(String documentUrl) {
        return spanHelper.traced("ihe.xds.retrieve_document_set",
                () -> {
                    log.info("ITI-43 retrieve: url={}", documentUrl);

                    Supplier<byte[]> supplier = CircuitBreaker.decorateSupplier(
                            retrieveCircuitBreaker,
                            () -> restTemplate.getForObject(documentUrl, byte[].class));

                    return supplier.get();
                },
                "ihe.transaction", "ITI-43");
    }

    @Data
    public static class DocumentQueryResult {
        private String patientId;
        private String documentType;
        private int totalResults;

        public DocumentQueryResult() {
            this.totalResults = 0;
        }
    }
}
```

**Step 3: Create IheDocumentController**

```java
package com.healthdata.ihegateway.controller;

import com.healthdata.ihegateway.actors.DocumentConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ihe/xds")
@RequiredArgsConstructor
public class IheDocumentController {

    private final DocumentConsumer documentConsumer;

    @GetMapping("/query")
    public ResponseEntity<DocumentConsumer.DocumentQueryResult> queryDocuments(
            @RequestParam String patientId,
            @RequestParam(defaultValue = "clinical-note") String documentType) {
        return ResponseEntity.ok(documentConsumer.queryDocuments(patientId, documentType));
    }

    @GetMapping("/retrieve")
    public ResponseEntity<byte[]> retrieveDocument(@RequestParam String documentUrl) {
        byte[] content = documentConsumer.retrieveDocument(documentUrl);
        return ResponseEntity.ok(content);
    }
}
```

**Step 4: Write test**

```java
package com.healthdata.ihegateway.actors;

import com.healthdata.ihegateway.config.IheGatewayProperties;
import com.healthdata.ihegateway.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DocumentConsumerTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private DocumentConsumer consumer;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.trace.TracerProvider.noop().get("test");
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(noopTracer);
        IheGatewayProperties props = new IheGatewayProperties();
        props.setHealthixDocumentUrl("http://localhost:3010");
        consumer = new DocumentConsumer(restTemplate, registry, kafkaTemplate, spanHelper, props);
    }

    @Test
    void queryDocuments_withResults_publishesToKafka() {
        Map<String, Object> fhirBundle = Map.of("entry", List.of(
                Map.of("resource", Map.of("id", "doc-1")),
                Map.of("resource", Map.of("id", "doc-2"))
        ));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(fhirBundle);

        DocumentConsumer.DocumentQueryResult result = consumer.queryDocuments("P-001", "clinical-note");

        assertThat(result.getTotalResults()).isEqualTo(2);
        assertThat(result.getPatientId()).isEqualTo("P-001");
        verify(kafkaTemplate).send(eq("ihe.documents.received"), eq("P-001"), any());
    }

    @Test
    void queryDocuments_noResults_publishesEmptyResult() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(Map.of());

        DocumentConsumer.DocumentQueryResult result = consumer.queryDocuments("P-002", "discharge");

        assertThat(result.getTotalResults()).isEqualTo(0);
        verify(kafkaTemplate).send(eq("ihe.documents.received"), eq("P-002"), any());
    }

    @Test
    void retrieveDocument_returnsContent() {
        byte[] content = "<?xml version=\"1.0\"?><ClinicalDocument/>".getBytes();
        when(restTemplate.getForObject(eq("http://docs/doc-1"), eq(byte[].class))).thenReturn(content);

        byte[] result = consumer.retrieveDocument("http://docs/doc-1");

        assertThat(result).isEqualTo(content);
    }
}
```

**Step 5: Run test**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:ihe-gateway-service:test --tests "*DocumentConsumerTest" --no-daemon
```
Expected: 3 tests PASSED

**Step 6: Commit**

```bash
git add backend/modules/services/ihe-gateway-service/src/
git commit -m "feat(ihe-gateway): add XDS.b document consumer with ITI-18 query and ITI-43 retrieve via FHIR MHD"
```

---

## Task 11: Add XDS.b document source to healthix-adapter

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/ihe/DocumentSource.java`
- Test: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/ihe/DocumentSourceTest.java`

**Step 1: Create DocumentSource**

Implements ITI-41 (Provide & Register) — submits care gap reports and quality summaries to the HIE registry via FHIR MHD.

```java
package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.function.Supplier;

@Component
@ConditionalOnProperty(name = "external.healthix.enabled", havingValue = "true")
@Slf4j
public class DocumentSource {

    private static final String DOCUMENTS_SUBMITTED_TOPIC = "ihe.documents.submitted";

    private final RestTemplate restTemplate;
    private final CircuitBreaker circuitBreaker;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AdapterSpanHelper spanHelper;

    public DocumentSource(
            @Qualifier("healthixFhirRestTemplate") RestTemplate restTemplate,
            CircuitBreakerRegistry registry,
            KafkaTemplate<String, Object> kafkaTemplate,
            AdapterSpanHelper spanHelper) {
        this.restTemplate = restTemplate;
        this.circuitBreaker = registry.circuitBreaker("healthix-fhir");
        this.kafkaTemplate = kafkaTemplate;
        this.spanHelper = spanHelper;
    }

    /**
     * ITI-41: Provide & Register Document Set — submit a document to HIE registry.
     * Uses FHIR MHD (DocumentReference + Binary POST) as REST alternative to SOAP.
     */
    public DocumentSubmissionResult submitDocument(DocumentSubmission submission) {
        return spanHelper.traced("healthix.ihe.provide_and_register",
                () -> {
                    log.info("ITI-41 submit: patient={} type={}", submission.getPatientId(), submission.getDocumentType());

                    Supplier<Map> supplier = CircuitBreaker.decorateSupplier(
                            circuitBreaker,
                            () -> restTemplate.postForObject(
                                    "/fhir/DocumentReference",
                                    Map.of(
                                            "resourceType", "DocumentReference",
                                            "status", "current",
                                            "subject", Map.of("reference", "Patient/" + submission.getPatientId()),
                                            "type", Map.of("text", submission.getDocumentType()),
                                            "description", submission.getDescription()
                                    ),
                                    Map.class));

                    Map responseMap = supplier.get();

                    DocumentSubmissionResult result = new DocumentSubmissionResult();
                    result.setPatientId(submission.getPatientId());
                    result.setDocumentType(submission.getDocumentType());
                    result.setSuccess(responseMap != null);
                    if (responseMap != null) {
                        result.setDocumentId(String.valueOf(responseMap.getOrDefault("id", "")));
                    }

                    kafkaTemplate.send(DOCUMENTS_SUBMITTED_TOPIC, submission.getPatientId(), result);
                    log.info("ITI-41 submission result published: success={}", result.isSuccess());

                    return result;
                },
                "adapter", "healthix",
                "ihe.transaction", "ITI-41",
                "phi.level", "FULL");
    }

    @Data
    public static class DocumentSubmission {
        private String patientId;
        private String documentType;
        private String description;
        private byte[] content;
    }

    @Data
    public static class DocumentSubmissionResult {
        private String patientId;
        private String documentType;
        private String documentId;
        private boolean success;
    }
}
```

**Step 2: Write test**

```java
package com.healthdata.healthixadapter.ihe;

import com.healthdata.healthixadapter.observability.AdapterSpanHelper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class DocumentSourceTest {

    @Mock private RestTemplate restTemplate;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private DocumentSource source;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        io.opentelemetry.api.trace.Tracer noopTracer = io.opentelemetry.api.trace.TracerProvider.noop().get("test");
        AdapterSpanHelper spanHelper = new AdapterSpanHelper(noopTracer);
        source = new DocumentSource(restTemplate, registry, kafkaTemplate, spanHelper);
    }

    @Test
    void submitDocument_success_publishesResult() {
        Map<String, Object> response = Map.of("id", "doc-new-1", "resourceType", "DocumentReference");
        when(restTemplate.postForObject(eq("/fhir/DocumentReference"), any(), eq(Map.class)))
                .thenReturn(response);

        DocumentSource.DocumentSubmission submission = new DocumentSource.DocumentSubmission();
        submission.setPatientId("P-001");
        submission.setDocumentType("care-gap-report");
        submission.setDescription("HEDIS BCS care gap report");

        DocumentSource.DocumentSubmissionResult result = source.submitDocument(submission);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDocumentId()).isEqualTo("doc-new-1");
        verify(kafkaTemplate).send(eq("ihe.documents.submitted"), eq("P-001"), any());
    }

    @Test
    void submitDocument_nullResponse_marksFailure() {
        when(restTemplate.postForObject(eq("/fhir/DocumentReference"), any(), eq(Map.class)))
                .thenReturn(null);

        DocumentSource.DocumentSubmission submission = new DocumentSource.DocumentSubmission();
        submission.setPatientId("P-002");
        submission.setDocumentType("quality-summary");
        submission.setDescription("Q1 quality summary");

        DocumentSource.DocumentSubmissionResult result = source.submitDocument(submission);

        assertThat(result.isSuccess()).isFalse();
        verify(kafkaTemplate).send(eq("ihe.documents.submitted"), eq("P-002"), any());
    }
}
```

**Step 3: Run test**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:healthix-adapter-service:test --tests "*DocumentSourceTest" --no-daemon
```
Expected: 2 tests PASSED

**Step 4: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/
git commit -m "feat(healthix-adapter): add IHE XDS.b document source for ITI-41 Provide & Register via FHIR MHD"
```

---

## Task 12: Add ihe-gateway-service to Docker Compose and Kong

**Files:**
- Modify: `docker-compose.external-integrations.yml`
- Modify: `kong/kong.yaml`

**Step 1: Add ihe-gateway-service to docker-compose.external-integrations.yml**

Read the file first, then add the following service definition after the hedis-adapter-service block:

```yaml
  # ===========================================================================
  # IHE Gateway Service (Port 8125)
  # PHI Level: FULL - IHE transactions carry clinical data
  # ===========================================================================
  ihe-gateway-service:
    container_name: healthdata-ihe-gateway
    restart: unless-stopped
    build:
      context: ./backend
      dockerfile: modules/services/ihe-gateway-service/Dockerfile
    profiles: ["external", "external-ihe"]
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_started
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/ihe_gateway_db
      SPRING_DATASOURCE_USERNAME: healthdata
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SERVER_PORT: 8125
      IHE_GATEWAY_ENABLED: "true"
      HEALTHIX_FHIR_URL: ${HEALTHIX_FHIR_URL:-http://healthix-fhir:8080}
      HEALTHIX_MPI_URL: ${HEALTHIX_MPI_URL:-http://healthix-mpi:8000}
      HEALTHIX_DOCUMENT_URL: ${HEALTHIX_DOCUMENT_URL:-http://healthix-document:3010}
      SPRING_FLYWAY_ENABLED: "false"
      OTEL_EXPORTER_OTLP_ENDPOINT: http://jaeger:4318/v1/traces
    ports:
      - "8125:8125"
    networks:
      - hdim-network
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8125/ihe-gateway/health"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s
```

**Step 2: Add IHE routes to kong.yaml**

Read `kong/kong.yaml` to find the external adapter routes section, then add after the existing external routes:

```yaml
  - name: ihe-gateway
    url: http://ihe-gateway-service:8125
    routes:
      - name: ihe-api
        paths:
          - /ihe
        strip_path: false
        methods: [GET, POST, PUT]
        plugins:
          - name: user-patient-kafka
            config:
              require_patient: true
```

**Step 3: Commit**

```bash
git add docker-compose.external-integrations.yml kong/kong.yaml
git commit -m "feat(infra): add ihe-gateway-service to Docker Compose overlay and Kong gateway routes"
```

---

## Task 13: Create Grafana dashboard JSON for external integrations

**Files:**
- Create: `docker/grafana/dashboards/external-integrations.json`

**Step 1: Create dashboard JSON**

Create a Grafana dashboard with 4 panels:

1. **Overview** — request rate across all adapters
2. **CoreHive Adapter** — scoring requests, ROI requests, PHI blocks, latency
3. **Healthix Adapter** — FHIR notifications, C-CDA documents, MPI queries, HL7 messages
4. **hedis Adapter** — measures synced, CQL delegations, WebSocket connections

```json
{
  "dashboard": {
    "id": null,
    "uid": "hdim-external-integrations",
    "title": "HDIM External Integration Adapters",
    "tags": ["hdim", "external", "adapters"],
    "timezone": "browser",
    "refresh": "10s",
    "time": { "from": "now-1h", "to": "now" },
    "panels": [
      {
        "id": 1,
        "title": "Total Adapter Requests (all adapters)",
        "type": "timeseries",
        "gridPos": { "h": 8, "w": 24, "x": 0, "y": 0 },
        "targets": [
          {
            "expr": "sum(rate(hdim_adapter_corehive_scoring_requests_total[5m]))",
            "legendFormat": "CoreHive Scoring"
          },
          {
            "expr": "sum(rate(hdim_adapter_healthix_fhir_notifications_total[5m]))",
            "legendFormat": "Healthix FHIR"
          },
          {
            "expr": "sum(rate(hdim_adapter_healthix_hl7_messages_total[5m]))",
            "legendFormat": "Healthix HL7"
          },
          {
            "expr": "sum(rate(hdim_adapter_hedis_measures_synced_total[5m]))",
            "legendFormat": "hedis Measures"
          }
        ]
      },
      {
        "id": 2,
        "title": "CoreHive Adapter — Scoring & ROI",
        "type": "timeseries",
        "gridPos": { "h": 8, "w": 12, "x": 0, "y": 8 },
        "targets": [
          {
            "expr": "rate(hdim_adapter_corehive_scoring_requests_total[5m])",
            "legendFormat": "Scoring req/s"
          },
          {
            "expr": "rate(hdim_adapter_corehive_roi_requests_total[5m])",
            "legendFormat": "ROI req/s"
          },
          {
            "expr": "rate(hdim_adapter_corehive_scoring_errors_total[5m])",
            "legendFormat": "Scoring errors/s"
          },
          {
            "expr": "rate(hdim_adapter_corehive_phi_blocked_total[5m])",
            "legendFormat": "PHI blocked/s"
          }
        ]
      },
      {
        "id": 3,
        "title": "CoreHive Latency (p99)",
        "type": "gauge",
        "gridPos": { "h": 8, "w": 12, "x": 12, "y": 8 },
        "targets": [
          {
            "expr": "histogram_quantile(0.99, rate(hdim_adapter_corehive_scoring_latency_seconds_bucket[5m]))",
            "legendFormat": "Scoring p99"
          },
          {
            "expr": "histogram_quantile(0.99, rate(hdim_adapter_corehive_roi_latency_seconds_bucket[5m]))",
            "legendFormat": "ROI p99"
          }
        ]
      },
      {
        "id": 4,
        "title": "Healthix Adapter — FHIR, C-CDA, MPI, HL7",
        "type": "timeseries",
        "gridPos": { "h": 8, "w": 12, "x": 0, "y": 16 },
        "targets": [
          {
            "expr": "rate(hdim_adapter_healthix_fhir_notifications_total[5m])",
            "legendFormat": "FHIR notifications/s"
          },
          {
            "expr": "rate(hdim_adapter_healthix_ccda_documents_total[5m])",
            "legendFormat": "C-CDA docs/s"
          },
          {
            "expr": "rate(hdim_adapter_healthix_mpi_queries_total[5m])",
            "legendFormat": "MPI queries/s"
          },
          {
            "expr": "rate(hdim_adapter_healthix_hl7_messages_total[5m])",
            "legendFormat": "HL7 messages/s"
          }
        ]
      },
      {
        "id": 5,
        "title": "Healthix Latency (p99)",
        "type": "gauge",
        "gridPos": { "h": 8, "w": 12, "x": 12, "y": 16 },
        "targets": [
          {
            "expr": "histogram_quantile(0.99, rate(hdim_adapter_healthix_fhir_latency_seconds_bucket[5m]))",
            "legendFormat": "FHIR p99"
          },
          {
            "expr": "histogram_quantile(0.99, rate(hdim_adapter_healthix_mpi_latency_seconds_bucket[5m]))",
            "legendFormat": "MPI p99"
          },
          {
            "expr": "histogram_quantile(0.99, rate(hdim_adapter_healthix_ccda_latency_seconds_bucket[5m]))",
            "legendFormat": "C-CDA p99"
          }
        ]
      },
      {
        "id": 6,
        "title": "hedis Adapter — Measures, CQL, CRM",
        "type": "timeseries",
        "gridPos": { "h": 8, "w": 12, "x": 0, "y": 24 },
        "targets": [
          {
            "expr": "rate(hdim_adapter_hedis_measures_synced_total[5m])",
            "legendFormat": "Measures synced/s"
          },
          {
            "expr": "rate(hdim_adapter_hedis_cql_delegations_total[5m])",
            "legendFormat": "CQL delegations/s"
          },
          {
            "expr": "rate(hdim_adapter_hedis_crm_webhooks_total[5m])",
            "legendFormat": "CRM webhooks/s"
          }
        ]
      },
      {
        "id": 7,
        "title": "hedis WebSocket Connections",
        "type": "stat",
        "gridPos": { "h": 8, "w": 12, "x": 12, "y": 24 },
        "targets": [
          {
            "expr": "hdim_adapter_hedis_websocket_connections_active",
            "legendFormat": "Active connections"
          },
          {
            "expr": "rate(hdim_adapter_hedis_websocket_messages_total[5m])",
            "legendFormat": "Messages/s"
          }
        ]
      },
      {
        "id": 8,
        "title": "Circuit Breaker States",
        "type": "table",
        "gridPos": { "h": 6, "w": 24, "x": 0, "y": 32 },
        "targets": [
          {
            "expr": "resilience4j_circuitbreaker_state{application=~\".*adapter.*|.*ihe.*\"}",
            "legendFormat": "{{name}} ({{application}})",
            "format": "table",
            "instant": true
          }
        ]
      }
    ]
  }
}
```

**Step 2: Commit**

```bash
git add docker/grafana/dashboards/external-integrations.json
git commit -m "feat(infra): add Grafana dashboard for external integration adapter metrics"
```

---

## Task 14: Final verification — run all tests across all 4 services

**Step 1: Run all tests with --rerun**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:corehive-adapter-service:test \
          :modules:services:healthix-adapter-service:test \
          :modules:services:hedis-adapter-service:test \
          :modules:services:ihe-gateway-service:test \
          --no-daemon --rerun
```
Expected: All tests PASSED, 0 failures

**Step 2: Count total tests**

Note the test counts per adapter:
- corehive-adapter: existing + AdapterSpanHelperTest (3) + AdapterMetricsTest (3) + AtnaAuditArrForwardingTest (2)
- healthix-adapter: existing + AdapterSpanHelperTest (3) + AdapterMetricsTest (3) + PixV3ClientTest (2) + DocumentSourceTest (2)
- hedis-adapter: existing + AdapterSpanHelperTest (3) + AdapterMetricsTest (3)
- ihe-gateway: IheGatewayHealthControllerTest (1) + DocumentConsumerTest (3)

**Step 3: Final commit (if not already committed)**

```bash
git add -A
git status
# Verify only Sprint 3 files are staged
git commit -m "feat(adapters): Sprint 3 Observe — OpenTelemetry spans, Prometheus metrics, ECS logging, IHE PIX/XDS.b, ATNA ARR, ihe-gateway scaffold"
```

---

## Summary: Sprint 3 Exit Criteria Verification

| Criterion | Target | How to Verify |
|---|---|---|
| Custom OTel spans | 12+ | Count `spanHelper.traced` / `tracedRun` calls across 4 services |
| Prometheus metrics scraped | Yes | `curl http://localhost:8120/actuator/prometheus` returns `hdim_adapter_*` metrics |
| Grafana dashboard | 4 panels + overview | Import `external-integrations.json` into Grafana |
| PIXv3 cross-reference | Operational | `PixV3ClientTest` passes — queries MPI, publishes to `ihe.patient.crossref` |
| XDS.b ITI-18 query | Operational | `DocumentConsumerTest` passes — queries DocumentReference, publishes to `ihe.documents.received` |
| XDS.b ITI-41 submit | Operational | `DocumentSourceTest` passes — submits DocumentReference, publishes to `ihe.documents.submitted` |
| ATNA ARR forwarding | Operational | `AtnaAuditArrForwardingTest` passes — forwards to `ihe.audit.events` topic |
| ihe-gateway-service | Scaffolded | Compiles, health check test passes, registered in settings.gradle.kts |
| Structured logging | ECS format | `logback-spring.xml` with traceId/spanId/correlationId in all 3 adapters |
| All existing tests pass | 140+ | All 4 services pass test suite |
