package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BulkExportControllerTest {

    @Mock
    private BulkExportService exportService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void systemExportShouldUseSecurityContextAndBuildQueryString() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user-1", "n/a");
        auth.setDetails(Map.of("tenantId", "tenant-1"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID jobId = UUID.randomUUID();
        when(exportService.kickOffExport(eq("tenant-1"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), eq(null), any(), any(), eq("user-1")))
                .thenReturn(jobId);

        mockMvc.perform(get("/api/v1/$export")
                        .param("_type", "Patient,Observation")
                        .param("_since", "not-a-date")
                        .param("_typeFilter", "Observation?code=abc"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.CONTENT_LOCATION,
                        "/fhir/$export-poll-status/" + jobId));

        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
        verify(exportService).kickOffExport(eq("tenant-1"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), eq(null), any(), requestCaptor.capture(), eq("user-1"));
        assertThat(requestCaptor.getValue()).contains("_type=Patient,Observation")
                .contains("_since=not-a-date")
                .contains("_typeFilter=Observation?code=abc");
    }

    @Test
    void systemExportShouldDefaultTenantAndUser() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        when(exportService.kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), eq(null), any(), any(), eq("system")))
                .thenReturn(jobId);

        mockMvc.perform(get("/api/v1/$export"))
                .andExpect(status().isAccepted());

        verify(exportService).kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), eq(null), any(), any(), eq("system"));
    }

    @Test
    void systemExportShouldRejectWhenLimitExceeded() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(exportService.kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), any(), any(), any(), eq("system")))
                .thenThrow(new BulkExportService.ExportLimitExceededException("limit"));

        mockMvc.perform(get("/api/v1/$export"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void patientExportShouldRejectWhenLimitExceeded() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(exportService.kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.PATIENT),
                eq(null), any(), any(), any(), any(), eq("system")))
                .thenThrow(new BulkExportService.ExportLimitExceededException("limit"));

        mockMvc.perform(get("/api/v1/Patient/$export"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void groupExportShouldRejectWhenLimitExceeded() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        when(exportService.kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.GROUP),
                eq("group-1"), any(), any(), any(), any(), eq("system")))
                .thenThrow(new BulkExportService.ExportLimitExceededException("limit"));

        mockMvc.perform(get("/api/v1/Group/{id}/$export", "group-1"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void systemExportShouldParseValidSinceParam() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        when(exportService.kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), any(), any(), any(), eq("system")))
                .thenReturn(jobId);

        mockMvc.perform(get("/api/v1/$export")
                        .param("_since", "2024-01-01T00:00:00Z"))
                .andExpect(status().isAccepted());

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(exportService).kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.SYSTEM),
                eq(null), any(), sinceCaptor.capture(), any(), any(), eq("system"));
        assertThat(sinceCaptor.getValue()).isEqualTo(Instant.parse("2024-01-01T00:00:00Z"));
    }

    @Test
    void patientAndGroupExportShouldStartJobs() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user-2", "n/a");
        auth.setDetails(Map.of("tenantId", "tenant-2"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID patientJobId = UUID.randomUUID();
        UUID groupJobId = UUID.randomUUID();
        when(exportService.kickOffExport(eq("tenant-2"), eq(BulkExportJob.ExportLevel.PATIENT),
                eq(null), any(), any(), any(), any(), eq("user-2"))).thenReturn(patientJobId);
        when(exportService.kickOffExport(eq("tenant-2"), eq(BulkExportJob.ExportLevel.GROUP),
                eq("group-1"), any(), any(), any(), any(), eq("user-2"))).thenReturn(groupJobId);

        mockMvc.perform(get("/api/v1/Patient/$export")
                        .param("_type", "Patient"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.CONTENT_LOCATION,
                        "/fhir/$export-poll-status/" + patientJobId));

        mockMvc.perform(get("/api/v1/Group/{id}/$export", "group-1"))
                .andExpect(status().isAccepted())
                .andExpect(header().string(HttpHeaders.CONTENT_LOCATION,
                        "/fhir/$export-poll-status/" + groupJobId));
    }

    @Test
    void patientExportShouldBuildQueryStringForSinceOnly() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        when(exportService.kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.PATIENT),
                eq(null), any(), any(), any(), any(), eq("system")))
                .thenReturn(jobId);

        mockMvc.perform(get("/api/v1/Patient/$export")
                        .param("_since", "2024-01-01T00:00:00Z"))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
        verify(exportService).kickOffExport(eq("default"), eq(BulkExportJob.ExportLevel.PATIENT),
                eq(null), any(), any(), any(), requestCaptor.capture(), eq("system"));
        assertThat(requestCaptor.getValue()).contains("?_since=2024-01-01T00:00:00Z");
    }

    @Test
    void pollStatusShouldReturnExpectedResponses() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID pendingId = UUID.randomUUID();
        BulkExportJob pending = BulkExportJob.builder()
                .jobId(pendingId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.IN_PROGRESS)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", pendingId)).thenReturn(java.util.Optional.of(pending));

        mockMvc.perform(get("/api/v1/$export-poll-status/{jobId}", pendingId))
                .andExpect(status().isAccepted())
                .andExpect(header().string("X-Progress", "IN_PROGRESS"));

        UUID completedId = UUID.randomUUID();
        BulkExportJob completed = BulkExportJob.builder()
                .jobId(completedId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", completedId)).thenReturn(java.util.Optional.of(completed));
        when(exportService.buildManifest(completed))
                .thenReturn(new BulkExportService.ExportManifest(
                        Instant.now(),
                        "/fhir/$export",
                        true,
                        List.of(),
                        List.of()));

        mockMvc.perform(get("/api/v1/$export-poll-status/{jobId}", completedId))
                .andExpect(status().isOk());

        UUID failedId = UUID.randomUUID();
        BulkExportJob failed = BulkExportJob.builder()
                .jobId(failedId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.FAILED)
                .errorMessage("boom")
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", failedId)).thenReturn(java.util.Optional.of(failed));

        mockMvc.perform(get("/api/v1/$export-poll-status/{jobId}", failedId))
                .andExpect(status().isInternalServerError());

        UUID cancelledId = UUID.randomUUID();
        BulkExportJob cancelled = BulkExportJob.builder()
                .jobId(cancelledId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.CANCELLED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", cancelledId)).thenReturn(java.util.Optional.of(cancelled));

        mockMvc.perform(get("/api/v1/$export-poll-status/{jobId}", cancelledId))
                .andExpect(status().isGone());
    }

    @Test
    void pollStatusShouldReturnNotFoundWhenJobMissing() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        when(exportService.getJobStatus("default", jobId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/$export-poll-status/{jobId}", jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void pollStatusShouldReturnBadRequestForInvalidId() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/$export-poll-status/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelExportShouldHandleStatuses() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "user-3", "n/a");
        auth.setDetails(Map.of("tenantId", "tenant-3"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        UUID jobId = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/$export-poll-status/{jobId}", jobId))
                .andExpect(status().isAccepted());

        doThrow(new BulkExportService.ExportJobNotFoundException("missing"))
                .when(exportService).cancelJob("tenant-3", jobId, "user-3");
        mockMvc.perform(delete("/api/v1/$export-poll-status/{jobId}", jobId))
                .andExpect(status().isNotFound());

        doThrow(new IllegalStateException("invalid")).when(exportService).cancelJob("tenant-3", jobId, "user-3");
        mockMvc.perform(delete("/api/v1/$export-poll-status/{jobId}", jobId))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelExportShouldReturnBadRequestForInvalidId() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(delete("/api/v1/$export-poll-status/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadFileShouldReturnBadRequestForInvalidId() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvc.perform(get("/api/v1/download/not-a-uuid/file.ndjson"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadFileShouldReturnNotFoundForMissingFile() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .outputFiles(List.of(BulkExportJob.OutputFile.builder()
                        .type("Patient")
                        .url("/fhir/download/" + jobId + "/patient.ndjson")
                        .filePath("/tmp/missing.ndjson")
                        .count(1L)
                        .build()))
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", jobId)).thenReturn(java.util.Optional.of(job));

        mockMvc.perform(get("/api/v1/download/{jobId}/patient.ndjson", jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFileShouldReturnNotFoundWhenJobMissing() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        when(exportService.getJobStatus("default", jobId)).thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/api/v1/download/{jobId}/patient.ndjson", jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFileShouldReturnNotFoundWhenFileNotInJob() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .outputFiles(List.of(BulkExportJob.OutputFile.builder()
                        .type("Patient")
                        .url("/fhir/download/" + jobId + "/patient.ndjson")
                        .filePath("/tmp/patient.ndjson")
                        .count(1L)
                        .build()))
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", jobId)).thenReturn(java.util.Optional.of(job));

        mockMvc.perform(get("/api/v1/download/{jobId}/other.ndjson", jobId))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFileShouldReturnFileWhenPresent() throws Exception {
        BulkExportController controller = new BulkExportController(exportService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        Path tempFile = Files.createTempFile("bulk-export", ".ndjson");
        Files.writeString(tempFile, "{}");
        UUID jobId = UUID.randomUUID();
        BulkExportJob job = BulkExportJob.builder()
                .jobId(jobId)
                .tenantId("default")
                .status(BulkExportJob.ExportStatus.COMPLETED)
                .exportLevel(BulkExportJob.ExportLevel.SYSTEM)
                .outputFiles(List.of(BulkExportJob.OutputFile.builder()
                        .type("Patient")
                        .url("/fhir/download/" + jobId + "/" + tempFile.getFileName())
                        .filePath(tempFile.toString())
                        .count(1L)
                        .build()))
                .requestedAt(Instant.now())
                .build();
        when(exportService.getJobStatus("default", jobId)).thenReturn(java.util.Optional.of(job));

        mockMvc.perform(get("/api/v1/download/{jobId}/{fileName}",
                        jobId, tempFile.getFileName().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + tempFile.getFileName() + "\""));
    }
}
