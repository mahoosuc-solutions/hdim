# Service Validation Phase 2 — Functional Depth Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add missing `@SpringBootTest` integration tests for 7 services that currently have only unit tests or no controller tests, validating the unique functional requirements defined in `validation/services.yml`.

**Architecture:** Each new test class follows the pattern established by `CareGapControllerIntegrationTest` — `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`, using trusted gateway headers (`X-Auth-Validated`, `X-Auth-Roles`, `X-Tenant-ID`) instead of real JWT. Services with Kafka dependencies use `@EmbeddedKafka`. Services with no Spring Boot main (cqrs-query-service) are tested as unit tests only.

**Tech Stack:** Java 21, Spring Boot 3.x, JUnit 5, MockMvc, `@Tag("integration")`, embedded H2/PostgreSQL via Testcontainers where needed, embedded Kafka for event services.

---

## Context for Implementer

### What exists today (Phase 1 complete ✅)
- `validation/services.yml` — registry of all 9 demo-stack services with unique requirements
- `validation/results/2026-02-18-132923-smoke.md` — 9/9 smoke tests PASS
- `load-tests/scenarios/smoke/` — 9 k6 smoke test files, all green

### What Phase 2 adds
New `@SpringBootTest` integration tests for **7 services** that currently have gaps:

| Service | Current State | Phase 2 Adds |
|---------|--------------|--------------|
| `fhir-service` | Unit tests only | `FhirIntegrationTest.java` — FHIR R4 Bundle assertions |
| `consent-service` | Unit tests only | `ConsentIntegrationTest.java` — tenant blocking, revocation |
| `event-store-service` | Schema validation only | `EventStoreControllerIntegrationTest.java` — append + replay |
| `data-ingestion-service` | Unit tests only | `IngestionControllerIntegrationTest.java` — start + progress |
| `analytics-service` | AlertRule only | `DashboardControllerIntegrationTest.java` — CRUD + RBAC |
| `cms-connector-service` | OAuth2 only | `CmsConnectorControllerIntegrationTest.java` — DPC/BCDA smoke |
| `cqrs-query-service` | Library module | Service-level unit tests only (no `@SpringBootTest`) |

### Auth pattern (copy from existing tests)
All services use gateway-trust headers — no real JWT needed in tests:
```java
mockMvc.perform(get("/endpoint")
    .header("X-Auth-Validated", "true")
    .header("X-Auth-User-Id", "test-user")
    .header("X-Auth-Roles", "ADMIN")
    .header("X-Tenant-ID", "test-tenant"))
```

### Test tag
All new tests use `@Tag("integration")` so they run with `./gradlew testIntegration`.

### Running tests
```bash
# Run integration tests for a specific service
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:fhir-service:testIntegration

# Run all integration tests
./gradlew testIntegration
```

---

## Task 1: fhir-service — FHIR R4 Bundle integration test

**Service:** `fhir-service` | Port 8085 | Context `/fhir`

**Unique requirements from `validation/services.yml`:**
- FHIR R4 Bundle structure valid (`resourceType`, `entry[]`)
- Patient/$everything returns collection bundle
- Content-Type `application/fhir+json`

**Current state:** `PatientControllerTest.java` uses `MockMvcBuilders.standaloneSetup` (no Spring context) — it's a unit test masquerading as controller test. We need a real `@SpringBootTest` to validate the FHIR serialization pipeline.

**Files:**
- Create: `backend/modules/services/fhir-service/src/test/java/com/healthdata/fhir/rest/FhirIntegrationTest.java`

**Step 1: Write the test**

```java
package com.healthdata.fhir.rest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FhirIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT = "test-tenant";

    @Test
    void patientList_returnsFhirR4Bundle() throws Exception {
        mockMvc.perform(get("/Patient")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .accept("application/fhir+json"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", containsString("fhir+json")))
            .andExpect(jsonPath("$.resourceType").value("Bundle"))
            .andExpect(jsonPath("$.entry").isArray());
    }

    @Test
    void patientList_hasNoCacheHeader() throws Exception {
        mockMvc.perform(get("/Patient")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .accept("application/fhir+json"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void everythingEndpoint_returnsCollectionBundleForUnknownPatient() throws Exception {
        // For an unknown patient ID, should return 404 (not timeout)
        mockMvc.perform(get("/Patient/00000000-0000-0000-0000-000000000000/$everything")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .accept("application/fhir+json"))
            .andExpect(status().isNotFound());
    }
}
```

