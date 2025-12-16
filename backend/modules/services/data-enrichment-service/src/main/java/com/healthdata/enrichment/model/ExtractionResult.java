package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Complete extraction output with confidence scores.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractionResult {
    private String id;
    private String taskId;  // For async processing
    private String tenantId;

    @Builder.Default
    private List<ExtractedEntity> entities = new ArrayList<>();

    private double overallConfidence;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private LocalDateTime extractedAt = LocalDateTime.now();

    /**
     * Get entities filtered by type.
     */
    public List<ExtractedEntity> getEntitiesByType(EntityType type) {
        return entities.stream()
            .filter(e -> e.getType() == type)
            .collect(Collectors.toList());
    }
}
