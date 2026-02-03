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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
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

import com.healthdata.fhir.service.ObservationService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Observation Controller Tests")
class ObservationControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private ObservationService observationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObservationController controller = new ObservationController(observationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should create observation")
    void shouldCreateObservation() throws Exception {
        Observation observation = buildObservation(UUID.randomUUID());
        when(observationService.createObservation(eq(TENANT_ID), any(Observation.class), eq("user")))
                .thenReturn(observation);

        mockMvc.perform(post("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(observation)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/Observation/" + observation.getId()))
                .andExpect(content().string(containsString(observation.getId())));
    }

    @Test
    @DisplayName("Should return bad request on create invalid JSON")
    void shouldReturnBadRequestOnCreateInvalidJson() throws Exception {
        mockMvc.perform(post("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{bad-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return bad request on create error")
    void shouldReturnBadRequestOnCreate() throws Exception {
        when(observationService.createObservation(eq(TENANT_ID), any(Observation.class), eq("user")))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(post("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Observation\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bad")));
    }

    @Test
    @DisplayName("Should get observation")
    void shouldGetObservation() throws Exception {
        Observation observation = buildObservation(UUID.randomUUID());
        when(observationService.getObservation(TENANT_ID, observation.getId())).thenReturn(Optional.of(observation));

        mockMvc.perform(get("/Observation/{id}", observation.getId())
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(observation.getId())));
    }

    @Test
    @DisplayName("Should return not found for missing observation")
    void shouldReturnNotFound() throws Exception {
        when(observationService.getObservation(TENANT_ID, "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/Observation/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update observation")
    void shouldUpdateObservation() throws Exception {
        Observation observation = buildObservation(UUID.randomUUID());
        when(observationService.updateObservation(eq(TENANT_ID), eq(observation.getId()), any(Observation.class), eq("user")))
                .thenReturn(observation);

        mockMvc.perform(put("/Observation/{id}", observation.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(observation)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(observation.getId())));
    }

    @Test
    @DisplayName("Should return bad request on update invalid JSON")
    void shouldReturnBadRequestOnUpdateInvalidJson() throws Exception {
        mockMvc.perform(put("/Observation/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{broken"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return not found on update")
    void shouldReturnNotFoundOnUpdate() throws Exception {
        doThrow(new ObservationService.ObservationNotFoundException("missing"))
                .when(observationService).updateObservation(eq(TENANT_ID), eq("missing"), any(Observation.class), eq("user"));

        mockMvc.perform(put("/Observation/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Observation\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should delete observation")
    void shouldDeleteObservation() throws Exception {
        mockMvc.perform(delete("/Observation/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return not found on delete")
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new ObservationService.ObservationNotFoundException("missing"))
                .when(observationService).deleteObservation(TENANT_ID, "missing", "user");

        mockMvc.perform(delete("/Observation/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search observations by patient")
    void shouldSearchObservationsByPatient() throws Exception {
        when(observationService.searchObservationsByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on search error")
    void shouldReturnBadRequestOnSearchError() throws Exception {
        when(observationService.searchObservationsByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenThrow(new IllegalStateException("search failed"));

        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("search failed")));
    }

    @Test
    @DisplayName("Should search observations by code")
    void shouldSearchObservationsByCode() throws Exception {
        when(observationService.searchObservationsByPatientAndCode(eq(TENANT_ID), eq("patient-1"), eq("8480-6")))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "8480-6"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search observations by category")
    void shouldSearchObservationsByCategory() throws Exception {
        when(observationService.searchObservationsByPatientAndCategory(eq(TENANT_ID), eq("patient-1"), eq("vital-signs")))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("category", "vital-signs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search observations by date range")
    void shouldSearchObservationsByDateRange() throws Exception {
        when(observationService.searchObservationsByPatientAndDateRange(
                eq(TENANT_ID), eq("patient-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date", "2024-01-01T00:00:00/2024-01-02T00:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should require patient parameter for search")
    void shouldRequirePatientParameter() throws Exception {
        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("patient parameter is required")));
    }

    @Test
    @DisplayName("Should return bad request for invalid date format")
    void shouldReturnBadRequestForInvalidDateFormat() throws Exception {
        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("startDate/endDate")));
    }

    @Test
    @DisplayName("Should return bad request for parse errors")
    void shouldReturnBadRequestForParseErrors() throws Exception {
        mockMvc.perform(get("/Observation")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date", "bad/bad"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid date format")));
    }

    @Test
    @DisplayName("Should return lab results")
    void shouldReturnLabResults() throws Exception {
        when(observationService.getLabResultsByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Observation/lab-results")
                        .header("X-Tenant-ID", TENANT_ID)
                .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on lab results error")
    void shouldReturnBadRequestOnLabResultsError() throws Exception {
        when(observationService.getLabResultsByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("lab failed"));

        mockMvc.perform(get("/Observation/lab-results")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("lab failed")));
    }

    @Test
    @DisplayName("Should return vital signs")
    void shouldReturnVitalSigns() throws Exception {
        when(observationService.getVitalSignsByPatient(TENANT_ID, "patient-1"))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/Observation/vital-signs")
                        .header("X-Tenant-ID", TENANT_ID)
                .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on vital signs error")
    void shouldReturnBadRequestOnVitalSignsError() throws Exception {
        when(observationService.getVitalSignsByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("vitals failed"));

        mockMvc.perform(get("/Observation/vital-signs")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("vitals failed")));
    }
    @Test
    @DisplayName("Should return latest observation")
    void shouldReturnLatestObservation() throws Exception {
        Observation observation = buildObservation(UUID.randomUUID());
        when(observationService.getLatestObservationByPatientAndCode(TENANT_ID, "patient-1", "8480-6"))
                .thenReturn(Optional.of(observation));

        mockMvc.perform(get("/Observation/latest")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "8480-6"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(observation.getId())));
    }

    @Test
    @DisplayName("Should return not found for missing latest observation")
    void shouldReturnNotFoundForLatestObservation() throws Exception {
        when(observationService.getLatestObservationByPatientAndCode(TENANT_ID, "patient-1", "8480-6"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/Observation/latest")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("code", "8480-6"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return health check")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/Observation/_health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Observation")));
    }

    private Observation buildObservation(UUID id) {
        Observation observation = new Observation();
        observation.setId(id.toString());
        observation.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept().addCoding(new Coding().setCode("8480-6")));
        return observation;
    }
}
