package com.healthdata.enrichment.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of code validation with suggestions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeValidationResult {
    private String code;
    private boolean valid;
    private String description;
    private boolean billable;
    private String version;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    @Builder.Default
    private List<String> hierarchy = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
