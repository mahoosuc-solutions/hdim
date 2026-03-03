package com.healthdata.fhir.bulk;

import static com.healthdata.test.TestTenantConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.healthdata.fhir.config.AbstractFhirIntegrationTest;

/**
 * Integration tests for BulkImportController — proves async NDJSON import
 * lifecycle: initiate -> poll -> tenant isolation.
 */
class BulkImportControllerIT extends AbstractFhirIntegrationTest {

    private static final String TENANT_1 = PRIMARY_TENANT_ID;
    private static final String TENANT_2 = SECONDARY_TENANT_ID;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BulkImportRepository importRepository;

    @BeforeEach
    void cleanDatabase() {
        importRepository.deleteAll();
    }

    @Test
    void shouldInitiateImportJob() throws Exception {
        String ndjson = """
                {"resourceType":"Patient","name":[{"family":"ImportTest","given":["Alpha"]}],"gender":"male"}
                {"resourceType":"Patient","name":[{"family":"ImportTest","given":["Beta"]}],"gender":"female"}
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "patients.ndjson", "application/x-ndjson",
                ndjson.getBytes(StandardCharsets.UTF_8));

        String location = mockMvc.perform(multipart("/api/v1/$import")
                        .file(file)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(status().isAccepted())
                .andExpect(header().exists("Content-Location"))
                .andReturn()
                .getResponse()
                .getHeader("Content-Location");

        assertThat(location).contains("$import-poll-status");
    }

    @Test
    void shouldPollJobStatus() throws Exception {
        // Seed a job directly in the DB
        BulkImportJob job = BulkImportJob.builder()
                .jobId(UUID.randomUUID())
                .tenantId(TENANT_1)
                .status(BulkImportJob.ImportStatus.IN_PROGRESS)
                .processedRecords(50L)
                .failedRecords(2L)
                .submittedAt(Instant.now())
                .submittedBy("test-user")
                .build();
        job = importRepository.save(job);

        String response = mockMvc.perform(get("/api/v1/$import-poll-status/{jobId}", job.getJobId())
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("IN_PROGRESS");
        assertThat(response).contains("50");
    }

    @Test
    void shouldRejectWhenConcurrentLimitExceeded() throws Exception {
        // Seed 3 active jobs for the tenant
        for (int i = 0; i < 3; i++) {
            importRepository.save(BulkImportJob.builder()
                    .jobId(UUID.randomUUID())
                    .tenantId(TENANT_1)
                    .status(BulkImportJob.ImportStatus.IN_PROGRESS)
                    .processedRecords(0L)
                    .failedRecords(0L)
                    .submittedAt(Instant.now())
                    .submittedBy("test-user")
                    .build());
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "overflow.ndjson", "application/x-ndjson",
                "{\"resourceType\":\"Patient\"}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/$import")
                        .file(file)
                        .header("X-Tenant-Id", TENANT_1))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldEnforceTenantIsolation() throws Exception {
        // Create job for tenant-1
        BulkImportJob job = importRepository.save(BulkImportJob.builder()
                .jobId(UUID.randomUUID())
                .tenantId(TENANT_1)
                .status(BulkImportJob.ImportStatus.COMPLETED)
                .processedRecords(100L)
                .failedRecords(0L)
                .submittedAt(Instant.now())
                .submittedBy("test-user")
                .build());

        // Polling from tenant-2 should get 404
        mockMvc.perform(get("/api/v1/$import-poll-status/{jobId}", job.getJobId())
                        .header("X-Tenant-Id", TENANT_2))
                .andExpect(status().isNotFound());
    }
}
