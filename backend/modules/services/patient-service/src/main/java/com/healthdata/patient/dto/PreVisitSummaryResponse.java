package com.healthdata.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Pre-Visit Summary Response DTO
 *
 * Issue #6: Provides comprehensive pre-visit summary for providers
 * preparing for patient visits.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreVisitSummaryResponse {

    private String patientId;
    private String patientName;
    private Instant appointmentDate;
    private String appointmentType;

    private List<CareGapItem> careGaps;
    private List<RecentResultItem> recentResults;
    private List<MedicationItem> medications;
    private List<AgendaItem> suggestedAgenda;
    private String lastVisitSummary;

    // Patient demographics for quick reference
    private PatientDemographics demographics;

    // Risk indicators
    private RiskIndicators riskIndicators;

    // Summary timestamp
    private Instant generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareGapItem {
        private String gapId;
        private String measureId;
        private String measureName;
        private String priority; // HIGH, MEDIUM, LOW
        private String recommendation;
        private LocalDate dueDate;
        private String gapCategory;
        private List<String> suggestedActions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentResultItem {
        private String name;
        private String value;
        private String unit;
        private LocalDate date;
        private String trend; // improving, stable, worsening
        private String previousValue;
        private LocalDate previousDate;
        private String interpretation; // normal, abnormal, critical
        private String loincCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationItem {
        private String name;
        private String dosage;
        private String frequency;
        private String adherence; // good, moderate, poor, unknown
        private LocalDate startDate;
        private String prescriber;
        private String status; // active, on-hold, discontinued
        private List<String> potentialIssues; // drug interactions, duplicates, etc.
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgendaItem {
        private String topic;
        private String timeEstimate; // e.g., "5 min"
        private int priority; // 1 = highest
        private String category; // care-gap, medication, counseling, screening
        private String rationale; // Why this is suggested
        private List<String> talkingPoints;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientDemographics {
        private Integer age;
        private String gender;
        private String preferredLanguage;
        private String primaryInsurance;
        private String phoneNumber;
        private LocalDate lastVisitDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskIndicators {
        private BigDecimal hccScore;
        private String riskLevel; // low, moderate, high, very-high
        private List<String> chronicConditions;
        private Boolean hasRecentHospitalization;
        private Boolean hasRecentEDVisit;
        private Integer missedAppointments;
    }
}
