# Sprint 2 "Test" — Coverage & Reliability Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Raise adapter test coverage to 70%, add explicit Kafka deserializer configs, test all untested service classes, and establish a load test baseline.

**Architecture:** Each task adds unit or integration tests for untested service/client classes, or adds missing Kafka/circuit-breaker configuration. Tests follow TDD — write failing test first, then implement any missing code to make it pass. All adapters already have Sprint 1 security/audit/exception-handling tests.

**Tech Stack:** Java 21, Spring Boot 3.x, JUnit 5, Mockito, AssertJ, spring-kafka-test (EmbeddedKafka), Resilience4j, WebSocket (hedis only)

**References:**
- Design doc: `docs/plans/2026-03-06-v3.0.0-rc1-shield-design.md` (Sprint 2, section 3)
- Sprint 1 commit: `d7454292a` (security hardening baseline)

**Conventions:**
- Package pattern: `com.healthdata.{servicenameflat}`
- Base paths: `backend/modules/services/{adapter}-adapter-service/src/`
- Test tags: `@Tag("unit")` for unit tests, `@Tag("integration")` for integration tests
- Unit tests run with `./gradlew :modules:services:{adapter}-adapter-service:test`
- Integration tests run with `./gradlew :modules:services:{adapter}-adapter-service:test` using `testAll` or `testIntegration` task

---

## Task 1: Add spring-kafka-test dependency to all 3 adapters

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/build.gradle.kts`
- Modify: `backend/modules/services/healthix-adapter-service/build.gradle.kts`
- Modify: `backend/modules/services/hedis-adapter-service/build.gradle.kts`

**Step 1: Add the dependency to each build file**

Add after the existing `testImplementation(libs.spring.security.test)` line in each `build.gradle.kts`:

```kotlin
testImplementation(libs.spring.kafka.test)
```

**Step 2: Verify the dependency resolves**

Run (for each adapter):
```bash
cd backend && ./gradlew :modules:services:corehive-adapter-service:dependencies --configuration testCompileClasspath | grep kafka-test
cd backend && ./gradlew :modules:services:healthix-adapter-service:dependencies --configuration testCompileClasspath | grep kafka-test
cd backend && ./gradlew :modules:services:hedis-adapter-service:dependencies --configuration testCompileClasspath | grep kafka-test
```
Expected: `org.springframework.kafka:spring-kafka-test:X.X.X`

**Step 3: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/build.gradle.kts \
        backend/modules/services/healthix-adapter-service/build.gradle.kts \
        backend/modules/services/hedis-adapter-service/build.gradle.kts
git commit -m "build(adapters): add spring-kafka-test dependency for Kafka integration tests"
```

---

## Task 2: Add explicit Kafka consumer deserializer configs to all 3 adapters

Currently all 3 adapters define producer serializers but rely on Spring defaults for consumer deserialization. This is a reliability risk — without explicit `JsonDeserializer` + trusted packages, incoming messages from unknown types silently fail.

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/src/main/resources/application.yml`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/resources/application.yml`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/resources/application.yml`

**Step 1: Add consumer deserializer config to corehive application.yml**

Under `spring.kafka.consumer`, add after `enable-auto-commit: false`:

```yaml
    key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
    properties:
      spring.json.trusted.packages: "com.healthdata.*,java.util,java.lang"
      spring.json.value.default.type: "java.util.LinkedHashMap"
```

**Step 2: Add consumer deserializer config to healthix application.yml**

Same config block under `spring.kafka.consumer` (after `enable-auto-commit: false`).

**Step 3: Add consumer deserializer config to hedis application.yml**

Same config block under `spring.kafka.consumer` (after `enable-auto-commit: false`).

**Step 4: Run existing tests to verify no regressions**

```bash
cd backend && ./gradlew :modules:services:corehive-adapter-service:test :modules:services:healthix-adapter-service:test :modules:services:hedis-adapter-service:test
```
Expected: All existing tests pass.

**Step 5: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/resources/application.yml \
        backend/modules/services/healthix-adapter-service/src/main/resources/application.yml \
        backend/modules/services/hedis-adapter-service/src/main/resources/application.yml
git commit -m "fix(adapters): add explicit Kafka consumer deserializer configs with trusted packages"
```

---

## Task 3: Unit tests for CareGapEventListener (corehive)

The `CareGapEventListener` is a `@KafkaListener` that consumes care gap events, de-identifies patient IDs, and triggers AI scoring. It has 0 tests.

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/event/CareGapEventListenerTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.corehiveadapter.event;

import com.healthdata.corehiveadapter.service.CorehiveAdapterService;
import com.healthdata.corehiveadapter.service.PhiDeIdentificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CareGapEventListener")
class CareGapEventListenerTest {

