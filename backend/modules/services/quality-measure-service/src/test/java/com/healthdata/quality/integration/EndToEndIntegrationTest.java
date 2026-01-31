package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-End Integration Test Suite for HDIM Healthcare Platform.
 * 
 * Tests the complete patient health workflow from data creation through
 * real-time notifications, verifying all components work together.
 * 
 * Test Workflow:
 * 1. Create patient via FHIR API
 * 2. Create HbA1c observation (critical value: 9.5%)
 * 3. Verify Kafka event published
 * 4. Wait for risk assessment update
 * 5. Wait for care gap creation
 * 6. Wait for health score update
 * 7. Wait for clinical alert
 * 8. Verify WebSocket notification
 * 9. Verify read model updated
 * 
 * Success Criteria: All steps complete within 5 seconds
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@Testcontainers
@ActiveProfiles("test")
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("End-to-End Integration Tests")
public class EndToEndIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("hdim_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired(required = false)
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String baseUrl;
    private String tenantId;
    private HttpHeaders headers;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
    }

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        tenantId = "test-tenant-" + UUID.randomUUID().toString().substring(0, 8);
        
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId);
        headers.set("Accept", "application/fhir+json");
    }

    @Test
    @DisplayName("Complete Patient Health Workflow - End to End")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCompletePatientHealthWorkflow() {
        Instant startTime = Instant.now();
        
        // Step 1: Create patient via FHIR API
        String patientId = createPatient();
        assertThat(patientId).isNotNull();
        logStep("1. Patient created", startTime);

        // Step 2: Create HbA1c observation with critical value (9.5%)
        String observationId = createHbA1cObservation(patientId, 9.5);
        assertThat(observationId).isNotNull();
        logStep("2. HbA1c observation created (9.5%)", startTime);

        // Step 3: Verify Kafka event published
        verifyKafkaEventPublished(patientId);
        logStep("3. Kafka event published", startTime);

        // Step 4: Wait for risk assessment update
        waitForRiskAssessment(patientId);
        logStep("4. Risk assessment updated", startTime);

        // Step 5: Wait for care gap creation
        waitForCareGap(patientId);
        logStep("5. Care gap created", startTime);

        // Step 6: Wait for health score update
        waitForHealthScore(patientId);
        logStep("6. Health score updated", startTime);

        // Step 7: Verify clinical alert created
        verifyClinicalAlert(patientId);
        logStep("7. Clinical alert created", startTime);

        // Step 8: Verify read model updated
        verifyReadModel(patientId);
        logStep("8. Read model updated", startTime);

        // Verify total time is under 5 seconds
        Duration totalDuration = Duration.between(startTime, Instant.now());
        logStep("COMPLETE - Total time", startTime);
        
        assertThat(totalDuration)
                .as("Complete workflow should finish within 5 seconds")
                .isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Multi-Tenant Isolation Test")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMultiTenantIsolation() {
        // Create patient in tenant A
        String tenantA = "tenant-A-" + UUID.randomUUID().toString().substring(0, 8);
        String patientIdA = createPatientInTenant(tenantA);

        // Create patient in tenant B
        String tenantB = "tenant-B-" + UUID.randomUUID().toString().substring(0, 8);
        String patientIdB = createPatientInTenant(tenantB);

        // Verify tenant A cannot see tenant B's patient
        HttpHeaders headersA = createHeadersForTenant(tenantA);
        ResponseEntity<String> responseA = restTemplate.exchange(
                baseUrl + "/api/v1/patients/" + patientIdB,
                HttpMethod.GET,
                new HttpEntity<>(headersA),
                String.class
        );
        assertThat(responseA.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);

        // Verify tenant B cannot see tenant A's patient
        HttpHeaders headersB = createHeadersForTenant(tenantB);
        ResponseEntity<String> responseB = restTemplate.exchange(
                baseUrl + "/api/v1/patients/" + patientIdA,
                HttpMethod.GET,
                new HttpEntity<>(headersB),
                String.class
        );
        assertThat(responseB.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("High-Risk Patient Alert Workflow")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testHighRiskPatientAlertWorkflow() {
        // Create patient
        String patientId = createPatient();

        // Create multiple critical observations to trigger high-risk status
        createHbA1cObservation(patientId, 10.5);  // Very high HbA1c
        createBloodPressureObservation(patientId, 180, 110);  // Hypertensive crisis

        // Wait for high-risk classification
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> isPatientHighRisk(patientId));

        // Verify critical alert was generated
        await().atMost(Duration.ofSeconds(5))
                .until(() -> hasCriticalAlert(patientId));
    }

    @Test
    @DisplayName("Care Gap Closure Workflow")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testCareGapClosureWorkflow() {
        // Create patient
        String patientId = createPatient();

        // Create HbA1c observation to trigger care gap
        createHbA1cObservation(patientId, 9.0);

        // Wait for care gap creation
        String careGapId = waitForCareGap(patientId);

        // Verify care gap is open
        assertThat(getCareGapStatus(careGapId)).isEqualTo("OPEN");

        // Create new HbA1c observation with controlled value
        createHbA1cObservation(patientId, 6.5);

        // Wait for care gap closure
        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .until(() -> "CLOSED".equals(getCareGapStatus(careGapId)));
    }

    // ========== Helper Methods ==========

    private String createPatient() {
        return createPatientInTenant(tenantId);
    }

    private String createPatientInTenant(String tenant) {
        String patientJson = """
            {
                "resourceType": "Patient",
                "identifier": [{
                    "system": "http://hdim.healthdata.com/patient-id",
                    "value": "%s"
                }],
                "name": [{
                    "family": "TestPatient",
                    "given": ["Integration"]
                }],
                "birthDate": "1970-01-01",
                "gender": "male"
            }
            """.formatted(UUID.randomUUID().toString());

        HttpHeaders tenantHeaders = createHeadersForTenant(tenant);
        tenantHeaders.setContentType(MediaType.valueOf("application/fhir+json"));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/fhir/Patient",
                HttpMethod.POST,
                new HttpEntity<>(patientJson, tenantHeaders),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("id");
    }

    private String createHbA1cObservation(String patientId, double value) {
        String observationJson = """
            {
                "resourceType": "Observation",
                "status": "final",
                "code": {
                    "coding": [{
                        "system": "http://loinc.org",
                        "code": "4548-4",
                        "display": "Hemoglobin A1c/Hemoglobin.total in Blood"
                    }]
                },
                "subject": {
                    "reference": "Patient/%s"
                },
                "effectiveDateTime": "%s",
                "valueQuantity": {
                    "value": %s,
                    "unit": "%%",
                    "system": "http://unitsofmeasure.org",
                    "code": "%%"
                }
            }
            """.formatted(patientId, Instant.now().toString(), value);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/fhir/Observation",
                HttpMethod.POST,
                new HttpEntity<>(observationJson, headers),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return (String) response.getBody().get("id");
    }

    private void createBloodPressureObservation(String patientId, int systolic, int diastolic) {
        String observationJson = """
            {
                "resourceType": "Observation",
                "status": "final",
                "code": {
                    "coding": [{
                        "system": "http://loinc.org",
                        "code": "85354-9",
                        "display": "Blood pressure panel"
                    }]
                },
                "subject": {
                    "reference": "Patient/%s"
                },
                "effectiveDateTime": "%s",
                "component": [
                    {
                        "code": {
                            "coding": [{
                                "system": "http://loinc.org",
                                "code": "8480-6",
                                "display": "Systolic blood pressure"
                            }]
                        },
                        "valueQuantity": {
                            "value": %d,
                            "unit": "mmHg"
                        }
                    },
                    {
                        "code": {
                            "coding": [{
                                "system": "http://loinc.org",
                                "code": "8462-4",
                                "display": "Diastolic blood pressure"
                            }]
                        },
                        "valueQuantity": {
                            "value": %d,
                            "unit": "mmHg"
                        }
                    }
                ]
            }
            """.formatted(patientId, Instant.now().toString(), systolic, diastolic);

        restTemplate.exchange(
                baseUrl + "/fhir/Observation",
                HttpMethod.POST,
                new HttpEntity<>(observationJson, headers),
                Map.class
        );
    }

    private void verifyKafkaEventPublished(String patientId) {
        // In a real test, we'd use a Kafka consumer to verify
        // For now, we verify the event endpoint shows the event was processed
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> {
                    try {
                        ResponseEntity<String> response = restTemplate.exchange(
                                baseUrl + "/api/v1/events/patient/" + patientId + "/latest",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                String.class
                        );
                        return response.getStatusCode() == HttpStatus.OK;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    private void waitForRiskAssessment(String patientId) {
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(300))
                .until(() -> hasRiskAssessment(patientId));
    }

    private boolean hasRiskAssessment(String patientId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/patients/" + patientId + "/risk-assessment",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            return response.getStatusCode() == HttpStatus.OK && 
                   response.getBody() != null &&
                   response.getBody().get("riskLevel") != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String waitForCareGap(String patientId) {
        AtomicReference<String> careGapId = new AtomicReference<>();
        
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(300))
                .until(() -> {
                    try {
                        ResponseEntity<Map> response = restTemplate.exchange(
                                baseUrl + "/api/v1/care-gaps?patientId=" + patientId,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                Map.class
                        );
                        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                            var gaps = (java.util.List<?>) response.getBody().get("content");
                            if (gaps != null && !gaps.isEmpty()) {
                                var gap = (Map<?, ?>) gaps.get(0);
                                careGapId.set((String) gap.get("id"));
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        // Continue polling
                    }
                    return false;
                });
        
        return careGapId.get();
    }

    private void waitForHealthScore(String patientId) {
        await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(300))
                .until(() -> {
                    try {
                        ResponseEntity<Map> response = restTemplate.exchange(
                                baseUrl + "/api/v1/health-scores/patient/" + patientId,
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                Map.class
                        );
                        return response.getStatusCode() == HttpStatus.OK &&
                               response.getBody() != null &&
                               response.getBody().get("score") != null;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    private void verifyClinicalAlert(String patientId) {
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> hasCriticalAlert(patientId));
    }

    private boolean hasCriticalAlert(String patientId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/alerts/patient/" + patientId + "?severity=CRITICAL,HIGH",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                var alerts = (java.util.List<?>) response.getBody().get("content");
                return alerts != null && !alerts.isEmpty();
            }
        } catch (Exception e) {
            // Continue checking
        }
        return false;
    }

    private void verifyReadModel(String patientId) {
        await().atMost(Duration.ofSeconds(3))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> {
                    try {
                        ResponseEntity<Map> response = restTemplate.exchange(
                                baseUrl + "/api/v1/patients/" + patientId + "/summary",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                Map.class
                        );
                        return response.getStatusCode() == HttpStatus.OK &&
                               response.getBody() != null &&
                               response.getBody().get("healthScore") != null &&
                               response.getBody().get("careGapCount") != null;
                    } catch (Exception e) {
                        return false;
                    }
                });
    }

    private boolean isPatientHighRisk(String patientId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/patients/" + patientId + "/risk-assessment",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String riskLevel = (String) response.getBody().get("riskLevel");
                return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
            }
        } catch (Exception e) {
            // Continue checking
        }
        return false;
    }

    private String getCareGapStatus(String careGapId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/v1/care-gaps/" + careGapId,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("status");
            }
        } catch (Exception e) {
            // Return null if not found
        }
        return null;
    }

    private HttpHeaders createHeadersForTenant(String tenant) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("X-Tenant-ID", tenant);
        h.set("Accept", "application/json");
        return h;
    }

    private void logStep(String step, Instant startTime) {
        Duration elapsed = Duration.between(startTime, Instant.now());
        System.out.printf("[%6dms] %s%n", elapsed.toMillis(), step);
    }
}
