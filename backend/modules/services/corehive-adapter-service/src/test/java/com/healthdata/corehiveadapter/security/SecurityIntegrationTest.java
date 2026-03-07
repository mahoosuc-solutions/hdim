package com.healthdata.corehiveadapter.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "external.corehive.enabled=true",
        "external.corehive.api-key=test-key-abc123",
        "spring.profiles.active=integration",
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.listener.autoStartup=false",
        "spring.datasource.url=jdbc:h2:mem:corehive_security_test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "management.health.redis.enabled=false"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusEndpoint_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/external/corehive/status"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // Actuator should be accessible without auth (not 401/403)
                    assert s != 401 && s != 403 : "Expected actuator to be accessible but got " + s;
                });
    }

    @Test
    void scoreEndpoint_noApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/external/corehive/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "tenant-1")
                        .content("{\"patientId\":\"p1\",\"careGaps\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scoreEndpoint_wrongApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/external/corehive/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", "wrong-key")
                        .header("X-Tenant-ID", "tenant-1")
                        .content("{\"patientId\":\"p1\",\"careGaps\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scoreEndpoint_validApiKey_doesNotReturn401or403() throws Exception {
        mockMvc.perform(post("/api/v1/external/corehive/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", "test-key-abc123")
                        .header("X-Tenant-ID", "tenant-1")
                        .content("{\"patientId\":\"p1\",\"careGaps\":[]}"))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    // Should NOT be 401 or 403 — any other status (200, 400, 500) means auth passed
                    assert s != 401 && s != 403 : "Expected auth to pass but got " + s;
                });
    }
}
