package com.healthdata.qualitymeasure.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.qualityevent.api.v1.dto.EvaluateMeasureRequest;
import com.healthdata.qualityevent.api.v1.dto.MeasureEventResponse;
import com.healthdata.qualityevent.eventhandler.QualityMeasureEventHandler;
import com.healthdata.qualitymeasure.persistence.CohortMeasureRateRepository;
import com.healthdata.qualitymeasure.persistence.MeasureEvaluationRepository;
import com.healthdata.qualityevent.projection.CohortMeasureRateProjection;
import com.healthdata.qualityevent.projection.MeasureEvaluationProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

/**
 * Mock-based unit tests for QualityMeasureEventApplicationService
 *
 * No database required - validates business logic only
 *
 * ★ Insight ─────────────────────────────────────
 * This test suite uses Mockito to validate ApplicationService business logic
 * without requiring database connections or Liquibase migrations:
 *
 * 1. **Fast feedback loop** - Tests run in <5 seconds
 * 2. **Isolated testing** - Validates calculation logic independent of schema
 * 3. **Easy debugging** - Failures clearly identify logic issues vs. database issues
 * 4. **CI/CD friendly** - No database dependencies or container requirements
 *
 * The service calculates:
 * - Measure compliance thresholds (score > 0.75 = MET)
 * - Risk stratification levels (VERY_HIGH, HIGH, MEDIUM, LOW)
 * - Cohort aggregation (compliance rate = numerator / denominator)
 * ─────────────────────────────────────────────────
 */
@ExtendWith(MockitoExtension.class)
class QualityMeasureEventApplicationServiceTest {

    @Mock
    private QualityMeasureEventHandler measureEventHandler;

    @Mock
    private MeasureEvaluationRepository evaluationRepository;

    @Mock
    private CohortMeasureRateRepository cohortRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private QualityMeasureEventApplicationService applicationService;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-123";
    private static final String MEASURE_CODE = "CMS122";

    private EvaluateMeasureRequest measureRequest;

    @BeforeEach
    void setUp() {
        measureRequest = EvaluateMeasureRequest.builder()
            .patientId(PATIENT_ID)
            .measureCode(MEASURE_CODE)
            .score(0.85f)
            .build();
    }

    // ==================== evaluateMeasure() Tests ====================

    @Test
    void evaluateMeasure_ShouldReturnMET_WhenScoreAboveThreshold() {
        // Given - score > 0.75 should be MET
        measureRequest.setScore(0.85f);
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        MeasureEventResponse response = applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then
        assertThat(response.getMeasureStatus()).isEqualTo("MET");
        assertThat(response.getScore()).isEqualTo(0.85f);
        assertThat(response.getMeasureCode()).isEqualTo(MEASURE_CODE);
        assertThat(response.getPatientId()).isEqualTo(PATIENT_ID);
    }

    @Test
    void evaluateMeasure_ShouldReturnPARTIAL_WhenScoreBelowThreshold() {
        // Given - score between 0 and 0.75 should be PARTIAL
        measureRequest.setScore(0.65f);
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        MeasureEventResponse response = applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then
        assertThat(response.getMeasureStatus()).isEqualTo("PARTIAL");
        assertThat(response.getScore()).isEqualTo(0.65f);
    }

    @Test
    void evaluateMeasure_ShouldReturnNOT_MET_WhenScoreZero() {
        // Given - score = 0 should be NOT_MET
        measureRequest.setScore(0.0f);
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        MeasureEventResponse response = applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then
        assertThat(response.getMeasureStatus()).isEqualTo("NOT_MET");
        assertThat(response.getScore()).isEqualTo(0.0f);
    }

