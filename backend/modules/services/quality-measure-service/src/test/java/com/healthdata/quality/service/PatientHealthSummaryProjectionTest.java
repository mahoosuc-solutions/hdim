package com.healthdata.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doNothing;

import com.healthdata.quality.dto.PopulationMetricsDTO;
import com.healthdata.quality.persistence.CareGapRepository;
import com.healthdata.quality.persistence.ClinicalAlertEntity;
import com.healthdata.quality.persistence.ClinicalAlertRepository;
import com.healthdata.quality.persistence.HealthScoreHistoryEntity;
import com.healthdata.quality.persistence.HealthScoreHistoryRepository;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.persistence.MentalHealthAssessmentRepository;
import com.healthdata.quality.persistence.PatientHealthSummaryEntity;
import com.healthdata.quality.persistence.PatientHealthSummaryRepository;
import com.healthdata.quality.persistence.PopulationMetricsRepository;
import com.healthdata.quality.persistence.PopulationMetricsEntity;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Health Summary Projection Tests")
class PatientHealthSummaryProjectionTest {

    @Mock
    private PatientHealthSummaryRepository readRepository;

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private RiskAssessmentRepository riskRepository;

    @Mock
    private MentalHealthAssessmentRepository mentalHealthRepository;

    @Mock
    private PopulationMetricsRepository populationMetricsRepository;

    @Mock
    private HealthScoreHistoryRepository healthScoreHistoryRepository;

    @Mock
    private ClinicalAlertRepository clinicalAlertRepository;

    @InjectMocks
    private PatientHealthSummaryProjection projection;

    @Test
    @DisplayName("Should ignore out-of-order health score events")
    void shouldIgnoreOutOfOrderHealthScoreEvent() {
        UUID patientId = UUID.randomUUID();
        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setTenantId("tenant-1");
        summary.setPatientId(patientId);
        summary.setHealthScoreUpdatedAt(Instant.parse("2024-02-01T00:00:00Z"));

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(summary));

        projection.onHealthScoreUpdatedWithTimestamp(
            "tenant-1",
            patientId,
            75.0,
            "stable",
            Instant.parse("2024-01-01T00:00:00Z")
        );

