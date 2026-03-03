package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.ai.AIAgentDecisionEventEntity;
import com.healthdata.audit.repository.ai.AIAgentDecisionEventRepository;
import com.healthdata.auditquery.dto.qa.QAMetricsResponse;
import com.healthdata.auditquery.dto.qa.QAReviewQueueResponse;
import com.healthdata.auditquery.dto.qa.QAReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("QAAuditService")
class QAAuditServiceTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String OTHER_TENANT_ID = "other-tenant";
    private static final String REVIEWER_USER_ID = "reviewer-001";

    @Mock
    private AIAgentDecisionEventRepository decisionRepository;

    @InjectMocks
    private QAAuditService qaAuditService;

    private UUID decisionId;
    private AIAgentDecisionEventEntity decisionEntity;

    @BeforeEach
    void setUp() {
        decisionId = UUID.randomUUID();
        decisionEntity = AIAgentDecisionEventEntity.builder()
            .eventId(decisionId)
            .tenantId(TENANT_ID)
            .timestamp(Instant.now())
            .confidenceScore(0.85)
            .reviewStatus("PENDING")
            .build();
    }

    @Nested
    @DisplayName("approveDecision")
    class ApproveDecision {

        @Test
        @DisplayName("should set review status to APPROVED with reviewer info")
        void shouldSetReviewStatusApproved() {
            // Given
            QAReviewRequest request = QAReviewRequest.builder()
                .reviewNotes("Validated against clinical guidelines")
                .build();
            when(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decisionEntity));

            // When
            qaAuditService.approveDecision(TENANT_ID, decisionId, request, REVIEWER_USER_ID);

            // Then
            ArgumentCaptor<AIAgentDecisionEventEntity> captor =
                ArgumentCaptor.forClass(AIAgentDecisionEventEntity.class);
            verify(decisionRepository).save(captor.capture());

            AIAgentDecisionEventEntity saved = captor.getValue();
            assertThat(saved.getReviewStatus()).isEqualTo("APPROVED");
            assertThat(saved.getReviewedBy()).isEqualTo(REVIEWER_USER_ID);
            assertThat(saved.getReviewedAt()).isNotNull();
            assertThat(saved.getReviewNotes()).isEqualTo("Validated against clinical guidelines");
        }

        @Test
        @DisplayName("COMPLIANCE: should throw when tenant does not match decision tenant")
        void shouldThrow_WhenTenantMismatch() {
            // Given
            QAReviewRequest request = QAReviewRequest.builder().build();
            when(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decisionEntity));

            // When / Then
            assertThatThrownBy(() ->
                qaAuditService.approveDecision(OTHER_TENANT_ID, decisionId, request, REVIEWER_USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
        }
    }

    @Nested
    @DisplayName("rejectDecision")
    class RejectDecision {

        @Test
        @DisplayName("should append rejection reason to review notes")
        void shouldAppendRejectionReason_ToReviewNotes() {
            // Given
            QAReviewRequest request = QAReviewRequest.builder()
                .reviewNotes("Initial notes")
                .rejectionReason("Confidence score below threshold")
                .build();
            when(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decisionEntity));

            // When
            qaAuditService.rejectDecision(TENANT_ID, decisionId, request, REVIEWER_USER_ID);

            // Then
            ArgumentCaptor<AIAgentDecisionEventEntity> captor =
                ArgumentCaptor.forClass(AIAgentDecisionEventEntity.class);
            verify(decisionRepository).save(captor.capture());

            AIAgentDecisionEventEntity saved = captor.getValue();
            assertThat(saved.getReviewStatus()).isEqualTo("REJECTED");
            assertThat(saved.getReviewNotes()).contains("Rejection Reason: Confidence score below threshold");
            assertThat(saved.getReviewNotes()).contains("Initial notes");
        }
    }

    @Nested
    @DisplayName("flagDecision")
    class FlagDecision {

        @Test
        @DisplayName("should append flag reason to review notes")
        void shouldAppendFlagReason_ToReviewNotes() {
            // Given
            QAReviewRequest request = QAReviewRequest.builder()
                .reviewNotes("Needs expert review")
                .flagReason("Requires clinical expert review")
                .build();
            when(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decisionEntity));

            // When
            qaAuditService.flagDecision(TENANT_ID, decisionId, request, REVIEWER_USER_ID);

            // Then
            ArgumentCaptor<AIAgentDecisionEventEntity> captor =
                ArgumentCaptor.forClass(AIAgentDecisionEventEntity.class);
            verify(decisionRepository).save(captor.capture());

            AIAgentDecisionEventEntity saved = captor.getValue();
            assertThat(saved.getReviewStatus()).isEqualTo("FLAGGED");
            assertThat(saved.getReviewNotes()).contains("Flag Reason: Requires clinical expert review");
            assertThat(saved.getReviewNotes()).contains("Needs expert review");
        }
    }

    @Nested
    @DisplayName("markFalsePositive")
    class MarkFalsePositive {

        @Test
        @DisplayName("should set feedback comment with FALSE_POSITIVE prefix")
        void shouldSetFeedbackComment_WithFalsePositivePrefix() {
            // Given
            QAReviewRequest request = QAReviewRequest.builder()
                .falsePositiveContext("Patient already had this condition resolved")
                .build();
            when(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decisionEntity));

            // When
            qaAuditService.markFalsePositive(TENANT_ID, decisionId, request, REVIEWER_USER_ID);

            // Then
            ArgumentCaptor<AIAgentDecisionEventEntity> captor =
                ArgumentCaptor.forClass(AIAgentDecisionEventEntity.class);
            verify(decisionRepository).save(captor.capture());

            AIAgentDecisionEventEntity saved = captor.getValue();
            assertThat(saved.getUserFeedbackComment())
                .isEqualTo("FALSE_POSITIVE: Patient already had this condition resolved");
            assertThat(saved.getReviewedBy()).isEqualTo(REVIEWER_USER_ID);
            assertThat(saved.getReviewedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getReviewDetail")
    class GetReviewDetail {

        @Test
        @DisplayName("COMPLIANCE: should return empty when tenant does not match")
        void shouldReturnEmpty_WhenTenantMismatch() {
            // Given
            when(decisionRepository.findById(decisionId)).thenReturn(Optional.of(decisionEntity));

            // When
            Optional<QAReviewQueueResponse> result =
                qaAuditService.getReviewDetail(OTHER_TENANT_ID, decisionId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getQAMetrics")
    class GetQAMetrics {

        @Test
        @DisplayName("should calculate approval rate correctly from reviewed decisions")
        void shouldCalculateApprovalRate_Correctly() {
            // Given: 3 approved, 1 rejected, 1 flagged = 5 reviewed, approval rate = 3/5 = 0.6
            Instant now = Instant.now();
            Instant start = now.minusSeconds(86400);

            List<AIAgentDecisionEventEntity> decisions = List.of(
                buildDecisionWithStatus("APPROVED", 0.9),
                buildDecisionWithStatus("APPROVED", 0.85),
                buildDecisionWithStatus("APPROVED", 0.75),
                buildDecisionWithStatus("REJECTED", 0.4),
                buildDecisionWithStatus("FLAGGED", 0.6),
                buildDecisionWithStatus("PENDING", 0.5)
            );

            Page<AIAgentDecisionEventEntity> page = new PageImpl<>(decisions);
            when(decisionRepository.findByTenantIdAndTimestampBetween(
                eq(TENANT_ID), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(page);

            // When
            QAMetricsResponse metrics = qaAuditService.getQAMetrics(TENANT_ID, start, now);

            // Then
            assertThat(metrics.getTotalReviewed()).isEqualTo(5L);
            assertThat(metrics.getApprovedDecisions()).isEqualTo(3L);
            assertThat(metrics.getRejectedDecisions()).isEqualTo(1L);
            assertThat(metrics.getFlaggedDecisions()).isEqualTo(1L);
            assertThat(metrics.getPendingReview()).isEqualTo(1L);
            assertThat(metrics.getApprovalRate()).isCloseTo(0.6, within(0.001));
        }

        private AIAgentDecisionEventEntity buildDecisionWithStatus(String status, double confidence) {
            return AIAgentDecisionEventEntity.builder()
                .eventId(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .timestamp(Instant.now())
                .reviewStatus("PENDING".equals(status) ? null : status)
                .confidenceScore(confidence)
                .build();
        }
    }
}