    @Test
    void evaluateMeasure_ShouldRejectInvalidScore_WhenBelowZero() {
        // Given - score < 0 is invalid
        measureRequest.setScore(-0.1f);

        // When/Then
        assertThatThrownBy(() -> applicationService.evaluateMeasure(measureRequest, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Score must be between 0.0 and 1.0");
    }

    @Test
    void evaluateMeasure_ShouldRejectInvalidScore_WhenAboveOne() {
        // Given - score > 1.0 is invalid
        measureRequest.setScore(1.1f);

        // When/Then
        assertThatThrownBy(() -> applicationService.evaluateMeasure(measureRequest, TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Score must be between 0.0 and 1.0");
    }

    @Test
    void evaluateMeasure_ShouldPublishKafkaEvent() {
        // Given
        measureRequest.setScore(0.85f);
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then
        verify(kafkaTemplate).send(eq("measure.events"), eq(PATIENT_ID), any());
    }

    @Test
    void evaluateMeasure_ShouldDelegateToEventHandler() {
        // Given
        measureRequest.setScore(0.85f);
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then
        verify(measureEventHandler, times(1)).handle(any(com.healthdata.qualityevent.event.MeasureScoreCalculatedEvent.class));
    }

    // ==================== Cohort Metrics Tests ====================

    @Test
    void evaluateMeasure_ShouldCreateNewCohort_WhenNoneExists() {
        // Given - no existing cohort rate
        measureRequest.setScore(0.85f);
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When
        applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then - new cohort created with denominator=1, numerator=1 (score > 0.75)
        ArgumentCaptor<CohortMeasureRateProjection> captor = ArgumentCaptor.forClass(CohortMeasureRateProjection.class);
        verify(cohortRepository).save(captor.capture());

        CohortMeasureRateProjection saved = captor.getValue();
        assertThat(saved.getMeasureCode()).isEqualTo(MEASURE_CODE);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getDenominatorCount()).isEqualTo(1);
        assertThat(saved.getNumeratorCount()).isEqualTo(1);
        assertThat(saved.getComplianceRate()).isEqualTo(1.0f);
    }

    @Test
    void evaluateMeasure_ShouldIncrementBothCounters_WhenScoreMeetsThreshold() {
        // Given - existing cohort rate
        CohortMeasureRateProjection existingCohort = new CohortMeasureRateProjection(MEASURE_CODE, TENANT_ID);
        existingCohort.setDenominatorCount(10);
        existingCohort.setNumeratorCount(8);
        existingCohort.setComplianceRate(0.8f);
        existingCohort.setVersion(1);

        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.of(existingCohort));

        measureRequest.setScore(0.85f);  // Meets threshold

        // When
        applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then - both counters incremented
        ArgumentCaptor<CohortMeasureRateProjection> captor = ArgumentCaptor.forClass(CohortMeasureRateProjection.class);
        verify(cohortRepository).save(captor.capture());

        CohortMeasureRateProjection saved = captor.getValue();
        assertThat(saved.getDenominatorCount()).isEqualTo(11);
        assertThat(saved.getNumeratorCount()).isEqualTo(9);
        assertThat(saved.getComplianceRate()).isEqualTo(9.0f / 11.0f);
    }

    @Test
    void evaluateMeasure_ShouldIncrementDenominatorOnly_WhenScoreBelowThreshold() {
        // Given
        CohortMeasureRateProjection existingCohort = new CohortMeasureRateProjection(MEASURE_CODE, TENANT_ID);
        existingCohort.setDenominatorCount(10);
        existingCohort.setNumeratorCount(8);
        existingCohort.setComplianceRate(0.8f);
        existingCohort.setVersion(1);

        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.of(existingCohort));

        measureRequest.setScore(0.65f);  // Below threshold

        // When
        applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then - only denominator incremented
        ArgumentCaptor<CohortMeasureRateProjection> captor = ArgumentCaptor.forClass(CohortMeasureRateProjection.class);
        verify(cohortRepository).save(captor.capture());

        CohortMeasureRateProjection saved = captor.getValue();
        assertThat(saved.getDenominatorCount()).isEqualTo(11);
        assertThat(saved.getNumeratorCount()).isEqualTo(8);  // unchanged
        assertThat(saved.getComplianceRate()).isEqualTo(8.0f / 11.0f);
    }

    @Test
    void evaluateMeasure_ShouldCalculateComplianceRate_Correctly() {
        // Given
        CohortMeasureRateProjection existingCohort = new CohortMeasureRateProjection(MEASURE_CODE, TENANT_ID);
        existingCohort.setDenominatorCount(99);
        existingCohort.setNumeratorCount(75);
        existingCohort.setComplianceRate(0.757f);
        existingCohort.setVersion(1);

        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.of(existingCohort));

        measureRequest.setScore(0.85f);

        // When
        applicationService.evaluateMeasure(measureRequest, TENANT_ID);

        // Then - compliance = 76 / 100 = 0.76
        ArgumentCaptor<CohortMeasureRateProjection> captor = ArgumentCaptor.forClass(CohortMeasureRateProjection.class);
        verify(cohortRepository).save(captor.capture());

        CohortMeasureRateProjection saved = captor.getValue();
        assertThat(saved.getDenominatorCount()).isEqualTo(100);
        assertThat(saved.getNumeratorCount()).isEqualTo(76);
        assertThat(saved.getComplianceRate()).isEqualTo(0.76f);
    }

    // ==================== getRiskScore() Tests ====================

    @Test
    void getRiskScore_ShouldReturnLOW_WhenScoreAbove80() {
        // Given
        MeasureEvaluationProjection evaluation = mock(MeasureEvaluationProjection.class);
        when(evaluation.getScore()).thenReturn(85.0);

        when(evaluationRepository.findByPatientIdAndTenant(PATIENT_ID, TENANT_ID))
            .thenReturn(Optional.of(evaluation));

        // When
        MeasureEventResponse response = applicationService.getRiskScore(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getScore()).isEqualTo(85.0f);
    }

