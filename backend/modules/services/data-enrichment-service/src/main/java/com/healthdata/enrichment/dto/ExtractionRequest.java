package com.healthdata.enrichment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Request for extracting entities from clinical notes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionRequest {
    @NotBlank(message = "Clinical note cannot be empty")
    private String clinicalNote;

    private String tenantId;
    private boolean async;
}
