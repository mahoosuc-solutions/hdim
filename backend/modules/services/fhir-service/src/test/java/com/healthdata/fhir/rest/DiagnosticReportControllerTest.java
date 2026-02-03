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
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.DiagnosticReport;
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

import com.healthdata.fhir.service.DiagnosticReportService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnosticReport Controller Tests")
class DiagnosticReportControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private DiagnosticReportService diagnosticReportService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DiagnosticReportController controller = new DiagnosticReportController(diagnosticReportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create diagnostic report")
    void shouldCreateDiagnosticReport() throws Exception {
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.createDiagnosticReport(eq(TENANT_ID), any(DiagnosticReport.class), eq("user")))
                .thenReturn(report);

        mockMvc.perform(post("/DiagnosticReport")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(report)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get diagnostic report")
    void shouldGetDiagnosticReport() throws Exception {
        UUID reportId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(reportId.toString());
        when(diagnosticReportService.getDiagnosticReport(TENANT_ID, reportId))
                .thenReturn(Optional.of(report));

        mockMvc.perform(get("/DiagnosticReport/{id}", reportId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should return not found when report missing")
    void shouldReturnNotFoundWhenReportMissing() throws Exception {
        UUID reportId = UUID.randomUUID();
        when(diagnosticReportService.getDiagnosticReport(TENANT_ID, reportId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/DiagnosticReport/{id}", reportId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update diagnostic report")
    void shouldUpdateDiagnosticReport() throws Exception {
        UUID reportId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(reportId.toString());
        when(diagnosticReportService.updateDiagnosticReport(eq(TENANT_ID), eq(reportId), any(DiagnosticReport.class), eq("user")))
                .thenReturn(report);

        mockMvc.perform(put("/DiagnosticReport/{id}", reportId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(report)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should delete diagnostic report")
    void shouldDeleteDiagnosticReport() throws Exception {
        UUID reportId = UUID.randomUUID();

        mockMvc.perform(delete("/DiagnosticReport/{id}", reportId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search reports with invalid references")
    void shouldSearchReportsWithInvalidReferences() throws Exception {
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.searchReports(
                eq(TENANT_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(report)));

        mockMvc.perform(get("/DiagnosticReport")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/not-a-uuid")
                        .param("encounter", "Encounter/not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")))
                .andExpect(content().string(containsString(report.getId())));

        verify(diagnosticReportService).searchReports(
                eq(TENANT_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Should search reports with references")
    void shouldSearchReportsWithReferences() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID encounterId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.searchReports(
                eq(TENANT_ID),
                eq(patientId),
                eq(encounterId),
                eq("final"),
                eq("718-7"),
                eq("LAB"),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(report)));

        mockMvc.perform(get("/DiagnosticReport")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("encounter", "Encounter/" + encounterId)
                        .param("status", "final")
                        .param("code", "718-7")
                        .param("category", "LAB")
                        .param("_page", "0")
                        .param("_count", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get reports by patient")
    void shouldGetReportsByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getReportsByPatient(TENANT_ID, patientId))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get final reports")
    void shouldGetFinalReports() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getFinalReports(TENANT_ID, patientId))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/final", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get pending reports")
    void shouldGetPendingReports() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getPendingReports(TENANT_ID, patientId))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/pending", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get lab reports")
    void shouldGetLabReports() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getLabReports(TENANT_ID, patientId))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/lab", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get imaging reports")
    void shouldGetImagingReports() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getImagingReports(TENANT_ID, patientId))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/imaging", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get reports by encounter")
    void shouldGetReportsByEncounter() throws Exception {
        UUID encounterId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getReportsByEncounter(TENANT_ID, encounterId))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/encounter/{encounterId}", encounterId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should return not found when latest report missing")
    void shouldReturnNotFoundWhenLatestMissing() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(diagnosticReportService.getLatestReportByCode(TENANT_ID, patientId, "718-7"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/code/{code}/latest", patientId, "718-7")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return latest report when present")
    void shouldReturnLatestReportWhenPresent() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getLatestReportByCode(TENANT_ID, patientId, "718-7"))
                .thenReturn(Optional.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/code/{code}/latest", patientId, "718-7")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }

    @Test
    @DisplayName("Should get reports by date range")
    void shouldGetReportsByDateRange() throws Exception {
        UUID patientId = UUID.randomUUID();
        DiagnosticReport report = new DiagnosticReport();
        report.setId(UUID.randomUUID().toString());
        when(diagnosticReportService.getReportsByDateRange(eq(TENANT_ID), eq(patientId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/DiagnosticReport/patient/{patientId}/date-range", patientId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("start", "2025-01-01T00:00:00Z")
                        .param("end", "2025-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(report.getId())));
    }
}
