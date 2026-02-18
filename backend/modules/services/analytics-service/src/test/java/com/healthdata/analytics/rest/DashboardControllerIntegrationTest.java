package com.healthdata.analytics.rest;

import com.healthdata.analytics.TestAnalyticsApplication;
import com.healthdata.analytics.dto.DashboardDto;
import com.healthdata.analytics.service.DashboardService;
import com.healthdata.audit.service.AuditService;
import com.healthdata.audit.service.ai.AIAuditEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for DashboardController.
 *
 * Uses TestAnalyticsApplication (test-only Spring Boot app class) that restricts
 * component scan to com.healthdata.analytics. Audit module beans that appear via
 * AuditAutoConfiguration are mocked to prevent Kafka/JPA infrastructure dependencies.
 *
 * Verifies:
 * - getDashboards_withAdminRole_returns200: GET /api/analytics/dashboards with ADMIN role → 200 + array
 * - getDashboards_withoutAuth_returns401Or403: GET /api/analytics/dashboards without auth → 401 or 403
 * - getDashboardById_notFound_returns404: GET /api/analytics/dashboards/{unknown-uuid} → 404
 */
@SpringBootTest(
        classes = TestAnalyticsApplication.class,
        properties = {
                "spring.datasource.url=jdbc:tc:postgresql:16:///testdb?TC_REUSABLE=true",
                "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
                "spring.datasource.username=test",
                "spring.datasource.password=test",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.liquibase.enabled=false",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                        "com.healthdata.authentication.config.AuthenticationAutoConfiguration"
        }
)
@Import(DashboardControllerIntegrationTest.TestSecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
@Tag("integration")
@DisplayName("DashboardController Integration Tests")
class DashboardControllerIntegrationTest {

    /**
     * Test security configuration.
     * Enables method security (@PreAuthorize) so @PreAuthorize annotations on
     * controller methods are enforced. Provides a permissive HTTP filter chain
     * (HTTP-level filtering is bypassed via addFilters=false anyway).
     */
    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    // Mock audit infrastructure registered by AuditAutoConfiguration
    @MockBean
    private AIAuditEventPublisher aiAuditEventPublisher;

    @MockBean
    private AuditService auditService;

    @MockBean
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    // -----------------------------------------------------------------------
    // Test 1: Authorized ADMIN → 200 with array body
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("getDashboards_withAdminRole_returns200")
    void getDashboards_withAdminRole_returns200() throws Exception {
        DashboardDto dto = DashboardDto.builder()
                .id(UUID.randomUUID())
                .name("Pilot Dashboard")
                .isDefault(true)
                .isShared(false)
                .build();

        when(dashboardService.getDashboards(anyString())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Pilot Dashboard"));
    }

    // -----------------------------------------------------------------------
    // Test 2: No authentication → 401 or 403 (method security enforced)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getDashboards_withoutAuth_returns401Or403")
    void getDashboards_withoutAuth_returns401Or403() throws Exception {
        // With addFilters=false, the HTTP security filter chain is bypassed.
        // However, @EnableMethodSecurity + @PreAuthorize remain active via Spring AOP.
        // Without authentication in the SecurityContext, Spring Security throws
        // AuthenticationCredentialsNotFoundException, which Spring MVC resolves to
        // a 401 or 403 response (not a raw Java exception to the test).
        mockMvc.perform(get("/api/analytics/dashboards")
                        .header("X-Tenant-ID", "tenant-001"))
                .andExpect(status().is(org.hamcrest.Matchers.either(
                        org.hamcrest.Matchers.is(401))
                        .or(org.hamcrest.Matchers.is(403))));
    }

    // -----------------------------------------------------------------------
    // Test 3: Authorized ADMIN + unknown UUID → 404
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("getDashboardById_notFound_returns404")
    void getDashboardById_notFound_returns404() throws Exception {
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        when(dashboardService.getDashboard(eq(unknownId), anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/analytics/dashboards/{id}", unknownId)
                        .header("X-Tenant-ID", "tenant-001"))
                .andExpect(status().isNotFound());
    }
}
