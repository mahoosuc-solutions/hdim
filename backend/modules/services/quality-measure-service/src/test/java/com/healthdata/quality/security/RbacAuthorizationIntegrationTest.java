package com.healthdata.quality.security;

import com.healthdata.quality.client.CqlEngineServiceClient;
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC Authorization Integration Tests
 *
 * Tests @PreAuthorize enforcement on controller methods.
 * Uses "demo" profile to enable full security filter chain.
 *
 * The 'test' profile disables security with permitAll() for E2E testing,
 * but RBAC tests require the full security configuration to validate
 * role-based access control properly.
 *
 * STATUS (as of 2026-01-12):
 * ✅ FIXED - All 4 RBAC tests should now pass
 *
 * Root cause was identified: GatewayTrustTestHeaders was not including the required
 * X-Auth-Validated header, which prevented TrustedHeaderAuthFilter from processing
 * authentication headers. The filter requires this header to trust gateway-injected
 * authentication context.
 *
 * Solution: Modified GatewayTrustTestHeaders to always include X-Auth-Validated
 * header with a development-mode signature ("gateway-{timestamp}-dev-signature").
 * This is safe because:
 * 1. Test profile uses gateway.auth.dev-mode: true (accepts simple signatures)
 * 2. Production requires dev-mode: false and proper HMAC validation
 * 3. Authorization (@PreAuthorize) is still enforced regardless of auth method
 *
 * See: /backend/docs/RBAC_AUTHENTICATION_FIX.md for complete analysis
 */
@Tag("integration")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.kafka.enabled=false",
        "spring.kafka.bootstrap-servers=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.liquibase.enabled=false",  // Disable Liquibase for RBAC tests
        "spring.jpa.hibernate.ddl-auto=create-drop",  // Use Hibernate DDL for test database
        "spring.main.allow-bean-definition-overriding=true"  // Allow security config override
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("demo")  // Use "demo" profile to enable full security (@Profile("!test") beans)
@DisplayName("RBAC Authorization Integration Tests")
class RbacAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CqlEngineServiceClient cqlEngineServiceClient;

    private static final String TENANT_ID = "test-tenant-001";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String MEASURE_CDC_A1C9 = "HEDIS_CDC_A1C9";

    @Test
    @DisplayName("VIEWER role should NOT be able to calculate measures (403 Forbidden)")
    void viewerCannotCalculateMeasures() throws Exception {
        // Mock CQL response (won't be called due to RBAC blocking)
        when(cqlEngineServiceClient.evaluateCql(
            anyString(),
            anyString(),
            any(UUID.class),
            anyString()
        )).thenReturn("""
            {
                "libraryName": "HEDIS_CDC_2024",
                "measureResult": {
                    "measureName": "Diabetes Care",
                    "inNumerator": true,
                    "inDenominator": true,
                    "complianceRate": 90.0,
                    "score": 95.0
                }
            }
            """);

        var headers = GatewayTrustTestHeaders.viewerHeaders(TENANT_ID);

        // VIEWER role should be blocked by @PreAuthorize
        mockMvc.perform(post("/quality-measure/calculate")
                .headers(headers)
                .param("patient", PATIENT_ID.toString())
                .param("measure", MEASURE_CDC_A1C9))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EVALUATOR role SHOULD be able to calculate measures (201 Created)")
    void evaluatorCanCalculateMeasures() throws Exception {
        // Mock CQL response for successful calculation
        when(cqlEngineServiceClient.evaluateCql(
            anyString(),
            anyString(),
            any(UUID.class),
            anyString()
        )).thenReturn("""
            {
                "libraryName": "HEDIS_CDC_2024",
                "measureResult": {
                    "measureName": "Diabetes Care",
                    "inNumerator": true,
                    "inDenominator": true,
                    "complianceRate": 90.0,
                    "score": 95.0
                }
            }
            """);

        var headers = GatewayTrustTestHeaders.evaluatorHeaders(TENANT_ID);

        // EVALUATOR role should be allowed
        mockMvc.perform(post("/quality-measure/calculate")
                .headers(headers)
                .param("patient", PATIENT_ID.toString())
                .param("measure", MEASURE_CDC_A1C9))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.measureId").value(MEASURE_CDC_A1C9));
    }

    @Test
    @DisplayName("ADMIN role SHOULD be able to calculate measures (201 Created)")
    void adminCanCalculateMeasures() throws Exception {
        when(cqlEngineServiceClient.evaluateCql(
            anyString(),
            anyString(),
            any(UUID.class),
            anyString()
        )).thenReturn("""
            {
                "libraryName": "HEDIS_CDC_2024",
                "measureResult": {
                    "measureName": "Diabetes Care",
                    "inNumerator": true,
                    "inDenominator": true,
                    "complianceRate": 90.0,
                    "score": 95.0
                }
            }
            """);

        var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

        // ADMIN role should be allowed
        mockMvc.perform(post("/quality-measure/calculate")
                .headers(headers)
                .param("patient", PATIENT_ID.toString())
                .param("measure", MEASURE_CDC_A1C9))
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("MEASURE_DEVELOPER role SHOULD be able to view results and calculate")
    void measureDeveloperCanViewAndCalculate() throws Exception {
        var headers = GatewayTrustTestHeaders.builder()
            .tenantId(TENANT_ID)
            .roles("MEASURE_DEVELOPER")
            .build();

        // Can view results
        mockMvc.perform(get("/quality-measure/results")
                .headers(headers)
                .param("patient", PATIENT_ID.toString()))
            .andExpect(status().isOk());

        // Can calculate
        when(cqlEngineServiceClient.evaluateCql(
            anyString(),
            anyString(),
            any(UUID.class),
            anyString()
        )).thenReturn("""
            {
                "libraryName": "HEDIS_CDC_2024",
                "measureResult": {
                    "measureName": "Diabetes Care",
                    "inNumerator": true,
                    "inDenominator": true,
                    "complianceRate": 90.0,
                    "score": 95.0
                }
            }
            """);

        mockMvc.perform(post("/quality-measure/calculate")
                .headers(headers)
                .param("patient", PATIENT_ID.toString())
                .param("measure", MEASURE_CDC_A1C9))
            .andExpect(status().isCreated());
    }
}
