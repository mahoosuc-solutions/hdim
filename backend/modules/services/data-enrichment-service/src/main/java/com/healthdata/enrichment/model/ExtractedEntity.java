package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a recognized medical entity from clinical text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedEntity {
    private EntityType type;
    private String text;
    private int startPosition;
    private int endPosition;
    private double confidence;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
