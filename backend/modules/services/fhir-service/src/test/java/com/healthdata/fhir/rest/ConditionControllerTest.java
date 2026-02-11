package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.ConditionService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Condition Controller Tests")
class ConditionControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private ConditionService conditionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ConditionController controller = new ConditionController(conditionService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should create condition")
    void shouldCreateCondition() throws Exception {
        Condition condition = buildCondition(UUID.randomUUID());
        when(conditionService.createCondition(eq(TENANT_ID), any(Condition.class), eq("user")))
                .thenReturn(condition);

        mockMvc.perform(post("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(condition)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/Condition/" + condition.getId()))
                .andExpect(content().string(containsString(condition.getId())));
    }

    @Test
    @DisplayName("Should return bad request on create invalid JSON")
    void shouldReturnBadRequestOnCreateInvalidJson() throws Exception {
        mockMvc.perform(post("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{bad-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request on create error")
    void shouldReturnBadRequestOnCreate() throws Exception {
        when(conditionService.createCondition(eq(TENANT_ID), any(Condition.class), eq("user")))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(post("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Condition\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bad")));
    }

    @Test
    @DisplayName("Should get condition by id")
    void shouldGetCondition() throws Exception {
        Condition condition = buildCondition(UUID.randomUUID());
        when(conditionService.getCondition(TENANT_ID, condition.getId())).thenReturn(Optional.of(condition));

        mockMvc.perform(get("/Condition/{id}", condition.getId())
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(condition.getId())));
    }

    @Test
    @DisplayName("Should return not found when condition missing")
    void shouldReturnNotFound() throws Exception {
        when(conditionService.getCondition(TENANT_ID, "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/Condition/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update condition")
    void shouldUpdateCondition() throws Exception {
        Condition condition = buildCondition(UUID.randomUUID());
        when(conditionService.updateCondition(eq(TENANT_ID), eq(condition.getId()), any(Condition.class), eq("user")))
                .thenReturn(condition);

        mockMvc.perform(put("/Condition/{id}", condition.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(condition)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(condition.getId())));
    }

    @Test
    @DisplayName("Should return bad request on update invalid JSON")
    void shouldReturnBadRequestOnUpdateInvalidJson() throws Exception {
        mockMvc.perform(put("/Condition/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return not found on update when missing")
    void shouldReturnNotFoundOnUpdate() throws Exception {
        doThrow(new ConditionService.ConditionNotFoundException("missing"))
                .when(conditionService).updateCondition(eq(TENANT_ID), eq("missing"), any(Condition.class), eq("user"));

        mockMvc.perform(put("/Condition/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Condition\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete condition")
    void shouldDeleteCondition() throws Exception {
        mockMvc.perform(delete("/Condition/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return not found on delete when missing")
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new ConditionService.ConditionNotFoundException("missing"))
                .when(conditionService).deleteCondition(TENANT_ID, "missing", "user");

        mockMvc.perform(delete("/Condition/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search conditions by patient")
    void shouldSearchConditionsByPatient() throws Exception {
        when(conditionService.searchConditionsByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on search error")
    void shouldReturnBadRequestOnSearchError() throws Exception {
        when(conditionService.searchConditionsByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenThrow(new IllegalArgumentException("search failure"));

        mockMvc.perform(get("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("search failure")));
    }

    @Test
    @DisplayName("Should search conditions by patient and code")
    void shouldSearchConditionsByPatientAndCode() throws Exception {
        when(conditionService.searchConditionsByPatientAndCode(TENANT_ID, "patient-1", "E11.9"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "E11.9"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search conditions by patient and category")
    void shouldSearchConditionsByPatientAndCategory() throws Exception {
        when(conditionService.searchConditionsByPatientAndCategory(TENANT_ID, "patient-1", "encounter-diagnosis"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("category", "encounter-diagnosis"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request when patient param missing")
    void shouldReturnBadRequestWhenPatientMissing() throws Exception {
        mockMvc.perform(get("/Condition")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("patient parameter is required")));
    }

    @Test
    @DisplayName("Should get active conditions")
    void shouldGetActiveConditions() throws Exception {
        when(conditionService.getActiveConditionsByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition/active")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on active condition error")
    void shouldReturnBadRequestOnActiveConditionError() throws Exception {
        when(conditionService.getActiveConditionsByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("active failure"));

        mockMvc.perform(get("/Condition/active")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("active failure")));
    }

    @Test
    @DisplayName("Should get chronic conditions")
    void shouldGetChronicConditions() throws Exception {
        when(conditionService.getChronicConditionsByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition/chronic")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on chronic condition error")
    void shouldReturnBadRequestOnChronicConditionError() throws Exception {
        when(conditionService.getChronicConditionsByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("chronic failure"));

        mockMvc.perform(get("/Condition/chronic")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("chronic failure")));
    }

    @Test
    @DisplayName("Should get diagnoses")
    void shouldGetDiagnoses() throws Exception {
        when(conditionService.getDiagnosesByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition/diagnoses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on diagnoses error")
    void shouldReturnBadRequestOnDiagnosesError() throws Exception {
        when(conditionService.getDiagnosesByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("diagnoses failure"));

        mockMvc.perform(get("/Condition/diagnoses")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("diagnoses failure")));
    }

    @Test
    @DisplayName("Should get problem list")
    void shouldGetProblemList() throws Exception {
        when(conditionService.getProblemListByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Condition/problem-list")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on problem list error")
    void shouldReturnBadRequestOnProblemListError() throws Exception {
        when(conditionService.getProblemListByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("problem list failure"));

        mockMvc.perform(get("/Condition/problem-list")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("problem list failure")));
    }

    @Test
    @DisplayName("Should check has condition")
    void shouldCheckHasCondition() throws Exception {
        when(conditionService.hasActiveCondition(TENANT_ID, "patient-1", "E11.9")).thenReturn(true);

        mockMvc.perform(get("/Condition/has-condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "E11.9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    @DisplayName("Should return bad request on has condition error")
    void shouldReturnBadRequestOnHasConditionError() throws Exception {
        when(conditionService.hasActiveCondition(TENANT_ID, "patient-1", "E11.9"))
                .thenThrow(new IllegalStateException("has condition failure"));

        mockMvc.perform(get("/Condition/has-condition")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "E11.9"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("has condition failure")));
    }
    @Test
    @DisplayName("Should return health check")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/Condition/_health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Condition")));
    }

    private Condition buildCondition(UUID id) {
        Condition condition = new Condition();
        condition.setId(id.toString());
        condition.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        condition.setCode(new CodeableConcept().addCoding(new Coding().setCode("E11.9")));
        return condition;
    }
}
