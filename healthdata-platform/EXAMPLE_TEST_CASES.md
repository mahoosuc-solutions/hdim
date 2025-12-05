# HealthData Platform - Example Test Cases

This document provides complete, runnable example test cases for all testing scenarios using the test infrastructure.

## Table of Contents

1. [Repository Tests](#repository-tests)
2. [Service Tests](#service-tests)
3. [Integration Tests](#integration-tests)
4. [REST Controller Tests](#rest-controller-tests)
5. [Security Tests](#security-tests)
6. [Async Tests](#async-tests)

---

## Repository Tests

### Example 1: Basic Repository CRUD Test

```java
package com.healthdata.patient.test;

import com.healthdata.BaseRepositoryTest;
import com.healthdata.patient.domain.PatientEntity;
import com.healthdata.patient.persistence.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

/**
 * Test PatientRepository CRUD operations
 */
@DataJpaTest
public class PatientRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    public void testSaveAndRetrievePatient() {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setEmail("john.doe@example.com");
        patient.setDateOfBirth(LocalDate.of(1980, 1, 15));

        // When
        PatientEntity saved = patientRepository.save(patient);

        // Then
        assertNotNull(saved, "Patient should be saved");
        assertNotNull(saved.getId(), "Patient should have ID");

        PatientEntity retrieved = patientRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved, "Patient should be retrievable");
        assertEqual("John", retrieved.getFirstName(), "First name should match");
        assertEqual("john.doe@example.com", retrieved.getEmail(), "Email should match");
    }

    @Test
    public void testFindPatientByEmail() {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setEmail("jane.smith@example.com");
        patient.setFirstName("Jane");
        patientRepository.save(patient);

        // When
        PatientEntity found = patientRepository.findByEmail("jane.smith@example.com");

        // Then
        assertNotNull(found, "Patient should be found by email");
        assertEqual("Jane", found.getFirstName(), "First name should match");
    }

    @Test
    public void testUpdatePatient() {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setFirstName("Original");
        PatientEntity saved = patientRepository.save(patient);

        // When
        saved.setFirstName("Updated");
        patientRepository.save(saved);

        // Then
        PatientEntity updated = patientRepository.findById(saved.getId()).orElse(null);
        assertEqual("Updated", updated.getFirstName(), "Name should be updated");
    }

    @Test
    public void testDeletePatient() {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setFirstName("ToDelete");
        PatientEntity saved = patientRepository.save(patient);
        Long id = saved.getId();

        // When
        patientRepository.deleteById(id);

        // Then
        boolean exists = patientRepository.existsById(id);
        assertFalse(exists, "Patient should be deleted");
    }
}
```

### Example 2: Complex Repository Query Test

```java
package com.healthdata.quality.test;

import com.healthdata.BaseRepositoryTest;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Test QualityMeasureResultRepository complex queries
 */
@DataJpaTest
public class QualityMeasureResultRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private QualityMeasureResultRepository resultRepository;

    @Test
    public void testFindResultsByPatientAndMeasure() {
        // Given
        QualityMeasureResultEntity result1 = new QualityMeasureResultEntity();
        result1.setPatientId("patient-123");
        result1.setMeasureId("measure-001");
        result1.setStatus("COMPLIANT");
        result1.setCalculatedAt(LocalDateTime.now());
        resultRepository.save(result1);

        QualityMeasureResultEntity result2 = new QualityMeasureResultEntity();
        result2.setPatientId("patient-123");
        result2.setMeasureId("measure-002");
        result2.setStatus("NON_COMPLIANT");
        result2.setCalculatedAt(LocalDateTime.now());
        resultRepository.save(result2);

        // When
        List<QualityMeasureResultEntity> results = resultRepository
            .findByPatientIdAndMeasureId("patient-123", "measure-001");

        // Then
        assertNotNull(results, "Results should not be null");
        assertTrue(results.size() >= 1, "Should have at least one result");
        assertEqual("COMPLIANT", results.get(0).getStatus(), "Status should match");
    }

    @Test
    public void testFindNonCompliantResults() {
        // Given
        QualityMeasureResultEntity compliant = new QualityMeasureResultEntity();
        compliant.setStatus("COMPLIANT");
        resultRepository.save(compliant);

        QualityMeasureResultEntity nonCompliant = new QualityMeasureResultEntity();
        nonCompliant.setStatus("NON_COMPLIANT");
        resultRepository.save(nonCompliant);

        // When
        List<QualityMeasureResultEntity> results = resultRepository
            .findByStatus("NON_COMPLIANT");

        // Then
        assertTrue(results.stream()
            .allMatch(r -> "NON_COMPLIANT".equals(r.getStatus())),
            "All results should be non-compliant");
    }
}
```

---

## Service Tests

### Example 1: Basic Service Test with Mocks

```java
package com.healthdata.patient.test;

import com.healthdata.BaseServiceTest;
import com.healthdata.patient.domain.PatientEntity;
import com.healthdata.patient.dto.PatientDTO;
import com.healthdata.patient.persistence.PatientRepository;
import com.healthdata.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test PatientService with mocked repository
 */
@SpringBootTest
public class PatientServiceTest extends BaseServiceTest {

    @Autowired
    private PatientService patientService;

    @MockBean
    private PatientRepository patientRepository;

    @Test
    public void testGetPatientById() {
        // Given
        Long patientId = 123L;
        PatientEntity patientEntity = new PatientEntity();
        patientEntity.setId(patientId);
        patientEntity.setFirstName("John");
        patientEntity.setLastName("Doe");

        when(patientRepository.findById(patientId))
            .thenReturn(Optional.of(patientEntity));

        // When
        PatientDTO result = patientService.getPatientById(patientId);

        // Then
        assertNotNull(result, "Result should not be null");
        assertEqual("John", result.getFirstName(), "First name should match");
        assertEqual("Doe", result.getLastName(), "Last name should match");
        verify(patientRepository, times(1)).findById(patientId);
    }

    @Test
    public void testCreatePatient() {
        // Given
        PatientDTO newPatient = new PatientDTO();
        newPatient.setFirstName("Jane");
        newPatient.setLastName("Smith");
        newPatient.setEmail("jane@example.com");

        PatientEntity savedEntity = new PatientEntity();
        savedEntity.setId(1L);
        savedEntity.setFirstName("Jane");
        savedEntity.setLastName("Smith");
        savedEntity.setEmail("jane@example.com");

        when(patientRepository.save(any(PatientEntity.class)))
            .thenReturn(savedEntity);

        // When
        PatientDTO result = patientService.createPatient(newPatient);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getId(), "Result should have ID");
        assertEqual("Jane", result.getFirstName(), "First name should match");
        verify(patientRepository, times(1)).save(any(PatientEntity.class));
    }

    @Test
    public void testPatientNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(patientRepository.findById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then
        try {
            patientService.getPatientById(nonExistentId);
            assertTrue(false, "Should throw exception for non-existent patient");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("not found"), "Error message should be clear");
        }
    }
}
```

### Example 2: Service Test with Async Operations

```java
package com.healthdata.notification.test;

import com.healthdata.BaseServiceTest;
import com.healthdata.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * Test NotificationService async behavior
 */
@SpringBootTest
public class NotificationServiceAsyncTest extends BaseServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    public void testAsyncNotificationSending() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        String patientId = "patient-123";
        String message = "Your test results are ready";

        // When
        notificationService.sendAsyncNotification(patientId, message, () -> latch.countDown());

        // Then - Wait for async operation to complete
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "Async operation should complete within timeout");
    }

    @Test
    public void testBatchNotificationProcessing() {
        // Given
        int batchSize = 10;

        // When
        notificationService.processBatch(batchSize);

        // Then
        // Verify batch was processed
        waitSeconds(2); // Allow time for batch processing
        assertTrue(notificationService.isBatchCompleted(), "Batch should be completed");
    }
}
```

---

## Integration Tests

### Example 1: Basic Integration Test

```java
package com.healthdata.patient.test;

import com.healthdata.BaseIntegrationTest;
import com.healthdata.patient.domain.PatientEntity;
import com.healthdata.patient.dto.PatientDTO;
import com.healthdata.patient.persistence.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for patient functionality
 */
@SpringBootTest
public class PatientIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    public void testPatientLifecycle() throws Exception {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setFirstName("Integration");
        patient.setLastName("Test");
        patient.setEmail("integration@test.com");
        patientRepository.save(patient);

        // When
        MvcResult result = performGet("/api/patients/" + patient.getId());

        // Then
        assertSuccessResponse(result);
        PatientDTO dto = parseResponseContent(result, PatientDTO.class);
        assertNotNull(dto, "DTO should not be null");
        assertEqual("Integration", dto.getFirstName(), "First name should match");
    }

    @Test
    public void testListAllPatients() throws Exception {
        // Given
        for (int i = 0; i < 5; i++) {
            PatientEntity p = new PatientEntity();
            p.setFirstName("Patient" + i);
            patientRepository.save(p);
        }

        // When
        MvcResult result = performGet("/api/patients");

        // Then
        assertSuccessResponse(result);
        String content = getResponseContent(result);
        assertTrue(content.contains("Patient"), "Response should contain patients");
    }
}
```

### Example 2: Integration Test with Database Setup

```java
package com.healthdata.quality.test;

import com.healthdata.BaseIntegrationTest;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for quality measure calculations
 */
@SpringBootTest
public class QualityMeasureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private QualityMeasureResultRepository resultRepository;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // Setup test data before each test
        for (int i = 0; i < 3; i++) {
            QualityMeasureResultEntity result = new QualityMeasureResultEntity();
            result.setPatientId("patient-" + i);
            result.setMeasureId("measure-001");
            result.setStatus(i % 2 == 0 ? "COMPLIANT" : "NON_COMPLIANT");
            resultRepository.save(result);
        }
    }

    @Test
    public void testGetMeasureResults() throws Exception {
        // When
        MvcResult result = performGet("/api/measures/results");

        // Then
        assertSuccessResponse(result);
        assertJsonContentType(result);
        assertJsonFieldExists(result, "[0].patientId");
    }

    @Test
    public void testCalculateMeasureForPatient() throws Exception {
        // When
        MvcResult result = performPost(
            "/api/measures/calculate",
            toJson(new CalculateRequest("patient-0", "measure-001"))
        );

        // Then
        assertOkStatus(result);
        assertJsonFieldValue(result, "status", "SUCCESS");
    }
}
```

---

## REST Controller Tests

### Example 1: Basic REST Controller Test

```java
package com.healthdata.patient.test;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.patient.domain.PatientEntity;
import com.healthdata.patient.dto.PatientDTO;
import com.healthdata.patient.persistence.PatientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test PatientController REST endpoints
 */
@SpringBootTest
public class PatientControllerTest extends BaseWebControllerTest {

    @Autowired
    private PatientRepository patientRepository;

    @Test
    public void testListPatients() throws Exception {
        // Given
        PatientEntity p1 = new PatientEntity();
        p1.setFirstName("John");
        patientRepository.save(p1);

        // When
        MvcResult result = performGet("/api/patients");

        // Then
        assertOkStatus(result);
        assertJsonContentType(result);
        String content = getResponseContent(result);
        assertTrue(content.contains("John"), "Response should contain patient");
    }

    @Test
    public void testGetPatientById() throws Exception {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patientRepository.save(patient);

        // When
        MvcResult result = performGet("/api/patients/" + patient.getId());

        // Then
        assertOkStatus(result);
        PatientDTO dto = parseResponseContent(result, PatientDTO.class);
        assertEqual("Jane", dto.getFirstName(), "First name should match");
        assertEqual("Doe", dto.getLastName(), "Last name should match");
    }

    @Test
    public void testCreatePatient() throws Exception {
        // Given
        PatientDTO newPatient = new PatientDTO();
        newPatient.setFirstName("Alice");
        newPatient.setLastName("Smith");
        newPatient.setEmail("alice@example.com");

        // When
        MvcResult result = performPost("/api/patients", toJson(newPatient));

        // Then
        assertCreatedStatus(result);
        String location = assertLocationHeaderPresent(result);
        assertTrue(location.contains("/api/patients/"), "Location should contain patient ID");

        PatientDTO created = parseResponseContent(result, PatientDTO.class);
        assertNotNull(created.getId(), "Created patient should have ID");
    }

    @Test
    public void testUpdatePatient() throws Exception {
        // Given
        PatientEntity patient = new PatientEntity();
        patient.setFirstName("Bob");
        patientRepository.save(patient);

        PatientDTO updateDto = new PatientDTO();
        updateDto.setFirstName("Robert");

        // When
        MvcResult result = performPut("/api/patients/" + patient.getId(), toJson(updateDto));

        // Then
        assertOkStatus(result);
        PatientDTO updated = parseResponseContent(result, PatientDTO.class);
        assertEqual("Robert", updated.getFirstName(), "Name should be updated");
    }

    @Test
    public void testDeletePatient() throws Exception {
        // Given
        PatientEntity patient = new PatientEntity();
        patientRepository.save(patient);

        // When
        MvcResult result = performDelete("/api/patients/" + patient.getId());

        // Then
        assertNoContentStatus(result);
        boolean exists = patientRepository.existsById(patient.getId());
        assertFalse(exists, "Patient should be deleted");
    }

    @Test
    public void testPatientNotFound() throws Exception {
        // When
        MvcResult result = performGet("/api/patients/99999");

        // Then
        assertNotFoundStatus(result);
    }

    @Test
    public void testInvalidPatientData() throws Exception {
        // Given
        PatientDTO invalidPatient = new PatientDTO();
        // Missing required fields

        // When
        MvcResult result = performPost("/api/patients", toJson(invalidPatient));

        // Then
        assertBadRequestStatus(result);
    }
}
```

### Example 2: REST Controller with Pagination Test

```java
package com.healthdata.caregap.test;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.caregap.dto.CareGapDTO;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

/**
 * Test CareGapController with pagination
 */
@SpringBootTest
public class CareGapControllerTest extends BaseWebControllerTest {

    @Autowired
    private CareGapRepository careGapRepository;

    @Test
    public void testGetCareGapsWithPagination() throws Exception {
        // Given
        for (int i = 0; i < 15; i++) {
            CareGapEntity careGap = new CareGapEntity();
            careGap.setPatientId("patient-" + i);
            careGap.setGapType("TYPE_" + (i % 3));
            careGapRepository.save(careGap);
        }

        // When
        MvcResult result = performGet("/api/care-gaps?page=0&size=10");

        // Then
        assertOkStatus(result);
        assertJsonFieldExists(result, "content");
        assertJsonFieldExists(result, "totalElements");
        String totalElements = getJsonFieldValue(result, "totalElements");
        assertEqual("15", totalElements, "Should have 15 total care gaps");
    }

    @Test
    public void testFilterCareGaps() throws Exception {
        // Given
        for (int i = 0; i < 5; i++) {
            CareGapEntity careGap = new CareGapEntity();
            careGap.setGapType("URGENT");
            careGapRepository.save(careGap);
        }

        // When
        MvcResult result = performGet("/api/care-gaps?type=URGENT");

        // Then
        assertOkStatus(result);
        String content = getResponseContent(result);
        assertTrue(content.contains("URGENT"), "Response should contain URGENT gaps");
    }
}
```

---

## Security Tests

### Example 1: Authentication and Authorization Tests

```java
package com.healthdata.security.test;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.HealthDataTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test authentication and authorization
 */
@SpringBootTest
public class SecurityControllerTest extends BaseWebControllerTest {

    @Test
    public void testUnauthorizedAccess() throws Exception {
        // When
        MvcResult result = performGet("/api/admin/users");

        // Then
        assertUnauthorizedStatus(result);
    }

    @Test
    public void testAuthorizedAccess() throws Exception {
        // Given
        String token = HealthDataTestConfiguration.TestDataConfig.TEST_JWT_TOKEN;

        // When
        MvcResult result = performGetWithAuth("/api/admin/users", token);

        // Then
        assertOkStatus(result);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminRoleAccess() throws Exception {
        // When
        MvcResult result = performGet("/api/admin/settings");

        // Then
        assertOkStatus(result);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testUserCannotAccessAdmin() throws Exception {
        // When
        MvcResult result = performGet("/api/admin/settings");

        // Then
        assertForbiddenStatus(result);
    }

    @Test
    public void testInvalidTokenRejected() throws Exception {
        // When
        MvcResult result = performGetWithAuth("/api/patients", "invalid-token");

        // Then
        assertUnauthorizedStatus(result);
    }
}
```

### Example 2: Role-Based Access Control Test

```java
package com.healthdata.rbac.test;

import com.healthdata.BaseWebControllerTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test role-based access control
 */
@SpringBootTest
public class RoleBasedAccessControlTest extends BaseWebControllerTest {

    @Test
    @WithMockUser(roles = "PROVIDER")
    public void testProviderCanViewPatients() throws Exception {
        // When
        MvcResult result = performGet("/api/patients");

        // Then
        assertOkStatus(result);
    }

    @Test
    @WithMockUser(roles = "PROVIDER")
    public void testProviderCannotDeletePatients() throws Exception {
        // When
        MvcResult result = performDelete("/api/patients/123");

        // Then
        assertForbiddenStatus(result);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testAdminCanDeletePatients() throws Exception {
        // When
        MvcResult result = performDelete("/api/patients/123");

        // Then
        // Either 204 (successful delete) or 404 (patient not found)
        int status = getStatusCode(result);
        assertTrue(status == 204 || status == 404,
            "Admin should be able to attempt deletion");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    public void testViewerHasReadOnlyAccess() throws Exception {
        // Viewer can read
        MvcResult readResult = performGet("/api/patients");
        assertOkStatus(readResult);

        // Viewer cannot create
        String newPatient = toJson(new PatientDTO());
        MvcResult createResult = performPost("/api/patients", newPatient);
        assertForbiddenStatus(createResult);
    }
}
```

---

## Async Tests

### Example 1: Async Operation Test with CountDownLatch

```java
package com.healthdata.async.test;

import com.healthdata.BaseServiceTest;
import com.healthdata.batch.service.BatchProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test async batch processing
 */
@SpringBootTest
public class BatchProcessingAsyncTest extends BaseServiceTest {

    @Autowired
    private BatchProcessingService batchService;

    @Test
    public void testAsyncBatchProcessing() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);

        // When
        batchService.processBatchAsync(100, result -> {
            success.set(result.isSuccess());
            latch.countDown();
        });

        // Then - Wait for async processing
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Batch processing should complete");
        assertTrue(success.get(), "Batch should process successfully");
    }

    @Test
    public void testAsyncProcessingTimeout() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        // When
        batchService.processBatchAsync(1000, result -> {
            latch.countDown();
        });

        // Then - Should timeout
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertFalse(completed, "Should timeout for slow processing");
    }

    @Test
    public void testMultipleAsyncOperations() throws InterruptedException {
        // Given
        int operationCount = 5;
        CountDownLatch latch = new CountDownLatch(operationCount);

        // When
        for (int i = 0; i < operationCount; i++) {
            batchService.processAsync(i, () -> latch.countDown());
        }

        // Then - Wait for all operations
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All operations should complete");
    }
}
```

### Example 2: WebSocket Async Test

```java
package com.healthdata.websocket.test;

import com.healthdata.BaseServiceTest;
import com.healthdata.websocket.service.WebSocketService;
import com.healthdata.websocket.dto.WebSocketMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Test WebSocket messaging
 */
@SpringBootTest
public class WebSocketAsyncTest extends BaseServiceTest {

    @Autowired
    private WebSocketService webSocketService;

    @Test
    public void testWebSocketMessageBroadcast() throws InterruptedException {
        // Given
        BlockingQueue<WebSocketMessage> receivedMessages = new LinkedBlockingQueue<>();
        webSocketService.subscribe("/topic/updates", message -> {
            receivedMessages.offer(message);
        });

        WebSocketMessage testMessage = new WebSocketMessage();
        testMessage.setContent("Test message");

        // When
        webSocketService.broadcast("/topic/updates", testMessage);

        // Then - Wait for message
        WebSocketMessage received = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Should receive broadcast message");
        assertEqual("Test message", received.getContent(), "Message content should match");
    }

    @Test
    public void testMultipleSubscribersReceiveMessage() throws InterruptedException {
        // Given
        int subscriberCount = 3;
        BlockingQueue<WebSocketMessage>[] queues = new LinkedBlockingQueue[subscriberCount];

        for (int i = 0; i < subscriberCount; i++) {
            queues[i] = new LinkedBlockingQueue<>();
            final int index = i;
            webSocketService.subscribe("/topic/alerts", msg -> queues[index].offer(msg));
        }

        WebSocketMessage alert = new WebSocketMessage();
        alert.setContent("Alert for all");

        // When
        webSocketService.broadcast("/topic/alerts", alert);

        // Then - All subscribers should receive
        for (int i = 0; i < subscriberCount; i++) {
            WebSocketMessage received = queues[i].poll(5, TimeUnit.SECONDS);
            assertNotNull(received, "Subscriber " + i + " should receive message");
            assertEqual("Alert for all", received.getContent(), "Content should match for subscriber " + i);
        }
    }
}
```

---

## Running the Examples

### Run all tests:
```bash
./gradlew test
```

### Run specific test class:
```bash
./gradlew test --tests PatientRepositoryTest
```

### Run specific test method:
```bash
./gradlew test --tests PatientRepositoryTest.testSaveAndRetrievePatient
```

### Run with security profile:
```bash
./gradlew test -Dspring.profiles.active=test-security
```

### Run with coverage:
```bash
./gradlew test --coverage
```

---

## Summary

These example test cases demonstrate:

- **Repository Tests**: Direct database testing with H2
- **Service Tests**: Business logic testing with mocked dependencies
- **Integration Tests**: Full stack testing with real beans
- **REST Controller Tests**: HTTP endpoint testing with various scenarios
- **Security Tests**: Authentication and authorization testing
- **Async Tests**: Asynchronous operation testing with CountDownLatch

All examples use the test infrastructure base classes and follow Spring Boot testing best practices.

For more information, see:
- `TEST_INFRASTRUCTURE_GUIDE.md` - Complete reference
- `TEST_QUICK_REFERENCE.md` - Quick lookup