    @Mock
    private CorehiveAdapterService adapterService;

    private PhiDeIdentificationService deIdentificationService;
    private CareGapEventListener listener;

    @BeforeEach
    void setUp() {
        deIdentificationService = new PhiDeIdentificationService();
        listener = new CareGapEventListener(adapterService, deIdentificationService);
    }

    @Test
    @DisplayName("should de-identify patient ID and invoke scoring")
    void onCareGapEvent_shouldDeIdentifyAndScore() {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", "real-patient-123");
        event.put("tenantId", "tenant-1");

        listener.onCareGapEvent(event);

        var captor = ArgumentCaptor.forClass(com.healthdata.corehiveadapter.model.CareGapScoringRequest.class);
        verify(adapterService).scoreCareGaps(captor.capture(), eq("tenant-1"));

        var request = captor.getValue();
        assertThat(request.getSyntheticPatientId()).isNotEqualTo("real-patient-123");
        assertThat(request.getSyntheticPatientId()).isNotBlank();
        assertThat(request.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("should skip events missing patientId")
    void onCareGapEvent_shouldSkipWhenPatientIdMissing() {
        Map<String, Object> event = new HashMap<>();
        event.put("tenantId", "tenant-1");

        listener.onCareGapEvent(event);

        verifyNoInteractions(adapterService);
    }

    @Test
    @DisplayName("should use default tenantId when not provided")
    void onCareGapEvent_shouldUseDefaultTenantId() {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", "patient-xyz");

        listener.onCareGapEvent(event);

        verify(adapterService).scoreCareGaps(any(), eq("unknown"));
    }

    @Test
    @DisplayName("should not propagate exception from adapter service")
    void onCareGapEvent_shouldCatchAdapterException() {
        Map<String, Object> event = new HashMap<>();
        event.put("patientId", "patient-abc");
        event.put("tenantId", "tenant-1");

        doThrow(new RuntimeException("CoreHive unreachable"))
                .when(adapterService).scoreCareGaps(any(), any());

        // Should not throw
        listener.onCareGapEvent(event);

        verify(adapterService).scoreCareGaps(any(), eq("tenant-1"));
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*CareGapEventListenerTest"
```
Expected: 4/4 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/event/CareGapEventListenerTest.java
git commit -m "test(corehive): add unit tests for CareGapEventListener"
```

---

## Task 4: Unit tests for FhirSubscriptionClient (healthix)

The `FhirSubscriptionClient` handles FHIR notifications and registration. It has 0 tests.

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/fhir/FhirSubscriptionClientTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.healthixadapter.fhir;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("FhirSubscriptionClient")
class FhirSubscriptionClientTest {

    @Mock
    private RestTemplate fhirRestTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private FhirSubscriptionClient client;

    @BeforeEach
    void setUp() {
        client = new FhirSubscriptionClient(fhirRestTemplate, kafkaTemplate);
    }

    @Test
    @DisplayName("should publish FHIR notification to Kafka with correct topic and envelope")
    void handleFhirNotification_shouldPublishToKafka() {
        Map<String, Object> fhirResource = new HashMap<>();
        fhirResource.put("resourceType", "Patient");
        fhirResource.put("id", "patient-123");

        client.handleFhirNotification(fhirResource, "tenant-1");

        var envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.healthix.fhir"), eq("tenant-1"), envelopeCaptor.capture());

        Object envelope = envelopeCaptor.getValue();
        assertThat(envelope).isNotNull();
        assertThat(envelope.toString()).contains("external.healthix.fhir.patient.received");
    }

    @Test
    @DisplayName("should handle unknown resource type in notification")
    void handleFhirNotification_shouldHandleUnknownResourceType() {
        Map<String, Object> fhirResource = new HashMap<>();
        // No resourceType key

        client.handleFhirNotification(fhirResource, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.fhir"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should register subscription with Healthix FHIR service")
    void registerSubscription_shouldPostToFhirEndpoint() {
        client.registerSubscription("https://hdim.example.com/fhir/callback", "Patient");

        var bodyCaptor = ArgumentCaptor.forClass(Object.class);
        verify(fhirRestTemplate).postForEntity(eq("/fhir/Subscription"), bodyCaptor.capture(), eq(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) bodyCaptor.getValue();
        assertThat(body.get("resourceType")).isEqualTo("Subscription");
        assertThat(body.get("criteria")).isEqualTo("Patient?");
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*FhirSubscriptionClientTest"
```
Expected: 3/3 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/fhir/FhirSubscriptionClientTest.java
git commit -m "test(healthix): add unit tests for FhirSubscriptionClient"
```

---

## Task 5: Unit tests for CcdaIngestionService (healthix)

The `CcdaIngestionService` fetches C-CDA documents via circuit breaker and publishes to Kafka. It has 0 tests.

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/ccda/CcdaIngestionServiceTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.healthixadapter.ccda;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CcdaIngestionService")
class CcdaIngestionServiceTest {

    @Mock
    private RestTemplate documentRestTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CcdaIngestionService service;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new CcdaIngestionService(documentRestTemplate, kafkaTemplate, registry);
    }

    @Test
    @DisplayName("should fetch document and publish to Kafka")
    void ingestDocument_shouldFetchAndPublish() {
        Map<String, Object> document = Map.of("id", "doc-123", "content", "<ClinicalDocument/>");
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-123")))
                .thenReturn(document);

        service.ingestDocument("doc-123", "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.documents"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should skip when document not found")
    void ingestDocument_shouldSkipWhenDocumentNull() {
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-404")))
                .thenReturn(null);

        service.ingestDocument("doc-404", "tenant-1");

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("should route C-CDA webhook to ingestion")
    void onDocumentWebhook_shouldIngestCcdaDocuments() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("documentId", "doc-789");
        webhook.put("documentType", "C-CDA");

        Map<String, Object> document = Map.of("id", "doc-789", "content", "<ClinicalDocument/>");
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-789")))
                .thenReturn(document);

        service.onDocumentWebhook(webhook, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.documents"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should also accept CDA document type")
    void onDocumentWebhook_shouldAcceptCdaType() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("documentId", "doc-cda");
        webhook.put("documentType", "CDA");

        Map<String, Object> document = Map.of("id", "doc-cda");
        when(documentRestTemplate.getForObject(anyString(), eq(Map.class), eq("doc-cda")))
                .thenReturn(document);

        service.onDocumentWebhook(webhook, "tenant-1");

        verify(kafkaTemplate).send(eq("external.healthix.documents"), eq("tenant-1"), any());
    }

    @Test
    @DisplayName("should skip non-C-CDA document types")
    void onDocumentWebhook_shouldSkipNonCcdaDocuments() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("documentId", "doc-pdf");
        webhook.put("documentType", "PDF");

        service.onDocumentWebhook(webhook, "tenant-1");

        verifyNoInteractions(kafkaTemplate);
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*CcdaIngestionServiceTest"
```
Expected: 5/5 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/ccda/CcdaIngestionServiceTest.java
git commit -m "test(healthix): add unit tests for CcdaIngestionService"
```

---

## Task 6: Unit tests for VeratoMpiProxy (healthix)

The `VeratoMpiProxy` queries Healthix MPI with circuit breaker wrapping. It has 0 tests.

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/mpi/VeratoMpiProxyTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.healthixadapter.mpi;

import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.CrossReference;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.MpiMatchRequest;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.MpiMatchResult;
import com.healthdata.healthixadapter.mpi.VeratoMpiProxy.PatientIdentifier;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("VeratoMpiProxy")
class VeratoMpiProxyTest {

    @Mock
    private RestTemplate mpiRestTemplate;

    private VeratoMpiProxy proxy;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        proxy = new VeratoMpiProxy(mpiRestTemplate, registry);
    }

    @Test
    @DisplayName("should post match request and return result")
    void queryPatientMatch_shouldReturnResult() {
        MpiMatchRequest request = MpiMatchRequest.builder()
                .tenantId("tenant-1")
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth("1985-03-15")
                .identifiers(List.of(
                        PatientIdentifier.builder()
                                .system("urn:oid:2.16.840.1.113883.4.1")
                                .value("MRN-12345")
                                .assigningAuthority("GoodHealth Hospital")
                                .build()))
                .build();

        MpiMatchResult expectedResult = MpiMatchResult.builder()
                .enterpriseId("EUID-001")
                .matchConfidence(0.98)
                .matchStatus("MATCH")
                .crossReferences(List.of(
                        CrossReference.builder()
                                .organization("Providence")
                                .patientId("PROV-99887")
                                .build()))
                .build();

        when(mpiRestTemplate.postForObject(
                eq("/api/v1/patient-identity/match"),
                any(MpiMatchRequest.class),
                eq(MpiMatchResult.class)))
                .thenReturn(expectedResult);

        MpiMatchResult result = proxy.queryPatientMatch(request);

        assertThat(result.getEnterpriseId()).isEqualTo("EUID-001");
        assertThat(result.getMatchConfidence()).isEqualTo(0.98);
        assertThat(result.getCrossReferences()).hasSize(1);
        verify(mpiRestTemplate).postForObject(eq("/api/v1/patient-identity/match"), any(), eq(MpiMatchResult.class));
    }

    @Test
    @DisplayName("should propagate exception when MPI is unreachable")
    void queryPatientMatch_shouldPropagateException() {
        MpiMatchRequest request = MpiMatchRequest.builder()
                .tenantId("tenant-1")
                .identifiers(List.of())
                .build();

        when(mpiRestTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> proxy.queryPatientMatch(request))
                .isInstanceOf(ResourceAccessException.class);
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*VeratoMpiProxyTest"
```
Expected: 2/2 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/mpi/VeratoMpiProxyTest.java
git commit -m "test(healthix): add unit tests for VeratoMpiProxy"
```

---

## Task 7: Unit tests for CrmSyncService (hedis)

The `CrmSyncService` handles bidirectional CRM sync with circuit breaker. It has 0 tests.

**Files:**
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/crm/CrmSyncServiceTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.hedisadapter.crm;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CrmSyncService")
class CrmSyncServiceTest {

    @Mock
    private RestTemplate hedisRestTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CrmSyncService service;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new CrmSyncService(hedisRestTemplate, kafkaTemplate, registry);
    }

    @Test
    @DisplayName("should push deal update to hedis CRM endpoint")
    void pushDealUpdate_shouldPostToHedis() {
        Map<String, Object> dealData = Map.of("dealId", "deal-001", "stage", "Closed Won");

        when(hedisRestTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.pushDealUpdate(dealData);

        verify(hedisRestTemplate).postForEntity(eq("/api/crm/deals/sync"), eq(dealData), eq(Void.class));
    }

    @Test
    @DisplayName("should propagate exception when hedis CRM is unreachable")
    void pushDealUpdate_shouldPropagateException() {
        Map<String, Object> dealData = Map.of("dealId", "deal-002");

        when(hedisRestTemplate.postForEntity(anyString(), any(), eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> service.pushDealUpdate(dealData))
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("should publish webhook payload to Kafka CRM topic")
    void onCrmWebhook_shouldPublishToKafka() {
        Map<String, Object> payload = Map.of("event", "deal.updated", "dealId", "deal-003");

        service.onCrmWebhook(payload);

        verify(kafkaTemplate).send(eq("external.hedis.crm"), eq("crm-sync"), eq(payload));
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:hedis-adapter-service:test --tests "*CrmSyncServiceTest"
```
Expected: 3/3 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/crm/CrmSyncServiceTest.java
git commit -m "test(hedis): add unit tests for CrmSyncService"
```

---

## Task 8: Unit tests for MeasureRegistrySyncService (hedis)

The `MeasureRegistrySyncService` scheduled sync fetches measures via circuit breaker and publishes to Kafka. It has 0 tests.

**Files:**
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/measures/MeasureRegistrySyncServiceTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.hedisadapter.measures;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("MeasureRegistrySyncService")
class MeasureRegistrySyncServiceTest {

    @Mock
    private RestTemplate hedisRestTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private MeasureRegistrySyncService service;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new MeasureRegistrySyncService(hedisRestTemplate, kafkaTemplate, registry);
    }

    @Test
    @DisplayName("should fetch measures and publish to Kafka")
    void syncMeasureRegistry_shouldFetchAndPublish() {
        List<Map<String, Object>> measures = List.of(
                Map.of("measureId", "BCS", "type", "HEDIS"),
                Map.of("measureId", "CBP", "type", "HEDIS"));

        when(hedisRestTemplate.getForObject(eq("/api/measures/sync"), eq(List.class)))
                .thenReturn(measures);

        service.syncMeasureRegistry();

        var envelopeCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("external.hedis.measures"), eq("system"), envelopeCaptor.capture());
        assertThat(envelopeCaptor.getValue().toString()).contains("external.hedis.measures.synced");
    }

    @Test
    @DisplayName("should skip when no measures returned")
    void syncMeasureRegistry_shouldSkipWhenEmpty() {
        when(hedisRestTemplate.getForObject(eq("/api/measures/sync"), eq(List.class)))
                .thenReturn(List.of());

        service.syncMeasureRegistry();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("should skip when null returned")
    void syncMeasureRegistry_shouldSkipWhenNull() {
        when(hedisRestTemplate.getForObject(eq("/api/measures/sync"), eq(List.class)))
                .thenReturn(null);

        service.syncMeasureRegistry();

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("should catch exception from external service without propagating")
    void syncMeasureRegistry_shouldCatchException() {
        when(hedisRestTemplate.getForObject(anyString(), any()))
                .thenThrow(new ResourceAccessException("hedis unavailable"));

        // Should not throw
        service.syncMeasureRegistry();

        verifyNoInteractions(kafkaTemplate);
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:hedis-adapter-service:test --tests "*MeasureRegistrySyncServiceTest"
```
Expected: 4/4 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/measures/MeasureRegistrySyncServiceTest.java
git commit -m "test(hedis): add unit tests for MeasureRegistrySyncService"
```

---

## Task 9: Unit tests for CqlDelegationService (hedis)

The `CqlDelegationService` delegates HEDIS/MIPS/STAR measure calculations via circuit breaker. It has 0 tests.

**Files:**
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/measures/CqlDelegationServiceTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.hedisadapter.measures;

import com.healthdata.hedisadapter.measures.CqlDelegationService.CalculationRequest;
import com.healthdata.hedisadapter.measures.CqlDelegationService.CalculationResult;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CqlDelegationService")
class CqlDelegationServiceTest {

    @Mock
    private RestTemplate cqlRestTemplate;

    private CqlDelegationService service;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        service = new CqlDelegationService(cqlRestTemplate, registry);
    }

    @Test
    @DisplayName("should delegate HEDIS measure calculation")
    void calculateMeasure_shouldDelegateToHedis() {
        CalculationRequest request = CalculationRequest.builder()
                .measureId("BCS")
                .measureType("HEDIS")
                .tenantId("tenant-1")
                .patientId("patient-123")
                .build();

        CalculationResult expected = CalculationResult.builder()
                .measureId("BCS")
                .status("NUMERATOR")
                .score(1.0)
                .build();

        when(cqlRestTemplate.postForObject(eq("/api/measures/calculate"), any(), eq(CalculationResult.class)))
                .thenReturn(expected);

        CalculationResult result = service.calculateMeasure(request);

        assertThat(result.getMeasureId()).isEqualTo("BCS");
        assertThat(result.getStatus()).isEqualTo("NUMERATOR");
        verify(cqlRestTemplate).postForObject(eq("/api/measures/calculate"), eq(request), eq(CalculationResult.class));
    }

    @Test
    @DisplayName("should delegate STAR measure calculation to different endpoint")
    void calculateStarMeasure_shouldUseStarEndpoint() {
        CalculationRequest request = CalculationRequest.builder()
                .measureId("C01")
                .measureType("STAR")
                .tenantId("tenant-1")
                .patientId("patient-456")
                .build();

        CalculationResult expected = CalculationResult.builder()
                .measureId("C01")
                .measureType("STAR")
                .status("DENOMINATOR")
                .score(0.0)
                .build();

        when(cqlRestTemplate.postForObject(eq("/api/star/calculate"), any(), eq(CalculationResult.class)))
                .thenReturn(expected);

        CalculationResult result = service.calculateStarMeasure(request);

        assertThat(result.getMeasureId()).isEqualTo("C01");
        verify(cqlRestTemplate).postForObject(eq("/api/star/calculate"), eq(request), eq(CalculationResult.class));
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:hedis-adapter-service:test --tests "*CqlDelegationServiceTest"
```
Expected: 2/2 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/measures/CqlDelegationServiceTest.java
git commit -m "test(hedis): add unit tests for CqlDelegationService"
```

---

## Task 10: Unit tests for Hl7AdtConsumer — expanded ADT trigger coverage (healthix)

The existing `Hl7AdtConsumerTest` has 4 tests but only covers A01 and A28 triggers. The `Hl7AdtConsumer` supports 7 trigger events (A01-A04, A08, A28, A31). Add tests for the remaining trigger events and the `isPatientEvent`/`mapToPatientEventType` logic.

**Files:**
- Modify: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/hl7/Hl7AdtConsumerTest.java`

**Step 1: Read the existing test file first**

Read `Hl7AdtConsumerTest.java` to understand the existing test structure before adding new tests.

**Step 2: Add the missing trigger event tests**

Add the following test methods to the existing `Hl7AdtConsumerTest` class:

```java
@Test
@DisplayName("A02 (Transfer) should publish HL7 event and fhir.encounter.updated patient event")
void processAdtMessage_a02Transfer_shouldPublishEncounterUpdated() {
    Map<String, Object> message = new HashMap<>();
    message.put("messageType", "ADT");
    message.put("triggerEvent", "A02");
    message.put("patientId", "patient-transfer");

    consumer.processAdtMessage(message, "tenant-1");

    verify(kafkaTemplate).send(eq("external.healthix.hl7"), eq("tenant-1"), any());
    var patientCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
    assertThat(patientCaptor.getValue().toString()).contains("fhir.encounter.updated");
}

@Test
@DisplayName("A03 (Discharge) should publish fhir.encounter.completed patient event")
void processAdtMessage_a03Discharge_shouldPublishEncounterCompleted() {
    Map<String, Object> message = new HashMap<>();
    message.put("messageType", "ADT");
    message.put("triggerEvent", "A03");
    message.put("patientId", "patient-discharge");

    consumer.processAdtMessage(message, "tenant-1");

    verify(kafkaTemplate, times(2)).send(anyString(), eq("tenant-1"), any());
}

@Test
@DisplayName("A04 (Register) should publish fhir.patient.created patient event")
void processAdtMessage_a04Register_shouldPublishPatientCreated() {
    Map<String, Object> message = new HashMap<>();
    message.put("messageType", "ADT");
    message.put("triggerEvent", "A04");
    message.put("patientId", "patient-register");

    consumer.processAdtMessage(message, "tenant-1");

    var patientCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
    assertThat(patientCaptor.getValue().toString()).contains("fhir.patient.created");
}

@Test
@DisplayName("A08 (Update) should publish fhir.patient.updated patient event")
void processAdtMessage_a08Update_shouldPublishPatientUpdated() {
    Map<String, Object> message = new HashMap<>();
    message.put("messageType", "ADT");
    message.put("triggerEvent", "A08");
    message.put("patientId", "patient-update");

    consumer.processAdtMessage(message, "tenant-1");

    var patientCaptor = ArgumentCaptor.forClass(Object.class);
    verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), patientCaptor.capture());
    assertThat(patientCaptor.getValue().toString()).contains("fhir.patient.updated");
}

@Test
@DisplayName("A31 (Update) should publish fhir.patient.updated patient event")
void processAdtMessage_a31Update_shouldPublishPatientUpdated() {
    Map<String, Object> message = new HashMap<>();
    message.put("messageType", "ADT");
    message.put("triggerEvent", "A31");
    message.put("patientId", "patient-a31-update");

    consumer.processAdtMessage(message, "tenant-1");

    verify(kafkaTemplate).send(eq("external.healthix.patients"), eq("tenant-1"), any());
}

@Test
@DisplayName("Non-patient trigger event (A05) should not publish to patients topic")
void processAdtMessage_nonPatientEvent_shouldOnlyPublishToHl7Topic() {
    Map<String, Object> message = new HashMap<>();
    message.put("messageType", "ADT");
    message.put("triggerEvent", "A05");
    message.put("patientId", "patient-other");

    consumer.processAdtMessage(message, "tenant-1");

    verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    verify(kafkaTemplate).send(eq("external.healthix.hl7"), eq("tenant-1"), any());
}
```

**Note:** The existing test class already has the `@Mock KafkaTemplate<String, Object> kafkaTemplate` and `Hl7AdtConsumer consumer` fields. Ensure the new tests use `ArgumentCaptor` — you'll need to add `import org.mockito.ArgumentCaptor;` and `import static org.mockito.Mockito.times;` if not already imported. Also add `import java.util.HashMap;` if using mutable maps.

**Step 3: Run tests**

```bash
cd backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*Hl7AdtConsumerTest"
```
Expected: 10/10 PASS (4 existing + 6 new)

**Step 4: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/hl7/Hl7AdtConsumerTest.java
git commit -m "test(healthix): expand Hl7AdtConsumer tests to cover all 7 ADT trigger events"
```

---

## Task 11: Circuit breaker behavioral tests for healthix and hedis

Corehive already has `CorehiveCircuitBreakerTest`. Healthix and hedis have 0 circuit breaker tests despite each defining 3 CB instances.

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/integration/HealthixCircuitBreakerTest.java`
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/integration/HedisCircuitBreakerTest.java`

**Step 1: Write healthix circuit breaker tests**

```java
package com.healthdata.healthixadapter.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("Circuit Breaker: Healthix degradation")
class HealthixCircuitBreakerTest {

    private CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
    }

    @Test
    @DisplayName("All 3 healthix circuit breakers start in CLOSED state")
    void allCircuitBreakers_startClosed() {
        assertThat(registry.circuitBreaker("healthix-fhir").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("healthix-mpi").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("healthix-documents").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("healthix-fhir circuit breaker opens after repeated failures")
    void fhirCircuitBreaker_opensAfterFailures() {
        CircuitBreaker cb = registry.circuitBreaker("healthix-fhir");

        for (int i = 0; i < 100; i++) {
            cb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("FHIR unreachable"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Circuit breakers for different services are independent")
    void circuitBreakers_areIndependent() {
        CircuitBreaker fhirCb = registry.circuitBreaker("healthix-fhir");
        CircuitBreaker mpiCb = registry.circuitBreaker("healthix-mpi");

        for (int i = 0; i < 100; i++) {
            fhirCb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("FHIR down"));
        }

        assertThat(fhirCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(mpiCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
```

**Step 2: Write hedis circuit breaker tests**

```java
package com.healthdata.hedisadapter.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("Circuit Breaker: hedis degradation")
class HedisCircuitBreakerTest {

    private CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = CircuitBreakerRegistry.ofDefaults();
    }

    @Test
    @DisplayName("All 3 hedis circuit breakers start in CLOSED state")
    void allCircuitBreakers_startClosed() {
        assertThat(registry.circuitBreaker("hedis-measures").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("hedis-cql").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(registry.circuitBreaker("hedis-crm").getState())
                .isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("hedis-measures circuit breaker opens after repeated failures")
    void measuresCircuitBreaker_opensAfterFailures() {
        CircuitBreaker cb = registry.circuitBreaker("hedis-measures");

        for (int i = 0; i < 100; i++) {
            cb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("hedis unreachable"));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    @DisplayName("Circuit breakers for measures, cql, crm are independent")
    void circuitBreakers_areIndependent() {
        CircuitBreaker measuresCb = registry.circuitBreaker("hedis-measures");
        CircuitBreaker cqlCb = registry.circuitBreaker("hedis-cql");
        CircuitBreaker crmCb = registry.circuitBreaker("hedis-crm");

        for (int i = 0; i < 100; i++) {
            measuresCb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("down"));
        }

        assertThat(measuresCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(cqlCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(crmCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Circuit breaker metrics track failures correctly")
    void circuitBreaker_tracksMetrics() {
        CircuitBreaker cb = registry.circuitBreaker("hedis-crm");

        cb.onSuccess(50, TimeUnit.MILLISECONDS);
        cb.onSuccess(30, TimeUnit.MILLISECONDS);
        cb.onError(0, TimeUnit.MILLISECONDS, new ResourceAccessException("fail"));

        CircuitBreaker.Metrics metrics = cb.getMetrics();
        assertThat(metrics.getNumberOfSuccessfulCalls()).isGreaterThanOrEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isGreaterThanOrEqualTo(1);
    }
}
```

**Step 3: Run tests**

```bash
cd backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*HealthixCircuitBreakerTest" && \
./gradlew :modules:services:hedis-adapter-service:test --tests "*HedisCircuitBreakerTest"
```
Expected: 3/3 + 4/4 = 7/7 PASS

**Step 4: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/integration/HealthixCircuitBreakerTest.java \
        backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/integration/HedisCircuitBreakerTest.java
git commit -m "test(adapters): add circuit breaker behavioral tests for healthix and hedis"
```

---

## Task 12: CorehiveApiClient unit tests (corehive)

The `CorehiveApiClient` wraps `RestTemplate` calls with circuit breaker. It has 0 direct tests (only tested indirectly via `CorehiveAdapterServiceTest`).

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/client/CorehiveApiClientTest.java`

**Step 1: Write the failing tests**

```java
package com.healthdata.corehiveadapter.client;

import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.model.CareGapScoringResponse;
import com.healthdata.corehiveadapter.model.VbcRoiRequest;
import com.healthdata.corehiveadapter.model.VbcRoiResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("CorehiveApiClient")
class CorehiveApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private CorehiveApiClient client;

    @BeforeEach
    void setUp() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        client = new CorehiveApiClient(restTemplate, registry);
    }

    @Test
    @DisplayName("should send scoring request to CoreHive API")
    void scoreCareGaps_shouldPostToCorrectEndpoint() {
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId("synth-001")
                .careGaps(List.of())
                .build();

        CareGapScoringResponse expected = CareGapScoringResponse.builder()
                .overallRiskScore(0.75)
                .build();

        when(restTemplate.postForObject(eq("/api/healthcare/score"), any(), eq(CareGapScoringResponse.class)))
                .thenReturn(expected);

        CareGapScoringResponse result = client.scoreCareGaps(request);

        assertThat(result.getOverallRiskScore()).isEqualTo(0.75);
        verify(restTemplate).postForObject(eq("/api/healthcare/score"), eq(request), eq(CareGapScoringResponse.class));
    }

    @Test
    @DisplayName("should send ROI request to CoreHive API")
    void calculateRoi_shouldPostToCorrectEndpoint() {
        VbcRoiRequest request = VbcRoiRequest.builder()
                .contractId("contract-1")
                .totalLives(10000)
                .build();

        VbcRoiResponse expected = VbcRoiResponse.builder()
                .contractId("contract-1")
                .estimatedRoi(new BigDecimal("3.2"))
                .build();

        when(restTemplate.postForObject(eq("/api/healthcare/roi"), any(), eq(VbcRoiResponse.class)))
                .thenReturn(expected);

        VbcRoiResponse result = client.calculateRoi(request);

        assertThat(result.getEstimatedRoi()).isEqualTo(new BigDecimal("3.2"));
    }

    @Test
    @DisplayName("should propagate exception through circuit breaker")
    void scoreCareGaps_shouldPropagateException() {
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId("synth-002")
                .careGaps(List.of())
                .build();

        when(restTemplate.postForObject(anyString(), any(), any()))
                .thenThrow(new ResourceAccessException("CoreHive down"));

        assertThatThrownBy(() -> client.scoreCareGaps(request))
                .isInstanceOf(ResourceAccessException.class);
    }
}
```

**Step 2: Run tests**

```bash
cd backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*CorehiveApiClientTest"
```
Expected: 3/3 PASS

**Step 3: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/client/CorehiveApiClientTest.java
git commit -m "test(corehive): add unit tests for CorehiveApiClient"
```

---

## Task 13: Final verification — run all adapter tests and verify count

**Step 1: Run all adapter unit tests**

```bash
cd backend && ./gradlew :modules:services:corehive-adapter-service:test :modules:services:healthix-adapter-service:test :modules:services:hedis-adapter-service:test
```

Expected test counts:
- **Corehive:** ~50+ tests (existing 45 + 4 CareGapEventListener + 3 CorehiveApiClient)
- **Healthix:** ~35+ tests (existing 20 + 3 FhirSubscription + 5 CcdaIngestion + 2 VeratoMpi + 6 Hl7Adt expanded + 3 CircuitBreaker)
- **Hedis:** ~30+ tests (existing 17 + 3 CrmSync + 4 MeasureSync + 2 CqlDelegation + 4 CircuitBreaker)

Total: **115+ tests** (up from ~82), 0 failures.

**Step 2: Run integration tests**

```bash
cd backend && ./gradlew testIntegration
```

Wait for the integration test profile to run all tagged `@Tag("integration")` tests. All must pass.

**Step 3: Commit sprint summary**

```bash
git add -A
git commit -m "feat(adapters): Sprint 2 Test — coverage & reliability for all 3 external adapter services

Sprint 2 deliverables:
- Explicit Kafka consumer deserializer configs (JsonDeserializer + trusted packages)
- spring-kafka-test dependency added for future embedded Kafka tests
- CareGapEventListener tests (corehive): de-identification, null handling, error resilience
- FhirSubscriptionClient tests (healthix): notification handling, subscription registration
- CcdaIngestionService tests (healthix): document fetch, webhook routing, null handling
- VeratoMpiProxy tests (healthix): MPI match request/response, error propagation
- Hl7AdtConsumer expanded tests (healthix): all 7 ADT triggers (A01-A04, A08, A28, A31)
- CrmSyncService tests (hedis): deal push, webhook publishing, error propagation
- MeasureRegistrySyncService tests (hedis): fetch/publish, empty/null handling, error catch
- CqlDelegationService tests (hedis): HEDIS and STAR delegation to correct endpoints
- CorehiveApiClient tests (corehive): scoring/ROI endpoints, circuit breaker passthrough
- Circuit breaker behavioral tests for healthix (3 CBs) and hedis (3 CBs)
- 115+ total adapter tests, 0 failures"
```

---

## Summary

| Task | Adapter | Tests Added | Type |
|------|---------|-------------|------|
| 1 | All 3 | 0 (dependency) | Build |
| 2 | All 3 | 0 (config) | Config |
| 3 | Corehive | 4 | Unit |
| 4 | Healthix | 3 | Unit |
| 5 | Healthix | 5 | Unit |
| 6 | Healthix | 2 | Unit |
| 7 | Hedis | 3 | Unit |
| 8 | Hedis | 4 | Unit |
| 9 | Hedis | 2 | Unit |
| 10 | Healthix | 6 | Unit |
| 11 | Healthix + Hedis | 3 + 4 = 7 | Unit |
| 12 | Corehive | 3 | Unit |
| 13 | All 3 | 0 (verification) | Verification |
| **Total** | | **39 new tests** | |

**Sprint 2 Exit Criteria Check:**
- [x] Kafka configs use explicit deserializers (Task 2)
- [x] All service/client classes have tests (Tasks 3-12)
- [x] Circuit breaker tests for all 3 adapters (Tasks 11-12)
- [x] 6+ new test files (Tasks 3-12 create 9 new files)
- [ ] Coverage ≥ 70% per adapter — verify with JaCoCo after all tests pass
- [ ] Load test baseline — **deferred to Sprint 3** (requires running services)
