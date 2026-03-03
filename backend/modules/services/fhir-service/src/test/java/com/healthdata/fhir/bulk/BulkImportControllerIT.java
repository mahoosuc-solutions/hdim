package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for BulkImportController — proves async NDJSON import
 * lifecycle: initiate → poll → tenant isolation.
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
class BulkImportControllerIT {

    private static final String TENANT_1 = "bulk-import-tenant-1";
    private static final String TENANT_2 = "bulk-import-tenant-2";

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
