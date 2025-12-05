package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;
import java.util.Map;

/**
 * Suggested code with confidence.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSuggestion {
    private String code;
    private String description;
    private String codeSystem;
    private double confidence;
    private boolean billable;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
