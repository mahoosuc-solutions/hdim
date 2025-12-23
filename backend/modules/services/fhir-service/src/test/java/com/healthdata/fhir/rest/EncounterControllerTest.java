package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
import org.hl7.fhir.r4.model.Encounter;
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

import com.healthdata.fhir.service.EncounterService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Encounter Controller Tests")
class EncounterControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private EncounterService encounterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        EncounterController controller = new EncounterController(encounterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("Should create encounter")
    void shouldCreateEncounter() throws Exception {
        Encounter encounter = buildEncounter(UUID.randomUUID());
        when(encounterService.createEncounter(eq(TENANT_ID), any(Encounter.class), eq("user")))
                .thenReturn(encounter);

        mockMvc.perform(post("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(encounter)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/fhir/Encounter/" + encounter.getId()))
                .andExpect(content().string(containsString(encounter.getId())));
    }

    @Test
    @DisplayName("Should return bad request on create error")
    void shouldReturnBadRequestOnCreateError() throws Exception {
        when(encounterService.createEncounter(eq(TENANT_ID), any(Encounter.class), eq("user")))
                .thenThrow(new IllegalArgumentException("bad create"));

        mockMvc.perform(post("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Encounter\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("bad create")));
    }

    @Test
    @DisplayName("Should get encounter")
    void shouldGetEncounter() throws Exception {
        Encounter encounter = buildEncounter(UUID.randomUUID());
        when(encounterService.getEncounter(TENANT_ID, encounter.getId())).thenReturn(Optional.of(encounter));

        mockMvc.perform(get("/fhir/Encounter/{id}", encounter.getId())
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(encounter.getId())));
    }

    @Test
    @DisplayName("Should return not found for missing encounter")
    void shouldReturnNotFound() throws Exception {
        when(encounterService.getEncounter(TENANT_ID, "missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/Encounter/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update encounter")
    void shouldUpdateEncounter() throws Exception {
        Encounter encounter = buildEncounter(UUID.randomUUID());
        when(encounterService.updateEncounter(eq(TENANT_ID), eq(encounter.getId()), any(Encounter.class), eq("user")))
                .thenReturn(encounter);

        mockMvc.perform(put("/fhir/Encounter/{id}", encounter.getId())
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(encounter)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(encounter.getId())));
    }

    @Test
    @DisplayName("Should return bad request on invalid JSON")
    void shouldReturnBadRequestOnInvalidJson() throws Exception {
        mockMvc.perform(post("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{bad-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("error")));
    }

    @Test
    @DisplayName("Should return not found on update")
    void shouldReturnNotFoundOnUpdate() throws Exception {
        doThrow(new EncounterService.EncounterNotFoundException("missing"))
                .when(encounterService).updateEncounter(eq(TENANT_ID), eq("missing"), any(Encounter.class), eq("user"));

        mockMvc.perform(put("/fhir/Encounter/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{\"resourceType\":\"Encounter\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return bad request on update parse failure")
    void shouldReturnBadRequestOnUpdateParseFailure() throws Exception {
        mockMvc.perform(put("/fhir/Encounter/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("error")));
    }

    @Test
    @DisplayName("Should delete encounter")
    void shouldDeleteEncounter() throws Exception {
        mockMvc.perform(delete("/fhir/Encounter/{id}", "id-1")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return not found on delete when missing")
    void shouldReturnNotFoundOnDelete() throws Exception {
        doThrow(new EncounterService.EncounterNotFoundException("missing"))
                .when(encounterService).deleteEncounter(TENANT_ID, "missing", "user");

        mockMvc.perform(delete("/fhir/Encounter/{id}", "missing")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search encounters by patient")
    void shouldSearchEncountersByPatient() throws Exception {
        when(encounterService.searchEncountersByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should search encounters by patient and date range")
    void shouldSearchEncountersByPatientAndDateRange() throws Exception {
        when(encounterService.searchEncountersByPatientAndDateRange(
                eq(TENANT_ID), eq("patient-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request on search error")
    void shouldReturnBadRequestOnSearchError() throws Exception {
        when(encounterService.searchEncountersByPatient(eq(TENANT_ID), eq("patient-1"), any()))
                .thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("boom")));
    }

    @Test
    @DisplayName("Should return encounter categories")
    void shouldReturnEncounterCategories() throws Exception {
        when(encounterService.getFinishedEncountersByPatient(eq(TENANT_ID), eq("patient-1")))
                .thenReturn(new Bundle());
        when(encounterService.getActiveEncountersByPatient(eq(TENANT_ID), eq("patient-1")))
                .thenReturn(new Bundle());
        when(encounterService.getInpatientEncountersByPatient(eq(TENANT_ID), eq("patient-1")))
                .thenReturn(new Bundle());
        when(encounterService.getAmbulatoryEncountersByPatient(eq(TENANT_ID), eq("patient-1")))
                .thenReturn(new Bundle());
        when(encounterService.getEmergencyEncountersByPatient(eq(TENANT_ID), eq("patient-1")))
                .thenReturn(new Bundle());

        mockMvc.perform(get("/fhir/Encounter/finished")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fhir/Encounter/active")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fhir/Encounter/inpatient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fhir/Encounter/ambulatory")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/fhir/Encounter/emergency")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return bad request for encounter category errors")
    void shouldReturnBadRequestForEncounterCategoryErrors() throws Exception {
        when(encounterService.getFinishedEncountersByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("finished error"));
        when(encounterService.getActiveEncountersByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("active error"));
        when(encounterService.getInpatientEncountersByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("inpatient error"));
        when(encounterService.getAmbulatoryEncountersByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("ambulatory error"));
        when(encounterService.getEmergencyEncountersByPatient(TENANT_ID, "patient-1"))
                .thenThrow(new IllegalStateException("emergency error"));

        mockMvc.perform(get("/fhir/Encounter/finished")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("finished error")));

        mockMvc.perform(get("/fhir/Encounter/active")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("active error")));

        mockMvc.perform(get("/fhir/Encounter/inpatient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("inpatient error")));

        mockMvc.perform(get("/fhir/Encounter/ambulatory")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("ambulatory error")));

        mockMvc.perform(get("/fhir/Encounter/emergency")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("emergency error")));
    }

    @Test
    @DisplayName("Should check encounter presence in date range")
    void shouldCheckEncounterPresenceInDateRange() throws Exception {
        when(encounterService.hasEncounterInDateRange(eq(TENANT_ID), eq("patient-1"), any(), any()))
                .thenReturn(true);

        mockMvc.perform(get("/fhir/Encounter/has-encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hasEncounter")));

        verify(encounterService).hasEncounterInDateRange(eq(TENANT_ID), eq("patient-1"), any(), any());
    }

    @Test
    @DisplayName("Should return bad request when encounter presence check fails")
    void shouldReturnBadRequestWhenPresenceCheckFails() throws Exception {
        when(encounterService.hasEncounterInDateRange(eq(TENANT_ID), eq("patient-1"), any(), any()))
                .thenThrow(new IllegalArgumentException("range error"));

        mockMvc.perform(get("/fhir/Encounter/has-encounter")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("range error")));
    }

    @Test
    @DisplayName("Should return bad request when patient missing")
    void shouldReturnBadRequestWhenPatientMissing() throws Exception {
        mockMvc.perform(get("/fhir/Encounter")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("patient parameter is required")));
    }

    @Test
    @DisplayName("Should check encounter counts and health")
    void shouldCheckEncounterCountsAndHealth() throws Exception {
        when(encounterService.countInpatientEncounters(eq(TENANT_ID), eq("patient-1"), any(), any()))
                .thenReturn(2L);
        when(encounterService.countEmergencyEncounters(eq(TENANT_ID), eq("patient-1"), any(), any()))
                .thenReturn(1L);

        mockMvc.perform(get("/fhir/Encounter/count-inpatient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("2")));

        mockMvc.perform(get("/fhir/Encounter/count-emergency")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1")));

        mockMvc.perform(get("/fhir/Encounter/_health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Encounter")));
    }

    @Test
    @DisplayName("Should return bad request when encounter count checks fail")
    void shouldReturnBadRequestWhenCountChecksFail() throws Exception {
        when(encounterService.countInpatientEncounters(eq(TENANT_ID), eq("patient-1"), any(), any()))
                .thenThrow(new IllegalArgumentException("inpatient count error"));
        when(encounterService.countEmergencyEncounters(eq(TENANT_ID), eq("patient-1"), any(), any()))
                .thenThrow(new IllegalArgumentException("emergency count error"));

        mockMvc.perform(get("/fhir/Encounter/count-inpatient")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("inpatient count error")));

        mockMvc.perform(get("/fhir/Encounter/count-emergency")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "patient-1")
                        .param("date-start", "2024-01-01T00:00:00")
                        .param("date-end", "2024-01-02T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("emergency count error")));
    }

    private Encounter buildEncounter(UUID id) {
        Encounter encounter = new Encounter();
        encounter.setId(id.toString());
        encounter.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        return encounter;
    }
}