    @Test
    void getRiskScore_ShouldReturnMEDIUM_WhenScoreBetween60And80() {
        // Given
        MeasureEvaluationProjection evaluation = mock(MeasureEvaluationProjection.class);
        when(evaluation.getScore()).thenReturn(70.0);

        when(evaluationRepository.findByPatientIdAndTenant(PATIENT_ID, TENANT_ID))
            .thenReturn(Optional.of(evaluation));

        // When
        MeasureEventResponse response = applicationService.getRiskScore(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(response.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(response.getScore()).isEqualTo(70.0f);
    }

    @Test
    void getRiskScore_ShouldReturnHIGH_WhenScoreBetween40And60() {
        // Given
        MeasureEvaluationProjection evaluation = mock(MeasureEvaluationProjection.class);
        when(evaluation.getScore()).thenReturn(50.0);

        when(evaluationRepository.findByPatientIdAndTenant(PATIENT_ID, TENANT_ID))
            .thenReturn(Optional.of(evaluation));

        // When
        MeasureEventResponse response = applicationService.getRiskScore(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
        assertThat(response.getScore()).isEqualTo(50.0f);
    }

    @Test
    void getRiskScore_ShouldReturnVERY_HIGH_WhenScoreBelow40() {
        // Given
        MeasureEvaluationProjection evaluation = mock(MeasureEvaluationProjection.class);
        when(evaluation.getScore()).thenReturn(30.0);

        when(evaluationRepository.findByPatientIdAndTenant(PATIENT_ID, TENANT_ID))
            .thenReturn(Optional.of(evaluation));

        // When
        MeasureEventResponse response = applicationService.getRiskScore(PATIENT_ID, TENANT_ID);

        // Then
        assertThat(response.getRiskLevel()).isEqualTo("VERY_HIGH");
        assertThat(response.getScore()).isEqualTo(30.0f);
    }

    @Test
    void getRiskScore_ShouldThrowException_WhenNoEvaluationExists() {
        // Given
        when(evaluationRepository.findByPatientIdAndTenant(PATIENT_ID, TENANT_ID))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> applicationService.getRiskScore(PATIENT_ID, TENANT_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No evaluation found for patient");
    }

    // ==================== getCohortCompliance() Tests ====================

    @Test
    void getCohortCompliance_ShouldReturnRate_WhenCohortExists() {
        // Given
        CohortMeasureRateProjection cohort = new CohortMeasureRateProjection(MEASURE_CODE, TENANT_ID);
        cohort.setDenominatorCount(100);
        cohort.setNumeratorCount(85);
        cohort.setComplianceRate(0.85f);

        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.of(cohort));

        // When
        MeasureEventResponse response = applicationService.getCohortCompliance(MEASURE_CODE, TENANT_ID);

        // Then
        assertThat(response.getComplianceRate()).isEqualTo(0.85f);
        assertThat(response.getDenominatorCount()).isEqualTo(100);
        assertThat(response.getNumeratorCount()).isEqualTo(85);
        assertThat(response.getMeasureCode()).isEqualTo(MEASURE_CODE);
    }

    @Test
    void getCohortCompliance_ShouldThrowException_WhenNoCohortExists() {
        // Given
        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> applicationService.getCohortCompliance(MEASURE_CODE, TENANT_ID))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("No cohort data found for measure");
    }

    @Test
    void getCohortCompliance_ShouldHandle100PercentCompliance() {
        // Given
        CohortMeasureRateProjection cohort = new CohortMeasureRateProjection(MEASURE_CODE, TENANT_ID);
        cohort.setDenominatorCount(50);
        cohort.setNumeratorCount(50);
        cohort.setComplianceRate(1.0f);

        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.of(cohort));

        // When
        MeasureEventResponse response = applicationService.getCohortCompliance(MEASURE_CODE, TENANT_ID);

        // Then
        assertThat(response.getComplianceRate()).isEqualTo(1.0f);
    }

    @Test
    void getCohortCompliance_ShouldHandleZeroCompliance() {
        // Given
        CohortMeasureRateProjection cohort = new CohortMeasureRateProjection(MEASURE_CODE, TENANT_ID);
        cohort.setDenominatorCount(50);
        cohort.setNumeratorCount(0);
        cohort.setComplianceRate(0.0f);

        when(cohortRepository.findByMeasureCodeAndTenant(MEASURE_CODE, TENANT_ID))
            .thenReturn(Optional.of(cohort));

        // When
        MeasureEventResponse response = applicationService.getCohortCompliance(MEASURE_CODE, TENANT_ID);

        // Then
        assertThat(response.getComplianceRate()).isEqualTo(0.0f);
        assertThat(response.getNumeratorCount()).isEqualTo(0);
    }
}
