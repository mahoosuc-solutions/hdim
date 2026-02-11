package com.healthdata.nurseworkflow.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.nurseworkflow.application.ReferralCoordinationService;
import com.healthdata.nurseworkflow.domain.model.ReferralCoordinationEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ReferralCoordinationController
 *
 * Tests REST endpoint functionality including:
 * - Creating referrals to specialists
 * - Retrieving referral status and history
 * - Filtering by status, specialty, and priority
 * - Tracking appointment scheduling and results receipt
 * - Managing referral metrics for quality reporting
 */
@WebMvcTest(ReferralCoordinationController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReferralCoordinationController")
class ReferralCoordinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReferralCoordinationService referralCoordinationService;

    private String tenantId;
    private UUID patientId;
    private UUID coordinatorId;
    private ReferralCoordinationEntity testReferral;

    @BeforeEach
    void setUp() {
        tenantId = "TENANT001";
        patientId = UUID.randomUUID();
        coordinatorId = UUID.randomUUID();

        testReferral = ReferralCoordinationEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .coordinatorId(coordinatorId)
            .specialtyType("Cardiology")
            .status(ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION)
            .priority(ReferralCoordinationEntity.ReferralPriority.ROUTINE)
            .authorizationStatus(ReferralCoordinationEntity.AuthorizationStatus.PENDING)
            .requestedAt(Instant.now())
            .createdAt(Instant.now())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/referral-coordinations - should create referral")
    void testCreateReferral_Success() throws Exception {
        // Given
        when(referralCoordinationService.createReferral(any(ReferralCoordinationEntity.class)))
            .thenReturn(testReferral);

        String requestBody = objectMapper.writeValueAsString(testReferral);

        // When/Then
        mockMvc.perform(post("/api/v1/referral-coordinations")
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(testReferral.getId().toString()))
            .andExpect(jsonPath("$.patientId").value(patientId.toString()));

        verify(referralCoordinationService, times(1)).createReferral(any(ReferralCoordinationEntity.class));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/{id} - should return referral")
    void testGetReferral_Success() throws Exception {
        // Given
        when(referralCoordinationService.getReferralById(testReferral.getId()))
            .thenReturn(Optional.of(testReferral));

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/{id}", testReferral.getId())
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(testReferral.getId().toString()))
            .andExpect(jsonPath("$.specialtyType").value("Cardiology"));

        verify(referralCoordinationService, times(1)).getReferralById(testReferral.getId());
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/{id} - should return 404 when not found")
    void testGetReferral_NotFound() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(referralCoordinationService.getReferralById(nonExistentId))
            .thenReturn(Optional.empty());

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/{id}", nonExistentId)
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/pending - should return pending referrals")
    void testGetPendingReferrals_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<ReferralCoordinationEntity> mockPage = new PageImpl<>(
            List.of(testReferral), pageRequest, 1);

        when(referralCoordinationService.getPendingReferrals(tenantId, pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/pending")
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/patient/{patientId} - should return patient referral history")
    void testGetPatientReferralHistory_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<ReferralCoordinationEntity> mockPage = new PageImpl<>(
            List.of(testReferral), pageRequest, 1);

        when(referralCoordinationService.getPatientReferralHistory(tenantId, patientId, pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/patient/{patientId}", patientId)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].specialtyType").value("Cardiology"));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/status/{status} - should filter by status")
    void testGetReferralsByStatus_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<ReferralCoordinationEntity> mockPage = new PageImpl<>(
            List.of(testReferral), pageRequest, 1);

        when(referralCoordinationService.getReferralsByStatus(
            tenantId, ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION, pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/status/{status}",
                ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION)
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("PENDING_AUTHORIZATION"));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/specialty/{specialtyType} - should filter by specialty")
    void testGetReferralsBySpecialty_Success() throws Exception {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        Page<ReferralCoordinationEntity> mockPage = new PageImpl<>(
            List.of(testReferral), pageRequest, 1);

        when(referralCoordinationService.getReferralsBySpecialty(tenantId, "Cardiology", pageRequest))
            .thenReturn(mockPage);

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/specialty/{specialtyType}", "Cardiology")
                .header("X-Tenant-ID", tenantId)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].specialtyType").value("Cardiology"));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/awaiting-appointment-scheduling - should return referrals needing scheduling")
    void testFindAwaitingAppointmentScheduling_Success() throws Exception {
        // Given
        when(referralCoordinationService.findAwaitingAppointmentScheduling(tenantId))
            .thenReturn(List.of(testReferral));

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/awaiting-appointment-scheduling")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].specialtyType").value("Cardiology"));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/awaiting-results - should return referrals awaiting results")
    void testFindAwaitingResults_Success() throws Exception {
        // Given
        ReferralCoordinationEntity awaitingResults = ReferralCoordinationEntity.builder()
            .id(testReferral.getId())
            .tenantId(tenantId)
            .patientId(patientId)
            .coordinatorId(coordinatorId)
            .specialtyType("Cardiology")
            .status(ReferralCoordinationEntity.ReferralStatus.AWAITING_APPOINTMENT)
            .priority(ReferralCoordinationEntity.ReferralPriority.ROUTINE)
            .authorizationStatus(ReferralCoordinationEntity.AuthorizationStatus.PENDING)
            .requestedAt(Instant.now())
            .build();

        when(referralCoordinationService.findAwaitingResults(tenantId))
            .thenReturn(List.of(awaitingResults));

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/awaiting-results")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("AWAITING_APPOINTMENT"));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/urgent-awaiting-scheduling - should return urgent referrals")
    void testFindUrgentAwaitingScheduling_Success() throws Exception {
        // Given
        ReferralCoordinationEntity urgentReferral = ReferralCoordinationEntity.builder()
            .id(testReferral.getId())
            .tenantId(tenantId)
            .patientId(patientId)
            .coordinatorId(coordinatorId)
            .specialtyType("Cardiology")
            .status(ReferralCoordinationEntity.ReferralStatus.PENDING_AUTHORIZATION)
            .priority(ReferralCoordinationEntity.ReferralPriority.URGENT)
            .authorizationStatus(ReferralCoordinationEntity.AuthorizationStatus.PENDING)
            .requestedAt(Instant.now())
            .build();

        when(referralCoordinationService.findUrgentAwaitingScheduling(tenantId))
            .thenReturn(List.of(urgentReferral));

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/urgent-awaiting-scheduling")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].priority").value("URGENT"));
    }

    @Test
    @DisplayName("PUT /api/v1/referral-coordinations/{id} - should update referral")
    void testUpdateReferral_Success() throws Exception {
        // Given
        testReferral.setStatus(ReferralCoordinationEntity.ReferralStatus.AUTHORIZED);
        when(referralCoordinationService.updateReferral(any(ReferralCoordinationEntity.class)))
            .thenReturn(testReferral);

        String requestBody = objectMapper.writeValueAsString(testReferral);

        // When/Then
        mockMvc.perform(put("/api/v1/referral-coordinations/{id}", testReferral.getId())
                .header("X-Tenant-ID", tenantId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        verify(referralCoordinationService, times(1)).updateReferral(any(ReferralCoordinationEntity.class));
    }

    @Test
    @DisplayName("GET /api/v1/referral-coordinations/metrics/summary - should return referral metrics")
    void testGetMetrics_Success() throws Exception {
        // Given
        ReferralCoordinationService.ReferralMetrics metrics =
            new ReferralCoordinationService.ReferralMetrics(10, 3, 70);

        when(referralCoordinationService.getMetrics(tenantId))
            .thenReturn(metrics);

        // When/Then
        mockMvc.perform(get("/api/v1/referral-coordinations/metrics/summary")
                .header("X-Tenant-ID", tenantId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalReferrals").value(10))
            .andExpect(jsonPath("$.pendingReferrals").value(3))
            .andExpect(jsonPath("$.completionRate").value(70));
    }

    @Test
    @DisplayName("POST without X-Tenant-ID header - should return 400")
    void testCreateReferral_MissingTenantHeader() throws Exception {
        // Given
        String requestBody = objectMapper.writeValueAsString(testReferral);

        // When/Then
        mockMvc.perform(post("/api/v1/referral-coordinations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
}
