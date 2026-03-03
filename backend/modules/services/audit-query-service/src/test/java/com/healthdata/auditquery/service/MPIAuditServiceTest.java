package com.healthdata.auditquery.service;

import com.healthdata.audit.entity.MPIMergeEntity;
import com.healthdata.audit.repository.MPIMergeRepository;
import com.healthdata.auditquery.dto.mpi.MPIMergeEventResponse;
import com.healthdata.auditquery.dto.mpi.MPIMetricsResponse;
import com.healthdata.auditquery.dto.mpi.MPIReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@DisplayName("MPIAuditService Unit Tests")
class MPIAuditServiceTest {

    private static final String TENANT_ID = "test-tenant-audit";
    private static final String VALIDATOR_USERNAME = "mpi.validator";

    @Mock
    private MPIMergeRepository mpiMergeRepository;

    @InjectMocks
    private MPIAuditService mpiAuditService;

    private UUID mergeId;
    private MPIMergeEntity mergeEntity;

    @BeforeEach
    void setUp() {
        mergeId = UUID.randomUUID();
        mergeEntity = MPIMergeEntity.builder()
            .id(mergeId)
            .tenantId(TENANT_ID)
            .sourcePatientId("source-patient-001")
            .targetPatientId("target-patient-001")
            .mergeType("AUTOMATIC")
            .confidenceScore(0.95)
            .mergeStatus("PENDING")
            .validationStatus("NOT_VALIDATED")
            .mergeTimestamp(LocalDateTime.now())
            .performedBy("system")
            .build();
    }

    @Nested
    @DisplayName("Validate Merge")
    class ValidateMerge {

