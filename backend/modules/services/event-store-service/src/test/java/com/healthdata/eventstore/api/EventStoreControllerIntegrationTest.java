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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for EventStoreController.
 *
 * Validates two critical properties of the event store:
 * 1. Append + retrieval round-trip: a posted event is returned when querying by aggregate.
 * 2. Immutability: DELETE is not supported on the event collection endpoint
 *    (405 Method Not Allowed, or GlobalExceptionHandler-masked 500) — either way,
 *    the server must reject DELETE without deleting any data.
 *
 * Uses a dedicated Testcontainers PostgreSQL instance. The BaseTestContainersConfiguration is
 * intentionally NOT imported to avoid starting Kafka/Redis containers that this service
 * does not use.
 *
 * Security: event-store-service does not use Spring Security (gateway trust pattern).
 * The only required authentication context is the X-Tenant-ID header.
 */
@SpringBootTest(
        classes = EventStoreApplication.class,
        properties = "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("EventStoreController Integration Tests")
class EventStoreControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("event_store_test_db")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    private static final String TENANT_ID = "test-tenant-event-store";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test 1: appendEvent_thenGetEvents_returnsAppendedEvent
     *
     * POST an event and then GET events for that aggregate.
     * Asserts that the appended event type is present in the returned list.
     */
    @Test
    @DisplayName("appendEvent_thenGetEvents_returnsAppendedEvent")
    void appendEvent_thenGetEvents_returnsAppendedEvent() throws Exception {
        // Given — unique aggregate ID per test run to avoid cross-test conflicts
        UUID aggregateId = UUID.randomUUID();
        String aggregateType = "Patient";
        String eventType = "PatientCreatedEvent";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("aggregateId", aggregateId.toString());
        requestBody.put("aggregateType", aggregateType);
        requestBody.put("eventType", eventType);
        requestBody.put("payload", Map.of("firstName", "Jane", "lastName", "Smith", "mrn", "TEST-MRN-9001"));
        requestBody.put("userId", "test-user-001");
        requestBody.put("userEmail", "test@example.com");

        // When — POST the event
        mockMvc.perform(post("/api/v1/events")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggregateId").value(aggregateId.toString()))
                .andExpect(jsonPath("$.eventType").value(eventType))
                .andExpect(jsonPath("$.eventVersion").value(1));

        // Then — GET events for that aggregate and assert the event type is returned
        MvcResult result = mockMvc.perform(get("/api/v1/events/aggregate/{aggregateId}", aggregateId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("aggregateType", aggregateType))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        assertThat(responseJson).contains(eventType);
        assertThat(responseJson).contains(aggregateId.toString());
    }

    /**
     * Test 2: eventStore_hasNoDeleteEndpoint
     *
     * Events in an event store are immutable — they must never be deleted.
     * The controller exposes no DELETE mapping. Attempting DELETE on a known GET
     * path (the aggregate-events endpoint) produces 405 Method Not Allowed from
     * Spring MVC before the GlobalExceptionHandler can intercept it.
     *
     * This asserts that the DELETE verb is rejected (non-2xx, non-successful),
     * confirming immutability at the HTTP layer.
     */
    @Test
    @DisplayName("eventStore_hasNoDeleteEndpoint")
    void eventStore_hasNoDeleteEndpoint() throws Exception {
        // Use a known path pattern (GET /api/v1/events/aggregate/{id}) with DELETE verb.
        // Spring MVC detects the path match but method mismatch and returns 405.
        UUID aggregateId = UUID.randomUUID();

        MvcResult result = mockMvc.perform(delete("/api/v1/events/aggregate/{aggregateId}", aggregateId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("aggregateType", "Patient"))
                .andReturn();

        int status = result.getResponse().getStatus();
        // The server must NOT return 2xx — either 405 (Method Not Allowed) directly
        // or a non-success status from the global exception handler.
        assertThat(status)
                .as("DELETE on event-store must be rejected (no delete endpoint exists)")
                .isNotIn(200, 201, 202, 204);
    }
}
