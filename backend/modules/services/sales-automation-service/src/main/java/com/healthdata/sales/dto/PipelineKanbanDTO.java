package com.healthdata.sales.dto;

import com.healthdata.sales.entity.OpportunityStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for pipeline Kanban view - opportunities organized by stage
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineKanbanDTO {

    private List<KanbanColumn> columns;
    private PipelineSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KanbanColumn {
        private OpportunityStage stage;
        private String stageName;
        private int stageOrder;
        private int defaultProbability;
        private Long opportunityCount;
        private BigDecimal totalValue;
        private BigDecimal weightedValue;
        private List<KanbanCard> opportunities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KanbanCard {
        private UUID id;
        private String name;
        private String accountName;
        private UUID accountId;
        private String contactName;
        private UUID primaryContactId;
        private BigDecimal amount;
        private Integer probability;
        private BigDecimal weightedAmount;
        private LocalDate expectedCloseDate;
        private String nextStep;
        private String productTier;
        private String ownerName;
        private UUID ownerUserId;
        private Integer daysInStage;
        private Integer totalDaysOpen;
        private Boolean isOverdue;
        private Boolean isAtRisk;
        private Integer activityCount;
        private LocalDate lastActivityDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineSummary {
        private Long totalOpportunities;
        private Long openOpportunities;
        private BigDecimal totalPipelineValue;
        private BigDecimal weightedPipelineValue;
        private Double averageProbability;
        private BigDecimal averageDealSize;
        private Integer averageDaysOpen;
        private Map<String, BigDecimal> valueByStage;
        private Map<String, Long> countByStage;
    }
}
