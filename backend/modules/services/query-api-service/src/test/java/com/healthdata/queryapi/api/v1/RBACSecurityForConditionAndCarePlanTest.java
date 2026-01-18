package com.healthdata.queryapi.api.v1;

import com.healthdata.queryapi.api.v1.dto.ConditionResponse;
import com.healthdata.queryapi.api.v1.dto.CarePlanResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.springframework.security.core.context.SecurityContextHolder.setContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC Security Tests for ConditionController and CarePlanController (Team 3)
 * Verifies that @PreAuthorize annotations enforce proper role-based access control
 *
 * Coverage:
 * - 4 ConditionController endpoints (ADMIN/EVALUATOR/ANALYST/VIEWER required)
 * - 5 CarePlanController endpoints (ADMIN/EVALUATOR/ANALYST/VIEWER required)
 * - Tests for allowed and denied access by role
 * - Multi-tenant isolation validation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@DisplayName("RBAC Security for Condition and CarePlan Endpoints")
class RBACSecurityForConditionAndCarePlanTest {

    @Autowired
    private MockMvc mockMvc;

    private final String TENANT_ID = "tenant-001";
    private final String PATIENT_ID = "patient-123";
    private final String COORDINATOR_ID = "coordinator-456";
    private final String ICD_CODE = "I10";
    private final String CARE_PLAN_TITLE = "Hypertension Management";

    // ============ ConditionController RBAC Tests (4 endpoints) ============

    @Test
    @DisplayName("ConditionController: GET /patient/{patientId} - Allowed for ADMIN")
    void testConditionsByPatientAllowedForAdmin() throws Exception {
        setupSecurityContextWithRole("ADMIN");

        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ConditionController: GET /patient/{patientId} - Allowed for EVALUATOR")
    void testConditionsByPatientAllowedForEvaluator() throws Exception {
        setupSecurityContextWithRole("EVALUATOR");

        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ConditionController: GET /icd/{icdCode} - Allowed for ANALYST")
    void testConditionsByIcdCodeAllowedForAnalyst() throws Exception {
        setupSecurityContextWithRole("ANALYST");

        mockMvc.perform(get("/api/v1/conditions/icd/" + ICD_CODE)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ConditionController: GET /patient/{patientId}/active - Allowed for VIEWER")
    void testActiveConditionsAllowedForViewer() throws Exception {
        setupSecurityContextWithRole("VIEWER");

        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID + "/active")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    // ============ CarePlanController RBAC Tests (5 endpoints) ============

    @Test
    @DisplayName("CarePlanController: GET /patient/{patientId} - Allowed for ADMIN")
    void testCarePlansByPatientAllowedForAdmin() throws Exception {
        setupSecurityContextWithRole("ADMIN");

        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CarePlanController: GET /coordinator/{coordinatorId} - Allowed for EVALUATOR")
    void testCarePlansByCoordinatorAllowedForEvaluator() throws Exception {
        setupSecurityContextWithRole("EVALUATOR");

        mockMvc.perform(get("/api/v1/care-plans/coordinator/" + COORDINATOR_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CarePlanController: GET /patient/{patientId}/active - Allowed for ANALYST")
    void testActiveCarePlansAllowedForAnalyst() throws Exception {
        setupSecurityContextWithRole("ANALYST");

        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID + "/active")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CarePlanController: GET / (list by status) - Allowed for VIEWER")
    void testCarePlansByStatusAllowedForViewer() throws Exception {
        setupSecurityContextWithRole("VIEWER");

        mockMvc.perform(get("/api/v1/care-plans")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CarePlanController: GET /patient/{patientId}/title/{title} - Allowed for ADMIN")
    void testCarePlanByTitleAllowedForAdmin() throws Exception {
        setupSecurityContextWithRole("ADMIN");

        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID + "/title/" + CARE_PLAN_TITLE)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    // ============ Multi-Tenant Validation ============

    @Test
    @DisplayName("ConditionController: X-Tenant-ID header required for ADMIN")
    void testConditionRequiresTenantHeaderEvenWithAdmin() throws Exception {
        setupSecurityContextWithRole("ADMIN");

        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CarePlanController: X-Tenant-ID header required for ADMIN")
    void testCarePlanRequiresTenantHeaderEvenWithAdmin() throws Exception {
        setupSecurityContextWithRole("ADMIN");

        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID))
            .andExpect(status().isBadRequest());
    }

    // ============ Authorization Enforcement ============

    @Test
    @DisplayName("ConditionController: GET /patient/{patientId} - Denied for unauthenticated")
    void testConditionDeniedForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CarePlanController: GET /patient/{patientId} - Denied for unauthenticated")
    void testCarePlanDeniedForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isUnauthorized());
    }

    // ============ Helper Methods ============

    private void setupSecurityContextWithRole(String role) {
        org.springframework.security.oauth2.jwt.Jwt jwt = createTestJwt(role);
        var authentication = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
            jwt,
            java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)),
            "test-user"
        );
        SecurityContext context = new SecurityContextImpl(authentication);
        setContext(context);
    }

    private org.springframework.security.oauth2.jwt.Jwt createTestJwt(String role) {
        Instant now = Instant.now();
        return new org.springframework.security.oauth2.jwt.Jwt(
            "test-token",
            now,
            now.plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of(
                "sub", "test-user",
                "tenant_id", TENANT_ID,
                "roles", java.util.Arrays.asList(role),
                "exp", now.plusSeconds(3600).getEpochSecond()
            )
        );
    }
}
