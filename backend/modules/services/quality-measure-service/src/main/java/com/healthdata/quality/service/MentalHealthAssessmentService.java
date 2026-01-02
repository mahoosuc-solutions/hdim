package com.healthdata.quality.service;

import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.MentalHealthAssessmentRequest;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity.AssessmentType;
import com.healthdata.quality.persistence.MentalHealthAssessmentRepository;
import com.healthdata.quality.service.notification.MentalHealthNotificationTrigger;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Mental Health Assessment Service
 *
 * Handles mental health screening assessments with validated scoring algorithms:
 * - PHQ-9 (Depression)
 * - GAD-7 (Anxiety)
 * - PHQ-2 (Brief Depression Screen)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MentalHealthAssessmentService {

    private final MentalHealthAssessmentRepository repository;
    private final CareGapService careGapService;
    private final MentalHealthNotificationTrigger notificationTrigger;

    /**
     * Submit a mental health assessment
     */
    @Transactional
    public MentalHealthAssessmentDTO submitAssessment(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        log.info("Submitting {} assessment for patient {}", request.getAssessmentType(), request.getPatientId());

        // Calculate score based on assessment type
        MentalHealthAssessmentEntity assessment = calculateScore(tenantId, request);

        // Save to database
        assessment = repository.save(assessment);

        // Create care gap if positive screen
        if (assessment.getPositiveScreen() && assessment.getRequiresFollowup()) {
            careGapService.createMentalHealthFollowupGap(tenantId, assessment);
        }

        log.info("Assessment saved: {} - Score: {}/{}, Severity: {}, Positive: {}",
                assessment.getType(), assessment.getScore(), assessment.getMaxScore(),
                assessment.getSeverity(), assessment.getPositiveScreen());

        // Convert to DTO
        MentalHealthAssessmentDTO assessmentDTO = mapToDTO(assessment);

        // Trigger notification for assessment completion
        try {
            notificationTrigger.onAssessmentCompleted(tenantId, assessmentDTO);
        } catch (Exception e) {
            log.error("Failed to trigger mental health assessment notification for patient {}: {}",
                    assessment.getPatientId(), e.getMessage(), e);
            // Don't fail the assessment submission if notification fails
        }

        return assessmentDTO;
    }

    /**
     * Get patient assessments with optional filtering
     */
    public List<MentalHealthAssessmentDTO> getPatientAssessments(
        String tenantId,
        UUID patientId,
        String type,
        int limit,
        int offset
    ) {
        List<MentalHealthAssessmentEntity> assessments;

        if (type != null && !type.isBlank()) {
            AssessmentType assessmentType = AssessmentType.valueOf(type.toUpperCase().replace("-", "_"));
            assessments = repository.findByTenantIdAndPatientIdAndTypeOrderByAssessmentDateDesc(
                tenantId, patientId, assessmentType
            );
        } else {
            assessments = repository.findByTenantIdAndPatientIdOrderByAssessmentDateDesc(
                tenantId, patientId, PageRequest.of(offset / limit, limit)
            );
        }

        return assessments.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get assessment trend over time
     */
    public AssessmentTrend getAssessmentTrend(
        String tenantId,
        UUID patientId,
        String type,
        String startDateStr,
        String endDateStr
    ) {
        AssessmentType assessmentType = AssessmentType.valueOf(type.toUpperCase().replace("-", "_"));

        Instant startDate = startDateStr != null ?
            LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.systemDefault()).toInstant() :
            Instant.now().minus(java.time.Duration.ofDays(90));

        Instant endDate = endDateStr != null ?
            LocalDate.parse(endDateStr, DateTimeFormatter.ISO_DATE).atStartOfDay(ZoneId.systemDefault()).toInstant() :
            Instant.now();

        List<MentalHealthAssessmentEntity> assessments = repository.findByDateRange(
            tenantId, patientId, assessmentType, startDate, endDate
        );

        return buildTrend(patientId, assessmentType, assessments);
    }

    /**
     * Calculate score based on assessment type
     */
    private MentalHealthAssessmentEntity calculateScore(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        AssessmentType type = AssessmentType.valueOf(
            request.getAssessmentType().toUpperCase().replace("-", "_")
        );

        return switch (type) {
            case PHQ_9 -> scorePHQ9(tenantId, request);
            case GAD_7 -> scoreGAD7(tenantId, request);
            case PHQ_2 -> scorePHQ2(tenantId, request);
            case PHQ_A -> scorePHQA(tenantId, request);
            case AUDIT_C -> scoreAUDITC(tenantId, request);
            case DAST_10 -> scoreDAST10(tenantId, request);
            case PCL_5 -> scorePCL5(tenantId, request);
            case MDQ -> scoreMDQ(tenantId, request);
            default -> throw new UnsupportedOperationException("Assessment type not yet implemented: " + type);
        };
    }

    /**
     * Score PHQ-9 (Patient Health Questionnaire-9)
     * Range: 0-27
     * Threshold for positive screen: ≥10
     */
    private MentalHealthAssessmentEntity scorePHQ9(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        String severity;
        String interpretation;

        if (score <= 4) {
            severity = "minimal";
            interpretation = "Minimal or no depression";
        } else if (score <= 9) {
            severity = "mild";
            interpretation = "Mild depression";
        } else if (score <= 14) {
            severity = "moderate";
            interpretation = "Moderate depression";
        } else if (score <= 19) {
            severity = "moderately-severe";
            interpretation = "Moderately severe depression";
        } else {
            severity = "severe";
            interpretation = "Severe depression";
        }

        boolean positiveScreen = score >= 10;
        boolean requiresFollowup = score >= 10;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.PHQ_9)
            .score(score)
            .maxScore(27)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(10)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score GAD-7 (Generalized Anxiety Disorder-7)
     * Range: 0-21
     * Threshold for positive screen: ≥10
     */
    private MentalHealthAssessmentEntity scoreGAD7(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        String severity;
        String interpretation;

        if (score <= 4) {
            severity = "minimal";
            interpretation = "Minimal anxiety";
        } else if (score <= 9) {
            severity = "mild";
            interpretation = "Mild anxiety";
        } else if (score <= 14) {
            severity = "moderate";
            interpretation = "Moderate anxiety";
        } else {
            severity = "severe";
            interpretation = "Severe anxiety";
        }

        boolean positiveScreen = score >= 10;
        boolean requiresFollowup = score >= 10;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.GAD_7)
            .score(score)
            .maxScore(21)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(10)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score PHQ-2 (Brief Depression Screen)
     * Range: 0-6
     * Threshold for positive screen: ≥3
     */
    private MentalHealthAssessmentEntity scorePHQ2(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        String severity;
        String interpretation;

        if (score < 3) {
            severity = "negative";
            interpretation = "Negative screen for depression";
        } else {
            severity = "positive";
            interpretation = "Positive screen - recommend full PHQ-9 assessment";
        }

        boolean positiveScreen = score >= 3;
        boolean requiresFollowup = score >= 3;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.PHQ_2)
            .score(score)
            .maxScore(6)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(3)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score PHQ-A (PHQ-9 Modified for Adolescents)
     * Range: 0-27
     * Threshold for positive screen: ≥11
     * Different severity thresholds for adolescents
     */
    private MentalHealthAssessmentEntity scorePHQA(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        String severity;
        String interpretation;

        if (score <= 4) {
            severity = "minimal";
            interpretation = "Minimal or no depression";
        } else if (score <= 10) {
            severity = "mild";
            interpretation = "Mild depression";
        } else if (score <= 15) {
            severity = "moderate";
            interpretation = "Moderate depression";
        } else {
            severity = "severe";
            interpretation = "Severe depression";
        }

        boolean positiveScreen = score >= 11;
        boolean requiresFollowup = score >= 11;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.PHQ_A)
            .score(score)
            .maxScore(27)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(11)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score AUDIT-C (Alcohol Use Disorders Identification Test - Concise)
     * Range: 0-12
     * Threshold: ≥4 for men, ≥3 for women
     */
    private MentalHealthAssessmentEntity scoreAUDITC(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        // Extract gender from clinical notes
        String gender = extractGenderFromNotes(request.getClinicalNotes());
        int thresholdScore = "female".equalsIgnoreCase(gender) ? 3 : 4;

        boolean positiveScreen = score >= thresholdScore;
        String severity = positiveScreen ? "positive" : "negative";
        String interpretation = positiveScreen ?
            "Positive screen for hazardous alcohol use" :
            "Negative screen for hazardous alcohol use";

        boolean requiresFollowup = positiveScreen;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.AUDIT_C)
            .score(score)
            .maxScore(12)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(thresholdScore)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score DAST-10 (Drug Abuse Screening Test)
     * Range: 0-10
     * Severity: 0=none, 1-2=low, 3-5=moderate, 6-8=substantial, 9-10=severe
     */
    private MentalHealthAssessmentEntity scoreDAST10(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        String severity;
        String interpretation;

        if (score == 0) {
            severity = "none";
            interpretation = "No drug problem reported";
        } else if (score <= 2) {
            severity = "low";
            interpretation = "Low level drug problem";
        } else if (score <= 5) {
            severity = "moderate";
            interpretation = "Moderate drug problem";
        } else if (score <= 8) {
            severity = "substantial";
            interpretation = "Substantial drug problem";
        } else {
            severity = "severe";
            interpretation = "Severe drug problem";
        }

        boolean positiveScreen = score >= 1;
        boolean requiresFollowup = score >= 3;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.DAST_10)
            .score(score)
            .maxScore(10)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(1)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score PCL-5 (PTSD Checklist for DSM-5)
     * Range: 0-80
     * Threshold: ≥31-33 suggests PTSD
     */
    private MentalHealthAssessmentEntity scorePCL5(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        int score = request.getResponses().values().stream()
            .mapToInt(Integer::intValue)
            .sum();

        boolean positiveScreen = score >= 31;
        String severity = positiveScreen ? "positive" : "negative";
        String interpretation = positiveScreen ?
            "Probable PTSD diagnosis" :
            "Below clinical threshold for PTSD";

        boolean requiresFollowup = positiveScreen;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.PCL_5)
            .score(score)
            .maxScore(80)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(31)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Score MDQ (Mood Disorder Questionnaire)
     * 13 yes/no questions + 2 impact questions
     * Positive: ≥7 yes + problems caused + same time period
     */
    private MentalHealthAssessmentEntity scoreMDQ(
        String tenantId,
        MentalHealthAssessmentRequest request
    ) {
        // Count yes responses to questions 1-13
        int score = 0;
        for (int i = 1; i <= 13; i++) {
            score += request.getResponses().getOrDefault("q" + i, 0);
        }

        // Check impact questions
        boolean problemsCaused = request.getResponses().getOrDefault("problems_caused", 0) == 1;
        boolean sameTimePeriod = request.getResponses().getOrDefault("same_time", 0) == 1;

        // Positive screen requires all three criteria
        boolean positiveScreen = score >= 7 && problemsCaused && sameTimePeriod;
        String severity = positiveScreen ? "positive" : "negative";
        String interpretation = positiveScreen ?
            "Positive screen for bipolar disorder" :
            "Negative screen for bipolar disorder";

        boolean requiresFollowup = positiveScreen;

        return MentalHealthAssessmentEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .type(AssessmentType.MDQ)
            .score(score)
            .maxScore(13)
            .severity(severity)
            .interpretation(interpretation)
            .positiveScreen(positiveScreen)
            .thresholdScore(7)
            .requiresFollowup(requiresFollowup)
            .assessedBy(request.getAssessedBy())
            .assessmentDate(request.getAssessmentDate() != null ?
                request.getAssessmentDate() : Instant.now())
            .responses(request.getResponses())
            .clinicalNotes(request.getClinicalNotes())
            .build();
    }

    /**
     * Extract gender from clinical notes for AUDIT-C scoring
     */
    private String extractGenderFromNotes(String clinicalNotes) {
        if (clinicalNotes == null || clinicalNotes.isBlank()) {
            return "male"; // Default to male threshold
        }

        if (clinicalNotes.toLowerCase().contains("gender:female")) {
            return "female";
        } else if (clinicalNotes.toLowerCase().contains("gender:male")) {
            return "male";
        }

        return "male"; // Default
    }

    /**
     * Build trend data from assessments
     */
    private AssessmentTrend buildTrend(
        UUID patientId,
        AssessmentType type,
        List<MentalHealthAssessmentEntity> assessments
    ) {
        if (assessments.isEmpty()) {
            return AssessmentTrend.builder()
                .patientId(patientId)
                .assessmentType(type.name())
                .trend("unknown")
                .dataPoints(List.of())
                .averageScore(0.0)
                .build();
        }

        List<DataPoint> dataPoints = assessments.stream()
            .map(a -> new DataPoint(a.getAssessmentDate(), a.getScore()))
            .collect(Collectors.toList());

        double averageScore = assessments.stream()
            .mapToInt(MentalHealthAssessmentEntity::getScore)
            .average()
            .orElse(0.0);

        String trend = determineTrend(assessments);

        MentalHealthAssessmentEntity latest = assessments.get(assessments.size() - 1);
        MentalHealthAssessmentEntity previous = assessments.size() > 1 ?
            assessments.get(assessments.size() - 2) : null;

        return AssessmentTrend.builder()
            .patientId(patientId)
            .assessmentType(type.name())
            .trend(trend)
            .dataPoints(dataPoints)
            .averageScore(averageScore)
            .currentSeverity(latest.getSeverity())
            .previousSeverity(previous != null ? previous.getSeverity() : null)
            .build();
    }

    /**
     * Determine trend (improving, stable, declining)
     */
    private String determineTrend(List<MentalHealthAssessmentEntity> assessments) {
        if (assessments.size() < 2) {
            return "unknown";
        }

        int latestScore = assessments.get(assessments.size() - 1).getScore();
        int previousScore = assessments.get(assessments.size() - 2).getScore();

        int difference = latestScore - previousScore;

        if (Math.abs(difference) <= 2) {
            return "stable";
        } else if (difference > 2) {
            return "declining"; // Score increased = worse symptoms
        } else {
            return "improving"; // Score decreased = better symptoms
        }
    }

    /**
     * Map entity to DTO
     */
    private MentalHealthAssessmentDTO mapToDTO(MentalHealthAssessmentEntity entity) {
        return MentalHealthAssessmentDTO.builder()
            .id(entity.getId().toString())
            .patientId(entity.getPatientId())
            .type(entity.getType().name())
            .name(getAssessmentName(entity.getType()))
            .score(entity.getScore())
            .maxScore(entity.getMaxScore())
            .severity(entity.getSeverity())
            .interpretation(entity.getInterpretation())
            .positiveScreen(entity.getPositiveScreen())
            .thresholdScore(entity.getThresholdScore())
            .requiresFollowup(entity.getRequiresFollowup())
            .assessedBy(entity.getAssessedBy())
            .assessmentDate(entity.getAssessmentDate())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    /**
     * Get human-readable assessment name
     */
    private String getAssessmentName(AssessmentType type) {
        return switch (type) {
            case PHQ_9 -> "Patient Health Questionnaire-9";
            case GAD_7 -> "Generalized Anxiety Disorder-7";
            case PHQ_2 -> "Patient Health Questionnaire-2";
            case PHQ_A -> "PHQ-9 Modified for Adolescents";
            case AUDIT_C -> "Alcohol Use Disorders Identification Test";
            case DAST_10 -> "Drug Abuse Screening Test";
            case PCL_5 -> "PTSD Checklist";
            case MDQ -> "Mood Disorder Questionnaire";
            case CAGE_AID -> "CAGE Adapted to Include Drugs";
        };
    }

    /**
     * Assessment Trend DTO
     */
    @Data
    @Builder
    public static class AssessmentTrend {
        private UUID patientId;
        private String assessmentType;
        private String trend; // improving, stable, declining, unknown
        private List<DataPoint> dataPoints;
        private Double averageScore;
        private String currentSeverity;
        private String previousSeverity;
    }

    /**
     * Data Point for trend
     */
    @Data
    @Builder
    public static class DataPoint {
        private final Instant date;
        private final Integer score;
    }
}
