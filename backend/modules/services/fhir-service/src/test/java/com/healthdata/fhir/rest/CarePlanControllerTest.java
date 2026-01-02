package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.CarePlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.CarePlanService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarePlan Controller Tests")
class CarePlanControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private CarePlanService carePlanService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CarePlanController controller = new CarePlanController(carePlanService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create care plan")
    void shouldCreateCarePlan() throws Exception {
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.createCarePlan(eq(TENANT_ID), any(CarePlan.class), eq("user")))
                .thenReturn(carePlan);

        mockMvc.perform(post("/fhir/CarePlan")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(carePlan)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get care plan")
    void shouldGetCarePlan() throws Exception {
        UUID carePlanId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId.toString());
        when(carePlanService.getCarePlan(TENANT_ID, carePlanId))
                .thenReturn(java.util.Optional.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/{id}", carePlanId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should return not found for missing care plan")
    void shouldReturnNotFoundForMissingCarePlan() throws Exception {
        UUID carePlanId = UUID.randomUUID();
        when(carePlanService.getCarePlan(TENANT_ID, carePlanId))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/fhir/CarePlan/{id}", carePlanId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update care plan")
    void shouldUpdateCarePlan() throws Exception {
        UUID carePlanId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(carePlanId.toString());
        when(carePlanService.updateCarePlan(eq(TENANT_ID), eq(carePlanId), any(CarePlan.class), eq("user")))
                .thenReturn(carePlan);

        mockMvc.perform(put("/fhir/CarePlan/{id}", carePlanId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(carePlan)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should delete care plan")
    void shouldDeleteCarePlan() throws Exception {
        UUID carePlanId = UUID.randomUUID();

        mockMvc.perform(delete("/fhir/CarePlan/{id}", carePlanId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search care plans with invalid references")
    void shouldSearchCarePlansWithInvalidReferences() throws Exception {
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.searchCarePlans(
                eq(TENANT_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(carePlan)));

        mockMvc.perform(get("/fhir/CarePlan")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/not-a-uuid")
                        .param("encounter", "Encounter/not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")))
                .andExpect(content().string(containsString(carePlan.getId())));

        verify(carePlanService).searchCarePlans(
                eq(TENANT_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Should search care plans with references")
    void shouldSearchCarePlansWithReferences() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID encounterId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.searchCarePlans(
                eq(TENANT_ID),
                eq(patientId),
                eq(encounterId),
                eq("active"),
                eq("plan"),
                eq("category"),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(carePlan)));

        mockMvc.perform(get("/fhir/CarePlan")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("encounter", "Encounter/" + encounterId)
                        .param("status", "active")
                        .param("intent", "plan")
                        .param("category", "category")
                        .param("_page", "0")
                        .param("_count", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get care plans by patient")
    void shouldGetCarePlansByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getCarePlansByPatient(TENANT_ID, patientId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/patient/{patientId}", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get active care plans by patient")
    void shouldGetActiveCarePlansByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getActiveCarePlans(TENANT_ID, patientId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/patient/{patientId}/active", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get primary care plans by patient")
    void shouldGetPrimaryCarePlansByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getPrimaryCarePlans(TENANT_ID, patientId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/patient/{patientId}/primary", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get care plans with activities by patient")
    void shouldGetCarePlansWithActivitiesByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getCarePlansWithActivities(TENANT_ID, patientId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/patient/{patientId}/with-activities", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get care plans by encounter")
    void shouldGetCarePlansByEncounter() throws Exception {
        UUID encounterId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getCarePlansByEncounter(TENANT_ID, encounterId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/encounter/{encounterId}", encounterId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get care plans by condition")
    void shouldGetCarePlansByCondition() throws Exception {
        UUID conditionId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getCarePlansByCondition(TENANT_ID, conditionId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/condition/{conditionId}", conditionId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get care plans by goal")
    void shouldGetCarePlansByGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getCarePlansByGoal(TENANT_ID, goalId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/goal/{goalId}", goalId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get child care plans")
    void shouldGetChildCarePlans() throws Exception {
        UUID carePlanId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getChildCarePlans(TENANT_ID, carePlanId))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/{carePlanId}/children", carePlanId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should get expiring care plans")
    void shouldGetExpiringCarePlans() throws Exception {
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.getExpiringCarePlans(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/expiring")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("start", "2025-01-01T00:00:00Z")
                        .param("end", "2025-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }

    @Test
    @DisplayName("Should search care plans by text")
    void shouldSearchCarePlansByText() throws Exception {
        UUID patientId = UUID.randomUUID();
        CarePlan carePlan = new CarePlan();
        carePlan.setId(UUID.randomUUID().toString());
        when(carePlanService.searchByText(TENANT_ID, patientId, "term"))
                .thenReturn(List.of(carePlan));

        mockMvc.perform(get("/fhir/CarePlan/patient/{patientId}/search", patientId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("q", "term"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(carePlan.getId())));
    }
}
