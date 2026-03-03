package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.clinical.ClinicalDecisionEntity;
import com.healthdata.audit.repository.clinical.ClinicalDecisionRepository;
import com.healthdata.auditquery.dto.clinical.ClinicalReviewRequest;
import com.healthdata.auditquery.dto.clinical.ClinicalDecisionResponse;
import com.healthdata.auditquery.dto.clinical.ClinicalMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicalAuditService Unit Tests")
class ClinicalAuditServiceTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String REVIEWER_USERNAME = "dr.reviewer";

    @Mock
    private ClinicalDecisionRepository clinicalDecisionRepository;

    @InjectMocks
    private ClinicalAuditService clinicalAuditService;

    private UUID decisionId;
    private ClinicalDecisionEntity decisionEntity;

    @BeforeEach
    void setUp() {
        decisionId = UUID.randomUUID();
        decisionEntity = ClinicalDecisionEntity.builder()
            .id(decisionId)
            .tenantId(TENANT_ID)
            .patientId("patient-001")
            .decisionType("MEDICATION_ALERT")
            .alertSeverity("HIGH")
            .decisionTimestamp(LocalDateTime.now())
            .reviewStatus("PENDING")
            .evidenceGrade("A")
            .confidenceScore(0.92)
            .specialtyArea("Cardiology")
            .build();
    }

    @Nested
    @DisplayName("Accept Recommendation")
    class AcceptRecommendation {

        @Test
        @DisplayName("Should set status to APPROVED and record reviewer")
        void shouldSetStatusApproved_AndRecordReviewer() {
            // Given
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                .clinicalNotes("Recommendation validated against current guidelines")
                .build();

            when(clinicalDecisionRepository.findById(decisionId))
                .thenReturn(Optional.of(decisionEntity));
            when(clinicalDecisionRepository.save(any(ClinicalDecisionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ClinicalDecisionResponse response = clinicalAuditService.acceptRecommendation(
                TENANT_ID, decisionId, request, REVIEWER_USERNAME
            );

            // Then
            assertThat(response.getReviewStatus()).isEqualTo("APPROVED");
            assertThat(response.getReviewedBy()).isEqualTo(REVIEWER_USERNAME);
            assertThat(response.getReviewNotes()).isEqualTo("Recommendation validated against current guidelines");
            verify(clinicalDecisionRepository).save(any(ClinicalDecisionEntity.class));
        }

        @Test
        @DisplayName("Should throw when tenant ID does not match decision tenant (COMPLIANCE)")
        void shouldThrow_WhenTenantMismatch() {
            // Given
            String wrongTenant = "wrong-tenant";
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                .clinicalNotes("notes")
                .build();

            when(clinicalDecisionRepository.findById(decisionId))
                .thenReturn(Optional.of(decisionEntity));

            // When/Then
            assertThatThrownBy(() -> clinicalAuditService.acceptRecommendation(
                wrongTenant, decisionId, request, REVIEWER_USERNAME
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Decision not found in tenant");
        }

        @Test
        @DisplayName("Should throw when decision not found")
        void shouldThrow_WhenDecisionNotFound() {
            // Given
            UUID missingId = UUID.randomUUID();
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                .clinicalNotes("notes")
                .build();

            when(clinicalDecisionRepository.findById(missingId))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> clinicalAuditService.acceptRecommendation(
                TENANT_ID, missingId, request, REVIEWER_USERNAME
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clinical decision not found");
        }
    }

    @Nested
    @DisplayName("Reject Recommendation")
    class RejectRecommendation {

        @Test
        @DisplayName("Should set status to REJECTED with rationale")
        void shouldSetStatusRejected_WithRationale() {
            // Given
            String rationale = "Patient contraindication present";
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                .clinicalRationale(rationale)
                .build();

            when(clinicalDecisionRepository.findById(decisionId))
                .thenReturn(Optional.of(decisionEntity));
            when(clinicalDecisionRepository.save(any(ClinicalDecisionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ClinicalDecisionResponse response = clinicalAuditService.rejectRecommendation(
                TENANT_ID, decisionId, request, REVIEWER_USERNAME
            );

            // Then
            assertThat(response.getReviewStatus()).isEqualTo("REJECTED");
            assertThat(response.getReviewedBy()).isEqualTo(REVIEWER_USERNAME);
            assertThat(response.getReviewNotes()).isEqualTo(rationale);
            verify(clinicalDecisionRepository).save(any(ClinicalDecisionEntity.class));
        }
    }

    @Nested
    @DisplayName("Modify Recommendation")
    class ModifyRecommendation {

        @Test
        @DisplayName("Should set override when alternative action is provided")
        void shouldSetOverride_WhenAlternativeActionProvided() {
            // Given
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                .modifications("Adjusted dosage based on renal function")
                .alternativeAction("Prescribed alternative medication")
                .clinicalReasoning("Evidence-based alternative with better safety profile")
                .build();

            when(clinicalDecisionRepository.findById(decisionId))
                .thenReturn(Optional.of(decisionEntity));
            when(clinicalDecisionRepository.save(any(ClinicalDecisionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ClinicalDecisionResponse response = clinicalAuditService.modifyRecommendation(
                TENANT_ID, decisionId, request, REVIEWER_USERNAME
            );

            // Then
            assertThat(response.getReviewStatus()).isEqualTo("NEEDS_REVISION");
            assertThat(response.getHasOverride()).isTrue();
            assertThat(response.getOverrideReason()).isEqualTo("Evidence-based alternative with better safety profile");
            assertThat(response.getOverrideAppliedBy()).isEqualTo(REVIEWER_USERNAME);
            assertThat(response.getOverrideAppliedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not set override when no alternative action is provided")
        void shouldNotSetOverride_WhenNoAlternativeAction() {
            // Given
            ClinicalReviewRequest request = ClinicalReviewRequest.builder()
                .modifications("Minor text adjustment")
                .build();

            when(clinicalDecisionRepository.findById(decisionId))
                .thenReturn(Optional.of(decisionEntity));
            when(clinicalDecisionRepository.save(any(ClinicalDecisionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ClinicalDecisionResponse response = clinicalAuditService.modifyRecommendation(
                TENANT_ID, decisionId, request, REVIEWER_USERNAME
            );

            // Then
            assertThat(response.getReviewStatus()).isEqualTo("NEEDS_REVISION");
            assertThat(response.getHasOverride()).isNull();
            assertThat(response.getOverrideReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Clinical Metrics")
    class ClinicalMetrics {

        @Test
        @DisplayName("Should calculate acceptance rate correctly")
        void shouldCalculateAcceptanceRate_Correctly() {
            // Given - 10 total decisions: 6 approved, 2 rejected, 1 needs_revision, 1 pending
            List<ClinicalDecisionEntity> decisions = List.of(
                buildDecision("APPROVED", "HIGH", "A", 0.95),
                buildDecision("APPROVED", "MODERATE", "B", 0.88),
                buildDecision("APPROVED", "LOW", "A", 0.91),
                buildDecision("APPROVED", "CRITICAL", "A", 0.97),
                buildDecision("APPROVED", "HIGH", "B", 0.85),
                buildDecision("APPROVED", "MODERATE", "C", 0.72),
                buildDecision("REJECTED", "HIGH", "D", 0.45),
                buildDecision("REJECTED", "MODERATE", "C", 0.55),
                buildDecision("NEEDS_REVISION", "LOW", "B", 0.78),
                buildDecision("PENDING", "HIGH", "A", 0.90)
            );

            Page<ClinicalDecisionEntity> page = new PageImpl<>(decisions);

            when(clinicalDecisionRepository.findByTenantIdAndDateRange(
                eq(TENANT_ID), any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)
            )).thenReturn(page);

            // When
            ClinicalMetricsResponse metrics = clinicalAuditService.getClinicalMetrics(
                TENANT_ID, null, null
            );

            // Then
            assertThat(metrics.getTotalDecisions()).isEqualTo(10);
            assertThat(metrics.getAcceptedRecommendations()).isEqualTo(6);
            assertThat(metrics.getRejectedRecommendations()).isEqualTo(2);
            assertThat(metrics.getModifiedRecommendations()).isEqualTo(1);
            assertThat(metrics.getPendingReview()).isEqualTo(1);
            assertThat(metrics.getAverageAcceptanceRate()).isEqualTo(0.6);
            assertThat(metrics.getCriticalSeverityCount()).isEqualTo(1);
            assertThat(metrics.getHighSeverityCount()).isEqualTo(4);
        }
    }

    private ClinicalDecisionEntity buildDecision(String status, String severity, String grade, Double confidence) {
        return ClinicalDecisionEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId("patient-" + UUID.randomUUID().toString().substring(0, 8))
            .decisionType("MEDICATION_ALERT")
            .alertSeverity(severity)
            .decisionTimestamp(LocalDateTime.now())
            .reviewStatus(status)
            .evidenceGrade(grade)
            .confidenceScore(confidence)
            .build();
    }
}
