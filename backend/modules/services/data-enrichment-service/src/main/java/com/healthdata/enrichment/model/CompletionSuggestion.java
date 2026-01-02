package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * Suggested action to complete data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletionSuggestion {
    private String element;
    private String description;
    private int priority;
    private String action;
    private String reason;
}
