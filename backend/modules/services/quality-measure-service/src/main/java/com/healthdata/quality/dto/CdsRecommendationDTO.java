package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.CdsRecommendationEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for CDS Recommendation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsRecommendationDTO {

    private UUID id;
    private UUID patientId;
    private UUID ruleId;
    private String title;
    private String description;
    private String category;
    private String urgency;
    private String status;
    private Integer priority;
    private List<String> actionItems;
    private String suggestedIntervention;
    private String evidenceSource;
    private String clinicalGuideline;
    private UUID relatedCareGapId;
    private String relatedMeasureId;
    private String relatedMeasureName;
    private Instant dueDate;
    private Long daysOverdue;
    private Instant evaluatedAt;
    private Instant acknowledgedAt;
    private String acknowledgedBy;
    private Instant completedAt;
    private String completedBy;
    private String completionOutcome;
    private Instant declinedAt;
    private String declinedBy;
    private String declineReason;
    private Instant createdAt;
    private Instant updatedAt;

    // Related patient info (populated by service)
    private String patientName;
    private String patientMrn;
    private String patientRiskLevel;

    /**
     * Convert entity to DTO
     */
    public static CdsRecommendationDTO fromEntity(CdsRecommendationEntity entity) {
        if (entity == null) return null;

        Long daysOverdue = null;
        if (entity.getDueDate() != null && entity.getDueDate().isBefore(Instant.now())) {
            daysOverdue = java.time.Duration.between(entity.getDueDate(), Instant.now()).toDays();
        }

        return CdsRecommendationDTO.builder()
            .id(entity.getId())
            .patientId(entity.getPatientId())
            .ruleId(entity.getRuleId())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .category(entity.getCategory() != null ? entity.getCategory().name() : null)
            .urgency(entity.getUrgency() != null ? entity.getUrgency().name() : null)
            .status(entity.getStatus() != null ? entity.getStatus().name() : null)
            .priority(entity.getPriority())
            .actionItems(parseJsonArray(entity.getActionItems()))
            .suggestedIntervention(entity.getSuggestedIntervention())
            .evidenceSource(entity.getEvidenceSource())
            .clinicalGuideline(entity.getClinicalGuideline())
            .relatedCareGapId(entity.getRelatedCareGapId())
            .relatedMeasureId(entity.getRelatedMeasureId())
            .relatedMeasureName(entity.getRelatedMeasureName())
            .dueDate(entity.getDueDate())
            .daysOverdue(daysOverdue)
            .evaluatedAt(entity.getEvaluatedAt())
            .acknowledgedAt(entity.getAcknowledgedAt())
            .acknowledgedBy(entity.getAcknowledgedBy())
            .completedAt(entity.getCompletedAt())
            .completedBy(entity.getCompletedBy())
            .completionOutcome(entity.getCompletionOutcome())
            .declinedAt(entity.getDeclinedAt())
            .declinedBy(entity.getDeclinedBy())
            .declineReason(entity.getDeclineReason())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        return List.of(json.replace("[", "").replace("]", "").replace("\"", "").split(","));
    }
}
