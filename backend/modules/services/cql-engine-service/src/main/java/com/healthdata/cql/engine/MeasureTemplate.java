package com.healthdata.cql.engine;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a parsed measure template from CQL Library
 *
 * This class encapsulates the logic for a quality measure including:
 * - Denominator criteria (who is eligible)
 * - Numerator criteria (who is compliant)
 * - Exclusion criteria (who should be excluded)
 * - Data requirements (what FHIR resources are needed)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(builder = MeasureTemplate.MeasureTemplateBuilder.class)
public class MeasureTemplate {

    /**
     * Unique identifier for this template
     */
    private UUID templateId;

    /**
     * Measure identifier (e.g., "HEDIS-CDC", "HEDIS-CBP")
     */
    private String measureId;

    /**
     * Measure name
     */
    private String measureName;

    /**
     * Version of the measure specification
     */
    private String version;

    /**
     * CQL content (human-readable logic)
     */
    private String cqlContent;

    /**
     * ELM JSON (compiled expression logical model)
     */
    private String elmJson;

    /**
     * Parsed denominator expression
     * This defines who is eligible for the measure
     */
    private Map<String, Object> denominatorCriteria;

    /**
     * Parsed numerator expression
     * This defines who meets the measure criteria (is compliant)
     */
    private Map<String, Object> numeratorCriteria;

    /**
     * Parsed exclusion expression
     * This defines who should be excluded from the measure
     */
    private Map<String, Object> exclusionCriteria;

    /**
     * Data requirements - which FHIR resource types are needed
     * e.g., ["Patient", "Observation", "Condition"]
     */
    private String[] requiredResourceTypes;

    /**
     * Value set references
     * Maps value set names to their OIDs or URLs
     */
    private Map<String, String> valueSets;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Tenant ID for multi-tenancy
     */
    private String tenantId;

    /**
     * Whether this template is active and ready for use
     */
    private boolean active;

    @JsonPOJOBuilder(withPrefix = "")
    public static class MeasureTemplateBuilder {
        // Lombok generates the builder methods
    }
}
