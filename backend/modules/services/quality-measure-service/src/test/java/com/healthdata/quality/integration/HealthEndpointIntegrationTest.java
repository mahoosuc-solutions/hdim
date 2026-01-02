package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Health Check endpoint
 * Tests the /quality-measure/_health endpoint
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Health Endpoint Integration Tests")
class HealthEndpointIntegrationTest {

    private static final String HEALTH_ENDPOINT = "/quality-measure/_health";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return UP status on health check")
    void shouldReturnUpStatus() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("quality-measure-service"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Should not require tenant ID for health check")
    void shouldNotRequireTenantIdForHealthCheck() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return valid JSON format")
    void shouldReturnValidJsonFormat() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.service").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("Should include current date in timestamp")
    void shouldIncludeCurrentDateInTimestamp() throws Exception {
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}")));
    }

    @Test
    @DisplayName("Should respond quickly to health checks")
    void shouldRespondQuickly() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // Health check should respond in less than 1 second
        assert duration < 1000 : "Health check took too long: " + duration + "ms";
    }

    @Test
    @DisplayName("Should be accessible without authentication")
    void shouldBeAccessibleWithoutAuthentication() throws Exception {
        // Health endpoint should be public for monitoring
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should support GET method only")
    void shouldSupportGetMethodOnly() throws Exception {
        // GET should work
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // POST should not be allowed
        mockMvc.perform(post(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Should return consistent status across multiple calls")
    void shouldReturnConsistentStatus() throws Exception {
        // First call
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Second call
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Third call
        mockMvc.perform(get(HEALTH_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
