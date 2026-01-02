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

/**
 * Report of gaps in patient data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissingDataReport {
    private String patientId;
    private String tenantId;
    private double completenessScore;

    @Builder.Default
    private Map<String, List<String>> missingElements = new HashMap<>();

    @Builder.Default
    private List<CompletionSuggestion> suggestions = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
