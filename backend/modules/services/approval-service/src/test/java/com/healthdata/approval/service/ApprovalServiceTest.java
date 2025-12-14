package com.healthdata.approval.service;

import com.healthdata.approval.domain.entity.ApprovalHistory;
import com.healthdata.approval.domain.entity.ApprovalHistory.HistoryAction;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.repository.ApprovalHistoryRepository;
import com.healthdata.approval.repository.ApprovalRequestRepository;
import com.healthdata.approval.service.ApprovalService.CreateApprovalRequestDTO;
import com.healthdata.approval.websocket.ApprovalNotificationService;
import com.healthdata.approval.webhook.WebhookCallbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalService Tests")
class ApprovalServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private ApprovalHistoryRepository historyRepository;

    @Mock
    private ApprovalNotificationService notificationService;

    @Mock
    private WebhookCallbackService webhookCallbackService;

    @InjectMocks
    private ApprovalService approvalService;

    @Captor
    private ArgumentCaptor<ApprovalRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<ApprovalHistory> historyCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String REVIEWER_ID = "reviewer-789";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(approvalService, "defaultTimeoutHours", 24);
        ReflectionTestUtils.setField(approvalService, "autoEscalationHours", 4);
    }

    @Nested
    @DisplayName("Create Approval Request")
    class CreateApprovalRequestTests {

        @Test
        @DisplayName("should create approval request with all fields")
        void createApprovalRequest_WithAllFields_Success() {
            // Given
            CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
                TENANT_ID,
                RequestType.AGENT_ACTION,
                "MedicationTool",
                "patient-123",
                "EXECUTE",
                Map.of("action", "prescribe", "medication", "aspirin"),
                new BigDecimal("0.85"),
                RiskLevel.HIGH,
                USER_ID,
                "agent-runtime-service",
                "corr-123",
                "CLINICAL_REVIEWER",
                Instant.now().plus(Duration.ofHours(48))
            );

            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> {
                    ApprovalRequest req = invocation.getArgument(0);
                    ReflectionTestUtils.setField(req, "id", UUID.randomUUID());
                    return req;
                });

            when(historyRepository.save(any(ApprovalHistory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ApprovalRequest result = approvalService.createApprovalRequest(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getRequestType()).isEqualTo(RequestType.AGENT_ACTION);
            assertThat(result.getEntityType()).isEqualTo("MedicationTool");
            assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.PENDING);
            assertThat(result.getRequestedBy()).isEqualTo(USER_ID);

            verify(requestRepository).save(requestCaptor.capture());
            verify(historyRepository).save(historyCaptor.capture());

            ApprovalHistory history = historyCaptor.getValue();
            assertThat(history.getAction()).isEqualTo(HistoryAction.CREATED);
            assertThat(history.getActor()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should use default timeout when not provided")
        void createApprovalRequest_NoExpiration_UsesDefault() {
            // Given
            CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
                TENANT_ID,
                RequestType.GUARDRAIL_REVIEW,
                "AI_RESPONSE",
                null,
                "DELIVER_TO_USER",
                Map.of("content", "flagged content"),
                null,
                RiskLevel.MEDIUM,
                "SYSTEM",
                "agent-runtime-service",
                null,
                null,
                null // No expiration set
            );

            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> {
                    ApprovalRequest req = invocation.getArgument(0);
                    ReflectionTestUtils.setField(req, "id", UUID.randomUUID());
                    return req;
                });
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            Instant before = Instant.now();
            ApprovalRequest result = approvalService.createApprovalRequest(dto);
            Instant after = Instant.now();

            // Then
            assertThat(result.getExpiresAt())
                .isAfter(before.plus(Duration.ofHours(23)))
                .isBefore(after.plus(Duration.ofHours(25)));
        }

        @Test
        @DisplayName("should use empty map when payload is null")
        void createApprovalRequest_NullPayload_UsesEmptyMap() {
            // Given
            CreateApprovalRequestDTO dto = new CreateApprovalRequestDTO(
                TENANT_ID,
                RequestType.DATA_MUTATION,
                "Patient",
                "patient-456",
                "UPDATE",
                null, // Null payload
                null,
                RiskLevel.LOW,
                USER_ID,
                "fhir-service",
                null,
                null,
                null
            );

            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(invocation -> {
                    ApprovalRequest req = invocation.getArgument(0);
                    ReflectionTestUtils.setField(req, "id", UUID.randomUUID());
                    return req;
                });
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.createApprovalRequest(dto);

            // Then
            assertThat(result.getPayload()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Assign Request")
    class AssignRequestTests {

        @Test
        @DisplayName("should assign pending request to reviewer")
        void assignRequest_PendingRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.assignRequest(requestId, TENANT_ID, REVIEWER_ID, USER_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.ASSIGNED);
            assertThat(result.getAssignedTo()).isEqualTo(REVIEWER_ID);
            assertThat(result.getAssignedAt()).isNotNull();

            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.ASSIGNED);
        }

        @Test
        @DisplayName("should fail to assign non-pending request")
        void assignRequest_NonPendingRequest_Fails() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createApprovedRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() ->
                approvalService.assignRequest(requestId, TENANT_ID, REVIEWER_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only assign PENDING requests");
        }

        @Test
        @DisplayName("should throw when request not found")
        void assignRequest_NotFound_Throws() {
            // Given
            UUID requestId = UUID.randomUUID();
            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                approvalService.assignRequest(requestId, TENANT_ID, REVIEWER_ID, USER_ID))
                .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("Approve Request")
    class ApproveRequestTests {

        @Test
        @DisplayName("should approve pending request")
        void approve_PendingRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.approve(requestId, TENANT_ID, REVIEWER_ID, "Looks good");

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(result.getDecisionBy()).isEqualTo(REVIEWER_ID);
            assertThat(result.getDecisionReason()).isEqualTo("Looks good");
            assertThat(result.getDecisionAt()).isNotNull();

            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.APPROVED);
        }

        @Test
        @DisplayName("should approve assigned request")
        void approve_AssignedRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createAssignedRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.approve(requestId, TENANT_ID, REVIEWER_ID, null);

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        }

        @Test
        @DisplayName("should fail to approve already approved request")
        void approve_AlreadyApproved_Fails() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createApprovedRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() ->
                approvalService.approve(requestId, TENANT_ID, REVIEWER_ID, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve request");
        }

        @Test
        @DisplayName("should fail to approve expired request")
        void approve_ExpiredRequest_Fails() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createExpiredRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() ->
                approvalService.approve(requestId, TENANT_ID, REVIEWER_ID, "reason"))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Reject Request")
    class RejectRequestTests {

        @Test
        @DisplayName("should reject pending request")
        void reject_PendingRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.reject(requestId, TENANT_ID, REVIEWER_ID, "Not appropriate");

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.REJECTED);
            assertThat(result.getDecisionBy()).isEqualTo(REVIEWER_ID);
            assertThat(result.getDecisionReason()).isEqualTo("Not appropriate");

            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.REJECTED);
        }

        @Test
        @DisplayName("should fail to reject already rejected request")
        void reject_AlreadyRejected_Fails() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createRejectedRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When/Then
            assertThatThrownBy(() ->
                approvalService.reject(requestId, TENANT_ID, REVIEWER_ID, "reason"))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Escalate Request")
    class EscalateRequestTests {

        @Test
        @DisplayName("should escalate pending request")
        void escalate_PendingRequest_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(historyRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            ApprovalRequest result = approvalService.escalate(
                requestId, TENANT_ID, REVIEWER_ID, "supervisor-001", "Need senior review");

            // Then
            assertThat(result.getStatus()).isEqualTo(ApprovalStatus.ESCALATED);
            assertThat(result.getEscalatedTo()).isEqualTo("supervisor-001");
            assertThat(result.getEscalationCount()).isEqualTo(1);

            verify(historyRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getAction()).isEqualTo(HistoryAction.ESCALATED);
        }

        @Test
        @DisplayName("should fail to escalate already escalated request")
        void escalate_AlreadyEscalated_ThrowsException() {
            // Given
            UUID requestId = UUID.randomUUID();
            ApprovalRequest request = createPendingRequest(requestId);
            request.escalate("level1", "First escalation");  // Status is now ESCALATED

            when(requestRepository.findByTenantIdAndId(TENANT_ID, requestId))
                .thenReturn(Optional.of(request));

            // When & Then
            assertThatThrownBy(() -> approvalService.escalate(
                requestId, TENANT_ID, REVIEWER_ID, "level2", "Second escalation"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot escalate request in status: ESCALATED");
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethodTests {

        @Test
        @DisplayName("should get pending requests for user role")
        void getPendingForUser_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ApprovalRequest> requests = List.of(
                createPendingRequest(UUID.randomUUID()),
                createPendingRequest(UUID.randomUUID())
            );
            Page<ApprovalRequest> page = new PageImpl<>(requests, pageable, 2);

            when(requestRepository.findPendingByTenantAndRole(TENANT_ID, "CLINICAL_REVIEWER", pageable))
                .thenReturn(page);

            // When
            Page<ApprovalRequest> result = approvalService.getPendingForUser(TENANT_ID, "CLINICAL_REVIEWER", pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("should get requests assigned to user")
        void getAssignedToUser_Success() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<ApprovalRequest> requests = List.of(createAssignedRequest(UUID.randomUUID()));
            Page<ApprovalRequest> page = new PageImpl<>(requests, pageable, 1);

            when(requestRepository.findByAssignedToAndStatus(REVIEWER_ID, ApprovalStatus.ASSIGNED, pageable))
                .thenReturn(page);

            // When
            Page<ApprovalRequest> result = approvalService.getAssignedToUser(
                REVIEWER_ID, ApprovalStatus.ASSIGNED, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should get history for request")
        void getHistory_Success() {
            // Given
            UUID requestId = UUID.randomUUID();
            List<ApprovalHistory> history = List.of(
                createHistory(requestId, HistoryAction.CREATED),
                createHistory(requestId, HistoryAction.ASSIGNED),
                createHistory(requestId, HistoryAction.APPROVED)
            );

            when(historyRepository.findByApprovalRequestIdOrderByCreatedAtAsc(requestId))
                .thenReturn(history);

            // When
            List<ApprovalHistory> result = approvalService.getHistory(requestId);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getAction()).isEqualTo(HistoryAction.CREATED);
            assertThat(result.get(2).getAction()).isEqualTo(HistoryAction.APPROVED);
        }
    }

    @Nested
    @DisplayName("Statistics")
    class StatisticsTests {

        @Test
        @DisplayName("should calculate approval stats")
        void getStats_Success() {
            // Given
            Instant since = Instant.now().minus(Duration.ofDays(30));

            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.PENDING)).thenReturn(5L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.ASSIGNED)).thenReturn(3L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.APPROVED)).thenReturn(42L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.REJECTED)).thenReturn(8L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.EXPIRED)).thenReturn(2L);
            when(requestRepository.countByTenantIdAndStatus(TENANT_ID, ApprovalStatus.ESCALATED)).thenReturn(1L);
            when(requestRepository.averageDecisionTimeSeconds(TENANT_ID, since)).thenReturn(3600.0);
            when(requestRepository.countPendingByRiskLevel(TENANT_ID)).thenReturn(List.of(
                new Object[]{RiskLevel.HIGH, 2L},
                new Object[]{RiskLevel.MEDIUM, 3L}
            ));

            // When
            ApprovalService.ApprovalStats stats = approvalService.getStats(TENANT_ID, since);

            // Then
            assertThat(stats.pending()).isEqualTo(5);
            assertThat(stats.assigned()).isEqualTo(3);
            assertThat(stats.approved()).isEqualTo(42);
            assertThat(stats.rejected()).isEqualTo(8);
            assertThat(stats.expired()).isEqualTo(2);
            assertThat(stats.escalated()).isEqualTo(1);
            assertThat(stats.avgDecisionTimeSeconds()).isEqualTo(3600.0);
            assertThat(stats.pendingByRiskLevel()).containsEntry(RiskLevel.HIGH, 2L);
            assertThat(stats.pendingByRiskLevel()).containsEntry(RiskLevel.MEDIUM, 3L);
        }

        @Test
        @DisplayName("should handle null average decision time")
        void getStats_NullAvgTime_ReturnsZero() {
            // Given
            Instant since = Instant.now().minus(Duration.ofDays(30));

            when(requestRepository.countByTenantIdAndStatus(anyString(), any())).thenReturn(0L);
            when(requestRepository.averageDecisionTimeSeconds(TENANT_ID, since)).thenReturn(null);
            when(requestRepository.countPendingByRiskLevel(TENANT_ID)).thenReturn(Collections.emptyList());

            // When
            ApprovalService.ApprovalStats stats = approvalService.getStats(TENANT_ID, since);

            // Then
            assertThat(stats.avgDecisionTimeSeconds()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Expiration")
    class ExpirationTests {

        @Test
        @DisplayName("should expire old requests")
        void expireOldRequests_Success() {
            // Given
            when(requestRepository.expireRequests(any(Instant.class))).thenReturn(5);

            // When
            approvalService.expireOldRequests();

            // Then
            verify(requestRepository).expireRequests(any(Instant.class));
        }

        @Test
        @DisplayName("should find requests expiring soon")
        void findExpiringSoon_Success() {
            // Given
            List<ApprovalRequest> expiringRequests = List.of(
                createPendingRequest(UUID.randomUUID()),
                createPendingRequest(UUID.randomUUID())
            );

            when(requestRepository.findExpiringSoon(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(expiringRequests);

            // When
            List<ApprovalRequest> result = approvalService.findExpiringSoon(TENANT_ID, Duration.ofHours(4));

            // Then
            assertThat(result).hasSize(2);
        }
    }

    // Helper methods to create test fixtures

    private ApprovalRequest createPendingRequest(UUID id) {
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
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private ApprovalRequest createAssignedRequest(UUID id) {
        ApprovalRequest request = createPendingRequest(id);
        request.assign(REVIEWER_ID);
        return request;
    }

    private ApprovalRequest createApprovedRequest(UUID id) {
        ApprovalRequest request = createPendingRequest(id);
        request.approve(REVIEWER_ID, "Approved");
        return request;
    }

    private ApprovalRequest createRejectedRequest(UUID id) {
        ApprovalRequest request = createPendingRequest(id);
        request.reject(REVIEWER_ID, "Rejected");
        return request;
    }

    private ApprovalRequest createExpiredRequest(UUID id) {
        ApprovalRequest request = createPendingRequest(id);
        ReflectionTestUtils.setField(request, "status", ApprovalStatus.EXPIRED);
        return request;
    }

    private ApprovalHistory createHistory(UUID requestId, HistoryAction action) {
        ApprovalRequest request = createPendingRequest(requestId);
        return ApprovalHistory.builder()
            .approvalRequest(request)
            .action(action)
            .actor(USER_ID)
            .details(new HashMap<>())
            .build();
    }
}