        @Test
        @DisplayName("Should set status to VALIDATED and record validator")
        void shouldSetStatusValidated_AndRecordValidator() {
            // Given
            MPIReviewRequest request = MPIReviewRequest.builder()
                .validationNotes("Verified demographic match across all fields")
                .dataQualityAssessment("HIGH")
                .build();

            when(mpiMergeRepository.findById(mergeId))
                .thenReturn(Optional.of(mergeEntity));
            when(mpiMergeRepository.save(any(MPIMergeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MPIMergeEventResponse response = mpiAuditService.validateMerge(
                TENANT_ID, mergeId, request, VALIDATOR_USERNAME
            );

            // Then
            assertThat(response.getMergeStatus()).isEqualTo("VALIDATED");
            assertThat(response.getValidationStatus()).isEqualTo("VALIDATED");
            assertThat(response.getValidatedBy()).isEqualTo(VALIDATOR_USERNAME);
            assertThat(response.getValidatedAt()).isNotNull();
            assertThat(response.getValidationNotes()).isEqualTo("Verified demographic match across all fields");
            assertThat(response.getDataQualityAssessment()).isEqualTo("HIGH");
            verify(mpiMergeRepository).save(any(MPIMergeEntity.class));
        }

        @Test
        @DisplayName("Should throw when tenant ID does not match merge tenant (COMPLIANCE)")
        void shouldThrow_WhenTenantMismatch() {
            // Given
            String wrongTenant = "wrong-tenant";
            MPIReviewRequest request = MPIReviewRequest.builder()
                .validationNotes("notes")
                .build();

            when(mpiMergeRepository.findById(mergeId))
                .thenReturn(Optional.of(mergeEntity));

            // When/Then
            assertThatThrownBy(() -> mpiAuditService.validateMerge(
                wrongTenant, mergeId, request, VALIDATOR_USERNAME
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merge not found in tenant");
        }
    }

    @Nested
    @DisplayName("Rollback Merge")
    class RollbackMerge {

        @Test
        @DisplayName("Should set status to ROLLED_BACK with reason")
        void shouldSetStatusRolledBack_WithReason() {
            // Given
            String rollbackReason = "Incorrect patient match detected";
            MPIReviewRequest request = MPIReviewRequest.builder()
                .rollbackReason(rollbackReason)
                .build();

            when(mpiMergeRepository.findById(mergeId))
                .thenReturn(Optional.of(mergeEntity));
            when(mpiMergeRepository.save(any(MPIMergeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MPIMergeEventResponse response = mpiAuditService.rollbackMerge(
                TENANT_ID, mergeId, request, VALIDATOR_USERNAME
            );

            // Then
            assertThat(response.getMergeStatus()).isEqualTo("ROLLED_BACK");
            assertThat(response.getRollbackReason()).isEqualTo(rollbackReason);
            assertThat(response.getRolledBackBy()).isEqualTo(VALIDATOR_USERNAME);
            assertThat(response.getRolledBackAt()).isNotNull();
            verify(mpiMergeRepository).save(any(MPIMergeEntity.class));
        }
    }

    @Nested
    @DisplayName("Resolve Data Quality Issue")
    class ResolveDataQuality {

        @Test
        @DisplayName("Should clear data quality flag and set assessment")
        void shouldClearDataQualityFlag_AndSetAssessment() {
            // Given
            mergeEntity.setHasDataQualityIssues(true);
            mergeEntity.setDataQualityAssessment("LOW");

            MPIReviewRequest request = MPIReviewRequest.builder()
                .dataQualityAssessment("HIGH")
                .resolutionNotes("Corrected SSN mismatch via manual verification")
                .build();

            when(mpiMergeRepository.findById(mergeId))
                .thenReturn(Optional.of(mergeEntity));
            when(mpiMergeRepository.save(any(MPIMergeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            MPIMergeEventResponse response = mpiAuditService.resolveDataQualityIssue(
                TENANT_ID, mergeId, request, VALIDATOR_USERNAME
            );

            // Then
            assertThat(response.getHasDataQualityIssues()).isFalse();
            assertThat(response.getDataQualityAssessment()).isEqualTo("HIGH");
            assertThat(response.getValidatedBy()).isEqualTo(VALIDATOR_USERNAME);
            assertThat(response.getValidationNotes()).isEqualTo("Corrected SSN mismatch via manual verification");
            verify(mpiMergeRepository).save(any(MPIMergeEntity.class));
        }
    }

    @Nested
    @DisplayName("MPI Metrics")
    class MPIMetrics {

        @Test
        @DisplayName("Should calculate validation success rate correctly")
        void shouldCalculateValidationSuccessRate() {
            // Given - 10 total merges: 6 validated, 2 pending, 1 rolled_back, 1 failed
            List<MPIMergeEntity> merges = List.of(
                buildMerge("VALIDATED", "AUTOMATIC", 0.95),
                buildMerge("VALIDATED", "AUTOMATIC", 0.92),
                buildMerge("VALIDATED", "MANUAL", 0.88),
                buildMerge("VALIDATED", "ASSISTED", 0.85),
                buildMerge("VALIDATED", "AUTOMATIC", 0.97),
                buildMerge("VALIDATED", "MANUAL", 0.91),
                buildMerge("PENDING", "AUTOMATIC", 0.75),
                buildMerge("PENDING", "ASSISTED", 0.68),
                buildMerge("ROLLED_BACK", "AUTOMATIC", 0.55),
                buildMerge("FAILED", "AUTOMATIC", 0.30)
            );

            when(mpiMergeRepository.findByTenantIdAndDateRange(
                eq(TENANT_ID), any(LocalDateTime.class), any(LocalDateTime.class)
            )).thenReturn(merges);

            // When
            MPIMetricsResponse metrics = mpiAuditService.getMPIMetrics(
                TENANT_ID, null, null
            );

            // Then
            assertThat(metrics.getTotalMerges()).isEqualTo(10);
            assertThat(metrics.getValidatedMerges()).isEqualTo(6);
            assertThat(metrics.getPendingValidation()).isEqualTo(2);
            assertThat(metrics.getRolledBackMerges()).isEqualTo(1);
            assertThat(metrics.getFailedMerges()).isEqualTo(1);
            assertThat(metrics.getValidationSuccessRate()).isEqualTo(0.6);
            assertThat(metrics.getRollbackRate()).isEqualTo(0.1);
            assertThat(metrics.getAutomaticMerges()).isEqualTo(6);
            assertThat(metrics.getManualMerges()).isEqualTo(2);
            assertThat(metrics.getAssistedMerges()).isEqualTo(2);
        }
    }

    private MPIMergeEntity buildMerge(String status, String mergeType, Double confidence) {
        return MPIMergeEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .sourcePatientId("source-" + UUID.randomUUID().toString().substring(0, 8))
            .targetPatientId("target-" + UUID.randomUUID().toString().substring(0, 8))
            .mergeType(mergeType)
            .confidenceScore(confidence)
            .mergeStatus(status)
            .mergeTimestamp(LocalDateTime.now())
            .performedBy("system")
            .build();
    }
}
