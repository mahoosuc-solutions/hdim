package com.healthdata.quality.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.healthdata.quality.dto.AddressCareGapRequest;
import com.healthdata.quality.dto.CareGapDTO;
import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.MentalHealthAssessmentRequest;
import com.healthdata.quality.dto.PatientHealthOverviewDTO;
import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.service.CareGapService;
import com.healthdata.quality.service.MentalHealthAssessmentService;
import com.healthdata.quality.service.PatientHealthService;
import com.healthdata.quality.service.RiskStratificationService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Health Controller Tests")
class PatientHealthControllerTest {

    @Mock
    private PatientHealthService healthService;

    @Mock
    private MentalHealthAssessmentService mentalHealthService;

    @Mock
    private CareGapService careGapService;

    @Mock
    private RiskStratificationService riskService;

    @InjectMocks
    private PatientHealthController controller;

    @Test
    @DisplayName("Should return patient health overview")
    void shouldReturnPatientHealthOverview() {
        UUID patientId = UUID.randomUUID();
        PatientHealthOverviewDTO overview = PatientHealthOverviewDTO.builder()
            .patientId(patientId)
            .build();
        when(healthService.getPatientHealthOverview("tenant-1", patientId)).thenReturn(overview);

        var response = controller.getPatientHealthOverview("tenant-1", patientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPatientId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("Should submit mental health assessment")
    void shouldSubmitMentalHealthAssessment() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentRequest request = MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("PHQ-9")
            .responses(Map.of("q1", 1))
            .assessedBy("provider-1")
            .build();
        MentalHealthAssessmentDTO assessment = MentalHealthAssessmentDTO.builder()
            .patientId(patientId)
            .build();
        when(mentalHealthService.submitAssessment("tenant-1", request)).thenReturn(assessment);

        var response = controller.submitMentalHealthAssessment("tenant-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getPatientId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("Should return patient assessments")
    void shouldReturnPatientAssessments() {
        UUID patientId = UUID.randomUUID();
        when(mentalHealthService.getPatientAssessments("tenant-1", patientId, "PHQ-9", 10, 0))
            .thenReturn(List.of(MentalHealthAssessmentDTO.builder().patientId(patientId).build()));

        var response = controller.getPatientAssessments("tenant-1", patientId, "PHQ-9", 10, 0);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Should return assessment trend")
    void shouldReturnAssessmentTrend() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentService.AssessmentTrend trend =
            MentalHealthAssessmentService.AssessmentTrend.builder()
                .trend("improving")
                .dataPoints(List.of())
                .build();
        when(mentalHealthService.getAssessmentTrend("tenant-1", patientId, "PHQ-9", null, null))
            .thenReturn(trend);

        var response = controller.getAssessmentTrend("tenant-1", patientId, "PHQ-9", null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTrend()).isEqualTo("improving");
    }

    @Test
    @DisplayName("Should return assessment trend with date filters")
    void shouldReturnAssessmentTrendWithDates() {
        UUID patientId = UUID.randomUUID();
        MentalHealthAssessmentService.AssessmentTrend trend =
            MentalHealthAssessmentService.AssessmentTrend.builder()
                .trend("stable")
                .dataPoints(List.of())
                .build();
        when(mentalHealthService.getAssessmentTrend(
            "tenant-1", patientId, "GAD-7", "2024-01-01", "2024-02-01"))
            .thenReturn(trend);

        var response = controller.getAssessmentTrend(
            "tenant-1", patientId, "GAD-7", "2024-01-01", "2024-02-01");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getTrend()).isEqualTo("stable");
    }

    @Test
    @DisplayName("Should return care gap trends")
    void shouldReturnCareGapTrends() {
        List<CareGapService.CareGapTrendPoint> points = List.of(
            new CareGapService.CareGapTrendPoint(
                Instant.parse("2026-02-24T00:00:00Z"),
                12,
                2,
                1,
                Map.of("high", 4, "medium", 5, "low", 3),
                Map.of("screening", 3, "medication", 2, "followup", 4, "lab", 1, "assessment", 2)
            )
        );
        when(careGapService.getCareGapTrends("tenant-1", 30)).thenReturn(points);

        var response = controller.getCareGapTrends("tenant-1", 30);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).totalGaps()).isEqualTo(12);
    }

    @Test
    @DisplayName("Should return patient care gaps")
    void shouldReturnPatientCareGaps() {
        UUID patientId = UUID.randomUUID();
        when(careGapService.getPatientCareGaps("tenant-1", patientId, null, null))
            .thenReturn(List.of(CareGapDTO.builder().patientId(patientId).build()));

        var response = controller.getPatientCareGaps("tenant-1", patientId, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Should return patient care gaps filtered by status and category")
    void shouldReturnPatientCareGapsWithFilters() {
        UUID patientId = UUID.randomUUID();
        when(careGapService.getPatientCareGaps("tenant-1", patientId, "open", "mental-health"))
            .thenReturn(List.of(CareGapDTO.builder().patientId(patientId).build()));

        var response = controller.getPatientCareGaps("tenant-1", patientId, "open", "mental-health");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Should address care gap")
    void shouldAddressCareGap() {
        AddressCareGapRequest request = AddressCareGapRequest.builder()
            .addressedBy("provider-1")
            .notes("done")
            .status("addressed")
            .build();
        UUID gapId = UUID.randomUUID();
        CareGapDTO gap = CareGapDTO.builder().id(gapId.toString()).build();
        when(careGapService.addressCareGap(eq("tenant-1"), eq(gapId), eq(request)))
            .thenReturn(gap);

        var response = controller.addressCareGap("tenant-1", gapId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo(gapId.toString());
    }

    @Test
    @DisplayName("Should calculate and fetch risk stratification")
    void shouldCalculateAndFetchRiskStratification() {
        UUID patientId = UUID.randomUUID();
        RiskAssessmentDTO assessment = RiskAssessmentDTO.builder().patientId(patientId).build();
        when(riskService.calculateRiskAssessment("tenant-1", patientId)).thenReturn(assessment);
        when(riskService.getRiskAssessment("tenant-1", patientId)).thenReturn(assessment);

        var calcResponse = controller.calculateRiskStratification("tenant-1", patientId);
        var getResponse = controller.getRiskStratification("tenant-1", patientId);

        assertThat(calcResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getPatientId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("Should return patient health score")
    void shouldReturnPatientHealthScore() {
        UUID patientId = UUID.randomUUID();
        HealthScoreDTO score = HealthScoreDTO.builder().patientId(patientId).build();
        when(healthService.calculateHealthScore("tenant-1", patientId)).thenReturn(score);

        var response = controller.getHealthScore("tenant-1", patientId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPatientId()).isEqualTo(patientId);
    }
}
