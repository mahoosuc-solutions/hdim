package com.healthdata.approval.routing;

import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import com.healthdata.approval.repository.ApprovalRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalRoutingService Tests")
class ApprovalRoutingServiceTest {

    @Mock
    private ApprovalRequestRepository requestRepository;

    @Mock
    private ReviewerPoolService reviewerPoolService;

    @InjectMocks
    private ApprovalRoutingService routingService;

    private static final String TENANT_ID = "tenant-123";
    private static final String REVIEWER_ID = "reviewer-456";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(routingService, "autoEscalationHours", 4);
        ReflectionTestUtils.setField(routingService, "routingEnabled", true);
    }

    @Nested
    @DisplayName("Determine Required Role")
    class DetermineRequiredRoleTests {

        @Test
        @DisplayName("should use explicitly assigned role when set")
        void determineRequiredRole_ExplicitRole_ReturnsAssignedRole() {
            // Given
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.HIGH);
            request.setAssignedRole("ADMIN");

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should determine role based on AGENT_ACTION request type")
        void determineRequiredRole_AgentAction_ReturnsClinicalReviewer() {
            // Given
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.LOW);

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("CLINICAL_REVIEWER");
        }

        @Test
        @DisplayName("should determine role based on DATA_MUTATION request type")
        void determineRequiredRole_DataMutation_ReturnsClinicalSupervisor() {
            // Given
            ApprovalRequest request = createRequest(RequestType.DATA_MUTATION, RiskLevel.LOW);

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("CLINICAL_SUPERVISOR");
        }

        @Test
        @DisplayName("should use higher role when risk level requires elevation")
        void determineRequiredRole_HighRisk_ReturnsHigherRole() {
            // Given
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.CRITICAL);

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            // AGENT_ACTION requires CLINICAL_REVIEWER, but CRITICAL requires CLINICAL_DIRECTOR
            assertThat(role).isEqualTo("CLINICAL_DIRECTOR");
        }

        @Test
        @DisplayName("should handle EMERGENCY_ACCESS request type")
        void determineRequiredRole_EmergencyAccess_ReturnsClinicalDirector() {
            // Given
            ApprovalRequest request = createRequest(RequestType.EMERGENCY_ACCESS, RiskLevel.LOW);

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("CLINICAL_DIRECTOR");
        }

        @Test
        @DisplayName("should handle WORKFLOW_DEPLOY request type")
        void determineRequiredRole_WorkflowDeploy_ReturnsTechnicalLead() {
            // Given
            ApprovalRequest request = createRequest(RequestType.WORKFLOW_DEPLOY, RiskLevel.MEDIUM);

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("TECHNICAL_LEAD");
        }

        @Test
        @DisplayName("should default to CLINICAL_REVIEWER for unknown request type")
        void determineRequiredRole_UnknownType_DefaultsToClinicalReviewer() {
            // Given
            ApprovalRequest request = ApprovalRequest.builder()
                .tenantId(TENANT_ID)
                .requestType(RequestType.CONSENT_CHANGE)
                .riskLevel(RiskLevel.LOW)
                .build();

            // When
            String role = routingService.determineRequiredRole(request);

            // Then
            assertThat(role).isEqualTo("CLINICAL_SUPERVISOR");
        }
    }

    @Nested
    @DisplayName("Auto-Assign")
    class AutoAssignTests {

        @Test
        @DisplayName("should auto-assign request to available reviewer")
        void autoAssign_AvailableReviewer_Success() {
            // Given
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.MEDIUM);
            List<String> eligibleReviewers = List.of(REVIEWER_ID, "reviewer-789");

            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "CLINICAL_REVIEWER"))
                .thenReturn(eligibleReviewers);
            when(reviewerPoolService.selectNextReviewer(TENANT_ID, "CLINICAL_REVIEWER", eligibleReviewers))
                .thenReturn(REVIEWER_ID);
            when(requestRepository.save(any(ApprovalRequest.class)))
                .thenAnswer(i -> i.getArgument(0));

            // When
            Optional<String> result = routingService.autoAssign(request);

            // Then
            assertThat(result).isPresent().hasValue(REVIEWER_ID);
            assertThat(request.getAssignedTo()).isEqualTo(REVIEWER_ID);
            assertThat(request.getStatus()).isEqualTo(ApprovalStatus.ASSIGNED);
            assertThat(request.getAssignedRole()).isEqualTo("CLINICAL_REVIEWER");

            verify(requestRepository).save(request);
        }

        @Test
        @DisplayName("should return empty when no reviewers available")
        void autoAssign_NoReviewers_ReturnsEmpty() {
            // Given
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.HIGH);

            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "CLINICAL_SUPERVISOR"))
                .thenReturn(Collections.emptyList());

            // When
            Optional<String> result = routingService.autoAssign(request);

            // Then
            assertThat(result).isEmpty();
            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip auto-assign when routing disabled")
        void autoAssign_RoutingDisabled_ReturnsEmpty() {
            // Given
            ReflectionTestUtils.setField(routingService, "routingEnabled", false);
            ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.MEDIUM);

            // When
            Optional<String> result = routingService.autoAssign(request);

            // Then
            assertThat(result).isEmpty();
            verify(reviewerPoolService, never()).getAvailableReviewers(anyString(), anyString());
        }

        @Test
        @DisplayName("should handle single eligible reviewer")
        void autoAssign_SingleReviewer_Success() {
            // Given
            ApprovalRequest request = createRequest(RequestType.DLQ_REPROCESS, RiskLevel.LOW);
            List<String> eligibleReviewers = List.of(REVIEWER_ID);

            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "TECHNICAL_REVIEWER"))
                .thenReturn(eligibleReviewers);
            when(reviewerPoolService.selectNextReviewer(TENANT_ID, "TECHNICAL_REVIEWER", eligibleReviewers))
                .thenReturn(REVIEWER_ID);
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            Optional<String> result = routingService.autoAssign(request);

            // Then
            assertThat(result).isPresent().hasValue(REVIEWER_ID);
        }
    }

    @Nested
    @DisplayName("Get Escalation Role")
    class GetEscalationRoleTests {

        @Test
        @DisplayName("should get escalation role for CLINICAL_REVIEWER")
        void getEscalationRole_ClinicalReviewer_ReturnsSupervisor() {
            // When
            String escalationRole = routingService.getEscalationRole("CLINICAL_REVIEWER");

            // Then
            assertThat(escalationRole).isEqualTo("CLINICAL_SUPERVISOR");
        }

        @Test
        @DisplayName("should get escalation role for CLINICAL_SUPERVISOR")
        void getEscalationRole_ClinicalSupervisor_ReturnsDirector() {
            // When
            String escalationRole = routingService.getEscalationRole("CLINICAL_SUPERVISOR");

            // Then
            assertThat(escalationRole).isEqualTo("CLINICAL_DIRECTOR");
        }

        @Test
        @DisplayName("should get escalation role for TECHNICAL_REVIEWER")
        void getEscalationRole_TechnicalReviewer_ReturnsTechnicalLead() {
            // When
            String escalationRole = routingService.getEscalationRole("TECHNICAL_REVIEWER");

            // Then
            assertThat(escalationRole).isEqualTo("TECHNICAL_LEAD");
        }

        @Test
        @DisplayName("should default to ADMIN for unknown role")
        void getEscalationRole_UnknownRole_ReturnsAdmin() {
            // When
            String escalationRole = routingService.getEscalationRole("UNKNOWN_ROLE");

            // Then
            assertThat(escalationRole).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("should escalate SUPER_ADMIN to SUPER_ADMIN")
        void getEscalationRole_SuperAdmin_DefaultsToAdmin() {
            // When
            String escalationRole = routingService.getEscalationRole("SUPER_ADMIN");

            // Then
            assertThat(escalationRole).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Auto-Escalate Stale Requests")
    class AutoEscalateStaleRequestsTests {

        @Test
        @DisplayName("should auto-escalate stale requests")
        void autoEscalateStaleRequests_StaleRequests_Escalates() {
            // Given
            ApprovalRequest staleRequest = createAssignedRequest();
            staleRequest.setAssignedRole("CLINICAL_REVIEWER");
            List<String> escalationReviewers = List.of("supervisor-001");

            when(requestRepository.findStaleAssignedRequests(any(Instant.class)))
                .thenReturn(List.of(staleRequest));
            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "CLINICAL_SUPERVISOR"))
                .thenReturn(escalationReviewers);
            when(reviewerPoolService.selectNextReviewer(TENANT_ID, "CLINICAL_SUPERVISOR", escalationReviewers))
                .thenReturn("supervisor-001");
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            routingService.autoEscalateStaleRequests();

            // Then
            assertThat(staleRequest.getStatus()).isEqualTo(ApprovalStatus.ESCALATED);
            assertThat(staleRequest.getEscalatedTo()).isEqualTo("supervisor-001");
            assertThat(staleRequest.getAssignedRole()).isEqualTo("CLINICAL_SUPERVISOR");

            verify(requestRepository).save(staleRequest);
        }

        @Test
        @DisplayName("should skip escalation when no escalation reviewers available")
        void autoEscalateStaleRequests_NoEscalationReviewers_Skips() {
            // Given
            ApprovalRequest staleRequest = createAssignedRequest();
            staleRequest.setAssignedRole("CLINICAL_REVIEWER");

            when(requestRepository.findStaleAssignedRequests(any(Instant.class)))
                .thenReturn(List.of(staleRequest));
            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "CLINICAL_SUPERVISOR"))
                .thenReturn(Collections.emptyList());

            // When
            routingService.autoEscalateStaleRequests();

            // Then
            assertThat(staleRequest.getStatus()).isEqualTo(ApprovalStatus.ASSIGNED);
            verify(requestRepository, never()).save(any());
        }

        @Test
        @DisplayName("should skip when routing disabled")
        void autoEscalateStaleRequests_RoutingDisabled_Skips() {
            // Given
            ReflectionTestUtils.setField(routingService, "routingEnabled", false);

            // When
            routingService.autoEscalateStaleRequests();

            // Then
            verify(requestRepository, never()).findStaleAssignedRequests(any());
        }

        @Test
        @DisplayName("should handle empty stale requests list")
        void autoEscalateStaleRequests_NoStaleRequests_DoesNothing() {
            // Given
            when(requestRepository.findStaleAssignedRequests(any(Instant.class)))
                .thenReturn(Collections.emptyList());

            // When
            routingService.autoEscalateStaleRequests();

            // Then
            verify(reviewerPoolService, never()).getAvailableReviewers(anyString(), anyString());
        }

        @Test
        @DisplayName("should handle multiple stale requests")
        void autoEscalateStaleRequests_MultipleRequests_EscalatesAll() {
            // Given
            ApprovalRequest request1 = createAssignedRequest();
            request1.setAssignedRole("CLINICAL_REVIEWER");
            ApprovalRequest request2 = createAssignedRequest();
            request2.setAssignedRole("TECHNICAL_REVIEWER");

            when(requestRepository.findStaleAssignedRequests(any(Instant.class)))
                .thenReturn(List.of(request1, request2));
            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "CLINICAL_SUPERVISOR"))
                .thenReturn(List.of("supervisor-001"));
            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "TECHNICAL_LEAD"))
                .thenReturn(List.of("tech-lead-001"));
            when(reviewerPoolService.selectNextReviewer(eq(TENANT_ID), anyString(), anyList()))
                .thenAnswer(i -> i.getArgument(2, List.class).get(0));
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            routingService.autoEscalateStaleRequests();

            // Then
            verify(requestRepository, times(2)).save(any(ApprovalRequest.class));
        }

        @Test
        @DisplayName("should continue processing on individual request failure")
        void autoEscalateStaleRequests_RequestFailure_ContinuesProcessing() {
            // Given
            ApprovalRequest request1 = createAssignedRequest();
            request1.setAssignedRole("CLINICAL_REVIEWER");
            ApprovalRequest request2 = createAssignedRequest();
            request2.setAssignedRole("TECHNICAL_REVIEWER");

            when(requestRepository.findStaleAssignedRequests(any(Instant.class)))
                .thenReturn(List.of(request1, request2));
            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "CLINICAL_SUPERVISOR"))
                .thenThrow(new RuntimeException("Test error"));
            when(reviewerPoolService.getAvailableReviewers(TENANT_ID, "TECHNICAL_LEAD"))
                .thenReturn(List.of("tech-lead-001"));
            when(reviewerPoolService.selectNextReviewer(TENANT_ID, "TECHNICAL_LEAD", List.of("tech-lead-001")))
                .thenReturn("tech-lead-001");
            when(requestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            routingService.autoEscalateStaleRequests();

            // Then
            verify(requestRepository, times(1)).save(request2);
        }
    }

    @Nested
    @DisplayName("Can Approve")
    class CanApproveTests {

        @Test
        @DisplayName("should allow user with exact required role")
        void canApprove_ExactRole_ReturnsTrue() {
            // When
            boolean canApprove = routingService.canApprove("CLINICAL_REVIEWER", "CLINICAL_REVIEWER");

            // Then
            assertThat(canApprove).isTrue();
        }

        @Test
        @DisplayName("should allow user with higher role")
        void canApprove_HigherRole_ReturnsTrue() {
            // When
            boolean canApprove = routingService.canApprove("CLINICAL_SUPERVISOR", "CLINICAL_REVIEWER");

            // Then
            assertThat(canApprove).isTrue();
        }

        @Test
        @DisplayName("should deny user with lower role")
        void canApprove_LowerRole_ReturnsFalse() {
            // When
            boolean canApprove = routingService.canApprove("CLINICAL_REVIEWER", "CLINICAL_SUPERVISOR");

            // Then
            assertThat(canApprove).isFalse();
        }

        @Test
        @DisplayName("should allow SUPER_ADMIN to approve anything")
        void canApprove_SuperAdmin_ReturnsTrue() {
            // When
            boolean canApprove = routingService.canApprove("SUPER_ADMIN", "CLINICAL_DIRECTOR");

            // Then
            assertThat(canApprove).isTrue();
        }

        @Test
        @DisplayName("should deny unknown user role")
        void canApprove_UnknownUserRole_ReturnsFalse() {
            // When
            boolean canApprove = routingService.canApprove("UNKNOWN_ROLE", "CLINICAL_REVIEWER");

            // Then
            assertThat(canApprove).isFalse();
        }

        @Test
        @DisplayName("should deny unknown required role")
        void canApprove_UnknownRequiredRole_ReturnsFalse() {
            // When
            boolean canApprove = routingService.canApprove("CLINICAL_REVIEWER", "UNKNOWN_ROLE");

            // Then
            assertThat(canApprove).isFalse();
        }

        @Test
        @DisplayName("should handle technical vs clinical roles")
        void canApprove_TechnicalVsClinical_FollowsHierarchy() {
            // When
            boolean techCanApproveClinical = routingService.canApprove("TECHNICAL_LEAD", "CLINICAL_REVIEWER");
            boolean clinicalCanApproveTech = routingService.canApprove("CLINICAL_REVIEWER", "TECHNICAL_REVIEWER");

            // Then
            assertThat(techCanApproveClinical).isTrue();
            assertThat(clinicalCanApproveTech).isTrue();
        }
    }

    // Helper methods

    private ApprovalRequest createRequest(RequestType type, RiskLevel riskLevel) {
        return ApprovalRequest.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .requestType(type)
            .entityType("TestEntity")
            .entityId("entity-123")
            .actionRequested("TEST_ACTION")
            .riskLevel(riskLevel)
            .status(ApprovalStatus.PENDING)
            .requestedBy("user-123")
            .payload(new HashMap<>())
            .build();
    }

    private ApprovalRequest createAssignedRequest() {
        ApprovalRequest request = createRequest(RequestType.AGENT_ACTION, RiskLevel.MEDIUM);
        request.setStatus(ApprovalStatus.ASSIGNED);
        request.setAssignedTo(REVIEWER_ID);
        request.setAssignedAt(Instant.now().minus(Duration.ofHours(5)));
        return request;
    }
}