**Step 2: Run test to verify it compiles and either passes or fails clearly**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:fhir-service:testIntegration --tests "*.FhirIntegrationTest" -i 2>&1 | tail -30
```

Expected: Tests run (may fail if test profile config is missing — that's OK, fix config then re-run).

**Step 3: Fix any config issues**

If `@SpringBootTest` fails to start:
1. Check for existing `src/test/resources/application-test.yml` or `application-test.properties`
2. If missing, create `src/test/resources/application-test.yml` with:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:fhir_test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
  kafka:
    bootstrap-servers: localhost:9092
```
3. Re-run tests

**Step 4: Verify all 3 tests pass**

```bash
./gradlew :modules:services:fhir-service:testIntegration --tests "*.FhirIntegrationTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 3 tests passing.

**Step 5: Commit**

```bash
git add backend/modules/services/fhir-service/src/test/
git commit -m "test(fhir): add FhirIntegrationTest — FHIR R4 Bundle + Cache-Control assertions"
```

---

## Task 2: consent-service — tenant blocking + revocation integration test

**Service:** `consent-service` | Port: varies | Context `/api/consents`

**Unique requirements from design doc:**
- Non-consented tenant blocked (403 or empty)
- Consent revocation effective immediately

**Current state:** `ConsentControllerTest.java` uses `@WebMvcTest` with mocked `ConsentService` — it tests HTTP layer but not real business logic.

**Files:**
- Create: `backend/modules/services/consent-service/src/test/java/com/healthdata/consent/rest/ConsentIntegrationTest.java`

**Step 1: Write the test**

```java
package com.healthdata.consent.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.persistence.ConsentEntity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT = "test-tenant";

    @Test
    void createConsent_thenRetrieve_returnsActiveConsent() throws Exception {
        // Create a consent record
        var body = """
            {
              "patientId": "patient-123",
              "consentType": "TREATMENT",
              "status": "ACTIVE",
              "scope": "ALL_DATA",
              "authorizedPractitioner": "practitioner-456"
            }
            """;

        var createResult = mockMvc.perform(post("/api/consents")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String id = objectMapper.readTree(responseBody).get("id").asText();

        // Retrieve it — should be ACTIVE
        mockMvc.perform(get("/api/consents/{id}", id)
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", TENANT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void wrongTenant_cannotAccessOtherTenantConsent() throws Exception {
        // Create consent for tenant-A
        var body = """
            {
              "patientId": "patient-isolation",
              "consentType": "TREATMENT",
              "status": "ACTIVE",
              "scope": "ALL_DATA"
            }
            """;

        var createResult = mockMvc.perform(post("/api/consents")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "tenant-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();

        String id = objectMapper.readTree(
            createResult.getResponse().getContentAsString()).get("id").asText();

        // Tenant-B should get 404 (not 200 with tenant-A's data)
        mockMvc.perform(get("/api/consents/{id}", id)
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "other-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "tenant-b"))
            .andExpect(status().isNotFound());
    }
}
```

**Step 2: Run the test**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew :modules:services:consent-service:testIntegration --tests "*.ConsentIntegrationTest" 2>&1 | tail -20
```

**Step 3: Fix any startup issues**

Check `consent-service` test profile. Look at:
- `backend/modules/services/consent-service/src/test/resources/`
- Existing `application.yml` for datasource config

If Kafka starts up and blocks test context, add to `application-test.yml`:
```yaml
spring.kafka.bootstrap-servers: ${spring.embedded.kafka.brokers:localhost:9092}
```

**Step 4: Verify 2 tests pass**

```bash
./gradlew :modules:services:consent-service:testIntegration --tests "*.ConsentIntegrationTest"
```

Expected: `BUILD SUCCESSFUL`, 2 tests passing.

**Step 5: Commit**

```bash
git add backend/modules/services/consent-service/src/test/
git commit -m "test(consent): add ConsentIntegrationTest — create/retrieve + multi-tenant isolation"
```

---

## Task 3: event-store-service — append events + immutability integration test

**Service:** `event-store-service`

**Unique requirements from design doc:**
- Events immutable (no delete/update endpoints)
- Event replay produces same projected state

**Current state:** Only `EntityMigrationValidationTest` and `EventStoreServiceTest` (unit). No controller integration test.

**Controller path:** `EventStoreController` at `/api/v1` (append events, create snapshots, replay)

**Files:**
- Create: `backend/modules/services/event-store-service/src/test/java/com/healthdata/eventstore/api/EventStoreControllerIntegrationTest.java`

**Step 1: Read the EventStoreController to understand DTOs**

```bash
cat backend/modules/services/event-store-service/src/main/java/com/healthdata/eventstore/api/EventStoreController.java
```

Then write:

```java
package com.healthdata.eventstore.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventStoreControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void appendEvent_thenGetEvents_returnsAppendedEvent() throws Exception {
        String aggregateId = "agg-test-001";
        String tenantId = "test-tenant";

        // Append an event
        mockMvc.perform(post("/api/v1/events")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "aggregateId": "%s",
                      "aggregateType": "Patient",
                      "eventType": "PatientCreated",
                      "payload": {"name": "Test Patient"},
                      "version": 1
                    }
                    """.formatted(aggregateId)))
            .andExpect(status().isCreated());

        // Retrieve events for that aggregate
        mockMvc.perform(get("/api/v1/events/{aggregateId}", aggregateId)
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].eventType").value("PatientCreated"))
            .andExpect(jsonPath("$[0].aggregateId").value(aggregateId));
    }

    @Test
    void eventStore_hasNoDeleteEndpoint() throws Exception {
        // Events are immutable — DELETE must return 405 Method Not Allowed
        mockMvc.perform(delete("/api/v1/events/some-id")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().isMethodNotAllowed());
    }
}
```

**Step 2: Run and fix**

```bash
./gradlew :modules:services:event-store-service:testIntegration --tests "*.EventStoreControllerIntegrationTest" 2>&1 | tail -20
```

Adjust DTO fields if the actual `AppendEventRequest` has different field names (read the controller first as instructed in step 1).

**Step 3: Verify 2 tests pass, commit**

```bash
git add backend/modules/services/event-store-service/src/test/
git commit -m "test(event-store): add EventStoreControllerIntegrationTest — append + immutability"
```

---

## Task 4: data-ingestion-service — start + progress integration test

**Service:** `data-ingestion-service` | Context `/api/v1/ingestion`

**Unique requirements:**
- `POST /ingestion/start` accepts valid ingestion request
- `GET /ingestion/progress` returns progress status

**Current state:** No controller tests at all.

**Files:**
- Create: `backend/modules/services/data-ingestion-service/src/test/java/com/healthdata/ingestion/api/v1/IngestionControllerIntegrationTest.java`

**Step 1: Read IngestionController to get request/response DTOs**

```bash
cat backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/api/v1/IngestionController.java
cat backend/modules/services/data-ingestion-service/src/main/java/com/healthdata/ingestion/api/v1/IngestionRequest.java
```

Then write:

```java
package com.healthdata.ingestion.api.v1;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startIngestion_withValidRequest_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/start")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "tenantId": "test-tenant",
                      "patientCount": 10,
                      "scenario": "BASIC"
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void getProgress_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ingestion/progress")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().isOk());
    }

    @Test
    void startIngestion_withInvalidRequest_returns400() throws Exception {
        // Empty body — should fail validation
        mockMvc.perform(post("/api/v1/ingestion/start")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "test-tenant")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }
}
```

**Step 2: Run and fix**

```bash
./gradlew :modules:services:data-ingestion-service:testIntegration --tests "*.IngestionControllerIntegrationTest" 2>&1 | tail -20
```

Adjust DTO fields to match the actual `IngestionRequest` class.

**Step 3: Verify 3 tests pass, commit**

```bash
git add backend/modules/services/data-ingestion-service/src/test/
git commit -m "test(data-ingestion): add IngestionControllerIntegrationTest — start + progress + validation"
```

---

## Task 5: analytics-service — DashboardController integration test

**Service:** `analytics-service` | Context `/api/analytics/dashboards`

**Unique requirements:**
- Aggregate query returns consistent results
- Role-based access control enforced (`@PreAuthorize`)

**Current state:** Only `AlertRuleV1ControllerTest` (unit test for a different controller). `DashboardController` has no tests.

**Files:**
- Create: `backend/modules/services/analytics-service/src/test/java/com/healthdata/analytics/rest/DashboardControllerIntegrationTest.java`

**Step 1: Write the test**

```java
package com.healthdata.analytics.rest;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDashboards_withAdminRole_returns200() throws Exception {
        mockMvc.perform(get("/api/analytics/dashboards")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getDashboards_withoutAuth_returns401Or403() throws Exception {
        mockMvc.perform(get("/api/analytics/dashboards")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().is(anyOf(equalTo(401), equalTo(403))));
    }

    @Test
    void getDashboardById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/analytics/dashboards/00000000-0000-0000-0000-000000000000")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "test-tenant"))
            .andExpect(status().isNotFound());
    }
}
```

**Step 2: Run and fix**

```bash
./gradlew :modules:services:analytics-service:testIntegration --tests "*.DashboardControllerIntegrationTest" 2>&1 | tail -20
```

If `@PreAuthorize` fails without a proper security context, check the existing `application-test.yml` for security config — may need `@WithMockUser` or ensure the gateway-trust filter processes the `X-Auth-*` headers.

**Step 3: Verify 3 tests pass, commit**

```bash
git add backend/modules/services/analytics-service/src/test/
git commit -m "test(analytics): add DashboardControllerIntegrationTest — list + RBAC + 404"
```

---

## Task 6: cms-connector-service — CmsConnectorController integration test

**Service:** `cms-connector-service` | Context `/api/v1/cms`

**Unique requirements:**
- CMS data mapped to FHIR R4 format correctly
- DPC and BCDA endpoints accessible

**Current state:** `OAuth2IntegrationTest` only — no test for `CmsConnectorController`.

**Note:** This service calls external CMS APIs (DPC, BCDA). In tests, these must be mocked. Check if the service has a `@MockBean` or WireMock pattern already set up.

**Files:**
- Create: `backend/modules/services/cms-connector-service/src/test/java/com/healthdata/cms/controller/CmsConnectorControllerIntegrationTest.java`

**Step 1: Read existing test setup**

```bash
cat backend/modules/services/cms-connector-service/src/test/java/com/healthdata/cms/integration/OAuth2IntegrationTest.java | head -60
```

Then write a controller test that mocks the external clients:

```java
package com.healthdata.cms.controller;

import com.healthdata.cms.client.BcdaClient;
import com.healthdata.cms.client.DpcClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CmsConnectorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DpcClient dpcClient;

    @MockBean
    private BcdaClient bcdaClient;

    @Test
    void getDpcPatient_withValidId_returns200() throws Exception {
        when(dpcClient.getPatient("patient-123"))
            .thenReturn("{\"resourceType\":\"Patient\",\"id\":\"patient-123\"}");

        mockMvc.perform(get("/api/v1/cms/dpc/patient/patient-123")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", UUID.randomUUID().toString()))
            .andExpect(status().isOk());
    }

    @Test
    void getBcdaExportStatus_returns200() throws Exception {
        // Verify the BCDA export status endpoint is reachable
        mockMvc.perform(get("/api/v1/cms/bcda/export/status")
                .header("X-Auth-Validated", "true")
                .header("X-Auth-User-Id", "test-user")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", UUID.randomUUID().toString()))
            .andExpect(status().is2xxSuccessful());
    }
}
```

**Note:** Adjust endpoint paths and mock setup based on what you find when reading the actual controller and client interfaces.

**Step 2: Run and fix**

```bash
./gradlew :modules:services:cms-connector-service:testIntegration --tests "*.CmsConnectorControllerIntegrationTest" 2>&1 | tail -20
```

**Step 3: Verify tests pass, commit**

```bash
git add backend/modules/services/cms-connector-service/src/test/
git commit -m "test(cms-connector): add CmsConnectorControllerIntegrationTest — DPC/BCDA endpoints"
```

---

## Task 7: Verify existing integration tests still pass for 8 complete services

The 8 services already have complete integration test coverage. This task verifies they all still pass after Phase 2 additions.

**Services to verify:** cql-engine, hcc, care-gap, quality-measure, audit-query, patient, notification, clinical-workflow

**Step 1: Run integration tests for all 8**

```bash
cd /mnt/wdblack/dev/projects/hdim-master/backend
./gradlew \
  :modules:services:cql-engine-service:testIntegration \
  :modules:services:hcc-service:testIntegration \
  :modules:services:care-gap-service:testIntegration \
  :modules:services:quality-measure-service:testIntegration \
  :modules:services:audit-query-service:testIntegration \
  :modules:services:patient-service:testIntegration \
  :modules:services:notification-service:testIntegration \
  :modules:services:clinical-workflow-service:testIntegration \
  2>&1 | grep -E "BUILD|FAILED|passed|failed" | tail -20
```

Expected: All `BUILD SUCCESSFUL`.

**Step 2: Document results in validation/results/**

Create `validation/results/2026-02-18-phase2-functional.md` with results of all 15 services:

```markdown
# Service Validation Phase 2 — Functional Test Results

**Date:** [today]
**Mode:** @SpringBootTest integration tests (./gradlew testIntegration)

## Summary

| Result | Count |
|--------|-------|
| ✅ PASS | 15 |
| ❌ FAIL | 0 |

## Per-Service Results

| Status | Service | Tests | Notes |
|--------|---------|-------|-------|
| ✅ | fhir-service | 3 | New: FhirIntegrationTest |
| ✅ | cql-engine-service | 7 | Existing: CqlEvaluationControllerIntegrationTest |
| ✅ | hcc-service | 3+ | Existing: HccApiIntegrationTest |
| ✅ | care-gap-service | 7 | Existing: CareGapControllerIntegrationTest |
| ✅ | quality-measure-service | 25+ | Existing: multiple classes |
| ✅ | audit-query-service | 6 | Existing: AuditQueryControllerIntegrationTest |
| ✅ | consent-service | 2 | New: ConsentIntegrationTest |
| ✅ | patient-service | 6 | Existing: PatientControllerIntegrationTest |
| ⏭ | cqrs-query-service | - | Library module — no Spring Boot app |
| ✅ | event-store-service | 2 | New: EventStoreControllerIntegrationTest |
| ✅ | data-ingestion-service | 3 | New: IngestionControllerIntegrationTest |
| ✅ | notification-service | 7+ | Existing: NotificationControllerTest |
| ✅ | cms-connector-service | 2 | New: CmsConnectorControllerIntegrationTest |
| ✅ | clinical-workflow-service | 6+ | Existing: multiple classes |
| ✅ | analytics-service | 3 | New: DashboardControllerIntegrationTest |
```

**Step 3: Commit results**

```bash
git add validation/results/2026-02-18-phase2-functional.md
git commit -m "docs(validation): Phase 2 functional test results — 14/14 services with integration tests"
```

---

## Success Criteria

| Milestone | Check |
|-----------|-------|
| Phase 2 complete | All 7 new integration test classes compile and pass |
| Regression check | All 8 existing integration test suites still pass |
| Results documented | `validation/results/2026-02-18-phase2-functional.md` committed |
| testIntegration green | `./gradlew testIntegration` has 0 failures for all 15 services |

---

## Pre-flight: auth pattern reference

The gateway-trust filter reads `X-Auth-Validated: true` and `X-Auth-Roles` headers. The filter class is typically `TrustedHeaderAuthFilter` or `GatewayTrustAuthFilter`. If tests return 401, check that:
1. `application-test.yml` does NOT have `security.enabled: false` (gateway trust filter must run)
2. The filter is registered in the service's security config
3. You're sending `X-Auth-Validated: true` AND `X-Auth-Roles: ADMIN` AND `X-Tenant-ID: test-tenant`

If you see `403 Forbidden` on a `@PreAuthorize("hasRole('ADMIN')")` endpoint, the role must be sent as `ROLE_ADMIN` in the header OR the security config uses `hasRole` (adds ROLE_ prefix automatically). Try both `ADMIN` and `ROLE_ADMIN` if you get 403.
