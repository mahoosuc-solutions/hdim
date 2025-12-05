package com.healthdata.api.dto;

import com.healthdata.patient.domain.Patient;
import com.healthdata.quality.domain.MeasureResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive patient health overview DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientHealthOverview {
    private Patient patient;
    private Double overallScore;
    private MeasureResult diabetesControl;
    private MeasureResult bloodPressureControl;
    private MeasureResult medicationAdherence;
    private List<CareGapSummary> careGaps;
    private Map<String, Object> riskFactors;
    private LocalDateTime lastAssessment;
    private String healthStatus; // EXCELLENT, GOOD, FAIR, POOR

    @Data
    @Builder
    public static class CareGapSummary {
        private String id;
        private String type;
        private String description;
        private String priority;
        private LocalDateTime dueDate;
    }
}