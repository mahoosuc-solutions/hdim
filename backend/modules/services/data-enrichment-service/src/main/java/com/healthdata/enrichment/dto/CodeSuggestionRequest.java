package com.healthdata.enrichment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for code suggestions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeSuggestionRequest {
    @NotBlank(message = "Text cannot be empty")
    private String text;

    @NotBlank(message = "Code system cannot be empty")
    private String codeSystem;

    private String context;
    private int maxSuggestions = 10;
}
