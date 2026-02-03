package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.CoverageService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Coverage Controller Tests")
@Tag("integration")
class CoverageControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private CoverageService coverageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CoverageController controller = new CoverageController(coverageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create coverage")
    void shouldCreateCoverage() throws Exception {
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        when(coverageService.createCoverage(eq(TENANT_ID), any(Coverage.class), eq("user")))
                .thenReturn(coverage);

        mockMvc.perform(post("/fhir/Coverage")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(coverage)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should get coverage")
    void shouldGetCoverage() throws Exception {
        UUID coverageId = UUID.randomUUID();
        Coverage coverage = new Coverage();
        coverage.setId(coverageId.toString());
        when(coverageService.getCoverage(TENANT_ID, coverageId))
                .thenReturn(Optional.of(coverage));

        mockMvc.perform(get("/fhir/Coverage/{id}", coverageId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should return not found when coverage missing")
    void shouldReturnNotFoundWhenCoverageMissing() throws Exception {
        UUID coverageId = UUID.randomUUID();
        when(coverageService.getCoverage(TENANT_ID, coverageId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/Coverage/{id}", coverageId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update coverage")
    void shouldUpdateCoverage() throws Exception {
        UUID coverageId = UUID.randomUUID();
        Coverage coverage = new Coverage();
        coverage.setId(coverageId.toString());
        when(coverageService.updateCoverage(eq(TENANT_ID), eq(coverageId), any(Coverage.class), eq("user")))
                .thenReturn(coverage);

        mockMvc.perform(put("/fhir/Coverage/{id}", coverageId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(coverage)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should delete coverage")
    void shouldDeleteCoverage() throws Exception {
        UUID coverageId = UUID.randomUUID();

        mockMvc.perform(delete("/fhir/Coverage/{id}", coverageId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search coverages with patient reference")
    void shouldSearchCoveragesWithPatientReference() throws Exception {
        UUID patientId = UUID.randomUUID();
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        when(coverageService.searchCoverages(
                eq(TENANT_ID),
                eq(patientId),
                eq("active"),
                eq("plan"),
                eq("subscriber"),
                eq("payor"),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(coverage)));

        mockMvc.perform(get("/fhir/Coverage")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("status", "active")
                        .param("type", "plan")
                        .param("subscriber", "subscriber")
                        .param("payor", "payor")
                        .param("_page", "0")
                        .param("_count", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should get coverages by patient")
    void shouldGetCoveragesByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        when(coverageService.getCoveragesByPatient(TENANT_ID, patientId))
                .thenReturn(List.of(coverage));

        mockMvc.perform(get("/fhir/Coverage/patient/{patientId}", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should get active coverages")
    void shouldGetActiveCoverages() throws Exception {
        UUID patientId = UUID.randomUUID();
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        when(coverageService.getActiveCoverages(TENANT_ID, patientId))
                .thenReturn(List.of(coverage));

        mockMvc.perform(get("/fhir/Coverage/patient/{patientId}/active", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should get primary coverage")
    void shouldGetPrimaryCoverage() throws Exception {
        UUID patientId = UUID.randomUUID();
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        when(coverageService.getPrimaryCoverage(TENANT_ID, patientId))
                .thenReturn(Optional.of(coverage));

        mockMvc.perform(get("/fhir/Coverage/patient/{patientId}/primary", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }

    @Test
    @DisplayName("Should return not found for missing primary coverage")
    void shouldReturnNotFoundForMissingPrimaryCoverage() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(coverageService.getPrimaryCoverage(TENANT_ID, patientId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/Coverage/patient/{patientId}/primary", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should check active coverage")
    void shouldCheckActiveCoverage() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(coverageService.hasActiveCoverage(TENANT_ID, patientId)).thenReturn(true);

        mockMvc.perform(get("/fhir/Coverage/patient/{patientId}/has-active", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
    }

    @Test
    @DisplayName("Should get coverages by subscriber")
    void shouldGetCoveragesBySubscriber() throws Exception {
        Coverage coverage = new Coverage();
        coverage.setId(UUID.randomUUID().toString());
        when(coverageService.getCoveragesBySubscriberId(TENANT_ID, "sub-1"))
                .thenReturn(List.of(coverage));

        mockMvc.perform(get("/fhir/Coverage/subscriber/{subscriberId}", "sub-1")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(coverage.getId())));
    }
}
