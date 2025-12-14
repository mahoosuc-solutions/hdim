package com.healthdata.approval.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.approval.domain.entity.ApprovalHistory;
import com.healthdata.approval.domain.entity.ApprovalHistory.HistoryAction;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.service.ApprovalService;
import com.healthdata.approval.service.ApprovalService.ApprovalStats;
import com.healthdata.approval.service.ApprovalService.CreateApprovalRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ApprovalController.class, ApprovalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ApprovalController Tests")
class ApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalService approvalService;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String REVIEWER_ID = "reviewer-789";

    @Nested
    @DisplayName("POST /api/v1/approvals")
    class CreateApprovalTests {

        @Test
        @DisplayName("should create approval request successfully")
        void createRequest_Success() throws Exception {
            // Given
            ApprovalController.CreateRequestDTO requestDto = new ApprovalController.CreateRequestDTO(
                RequestType.AGENT_ACTION,
                "MedicationTool",
                "patient-123",
                "EXECUTE",
                Map.of("action", "prescribe"),
                new BigDecimal("0.85"),
                RiskLevel.HIGH,
                "agent-runtime-service",
                "corr-123",
                "CLINICAL_REVIEWER",
                null
            );

            ApprovalRequest created = createSampleRequest(UUID.randomUUID());

            when(approvalService.createApprovalRequest(any(CreateApprovalRequestDTO.class)))
                .thenReturn(created);

            // When/Then
            mockMvc.perform(post("/api/v1/approvals")
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.requestType").value("AGENT_ACTION"))
                .andExpect(jsonPath("$.status").value("PENDING"));

            verify(approvalService).createApprovalRequest(any(CreateApprovalRequestDTO.class));
        }

        @Test
        @DisplayName("should fail without tenant header")
        void createRequest_MissingTenantHeader_Fails() throws Exception {
            // Given
            ApprovalController.CreateRequestDTO requestDto = new ApprovalController.CreateRequestDTO(
                RequestType.AGENT_ACTION,
                "TestTool",
                null,
                "EXECUTE",
                null,
                null,
                RiskLevel.LOW,
                null,
                null,
                null,
                null
            );

            // When/Then
            mockMvc.perform(post("/api/v1/approvals")
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/{id}")
    class GetApprovalTests {

        @Test
        @DisplayName("should get approval request by ID")
        void getRequest_Success() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createSampleRequest(requestId);

            when(approvalService.getRequest(requestId, TENANT_ID)).thenReturn(request);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/{id}", requestId)
                    .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId.toString()))
                .andExpect(jsonPath("$.entityType").value("TestTool"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void getRequest_NotFound() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            when(approvalService.getRequest(requestId, TENANT_ID))
                .thenThrow(new NoSuchElementException("Not found"));

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/{id}", requestId)
                    .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals")
    class GetAllApprovalsTests {

        @Test
        @DisplayName("should get all requests with pagination")
        void getAllRequests_Success() throws Exception {
            // Given
            List<ApprovalRequest> requests = List.of(
                createSampleRequest(UUID.randomUUID()),
                createSampleRequest(UUID.randomUUID())
            );
            var page = new PageImpl<>(requests, PageRequest.of(0, 20), 2);

            when(approvalService.getAllForTenant(eq(TENANT_ID), isNull(), any(Pageable.class)))
                .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals")
                    .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("should filter by status")
        void getAllRequests_FilterByStatus() throws Exception {
            // Given
            List<ApprovalRequest> requests = List.of(createSampleRequest(UUID.randomUUID()));
            var page = new PageImpl<>(requests, PageRequest.of(0, 20), 1);

            when(approvalService.getAllForTenant(eq(TENANT_ID), eq(ApprovalStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals")
                    .header("X-Tenant-Id", TENANT_ID)
                    .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/pending")
    class GetPendingApprovalsTests {

        @Test
        @DisplayName("should get pending requests for role")
        void getPendingRequests_Success() throws Exception {
            // Given
            List<ApprovalRequest> requests = List.of(createSampleRequest(UUID.randomUUID()));
            var page = new PageImpl<>(requests, PageRequest.of(0, 20), 1);

            when(approvalService.getPendingForUser(eq(TENANT_ID), eq("CLINICAL_REVIEWER"), any(Pageable.class)))
                .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/pending")
                    .header("X-Tenant-Id", TENANT_ID)
                    .param("role", "CLINICAL_REVIEWER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/assigned")
    class GetAssignedApprovalsTests {

        @Test
        @DisplayName("should get assigned requests for user")
        void getAssignedRequests_Success() throws Exception {
            // Given
            List<ApprovalRequest> requests = List.of(createSampleRequest(UUID.randomUUID()));
            var page = new PageImpl<>(requests, PageRequest.of(0, 20), 1);

            when(approvalService.getAssignedToUser(eq(USER_ID), isNull(), any(Pageable.class)))
                .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/assigned")
                    .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/assign")
    class AssignApprovalTests {

        @Test
        @DisplayName("should assign request to reviewer")
        void assignRequest_Success() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest assigned = createSampleRequest(requestId);
            assigned.assign(REVIEWER_ID);

            when(approvalService.assignRequest(requestId, TENANT_ID, REVIEWER_ID, USER_ID))
                .thenReturn(assigned);

            ApprovalController.AssignRequestDTO body = new ApprovalController.AssignRequestDTO(REVIEWER_ID);

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/assign", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedTo").value(REVIEWER_ID));
        }

        @Test
        @DisplayName("should fail with empty assignedTo")
        void assignRequest_EmptyAssignee_Fails() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalController.AssignRequestDTO body = new ApprovalController.AssignRequestDTO("");

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/assign", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/approve")
    class ApproveRequestTests {

        @Test
        @DisplayName("should approve request")
        void approveRequest_Success() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest approved = createSampleRequest(requestId);
            approved.approve(USER_ID, "Looks good");

            when(approvalService.approve(requestId, TENANT_ID, USER_ID, "Looks good"))
                .thenReturn(approved);

            ApprovalController.DecisionDTO body = new ApprovalController.DecisionDTO("Looks good");

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/approve", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionBy").value(USER_ID));
        }

        @Test
        @DisplayName("should fail when approval fails")
        void approveRequest_IllegalState_Fails() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            when(approvalService.approve(eq(requestId), eq(TENANT_ID), eq(USER_ID), any()))
                .thenThrow(new IllegalStateException("Cannot approve"));

            ApprovalController.DecisionDTO body = new ApprovalController.DecisionDTO("reason");

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/approve", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/reject")
    class RejectRequestTests {

        @Test
        @DisplayName("should reject request")
        void rejectRequest_Success() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest rejected = createSampleRequest(requestId);
            rejected.reject(USER_ID, "Not appropriate");

            when(approvalService.reject(requestId, TENANT_ID, USER_ID, "Not appropriate"))
                .thenReturn(rejected);

            ApprovalController.DecisionDTO body = new ApprovalController.DecisionDTO("Not appropriate");

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/reject", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decisionReason").value("Not appropriate"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/approvals/{id}/escalate")
    class EscalateRequestTests {

        @Test
        @DisplayName("should escalate request")
        void escalateRequest_Success() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest escalated = createSampleRequest(requestId);
            escalated.escalate("supervisor-001", "Need senior review");

            when(approvalService.escalate(requestId, TENANT_ID, USER_ID, "supervisor-001", "Need senior review"))
                .thenReturn(escalated);

            ApprovalController.EscalateDTO body = new ApprovalController.EscalateDTO(
                "supervisor-001", "Need senior review");

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/escalate", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.escalatedTo").value("supervisor-001"));
        }

        @Test
        @DisplayName("should fail without escalatedTo")
        void escalateRequest_MissingEscalateTo_Fails() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalController.EscalateDTO body = new ApprovalController.EscalateDTO("", "reason");

            // When/Then
            mockMvc.perform(post("/api/v1/approvals/{id}/escalate", requestId)
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-User-Id", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/{id}/history")
    class GetHistoryTests {

        @Test
        @DisplayName("should get approval history")
        void getHistory_Success() throws Exception {
            // Given
            UUID requestId = UUID.randomUUID();
            List<ApprovalHistory> history = List.of(
                createHistory(requestId, HistoryAction.CREATED),
                createHistory(requestId, HistoryAction.APPROVED)
            );

            when(approvalService.getHistory(requestId)).thenReturn(history);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/{id}/history", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].action").value("CREATED"))
                .andExpect(jsonPath("$[1].action").value("APPROVED"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/stats")
    class GetStatsTests {

        @Test
        @DisplayName("should get approval statistics")
        void getStats_Success() throws Exception {
            // Given
            ApprovalStats stats = new ApprovalStats(
                5, 3, 42, 8, 2, 1, 3600.0,
                Map.of(RiskLevel.HIGH, 2L, RiskLevel.MEDIUM, 3L)
            );

            when(approvalService.getStats(eq(TENANT_ID), any(Instant.class))).thenReturn(stats);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/stats")
                    .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").value(5))
                .andExpect(jsonPath("$.approved").value(42))
                .andExpect(jsonPath("$.avgDecisionTimeSeconds").value(3600.0));
        }

        @Test
        @DisplayName("should accept custom days parameter")
        void getStats_CustomDays() throws Exception {
            // Given
            ApprovalStats stats = new ApprovalStats(0, 0, 0, 0, 0, 0, 0.0, Map.of());
            when(approvalService.getStats(eq(TENANT_ID), any(Instant.class))).thenReturn(stats);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/stats")
                    .header("X-Tenant-Id", TENANT_ID)
                    .param("days", "90"))
                .andExpect(status().isOk());

            verify(approvalService).getStats(eq(TENANT_ID), any(Instant.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/approvals/expiring")
    class GetExpiringTests {

        @Test
        @DisplayName("should get expiring requests")
        void getExpiringSoon_Success() throws Exception {
            // Given
            List<ApprovalRequest> expiring = List.of(
                createSampleRequest(UUID.randomUUID()),
                createSampleRequest(UUID.randomUUID())
            );

            when(approvalService.findExpiringSoon(eq(TENANT_ID), any(Duration.class)))
                .thenReturn(expiring);

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/expiring")
                    .header("X-Tenant-Id", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("should accept custom hours parameter")
        void getExpiringSoon_CustomHours() throws Exception {
            // Given
            when(approvalService.findExpiringSoon(eq(TENANT_ID), eq(Duration.ofHours(8))))
                .thenReturn(Collections.emptyList());

            // When/Then
            mockMvc.perform(get("/api/v1/approvals/expiring")
                    .header("X-Tenant-Id", TENANT_ID)
                    .param("hours", "8"))
                .andExpect(status().isOk());

            verify(approvalService).findExpiringSoon(TENANT_ID, Duration.ofHours(8));
        }
    }

    // Helper methods

    private ApprovalRequest createSampleRequest(UUID id) {
        ApprovalRequest request = ApprovalRequest.builder()
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestTool")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .payload(new HashMap<>())
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.PENDING)
            .requestedBy(USER_ID)
            .expiresAt(Instant.now().plus(Duration.ofHours(24)))
            .build();
        org.springframework.test.util.ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private ApprovalHistory createHistory(UUID requestId, HistoryAction action) {
        ApprovalRequest request = createSampleRequest(requestId);
        return ApprovalHistory.builder()
            .approvalRequest(request)
            .action(action)
            .actor(USER_ID)
            .details(new HashMap<>())
            .build();
    }
}
