package com.healthdata.payer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "ROI calculation input parameters for HDIM value projection")
public class RoiCalculationRequest {

    @NotNull(message = "Organization type is required")
    @JsonProperty("orgType")
    @Schema(description = "Organization type", example = "ACO", allowableValues = {"ACO", "HEALTH_SYSTEM", "HIE", "PAYER", "FQHC"})
    private String orgType;

    @NotNull(message = "Patient population is required")
    @Min(value = 1000, message = "Patient population must be at least 1,000")
    @Max(value = 5000000, message = "Patient population must be at most 5,000,000")
    @JsonProperty("patientPopulation")
    @Schema(description = "Total patient population size", example = "25000", minimum = "1000", maximum = "5000000")
    private Integer patientPopulation;

    @NotNull(message = "Current quality score is required")
    @DecimalMin(value = "0", message = "Quality score must be between 0 and 100")
    @DecimalMax(value = "100", message = "Quality score must be between 0 and 100")
    @JsonProperty("currentQualityScore")
    @Schema(description = "Current HEDIS quality score percentage", example = "70.0", minimum = "0", maximum = "100")
    private Double currentQualityScore;

    @NotNull(message = "Current star rating is required")
    @DecimalMin(value = "1.0", message = "Star rating must be between 1.0 and 5.0")
    @DecimalMax(value = "5.0", message = "Star rating must be between 1.0 and 5.0")
    @JsonProperty("currentStarRating")
    @Schema(description = "Current CMS Star Rating", example = "3.5", minimum = "1.0", maximum = "5.0")
    private Double currentStarRating;

    @NotNull(message = "Manual reporting hours is required")
    @Min(value = 0, message = "Reporting hours must be non-negative")
    @Max(value = 1000, message = "Reporting hours must be at most 1,000")
    @JsonProperty("manualReportingHours")
    @Schema(description = "Weekly hours spent on manual quality reporting", example = "40", minimum = "0", maximum = "1000")
    private Integer manualReportingHours;

    // Optional: save the calculation and capture lead info
    @JsonProperty("save")
    private Boolean save;

    @JsonProperty("contactName")
    private String contactName;

    @JsonProperty("contactEmail")
    private String contactEmail;

    @JsonProperty("contactCompany")
    private String contactCompany;
}
