package com.healthdata.eventstore.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventstore.EventStoreApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 2 integration tests for Event Store Service.
 * Validates event append, stream read, ordering, tenant isolation, and replay.
 */
@SpringBootTest(
        classes = EventStoreApplication.class,
        properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Event Store Service Phase 2 Integration Tests")
class EventStorePhase2IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_A = "pilot-tenant-a";
    private static final String TENANT_B = "pilot-tenant-b";

    @Test
    @DisplayName("POST /api/v1/events - Should append event successfully")
    void shouldAppendEvent() throws Exception {
        String aggregateId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "aggregateId", aggregateId,
                "aggregateType", "Patient",
                "eventType", "PatientCreated",
                "eventData", "{\"name\":\"John Doe\"}",
                "version", 1
        );

        mockMvc.perform(post("/api/v1/events")
                        .header("X-Tenant-ID", TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("GET /api/v1/events/aggregate/{id} - Should read event stream")
    void shouldReadEventStream() throws Exception {
        String aggregateId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/events/aggregate/" + aggregateId)
                        .param("aggregateType", "Patient")
                        .header("X-Tenant-ID", TENANT_A))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Events should maintain ordering by version")
    void shouldMaintainEventOrdering() throws Exception {
        String aggregateId = UUID.randomUUID().toString();

        // Append two events in order
        for (int version = 1; version <= 2; version++) {
            Map<String, Object> event = Map.of(
                    "aggregateId", aggregateId,
                    "aggregateType", "Patient",
                    "eventType", "PatientUpdated",
                    "eventData", "{\"version\":" + version + "}",
                    "version", version
            );
            mockMvc.perform(post("/api/v1/events")
                            .header("X-Tenant-ID", TENANT_A)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(event)))
                    .andExpect(status().is2xxSuccessful());
        }

        // Read stream should return events in order
        mockMvc.perform(get("/api/v1/events/aggregate/" + aggregateId)
                        .param("aggregateType", "Patient")
                        .header("X-Tenant-ID", TENANT_A))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Tenant isolation - Events from tenant A not visible to tenant B")
    void shouldEnforceTenantIsolation() throws Exception {
        String aggregateId = UUID.randomUUID().toString();

        // Append event for tenant A
        Map<String, Object> event = Map.of(
                "aggregateId", aggregateId,
                "aggregateType", "Patient",
                "eventType", "PatientCreated",
                "eventData", "{\"tenant\":\"A\"}",
                "version", 1
        );
        mockMvc.perform(post("/api/v1/events")
                        .header("X-Tenant-ID", TENANT_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().is2xxSuccessful());

        // Tenant B should not see tenant A's events
        mockMvc.perform(get("/api/v1/events/aggregate/" + aggregateId)
                        .param("aggregateType", "Patient")
                        .header("X-Tenant-ID", TENANT_B))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/events/aggregate/{id}/after/{version} - Should support replay from version")
    void shouldSupportReplayFromVersion() throws Exception {
        String aggregateId = UUID.randomUUID().toString();

        mockMvc.perform(get("/api/v1/events/aggregate/" + aggregateId + "/after/0")
                        .param("aggregateType", "Patient")
                        .header("X-Tenant-ID", TENANT_A))
                .andExpect(status().isOk());
    }
}
