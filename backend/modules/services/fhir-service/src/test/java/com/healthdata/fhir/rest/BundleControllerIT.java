package com.healthdata.fhir.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.healthdata.fhir.persistence.ConditionRepository;
import com.healthdata.fhir.persistence.ObservationRepository;
import com.healthdata.fhir.persistence.PatientRepository;

/**
 * Integration tests for BundleController — proves FHIR transaction/batch
 * Bundles flow through to database persistence with tenant isolation.
 */
@SpringBootTest(
    properties = {
        "spring.cache.type=simple",
        "spring.data.redis.repositories.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    },
    classes = {
        com.healthdata.fhir.FhirServiceApplication.class,
        com.healthdata.fhir.config.TestCacheConfiguration.class,
        com.healthdata.fhir.config.TestSecurityConfiguration.class
    }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Tag("integration")
class BundleControllerIT {

    private static final String TENANT_1 = "bundle-test-tenant-1";
    private static final String TENANT_2 = "bundle-test-tenant-2";
    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:postgresql:15-alpine:///testdb");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm");
        registry.add("jwt.access-token-expiration", () -> "1h");
        registry.add("jwt.refresh-token-expiration", () -> "1d");
        registry.add("jwt.issuer", () -> "test-issuer");
        registry.add("jwt.audience", () -> "test-audience");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ConditionRepository conditionRepository;

    @Autowired
    private ObservationRepository observationRepository;

    @BeforeEach
    void cleanDatabase() {
        observationRepository.deleteAll();
        conditionRepository.deleteAll();
        patientRepository.deleteAll();
    }

    @Test
    void shouldProcessTransactionBundle() throws Exception {
        String bundle = """
                {
                  "resourceType": "Bundle",
                  "type": "transaction",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Patient",
                        "name": [{"family": "TransactionTest", "given": ["Alice"]}],
                        "gender": "female",
                        "birthDate": "1990-03-15"
                      },
                      "request": {"method": "POST", "url": "Patient"}
                    },
                    {
                      "resource": {
                        "resourceType": "Condition",
                        "code": {"coding": [{"system": "http://snomed.info/sct", "code": "44054006", "display": "Diabetes mellitus type 2"}]},
                        "clinicalStatus": {"coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]}
                      },
                      "request": {"method": "POST", "url": "Condition"}
                    },
                    {
                      "resource": {
                        "resourceType": "Observation",
                        "status": "final",
                        "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4", "display": "Hemoglobin A1c"}]},
                        "valueQuantity": {"value": 7.2, "unit": "%", "system": "http://unitsofmeasure.org", "code": "%"}
                      },
                      "request": {"method": "POST", "url": "Observation"}
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/Bundle")
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(FHIR_JSON)
                        .content(bundle))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response.contains("transaction-response") || response.contains("batch-response"))
                .as("Response should be a transaction-response or batch-response Bundle")
                .isTrue();

        // Verify resources persisted
        assertThat(patientRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(conditionRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(observationRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldProcessBatchBundle() throws Exception {
        String bundle = """
                {
                  "resourceType": "Bundle",
                  "type": "batch",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Patient",
                        "name": [{"family": "BatchTest", "given": ["Bob"]}],
                        "gender": "male",
                        "birthDate": "1978-11-02"
                      },
                      "request": {"method": "POST", "url": "Patient"}
                    },
                    {
                      "resource": {
                        "resourceType": "Observation",
                        "status": "final",
                        "code": {"coding": [{"system": "http://loinc.org", "code": "2093-3", "display": "Total Cholesterol"}]},
                        "valueQuantity": {"value": 210, "unit": "mg/dL", "system": "http://unitsofmeasure.org", "code": "mg/dL"}
                      },
                      "request": {"method": "POST", "url": "Observation"}
                    }
                  ]
                }
                """;

        String response = mockMvc.perform(post("/Bundle")
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(FHIR_JSON)
                        .content(bundle))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("batch-response");
        assertThat(patientRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
        assertThat(observationRepository.findAll()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRejectEmptyBundle() throws Exception {
        String emptyBundle = """
                {
                  "resourceType": "Bundle",
                  "type": "transaction",
                  "entry": []
                }
                """;

        mockMvc.perform(post("/Bundle")
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(FHIR_JSON)
                        .content(emptyBundle))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidBundleType() throws Exception {
        String collectionBundle = """
                {
                  "resourceType": "Bundle",
                  "type": "collection",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Patient",
                        "name": [{"family": "Invalid", "given": ["Test"]}]
                      },
                      "request": {"method": "POST", "url": "Patient"}
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/Bundle")
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(FHIR_JSON)
                        .content(collectionBundle))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Create a patient under tenant-1
        String bundle = """
                {
                  "resourceType": "Bundle",
                  "type": "transaction",
                  "entry": [
                    {
                      "resource": {
                        "resourceType": "Patient",
                        "name": [{"family": "TenantIsolated", "given": ["Carol"]}],
                        "gender": "female",
                        "birthDate": "1995-07-20"
                      },
                      "request": {"method": "POST", "url": "Patient"}
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/Bundle")
                        .header("X-Tenant-Id", TENANT_1)
                        .contentType(FHIR_JSON)
                        .content(bundle))
                .andExpect(status().isOk());

        // Verify patient exists for tenant-1 but not for tenant-2
        var tenant1Patients = patientRepository.findAll().stream()
                .filter(p -> TENANT_1.equals(p.getTenantId()))
                .toList();
        var tenant2Patients = patientRepository.findAll().stream()
                .filter(p -> TENANT_2.equals(p.getTenantId()))
                .toList();

        assertThat(tenant1Patients).isNotEmpty();
        assertThat(tenant2Patients).isEmpty();
    }
}
