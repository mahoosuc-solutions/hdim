package com.healthdata.quality.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quality Measure Result DTO
 * API response object for quality measure calculations
 *
 * This DTO excludes internal implementation details like raw CQL results
 * and JSONB columns that cause OpenAPI schema generation issues.
 */
@Schema(description = "Quality measure calculation result for a patient")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QualityMeasureResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Unique identifier for this quality measure result", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "Tenant identifier", example = "demo-tenant")
    private String tenantId;

    @Schema(description = "Patient identifier", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID patientId;

    // Measure information
    @Schema(description = "Quality measure identifier (e.g., HEDIS code)", example = "CDC-A1C9")
    private String measureId;

    @Schema(description = "Human-readable measure name", example = "Comprehensive Diabetes Care: HbA1c Control (<9.0%)")
    private String measureName;

    @Schema(description = "Measure category or program", example = "HEDIS", allowableValues = {"HEDIS", "CMS", "CUSTOM"})
    private String measureCategory;

    @Schema(description = "Measurement year", example = "2024")
    private Integer measureYear;

    // Result details
    @Schema(description = "Whether patient meets numerator criteria (compliant)", example = "true")
    private Boolean numeratorCompliant;

    @Schema(description = "Whether patient is in the denominator (eligible)", example = "true")
    private Boolean denominatorEligible;

    /**
     * Backward-compatible JSON mapping for the historical typo in the API field name.
     *
     * We intentionally emit BOTH fields:
     * - denominatorEligible (correct)
     * - denominatorElligible (deprecated typo)
     *
     * This allows existing clients to keep working while new clients migrate.
     */
    @JsonGetter("denominatorElligible")
    @Schema(
        description = "DEPRECATED: historical typo for denominatorEligible. Will be removed in a future major version.",
        deprecated = true
    )
    public Boolean getDenominatorElligible() {
        return denominatorEligible;
    }

    @JsonSetter("denominatorElligible")
    public void setDenominatorElligible(Boolean denominatorElligible) {
        this.denominatorEligible = denominatorElligible;
    }

    @Schema(description = "Compliance rate percentage (0.0 to 100.0)", example = "85.5")
    private Double complianceRate;

    @Schema(description = "Quality score", example = "92.3")
    private Double score;

    // Calculation details
    @Schema(description = "Date when measure was calculated", example = "2024-11-03")
    private LocalDate calculationDate;

    @Schema(description = "CQL library used for calculation", example = "HEDIS_CDC_2024")
    private String cqlLibrary;

    // Audit fields
    @Schema(description = "Timestamp when record was created", example = "2024-11-03T10:15:30")
    private LocalDateTime createdAt;

    @Schema(description = "User or system that created this record", example = "system")
    private String createdBy;

    @Schema(description = "Version number for optimistic locking", example = "1")
    private Integer version;
}
