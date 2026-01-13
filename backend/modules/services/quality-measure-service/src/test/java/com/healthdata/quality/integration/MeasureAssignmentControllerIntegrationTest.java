package com.healthdata.quality.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.controller.MeasureAssignmentController;
import com.healthdata.quality.persistence.PatientMeasureAssignmentEntity;
import com.healthdata.quality.service.MeasureAssignmentService;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MeasureAssignmentController
 *
 * Tests all 5 REST endpoints:
 * - GET /quality-measure/patients/{patientId}/measure-assignments
 * - POST /quality-measure/patients/{patientId}/measure-assignments
 * - DELETE /quality-measure/measure-assignments/{assignmentId}
 * - PUT /quality-measure/measure-assignments/{assignmentId}/dates
 * - GET /quality-measure/patients/{patientId}/measure-assignments/count
 *
 * Validates:
 * - RBAC enforcement (EVALUATOR, ADMIN, SUPER_ADMIN roles)
 * - X-Tenant-ID header validation
 * - Request/Response DTO mapping
 * - HTTP status codes
 * - Error handling
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@DisplayName("MeasureAssignmentController Integration Tests")
class MeasureAssignmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MeasureAssignmentService assignmentService;

    private String tenantId;
    private UUID patientId;
    private UUID measureId;
    private UUID assignedBy;
    private PatientMeasureAssignmentEntity testAssignment;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT-001";
        patientId = UUID.randomUUID();
        measureId = UUID.randomUUID();
        assignedBy = UUID.randomUUID();

        testAssignment = PatientMeasureAssignmentEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .assignedBy(assignedBy)
                .assignedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .effectiveFrom(LocalDate.now())
                .active(true)
                .autoAssigned(false)
                .build();
    }

    // ========================================
    // GET /patients/{patientId}/measure-assignments
    // ========================================

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return active assignments for patient")
    void shouldReturnActiveAssignments() throws Exception {
        // Given
        List<PatientMeasureAssignmentEntity> assignments = Arrays.asList(testAssignment);
        when(assignmentService.getActiveAssignments(tenantId, patientId))
                .thenReturn(assignments);

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testAssignment.getId().toString()))
                .andExpect(jsonPath("$[0].patientId").value(patientId.toString()))
                .andExpect(jsonPath("$[0].measureId").value(measureId.toString()))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(assignmentService).getActiveAssignments(tenantId, patientId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return effective assignments when effectiveDate parameter provided")
    void shouldReturnEffectiveAssignmentsWhenDateProvided() throws Exception {
        // Given
        LocalDate effectiveDate = LocalDate.now().minusDays(30);
        List<PatientMeasureAssignmentEntity> assignments = List.of(testAssignment);
        when(assignmentService.getEffectiveAssignments(tenantId, patientId, effectiveDate))
                .thenReturn(assignments);

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .param("effectiveDate", effectiveDate.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(assignmentService).getEffectiveAssignments(tenantId, patientId, effectiveDate);
        verify(assignmentService, never()).getActiveAssignments(any(), any());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return empty list when no assignments exist")
    void shouldReturnEmptyListWhenNoAssignments() throws Exception {
        // Given
        when(assignmentService.getActiveAssignments(tenantId, patientId))
                .thenReturn(List.of());

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should deny access when user lacks required role (VIEWER not allowed)")
    void shouldDenyAccessForViewerRole() throws Exception {
        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isForbidden());

        verify(assignmentService, never()).getActiveAssignments(any(), any());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return 400 when X-Tenant-ID header missing")
    void shouldReturn400WhenTenantHeaderMissing() throws Exception {
        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments", patientId))
                .andExpect(status().isBadRequest());
    }

    // ========================================
    // POST /patients/{patientId}/measure-assignments
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully create measure assignment")
    void shouldSuccessfullyCreateMeasureAssignment() throws Exception {
        // Given
        MeasureAssignmentController.AssignMeasureRequest request = new MeasureAssignmentController.AssignMeasureRequest();
        request.setMeasureId(measureId);
        request.setAssignmentReason("Annual wellness visit");
        request.setEffectiveFrom(LocalDate.now());

        when(assignmentService.assignMeasure(
                eq(tenantId), eq(patientId), eq(measureId), any(UUID.class),
                eq("Annual wellness visit"), any(), any(), any()))
                .thenReturn(testAssignment);

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testAssignment.getId().toString()))
                .andExpect(jsonPath("$.active").value(true));

        verify(assignmentService).assignMeasure(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 409 CONFLICT when duplicate assignment exists")
    void shouldReturn409WhenDuplicateAssignment() throws Exception {
        // Given
        MeasureAssignmentController.AssignMeasureRequest request = new MeasureAssignmentController.AssignMeasureRequest();
        request.setMeasureId(measureId);
        request.setAssignmentReason("Test reason");

        when(assignmentService.assignMeasure(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("already has an active assignment"));

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when measureId is null")
    void shouldReturn400WhenMeasureIdIsNull() throws Exception {
        // Given
        MeasureAssignmentController.AssignMeasureRequest request = new MeasureAssignmentController.AssignMeasureRequest();
        request.setMeasureId(null); // Violates @NotNull
        request.setAssignmentReason("Test reason");

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(assignmentService, never()).assignMeasure(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when assignmentReason is blank")
    void shouldReturn400WhenAssignmentReasonIsBlank() throws Exception {
        // Given
        MeasureAssignmentController.AssignMeasureRequest request = new MeasureAssignmentController.AssignMeasureRequest();
        request.setMeasureId(measureId);
        request.setAssignmentReason("   "); // Violates @NotBlank

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should deny access when user lacks ADMIN role for assignment creation")
    void shouldDenyAccessForNonAdminOnCreate() throws Exception {
        // Given
        MeasureAssignmentController.AssignMeasureRequest request = new MeasureAssignmentController.AssignMeasureRequest();
        request.setMeasureId(measureId);
        request.setAssignmentReason("Test");

        // When / Then
        mockMvc.perform(post("/quality-measure/patients/{patientId}/measure-assignments", patientId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // DELETE /measure-assignments/{assignmentId}
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully deactivate assignment")
    void shouldSuccessfullyDeactivateAssignment() throws Exception {
        // Given
        UUID assignmentId = testAssignment.getId();
        testAssignment.setActive(false);

        when(assignmentService.deactivateAssignment(tenantId, assignmentId, assignedBy))
                .thenReturn(testAssignment);

        // When / Then
        mockMvc.perform(delete("/quality-measure/measure-assignments/{assignmentId}", assignmentId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(assignmentService).deactivateAssignment(tenantId, assignmentId, assignedBy);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when assignment not found for deactivation")
    void shouldReturn404WhenAssignmentNotFoundForDeactivation() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();
        when(assignmentService.deactivateAssignment(tenantId, assignmentId, assignedBy))
                .thenThrow(new IllegalArgumentException("Assignment not found"));

        // When / Then
        mockMvc.perform(delete("/quality-measure/measure-assignments/{assignmentId}", assignmentId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should deny access when user lacks ADMIN role for deactivation")
    void shouldDenyAccessForNonAdminOnDeactivate() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();

        // When / Then
        mockMvc.perform(delete("/quality-measure/measure-assignments/{assignmentId}", assignmentId)
                        .header("X-Tenant-ID", tenantId)
                        .header("X-Auth-User-Id", assignedBy.toString()))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // PUT /measure-assignments/{assignmentId}/dates
    // ========================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should successfully update effective dates")
    void shouldSuccessfullyUpdateEffectiveDates() throws Exception {
        // Given
        UUID assignmentId = testAssignment.getId();
        LocalDate newStartDate = LocalDate.now().plusDays(1);
        LocalDate newEndDate = LocalDate.now().plusDays(90);

        MeasureAssignmentController.UpdateDatesRequest request = new MeasureAssignmentController.UpdateDatesRequest();
        request.setEffectiveFrom(newStartDate);
        request.setEffectiveUntil(newEndDate);

        testAssignment.setEffectiveFrom(newStartDate);
        testAssignment.setEffectiveUntil(newEndDate);

        when(assignmentService.updateEffectiveDates(tenantId, assignmentId, newStartDate, newEndDate))
                .thenReturn(testAssignment);

        // When / Then
        // NOTE: LocalDate serializes as array [year, month, day] due to Jackson default behavior
        // Jackson configuration attempts to use ISO-8601 strings have not been successful
        // See: application.yml and application-test.yml jackson.serialization.WRITE_DATES_AS_TIMESTAMPS
        // TODO: Investigate why Spring Boot Jackson autoconfiguration is not working as expected
        mockMvc.perform(put("/quality-measure/measure-assignments/{assignmentId}/dates", assignmentId)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveFrom").isArray())
                .andExpect(jsonPath("$.effectiveFrom[0]").value(newStartDate.getYear()))
                .andExpect(jsonPath("$.effectiveFrom[1]").value(newStartDate.getMonthValue()))
                .andExpect(jsonPath("$.effectiveFrom[2]").value(newStartDate.getDayOfMonth()))
                .andExpect(jsonPath("$.effectiveUntil").isArray())
                .andExpect(jsonPath("$.effectiveUntil[0]").value(newEndDate.getYear()))
                .andExpect(jsonPath("$.effectiveUntil[1]").value(newEndDate.getMonthValue()))
                .andExpect(jsonPath("$.effectiveUntil[2]").value(newEndDate.getDayOfMonth()));

        verify(assignmentService).updateEffectiveDates(tenantId, assignmentId, newStartDate, newEndDate);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 404 when assignment not found for date update")
    void shouldReturn404WhenAssignmentNotFoundForDateUpdate() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();
        MeasureAssignmentController.UpdateDatesRequest request = new MeasureAssignmentController.UpdateDatesRequest();
        request.setEffectiveFrom(LocalDate.now());

        when(assignmentService.updateEffectiveDates(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Assignment not found"));

        // When / Then
        mockMvc.perform(put("/quality-measure/measure-assignments/{assignmentId}/dates", assignmentId)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return 400 when effectiveFrom is null")
    void shouldReturn400WhenEffectiveFromIsNull() throws Exception {
        // Given
        UUID assignmentId = UUID.randomUUID();
        MeasureAssignmentController.UpdateDatesRequest request = new MeasureAssignmentController.UpdateDatesRequest();
        request.setEffectiveFrom(null); // Violates @NotNull

        // When / Then
        mockMvc.perform(put("/quality-measure/measure-assignments/{assignmentId}/dates", assignmentId)
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========================================
    // GET /patients/{patientId}/measure-assignments/count
    // ========================================

    @Test
    @WithMockUser(roles = "EVALUATOR")
    @DisplayName("Should return assignment count")
    void shouldReturnAssignmentCount() throws Exception {
        // Given
        when(assignmentService.countActiveAssignments(tenantId, patientId))
                .thenReturn(5L);

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments/count", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));

        verify(assignmentService).countActiveAssignments(tenantId, patientId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return zero count when no assignments exist")
    void shouldReturnZeroCountWhenNoAssignments() throws Exception {
        // Given
        when(assignmentService.countActiveAssignments(tenantId, patientId))
                .thenReturn(0L);

        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments/count", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("Should deny access when user lacks required role for count endpoint")
    void shouldDenyAccessForViewerRoleOnCount() throws Exception {
        // When / Then
        mockMvc.perform(get("/quality-measure/patients/{patientId}/measure-assignments/count", patientId)
                        .header("X-Tenant-ID", tenantId))
                .andExpect(status().isForbidden());
    }
}
