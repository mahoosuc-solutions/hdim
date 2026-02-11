package com.healthdata.quality.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.controller.MeasureOverrideController;
import com.healthdata.quality.persistence.PatientMeasureOverrideEntity;
import com.healthdata.quality.service.MeasureOverrideService;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MeasureOverrideController
 *
 * Tests all 8 REST endpoints:
 * - GET /patients/{patientId}/measure-overrides
 * - POST /patients/{patientId}/measure-overrides
 * - POST /measure-overrides/{overrideId}/approve
 * - POST /measure-overrides/{overrideId}/review
 * - DELETE /measure-overrides/{overrideId}
 * - GET /measure-overrides/pending-approval
 * - GET /measure-overrides/due-for-review
 * - POST /patients/{patientId}/measures/{measureId}/resolve-overrides
 *
 * Validates:
 * - HIPAA compliance (clinical justification requirement)
 * - RBAC enforcement (ADMIN, EVALUATOR, SUPER_ADMIN roles)
 * - X-Tenant-ID header validation
 * - Request/Response DTO mapping
 * - HTTP status codes
 * - Error handling
 */
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@DisplayName("MeasureOverrideController Integration Tests")
class MeasureOverrideControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeasureOverrideService overrideService;

    private String tenantId;
    private UUID patientId;
    private UUID measureId;
    private UUID createdBy;
    private PatientMeasureOverrideEntity testOverride;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT-001";
        patientId = UUID.randomUUID();
        measureId = UUID.randomUUID();
        createdBy = UUID.randomUUID();

        testOverride = PatientMeasureOverrideEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .overrideType("PARAMETER")
                .overrideField("minimumAge")
                .originalValue("65")
                .overrideValue("50")
                .valueType("NUMERIC")
                .clinicalReason("Patient has early onset chronic disease requiring modified screening age")
                .active(true)
                .effectiveFrom(LocalDate.now())
                .requiresPeriodicReview(true)
                .reviewFrequencyDays(90)
                .createdBy(createdBy)
                .build();
    }

    // ========================================
    // GET /patients/{patientId}/measure-overrides
    // ========================================

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return active overrides for patient and measure")
    void shouldReturnActiveOverrides() throws Exception {
        // Given
        List<PatientMeasureOverrideEntity> overrides = List.of(testOverride);
        when(overrideService.getActiveOverrides(tenantId, patientId, measureId))
                .thenReturn(overrides);

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .param("measureId", measureId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testOverride.getId().toString()))
                .andExpect(jsonPath("$[0].overrideField").value("minimumAge"))
                .andExpect(jsonPath("$[0].overrideValue").value("50"))
                .andExpect(jsonPath("$[0].clinicalReason").exists());

        verify(overrideService).getActiveOverrides(tenantId, patientId, measureId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return effective overrides when effectiveDate parameter provided")
    void shouldReturnEffectiveOverridesWhenDateProvided() throws Exception {
        // Given
        LocalDate effectiveDate = LocalDate.now().minusDays(30);
        List<PatientMeasureOverrideEntity> overrides = List.of(testOverride);
        when(overrideService.getEffectiveOverrides(tenantId, patientId, measureId, effectiveDate))
                .thenReturn(overrides);

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .param("measureId", measureId.toString())
                        .param("effectiveDate", effectiveDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(overrideService).getEffectiveOverrides(tenantId, patientId, measureId, effectiveDate);
        verify(overrideService, never()).getActiveOverrides(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return 400 when measureId parameter is missing")
    void shouldReturn400WhenMeasureIdParameterMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isBadRequest());

        verify(overrideService, never()).getActiveOverrides(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should deny access when user lacks required role (VIEWER not allowed)")
    void shouldDenyAccessForViewerRole() throws Exception {
        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .param("measureId", measureId.toString()))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // POST /patients/{patientId}/measure-overrides
    // HIPAA Compliance Tests
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully create override with valid clinical reason (HIPAA compliance)")
    void shouldSuccessfullyCreateOverrideWithClinicalReason() throws Exception {
        // Given
        MeasureOverrideController.CreateOverrideRequest request = new MeasureOverrideController.CreateOverrideRequest();
        request.setMeasureId(measureId);
        request.setOverrideType("PARAMETER");
        request.setOverrideField("minimumAge");
        request.setOriginalValue("65");
        request.setOverrideValue("50");
        request.setValueType("NUMERIC");
        request.setClinicalReason("Patient has early onset diabetes requiring modified screening criteria");
        request.setRequiresApproval(false);

        when(overrideService.createOverride(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(testOverride);

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testOverride.getId().toString()))
                .andExpect(jsonPath("$.clinicalReason").exists())
                .andExpect(jsonPath("$.active").value(true));

        verify(overrideService).createOverride(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when clinical reason is missing (HIPAA violation)")
    void shouldReturn400WhenClinicalReasonMissing() throws Exception {
        // Given
        MeasureOverrideController.CreateOverrideRequest request = new MeasureOverrideController.CreateOverrideRequest();
        request.setMeasureId(measureId);
        request.setOverrideType("PARAMETER");
        request.setOverrideField("minimumAge");
        request.setOverrideValue("50");
        // clinicalReason not set - violates @NotBlank

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(overrideService, never()).createOverride(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when clinical reason is blank (HIPAA violation)")
    void shouldReturn400WhenClinicalReasonBlank() throws Exception {
        // Given
        MeasureOverrideController.CreateOverrideRequest request = new MeasureOverrideController.CreateOverrideRequest();
        request.setMeasureId(measureId);
        request.setOverrideType("PARAMETER");
        request.setOverrideField("minimumAge");
        request.setOverrideValue("50");
        request.setClinicalReason("   "); // Blank - violates @NotBlank

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 CONFLICT when conflicting override exists for same field")
    void shouldReturn409WhenConflictingOverrideExists() throws Exception {
        // Given
        MeasureOverrideController.CreateOverrideRequest request = new MeasureOverrideController.CreateOverrideRequest();
        request.setMeasureId(measureId);
        request.setOverrideType("PARAMETER");
        request.setOverrideField("minimumAge");
        request.setOverrideValue("50");
        request.setClinicalReason("Valid reason");

        when(overrideService.createOverride(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Conflicting override already exists"));

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when required fields are missing")
    void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
        // Given
        MeasureOverrideController.CreateOverrideRequest request = new MeasureOverrideController.CreateOverrideRequest();
        // measureId not set - violates @NotNull
        request.setClinicalReason("Valid reason");

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should deny access when user lacks ADMIN role for override creation")
    void shouldDenyAccessForNonAdminOnCreate() throws Exception {
        // Given
        MeasureOverrideController.CreateOverrideRequest request = new MeasureOverrideController.CreateOverrideRequest();
        request.setMeasureId(measureId);
        request.setOverrideType("PARAMETER");
        request.setOverrideField("minimumAge");
        request.setOverrideValue("50");
        request.setClinicalReason("Valid reason");

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-overrides", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // POST /measure-overrides/{overrideId}/approve
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully approve pending override")
    void shouldSuccessfullyApprovePendingOverride() throws Exception {
        // Given
        UUID overrideId = testOverride.getId();
        UUID approvedBy = UUID.randomUUID();
        MeasureOverrideController.ApprovalRequest request = new MeasureOverrideController.ApprovalRequest();
        request.setApprovalNotes("Reviewed and approved by clinical team");

        when(overrideService.approveOverride(tenantId, overrideId, approvedBy, "Reviewed and approved by clinical team"))
                .thenReturn(testOverride);

        // When / Then
        mockMvc.perform(post("/quality-measure/measure-overrides/{overrideId}/approve", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", approvedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(overrideId.toString()));

        verify(overrideService).approveOverride(tenantId, overrideId, approvedBy, "Reviewed and approved by clinical team");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully approve override without approval notes (optional)")
    void shouldSuccessfullyApproveWithoutNotes() throws Exception {
        // Given
        UUID overrideId = testOverride.getId();
        UUID approvedBy = UUID.randomUUID();

        when(overrideService.approveOverride(tenantId, overrideId, approvedBy, null))
                .thenReturn(testOverride);

        // When / Then
        mockMvc.perform(post("/quality-measure/measure-overrides/{overrideId}/approve", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", approvedBy.toString()))
                .andExpect(status().isOk());

        verify(overrideService).approveOverride(tenantId, overrideId, approvedBy, null);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when override not found for approval")
    void shouldReturn404WhenOverrideNotFoundForApproval() throws Exception {
        // Given
        UUID overrideId = UUID.randomUUID();
        when(overrideService.approveOverride(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Override not found"));

        // When / Then
        mockMvc.perform(post("/quality-measure/measure-overrides/{overrideId}/approve", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should deny access when user lacks ADMIN role for approval")
    void shouldDenyAccessForNonAdminOnApproval() throws Exception {
        // When / Then
        mockMvc.perform(post("/quality-measure/measure-overrides/{overrideId}/approve", UUID.randomUUID())
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // POST /measure-overrides/{overrideId}/review
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully mark override as reviewed")
    void shouldSuccessfullyMarkOverrideAsReviewed() throws Exception {
        // Given
        UUID overrideId = testOverride.getId();
        UUID reviewedBy = UUID.randomUUID();

        when(overrideService.markReviewed(tenantId, overrideId, reviewedBy))
                .thenReturn(testOverride);

        // When / Then
        mockMvc.perform(post("/quality-measure/measure-overrides/{overrideId}/review", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", reviewedBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(overrideId.toString()));

        verify(overrideService).markReviewed(tenantId, overrideId, reviewedBy);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when override not found for review")
    void shouldReturn404WhenOverrideNotFoundForReview() throws Exception {
        // Given
        UUID overrideId = UUID.randomUUID();
        when(overrideService.markReviewed(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Override not found"));

        // When / Then
        mockMvc.perform(post("/quality-measure/measure-overrides/{overrideId}/review", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    // ========================================
    // DELETE /measure-overrides/{overrideId}
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully deactivate override")
    void shouldSuccessfullyDeactivateOverride() throws Exception {
        // Given
        UUID overrideId = testOverride.getId();
        testOverride.setActive(false);

        when(overrideService.deactivateOverride(tenantId, overrideId, createdBy))
                .thenReturn(testOverride);

        // When / Then
        mockMvc.perform(delete("/quality-measure/measure-overrides/{overrideId}", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(overrideService).deactivateOverride(tenantId, overrideId, createdBy);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when override not found for deactivation")
    void shouldReturn404WhenOverrideNotFoundForDeactivation() throws Exception {
        // Given
        UUID overrideId = UUID.randomUUID();
        when(overrideService.deactivateOverride(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Override not found"));

        // When / Then
        mockMvc.perform(delete("/quality-measure/measure-overrides/{overrideId}", overrideId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", createdBy.toString()))
                .andExpect(status().isNotFound());
    }

    // ========================================
    // GET /measure-overrides/pending-approval
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return pending approval overrides")
    void shouldReturnPendingApprovalOverrides() throws Exception {
        // Given
        PatientMeasureOverrideEntity pendingOverride = PatientMeasureOverrideEntity.builder()
                .id(UUID.randomUUID())
                .approvedBy(null)
                .build();
        List<PatientMeasureOverrideEntity> overrides = List.of(pendingOverride);

        when(overrideService.getPendingApprovals(tenantId))
                .thenReturn(overrides);

        // When / Then
        mockMvc.perform(get("/quality-measure/measure-overrides/pending-approval")
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(overrideService).getPendingApprovals(tenantId);
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should deny access when user lacks ADMIN role for pending approvals")
    void shouldDenyAccessForNonAdminOnPendingApprovals() throws Exception {
        // When / Then
        mockMvc.perform(get("/quality-measure/measure-overrides/pending-approval")
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // GET /measure-overrides/due-for-review
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return overrides due for review")
    void shouldReturnOverridesDueForReview() throws Exception {
        // Given
        List<PatientMeasureOverrideEntity> overrides = List.of(testOverride);
        when(overrideService.getOverridesDueForReview(eq(tenantId), any()))
                .thenReturn(overrides);

        // When / Then
        mockMvc.perform(get("/quality-measure/measure-overrides/due-for-review")
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(overrideService).getOverridesDueForReview(eq(tenantId), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should use provided asOfDate parameter when specified")
    void shouldUseProvidedAsOfDateParameter() throws Exception {
        // Given
        LocalDate asOfDate = LocalDate.now().minusDays(7);
        when(overrideService.getOverridesDueForReview(tenantId, asOfDate))
                .thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/quality-measure/measure-overrides/due-for-review")
                        .header("X-Tenant-ID", tenantId)
                        .param("asOfDate", asOfDate.toString()))
                .andExpect(status().isOk());

        verify(overrideService).getOverridesDueForReview(tenantId, asOfDate);
    }

    // ========================================
    // POST /patients/{patientId}/measures/{measureId}/resolve-overrides
    // ========================================

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should resolve all applicable overrides")
    void shouldResolveAllApplicableOverrides() throws Exception {
        // Given
        Map<String, Object> resolvedValues = Map.of(
                "minimumAge", 50.0,
                "frequencyMonths", 12
        );

        when(overrideService.resolveOverrides(eq(tenantId), eq(patientId), eq(measureId), any(LocalDate.class)))
                .thenReturn(resolvedValues);

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measures/{measureId}/resolve-overrides",
                                patientId, measureId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minimumAge").value(50.0))
                .andExpect(jsonPath("$.frequencyMonths").value(12));

        verify(overrideService).resolveOverrides(eq(tenantId), eq(patientId), eq(measureId), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should use provided evaluationDate when specified")
    void shouldUseProvidedEvaluationDate() throws Exception {
        // Given
        LocalDate evaluationDate = LocalDate.now().minusDays(30);
        when(overrideService.resolveOverrides(tenantId, patientId, measureId, evaluationDate))
                .thenReturn(Map.of());

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measures/{measureId}/resolve-overrides",
                                patientId, measureId)
                        .header("X-Tenant-ID", tenantId)
                        .param("evaluationDate", evaluationDate.toString()))
                .andExpect(status().isOk());

        verify(overrideService).resolveOverrides(tenantId, patientId, measureId, evaluationDate);
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return empty map when no overrides exist")
    void shouldReturnEmptyMapWhenNoOverridesExist() throws Exception {
        // Given
        when(overrideService.resolveOverrides(any(), any(), any(), any()))
                .thenReturn(Map.of());

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measures/{measureId}/resolve-overrides",
                                patientId, measureId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
