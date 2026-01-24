package com.healthdata.ingestion.api.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for starting data ingestion.
 *
 * <p>Configures the data generation parameters for load testing and demonstration purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionRequest {

    /**
     * Tenant ID for data isolation (required).
     */
    @NotNull(message = "Tenant ID is required")
    private String tenantId;

    /**
     * Number of patients to generate (10-10,000).
     */
    @NotNull(message = "Patient count is required")
    @Min(value = 10, message = "Patient count must be at least 10")
    @Max(value = 10000, message = "Patient count must not exceed 10,000")
    private Integer patientCount;

    /**
     * Whether to create care gaps for patients (default: true).
     */
    @Builder.Default
    private Boolean includeCareGaps = true;

    /**
     * Whether to seed quality measures (default: true).
     */
    @Builder.Default
    private Boolean includeQualityMeasures = true;

    /**
     * Data generation scenario (basic, hedis, complex, risk-stratification).
     */
    @Pattern(regexp = "basic|hedis|complex|risk-stratification",
            message = "Scenario must be one of: basic, hedis, complex, risk-stratification")
    @Builder.Default
    private String scenario = "hedis";
}
