package com.healthdata.fhir.rest;

import static com.healthdata.test.TestTenantConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.healthdata.fhir.config.AbstractFhirIntegrationTest;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

// Note: KafkaTemplate mock is provided by TestSecurityConfiguration - do not duplicate @MockBean here
class PatientControllerIT extends AbstractFhirIntegrationTest {

    private static final String TENANT = PRIMARY_TENANT_ID;
    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void cleanDatabase() {
        patientRepository.deleteAll();
    }

    @Test
    void patientLifecycle_shouldSupportCrudAndSearch() throws Exception {
        String patientPayload = """
                {
                  "resourceType": "Patient",
                  "name": [
                    {
                      "family": "Chen",
                      "given": [
                        "Maya"
                      ]
                    }
                  ],
                  "gender": "female",
                  "birthDate": "1985-05-20"
                }
                """;

        String createResponse = mockMvc.perform(post("/fhir/Patient")
                        .header("X-Tenant-Id", TENANT)
                        .contentType(FHIR_JSON)
                        .content(patientPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String createdId = com.jayway.jsonpath.JsonPath.read(createResponse, "$.id");
        assertThat(createdId).isNotBlank();

        mockMvc.perform(get("/fhir/Patient/{id}", createdId)
                        .header("X-Tenant-Id", TENANT)
                        .accept(FHIR_JSON))
                .andExpect(status().isOk());

        String updatePayload = """
                {
                  "resourceType": "Patient",
                  "id": "%s",
                  "name": [
                    {
                      "family": "Chen",
                      "given": [
                        "Maya",
                        "L."
                      ]
                    }
                  ],
                  "gender": "female",
                  "birthDate": "1985-05-20"
                }
                """.formatted(createdId);

        mockMvc.perform(put("/fhir/Patient/{id}", createdId)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(FHIR_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fhir/Patient")
                        .header("X-Tenant-Id", TENANT)
                        .param("family", "Chen")
                        .accept(FHIR_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/fhir/Patient/{id}", createdId)
                        .header("X-Tenant-Id", TENANT))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/fhir/Patient/{id}", createdId)
                        .header("X-Tenant-Id", TENANT)
                        .accept(FHIR_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchShouldUseExistingSeedData() throws Exception {
        patientRepository.save(PatientEntity.builder()
                .id(UUID.fromString("f23e6b4e-93e9-4bcd-9ad7-916844d37f4c"))
                .tenantId(TENANT)
                .resourceType("Patient")
                .resourceJson("""
                        {"resourceType":"Patient","id":"f23e6b4e-93e9-4bcd-9ad7-916844d37f4c","name":[{"family":"Iglesias","given":["Rafael"]}]}
                        """)
                .firstName("Rafael")
                .lastName("Iglesias")
                .gender("male")
                .birthDate(LocalDate.of(1990, 2, 15))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build());

        mockMvc.perform(get("/fhir/Patient")
                        .header("X-Tenant-Id", TENANT)
                        .param("name", "Iglesias")
                        .accept(FHIR_JSON))
                .andExpect(status().isOk())
                .andExpect(result ->
                        assertThat(result.getResponse().getContentAsString()).contains("Iglesias"));
    }
}
