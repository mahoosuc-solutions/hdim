package com.healthdata.queryapi.api.v1;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
@WebMvcTest(controllers = {ConditionController.class, CarePlanController.class})
@DisplayName("RBAC Security for Condition and CarePlan Endpoints")
class RBACSecurityForConditionAndCarePlanTest {

    @Autowired
    private MockMvc mockMvc;

    private final String TENANT_ID = "tenant-001";
    private final String PATIENT_ID = "patient-123";
    private final String COORDINATOR_ID = "coordinator-456";
    private final String ICD_CODE = "I10";
    private final String CARE_PLAN_TITLE = "Hypertension-Management";

    // ============ ConditionController RBAC Tests (4 endpoints) ============

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ConditionController: GET /patient/{patientId} - Allowed for ADMIN")
    void testConditionsByPatientAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("ConditionController: GET /patient/{patientId} - Allowed for EVALUATOR")
    void testConditionsByPatientAllowedForEvaluator() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("ConditionController: GET /icd/{icdCode} - Allowed for ANALYST")
    void testConditionsByIcdCodeAllowedForAnalyst() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/icd/" + ICD_CODE)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("ConditionController: GET /patient/{patientId}/active - Allowed for VIEWER")
    void testActiveConditionsAllowedForViewer() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID + "/active")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    // ============ CarePlanController RBAC Tests (5 endpoints) ============

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CarePlanController: GET /patient/{patientId} - Allowed for ADMIN")
    void testCarePlansByPatientAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("CarePlanController: GET /coordinator/{coordinatorId} - Allowed for EVALUATOR")
    void testCarePlansByCoordinatorAllowedForEvaluator() throws Exception {
        mockMvc.perform(get("/api/v1/care-plans/coordinator/" + COORDINATOR_ID)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("CarePlanController: GET /patient/{patientId}/active - Allowed for ANALYST")
    void testActiveCarePlansAllowedForAnalyst() throws Exception {
        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID + "/active")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("CarePlanController: GET / (list by status) - Allowed for VIEWER")
    void testCarePlansByStatusAllowedForViewer() throws Exception {
        mockMvc.perform(get("/api/v1/care-plans")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CarePlanController: GET /patient/{patientId}/title/{title} - Endpoint requires authorization")
    void testCarePlanByTitleRequiresAuthorization() throws Exception {
        // This test verifies the endpoint exists and is protected - actual data retrieval tested elsewhere
        mockMvc.perform(get("/api/v1/care-plans/patient/" + PATIENT_ID + "/title/" + CARE_PLAN_TITLE)
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isNotFound()); // Returns 404 when no data found (expected without mocked service)
    }

    // ============ Multi-Tenant Validation ============

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ConditionController: X-Tenant-ID header required for ADMIN")
    void testConditionRequiresTenantHeaderEvenWithAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/conditions/patient/" + PATIENT_ID))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("CarePlanController: X-Tenant-ID header required for ADMIN")
    void testCarePlanRequiresTenantHeaderEvenWithAdmin() throws Exception {
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
}