        verify(readRepository, never()).save(any(PatientHealthSummaryEntity.class));
    }

    @Test
    @DisplayName("Should rebuild projection with aggregated data")
    void shouldRebuildProjectionForPatient() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(careGapRepository.countOpenCareGaps(tenantId, patientId)).thenReturn(2L);
        when(careGapRepository.countUrgentCareGaps(tenantId, patientId)).thenReturn(1L);

        RiskAssessmentEntity risk = RiskAssessmentEntity.builder()
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .riskScore(70)
            .assessmentDate(Instant.parse("2024-01-10T00:00:00Z"))
            .build();
        when(riskRepository.findLatestByTenantIdAndPatientId(tenantId, patientId))
            .thenReturn(Optional.of(risk));

        HealthScoreHistoryEntity latest = HealthScoreHistoryEntity.builder()
            .overallScore(80.0)
            .calculatedAt(Instant.parse("2024-02-01T00:00:00Z"))
            .build();
        HealthScoreHistoryEntity previous = HealthScoreHistoryEntity.builder()
            .overallScore(70.0)
            .calculatedAt(Instant.parse("2024-01-01T00:00:00Z"))
            .build();
        when(healthScoreHistoryRepository.findRecentScores(tenantId, patientId, 2))
            .thenReturn(List.of(latest, previous));

        when(clinicalAlertRepository.countByTenantIdAndPatientIdAndStatus(
            tenantId, patientId, ClinicalAlertEntity.AlertStatus.ACTIVE)).thenReturn(3L);

        ClinicalAlertEntity critical = ClinicalAlertEntity.builder()
            .patientId(patientId)
            .severity(ClinicalAlertEntity.AlertSeverity.CRITICAL)
            .status(ClinicalAlertEntity.AlertStatus.ACTIVE)
            .build();
        when(clinicalAlertRepository.findByTenantIdAndSeverityAndStatusOrderByTriggeredAtDesc(
            tenantId, ClinicalAlertEntity.AlertSeverity.CRITICAL, ClinicalAlertEntity.AlertStatus.ACTIVE))
            .thenReturn(List.of(critical, critical));

        MentalHealthAssessmentEntity mental = new MentalHealthAssessmentEntity();
        mental.setScore(12);
        mental.setSeverity("moderate");
        mental.setAssessmentDate(Instant.parse("2024-01-15T00:00:00Z"));
        when(mentalHealthRepository.findFirstByTenantIdAndPatientIdOrderByAssessmentDateDesc(
            tenantId, patientId)).thenReturn(Optional.of(mental));

        projection.rebuildProjectionForPatient(tenantId, patientId);

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity saved = captor.getValue();
        assertThat(saved.getOpenCareGapsCount()).isEqualTo(2);
        assertThat(saved.getUrgentGapsCount()).isEqualTo(1);
        assertThat(saved.getRiskLevel()).isEqualTo("high");
        assertThat(saved.getRiskScore()).isEqualTo(70.0);
        assertThat(saved.getLatestHealthScore()).isEqualTo(80.0);
        assertThat(saved.getHealthTrend()).isEqualTo("improving");
        assertThat(saved.getActiveAlertsCount()).isEqualTo(3);
        assertThat(saved.getCriticalAlertsCount()).isEqualTo(2);
        assertThat(saved.getLatestMentalHealthScore()).isEqualTo(12);
        assertThat(saved.getMentalHealthSeverity()).isEqualTo("moderate");
    }

    @Test
    @DisplayName("Should return zero metrics when repository returns nulls")
    void shouldReturnZeroMetricsWhenNulls() {
        when(readRepository.countByTenantId("tenant-1")).thenReturn(null);
        when(readRepository.averageHealthScoreByTenantId("tenant-1")).thenReturn(null);
        when(readRepository.countHighRiskPatients("tenant-1")).thenReturn(null);
        when(readRepository.countMediumRiskPatients("tenant-1")).thenReturn(null);
        when(readRepository.totalOpenCareGaps("tenant-1")).thenReturn(null);

        PopulationMetricsDTO metrics = projection.getPopulationMetrics("tenant-1");

        assertThat(metrics.getTotalPatients()).isEqualTo(0);
        assertThat(metrics.getAverageHealthScore()).isEqualTo(0.0);
        assertThat(metrics.getHighRiskCount()).isEqualTo(0);
        assertThat(metrics.getMediumRiskCount()).isEqualTo(0);
        assertThat(metrics.getTotalCareGaps()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should skip daily metrics when no tenants")
    void shouldSkipDailyMetricsWhenNoTenants() {
        when(readRepository.findDistinctTenantIds()).thenReturn(List.of());

        projection.calculateDailyPopulationMetrics();

        verify(populationMetricsRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update health score summary")
    void shouldUpdateHealthScoreSummary() {
        UUID patientId = UUID.randomUUID();

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.empty());

        projection.onHealthScoreUpdated("tenant-1", patientId, 82.5, "improving");

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity summary = captor.getValue();
        assertThat(summary.getLatestHealthScore()).isEqualTo(82.5);
        assertThat(summary.getHealthTrend()).isEqualTo("improving");
        assertThat(summary.getHealthScoreUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update care gap counts")
    void shouldUpdateCareGapCounts() {
        UUID patientId = UUID.randomUUID();

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(new PatientHealthSummaryEntity()));
        when(careGapRepository.countOpenCareGaps("tenant-1", patientId)).thenReturn(4L);
        when(careGapRepository.countUrgentCareGaps("tenant-1", patientId)).thenReturn(2L);

        projection.onCareGapAutoClosed("tenant-1", patientId, "gap-1");

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity summary = captor.getValue();
        assertThat(summary.getOpenCareGapsCount()).isEqualTo(4);
        assertThat(summary.getUrgentGapsCount()).isEqualTo(2);
        assertThat(summary.getCareGapsUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create summary when care gap update arrives for new patient")
    void shouldCreateSummaryWhenCareGapUpdateArrivesForNewPatient() {
        UUID patientId = UUID.randomUUID();

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.empty());
        when(careGapRepository.countOpenCareGaps("tenant-1", patientId)).thenReturn(1L);
        when(careGapRepository.countUrgentCareGaps("tenant-1", patientId)).thenReturn(0L);

        projection.onCareGapAutoClosed("tenant-1", patientId, "gap-2");

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity summary = captor.getValue();
        assertThat(summary.getTenantId()).isEqualTo("tenant-1");
        assertThat(summary.getPatientId()).isEqualTo(patientId);
        assertThat(summary.getOpenCareGapsCount()).isEqualTo(1);
        assertThat(summary.getUrgentGapsCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should increment alert counts for critical alerts")
    void shouldIncrementAlertCounts() {
        UUID patientId = UUID.randomUUID();
        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setActiveAlertsCount(1);
        summary.setCriticalAlertsCount(1);

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(summary));

        projection.onClinicalAlertTriggered("tenant-1", patientId, "CRITICAL", "alert");

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity updated = captor.getValue();
        assertThat(updated.getActiveAlertsCount()).isEqualTo(2);
        assertThat(updated.getCriticalAlertsCount()).isEqualTo(2);
        assertThat(updated.getAlertsUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should create summary when risk assessment arrives for new patient")
    void shouldCreateSummaryWhenRiskAssessmentArrivesForNewPatient() {
        UUID patientId = UUID.randomUUID();

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.empty());

        projection.onRiskAssessmentUpdated("tenant-1", patientId, "LOW", 22.0);

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getPatientId()).isEqualTo(patientId);
        assertThat(saved.getRiskLevel()).isEqualTo("LOW");
        assertThat(saved.getRiskScore()).isEqualTo(22.0);
        assertThat(saved.getRiskUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should rebuild projections for all tenant patients")
    void shouldRebuildAllProjections() {
        PatientHealthSummaryProjection projectionSpy = spy(new PatientHealthSummaryProjection(
            readRepository,
            careGapRepository,
            riskRepository,
            mentalHealthRepository,
            populationMetricsRepository,
            healthScoreHistoryRepository,
            clinicalAlertRepository
        ));

        when(readRepository.findDistinctTenantIds()).thenReturn(List.of("tenant-1"));
        when(careGapRepository.findDistinctTenantIds()).thenReturn(List.of());
        UUID patientA = UUID.randomUUID();
        UUID patientB = UUID.randomUUID();
        when(readRepository.findDistinctPatientIdsByTenantId("tenant-1"))
            .thenReturn(List.of(patientA));
        when(careGapRepository.findDistinctPatientIdsByTenantId("tenant-1"))
            .thenReturn(List.of(patientB));

        doNothing().when(projectionSpy).rebuildProjectionForPatient(any(), any());

        projectionSpy.rebuildAllProjections();

        verify(projectionSpy).rebuildProjectionForPatient("tenant-1", patientA);
        verify(projectionSpy).rebuildProjectionForPatient("tenant-1", patientB);
    }

    @Test
    @DisplayName("Should continue rebuild when one patient fails")
    void shouldContinueRebuildWhenOnePatientFails() {
        PatientHealthSummaryProjection projectionSpy = spy(new PatientHealthSummaryProjection(
            readRepository,
            careGapRepository,
            riskRepository,
            mentalHealthRepository,
            populationMetricsRepository,
            healthScoreHistoryRepository,
            clinicalAlertRepository
        ));

        when(readRepository.findDistinctTenantIds()).thenReturn(List.of("tenant-1"));
        when(careGapRepository.findDistinctTenantIds()).thenReturn(List.of());
        UUID patientA = UUID.randomUUID();
        UUID patientB = UUID.randomUUID();
        when(readRepository.findDistinctPatientIdsByTenantId("tenant-1"))
            .thenReturn(List.of(patientA, patientB));
        when(careGapRepository.findDistinctPatientIdsByTenantId("tenant-1"))
            .thenReturn(List.of());

        doThrow(new RuntimeException("boom"))
            .when(projectionSpy).rebuildProjectionForPatient("tenant-1", patientA);
        doNothing().when(projectionSpy).rebuildProjectionForPatient("tenant-1", patientB);

        projectionSpy.rebuildAllProjections();

        verify(projectionSpy).rebuildProjectionForPatient("tenant-1", patientA);
        verify(projectionSpy).rebuildProjectionForPatient("tenant-1", patientB);
    }

    @Test
    @DisplayName("Should calculate daily population metrics for tenant")
    void shouldCalculateDailyPopulationMetrics() {
        when(readRepository.findDistinctTenantIds()).thenReturn(List.of("tenant-1"));
        when(populationMetricsRepository.findByTenantIdAndMetricDate(
            any(), any(LocalDate.class))).thenReturn(Optional.empty());
        when(readRepository.countByTenantId("tenant-1")).thenReturn(10L);
        when(readRepository.averageHealthScoreByTenantId("tenant-1")).thenReturn(72.5);
        when(readRepository.countHighRiskPatients("tenant-1")).thenReturn(2L);
        when(readRepository.countMediumRiskPatients("tenant-1")).thenReturn(4L);
        when(readRepository.totalOpenCareGaps("tenant-1")).thenReturn(6L);

        projection.calculateDailyPopulationMetrics();

        ArgumentCaptor<PopulationMetricsEntity> captor =
            ArgumentCaptor.forClass(PopulationMetricsEntity.class);
        verify(populationMetricsRepository).save(captor.capture());

        PopulationMetricsEntity metrics = captor.getValue();
        assertThat(metrics.getTenantId()).isEqualTo("tenant-1");
        assertThat(metrics.getTotalPatients()).isEqualTo(10);
        assertThat(metrics.getAverageHealthScore()).isEqualTo(72.5);
        assertThat(metrics.getHighRiskCount()).isEqualTo(2);
        assertThat(metrics.getMediumRiskCount()).isEqualTo(4);
        assertThat(metrics.getTotalCareGaps()).isEqualTo(6);
        assertThat(metrics.getMetricDate()).isNotNull();
    }

    @Test
    @DisplayName("Should update risk assessment summary")
    void shouldUpdateRiskAssessmentSummary() {
        UUID patientId = UUID.randomUUID();
        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(summary));

        projection.onRiskAssessmentUpdated("tenant-1", patientId, "HIGH", 82.0);

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity updated = captor.getValue();
        assertThat(updated.getRiskLevel()).isEqualTo("HIGH");
        assertThat(updated.getRiskScore()).isEqualTo(82.0);
        assertThat(updated.getRiskUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not increment critical alerts for non-critical severity")
    void shouldNotIncrementCriticalAlertsForNonCriticalSeverity() {
        UUID patientId = UUID.randomUUID();
        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setActiveAlertsCount(2);
        summary.setCriticalAlertsCount(1);

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(summary));

        projection.onClinicalAlertTriggered("tenant-1", patientId, "HIGH", "alert");

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity updated = captor.getValue();
        assertThat(updated.getActiveAlertsCount()).isEqualTo(3);
        assertThat(updated.getCriticalAlertsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should update health score when event timestamp is newer")
    void shouldUpdateHealthScoreWhenTimestampIsNewer() {
        UUID patientId = UUID.randomUUID();
        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setHealthScoreUpdatedAt(Instant.parse("2024-01-01T00:00:00Z"));

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(summary));

        Instant eventTime = Instant.parse("2024-02-01T00:00:00Z");
        projection.onHealthScoreUpdatedWithTimestamp(
            "tenant-1",
            patientId,
            88.0,
            "improving",
            eventTime
        );

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());
        assertThat(captor.getValue().getHealthScoreUpdatedAt()).isEqualTo(eventTime);
    }

    @Test
    @DisplayName("Should create summary when clinical alert arrives for new patient")
    void shouldCreateSummaryWhenClinicalAlertArrivesForNewPatient() {
        UUID patientId = UUID.randomUUID();

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.empty());

        projection.onClinicalAlertTriggered("tenant-1", patientId, "critical", "alert");

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getPatientId()).isEqualTo(patientId);
        assertThat(saved.getActiveAlertsCount()).isEqualTo(1);
        assertThat(saved.getCriticalAlertsCount()).isEqualTo(1);
        assertThat(saved.getAlertsUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update health score when existing summary has no timestamp")
    void shouldUpdateHealthScoreWhenExistingSummaryHasNoTimestamp() {
        UUID patientId = UUID.randomUUID();
        PatientHealthSummaryEntity summary = new PatientHealthSummaryEntity();
        summary.setHealthScoreUpdatedAt(null);

        when(readRepository.findByTenantIdAndPatientId("tenant-1", patientId))
            .thenReturn(Optional.of(summary));

        projection.onHealthScoreUpdatedWithTimestamp(
            "tenant-1",
            patientId,
            91.0,
            "stable",
            Instant.parse("2024-03-01T00:00:00Z")
        );

        ArgumentCaptor<PatientHealthSummaryEntity> captor =
            ArgumentCaptor.forClass(PatientHealthSummaryEntity.class);
        verify(readRepository).save(captor.capture());

        PatientHealthSummaryEntity saved = captor.getValue();
        assertThat(saved.getLatestHealthScore()).isEqualTo(91.0);
        assertThat(saved.getHealthTrend()).isEqualTo("stable");
        assertThat(saved.getHealthScoreUpdatedAt()).isEqualTo(Instant.parse("2024-03-01T00:00:00Z"));
    }

    @Test
    @DisplayName("Should calculate daily population metrics for existing entity")
    void shouldCalculateDailyPopulationMetricsForExistingEntity() {
        when(readRepository.findDistinctTenantIds()).thenReturn(List.of("tenant-1"));
        when(populationMetricsRepository.findByTenantIdAndMetricDate(
            any(), any(LocalDate.class))).thenReturn(Optional.of(new PopulationMetricsEntity()));
        when(readRepository.countByTenantId("tenant-1")).thenReturn(5L);
        when(readRepository.averageHealthScoreByTenantId("tenant-1")).thenReturn(78.5);
        when(readRepository.countHighRiskPatients("tenant-1")).thenReturn(2L);
        when(readRepository.countMediumRiskPatients("tenant-1")).thenReturn(1L);
        when(readRepository.totalOpenCareGaps("tenant-1")).thenReturn(4L);

        projection.calculateDailyPopulationMetrics();

        ArgumentCaptor<PopulationMetricsEntity> captor =
            ArgumentCaptor.forClass(PopulationMetricsEntity.class);
        verify(populationMetricsRepository).save(captor.capture());
        PopulationMetricsEntity saved = captor.getValue();
        assertThat(saved.getTotalPatients()).isEqualTo(5);
        assertThat(saved.getAverageHealthScore()).isEqualTo(78.5);
        assertThat(saved.getHighRiskCount()).isEqualTo(2);
        assertThat(saved.getMediumRiskCount()).isEqualTo(1);
        assertThat(saved.getTotalCareGaps()).isEqualTo(4);
    }
}
