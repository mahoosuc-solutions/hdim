package com.healthdata.quality.dto;

import com.healthdata.quality.persistence.CdsRuleEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for CDS Rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdsRuleDTO {

    private UUID id;
    private String ruleName;
    private String ruleCode;
    private String description;
    private String category;
    private Integer priority;
    private String cqlLibraryName;
    private String cqlExpression;
    private String recommendationTemplate;
    private String evidenceSource;
    private String clinicalGuideline;
    private List<String> actionItems;
    private String defaultUrgency;
    private Boolean active;
    private Boolean requiresAcknowledgment;
    private List<String> applicableConditions;
    private List<String> exclusionCriteria;
    private String version;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert entity to DTO
     */
    public static CdsRuleDTO fromEntity(CdsRuleEntity entity) {
        if (entity == null) return null;

        return CdsRuleDTO.builder()
            .id(entity.getId())
            .ruleName(entity.getRuleName())
            .ruleCode(entity.getRuleCode())
            .description(entity.getDescription())
            .category(entity.getCategory() != null ? entity.getCategory().name() : null)
            .priority(entity.getPriority())
            .cqlLibraryName(entity.getCqlLibraryName())
            .cqlExpression(entity.getCqlExpression())
            .recommendationTemplate(entity.getRecommendationTemplate())
            .evidenceSource(entity.getEvidenceSource())
            .clinicalGuideline(entity.getClinicalGuideline())
            .actionItems(parseJsonArray(entity.getActionItems()))
            .defaultUrgency(entity.getDefaultUrgency() != null ? entity.getDefaultUrgency().name() : null)
            .active(entity.getActive())
            .requiresAcknowledgment(entity.getRequiresAcknowledgment())
            .applicableConditions(parseJsonArray(entity.getApplicableConditions()))
            .exclusionCriteria(parseJsonArray(entity.getExclusionCriteria()))
            .version(entity.getVersion())
            .createdBy(entity.getCreatedBy())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        // Simple parsing - in production use Jackson
        return List.of(json.replace("[", "").replace("]", "").replace("\"", "").split(","));
    }
}
